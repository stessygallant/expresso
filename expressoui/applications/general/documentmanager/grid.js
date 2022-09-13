expresso.applications.general.documentmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
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
    // getMobileColumns: function () {
    //     return {
    //         mobileNumberFieldName: "fileName",
    //         mobileDescriptionFieldName: "description",
    //         mobileTopRightFieldName: "creationDate",
    //         mobileMiddleLeftFieldName: "creationUserFullName",
    //         mobileMiddleRightFieldName: null
    //     };
    // },

    // @override
    getMobileColumnTemplate: function () {
        var fields = this.resourceManager.model.fields;
        var mobileTemplate = [];
        mobileTemplate.push("<div class='mobile-grid-column'>");
        mobileTemplate.push("<a class='document' target='_blank' href='#=absolutePath?absolutePath:\"_\"#'>#=fileName#</a>");
        mobileTemplate.push("</div>");
        return mobileTemplate.join("");
    },

    // @override
    getGridFilter: function () {
        var gridFilter = []; // expresso.layout.resourcemanager.Grid.fn.getGridFilter.call(this);
        var resource = this.resourceManager.siblingResourceManager.currentResource;
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

        // register the drag&drop listener
        if (this.$domElement.find(".exp-upload-button").length) {
            this.createKendoUpload();
        }
    },

    // @override
    getToolbarButtons: function () {
        var toolbar = expresso.layout.resourcemanager.Grid.fn.getToolbarButtons.call(this);

        // add the default create
        if (this.isUserAllowed("create") && expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
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

        var expressoUpload = expresso.util.UIUtil.buildUpload(null, $fileDiv, {
            url: function () {
                return _this.resourceManager.getUploadDocumentPath(_this.resourceManager.siblingResourceManager);
            },
            customData: function () {
                return expresso.util.UIUtil.getDocumentUploadCustomData(_this.resourceManager.siblingResourceManager);
            },
            async: {
                autoUpload: true
            },
            dropZone: "#" + buttonId
        });

        expressoUpload.$deferred.done(function () {
            // refresh the resources
            _this.loadResources();
        });

        this.kendoUpload = expressoUpload.kendoUpload;
    },

    // @override
    getResourceUrl: function (id) {
        var url = expresso.layout.resourcemanager.ResourceManager.fn.getResourceUrl.call(this, id);
        // for kendo sync, add sibling info
        return this.addSiblingParams(url, this.siblingResourceManager);
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

