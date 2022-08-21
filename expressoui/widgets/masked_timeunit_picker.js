/*
 * This Time picker will send a value according to the format
 */
(function ($, kendo) {
    var ui = kendo.ui,
        MaskedTextBox = ui.MaskedTextBox;

    var ExpressoMaskedTimeUnitPicker = MaskedTextBox.extend({
        /**
         *
         * @param element
         * @param options
         * mask: default is 00:00 (also supported 00:00:00)
         */
        init: function (element, options) {
            options = options || {};
            if (options.change) {
                options.userchange = options.change;
            }

            // options
            options.format = options.format || "mm:ss";
            options.mask = options.format.replace(/[smH]/g, '0');
            options.change = this._onChange;
            MaskedTextBox.fn.init.call(this, element, options);
        },

        /**
         * @param value
         * @return {*}
         */
        value: function (value) {
            if (value === undefined) {
                // return the value according to the timeUnit
                value = MaskedTextBox.fn.value.call(this);

                var total = 0;
                for (var i = 0, s = 0, l = this.options.mask.length; s < l; i++, s += 3) {
                    total += Math.pow(60, i) * parseInt(value.substring(l - s - 2, l - s));
                }
                // console.log(value + " -> " + total);
                return total;
            } else {
                var time = expresso.util.Formatter.formatSeconds(value, {format: this.options.format});
                // console.log(value + " -> " + time);
                return MaskedTextBox.fn.value.call(this, time);
            }
        },

        _onChange: function (e) {
            // make sure that the time entered is ok
            var value = MaskedTextBox.fn.value.call(this);
            if (value.indexOf('_') != -1 || value.charAt(0) > 2 || value.charAt(3) > 5 ||
                (value.charAt(0) == 2 && value.charAt(1) > 3)) {
                MaskedTextBox.fn.value.call(this, "");
            }

            if (this.options.userchange) {
                this.options.userchange.call(this, e);
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoMaskedTimeUnitPicker).
            // The jQuery plugin would be jQuery.fn.kendoExpressoMaskedTimeUnitPicker.
            name: "ExpressoMaskedTimeUnitPicker",

            readonly: false
        }
    });

    ui.plugin(ExpressoMaskedTimeUnitPicker);
}(jQuery, window.kendo));