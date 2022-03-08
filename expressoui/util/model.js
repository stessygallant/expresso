var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * This is an utility module. It contains some utilities method to handle the resource model.
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.util.Model = (function () {

        /**
         *
         * @param resourceManager
         * @returns {jQuery} promise when the model is complete
         */
        var initModel = function (resourceManager) {
            var $deferred = $.Deferred();

            if (!resourceManager || !resourceManager.applicationPath) {
                return $deferred.reject();
            }

            if (!resourceManager.model) {
                resourceManager.model = resourceManager.applicationPath + "/model.js";
            }

            var $deferredModel;
            if (typeof resourceManager.model === "string") {
                var modelPath = resourceManager.model;
                // console.log("MODEL PATH [" + modelPath + "]");
                $deferredModel = $.get(modelPath).then(function (model) {
                    // backward compatibility: model could use _this to refer to resourceManager
                    var _this = resourceManager; // DO NOT TOUCH _this
                    if (model.startsWith("(") || model.startsWith("{")) {
                        // backward compatibility: model could be defined directly without namespace
                        model = eval(model);
                    } else {
                        // model is assigned to the namespace .Model
                        // sherpa.applications.operation.gatetransactionmanager.Model
                        var modelNamespace = expresso.Common.getApplicationNamespace(modelPath) + ".Model";
                        // console.log("modelNamespace [" + modelNamespace + "]");
                        model = eval(modelNamespace);
                    }
                    return model;
                });
            } else {
                $deferredModel = $.Deferred().resolve(resourceManager.model);
            }

            // once the model is loaded
            $deferredModel.done(function (model) {
                model = $.extend(true, {}, model && model.fields ? model : {fields: model});

                // create the model for the kendo UI Grid
                model = $.extend(true, {}, {
                    id: "id",
                    fields: {
                        type: {
                            type: "string",
                            editable: false,
                            defaultValue: resourceManager.resourceName
                        },
                        id: {
                            type: "number",
                            defaultValue: null,
                            editable: false,
                            nullable: true
                        },

                        // avoid having to define those fields in almost every class
                        // it is needed if we want to display those values in the Grid
                        creationDate: {
                            type: "date",
                            editable: false,
                            nullable: true,
                            defaultValue: null,
                            refreshable: true,
                            timestamp: true
                        },
                        creationUserId: {
                            type: "number",
                            editable: false,
                            nullable: true,
                            refreshable: true,
                            reference: "user",
                            defaultValue: null
                        },
                        creationUserFullName: {
                            type: "string",
                            transient: true
                        },
                        lastModifiedDate: {
                            type: "date",
                            editable: false,
                            nullable: true,
                            defaultValue: null,
                            refreshable: true,
                            timestamp: true
                        },
                        lastModifiedUserId: {
                            type: "number",
                            editable: false,
                            nullable: true,
                            refreshable: true,
                            reference: "user",
                            defaultValue: null
                        },
                        lastModifiedUserFullName: {
                            type: "string",
                            transient: true
                        },

                        // add by default a possible derived class
                        derived: {
                            transient: true,
                            defaultValue: {}
                        }
                    }
                }, model);

                for (var f in model.fields) {
                    var field = model.fields[f];
                    if (field) {
                        //console.log("FIELD: [" + f + "]", field);

                        // always set the name of the field in the field
                        field.name = f;

                        // for each field ending with Id, we need to create an object as KendoUI will throw an exception
                        // if the object is not defined
                        if (f.endsWith("Id")) {
                            var objectName = f.substring(0, f.length - 2);
                            model.fields[objectName] = model.fields[objectName] || {
                                defaultValue: {},
                                transient: true
                            };

                            // if there is no default value, set it to null (number are usually defaulted to 0)
                            if (field.defaultValue === undefined) {
                                field.defaultValue = null;
                            }

                            // if the Id is not mandatory, set the nullable attribute
                            // if (field.nullable === undefined) {
                            //     field.nullable = !(field.validation && field.validation.required);
                            // }
                        }

                        // any derived field is transient
                        if (f.indexOf(".") != -1) {
                            field.transient = (field.transient !== false);
                        }

                        if (f.endsWith("Ids")) {
                            // if default value is an array, set it to null (backward compatibility)
                            if (field.nullable && field.defaultValue && field.defaultValue.length === 0) {
                                field.defaultValue = null;
                            }

                            // then add the Labels conterpart
                            var labelField = f.substring(0, f.length - 3) + "Labels";
                            if (!model.fields[labelField]) {
                                model.fields[labelField] = {
                                    type: "string",
                                    transient: true
                                };
                            }
                        }

                        // if nullable is true, the defaultValue is ignored
                        if (field.nullable && field.defaultValue !== undefined && field.defaultValue !== null) {
                            console.warn("When field [" + f + "] is nullable, default value [" + field.defaultValue + "] is ignored. Setting nullable=false");
                            field.nullable = false;
                        }

                        if (/*field.type &&*/ !field.nullable && (!field.transient || field.nullable === false) &&
                            field.editable !== false && field.type != "boolean") {
                            field.validation = field.validation || {};
                            if (field.validation.required === undefined) {
                                //console.log("Adding required to [" + f + "]");
                                field.validation.required = true;
                            }
                        }

                        // if there is no default value, set it to null (number are usually defaulted to 0)
                        if (field.transient && field.defaultValue === undefined) {
                            field.defaultValue = null;
                        }

                        // for each field ending with Key, we assume it is unique
                        if (f.endsWith("Key")) {
                            field.unique = (field.unique !== false);
                        }

                        if (field.keyField) {
                            field.unique = true;
                        }

                        if (field.unique) {
                            // if there is no default value, set it to null
                            if (field.defaultValue === undefined) {
                                field.defaultValue = null;
                            }
                        }

                        // it means an array of ID
                        if (field.multipleSelection) {
                            field.defaultValue = field.defaultValue || [];
                        }

                        // we cannot do it because the form.js already contains some conversions to combobox
                        // if (f.endsWith("Id") && field.reference !== false && !field.values) {
                        //     field.reference = field.reference || true;
                        // }

                        //
                        // complete reference and values
                        //
                        var reference = field.reference || field.values;
                        if (reference) {
                            if (typeof reference === "function") {
                                // execute the function
                                reference = reference();
                            } else if ($.isArray(reference)) {
                                reference = {
                                    data: field.values
                                };
                            }

                            if (reference === true) {
                                reference = {};
                            } else if (typeof reference === "string") {
                                reference = {
                                    wsPath: reference,
                                    resourceName: reference
                                };
                            }

                            if (!reference.fieldName) {
                                if (f.endsWith("No")) {
                                    reference.fieldName = f.substring(0, f.length - 2) + "Id";
                                } else {
                                    // shall end with Id
                                    reference.fieldName = f;
                                }
                            }

                            if (!reference.resourceName) {
                                //console.log("Adding reference to [" + reference.fieldName + "]");

                                // by default, assume the field name is the resourceName + Id (or + No)
                                // ex: workOrderId/workOrderNo would mean resourceName=workOrder

                                var referenceResourceName = reference.fieldName;
                                if (referenceResourceName.indexOf(".") != -1) {
                                    // ex: derived.projectStatus will be projectStatus
                                    referenceResourceName = referenceResourceName.substring(referenceResourceName.indexOf(".") + 1);
                                }

                                if (referenceResourceName.endsWith("Id") || referenceResourceName.endsWith("No")) {
                                    referenceResourceName = referenceResourceName.substring(0, referenceResourceName.length - 2);
                                } else if (referenceResourceName.endsWith("Ids")) {
                                    referenceResourceName = referenceResourceName.substring(0, referenceResourceName.length - 3);
                                } else {
                                    console.warn("Invalid reference for [" + f + "]");
                                    referenceResourceName = null;
                                }
                                reference.resourceName = referenceResourceName;
                            }

                            if (!reference.wsPath) {
                                reference.wsPath = reference.resourceName;
                            }

                            // resource manager
                            if (reference.resourceName) {
                                // by default, reference allow search button. values does not.
                                if (field.values) {
                                    if (!reference.searchButtonEnabled) {
                                        reference.resourceManagerDef = null;
                                    } else {
                                        //reference.allowCreate = true;
                                        reference.allowView = false;
                                    }
                                }

                                // if we do not want to display the search button, make sure the manager is null
                                if (reference.searchButtonEnabled === false) {
                                    reference.resourceManagerDef = null;
                                }

                                // define the manager if not defined
                                if (reference.resourceManagerDef === undefined) {
                                    reference.resourceManagerDef = reference.resourceName.capitalize() + "Manager";
                                }
                            } else {
                                reference.resourceManagerDef = null;
                            }

                            // put back the reference
                            if (field.reference) {
                                field.reference = reference;
                            } else if (field.values) {
                                field.values = reference;
                            }
                        }

                        // parse the reference
                        if (field.reference) {
                            // by default, do NOT autoLoad values
                            field.reference.autoLoad = (field.reference.autoLoad === true);
                        }

                        // parse the values
                        if (field.values) {
                            // by default, autoLoad values
                            field.values.autoLoad = (field.values.autoLoad !== false);
                        }
                    }
                }

                // overwrite the model
                resourceManager.model = model;

                // console.log(resourceManager.resourceName + " - Model: ", resourceManager.masterResourceManager);
                $.when(
                    // load values if needed
                    loadValues(resourceManager.model, resourceManager.labels),

                    // get the appClass
                    expresso.Common.sendRequest(resourceManager.getWebServicePath() + "/appClass", null, null, {jsonCompliance: true}).done(function (appClassFields) {
                        for (var fieldName in appClassFields) {
                            var field = appClassFields[fieldName];

                            // verify if the field is mapped
                            var modelField = resourceManager.model.fields[fieldName];

                            if (!modelField) {
                                // if a transient field is not define, it is not an issue (but it should be defined)
                                if (!field.transient) {
                                    console.error("AppClass [" + resourceManager.getResourceName() + "] missing field mapping[" + fieldName + "]");
                                }
                            } else {
                                // override the restrictedRole
                                modelField.restrictedRole = field.restrictedRole;
                            }
                        }
                    })
                ).done(function () {
                    $deferred.resolve(resourceManager.model);
                });
            });

            return $deferred;
        };

        /**
         *
         * @param model
         * @param labels
         * @returns {jQuery|{}}
         */
        var loadValues = function (model, labels) {
            var promises = [];

            // get all values if needed
            $.each(model.fields, function (fieldName, field) {

                if (field && typeof field.defaultValue === "function") {
                    field.defaultValue = field.defaultValue();
                }

                // download the list of values if needed
                if (field) {
                    field.values = field.values || field.treeValues;
                }
                if (field && field.values && field.values.wsPath && field.values.autoLoad !== false) {
                    // get the list of values from the reference
                    var filter;
                    var values = field.values;
                    if (values.filter) {
                        filter = values.filter;
                        if (typeof filter === "function") {
                            filter = filter();
                        }
                    } else {
                        filter = [];
                    }
                    // by default, get all options
                    // the list will be used by the grid only
                    // the form will download the list itself (only active items)
                    filter = expresso.Common.buildKendoFilter(filter, {activeOnly: false});

                    promises.push(expresso.Common.sendRequest(values.wsPath, null, null, filter,
                        {waitOnElement: null}).done(function (result) {
                        values.data = [];

                        expresso.Common.updateValues(values.data, result, undefined, undefined, labels);

                        // if there is a defaultValue, and the defaultValue is a string, and the field type is number.
                        // It means that we are using a pgmKey and that we need to replace it with the id
                        if (field.defaultValue !== undefined && typeof field.defaultValue === "string" && field.type == "number") {
                            //console.log("Replacing defaultValue pgmKey [" + field.defaultValue + "] by id");
                            var defaultValues = $.grep(values.data, function (v) {
                                return v.pgmKey == field.defaultValue;
                            });

                            if (defaultValues && defaultValues.length) {
                                field.defaultValue = defaultValues[0].id;
                            } else {
                                console.warn("Cannot find the pgmKey [" + field.defaultValue + "] in your values for the field [" + fieldName + "]");
                                field.defaultValue = null;
                            }
                        }
                    }));
                } else if (field && field.reference) {
                    // if there is a defaultValue, and the defaultValue is a string, and the field type is number.
                    // It means that we are using a pgmKey and that we need to replace it with the id
                    if (field.defaultValue !== undefined && typeof field.defaultValue === "string" && field.type == "number") {
                        promises.push(expresso.Common.sendRequest(field.reference.wsPath, null, null,
                            expresso.Common.buildKendoFilter({pgmKey: field.defaultValue}),
                            {waitOnElement: null}).done(function (result) {
                            if (result.total == 0) {
                                alert("Invalid pgmKey [" + field.defaultValue + "] for resource [" + fieldName + "]");
                            } else {
                                field.defaultValue = result.data[0].id;
                            }
                        }));
                    }
                }
            });
            return $.when.apply(null, promises);
        };

        // return public properties and methods
        return {
            initModel: initModel
        };
    }()
);

