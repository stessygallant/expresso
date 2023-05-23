(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    //  <input type="file" accept="image/*" capture="environment">
    var ExpressoDocumentPicker = Widget.extend({
        $element: undefined,
        $wrapper: undefined,
        readOnly: false,

        // these options are mandatory
        resourceSecurityPath: undefined,
        resourceName: undefined,
        resourceId: undefined,
        documentTypePgmKey: "DOCUMENT",

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            //console.log("Options", this.options);
            this.$element = $(element);

            if (!this.resourceSecurityPath) {
                this.resourceSecurityPath = this.resourceName;
            }

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

            // TO DO convert to <input type="file">
            // $el.attr("type", "file");

            // wrap the input
            this.$element.hide();
            this.$element.wrap("<div class='k-widget exp-document-picker'></div>");
            this.$wrapper = this.$element.parent();

            this.$wrapper.append("<div class='document'</div>");

        },

        /**
         *
         * @param v
         * @returns {[]}
         */
        value: function (v) {
            if (v === undefined) {
                // getter
                return null;
            } else {
                //setter
            }
        },

        /**
         *
         * @param readonly
         */
        readonly: function (readonly) {
            this.readOnly = readonly;

            if (readonly) {
                this.$wrapper.find(".document .fa-plus").hide();
                this.$wrapper.find(".document .fa-trash").hide();
            } else {
                this.$wrapper.find(".document .fa-plus").show();
                this.$wrapper.find(".document .fa-trash").show();
            }
        },

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoDocumentPicker).
            // The jQuery plugin would be jQuery.fn.kendoExpressoDocumentPicker
            name: "ExpressoDocumentPicker",
            readonly: false
        }
    });

    ui.plugin(ExpressoDocumentPicker);
}(jQuery, window.kendo));