expresso.applications.general.audittrailmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.Grid.fn.initDOMElement.call(this, $domElement);

        // when a resource is updated, update the grid
        var _this = this;
        this.resourceManager.siblingResourceManager.eventCentral.subscribeEvent(this.RM_EVENTS.RESOURCE_UPDATED, function () {
            _this.loadResources();
        });
    },

    // @override
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: "fmtResourceFieldName",
            mobileDescriptionFieldName: "fmtChange",
            mobileTopRightFieldName: "creationDate",
            mobileMiddleLeftFieldName: null,
            mobileMiddleRightFieldName: null
        };
    },

    // @override
    getColumns: function () {
        return [{
            field: "creationDate"
        }, {
            field: "creationUserFullName",
            width: 200
        }, {
            field: "fmtResourceFieldName",
            width: 200
        }, {
            field: "oldValue"
        }, {
            field: "newValue"
        }
        ];
    },

    // @override
    parseResponseItem: function (item) {
        item = expresso.layout.resourcemanager.Grid.fn.parseResponseItem.call(this, item);
        item.fmtResourceFieldName = (item.resourceFieldName ? this.resourceManager.getLabel(item.resourceFieldName) : "");
        item.fmtChange = item.oldValue + " -> " + item.newValue;
        return item;
    },

    // @override
    getGridFilter: function () {
        var gridFilter = []; // expresso.layout.resourcemanager.Grid.fn.getGridFilter.call(this);
        var resource = this.resourceManager.siblingResourceManager.currentResource;
        gridFilter.push.apply(gridFilter, [{
            field: "resourceId",
            operator: "eq",
            value: resource ? resource.id : null
        }, {
            field: "resourceName",
            operator: "eq",
            value: this.resourceManager.siblingResourceManager.getResourceName()
        }]);
        return gridFilter;
    }
});

