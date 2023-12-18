/**
 * Anonymous function to contains local variables
 */
(function () {

    /**
     * Handle setting value on the initial element and put it back to the KendoUI element.
     * Developers do not have to know if the HTML element has been wrapped with Kendo UI element.
     * @param value
     * @param [triggerChangeEvent] true if you want to trigger the change on the widget.
     *     Default is false, but for ComboBox, default is true
     * @return {*}
     */
    $.fn.setval = function (value, triggerChangeEvent) {
        var $this = this;

        if (arguments.length >= 1) {
            var role = $this.data("role");
            if (role) {
                // then set the value to Kendo UI element
                var widget = expresso.util.UIUtil.getKendoWidget($this);
                if (widget) {
                    // setter invoked on a DOM element enhanced by KendoUI
                    if (typeof widget.value === "function") {

                        // dropdowntree does not support null
                        if (role == "dropdowntree" && value === null) {
                            value = "";
                        }
                        widget.value(value);

                        if (value === null && role == "combobox") {
                            // we need to do it otherwise the dataItem will remain
                            widget.select(null);
                            widget.text(null);
                        }

                        if (triggerChangeEvent !== false &&
                            (triggerChangeEvent || role == "combobox" || role == "dropdownlist" || role == "dropdowntree")) {
                            widget.trigger("change");
                        } else {
                            widget.expressoPreviousValue = value;
                        }
                        return this;
                    }
                }
            } else {
                if ($this.attr("type") === "checkbox") {
                    $this.prop("checked", !!value).trigger("change");
                    return this;
                } else if ($this.attr("type") === "radio") {
                    $this.filter("[value='" + value + "']").prop("checked", true).trigger("change");
                    return this;
                }
            }
        }

        // execute the original val method otherwise
        return $.fn.val.apply(this, arguments);
    };

    /**
     * Get the value of an element: if it is a kendo widget, get the value of the widget
     * @returns {*}
     */
    $.fn.getval = function () {
        var $this = this;
        if ($this.data("role")) {
            var widget = expresso.util.UIUtil.getKendoWidget($this);
            return widget.value();
        } else {
            return $this.val();
        }
    };

    $.fn.getDataItem = function () {
        var $this = this;
        if ($this.data("role")) {
            var widget = expresso.util.UIUtil.getKendoWidget($this);
            if (widget && widget.dataItem) {
                return widget.dataItem();
            }
        }
        return null;
    };

    /**
     * Utility method to verify is a scroll bar is visible
     * @returns {{vertical: boolean, horizontal: boolean}}
     */
    $.fn.hasScrollBar = function () {
        var e = this.get(0);
        return {
            vertical: e.scrollHeight > e.clientHeight,
            horizontal: e.scrollWidth > e.clientWidth
        };
    };

    /**
     * Get the DOM traversing path
     * @returns {string}
     */
    $.fn.getPath = function () {
        var path = [];
        var $el = this;
        do {
            var nodeName = $el[0].nodeName.toLowerCase();
            if (nodeName == "html") {
                break;
            }
            path.push(nodeName + ($el[0].classList.length ? ".'" + $el[0].classList/*.item(0)*/ + "'" : ""));
            $el = $el.parent();
        } while ($el && $el.length)
        return path.reverse().join(' > ');
    };

    (function ($, sr) {
        // debouncing function from John Hann
        // http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/
        var debounce = function (func, threshold, execAsap) {
            var timeout;

            return function debounced() {
                var obj = this, args = arguments;

                function delayed() {
                    if (!execAsap)
                        func.apply(obj, args);
                    timeout = null;
                }

                if (timeout)
                    clearTimeout(timeout);
                else if (execAsap)
                    func.apply(obj, args);

                timeout = window.setTimeout(delayed, threshold || 100);
            };
        };
        // smartresize
        jQuery.fn[sr] = function (fn) {
            return fn ? this.on('resize.app', debounce(fn)) : this.trigger(sr);
        };

    })(jQuery, 'smartresize');


})();

