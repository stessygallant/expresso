var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Preview class
 */
expresso.layout.resourcemanager.Preview = expresso.layout.resourcemanager.SectionBase.extend({

    // keep a reference on the current shown tab
    currentTab: undefined,

    kendoTabStrip: undefined,

    contents: undefined,

    /**
     * Set the reference to the jquery object for the DOM element
     * @param $domElement reference to the jquery object for the DOM element
     */
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        // initialize the tabs and select the first tab
        var _this = this;

        // verify if the preview.html defines a <ul>
        var $ul = $domElement.find("ul");
        if (!$ul.length) {
            $ul = $("<ul></ul>").appendTo($domElement);
        }

        this.contents = [];
        var contentUrls = [];
        var contents = this.getContents();
        for (var i = 0; i < contents.length; i++) {
            var content = contents[i];
            var contentUrl;
            var userAllowed = true;
            if (typeof content === "string") {
                contentUrl = content;
            } else {
                contentUrl = content.contentUrl;

                var resourceSecurityPath = null;
                if (content.resource) {
                    // deprecated
                    resourceSecurityPath = content.resource;
                } else if (content.resourceSecurityPath) {
                    resourceSecurityPath = content.resourceSecurityPath;
                } else if (content.countUrl && content.countUrl.url) {
                    resourceSecurityPath = expresso.Common.getResourceSecurityPathFromPath(content.countUrl.url);
                } else if (content.countUrl) {
                    resourceSecurityPath = expresso.Common.getResourceSecurityPathFromPath(content.countUrl);
                }

                if (resourceSecurityPath == "document") {
                    if (expresso.Common.getSiteNamespace().config.Configurations.supportSubResourceDocument !== false) {
                        // the user needs to have read access to <resourcePath>/document
                        resourceSecurityPath = this.resourceManager.getResourceSecurityPath() + "/document";
                    }
                }

                // if the resource name is defined, verify if the user has access
                if (resourceSecurityPath && !expresso.Common.isUserAllowed(resourceSecurityPath)) {
                    userAllowed = false;
                }

                // if a role is defined, verify if the user is in the role
                if (content.role && !expresso.Common.isUserInRole(content.role)) {
                    userAllowed = false;
                }

                // console.log("Preview on [" + resourceSecurityPath + "]: " + userAllowed);

                // if there is a title in the object, use it and insert it to the $domElement
                // this could be used to insert dynamically a new tab based on privileges
                if (userAllowed && content.title) {
                    $ul.append("<li>" + (_this.getLabel(content.title, null, true) ?
                        _this.getLabel(content.title) : content.title) + "</li>");
                } else {
                    if (!userAllowed) {
                        console.warn("User is not allowed to see the tab [" + contentUrl + "]. " +
                            "User not allowed read on [" + resourceSecurityPath + "]");
                    }
                }
            }

            if (userAllowed) {
                contentUrls.push(this.resourceManager.applicationPath + "/preview_tab_" + contentUrl + ".html");
                this.contents.push(content);
            }
        }

        // if there is not content, remove the split
        if (!this.contents.length) {
            try {
                $domElement.closest("[data-role=splitter]").data("kendoSplitter").collapse($domElement.closest(".k-pane"));
            } catch (ex) {
                console.warn("No splitter to collapse");
            }
        } else {
            this.kendoTabStrip = $domElement.kendoTabStrip({
                animation: false,
                contentUrls: contentUrls,
                contentLoad: function (e) {
                    //var tabLabel = $(e.item).find("> .k-link").text();
                    var $tab = $(e.item);
                    var tabName = _this.contents[$tab.index()];
                    if (typeof tabName !== "string") {
                        tabName = tabName.contentUrl;
                    }
                    //console.log("Loaded tab [" + tabName + "]");

                    var $contentElement = $(e.contentElement);

                    // convert the form (if any) to a custom form
                    _this.addPromise($contentElement.kendoExpressoForm({labels: _this.resourceManager.labels}).data("kendoExpressoForm").ready());

                    // then get the script
                    var tabScriptPath = _this.resourceManager.applicationPath + "/preview_tab_" + tabName.toLowerCase() + ".js";
                    expresso.Common.getScript(tabScriptPath).done(function () {

                        // get the object-class and instantiate the object
                        var objectClass = _this.resourceManager.applicationPath.replace(/\//g, '.') + ".PreviewTab" + tabName.capitalize();
                        if (!objectClass.startsWith("expresso")) {
                            // add the name of the site
                            objectClass = expresso.Common.getSiteName() + "." + objectClass;
                        }
                        //console.log("Initializing [" + objectClass + "]: " + tabName);
                        var objectInstance = eval("new " + objectClass + "(_this.resourceManager, tabName, $tab)");
                        objectInstance.initDOMElement($contentElement);

                        // make sure that the new loaded tab is the new shown tab
                        _this.selectNewTab(e);
                    });
                },
                activate: function (e) {
                    _this.selectNewTab(e);
                }
            }).data("kendoTabStrip");

            // add the functionality to open the tab as master when you CTRL-click on it
            _this.$domElement.find("li.k-item").on("click", function (e) {
                if (e.ctrlKey) {

                    var $li = $(this);
                    var objectInstance = $(_this.kendoTabStrip.contentElement($li.index())).data("object-instance");
                    if (objectInstance && objectInstance.subResourceManager && objectInstance.subResourceManager.appDef) {
                        //console.log("appDef", objectInstance.subResourceManager.appDef);
                        //console.log(" window.location", window.location);
                        var url = window.location.protocol + "//" + window.location.host + window.location.pathname;
                        url += "?app=" + objectInstance.subResourceManager.appDef.absoluteAppName;
                        console.log("Opening tab in new window URL [" + url + "]");
                        window.open(url, "_blank");
                    }
                }
            });

            // when a new resource is selected, refresh the tab
            this.subscribeEvent([this.RM_EVENTS.RESOURCE_SELECTED, this.RM_EVENTS.RESOURCE_UPDATED], function (e, resource) {
                // only refresh if the preview is visible
                if (_this.$domElement.is(":visible")) {
                    _this.refresh(resource);
                } else {
                    //console.log("PREVIEW - do no call refresh because the $domElement is NOT visible", resource);
                }
            });

            // init the first tab
            window.setTimeout(function () {
                var tabIndex = 0; // select the fist one by default
                if (_this.resourceManager) {
                    if (_this.resourceManager.options.defaultTab) {
                        for (var i = 0, l = _this.contents.length; i < l; i++) {
                            if (_this.contents[i].contentUrl == _this.resourceManager.options.defaultTab) {
                                tabIndex = i;
                                break;
                            }
                        }
                    }
                    _this.kendoTabStrip.select(tabIndex);
                }
            }, 100);
        }
    },

    /**
     * Select a new tab (on a tab activation event)
     * @param e event triggered
     */
    selectNewTab: function (e) {
        //var tabName = $(e.item).find("> .k-link").text();
        //console.log("Activate/select tab [" + tabName + "]");

        // unselect the current tab
        if (this.currentTab) {
            this.currentTab.onTabActivationEvent(false);
        }

        // select the new tab
        var objectInstance = $(e.contentElement).data("object-instance");
        if (objectInstance) {
            this.currentTab = objectInstance;
            this.currentTab.onTabActivationEvent(true);
        }
    },

    /**
     * Resize the preview pane
     */
    resizeContent: function () {
        if (this.$domElement) {
            expresso.layout.resourcemanager.SectionBase.fn.resizeContent.call(this);
            var h = this.$domElement.innerHeight() - this.$domElement.children(".k-tabstrip-items").outerHeight(true);
            this.$domElement.children(".k-content").height(h);

            // then resize all tabs
            this.$domElement.children(".k-content").each(function () {
                var tab = $(this).data("object-instance");
                if (tab && tab.resizeContent) {
                    tab.resizeContent();
                }
            });
        }
    },

    /**
     * Get the array that contains the name of each tab
     * @returns {string[]}
     */
    getContents: function () {
        alert("Method [getContents] MUST be implemented by preview");
        return [];
    },

    /**
     * Force a refresh of the tabs
     */
    forceRefresh: function () {
        this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, this.resourceManager.currentResource);
    },

    /**
     * Refresh the content of the preview with the new selected resource.
     * Handle the count label on the preview tabs
     * @param resource the resource object
     */
    refresh: function (resource) {
        //console.log("PREVIEW - refresh", resource);
        var _this = this;
        $.each(this.contents, function (index, value) {
            if (typeof value !== "string" && value.countUrl) {
                // find the tab
                var $tab = _this.$domElement.find("ul.k-tabstrip-items li:nth-child(" + (index + 1) + ")");

                _this.refreshCount($tab);
            }
        });
    },

    /**
     *
     * @param $tab
     */
    refreshCount: function ($tab) {

        // verify if the tab has a countUrl
        var tabContent = this.contents[$tab.index()];
        if (!tabContent.countUrl) {
            // no countUrl
            return;
        }

        var $countLabel = $tab.find("> span.k-link").find("span.count-label");
        if (!$countLabel.length) {
            // add the default count label
            $countLabel = $("<span class='count-label'></span>").appendTo($tab.find("> span.k-link"));
        }
        $countLabel.hide();

        // make sure that a resource is selected
        var resource = this.resourceManager.currentResource;
        if (resource && resource.id) {
            var url;
            var filter = {};

            // for subresource, always count and display ALL resources by default(not only active resources)
            if (typeof tabContent.countUrl === "string") {
                url = tabContent.countUrl.replace("{id}", resource.id);
                filter = expresso.Common.buildKendoFilter([], {countOnly: true, activeOnly: false});
            } else {
                url = tabContent.countUrl.url.replace("{id}", resource.id);

                if (tabContent.countUrl.filter) {
                    filter = tabContent.countUrl.filter;
                    if (typeof tabContent.countUrl.filter === "function") {
                        filter = filter();
                    } else {
                        // make a copy to avoid mofifying the source
                        if ($.isArray(filter)) {
                            filter = {logic: "and", filters: $.extend(true, [], filter)};
                        } else {
                            filter = $.extend(true, {}, filter);
                        }
                    }
                }
                filter = expresso.Common.buildKendoFilter(filter, {
                    countOnly: true,
                    activeOnly: filter.activeOnly || false
                });

                // replace the {id} with the current id
                filter = filter.replaceAll("{id}", resource.id);
            }

            // get the count from the server
            this.sendRequest(url, null, null, filter, {
                ignoreErrors: true,
                waitOnElement: null
            }).done(function (result) {
                if (result.total) {
                    $countLabel.html(result.total).show();
                }
            });
        }
    },

    // @override
    destroy: function () {
        // destroy each tab
        this.$domElement.children(".k-content").each(function () {
            var tab = $(this).data("object-instance");
            if (tab) {
                tab.destroy();
            }
            expresso.util.UIUtil.destroyKendoWidgets($(this));
        });

        this.kendoTabStrip.destroy();
        this.kendoTabStrip = null;
        this.currentTab = null;
        this.contents = null;
        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    }

});
