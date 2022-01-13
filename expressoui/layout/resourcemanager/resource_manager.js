var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Layout defined to be used by resources manager. Provide a layout with 3
 * sections
 */
expresso.layout.resourcemanager.ResourceManager = expresso.layout.applicationbase.ApplicationBase.extend({

    // this is a unique ID to used to identified the HTML element if needed
    resourceManagerId: undefined,

    // use to configure Kendo UI model
    model: undefined,

    // name of the resource (camelCase)
    // ex: activityLogRequest, activityLogRequestChange
    resourceName: undefined,

    // path of the resource (usually the same as the name)
    // for sub resource, resourcePath is the relative path to the master resource
    // ex: change
    // user should use getRelativeWebServicePath(id) ex: /activityLogRequest/0/change
    resourcePath: undefined,

    // field that contains the unique number (id, no, etc) for the resource
    resourceFieldNo: undefined,

    // section objects
    sections: undefined,

    // current selected resource
    currentResource: undefined,

    // only used by sub resource to keep a reference to the master resource manager
    masterResourceManager: undefined,

    // reference to the sibling resource manager when used as sibling resource
    siblingResourceManager: undefined,

    // event central instance
    eventCentral: undefined,

    // array of available actions for the user
    availableActions: undefined,
    createAction: undefined,

    // available actions with restrictions for the current resource (promise)
    availableActionsPromise: undefined,
    createActionPromise: undefined,

    // if defined, use this height for the preview panel
    previewHeightRatio: undefined,

    // list of events
    RM_EVENTS: {
        RESOURCE_UPDATED: "resource-updated-event",
        RESOURCE_CREATED: "resource-created-event",
        RESOURCE_DELETED: "resource-deleted-event",
        RESOURCE_SELECTED: "resource-selected-event"
    },

    // responsive design attributes
    resizing: undefined,
    autoCollapsedPreview: undefined,

    /**
     * Method called when a new instance is created
     * @param applicationPath path for the application
     * @param resourcePath path of the resource
     * @param model KendoUI model of the resource (complete model or only fields)
     * @param sections (optional) define the list of sections for this manager
     */
    init: function (applicationPath, resourcePath, model, sections) {
        expresso.layout.applicationbase.ApplicationBase.fn.init.call(this, applicationPath);

        this.resourceManagerId = expresso.util.Util.guid();

        // if the user does not define the previewHeightRatio, use a default value
        if (!this.previewHeightRatio) {
            if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE) {
                this.previewHeightRatio = 0.3;
            } else {
                this.previewHeightRatio = 0.4;
            }
        }

        // initialize properties
        this.resourcePath = resourcePath;
        this.model = model;

        // for backward compatibility, if the model is defined and contains the type, use it
        this.resourceName = (model && model.fields && model.fields.type && model.fields.type.defaultValue ? model.fields.type.defaultValue :
            (model && model.type && model.type.defaultValue ? model.type.defaultValue : resourcePath));
        if (!this.resourceFieldNo) {
            this.resourceFieldNo = this.resourceName + "No";
        }

        // create the EC for the resource
        this.eventCentral = new expresso.util.EventCentral(this.resourceName);

        // initialized the sections container
        this.sections = sections || {
            grid: true,
            form: true,
            preview: true,
            filter: false // expresso.Common.getSiteNamespace().config.Configurations.supportFilter
        };

        console.log("ResourceManager [" + this.resourceName + "] initialized");
    },

    /**
     * Set the master resource manager (only used for sub resource)
     * @param masterResourceManager
     */
    setMasterResourceManager: function (masterResourceManager) {
        this.masterResourceManager = masterResourceManager;
    },

    /**
     * Set the sibling resource manager (only used for master resource)
     * @param siblingResourceManager
     */
    setSiblingResourceManager: function (siblingResourceManager) {
        this.siblingResourceManager = siblingResourceManager;
    },

    /**
     * Set the master property Id in the model to filter for the master resource
     * @param masterResourceManager
     */
    setMasterIdProperty: function (masterResourceManager) {
        if (masterResourceManager) {
            if (!this.model.masterIdProperty) {
                if (this.options.masterIdProperty) {
                    this.model.masterIdProperty = this.options.masterIdProperty;
                } else {
                    // if this is a subresource, guess the masterIdProperty
                    var masterIdProperty = masterResourceManager.resourceName + "Id";

                    // make sure the field is defined in the appClass
                    if (this.model.fields[masterIdProperty]) {
                        // console.log(this.resourceName + " - Setting the master property to [" + masterIdProperty + "]");
                        this.model.masterIdProperty = masterIdProperty;
                    }
                }
            }
        }
    },

    /**
     * Get the complete Web Service REST point URL for the resource
     * @param [id]
     * @returns {string}
     */
    getWebServicePath: function (id) {
        return expresso.Common.getWsResourcePathURL() + "/" + this.getRelativeWebServicePath(id);
    },

    /**
     * Get the resource security name (ex: activityLogRequest/change)
     * @returns {string}
     */
    getResourceSecurityPath: function () {
        var resourceSecurityPath = this.resourcePath;
        if (this.masterResourceManager) {
            resourceSecurityPath = this.masterResourceManager.getResourceSecurityPath() + "/" + resourceSecurityPath;
        }
        return resourceSecurityPath;
    },

    /**
     * Get the resource name (ex: activityLogRequestChange)
     * @returns {string}
     */
    getResourceName: function () {
        return this.resourceName;
    },

    /**
     * Get the relative Web Service REST point URL for the resource
     * @param [id]
     * @returns {string}
     */
    getRelativeWebServicePath: function (id) {
        var wsPath = "";

        if (this.masterResourceManager) {
            var masterId;

            if (this.masterResourceManager.getCurrentResourceId() !== null) {
                masterId = this.masterResourceManager.getCurrentResourceId();
                //console.log("Master current resource masterId [" + masterId + "]");
            } else {
                if (this.masterResourceManager.loadedBySubApplication) {
                    masterId = 0;
                } else {
                    // this happen when the user unselect the only selected line or if he selects more than one
                    //console.warn("ERROR: No master resource [" + this.masterResourceManager.resourceName + "] selected");
                    masterId = -1;
                }
            }
            wsPath = this.masterResourceManager.getRelativeWebServicePath(masterId) + "/";
        }
        wsPath += this.resourcePath + (id !== undefined && id !== null ? "/" + id : "");

        //console.log("wsPath: [" + wsPath + "]");
        return wsPath;
    },

    /**
     *
     */
    getUploadDocumentPath: function (id) {
        return expresso.Common.getWsUploadPathURL() + "/" +
            this.getRelativeWebServicePath(id) + "?creationUserName=" + expresso.Common.getUserInfo().userName;
    },

    /**
     * Load all sections in the layout and initialize them
     * @return {*} a jQuery Promise (resolved when all sections loaded)
     */
    loadSections: function () {
        // load each html into their section
        var promises = [];

        for (var section in this.sections) {
            if (this.sections[section] === true && this.sections.hasOwnProperty(section)) {
                // if (section === "preview" && !this.$domElement.is(":visible")) {
                //     // do not load preview if not visible
                // } else {
                promises.push(this.loadSection(this.$domElement.find(".exp-container-" + section), section));
//                }
            }
        }

        return $.when.apply(null, promises).done(function () {
            //console.log("All sections loaded");
        });
    },

    /**
     * Load the section into the div in parameter (if not already loaded)
     * @param $div where to load the HTML
     * @param sectionName name of the section to load
     * @param customOptions
     * @return {*} a jQuery Promise (resolved when all loaded and initialized)
     */
    loadSection: function ($div, sectionName, customOptions) {
        var _this = this;
        if (this.sections[sectionName] === true) {
            var sectionFilePath = this.applicationPath + "/" + sectionName + ".html";
            // console.log("Loading section [" + sectionFilePath + "]");
            var $deferred = $.Deferred();
            if ($div && $div.length) {
                // ok, use this DIV
            } else {
                // if not, add a new div to the domElement of the manager
                $div = $("<div data-resource-manager-id='" + this.resourceManagerId + "' class='hidden'></div>")
                    .appendTo("body" /*this.$domElement*/);
            }

            expresso.Common.loadHTML($div, sectionFilePath, this.labels, !sectionName.startsWith("form")).done(function () {
                //console.log("Section [" + sectionName + "] loaded");

                // application could have been destroyed while waiting for HTML
                if (!_this.sections) {
                    return;
                }

                // then get the script
                var sectionScriptPath = _this.applicationPath + "/" + sectionName + ".js";
                expresso.Common.getScript(sectionScriptPath).done(function () {

                    //console.log("Script [" + sectionName + "] loaded");
                    var $domElement = $div.find(".exp-" + sectionName);

                    // define the section class based on the applicationPath
                    var sectionClass = _this.applicationPath.replace(/\//g, '.') + "." + sectionName.capitalize();
                    if (!sectionClass.startsWith("expresso")) {
                        // add the name of the site
                        sectionClass = expresso.Common.getSiteName() + "." + sectionClass;
                    }
                    var sectionObject = eval("new " + sectionClass + "(_this)");

                    sectionObject.initDOMElement($domElement, customOptions);
                    _this.sections[sectionName] = sectionObject;

                    $.when(
                        _this.sections[sectionName].isReady()
                    ).done(function () {
                        //console.log("Section [" + sectionClass + "] instantiated");
                        $deferred.resolve();
                    });
                });
            });
            return $deferred;
        } else {
            return this.sections[sectionName].isReady();
        }
    },

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.ApplicationBase.fn.initDOMElement.call(this, $domElement);
        var _this = this;

        if (expresso.Common.getSiteNamespace().config.Configurations.supportFilter) {
            this.$domElement.find(".exp-splitter-filter").kendoSplitter({
                panes: [{
                    collapsible: true,
                    resizable: false,
                    size: "15%",
                    collapsed: true
                }, {
                    collapsible: false
                }],
                resize: function () {
                    _this.resizeContent();
                }
            });
        }

        if (this.sections && this.sections["preview"]) {

            // the DOM element containing the splitter must define a height (100% is not correct)
            var totalHeight = this.$domElement.height();
            //console.log("totalHeight: " + totalHeight);

            // the splitter div must have a defined height
            var $previewSplitter = this.$domElement.find(".exp-splitter-preview");
            //console.log("$previewSplitter.height: " + $previewSplitter.height());

            // give the initial height to the preview
            var h = Math.round(totalHeight * this.previewHeightRatio);
            //console.log("Preview height: " + h);

            $previewSplitter.kendoSplitter({
                orientation: "vertical",
                panes: [{
                    collapsible: false
                }, {
                    collapsible: true,
                    size: h + "px"
                }],
                collapse: function () {
                    _this.autoCollapsedPreview = false; // this is called when the user do it
                },
                expand: function () {
                    _this.autoCollapsedPreview = false; // this is called when the user do it
                },
                resize: function () {
                    _this.resizeContent();
                }
            });
        }

        // subscribe to the selected (and updated) event to always know which one is current
        //console.log("******** RM subscribe [" + _this.resourceName + "]");
        _this.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_UPDATED], function (/*e, resource*/) {
            //console.log(_this.getResourceSecurityPath() + " - RM RESOURCE_UPDATED");
            // get the actions restrictions for this resource
            _this.verifyActionsRestrictions();
        });
        _this.eventCentral.subscribeEvent([_this.RM_EVENTS.RESOURCE_SELECTED], function (e, resource) {
            if ((_this.currentResource == null && resource != null) ||
                (_this.currentResource != null && resource == null) ||
                ((_this.currentResource != null && resource != null) && (_this.currentResource.id != resource.id))) {
                //console.log(_this.getResourceSecurityPath() + " - RM RESOURCE_SELECTED: " + (resource ? resource.id : null));
                _this.currentResource = resource;

                // get the actions restrictions for this resource
                _this.verifyActionsRestrictions();

                // get the creation restrictions for this resource
                _this.verifyCreationRestrictions();
            } else if (resource == null) {
                // get the creation restrictions for the grid
                _this.verifyCreationRestrictions();
            }
        });

        // parse the available actions
        this.parseAvailableActions();
    },

    // @override
    initData: function () {
        var _this = this;
        var $deferred = $.Deferred();

        $.when(
            // default initData
            expresso.layout.applicationbase.ApplicationBase.fn.initData.call(this)
        ).done(function () {
            // include the labels from the master resourcemanager if defined
            if (_this.masterResourceManager) {
                _this.labels = $.extend({}, _this.masterResourceManager.labels, _this.labels);
            }

            _this.initialized = true;

            // load the the model
            expresso.util.Model.initModel(_this).done(function () {

                if (_this.siblingResourceManager) {
                    _this.model.masterIdProperty = null; // remove the masterIdProperty set by the masterResourceManager
                    _this.setMasterIdProperty(_this.siblingResourceManager);
                } else if (_this.masterResourceManager) {
                    _this.setMasterIdProperty(_this.masterResourceManager);
                }

                $deferred.resolve();
            });
        });

        return $deferred;
    },

    /**
     * Load the contents: grid, preview, form
     * @param autoLoad true if you want to load/init the content of the application (default is false)
     */
    render: function (autoLoad) {
        var _this = this;

        var $containerDiv = this.$containerDiv || $("<div data-resource-manager-id='" + this.resourceManagerId + "' class='hidden'></div>")
            .appendTo("body");

        // add the splitter to the grid element if there is a preview
        var gridElement;
        if (this.sections["preview"]) {
            gridElement = "<div class='exp-splitter-preview'><div class='exp-container-grid'></div><div class='exp-container-preview'></div></div>"
        } else {
            gridElement = "<div class='exp-container-grid'></div>";
        }

        // add a splitter to the app element if there is a filter
        var appElement;
        if (this.sections["filter"]) {
            appElement = "<div class='exp-splitter-filter'><div class='exp-container-filter'></div>" + gridElement + "</div>";
        } else {
            appElement = gridElement;
        }

        // build the DOM element
        var $domElement = $("<div class='exp-resource-manager " + this.resourceName + "Manager'>" + appElement + "<div class='exp-container-form'></div></div>");

        // append the layout (must be attached to the browser DOM before enhancement the layout)
        $domElement.appendTo($containerDiv);

        this.$readyPromise = $.Deferred();

        // load the data (if any)
        this.initData().done(function () {
            _this.initDOMElement($domElement);

            // now load each sections in the layout
            $.when(_this.loadSections()).done(function () {
                _this.sections.grid.initGrid();
                if (autoLoad) {
                    var query;

                    // if there is an initial filter, use it
                    if (_this.options.initialFilter) {
                        query = {filter: _this.options.initialFilter};
                    } else if (_this.options.query) {
                        query = _this.options.query;
                    } else {
                        // verify if there is a favorite grid preferences to be loaded
                        var gridPreference = _this.getApplicationPreferences().favoriteGridPreference ?
                            _this.getApplicationPreferences().gridPreferences[_this.getApplicationPreferences().favoriteGridPreference] :
                            null;
                        if (gridPreference) {
                            query = gridPreference.query;
                        }
                    }

                    // load the grid
                    _this.sections.grid.isReady().done(function () {
                        // if there is a query, overwrite the current default grid filter
                        _this.sections.grid.loadResources(query, _this.options.autoEdit, !!query).done(function () {
                            _this.$readyPromise.resolve();
                        });
                    });
                } else {
                    _this.$readyPromise.resolve();
                }
            });
        });

        return this.$readyPromise;
    },

    /**
     * Display the list of resources (in a grid usually).
     * @param $div where to load the HTML code
     * @param [query] query for the grid
     * @param [autoEdit] true if loadResource is called with autoEdit
     * @returns {*} a promise when the grid is loaded
     */
    list: function ($div, query, autoEdit) {
        var _this = this;
        var $deferred = $.Deferred();
        this.isReady().done(function () {
            if ($div) {
                _this.$domElement.find(".exp-container-grid").children().appendTo($div);
            }
            _this.sections.grid.loadResources(query, autoEdit, !!query || autoEdit).done(function () {
                $deferred.resolve();
            });
        });
        return $deferred;
    },

    /**
     * Open the form in a dialog window.
     * @param [resource] the resource object to be modify. If null, new resource
     * @param [onFormOpen] callback when the form is opened
     * @returns {*}  a promise when the form is closed
     */
    displayForm: function (resource, onFormOpen) {
        var _this = this;
        var $deferred = $.Deferred();
        this.isReady().done(function () {
            if (resource && !resource.id) {

                // this is not a resource from the grid (this is a new resource)
                // we need to overwrite the grid.initializeNewResource method
                // to initialize the Grid resource with the value from this resource
                // Note: we cannot use "resource" in the initializeNewResource because
                // it would use the first resource on the first call (closure)
                _this.sections.grid.resourceForInitializeNewResource = resource;
                if (_this.sections.grid.initializeNewResource && !_this.sections.grid.initializeNewResourceOriginal) {
                    _this.sections.grid.initializeNewResourceOriginal = _this.sections.grid.initializeNewResource;
                    _this.sections.grid.initializeNewResource = function (newResource) {

                        // call the original method
                        _this.sections.grid.initializeNewResourceOriginal(newResource);

                        // copy all defined attributes to the new resource
                        //$.extend(newResource, _this.sections.grid.resourceForInitializeNewResource);
                        for (var p in _this.sections.grid.resourceForInitializeNewResource) {
                            newResource.set(p, _this.sections.grid.resourceForInitializeNewResource[p]);
                        }

                        _this.sections.grid.resourceForInitializeNewResource = null;
                        // console.log("newResource", newResource);
                    }
                }
            }

            //console.log("resource id:" + (resource && resource.id ? resource.id : -1));
            var query = {
                filter: {field: "id", operator: "eq", value: (resource && resource.id ? resource.id : -1)}
            };
            _this.list(null, query, true).done(function () {
                _this.sections.form.bindOnClose().done(function (resource) {
                    $deferred.resolve(resource);
                });

                if (onFormOpen) {
                    onFormOpen(_this.sections.form.$window, _this.getCurrentResource());
                }
            });
        });
        return $deferred;
    },

    /**
     * Get the current resource id
     */
    getCurrentResourceId: function () {
        return (this.currentResource ? this.currentResource.id : null);
    },

    /**
     * Return the current resource
     */
    getCurrentResource: function () {
        return this.currentResource;
    },

    // @override
    purgeResource: function (resource) {
        return expresso.Common.purgeResource(resource, this.model);
    },

    // @override
    resizeContent: function () {
        if (this.resizing) {
            return;
        }
        this.resizing = true;
        expresso.layout.applicationbase.ApplicationBase.fn.resizeContent.call(this);

        // if the height is too small, hide the preview
        if (this.$domElement) {
            var kendoSplitter = this.$domElement.find(".exp-splitter-preview").data("kendoSplitter");
            if (kendoSplitter && this.autoCollapsedPreview !== false) {
                var $previewDiv = this.$domElement.find(".exp-container-preview");
                if ($(window).height() < 550) {
                    kendoSplitter.collapse($previewDiv);
                } else {
                    kendoSplitter.expand($previewDiv);
                }
                // overwrite the attribute (not done by the user)
                this.autoCollapsedPreview = true;
            }
        }

        // resize all sections
        for (var sectionName in this.sections) {
            if (this.sections.hasOwnProperty(sectionName) && this.sections[sectionName] && this.sections[sectionName].resizeContent) {
                this.sections[sectionName].resizeContent();
            }
        }
        this.resizing = false;
    },

    /**
     * Return a promise with true if the user is allowed to perform the action on the resource
     * @param actionName
     * @param [waitDeferred] wait the deferred to resolve. Default is true
     * @return {*|PromiseLike<boolean>|Promise<boolean>}
     */
    isActionAllowed: function (actionName, waitDeferred) {
        if (actionName == "create") {
            if (this.createActionPromise) {
                return this.createActionPromise.then(function (result) {
                    //console.log(_this.getResourceSecurityPath() + " isActionAllowed: " + result.allowed);
                    return result.allowed;
                });
            } else {
                return $.Deferred().resolve(false);
            }
        } else {
            //console.log(this.getResourceSecurityPath() + " - isActionAllowed [" + actionName + "]");
            if (this.availableActionsPromise) {
                if (waitDeferred === false && this.availableActionsPromise.state() === "pending") {
                    // this is only an optimization when editing a new resource
                    // if the user cannot edit the resource, the backend will reject it anyway
                    return $.Deferred().resolve(true);
                } else {
                    return this.availableActionsPromise.then(function (actions) {
                        if (actions) {
                            for (var i = 0, l = actions.length; i < l; i++) {
                                if (actions[i].name == actionName) {
                                    //console.log(_this.getResourceSecurityPath() + " - isActionAllowed [" + actionName + "]: " + actions[i].allowed);
                                    return actions[i].allowed;
                                }
                            }
                        }
                        return false;
                    });
                }
            } else {
                return $.Deferred().resolve(false);
            }
        }
    },

    /**
     * Request the server to verify if the available actions are restricted
     */
    verifyActionsRestrictions: function () {
        //console.log(this.getResourceSecurityPath() + " verifyActionsRestrictions");
        if (this.availableActionsPromise && this.availableActionsPromise.state() == "pending") {
            // it is not pertinent anymore. A new selection has been made
            //console.log(this.getResourceSecurityPath() + " Rejecting previous promise");
            this.availableActionsPromise.reject();
        }

        // create a new promise
        this.availableActionsPromise = $.Deferred();

        var actions = $.map(this.availableActions, function (action) {
            return action.pgmKey;
        });

        if (actions.length) {
            this.sendRequestVerifyActionsRestrictions(actions, this.availableActionsPromise);
        } else {
            // nothing to validate
            //console.log(this.getResourceSecurityPath() + " No actions");
            this.availableActionsPromise.resolve(this.availableActions);
        }
    },

    /**
     * Send a request to the server to get the action restrictions
     *
     * @param actions
     * @param $deferred
     */
    sendRequestVerifyActionsRestrictions: function (actions, $deferred) {
        var _this = this;

        if (this.getCurrentResourceId() != -1) {
            // send the request to the server
            this.sendRequest(this.getRelativeWebServicePath() + "/verifyActionsRestrictions", null, null,
                {
                    actions: actions.join(","),
                    id: this.getCurrentResourceId()
                }, {waitOnElement: null, ignoreErrors: true})
                .done(function (result) {
                    // promise could have been rejected while waiting for the response
                    if ($deferred.state() == "pending") {
                        //console.log("result", result);
                        $.each(_this.availableActions, function (i, action) {
                            action.allowed = result[action.pgmKey];
                        });
                        //console.log(_this.getResourceSecurityPath() + " - sendRequestVerifyActionsRestrictions done");

                        $deferred.resolve(_this.availableActions);
                    }
                });
        } else {
            $.each(_this.availableActions, function (i, action) {
                action.allowed = false;
            });
            //console.log(_this.resourceName + " - this.availableActions", _this.availableActions);

            $deferred.resolve(_this.availableActions);
        }
    },

    /**
     * Request the server to verify if the create action is available
     */
    verifyCreationRestrictions: function () {
        var _this = this;

        // if there is already a promise, do not create a new one
        if (!this.createActionPromise || this.createActionPromise.state() !== "pending") {
            this.createActionPromise = $.Deferred();

            // verify if the user has the privilege to create
            if (_this.isUserAllowed("create")) {
                // send the request to the server
                this.sendRequest(this.getRelativeWebServicePath() + "/verifyCreationRestrictions", null,
                    null, {id: _this.getCurrentResourceId()}, {waitOnElement: null, ignoreErrors: true})
                    .done(function (result) {
                        _this.createActionPromise.resolve(result);
                    })
                    .fail(function () {
                        _this.createActionPromise.reject();
                    });
            } else {
                _this.createActionPromise.resolve(false);
            }
        }
    },

    /**
     * You can define actions to be added in the form and the grid toolbar.
     * Method in the action receive the resourceManager as "this".
     * @returns {Array} a list of actions (action name or complete object)
     */
    getAvailableActions: function () {
        // by default, no addionnal action buttons
        var actions = [];

        // var action = {
        //     name: "myaction",
        //     icon: "myicon",
        //     primary: false,                      // default is false
        //     showButtonInGridToolbar: true,       // default is in configuration
        //     showButtonInForm: true,              // default is true
        //     showButtonOnMobile: true,            // default is true
        //     supportMultipleSelections: true,     // default is true
        //     resourceCollectionAction: false,     // default is false
        //     saveBeforeAction: true,              // default is true
        //     reasonRequested: false,      // if a reason is requested, a default window will be displayed
        //     beforePerformAction: function(resource) {
        //          // resource is null if multiple resources have been selected
        //          // validate it the send request can be executed
        //          // return true will allow the performAction to be called
        //          return true;
        //     },
        //     performAction: function (resource, data) {
        //          // define the request to be called
        //          // perform action on each selected resource (method is called for each resource)
        //          return expresso.Common.sendRequest(_this.getRelativeWebServicePath(resource.id), "myaction");
        //     },
        //     afterPerformAction: function(resource) {
        //          // method is called for each resource
        //     }
        // };

        // your must define labels if you do not use standard actions
        // <actionName>ButtonLabel and <actionName>ButtonTitle

        // example 1: a standard submit action
        // actions.push({
        //     name: "submit"
        // });

        return actions;
    },

    /**
     * This method is to be called by the framework only.
     * Subclass should overwrite the "getAvailableActions" action
     */
    parseAvailableActions: function () {
        var _this = this;
        if (!this.availableActions) {
            this.availableActions = [];
            var availableActions = this.getAvailableActions();
            if (availableActions && availableActions.length) {
                $.each(availableActions, function (index, action) {
                    // complete the action if not completed
                    if (typeof action === "string") {
                        // standard action
                        action = {
                            name: action,
                            pgmKey: action
                        };
                    }

                    if (!action.pgmKey) {
                        action.pgmKey = action.name;
                    }

                    // skip the action if the user is not allowed
                    if (_this.isUserAllowed(action.pgmKey)) {

                        // add the icon if needed
                        if (!action.icon) {
                            var icon;
                            switch (action.pgmKey) {
                                // system actions should not be used for application purpose
                                case "read":
                                case "create":
                                case "update":
                                case "delete":
                                case "sync":
                                case "print":
                                case "duplicate":
                                case "deactivate":
                                case "email":
                                case "link":
                                    icon = "";
                                    break;

                                // predefined actions
                                case "cancel":
                                    icon = "fa-times";
                                    break;
                                case "complete":
                                case "terminate":
                                    icon = "fa-check";
                                    break;

                                case "approve":
                                    icon = "fa-thumbs-o-up";
                                    break;
                                case "reject":
                                    icon = "fa-thumbs-down";
                                    break;

                                case "open":
                                    icon = "fa-folder-o";
                                    break;
                                case "close":
                                    icon = "fa-folder";
                                    break;

                                // supported but not defined actions
                                case "lock":
                                    icon = "fa-lock";
                                    break;
                                case "unlock":
                                    icon = "fa-unlock";
                                    break;

                                case "accept":
                                    icon = "fa-check";
                                    break;
                                case "refuse":
                                    icon = "fa-ban";
                                    break;

                                case "import":
                                    icon = "fa-download";
                                    break;
                                case "export":
                                    icon = "fa-upload";
                                    break;

                                case "send":
                                    icon = "fa-paper-plane";
                                    break;
                                case "receive":
                                    icon = "fa-inbox";
                                    break;

                                case "start":
                                    icon = "fa-play";
                                    break;
                                case "stop":
                                    icon = "fa-stop";
                                    break;
                                case "hold":
                                    icon = "fa-hand-paper-o";
                                    break;

                                case "submit":
                                    icon = "fa-paper-plane-o";
                                    break;
                                case "process":
                                    icon = "fa-cogs";
                                    break;

                                case "init":
                                case "reset":
                                case "execute":
                                case "expedite":
                                case "validate":
                                case "revise":
                                default:
                                    icon = "";
                                    break;
                            }
                            action.icon = icon;
                        }

                        // define default label
                        if (!action.label) {
                            action.label = action.name + "ButtonLabel";
                        }

                        // define default title
                        if (!action.title) {
                            action.title = action.name + "ButtonTitle";
                        }

                        // add the performAction method if needed
                        if (!action.performAction) {
                            if (action.resourceCollectionAction) {
                                action.performAction = function (resource, data) {
                                    // resource is null
                                    return _this.sendRequest(_this.getRelativeWebServicePath(), action.pgmKey, data);
                                };
                            } else {
                                action.performAction = function (resource, data) {
                                    return _this.sendRequest(_this.getRelativeWebServicePath(resource.id), action.pgmKey, data);
                                };
                            }
                        }

                        if (!action.beforePerformAction) {
                            // none by default
                        }

                        if (!action.afterPerformAction) {
                            // none by default
                        }

                        if (action.showButtonInGridToolbar === undefined) {
                            action.showButtonInGridToolbar =
                                expresso.Common.getSiteNamespace().config.Configurations.showButtonInGridToolbar;
                        }

                        if (action.resourceCollectionAction) {
                            action.showButtonInGridToolbar = true;
                        }

                        if (action.showButtonOnMobile === undefined) {
                            action.showButtonOnMobile = true;
                        }

                        _this.availableActions.push(action);
                    }
                });
            }

            // add CRUD and standard actions
            $.each(["update", "delete", "duplicate", "deactivate"], function (i, action) {
                if (_this.isUserAllowed(action)) {
                    _this.availableActions.push({name: action, pgmKey: action, systemAction: true});
                }
            });
        }
        return _this.availableActions;
    },

    /**
     * This method is to be called by the framework only.
     * Subclass should overwrite the "getAvailableActions" action
     */
    getAvailableActionsWithRestrictions: function () {
        if (this.availableActionsPromise) {
            return this.availableActionsPromise;
        } else {
            // no resource selected yet...
            //console.trace("Not defined yet");
            return $.Deferred().resolve([]);
        }
    },

    /**
     * Verify is the user is allowed to perform the action on the resource
     * @param action
     * @returns {boolean}
     */
    isUserAllowed: function (action) {
        return expresso.Common.isUserAllowed(this.getResourceSecurityPath(), action);
    },

    // @override
    destroy: function () {
        //console.trace("Destroying RM [" + this.resourceName + "]");
        if (this.eventCentral) {
            this.eventCentral.destroy();
            this.eventCentral = null;
        }

        if (this.sections) {
            var sections = this.sections;
            this.sections = null;
            for (var section in sections) {
                if (sections.hasOwnProperty(section) && sections[section] && sections[section] !== true && sections[section].destroy) {
                    // console.log("  Destroying section [" + section + "]");
                    try {
                        sections[section].destroy();
                    } catch (e) {
                        console.trace("Problem destroying section [" + section + "] for resource [" + this.resourceName + "]", e);
                    }
                    sections[section] = null;
                }
            }
            sections = null;
        }

        this.appDef = null;
        this.model = null;
        this.resourceName = null;
        this.resourcePath = null;
        this.currentResource = null;
        this.availableActions = null;
        this.availableActionsPromise = null;

        // if the master has been loaded by the this manager, we must destroy it
        if (this.masterResourceManager && this.masterResourceManager.loadedBySubApplication) {
            var masterResourceManager = this.masterResourceManager;
            this.masterResourceManager = null;
            masterResourceManager.destroy();
            masterResourceManager = null;
        }
        this.masterResourceManager = null;
        this.siblingResourceManager = null;

        // destroy all related div
        // then also remove any div added by the application
        $("body").children("div[data-resource-manager-id='" + this.resourceManagerId + "']").each(function () {
            var $div = $(this);
            expresso.util.UIUtil.destroyKendoWidgets($div);
            $div.remove();
        });

        this.resourceManagerId = null;
        expresso.layout.applicationbase.ApplicationBase.fn.destroy.call(this);
    }
});
