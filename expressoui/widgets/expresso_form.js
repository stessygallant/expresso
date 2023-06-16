/*
 * This Form widget add classes to the HTML form elements
 */
(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoForm = Widget.extend({
        readyPromises: undefined,

        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            Widget.fn.init.call(this, element, options);
            this.readyPromises = [];

            //console.log("Options", this.options);
            this.processInputElements(element);
        },

        /**
         * Client must wait for the form to be ready
         * @returns {*}
         */
        ready: function () {
            var $form = $(this.element);
            if (this.readyPromises.length == 0) {
                expresso.util.UIUtil.showLoadingMask($form, false);

                // return an already resolved promise
                return $.Deferred().resolve().promise();
            } else {
                //console.log("readyPromises: " + this.readyPromises.length);
                return $.when.apply(null, this.readyPromises).done(function () {
                    expresso.util.UIUtil.showLoadingMask($form, false);
                    //console.log("Form is ready");
                });
            }
        },

        /**
         *
         * @param form
         */
        processInputElements: function (form) {
            var _this = this;
            var $form = $(form);

            var labels = this.options.labels;
            var resource = this.options.resource;

            // add a progress to the form
            //expresso.util.UIUtil.showLoadingMask($form, true);

            // process all input elements
            $form.find(":input").each(function (index, el) {
                if (el.name && el.name.endsWith("input")) {
                    // ignore
                } else {
                    var $el = $(el);

                    if ($el.closest(".exp-no-format").length) {
                        // if a parent specify no format, skip it
                    } else {
                        if (!$el.hasClass("exp-no-label")) {
                            // add a label for each input
                            _this.addLabel($el);
                        }

                        _this.upgradeInputType($el, labels, resource);
                    }
                }
            });

            //  try to localize the form
            expresso.Common.localizePage($form, labels);
        },

        /**
         *
         * @param $el
         * @param labels
         * @param resource
         */
        upgradeInputType: function ($el, labels, resource) {
            //var _this = this;
            var type = $el.attr("type");
            var clazz = $el.attr("class");
            var name = $el.attr("name");
            var el = $el[0];

            // make sure that the class does not have a k-class already
            if ((clazz && clazz.match(/(^|\s)k-[\w-]*\b/) && !(clazz.indexOf("k-valid") != -1 || clazz.indexOf("k-invalid") != -1 || clazz.indexOf("k-textbox") != -1))
                || $el.data("role") || type == "hidden") {
                // console.log("DO NOT enhance element [" + el.nodeName + "] Type [" + type + "]  Name [" + $el.attr("name") +
                //     "] Classes [" + clazz + ":" + (clazz && clazz.match(/k-[\w-]*\b/)) + "] Role [" + $el.data("role") + "]");
                return;
            }

            var model = this.options.model;
            var field;
            if (model) {
                field = model.fields[name];

                // if not found using the name, try with the binding name
                if (!field) {
                    var bindName = $el.attr("data-bind");
                    if (bindName && bindName.startsWith("value:")) {
                        bindName = bindName.substring("value:".length);
                        field = model.fields[bindName];
                    }
                }
            }

            var role = $el.data("exp-role");

            // console.log("Enhancing [" + el.nodeName + "] Type[" + type + "]  Name[" + $el.attr("name") +
            //     "] Role[" + role + "] FieldType[" + (field ? field.type : "n/a") + "]");

            // set the default value if needed (only for standalone form (do not set it for Kendo UI Form :model.id is not null)
            if ((!model || !model.id) && field && (!resource || !resource.id) && field.defaultValue !== undefined && !$el.val()) {
                // console.log("  Setting defaultValue [" + field.defaultValue + "]");
                $el.val(field.defaultValue);
            }

            if (role) {
                switch (role) {
                    case "languageselector":
                        $el.kendoExpressoLanguageSelector();
                        break;
                    case "timepicker":
                        $el.kendoExpressoTimePicker();
                        break;
                    case "datepicker":
                        $el.kendoExpressoDatePicker();
                        break;
                    case "maskedtimepicker":
                        $el.kendoExpressoMaskedTimePicker();
                        break;
                    case "percentageselector":
                        $el.kendoExpressoPercentageSelector();
                        break;
                    default:
                        console.warn("Unsupported role [" + role + "]");
                        break;
                }
            } else if (field && field.widget) {
                // if there is a widget define on the field, use it
                var widgetOptions = $.extend({}, true, {
                    field: field,
                    value: resource ? resource[name] : undefined
                }, field.widgetOptions)
                $el[field.widget](widgetOptions);
            } else if (field && field.inlineGridResourceManager) {
                this.displayInlineGrid(resource, field, $el);
            } else if (field && field.reference) {
                if (field.multipleSelection) {
                    if (field.lookupSelection) {
                        this.readyPromises.push(expresso.util.UIUtil.buildLookupSelection($el, field.reference.resourcePath, {
                            resource: resource,
                            model: model,
                            field: field,
                            filter: field.reference.filter,
                            nullable: field.nullable,
                            serverFiltering: field.reference.serverFiltering !== false,
                            fieldReference: field.reference
                        }));
                    } else {
                        this.readyPromises.push(
                            expresso.util.UIUtil.buildMultiSelect($el, field.reference.resourcePath, {
                                resource: resource,
                                model: model,
                                field: field,
                                filter: field.reference.filter,
                                nullable: field.nullable,
                                serverFiltering: field.reference.serverFiltering,
                                fieldReference: field.reference
                            }));
                    }
                } else {
                    if (field.reference.parentId) {
                        this.readyPromises.push(expresso.util.UIUtil.buildDropDownTree($el, field.reference.resourcePath, {
                            resource: resource,
                            model: model,
                            field: field,
                            filter: field.reference.filter,
                            triggerChangeOnInit: field.reference.triggerChangeOnInit,
                            parentId: field.reference.parentId,
                            parentRootId: field.reference.parentRootId,
                            allowNodeSelection: field.reference.allowNodeSelection,
                            fieldReference: field.reference
                        }));
                    } else {
                        this.readyPromises.push(
                            expresso.util.UIUtil.buildComboBox($el, field.reference.resourcePath, {
                                resource: resource,
                                model: model,
                                field: field,
                                filter: field.reference.filter,
                                nullable: field.nullable,
                                triggerChangeOnInit: field.reference.triggerChangeOnInit,
                                cascadeFrom: field.reference.cascadeFromId,
                                cascadeFromField: field.reference.cascadeFromField,
                                fieldReference: field.reference
                            }));
                    }
                }
            } else if (field && field.values) {
                if (field.multipleSelection) {
                    // if (field.lookupSelection) {
                    // }
                    // else {
                    this.readyPromises.push(expresso.util.UIUtil.buildMultiSelect($el,
                        field.values.resourcePath ? field.values.resourcePath : field.values.data, {
                            resource: resource,
                            model: model,
                            field: field,
                            nullable: field.nullable,
                            fieldValues: field.values
                        }));
                    // }
                } else {
                    if (field.values.parentId) {
                        if (field.values.resourcePath) {
                            this.readyPromises.push(expresso.util.UIUtil.buildDropDownTree($el, field.values.resourcePath, {
                                resource: resource,
                                model: model,
                                field: field,
                                filter: field.values.filter,
                                triggerChangeOnInit: field.values.triggerChangeOnInit,
                                parentId: field.values.parentId,
                                allowNodeSelection: field.values.allowNodeSelection,
                                fieldValues: field.values
                            }));
                        } else {
                            this.readyPromises.push(expresso.util.UIUtil.buildDropDownTree($el, field.values.data, {
                                resource: resource,
                                model: model,
                                field: field,
                                triggerChangeOnInit: field.values.triggerChangeOnInit,
                                parentId: field.values.parentId,
                                allowNodeSelection: field.values.allowNodeSelection,
                                fieldValues: field.values
                            }));
                        }
                    } else {
                        if (field.values.resourcePath) {
                            this.readyPromises.push(expresso.util.UIUtil.buildDropDownList($el, field.values.resourcePath, {
                                resource: resource,
                                model: model,
                                field: field,
                                filter: field.values.filter,
                                nullable: field.nullable,
                                triggerChangeOnInit: field.values.triggerChangeOnInit,
                                selectFirstOption: field.values.selectFirstOption,
                                grouping: field.values.grouping,
                                sortField: field.values.sortField,
                                cascadeFrom: field.values.cascadeFromId,
                                cascadeFromField: field.values.cascadeFromField,
                                fieldValues: field.values,
                                optionLabel: field.values.optionLabel
                            }));
                        } else {
                            this.readyPromises.push(expresso.util.UIUtil.buildDropDownList($el, field.values.data, {
                                resource: resource,
                                model: model,
                                field: field,
                                nullable: field.nullable,
                                triggerChangeOnInit: field.values.triggerChangeOnInit,
                                selectFirstOption: field.values.selectFirstOption,
                                grouping: field.values.grouping,
                                sortField: field.values.sortField,
                                fieldValues: field.values
                            }));
                        }
                    }
                }
            } else {
                if (field) {
                    switch (field.type) {
                        case "date":
                            if (field.timestamp) {
                                $el.attr("type", "datetime");
                            } else if (field.timeOnly) {
                                $el.attr("type", "time");
                            } else {
                                $el.attr("type", "date");
                            }
                            break;
                        case "number":
                            $el.attr("type", "number");

                            if (!$el.data("format")) {
                                $el.data("format", "{0:n" + (field.decimals || 0) + "}");
                            }

                            if (!$el.data("decimals")) {
                                $el.data("decimals", (field.decimals || 0));
                            }

                            if (field.currency) {
                                if (field.currency === true) {
                                    $el.data("format", "{0:c" + (field.decimals || 0) + "}");
                                } else {
                                    console.warn("currency attribute supports only [true]");
                                }
                            }
                            if (!field.allowNegative) {
                                if (!$el.attr("min")) {
                                    $el.attr("min", "0");
                                }
                            }

                            // support for min/max
                            if (field.min !== undefined) {
                                $el.attr("min", field.min);
                            }
                            if (field.max !== undefined) {
                                $el.attr("max", field.max);
                            }
                            break;
                        case "boolean":
                            $el.attr("type", "checkbox");
                            break;
                        case "string":
                            if (field.mask) {
                                if (!$el.data("mask")) {
                                    $el.data("mask", field.mask);
                                }
                            }
                            break;
                        case "picture":
                            $el.attr("type", "picture");
                            break;
                        case "document":
                            $el.attr("type", "document");
                            break;
                        default:
                            break;
                    }
                }

                switch (el.nodeName) {
                    case "INPUT":
                        type = $el.attr("type") || $el.data("type") || "text";

                        // set the input type if needed
                        if (!$el.attr("type")) {
                            $el.attr("type", type);
                        }

                        // keep the original data type
                        if (!$el.attr("data-type")) {
                            $el.attr("data-type", type);
                        }

                        if (type == "email" || name == "email") {
                            // put back the text type to avoid browser validation
                            type = "text";
                            $el.attr("type", "text");
                            $el.attr("data-type", "email");
                            $el.attr("data-validate", "false");
                        }
                        break;
                    case "BUTTON":
                        type = "button";
                        break;
                    case "SELECT":
                        type = "select";
                        break;
                    case "TEXTAREA":
                        type = "textarea";
                        break;
                    default:
                        alert("Cannot recognize the type for the element: [" + el.nodeName + "]");
                        break;
                }

                //console.log("Element [" + el.nodeName + " Type [" + type + "]  Name [" + name + "]");
                switch (type) {
                    case "button":
                        $el.addClass("k-button");
                        break;
                    case "text":
                        if ($el.data("mask")) {
                            $el.kendoMaskedTextBox({
                                mask: $el.data("mask"),
                                clearPromptChar: true,
                                unmaskOnPost: true,
                                change: function () {
                                    var maskedTextBox = this;
                                    if ($el.data("raw") == true) {
                                        var raw = maskedTextBox.raw(); //the result value will be "123456"
                                        if (resource && name) {
                                            if (maskedTextBox.options.mask) {
                                                resource.set(name, raw);
                                            }
                                        }
                                    }
                                }
                            });
                        } else {
                            $el.addClass("k-textbox");
                        }

                        // avoid suggestion on input field
                        if (!$el.attr("autocomplete")) {
                            $el.attr("autocomplete", "off");
                        }

                        // if the name of the input contains password, change the type
                        if (name && name.toLowerCase().indexOf("password") != -1) {
                            $el.attr("type", "password");
                            $el.attr("autocomplete", "off");
                        }
                        break;
                    case "password":
                        $el.addClass("k-textbox");
                        break;
                    case "number":
                        var format = $el.data("format");
                        var decimals = $el.data("decimals");

                        if (!format) {
                            format = "{0:n" + (decimals || 0) + "}";
                        }

                        // patch: for french: replace dot with comma
                        if (kendo.culture() && kendo.culture().name == "fr-CA") {
                            $el[0].addEventListener('keydown', function (event) {
                                if (event.key === '.') {
                                    window.setTimeout(function () {
                                        event.target.value += ',';
                                    }, 4);
                                    event.preventDefault();
                                }
                            });
                        }
                        $el.kendoNumericTextBox({
                            format: format,
                            decimals: decimals,
                            restrictDecimals: true
                        });
                        break;
                    case "checkbox":
                        this.convertCheckBox($el);
                        break;
                    case "radio":
                        this.convertRadioButton($el);
                        break;
                    case "textarea":
                        $el.addClass("k-textbox");
                        if (!$el.hasClass("half-length") && !$el.hasClass("exp-half-length")) {
                            $el.addClass("exp-full-length");
                        }
                        if (!$el.attr("rows")) {
                            $el.attr("rows", "3");
                        }
                        break;
                    case "date":
                        // Kendo will remove the type=date.
                        $el.addClass("date");
                        $el.kendoExpressoDatePicker({
                            min: (field ? field.min : undefined),
                            max: (field ? field.max : undefined)
                        });
                        break;
                    case "datetime":
                        // put a real type
                        $el.attr("type", "date");
                        $el.addClass("date");
                        var dateFormat = $el.data("format");
                        $el.kendoDateTimePicker({
                            interval: (field ? field.interval : undefined),
                            format: dateFormat
                        });
                        break;
                    case "time":
                        $el.attr("type", "text");
                        $el.addClass("time");
                        $el.kendoExpressoTimePicker({ // kendoExpressoMaskedTimePicker?
                            interval: (field ? field.interval : undefined)
                        });
                        break;
                    case "select":
                        if ($el.children("option").length) {
                            // we must localize the option before
                            expresso.Common.localizePage($el, labels);

                            // then build a dropdown list based on the options
                            $el.kendoDropDownList();

                            // trigger the change event
                            window.setTimeout(function () {
                                $el.data("kendoDropDownList").trigger("change");
                            }, 10);
                        }
                        break;

                    case "picture":
                        $el.attr("type", "text");
                        $el.kendoExpressoPicturePicker({
                            resourceName: field && field.picture && field.picture.resourceName ? field.picture.resourceName : (resource && resource.type ? resource.type : $el.data("resourceName")),
                            resourceId: (resource && resource.id ? resource.id : null),
                            resourceSecurityPath: field && field.picture && field.picture.resourceSecurityPath ? field.picture.resourceSecurityPath : $el.data("resourceSecurityPath"),
                            documentTypePgmKey: field && field.picture && field.picture.documentTypePgmKey ? field.picture.documentTypePgmKey : $el.data("documentTypePgmKey"),
                            maxWidth: field && field.picture && field.picture.maxWidth ? field.picture.maxWidth : $el.data("maxWidth"),
                            value: resource && field && resource[field.name] ? resource[field.name] : null
                        });
                        break;

                    case "document":
                        $el.attr("type", "text");
                        $el.kendoExpressoDocumentPicker({
                            resourceName: field && field.document && field.document.resourceName ? field.document.resourceName : (resource && resource.type ? resource.type : $el.data("resourceName")),
                            resourceId: (resource && resource.id ? resource.id : null),
                            resourceSecurityPath: field && field.document && field.document.resourceSecurityPath ? field.document.resourceSecurityPath : $el.data("resourceSecurityPath"),
                            documentTypePgmKey: field && field.document && field.document.documentTypePgmKey ? field.document.documentTypePgmKey : $el.data("documentTypePgmKey")
                        });
                        break;

                    case "email":
                    default:
                        // nothing special
                        break;
                }
            }
        },

        /**
         *
         * @param $input
         */
        addLabel: function ($input) {
            var name = $input.attr("name");
            var type = $input.attr("type");

            // if it is a radio, the value is used instead of the name (always same name)
            if (type == "radio") {
                name = $input.val();
            }

            // if there is no name, we do not add a text
            if (!name || type == "hidden") {
                return;
            }
            // console.log("Add label to element [" + name + "] Type [" + type + "]  Name [" + name + "]");

            // if this is a button, we need to handle it differently
            if ($input.attr("type") == "submit" || $input.attr("type") == "button") {
                // nothing to do
            } else {
                // if the $input is a KendoUI widget, get the widget
                if ($input.data("role") != null) {
                    $input = $input.closest(".k-widget");
                }

                // wrap the input in a DIV
                if (!$input.closest(".input-wrap").length) {
                    $input.wrap("<div class='exp-input-wrap input-wrap " +
                        ($input.hasClass("exp-full-length") || $input.hasClass("full-length") || ($input.is("textarea") &&
                            !$input.hasClass("exp-half-length") && !$input.hasClass("half-length")) ? "exp-full-length" : "") +
                        "'></div>");
                }

                // add a label if not defined
                var $label = $input.parent().children("label");
                if (!$label.length && !$input.parent().hasClass("exp-no-label")) {
                    $label = $("<label></label>");
                    $input.before($label);
                }

                // for checkbox and radio, move the label after the input
                if (type == "checkbox" || type == "radio" ||
                    (this.options.model && this.options.model.fields && this.options.model.fields[name] && this.options.model.fields[name].type == "boolean")) {
                    $label.insertAfter($input);
                }

                // set the input name for translation
                $label.attr("data-input-name", name);
            }
        },

        /**
         * Convert an HTML checkbox to Kendo UI checkbox
         * @param $input
         */
        convertCheckBox: function ($input) {
            if (!$input.hasClass("k-checkbox")) {
                var uniqueID = expresso.util.Util.guid();
                $input.attr("id", uniqueID);
                $input.addClass("k-checkbox");
                var $label = $input.parent().children("label");
                $label.attr("for", uniqueID);
                $label.addClass("k-checkbox-label");
            }
        },


        /**
         * Convert an HTML radio button to Kendo UI radio button
         * @param $input
         */
        convertRadioButton: function ($input) {
            if (!$input.hasClass("k-radio")) {
                var uniqueID = expresso.util.Util.guid();
                $input.attr("id", uniqueID);
                $input.addClass("k-radio");
                var $label = $input.parent().children("label");
                $label.attr("for", uniqueID);
                $label.addClass("k-radio-label");
            }
        },

        /**
         *
         * @param resource
         * @param field
         * @param $input
         */
        displayInlineGrid: function (resource, field, $input) {
            $input.hide();

            var masterResourceManager = this.options.resourceManager;
            var $div = $("<div class='exp-grid-inline'></div>").appendTo($input.parent());

            // if the parent is already created -> inline grid is online (auto sync ON)
            // if the parent is not already created -> inline grid is offline (auto sync OFF)
            expresso.Common.loadApplication(field.inlineGridResourceManager.resourceManager, {
                autoSyncGridDataSource: !!(resource && resource.id), // false,
                multipleSelectionEnabled: false,
                activeOnly: field.inlineGridResourceManager.activeOnly
            }).done(function (appInstance) {
                $div.data("resourceManager", appInstance);
                if (masterResourceManager) {
                    appInstance.masterResourceManager = masterResourceManager;

                    // when there is a change in subresource (update or create or delete), publish an update on the current resource
                    appInstance.eventCentral.subscribeEvent([appInstance.RM_EVENTS.RESOURCE_UPDATED, appInstance.RM_EVENTS.RESOURCE_CREATED,
                        appInstance.RM_EVENTS.RESOURCE_DELETED], function () {
                        // console.log("INLINEGRID - child has been changed (reloading master and refreshing counts)");
                        if (appInstance.masterResourceManager.currentResource) {
                            //force the refresh of the form
                            // DO NOT DO IT MANUALLY -> appInstance.masterResourceManager.currentResource.dirty = true;
                            appInstance.masterResourceManager.currentResource.set("makeCurrentResourceDirty", true);
                            appInstance.masterResourceManager.sections.grid.reloadCurrentResource();
                        }
                    });
                } else {
                    // we must fake a master current resource
                    appInstance.masterResourceManager.currentResource = {id: (resource && resource.id ? resource.id : -1)};
                }
                appInstance.list($div, {});
            });
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoForm).
            // The jQuery plugin would be jQuery.fn.kendoExpressoForm.
            name: "ExpressoForm",

            readonly: false
        }
    });

    ui.plugin(ExpressoForm);
}(jQuery, window.kendo));