(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoMultiInput = Widget.extend({
        $element: undefined,
        $input: undefined,
        $wrapper: undefined,
        $inputDiv: undefined,
        readOnly: false,

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            //console.log("Options", this.options);
            this.$element = $(element);

            // convert
            this._convertElementToWidget();

            // set initial value
            var initialValue;
            if (options.value !== undefined) {
                initialValue = options.value;
            } else if (this.$element.val()) {
                initialValue = this.$element.val();
            }
            if (initialValue !== undefined) {
                this.value(initialValue);
            }
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            // wrap the input
            this.$element.hide();
            this.$element.wrap("<div class='k-widget exp-multiinput'></div>");
            this.$wrapper = this.$element.parent();

            // add a text box and a div
            this.$wrapper.append("<input class='k-textbox k-input'>" +
                "<button class='k-button exp-add-input'><i class='fa fa-plus'></i></button>" +
                "<div class='exp-multiinput-div'></div>");
            this.$input = this.$wrapper.find(".k-input");
            this.$inputDiv = this.$wrapper.find(".exp-multiinput-div");

            // add a listener to the input
            this.$input.on("change", function () {
                var value = $(this).val();
                _this._validateInputValue(value).done(function (validatedValue) {
                    _this._addInputValue(validatedValue);
                }).always(function () {
                    _this.$input.val(null);
                    _this.$input.focus();
                });
            });

            // add a listener to remove an entry
            this.$wrapper.on("click", ".exp-close-button", function () {
                var $button = $(this);
                var $entry = $button.parent();
                $entry.remove();
                _this._refreshValue();
            });
        },

        /**
         *
         * @param value
         * @returns {*}
         * @private
         */
        _validateInputValue: function (value) {
            var _this = this;
            var $deferred;
            if (this.options.validate) {
                var valid = this.options.validate(value);
                if (valid === true || valid === undefined) {
                    $deferred = $.Deferred().resolve(value);
                } else if (valid === false) {
                    $deferred = $.Deferred().reject();
                } else {
                    expresso.util.UIUtil.showLoadingMask(this.$input.closest("div"), true);
                    $deferred = valid;
                }
            } else {
                $deferred = $.Deferred().resolve(value);
            }
            $deferred.always(function () {
                expresso.util.UIUtil.showLoadingMask(_this.$input.closest("div"), false);
            });
            return $deferred;
        },

        /**
         *
         * @param value
         * @private
         */
        _addInputValue: function (value) {
            if (value || value === 0) {
                this.$inputDiv.append("<div class='exp-multiinput-entry'><span class='exp-multiinput-value'>" +
                    value + "</span>" +
                    "<span class='exp-close-button'>X</span>" +
                    "</div>");

                if (this.readOnly) {
                    this.$inputDiv.find(".exp-close-button").hide();
                }

                this._refreshValue();
            }
        },

        /**
         *
         * @private
         */
        _refreshValue: function () {
            // update $(this.element)
            var v = this.value().join(",");
            $(this.element).val(v);
            $(this.element).trigger("change");
        },

        /**
         *
         * @param v
         * @returns {[]}
         */
        value: function (v) {
            if (v === undefined) {
                // getter
                var values = [];
                this.$inputDiv.find(".exp-multiinput-value").each(function () {
                    values.push($(this).text());
                });
                return values;
            } else {
                //setter
                var _this = this;
                if (v === null) {
                    // skip
                } else if ($.isArray(v)) {
                    $.each(v, function () {
                        _this._addInputValue(this);
                    });
                } else if (v.indexOf && v.indexOf(",") != -1) {
                    $.each(v.split(","), function () {
                        _this._addInputValue(this);
                    });
                } else {
                    _this._addInputValue(v);
                }
            }
        },

        /**
         *
         * @param readonly
         */
        readonly: function (readonly) {
            this.readOnly = readonly;

            if (readonly) {
                this.$input.attr("readonly", "readonly");
                this.$wrapper.find(".exp-add-input").attr("disabled", "disabled");
                this.$inputDiv.find(".exp-close-button").hide();
            } else {
                this.$input.removeAttr("readonly");
                this.$wrapper.find(".exp-add-input").removeAttr("disabled");
                this.$inputDiv.find(".exp-close-button").show();
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoMultiInput).
            // The jQuery plugin would be jQuery.fn.kendoExpressoMultiInput
            name: "ExpressoMultiInput",

            readonly: false
        }
    });

    ui.plugin(ExpressoMultiInput);
}(jQuery, window.kendo));