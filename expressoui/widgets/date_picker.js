/*
 * This Time picker will send a value according to the format
 */
(function ($, kendo) {
    var ui = kendo.ui,
        DatePicker = ui.DatePicker;

    var ExpressoDatePicker = DatePicker.extend({

        options: {
            name: "ExpressoDatePicker"
        },
        events: ["change"],

        init: function (element, options) {
            DatePicker.fn.init.call(this, element, options);

            // backward compatibility for code that uses .data("kendoDatePicker") directly
            // in the code (best practice: use UIUtil.getKendoWidget
            $(element).data("kendoDatePicker", $(element).data("kendoExpressoDatePicker"));
        },

        _change: function (value) {
            var widget = this;
            var date = expresso.util.Formatter.parseDate(value);

            //if min or max is specified, format date input to respect specified values
            if (widget.min() || widget.max()) {

                if (date < widget.min()) {
                    date = widget.min();
                } else if (date > widget.max()) {
                    date = widget.max();
                }
            }
            DatePicker.fn._change.call(this, date);
        }
    });

    ui.plugin(ExpressoDatePicker);
}(jQuery, window.kendo));