var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Preview class
 */
expresso.layout.resourcemanager.PreviewTab = expresso.layout.resourcemanager.SectionBase.extend({

    // the name of the tab. Only for debugging purpose
    tabName: undefined,

    // reference to the tab
    $tab: undefined,

    // apply only for sub resource.
    subResourceManager: undefined,

    // instance of the application
    appInstance: undefined,

    // indicate if the tab is the current shown tab
    currentTab: false,

    // skip refresh to the current tab
    skipRefresh: false,

    /**
     * Method called when a new instance is created
     * @param resourceManager  reference to the resource manager
     * @param tabName the name of the tab. Only for debugging purpose
     * @param $tab
     */
    init: function (resourceManager, tabName, $tab) {
        expresso.layout.resourcemanager.SectionBase.fn.init.call(this, resourceManager);

        this.tabName = tabName;
        this.$tab = $tab;
    },

    /**
     * Set the reference to the jquery object for the DOM element
     * @param $domElement reference to the jquery object for the DOM element
     */
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // when a new resource is selected, refresh the tab
        this.subscribeEvent(this.RM_EVENTS.RESOURCE_SELECTED, function (e, resource) {
            _this.validateRefresh(resource);
        });
    },

    /**
     * Method called when this tab is activated (already selected and initialized)
     */
    onTabActivationEvent: function (currentTab) {
        // console.log("Preview tab [" + this.resourceManager.resourceName + "] - onTabActivationEvent");

        this.currentTab = currentTab;

        // if the tab is already loading resources, skip it
        // this could happen when the SELECTED event has just arrived and it is waiting on isReady
        if (this.resourceManager.sections.grid.loadingResources) {
            //console.log("******* Skipping onTabActivationEvent [" + this.resourceManager.resourceName + "]");
        } else {
            this.validateRefresh(this.resourceManager.currentResource);
        }
    },

    /**
     * Validate if we should call the refresh method on the event
     * @param resource
     */
    validateRefresh: function (resource) {
        // refresh only if the preview is visible
        if (!this.resourceManager.sections["preview"].$domElement.is(":visible")) {
            // if the preview is not visible, just ignore it
            //console.log("Preview tab [" + this.resourceManager.resourceName + "] - validateRefresh- PREVIEW NOT VISIBLE");
            return;
        }

        //console.log("Preview tab [" + this.resourceManager.resourceName + "] - validateRefresh");

        var _this = this;
        // this tab will be refreshed only when the tab is currently active or the tab is being shown
        $.when(_this.isReady()).done(function () {
            if (_this.currentTab) {
                // console.log("Refreshing tab [" + _this.tabName + "]", resource);
                _this.refresh(resource);
            }
        });
    },

    /**
     * Refresh the content of the preview tab with the new selected resource.
     * @param resource the resource object
     */
    refresh: function (resource) {
        // console.log("Preview tab [" + this.resourceManager.resourceName + "] - refresh");

        if (this.subResourceManager) {
            // verify if the create is available
            this.subResourceManager.verifyCreationRestrictions();

            this.subResourceManager.sections.grid.loadResources();
        } else if (this.appInstance) {
            // if there is a refresh method, call it
            if (this.appInstance.refresh) {
                this.appInstance.refresh(resource);
            }
        } else {
            // reset the form
            var $form = this.$domElement;
            expresso.util.UIUtil.resetForm($form);
            expresso.util.UIUtil.setFormReadOnly($form, !(resource && resource.id));
        }
    },

    /**
     * Load a sub resource manager and display the grid in the tab
     * @param subResourceManagerDef the sub resource manager as defined in the application.js
     * @param [options] options to be passed to the manager
     * @returns {*} a promise when the resource manager is loaded
     */
    loadSubResourceManager: function (subResourceManagerDef, options) {
        var _this = this;
        var $deferred = $.Deferred();

        // by default, we display all sub resources (not only active)
        options = $.extend({}, {activeOnly: false}, options);

        expresso.Common.loadApplication(subResourceManagerDef, options, null, _this.resourceManager).done(function (subResourceManager) {
            _this.subResourceManager = subResourceManager;

            // when there is a change in subresource (update or create or delete), publish an update on the current resource
            _this.subResourceManager.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_UPDATED, _this.RM_EVENTS.RESOURCE_CREATED,
                _this.RM_EVENTS.RESOURCE_DELETED], function () {
                //console.log("PREVIEWTAB - child has been changed (reloading master and refreshing counts)");
                _this.reloadMasterResource();
            });

            // when there is a change in master resource (update), update the buttons
            _this.resourceManager.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_UPDATED], function () {
                //console.log("PREVIEWTAB - master has changed, refresh buttons");
                // _this.resourceManager.verifyActionsRestrictions();
                // _this.resourceManager.sections.grid.enableButtons();
            });

            _this.subResourceManager.render().done(function () {
                // we have to move the grid
                _this.subResourceManager.$domElement.find(".exp-container-grid").children().appendTo(
                    _this.$domElement.children("div").first());
                $deferred.resolve(_this.subResourceManager);
            });
        });

        // wait until the resource manager is loaded
        this.addPromise($deferred);
        return $deferred;
    },

    /**
     * Load a resource manager and display the grid in the tab.
     * The resource loaded is NOT a sub resource of the master resource.
     * Usually, a permanent filter will be used (options={filter: {}}) to filter the sub grid
     *
     * @param resourceManagerDef the resource manager as defined in the application.js
     * @param [options] options to be passed to the manager
     * resource first
     * @returns {*} a promise when the resource manager is loaded
     */
    loadSiblingResourceManager: function (resourceManagerDef, options) {
        var _this = this;
        var $deferred = $.Deferred();

        // by default, we display all sibling resources (not only active)
        options = $.extend({}, {activeOnly: false}, options);

        expresso.Common.loadApplication(resourceManagerDef, options, null, null,
            _this.resourceManager).done(function (resourceManager) {
            _this.subResourceManager = resourceManager;

            // when there is a change in sibling (update or create or delete), publish an update on the current resource
            _this.subResourceManager.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_UPDATED, _this.RM_EVENTS.RESOURCE_CREATED,
                _this.RM_EVENTS.RESOURCE_DELETED], function () {
                //console.log("PREVIEWTAB - child has been changed (reloading master and refreshing counts)");
                _this.reloadMasterResource();
            });

            // if there is no filter defined, add the sibling default filter
            // if no filter is defined, a masterIdProperty must be defined
            if (!_this.subResourceManager.options.filter) {
                // add a filter on the sibling resource name
                // if there is no sibling resource selected, do not select them (-1)
                _this.subResourceManager.options.filter = function () {
                    if (_this.subResourceManager.model.masterIdProperty) {
                        return {
                            field: _this.subResourceManager.model.masterIdProperty,
                            operator: "eq",
                            value: _this.resourceManager.currentResource && _this.resourceManager.currentResource.id ?
                                _this.resourceManager.currentResource.id : -1
                        }
                    } else {
                        return null;
                    }
                };
            }

            // when there is a change in sibling resource (create or delete), publish an update on the current resource
            _this.subResourceManager.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_CREATED,
                _this.RM_EVENTS.RESOURCE_DELETED], function () {
                // refresh the count
                _this.resourceManager.sections.preview.refreshCount(_this.$tab);
            });

            _this.subResourceManager.render().done(function () {
                // we have to move the grid
                _this.subResourceManager.$domElement.find(".exp-container-grid").children().appendTo(
                    _this.$domElement.children("div").first());
                $deferred.resolve(_this.subResourceManager);
            });
        });

        // wait until the resource manager is loaded
        this.addPromise($deferred);
        return $deferred;
    },

    /**
     * Load a standalone application in a tab.
     * The application shall implement a method: refresh: function(resource)
     *
     * @param appDef
     * @param options
     */
    loadApplication: function (appDef, options) {
        var _this = this;
        var $div = this.$domElement.children("div").first();
        this.addPromise(expresso.Common.loadApplication(appDef, options, $div).done(function (appInstance) {
            _this.appInstance = appInstance;
            _this.appInstance.render();
        }));
    },

    /**
     * Publish an event. Shortcut to the resourceManager.eventCentral.publishEvent
     * @param e event type. Refer to RM_EVENTS
     * @param data data for the event
     */
    publishEvent: function (e, data) {
        // do not refresh the current tab
        this.skipRefresh = true;

        // publish the event
        expresso.layout.resourcemanager.SectionBase.fn.publishEvent.call(this, e, data);
    },

    // @override
    resizeContent: function () {
        expresso.layout.resourcemanager.SectionBase.fn.resizeContent.call(this);

        if (this.subResourceManager) {
            this.subResourceManager.resizeContent();
        } else if (this.appInstance) {
            this.appInstance.resizeContent();
        }
    },

    /**
     *  reload the master resource
     */
    reloadMasterResource: function () {
        this.resourceManager.sections.grid.reloadCurrentResource();
    },

    // @override
    destroy: function () {
        this.tabName = null;
        this.$tab = null;
        this.currentTab = null;

        if (this.subResourceManager) {
            this.subResourceManager.destroy();
            this.subResourceManager = null;
        }

        if (this.appInstance) {
            this.appInstance.destroy();
            this.appInstance = null;
        }

        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    }
});
