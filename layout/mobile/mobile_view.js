var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.mobile = expresso.layout.mobile || {};

/**
 * Base class for any mobile view
 */
expresso.layout.mobile.MobileView = kendo.Class.extend({
    // a reference to the jquery object for the DOM element
    $domElement: undefined,

    // reference to the resource manager
    resourceManager: undefined,

    // promises to be verify before calling a method on the view
    readyPromises: undefined,

    /**
     * Method called when a new instance is created
     * @param resourceManager  reference to the resource manager
     */
    init: function (resourceManager) {
        this.resourceManager = resourceManager;
        this.readyPromises = [];
    },

    /**
     * Set the reference to the jquery object for the DOM element.
     *
     * @param $domElement reference to the jquery object for the DOM element
     */
    initDOMElement: function ($domElement, model) {
        var _this = this;
        this.$domElement = $domElement;

        // add the instance on the DOM element
        $domElement.data("object-instance", this);

        // resize the section at the end of this call
        setTimeout(function () {
            _this.resizeContent();
        }, 100);
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
     * Always validate that the section is ready before calling any method on it
     * @returns a Promise to be resolved when the section is ready
     */
    isReady: function () {
        if (this.readyPromises.length == 0) {
            // return a already resolved promise
            return $.Deferred().resolve().promise();
        } else {
            return $.when.apply(null, this.readyPromises);
        }
    },

    /**
     * Resize the section if needed
     */
    resizeContent: function () {
        //console.log("CALLING resizeContent - " + this.resourceManager.resourceName, this);

        // by default, do nothing
    },

    /**
     * Get the label for the key. Shortcut to the resourceManager.getLabel
     * @param key
     * @param [params]
     * @param [nullIfNoExist]
     * @param [shortLabel] default is false
     * @return {string} the label
     */
    getLabel: function (key, params, nullIfNoExist, shortLabel) {
        return this.resourceManager.getLabel(key, params, nullIfNoExist, shortLabel);
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
        return this.resourceManager.sendRequest(path, action, data, queryString, $.extend({}, {waitOnElement: this.$domElement}, options));
    },

    /**
     * Get the application preferences from the Resource Manager
     * @return {*}
     */
    getApplicationPreferences: function () {
        return this.resourceManager.getApplicationPreferences();
    },

    /**
     * Save the application preferences
     * @return {*}
     */
    saveApplicationPreferences: function () {
        return this.resourceManager.saveApplicationPreferences();
    },

    /**
     * Verify is the user is allowed to perform the action on the resource
     * @param action
     * @returns {boolean}
     */
    isUserAllowed: function (action) {
        return this.resourceManager.isUserAllowed(action);
    },

    /**
     * Destroy the view
     */
    destroy: function () {
        expresso.util.UIUtil.destroyKendoWidgets(this.$domElement);

        this.$domElement.empty();
        this.$domElement = null;
        this.resourceManager = null;
        this.readyPromises = null;
    },

    getModel: function () {
        // by default, return nothing
        return null;
    }
});
