var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.mobile = expresso.layout.mobile || {};

/**
 * Base expresso layout for mobile resource manager
 */
expresso.layout.mobile.MobileResourceManager = expresso.layout.mobile.MobileBase.extend({

    // use to configure Kendo UI model
    model: undefined,

    // name of the resource (camelCase)
    // ex: activityLogRequest, activityLogRequestChange
    resourceName: undefined,

    // path of the resource (usually the same as the name)
    // for sub resource, resourcePath is the relative path to the master resource
    // ex: /activityLogRequest/0/change
    resourcePath: undefined,

    // field that contains the unique number (id, no, etc) for the resource
    resourceFieldNo: undefined,

    // current selected resource
    currentResource: undefined,

    // current selected tab key
    currentTabKey: undefined,

    // reference to the list view
    kendoMobileListView: undefined,

    // active only flag
    activeOnly: undefined,

    /**
     *
     * @param applicationPath
     * @param resourceName
     * @param model
     * @param [resourcePath] Optional. Default is same as resourceName
     */
    init: function (applicationPath, resourceName, model, resourcePath) {
        expresso.layout.mobile.MobileBase.fn.init.call(this, applicationPath);

        // initialize properties
        this.resourceName = resourceName;
        this.resourcePath = resourcePath || resourceName;
        this.model = model;
        if (!this.resourceFieldNo) {
            this.resourceFieldNo = this.resourceName + "No";
        }
    },

    // @override
    initData: function () {
        var _this = this;
        var $deferred = $.Deferred();

        $.when(
            // default initData
            expresso.layout.mobile.MobileBase.fn.initData.call(this)
        ).done(function () {
            // load the the model
            expresso.util.Model.initModel(_this).done(function () {
                $deferred.resolve();
            });
        });

        return $deferred;
    },

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.mobile.MobileBase.fn.initDOMElement.call(this, $domElement);
        var _this = this;

        // add the action buttons to the list template
        var $listViewTemplate = this.$domElement.find(".list-view-template");
        var actions = this.getAvailableActions();
        if (actions && actions.length) {
            var $actionDiv = $("<div class='action-buttons'></div>").appendTo($listViewTemplate);
            $.each(actions, function () {
                var action = this;
                $actionDiv.append("<button data-role='button' class='km-primary km-button' " +
                    "data-action='" + action.name + "' ><span class='fa " + action.icon + "'></span></button>");
            });
        }

        // build the list view
        var $div = this.$domElement.children("div").first();
        var $ul = $("<ul class='exp-mobile-list-view'></ul>").appendTo($div);
        this.kendoMobileListView = $ul.kendoMobileListView({
            dataSource: _this.getDataSource(),
            autoBind: false,
            dataBound: function () {
                _this.onListViewLoaded();
            },
            //filterable: true,
            template: $listViewTemplate.html()
        }).data("kendoMobileListView");

        var $header = this.$domElement.closest(".main-view").find("[data-role=header]");
        var $footer = this.$domElement.closest(".main-view").find("[data-role=footer]");

        // add the + button in the header
        var $navbar = $header.find("[data-role=navbar]");

        // verify create permission
        if (this.isUserAllowed("create")) {
            //temp no creation for now.
            //$navbar.append("<a data-role='button' data-align='right'><span class='fa fa-plus fa-lg'></span></a>");
        }

        // add a tabstrip
        var tabStrips = this.getTabStrips();
        if (tabStrips) {
            var $tabStrip = $("<div data-role='tabstrip'></div>").appendTo($footer);
            var tabSelected = false;
            $.each(tabStrips, function (index) {
                var tabStrip = this;
                $tabStrip.append("<a data-tab-key='" + tabStrip.tabKey + "' class='" + tabStrip.classes + "'>" + tabStrip.title + "</a>");

                $tabStrip.find(".km-icon").replaceWith("<h1>test</h1>");

                if (tabStrip.defaultSelected) {
                    $tabStrip.attr("data-selected-index", index);
                    _this.onTabSelected(tabStrip.tabKey);
                    tabSelected = true;
                }
            });
            // if no default is specified, select the first one
            if (!tabSelected) {
                _this.onTabSelected(tabStrips[0].tabKey);
            }
        }
    },

    /**
     *
     */
    onDomElementInitialized: function () {
        var _this = this;
        return expresso.layout.mobile.MobileBase.fn.onDomElementInitialized.call(this).done(function () {
            // register event listeners
            var $listView = _this.$domElement.find("ul[data-role='listview']");

            // goto view
            $listView.on("click", "li", function () {
                var $li = $(this);
                var id = $li.children(".km-listview-link").data("id");
                _this.currentResource = _this.kendoMobileListView.dataSource.get(id);
                _this.loadView("form");
            });

            // handle actions
            $listView.on("click", "li .action-buttons .km-button", function (e) {
                e.stopPropagation();
                e.preventDefault();
                var $button = $(this);
                var $li = $button.closest("li");
                var currentAction = $button.data("action");
                var id = $button.closest("li").find(".km-listview-link").data("id");
                _this.currentResource = _this.kendoMobileListView.dataSource.get(id);

                var actions = _this.getAvailableActions();
                if (actions && actions.length) {
                    $.each(actions, function () {
                        var action = this;
                        if (action.name == currentAction && action.type == "modal") {
                            var id = $li.children(".km-listview-link").data("id");
                            _this.currentResource = _this.kendoMobileListView.dataSource.get(id);
                            _this.loadModal(action, _this.currentResource);
                        } else if (action.name == currentAction && action.type == "confirm") {
                            expresso.util.UIUtil.buildYesNoWindow(_this.getLabel(action.name + "WindowTitle"), _this.getLabel(action.name + "WindowDescription"))
                                .done(function () {
                                    _this.executionAction(action.name, _this.currentResource).done(function () {
                                        _this.refreshView();
                                    });
                                });
                        }
                    });
                }
            });

            // tab strip
            var $footer = _this.$domElement.closest(".main-view").find("[data-role=footer]");
            var $tabStrip = $footer.find("div[data-role='tabstrip']");

            if ($tabStrip.length) {
                var tabStrip = $tabStrip.data("kendoMobileTabStrip");

                tabStrip.bind("select", function (e) {
                    var $tab = e.item;
                    _this.onTabSelected($tab.data("tabKey"));
                });

                //Go through every item and refresh its count badge
                _this.refreshCountTabStrip()
            } else {
                // then load the list
                _this.kendoMobileListView.dataSource.fetch();
            }
        });
    },

    /**
     *
     * @param $tab
     */
    refreshCountTabStrip: function () {
        var _this = this;

        //tabstrip
        var $footer = _this.$domElement.closest(".main-view").find("[data-role=footer]");
        var $tabStrip = $footer.find("div[data-role='tabstrip']");
        var tabStripElements = _this.getTabStrips();

        if ($tabStrip.length) {
            var $tabStrip = $tabStrip.data("kendoMobileTabStrip");
            $.each(tabStripElements, function ($index) {
                var $tabItem = this;

                // verify if the tab has a countUrl
                if (!$tabItem.countUrl) {
                    // no countUrl
                    return;
                }

                var url;
                var filter;

                // for subresource, always count and display ALL resources by default(not only active resources)
                if (typeof $tabItem.countUrl === "string") {
                    url = $tabItem.countUrl;
                    filter = expresso.Common.buildKendoFilter([], {countOnly: true, activeOnly: false});
                } else {
                    url = $tabItem.countUrl.url;

                    // make a copy to avoid mofifying the source
                    if ($.isArray($tabItem.countUrl.filter)) {
                        filter = {
                            logic: $tabItem.countUrl.filter.logic || "and",
                            filters: $.extend(true, [], $tabItem.countUrl.filters)
                        };
                    } else {
                        filter = $.extend(true, {}, $tabItem.countUrl.filter);
                    }
                    filter = expresso.Common.buildKendoFilter(filter, {
                        countOnly: true,
                        activeOnly: filter.activeOnly || false
                    });
                }
                // get the count from the server
                expresso.Common.sendRequest(url, null, null, filter, {
                    ignoreErrors: true,
                    waitOnElement: null
                }).done(function (result) {
                    if (result.total) {
                        $tabStrip.badge($index, result.total)
                    }
                });
            });
        }

    },


    // @override
    initView: function (view, $view) {
        expresso.layout.mobile.MobileBase.fn.initView.call(this, view, $view);

        if (view.initForm) {
            view.initForm(this.currentResource, this.model);
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

    /**
     * Get the complete Web Service REST point URL for the resource
     * @param id (optional)
     * @returns {string}
     */
    getWebServicePath: function (id) {
        return expresso.Common.getWsResourcePathURL() + "/" + this.getRelativeWebServicePath(id);
    },

    /**
     * Get the resource pgmKey (ex:activityLogRequest, activityLogRequest/change)
     * @returns {*}
     */
    getResourceSecurityPath: function () {
        // if the resource path contains /, only return odd token
        if (this.resourcePath.indexOf("/") != -1) {
            return $.grep(this.resourcePath.split("/"), function (path, index) {
                return ((index % 2) == 0);
            }).join("/");
        } else {
            return this.resourcePath;
        }
    },

    /**
     * Get the relative Web Service REST point URL for the resource
     * @param [id] (optional)
     * @returns {string}
     */
    getRelativeWebServicePath: function (id) {
        return this.resourcePath + (id !== undefined && id !== null ? "/" + id : "");
    },

    /**
     *
     */
    getUploadDocumentPath: function (id) {
        return expresso.Common.getWsUploadPathURL() + "/" +
            this.getRelativeWebServicePath(id) + "?creationUserName=" + expresso.Common.getUserInfo().userName;
    },

    /**
     * tabstring: {
     *     tabKey: "myTabKey",  // use to identify the tab
     *     dataIcon: "cart",
     *     classes: "myClass",  // do not forget to add "fa" if your icon is FontAwesome
     *     title: "My Title",
     *     defaultSelected: true  // only one should be true. Default is false
     * }
     * @returns array of tabstrip
     */
    getTabStrips: function () {
        return null;
    },

    /**
     *
     * @param tabKey
     */
    onTabSelected: function (tabKey) {
        //console.log("Selecting tab " + tabKey);
        this.currentTabKey = tabKey;

        // trigger refresh list view
        this.kendoMobileListView.dataSource.read();
    },

    // override
    refreshView: function () {
        // trigger refresh list view
        this.kendoMobileListView.dataSource.read();

        //refresh the count badge over the tab
        this.refreshCountTabStrip();

    },

    /**
     *
     */
    onListViewLoaded: function () {
        var _this = this;

        // for each resource, verify the actions allowed
        this.$domElement.find(".exp-mobile-list-view li").each(function () {
            var $li = $(this);
            _this.verifyActionButtons($li);
        });
    },

    /**
     * Verify if the user is allowed to perform the action on the resource
     * @param $li
     */
    verifyActionButtons: function ($li) {
        var _this = this;
        var resourceId = $li.children("a").data("id");

        var actions = this.getAvailableActions();
        var actionList = [];
        $.each(actions, function () {
            actionList.push(this.name);
        });

        this.sendRequest(_this.getRelativeWebServicePath() + "/verifyActionsRestrictions", null, null, {
            actions: actionList.join(","),
            id: resourceId
        }, {waitOnElement: null, ignoreErrors: true}).done(function (result) {
            $.each(actionList, function () {
                var action = this;
                var $actionButton = $li.find(".action-buttons button[data-action='" + action + "']");
                if (result[action]) {
                    $actionButton.show();
                } else {
                    $actionButton.hide();
                }
            });
        });
    },

    /**
     * Execute the action on the resource
     *
     * @param action
     * @param resource
     * @param params
     * @returns {*}
     */
    executionAction: function (action, resource, params) {
        return this.sendRequest(this.getRelativeWebServicePath(resource.id), action, params);
    },

    // @override
    purgeResource: function (resource) {
        return expresso.Common.purgeResource(resource, this.model);
    },

    /**
     * Get the data source for the resource
     * @returns
     */
    getDataSource: function () {
        var _this = this;
        return {
            transport: {
                parameterMap: function (data, operation) {
                    if (operation === "read") {
                        var filter = _this.getListFilter();
                        if (filter) {
                            return expresso.Common.buildKendoFilter(filter, {
                                activeOnly: !(_this.activeOnly === false)
                            }, true);
                        } else {
                            return data;
                        }
                    } else {
                        var props = _this.purgeResource(data);
                        var s = JSON.stringify(props);
                        //console.log("PROPS: " + s);
                        return s;
                    }
                },
                read: {
                    dataType: "json",
                    type: "GET",
                    url: function () {
                        return _this.getWebServicePath();
                    }
                },
                create: {
                    dataType: "json",
                    contentType: "application/json; charset=utf-8",
                    type: "POST",
                    url: function () {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.labels);

                        return _this.getWebServicePath();
                    }
                },
                update: {
                    dataType: "json",
                    contentType: "application/json; charset=utf-8",
                    type: "PUT",
                    url: function (e) {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.labels);

                        return _this.getWebServicePath(e.id);
                    }
                },
                destroy: {
                    dataType: "json",
                    type: "DELETE",
                    url: function (e) {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.labels);

                        return _this.getWebServicePath(e.id);
                    }
                }
            },
            sort: _this.getSort(),
            schema: {
                model: {id: "id"},
                //model: model,
                data: function (d) {
                    if (d && d.data) {
                        return d.data;
                    } else {
                        return [d];
                    }
                },
                total: "total",
                parse: function (response) {
                    if (response) {
                        response = _this.parseResponse(response);
                    }
                    return response;
                }
            }
        };
    },

    /**
     * Get the sort properties for the grid.
     * @returns {*} // ex: {field: <tbd>,dir: "desc"|"asc"};
     */
    getSort: function () {
        return undefined;
    },

    /**
     * Executed before the server response is used. Use it to preprocess or parse the server response.
     *
     * @param response  response from the server
     * @returns {*} return the response
     */
    parseResponse: function (response) {
        if (response.data && response.data.length !== undefined) {
            for (var i = 0; i < response.data.length; i++) {
                response.data[i] = this.parseResponseItem(response.data[i]);
            }
        } else {
            // this is a simple object
            response = this.parseResponseItem(response);
        }
        return response;

    },

    /**
     * Executed before the server response is used. Use it to preprocess or parse each item.
     *
     * @param item  item (row) to be preprocessed if needed
     * @returns {*} return the item (modified)
     */
    parseResponseItem: function (item) {
        return expresso.Common.parseResponseItem(item, this.model);
    },

    /**
     * Return the filter for the list of resources
     * @returns Kendo List filter
     */
    getListFilter: function () {
        return null;
    },

    /**
     * Get the list of available actions for the resource.
     * By default, none
     * @returns {*[]}
     */
    getAvailableActions: function () {
        return [];
    }
});
