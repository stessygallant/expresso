var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.applicationbase = expresso.layout.applicationbase || {};

/**
 * Base expresso.layout. Provide a basic layout: only 1 section
 */
expresso.layout.applicationbase.ApplicationBase = expresso.layout.applicationbase.AbstractApplicationBase.extend({
    // if true, this application must has been loaded by the sub application
    loadedBySubApplication: false,

    // true if this application should be displayed as a master application
    // useful for subresources manager that are being displayed in a master grid
    displayAsMaster: false,

    // array of intervals
    intervals: undefined,

    // flag to indicate if the application is displayed full screen
    fullScreen: undefined,

    // refresh delay (in seconds). Default is 10 minutes
    refreshViewDelay: undefined,


    /**
     * Method called when a new instance is created
     * @param applicationPath path for the application
     */
    init: function (applicationPath) {
        expresso.layout.applicationbase.AbstractApplicationBase.fn.init.call(this, applicationPath);

        this.intervals = [];

        if (!this.refreshViewDelay) {
            this.refreshViewDelay = expresso.Common.isProduction() ? 10 * 60 : 60;
        }
    },

    /**
     * Set the reference to the jquery object for the DOM element.
     *
     * @param $domElement reference to the jquery object for the DOM element
     */
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.AbstractApplicationBase.fn.initDOMElement.call(this, $domElement);

        var _this = this;
        this.$domElement = $domElement;

        // add the instance on the DOM element
        $domElement.data("object-instance", this);

        // resize section on window resize
        $(window).smartresize(function () {
            _this.resizeContent();
        });

        // handle the ESC (it will get out of the fullscreen mode)
        $(document).on("keyup.app", function (e) {
            if (e.keyCode == 27) { // escape key maps to keycode `27`
                if (_this.fullScreen === true) {
                    _this.setFullScreenMode(false);
                }
            }
        });

        // add a button if the flag is defined
        if (this.fullScreen === true) {
            this.fullScreen = false; // by default, it starts not in full screen
            $("<button class='full-screen-button k-button' title='toogleFullScreen'><span class='fa fa-television'></span></button>").appendTo(this.$domElement).on("click", function () {
                _this.setFullScreenMode(!_this.fullScreen);
            });
        }
    },

    /**
     * Refresh the view (at interval in full screen mode).
     * Must be implemented by the subclass
     */
    refreshView: function () {
        alert("Application that supports fullscreen mode must implement this method");
    },

    /**
     *
     * @param fullScreen
     */
    setFullScreenMode: function (fullScreen) {
        var _this = this;

        if (!fullScreen && this.fullScreen) {
            this.fullScreen = false;

            // request full screen only if not in an iframe
            if (window.self == window.top) {
                _this.exitFullScreen();
            }

            // do a full reload
            location.reload();
        } else if (fullScreen && !this.fullScreen) {
            this.fullScreen = true;
            // request full screen only if not in an iframe
            if (window.self == window.top) {
                _this.requestFullScreen();
            }

            // hide sections
            $(".main-header").hide();
            $(".main-menu").hide();
            $(".user-div").hide();
            $(".main-footer").hide();
            $(".main-div .main-title").hide();
            $(".main-div").css("top", 0).css("left", 0).height("100%").width("100%");
            $(".main-content").height("100%").width("100%");

            // make sure never to display an error message
            expresso.Common.doNotDisplayAjaxErrorMessage(true);

            // add a div to display the last update time
            var $lastUpdatedTimeDiv = $("<div class='last-updated-time'>" + this.getLabel("lastUpdatedTime") + "<span></span></div>").appendTo(this.$domElement);
            var $lastUpdatedTimeSpan = $lastUpdatedTimeDiv.find("span");

            // refresh the view every n seconds
            //console.log("Full screen mode");
            this.addInterval(function () {
                //console.log("Refresh view");
                _this.refreshView();

                // add the last updated time
                $lastUpdatedTimeSpan.text(expresso.util.Formatter.formatDate(new Date(), expresso.util.Formatter.DATE_FORMAT.DATE_TIME));

            }, this.refreshViewDelay);
        }
    },

    /**
     * Helper method to update the values array
     * @param va array to push the values
     * @param data array of data
     * @param [field] Field to use for the description (default is "label")
     * @param [allValuesLabel] if not null, defined the description for the all values
     */
    updateValues: function (va, data, field, allValuesLabel) {
        return expresso.Common.updateValues(va, data, field, allValuesLabel, this.labels);
    },

    // @override
    onDomElementInitialized: function () {
        return this.$domElement.kendoExpressoForm({labels: this.labels}).data("kendoExpressoForm").ready();
    },

    /**
     * A utility method to handle the creation and removal of interval when not needed anymore
     *
     * @param fct
     * @param seconds
     * @returns {*} the newly created interval
     */
    addInterval: function (fct, seconds) {
        // execute the fct now
        fct();

        // now set the interval
        if (seconds) {
            var newInterval = window.setInterval(fct, seconds * 1000);
            this.intervals.push(newInterval);
            return newInterval;
        } else {
            alert("Cannot set an interval with 0 seconds");
            return null;
        }
    },

    /**
     * Remove all intervals defined for this application
     */
    clearIntervals: function () {
        for (var i = 0; i < this.intervals.length; i++) {
            try {
                window.clearInterval(this.intervals[i]);
            } catch (e) {
                // ignore
            }
        }
        this.intervals = [];
    },

    /**
     *
     */
    requestFullScreen: function () {
        var el = document.documentElement,
            rfs = el.requestFullscreen
                || el.webkitRequestFullScreen
                || el.mozRequestFullScreen
                || el.msRequestFullscreen;

        try {
            rfs.call(el);
        } catch (e) {
            // Not supported. ignore
        }
    },

    /**
     *
     */
    exitFullScreen: function () {
        var el = document,
            rfs = el.exitFullscreen
                || el.msExitFullscreen
                || el.mozCancelFullScreen
                || el.webkitExitFullscreen;
        try {
            rfs.call(el);
        } catch (e) {
            // Not supported. ignore
        }
    },

    /**
     *
     * @param report
     */
    executeReport: function (report) {
        var params = {
            format: report.format || "pdf"
        };

        // add custom params
        if (report.params) {
            if (typeof report.params === "function") {
                $.extend(params, (report.params)());
            } else {
                $.extend(params, report.params);
            }
        }

        // add labels
        report.label = report.label || this.getLabel("report-" + report.name);
        report.labels = this.labels;

        // add path
        report.path = report.path || (this.applicationPath + "/report-" + report.name + ".html");

        // TODO modify method params expresso.Common.executeReport(report, params, report.type == "custom");
    },

    /**
     * This method is called when the main application is requested to switch to another application
     */
    destroy: function () {

        expresso.layout.applicationbase.AbstractApplicationBase.fn.destroy.call(this);
    }

});
