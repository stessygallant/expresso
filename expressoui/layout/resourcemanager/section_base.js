var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Base class for any sub section of the resource manager
 */
expresso.layout.resourcemanager.SectionBase = kendo.Class.extend({
    // a reference to the jquery object for the DOM element
    $domElement: undefined,

    // reference to the resource manager
    resourceManager: undefined,

    RM_EVENTS: undefined,

    // promises to be verify before calling a method on the section
    readyPromises: undefined,

    /**
     * Method called when a new instance is created
     * @param resourceManager  reference to the resource manager
     */
    init: function (resourceManager) {
        this.resourceManager = resourceManager;
        this.readyPromises = [];

        // add a shortcut for events definition
        this.RM_EVENTS = resourceManager.RM_EVENTS;
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

        // resize if slipper is resize
        var kendoSplitter = $(".exp-splitter-preview").data("kendoSplitter");
        if (kendoSplitter) {
            kendoSplitter.bind("resize", function () {
                _this.resizeContent();
            });
        }

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
            var $element = this.$window || this.$domElement;
            expresso.util.UIUtil.showLoadingMask($element, true, "waitSectionReady");
            return $.when.apply(null, this.readyPromises).always(function () {
                expresso.util.UIUtil.showLoadingMask($element, false, "waitSectionReady");
            });
        }
    },

    /**
     * Resize the section if needed
     */
    resizeContent: function () {
        // console.log("CALLING resizeContent - " + this.resourceManager.resourceName, this);

        // by default, do nothing
    },

    /**
     * Subscribe to an event. Shortcut to the resourceManager.eventCentral.subscribeEvent
     *
     * @param e event type. Refer to RM_EVENTS
     * @param eh event handler
     * @param [onceOnly]
     */
    subscribeEvent: function (e, eh, onceOnly) {
        this.resourceManager.eventCentral.subscribeEvent(e, eh, onceOnly);
    },

    /**
     * Publish an event. Shortcut to the resourceManager.eventCentral.publishEvent
     * @param e event type. Refer to RM_EVENTS
     * @param data data for the event
     */
    publishEvent: function (e, data) {
        this.resourceManager.eventCentral.publishEvent(e, data);
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
     * Destroy the section
     */
    destroy: function () {
        expresso.util.UIUtil.destroyKendoWidgets(this.$domElement);

        this.$domElement.empty();
        this.$domElement = null;
        this.resourceManager = null;
        this.RM_EVENTS = null;
        this.readyPromises = null;
    }
});
