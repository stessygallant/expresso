(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoLanguageSelector = Widget.extend({
        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            //console.log("Options", this.options);
            this.processInput(element, this.options.labels);
        },

        /**
         *
         */
        processInput: function (input, labels) {
            var $input = $(input);

            $input.hide();
            $input.wrap("<span class='wrapper'></span>");

            var $languageDiv = $("<div class='language-div'></div>");
            $languageDiv.insertBefore($input);

            $languageDiv.kendoButtonGroup({
                items: [
                    {text: expresso.Common.getLabel("fr")},
                    {text: expresso.Common.getLabel("en")}
                ],
                index: $input.val() == "en" ? 1 : ($input.val() == "fr" ? 0 : undefined),
                select: function (e) {
                    switch (parseInt(e.indices)) {
                        case 1:
                            $input.val("en");
                            break;
                        case 0:
                        default:
                            $input.val("fr");
                            break;
                    }
                    $input.trigger("change");
                }
            });

        },

        value: function (v) {
            if (v === undefined) {
                // getter
                return $(this.element).val();
            }
            else {
                //setter
                if (v) {
                    $(this.element).parent().find(".language-div").data("kendoButtonGroup").select(
                        v == "en" ? 1 : (v == "fr" ? 0 : null)
                    );
                }
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoLanguageSelector).
            // The jQuery plugin would be jQuery.fn.kendoExpressoLanguageSelector
            name: "ExpressoLanguageSelector",

            readonly: false
        }
    });

    ui.plugin(ExpressoLanguageSelector);
}(jQuery, window.kendo));