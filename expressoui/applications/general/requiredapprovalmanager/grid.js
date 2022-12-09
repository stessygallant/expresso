expresso.applications.general.requiredapprovalmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    activeOnly: true,

    // @override
    getColumns: function () {

        // if there is a resourceName in the filter, then we can use the manager
        var resourceManagerDef;
        if (this.resourceManager.options.filter && this.resourceManager.options.filter.resourceName) {
            resourceManagerDef = this.resourceManager.options.filter.resourceName.capitalize() + "Manager";
        }
        var columns = [{
            field: "requiredApprovalStatusId",
            width: 100
        }, {
            field: "resourceName",
            width: 120,
            hidden: true
        }, {
            field: "resourceId",
            width: 90,
            hidden: true
        }, {
            field: "resourceNo",
            width: 90,
            reference: {
                fieldName: "resourceId",
                resourceManagerDef: resourceManagerDef
            }
        }, {
            field: "resourceDescription",
            width: 250
        }, {
            field: "resourceFieldName",
            width: 200,
            hidden: true
        }, {
            field: "resourceFieldName",
            width: 200,
            template: "#= fmtResourceFieldName #",
            filterable: false
        }, {
            field: "oldValue",
            width: 150
        }, {
            field: "newValue",
            width: 150
        }, {
            field: "creationDate"
        }, {
            field: "creationUserFullName",
            width: 150
        }, {
            field: "notes",
            width: 300
        }, {
            field: "approbationUser.fullName",
            width: 160
        }, {
            field: "approbationDate"
        }, {
            field: "approbationComment",
            width: 300
        }, {}];
        return columns;
    },

    // @override
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: null,
            mobileDescriptionFieldName: null,
            mobileTopRightFieldName: null,
            mobileMiddleLeftFieldName: null,
            mobileMiddleRightFieldName: null
        };
    },

    // @override
    parseResponseItem: function (item) {
        item = expresso.layout.resourcemanager.Grid.fn.parseResponseItem.call(this, item);
        item.fmtResourceFieldName = this.resourceManager.getLabel(item.resourceFieldName); // + " (" + item.resourceFieldName + ")";
        return item;
    },

    // @override
    initGrid: function () {
        expresso.layout.resourcemanager.Grid.fn.initGrid.call(this);

        // approbation button group
        var _this = this;
        var $approbationButtonGroup = this.$domElement.find(".approbation-group");
        if ($approbationButtonGroup.length) {
            $approbationButtonGroup.kendoButtonGroup({
                select: function () {
                    _this.loadResources();
                }
            }).data("kendoButtonGroup").select(0);
        }
    },

    // @override
    getToolbarButtons: function () {
        var toolbar = expresso.layout.resourcemanager.Grid.fn.getToolbarButtons.call(this);
        if (this.isUserAllowed("approve")) {
            this.addSeparatorToToolbar(toolbar);
            toolbar.push({template: "<ul class='approbation-group'><li>" + this.getLabel('allModifications') + "</li><li>" + this.getLabel('mineOnlyModifications') + "</li></ul>"});
        }
        return toolbar;
    },

    // @override
    getGridFilter: function () {
        var gridFilter = expresso.layout.resourcemanager.Grid.fn.getGridFilter.call(this);

        var $approbationButtonGroup = this.$domElement.find(".approbation-group");
        if ($approbationButtonGroup.length) {
            if ($approbationButtonGroup.data("kendoButtonGroup").current().index() == 1) { // mine only
                expresso.Common.addKendoFilter(gridFilter, {mine: true});
            }
        }
        return gridFilter;
    }
});
