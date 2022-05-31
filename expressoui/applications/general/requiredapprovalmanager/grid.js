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
            width: 110
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
            field: "fmtResourceFieldName",
            width: 160
        }, {
            field: "oldValue",
            width: 250
        }, {
            field: "newValue",
            width: 250
        }, {
            field: "approbationUser.fullName",
            width: 160,
            hidden: true
        }, {
            field: "approbationDate",
            hidden: true
        }, {
            field: "approbationComment",
            width: 300,
            hidden: true
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
        item.fmtResourceFieldName = (item.resourceFieldName ? this.resourceManager.getLabel(item.resourceFieldName) : "");
        return item;
    }
});
