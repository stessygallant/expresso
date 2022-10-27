(function ($, kendo) {
    var ui = kendo.ui,
        DatePicker = ui.DatePicker;

    var ExpressoDateSelector = DatePicker.extend({
        $masterWrapper: undefined,

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoDateSelector).
            // The jQuery plugin would be jQuery.fn.kendoExpressoDateSelector
            name: "ExpressoDateSelector"
        },
        events: ["change"],

        /**
         *
         * @param element
         * @param options
         */
        init: function (element, options) {
            DatePicker.fn.init.call(this, element, options);

            // convert
            this._convertElementToWidget();
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            this.$masterWrapper = this.wrapper.closest(".k-datepicker").wrap("<div class='exp-date-selector'></div>").parent();
            this.$masterWrapper.prepend("<button class='exp-day-selector exp-previous-day'><i class='fa fa-chevron-left'></i></button>");
            this.$masterWrapper.append("<button class='exp-day-selector exp-next-day'><i class='fa fa-chevron-right'></i></button>");

            // add a listener for day selector
            this.$masterWrapper.find("button.exp-day-selector").on("click", function () {
                var date = _this.element.val();
                if (date) {
                    date = expresso.util.Formatter.parseDate(date);
                    date = date.addDays($(this).hasClass("exp-next-day") ? 1 : -1);
                    _this.value(date);
                    _this.trigger("change");
                }
            });
        },

        /**
         *
         */
        destroy: function () {
            this.$masterWrapper = null;
            DatePicker.fn.destroy.call(this);
        }
    });

    ui.plugin(ExpressoDateSelector);
}(jQuery, window.kendo));
