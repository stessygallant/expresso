/*
 * This Time picker will send a value according to the format
 */
(function ($, kendo) {
    var ui = kendo.ui,
        MaskedTextBox = ui.MaskedTextBox;

    var ExpressoMaskedTimePicker = MaskedTextBox.extend({
        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            options = options || {};
            if (options.change) {
                options.userchange = options.change;
            }
            options.mask = "00:00";
            options.change = this._onChange;
            MaskedTextBox.fn.init.call(this, element, options);

            // TODO make sure that there is not 2 change event triggerred
            // the first by the browser and the second by KendoMaskedTextBox
            $(element).on("change", function (e) {
                if (!e.isTrigger) {
                    e.stopPropagation();
                }
            });
        },

        /**
         * @param value
         * @return {*}
         */
        value: function (value) {
            if (value) {
                if (typeof value === "string" && value.length > 5) {
                    // it may be a date in string format
                    value = expresso.util.Formatter.parseDateTime(value);
                }

                // if the value is a date, get the HH:mm
                if (value instanceof Date) {
                    value = ("" + value.getHours()).padStart(2, '0') + ":" + ("" + value.getMinutes()).padStart(2, '0');
                }
            }
            return MaskedTextBox.fn.value.call(this, value);
        },

        _onChange: function (e) {
            // make sure that the time entered is ok
            var value = this.value();
            if (value.indexOf('_') != -1 || value.charAt(0) > 2 || value.charAt(3) > 5 ||
                (value.charAt(0) == 2 && value.charAt(1) > 3)) {
                this.value("");
            }

            if (this.options.userchange) {
                this.options.userchange.call(this, e);
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoMaskedTimePicker).
            // The jQuery plugin would be jQuery.fn.kendoExpressoMaskedTimePicker.
            name: "ExpressoMaskedTimePicker",

            readonly: false
        }
    });

    ui.plugin(ExpressoMaskedTimePicker);
}(jQuery, window.kendo));