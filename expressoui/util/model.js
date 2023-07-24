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
                $deferredModel = loadModel(modelPath, resourceManager);
            } else {
                $deferredModel = $.Deferred().resolve(resourceManager.model);
            }

            // once the model is loaded
            $deferredModel.done(function (model) {
                model = $.extend(true, {}, model && model.fields ? model : {fields: model});

                // for backward compatibility, if the model is defined and contains the type, use it
                if (!resourceManager.resourceName) {
                    resourceManager.resourceName = (model.fields.type && model.fields.type.defaultValue ? model.fields.type.defaultValue :
                        resourceManager.resourcePath);
                }
                if (!resourceManager.resourceFieldNo) {
                    resourceManager.resourceFieldNo = resourceManager.resourceName + "No";
                }

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

                // overwrite the model
                resourceManager.model = model;

                // set the masterIdProperty
                if (resourceManager.siblingResourceManager) {
                    model.masterIdProperty = null; // remove the masterIdProperty set by the masterResourceManager
                    resourceManager.setMasterIdProperty(resourceManager.siblingResourceManager);
                } else if (resourceManager.masterResourceManager) {
                    resourceManager.setMasterIdProperty(resourceManager.masterResourceManager);
                }

                for (var f in model.fields) {
                    var field = model.fields[f];
                    if (field) {
                        //console.log("FIELD: [" + f + "]", field);

                        // always set the name of the field in the field
                        field.name = f;

                        // for each field ending with Id, we need to create an object as KendoUI will throw an exception
                        // if the object is not defined
                        if (f.endsWith("Id")) {
                            // only for first level object
                            // otherwise the grid.fixObjectReferences will take care of it
                            if (f.indexOf(".") == -1) {
                                var objectFieldName = f.substring(0, f.length - 2);
                                model.fields[objectFieldName] = model.fields[objectFieldName] || {
                                    defaultValue: {},
                                    transient: true
                                };
                            }

                            // if there is no default value, set it to null (number are usually defaulted to 0)
                            if (field.defaultValue === undefined) {
                                field.defaultValue = null;
                            }
                        }

                        // any derived field is transient
                        if (f.indexOf(".") != -1) {
                            field.transient = (field.transient !== false);
                        }

                        // inline grids are always transient
                        if (field.inlineGridResourceManager) {
                            if (typeof field.inlineGridResourceManager === "string") {
                                field.inlineGridResourceManager = {
                                    resourceManager: field.inlineGridResourceManager
                                }
                            }

                            field.transient = true;
                            if (!field.nullable) {
                                field.validation = field.validation || {};
                                field.validation.required = true;
                            }
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

                        // when using inline grid, the master resource may not be defined yet
                        if (resourceManager.options.autoSyncGridDataSource === false) {
                            if (field.name == model.masterIdProperty) {
                                field.nullable = true;
                                field.defaultValue = null;
                            }
                        }

                        // patch for date when defaultValue are "stalled" when the application is opened for a long time
                        if (field.type == "date" && field.defaultValue === undefined && field.nullable !== true) {
                            field.defaultValue = null;
                            field.setNewDate = true;
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

                        //
                        // complete reference and values
                        //
                        var reference = field.reference || field.values;
                        if (reference) {
                            if (typeof reference === "function") {
                                // execute the function
                                reference = reference();
                            }

                            if ($.isArray(reference)) {
                                reference = {
                                    data: reference
                                };
                            } else if (reference === true) {
                                reference = {};
                            } else if (typeof reference === "string") {
                                reference = {
                                    resourcePath: reference,
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

                            if (reference.data) {
                                // make sure that the list contains all needed attributes
                                expresso.Common.updateDataValues(reference.data, resourceManager.labels);
                            } else {
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

                                // backward compatibility
                                if (!reference.resourcePath && reference.wsPath) {
                                    reference.resourcePath = reference.wsPath;
                                }

                                if (!reference.resourcePath) {
                                    reference.resourcePath = reference.resourceName;
                                    // backward compatibility
                                    reference.wsPath = reference.resourcePath;
                                }

                                // resource manager
                                if (reference.resourceName) {
                                    // backward compatibility
                                    if (reference.resourceManagerDef !== undefined) {
                                        reference.resourceManager = reference.resourceManagerDef;
                                    }

                                    // by default, reference allow search button. values does not.
                                    if (field.values) {
                                        if (!reference.searchButtonEnabled) {
                                            reference.resourceManager = null;
                                        } else {
                                            //reference.allowCreate = true;
                                            reference.allowView = false;
                                        }
                                    }

                                    // if we do not want to display the search button, make sure the manager is null
                                    if (reference.searchButtonEnabled === false) {
                                        reference.resourceManager = null;
                                    }

                                    // define the manager if not defined
                                    if (reference.resourceManager === undefined) {
                                        reference.resourceManager = reference.resourceName.capitalize() + "Manager";
                                    }
                                } else {
                                    reference.resourceManager = null;
                                }
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

                        // Hierarchical reference
                        if (field.hierarchicalParent) {
                            model.hierarchicalParent = field.name;
                        }
                    }
                }

                // console.log(resourceManager.resourceName + " - Model: ", resourceManager.masterResourceManager);
                $.when(
                    // load values if needed
                    loadValues(resourceManager.model, resourceManager.labels),

                    // get the appClass
                    resourceManager.invalidManager ? $.Deferred().resolve() :
                        expresso.Common.sendRequest(resourceManager.getWebServicePath() + "/appClass", null, null, {
                            jsonCompliance: true
                        }, {waitOnElement: null}).done(function (appClassFields) {
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
                                    // pgmKey are always restricted by admin
                                    modelField.restrictedRole = field.restrictedRole || (fieldName == "pgmKey" && modelField.nullable ?
                                        expresso.Common.getSiteNamespace().config.Configurations.adminRole : undefined);

                                    // set if require approval
                                    modelField.requireApprovalRole = field.requireApprovalRole;
                                    if (field.requireApprovalRole) {
                                        // set a flag on the model
                                        resourceManager.model.requireApprovalRole = true;
                                    }

                                    // set the hierarchical parent
                                    if (modelField.hierarchicalParent === undefined && field.hierarchicalParent !== undefined) {
                                        modelField.hierarchicalParent = field.hierarchicalParent;
                                    }
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
         * @param modelPath
         * @param [resourceManager]
         * @returns {*}
         */
        var loadModel = function (modelPath, resourceManager) {
            if (modelPath.endsWith(".Model")) {
                // this is a namespace. Get the model path
                if (modelPath.startsWith("expresso")) {
                    // remove only the application name (last)
                    modelPath = modelPath.split(".").slice(0, -1).join('/');
                } else {
                    // remove first namespace and the application name (last)
                    modelPath = modelPath.split(".").slice(1, -1).join('/');
                }
                modelPath += "/model.js";
            }

            modelPath += (modelPath.indexOf("?") != -1 ? "&" : "?") + "ver=" + expresso.Common.getSiteNamespace().config.Configurations.version;

            // model is assigned to the namespace .Model
            var modelNamespace = expresso.Common.getApplicationNamespace(modelPath) + ".Model";

            // create namespace
            expresso.Common.createApplicationNamespace(modelNamespace);

            // console.log("MODEL PATH [" + modelPath + "]");
            var $deferred = $.Deferred();
            $.get(modelPath).done(function (model) {
                // backward compatibility: model could use _this to refer to resourceManager
                var _this = resourceManager; // DO NOT TOUCH _this
                if (model.startsWith("(") || model.startsWith("{")) {
                    // backward compatibility: model could be defined directly without namespace
                    console.warn("Model file is not standard: missing namespace");
                    model = eval(model);
                    $deferred.resolve(model);
                } else {
                    // console.log("modelNamespace [" + modelNamespace + "]");
                    model = eval(modelNamespace);

                    // if extends -> load the extended model
                    if (model.extendsModel) {
                        expresso.util.Model.loadModel(model.extendsModel.model).done(function () {
                            $.extend(true, model, eval(model.extendsModel.model), {
                                type: {
                                    defaultValue: model.type.defaultValue
                                }
                            });
                            $deferred.resolve(model);
                        });
                    } else {
                        $deferred.resolve(model);
                    }
                }
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
                if (field && field.values && field.values.resourcePath && field.values.autoLoad !== false) {
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

                    promises.push(expresso.Common.sendRequest(values.resourcePath, null, null, filter,
                        {waitOnElement: null}).done(function (result) {
                        values.data = expresso.Common.updateDataValues(result, labels);

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
                        promises.push(expresso.Common.sendRequest(field.reference.resourcePath, null, null,
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
            initModel: initModel,
            loadModel: loadModel
        };
    }()
);

