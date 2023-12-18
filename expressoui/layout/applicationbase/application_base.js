var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.applicationbase = expresso.layout.applicationbase || {};

/**
 * Base expresso.layout. Provide a basic layout: only 1 section
 */
expresso.layout.applicationbase.ApplicationBase = kendo.Class.extend({
    // Application definition
    appDef: undefined,

    // relative path of the application from the index.html
    applicationPath: undefined,

    // div where to put the $domElement
    $containerDiv: undefined,

    // a reference to the jquery object for the DOM element
    $domElement: undefined,

    // user defined option for the application
    options: undefined,

    // labels for the application
    labels: undefined,

    // promise when the application is ready
    $readyPromise: undefined,

    // promises to be verified before calling a method on the section
    readyPromises: undefined,

    // application preferences for the user
    applicationPreferences: undefined,

    // if true, this application must have been loaded by the sub application
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

    // web socket to listen to update
    webSocket: undefined,
    webSocketReconnectionDelay: undefined, // n seconds


    /**
     * Method called when a new instance is created
     * @param applicationPath path for the application
     */
    init: function (applicationPath) {
        //console.log("ApplicationBase initialized");

        this.applicationPath = applicationPath;

        // init default options
        this.options = {queryParameters: {}};

        this.readyPromises = [];

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
        var _this = this;
        this.$domElement = $domElement;

        // add the instance on the DOM element
        $domElement.data("object-instance", this);

        // resize the application at the end of this call
        window.setTimeout(function () {
            if (_this.$domElement) {
                _this.resizeContent();
            }
        }, 100);

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

        console.log("Application [" + this.appDef.absoluteAppName + "] initialized");
    },

    /**
     * Give a chance to the application to load data before loading sections
     * @returns {*} a promise when the data is loaded
     */
    initData: function () {
        //return $.Deferred().resolve().promise();
        // load the labels
        var _this = this;

        return $.when(
            // load application preferences
            _this.loadApplicationPreferences(),

            // load applications labels
            expresso.Common.loadLabels(this.applicationPath).done(function (applicationsLabels) {
                // always append the common labels
                _this.labels = $.extend({}, expresso.Labels, applicationsLabels);
            })
        );
    },

    /**
     * Resize the content if needed
     */
    resizeContent: function () {
        // by default, resize all kendo widgets in the DOM element
        // kendo.resize($domElement);
    },

    /**
     * This method add a new promise to the section.
     * All promises must be resolved before using the section (see isReady)
     * @param promise
     */
    addPromise: function (promise) {
        this.readyPromises.push(promise);
    },

    /**
     * Load the content: app.html and append it to the layout
     * @param autoLoad true if you want to load/init the content of the application (default is false)
     */
    render: function (autoLoad) {
        var _this = this;
        var $containerDiv = this.$containerDiv || $("<div class='hidden' hidden></div>").appendTo($("body"));
        var $domElement = $("<div class='exp-application-base'></div>");
        $domElement.appendTo($containerDiv);

        // load the HTML page
        this.$readyPromise = $.Deferred();
        expresso.Common.loadHTML($domElement, _this.applicationPath + "/app.html", null, false).done(function () {
            // application could have been destroyed while waiting for HTML
            if (_this.applicationPath) {
                // console.log("app.html loaded successfully");
                _this.initDOMElement($domElement);

                // then customize the form if needed
                _this.$domElement.kendoExpressoForm({labels: this.labels}).data("kendoExpressoForm").ready().done(function () {
                    // localize the application
                    expresso.Common.localizePage(_this.$domElement, _this.labels);

                    _this.$readyPromise.resolve();
                });
            }
        });

        return this.$readyPromise;
    },

    /**
     * Get the label for the key.
     * @param key
     * @param [params]
     * @param [nullIfNoExist]
     * @param [shortLabel] default is false
     * @return {string} the label
     */
    getLabel: function (key, params, nullIfNoExist, shortLabel) {
        return expresso.Common.getLabel(key, this.labels, params, nullIfNoExist, shortLabel);
    },

    /**
     * Send a REST request to the server. But first, verify that the user is allowed to send this request
     * @param [path] resource path
     * @param [action] action on the resource (CRUD or custom)
     * @param [data] data to be sent in the body
     * @param [queryString]
     * @param [options]
     * @returns {*} a Ajax promise
     */
    sendRequest: function (path, action, data, queryString, options) {
        // add the application labels and send the request
        return expresso.Common.sendRequest(path, action, data, queryString, $.extend({}, {
            waitOnElement: this.$domElement,
            labels: this.labels
        }, options));
    },

    /**
     *
     */
    setOptions: function (options) {
        this.options = options || {};
        this.options.queryParameters = this.options.queryParameters || {};
        // console.log("queryParameters", this.options.queryParameters);
    },

    /**
     * Always validate that the section is ready before calling any method on it
     * @returns a Promise to be resolved when the section is ready
     */
    isReady: function () {
        if (!this.$readyPromise) {
            this.readyPromises.push(this.render());
        } else {
            this.readyPromises.push(this.$readyPromise);
        }

        return $.when.apply(null, this.readyPromises);
    },

    /**
     * Load the application preferences
     * @return {*} promise when the request is done
     */
    loadApplicationPreferences: function () {
        var _this = this;
        return expresso.Common.loadApplicationPreferences(this.appDef.appClass).done(function (applicationPreferences) {
            _this.applicationPreferences = applicationPreferences;
        });
    },

    /**
     * Get the application preferences
     * @return {*}
     */
    getApplicationPreferences: function () {
        return this.applicationPreferences.preferences;
    },

    /**
     * Save the application preferences
     * @return {*} promise when the request is done
     */
    saveApplicationPreferences: function () {
        var _this = this;
        return expresso.Common.saveApplicationPreferences(this.applicationPreferences).done(function (updatedApplicationPreferences) {
            _this.applicationPreferences = updatedApplicationPreferences;
        });
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
            this.clearIntervals();
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

    /**
     * A utility method to handle the creation and removal of interval when not needed anymore
     *
     * @param fct
     * @param seconds
     * @param [executeNow] default is true
     * @returns {*} the newly created interval
     */
    addInterval: function (fct, seconds, executeNow) {
        if (executeNow !== false) {
            // execute the fct now
            fct();
        }

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

        // to do: modify method params expresso.Common.executeReport(report, params, report.type == "custom");
    },

    /**
     *
     * @param resourceSecurityPath
     */
    connectWebSocket: function (resourceSecurityPath) {
        var _this = this;
        if (!this.webSocketReconnectionDelay) {
            this.webSocketReconnectionDelay = 20; // n seconds
        }

        try {

            /**
             * Utility method to keep the web socket opened
             */
            function webSocketKeepAlive() {
                try {
                    // console.log("webSocketKeepAlive at" + new Date());
                    _this.webSocket.send(JSON.stringify({message: "keepAlive"}));
                } catch (ex1) {
                    // this will trigger a reconnect
                    _this.webSocket.close();
                }
                window.setTimeout(function () {
                    webSocketKeepAlive();
                }, 30 * 1000);
            }

            var path = expresso.Common.getWsBasePathURL();
            path = path.substring("http".length); // keep the "s" is any
            this.webSocket = new WebSocket("ws" + path + "/websocket/" + resourceSecurityPath);
            this.webSocket.onerror = function (event) {
                console.warn("Error on web socket at" + new Date(), event);
            };

            this.webSocket.onopen = function (event) {
                console.log("Web socket connection established at " + new Date());
                webSocketKeepAlive();
            };

            this.webSocket.onmessage = function (event) {
                var data = (event && event.data ? JSON.parse(event.data) : null);
                _this.listenWebSocket(data);
            }

            this.webSocket.onclose = function () {
                if (_this.webSocketReconnectionDelay) {
                    console.warn("Web socket closed at " + new Date() + ". Reconnecting web socket in " + _this.webSocketReconnectionDelay + " seconds");
                    window.setTimeout(function () {
                        _this.connectWebSocket(resourceSecurityPath);
                    }, _this.webSocketReconnectionDelay * 1000);
                } else {
                    console.log("Web socket closed." + new Date());
                }
            }


        } catch (ex) {
            console.error("Cannot establish web socket connection: " + ex);
        }
    },

    /**
     *
     */
    closeWebSocket: function () {
        if (this.webSocket) {
            try {
                this.webSocketReconnectionDelay = null;
                this.webSocket.close();
            } catch (ex) {
                // ignore
            }
            this.webSocket = null;
        }
    },

    /**
     *
     * @param data
     */
    listenWebSocket: function (data) {
        // by default, it calls refresh view
        this.refreshView();
    },

    /**
     * This method is called when the main application is requested to switch to another application
     */
    destroy: function () {
        this.clearIntervals();

        // remove all event handlers
        $(window).off(".app");
        $(window.document).off(".app");

        // Web socket
        this.closeWebSocket();

        this.labels = null;
        this.applicationPath = null;

        if (this.$domElement) {
            // remove the data
            this.$domElement.data("object-instance", null);

            expresso.util.UIUtil.destroyKendoWidgets(this.$domElement);

            // remove HTML DOM elements
            this.$domElement.empty();

            this.$domElement = null;
        }
    }
});
