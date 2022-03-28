﻿expresso.applications.general.documentmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // use when upload document is needed
    kendoUpload: undefined,

    // @override
    getColumns: function () {
        var columns = [{
            field: "creationDate"
        }, {
            field: "creationUserFullName",
            width: 150
        }, {
            field: "fileName",
            width: 200
        }, {
            field: "description"
        }
        ];

        if (this.resourceManager.model.fields["documentTypeId"].values.data &&
            this.resourceManager.model.fields["documentTypeId"].values.data.length > 0) {
            columns.push.apply(columns, [{
                field: "documentTypeId",
                width: 150
            }]);
        }

        if (this.resourceManager.options.showDates) {
            columns.push.apply(columns, [{
                field: "fromDate"
            }, {
                field: "toDate"
            }]);
        }

        return columns;
    },

    // @override
    getGridFilter: function () {
        var gridFilter = []; // expresso.layout.resourcemanager.Grid.fn.getGridFilter.call(this);
        var resource = this.resourceManager.siblingResourceManager.currentResource;
        //console.log("resourceId: " + (resource ? resource.id : null));
        gridFilter.push.apply(gridFilter, [{
            field: "resourceId",
            operator: "eq",
            value: (resource ? resource.id : null)
        }, {
            field: "resourceName",
            operator: "eq",
            value: this.resourceManager.siblingResourceManager.getResourceName()
        }]);
        // console.log("document getGridFilter: " + JSON.stringify(gridFilter));
        return gridFilter;
    },

    // @override
    initGrid: function () {
        expresso.layout.resourcemanager.Grid.fn.initGrid.call(this);
        var _this = this;
        var $grid = this.$domElement;

        // register the drag&drop listener
        $grid.find(".exp-upload-button").each(function () {
            _this.createKendoUpload();
        });
    },

    // @override
    getToolbarButtons: function () {
        var toolbar = expresso.layout.resourcemanager.Grid.fn.getToolbarButtons.call(this);

        // add the default create
        if (this.isUserAllowed("create")) {
            var guid = expresso.util.Util.guid();
            this.addButtonToToolbar(toolbar,
                {template: "<button id='" + guid + "' type='button' class='k-button exp-button exp-creation-button exp-upload-button' title='uploadDocument'><span class='fa fa-download'><span data-text-key='uploadDocumentButton'></span></span></button><input name='file' type='file'>"},
                "exp-toolbar-marker-addition");
        }
        return toolbar;
    },

    /**
     * Create a Drag&Drop component
     */
    createKendoUpload: function () {
        var _this = this;

        var $fileDiv = this.$domElement.find(".k-grid-toolbar input[name=file]");

        // in order to avoid conflict with multiple grid with upload button
        // we need to use an unique ID (and not only the class)
        var buttonId = this.$domElement.find(".exp-upload-button").attr("id");

        $fileDiv.kendoUpload({
            async: {
                saveUrl: _this.resourceManager.getUploadDocumentPath(),
                removeUrl: null,
                autoUpload: true
            },
            multiple: false,
            showFileList: false,
            dropZone: "#" + buttonId,
            upload: function (e) {

                var data = {};

                // add the creation user
                data["creationUserId"] = expresso.Common.getUserInfo().id;

                // add the document meta data
                if (_this.resourceManager.siblingResourceManager && _this.resourceManager.siblingResourceManager.currentResource) {
                    data["resourceName"] = _this.resourceManager.siblingResourceManager.getResourceName();
                    data["resourceId"] = _this.resourceManager.siblingResourceManager.currentResource.id;
                }

                // add token if present
                if (expresso.Security) {
                    data["sessionToken"] = expresso.Security.getSessionToken();
                }

                //console.log("Upload data: " + JSON.stringify(data));
                e.data = data;

                expresso.util.UIUtil.showLoadingMask(_this.$domElement, true);
            },
            success: function (e) {
                //  refresh the resource
                // var updatedResource = e.response;
                // _this.resourceManager.sections.grid.updateResource(resource, updatedResource);
                _this.resourceManager.sections.grid.loadResources();
                expresso.util.UIUtil.showLoadingMask(_this.$domElement, false);
            },
            error: function (e) {
                expresso.util.UIUtil.showLoadingMask(_this.$domElement, false);
                expresso.Common.displayServerValidationMessage(e.XMLHttpRequest);
            }
        });

        this.kendoUpload = $fileDiv.data("kendoUpload");
    },

    // @override
    isCreatable: function () {
        if (this.resourceManager.siblingResourceManager && this.resourceManager.siblingResourceManager.currentResource &&
            this.resourceManager.siblingResourceManager.currentResource.id) {
            return $.Deferred().resolve(true);
        } else {
            return $.Deferred().resolve(false);
        }
    },

    // @override
    destroy: function () {
        if (this.kendoUpload) {
            this.kendoUpload.destroy();
            this.kendoUpload = null;
        }

        expresso.layout.resourcemanager.Grid.fn.destroy.call(this);
    }

});
