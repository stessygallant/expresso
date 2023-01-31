var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.applicationbase = expresso.layout.applicationbase || {};

/**
 * Base expresso.layout. Provide a basic layout: only 1 section
 */
expresso.layout.applicationbase.AbstractApplicationBase = kendo.Class.extend({

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

    // application preferences for the user
    applicationPreferences: undefined,

    /**
     * Method called when a new instance is created
     * @param applicationPath path for the application
     */
    init: function (applicationPath) {
        //console.log("ApplicationBase initialized");

        this.applicationPath = applicationPath;

        // init default options
        this.options = {queryParameters: {}};
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

        // resize the section at the end of this call
        window.setTimeout(function () {
            if (_this.$domElement) {
                _this.resizeContent();
            }
        }, 100);
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
            // initialized the promise
            return this.render();
        } else {
            return this.$readyPromise;
        }
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
     * This method is called when the main application is requested to switch to another application
     */
    destroy: function () {
        this.clearIntervals();

        // remove all event handlers
        $(window).off(".app");
        $(window.document).off(".app");

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
