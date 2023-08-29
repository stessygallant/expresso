expresso.applications.general.documenttypemanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "resourceName",
            width: 200
        }, {
            field: "pgmKey",
            width: 120
        }, {
            field: "sortOrder",
            width: 70
        }, {
            field: "description",
            width: 250
        }, {
            field: "deactivationDate",
            hidden: true
        }, {}];
        return columns;
    },

    // @override
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: "resourceName",
            mobileTopRightFieldName: "pgmKey",
            mobileMiddleLeftFieldName: null,
            mobileMiddleRightFieldName: null,
            mobileDescriptionFieldName: "description"
        };
    }
});
