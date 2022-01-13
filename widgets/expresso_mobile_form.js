/*
 * This Form widget add classes to the HTML form elements
 */
(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;
    var readyPromises;

    var ExpressoForm = Widget.extend({
        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            Widget.fn.init.call(this, element, options);
            this.readyPromises = [];

            //console.log("Options", this.options);
            this.processInputElements(element, this.options.labels, this.options.resource, this.options.model);
        },

        /**
         * Client must wait for the form to be ready
         * @returns {*}
         */
        ready: function () {
            var $form = $(this.element);
            if (this.readyPromises.length == 0) {
                expresso.util.UIUtil.showLoadingMask($form, false);

                // return a already resolved promise
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
         * @param labels
         * @param resource
         * @param model
         */
        processInputElements: function (form, labels, resource, model) {
            var _this = this;
            var $form = $(form);

            // add a progress to the form
            //expresso.util.UIUtil.showLoadingMask($form, true);

            // make sure that the container is ok
            if ($form[0].tagName == "DIV") {
                $form.children().wrapAll("<ul class='exp-mobile-form-listview' data-role='listview' data-style='inset'></ul>");
                $form = $form.children("ul");
            }

            // always wrap a form around the inputs
            $form.wrap($("<form class='exp-mobile-form' action='#'></form>"));

            // process all input elements
            $form.find(":input").each(function () {
                var $el = $(this)
                if (this.name && this.name.endsWith("input")) {
                    // ignore Kendo UI input
                } else {

                    if ($el.closest(".exp-no-format").length) {
                        // if a parent specify no format, skip it
                    } else {
                        if (!$el.hasClass("exp-no-label")) {
                            // add a label for each input
                            _this.addLabel($el, labels);
                        }

                        _this.upgradeInputType($el, labels, resource, model);
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
         * @param model
         */
        upgradeInputType: function ($el, labels, resource, model) {
            //var _this = this;
            var type = $el.attr("type");
            var clazz = $el.attr("class");
            var name = $el.attr("name");

            // make sure that the class does not have a k-class already
            if ((clazz && clazz.match(/(^|\s)k-[\w-]*\b/) && !(clazz.indexOf("k-valid") != -1 || clazz.indexOf("k-invalid") != -1 || clazz.indexOf("k-textbox") != -1))
                || $el.data("role") || type == "hidden") {
                // console.log("DO NOT enhance element [" + el.nodeName + "] Type [" + type + "]  Name [" + $el.attr("name") +
                //     "] Classes [" + clazz + ":" + (clazz && clazz.match(/k-[\w-]*\b/)) + "] Role [" + $el.data("role") + "]");
                return;
            }

            if (name && model && model.fields) {
                var field = model.fields[name];

                // convert only input in the model
                if (field) {
                    var $input = $el;


                    // set kendo widgets
                    if (field.type == "date") {
                        if (field.timestamp) {
                            $input.attr('type', 'datetime');
                        } else {
                            //$input.attr('type', 'date');
                            $input.attr('data-role', 'datepicker');
                        }
                    } else if (field.type == "string") {
                        $input.attr('type', 'text');
                    } else if (field.type == "boolean") {
                        $input.attr('type', 'checkbox');
                        $input.attr('data-role', 'switch');
                    } else if (field.type == "number") {
                        $input.attr('type', 'number');
                    } else {
                        $input.attr('type', 'text');
                    }

                    // values
                    if (field.values) {
                        // use a drop down list
                        expresso.util.UIUtil.buildDropDownList($input, field.values.data, {
                            resource: resource,
                            model: model,
                            field: field
                        });
                    }

                    // reference
                    if (field.reference) {
                        // use a combo box
                        expresso.util.UIUtil.buildComboBox($input, field.reference.wsPath, {
                            resource: resource,
                            model: model,
                            field: field
                        });
                    }

                    // set required
                    if (!field.nullable && field.type != "boolean" &&
                        (!resource.id || field.updatable !== false) &&
                        !$input.is("[readonly]")) {
                        var $label = $input.closest(".input-wrap").find("label");
                        $label.addClass("km-required");
                    }

                    // set value
                    if (resource.id) {
                        var value;

                        if (field.type == "date") {
                            value = expresso.util.Formatter.formatDate(resource[name], expresso.util.Formatter.DATE_FORMAT.DATE_ONLY);
                        } else if (field.type == "datetime") {
                            value = expresso.util.Formatter.formatDate(resource[name], expresso.util.Formatter.DATE_FORMAT.DATE_TIME);
                        } else if (field.type == "boolean") {
                            if (resource[name]) {
                                $input.prop("checked", true);
                            }
                        } else {
                            value = resource[name];
                        }

                        $input.setval(value);
                    }
                }
            }
        },

        /**
         *
         * @param $input
         * @param labels
         * */
        addLabel: function ($input, labels) {
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
                    $input.wrap("<li class='input-wrap'></li>");
                }

                // wrap the label around the input
                if (!$input.closest("label").length) {
                    if (type == "checkbox" || type == "radio") {
                        $input.wrap("<label class='km-checkbox-label'></label>");
                        $input.parent().append(expresso.Common.getLabel(name, labels));
                    } else {
                        $input.wrap("<label class='km-label-above'>" +
                            expresso.Common.getLabel(name, labels)
                            + "</label>");
                    }
                }
            }
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