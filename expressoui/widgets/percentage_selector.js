(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoPercentageSelector = Widget.extend({
        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            //console.log("Options", this.options);
            this.processInput(element);
        },

        /**
         *
         * @param input
         */
        processInput: function (input) {
            var $input = $(input);

            $input.hide();
            $input.wrap("<span class='wrapper'></span>");

            var $percentageInput = $("<input class='percentage-input'>");
            $percentageInput.insertBefore($input);

            $percentageInput.kendoSlider({
                min: 0,
                max: 100,
                smallStep: 5,
                largeStep: 25,
                showButtons: false,
                value: $input.val(),
                change: function (e) {
                    $input.setval(e.value);
                }
            });

        },

        value: function (value) {
            if (value !== undefined) {
                this.element.val(value);
                $(this.element).trigger("change");
            } else {
                return this.element.val();
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoLanguageSelector).
            // The jQuery plugin would be jQuery.fn.kendoExpressoPercentageSelector
            name: "ExpressoPercentageSelector",

            readonly: false
        }
    });

    ui.plugin(ExpressoPercentageSelector);
}(jQuery, window.kendo));