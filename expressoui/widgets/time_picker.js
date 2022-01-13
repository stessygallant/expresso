/*
 * This Time picker will send a value according to the format
 */
(function ($, kendo) {
    var ui = kendo.ui,
        TimePicker = ui.TimePicker;

    var ExpressoTimePicker = TimePicker.extend({
        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            TimePicker.fn.init.call(this, element, options);
        },

        /**
         * Because value() return the full date, we need to overwrite it to return the format only
         * @param value
         * @return {*}
         */
        value: function (value) {
            if (value !== undefined) {
                return TimePicker.fn.value.call(this, value);
            } else {
                return this.element.val();
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoTimePicker).
            // The jQuery plugin would be jQuery.fn.kendoExpressoTimePicker.
            name: "ExpressoTimePicker",

            readonly: false
        }
    });

    ui.plugin(ExpressoTimePicker);
}(jQuery, window.kendo));