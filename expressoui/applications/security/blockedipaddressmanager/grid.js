expresso.applications.security.blockedipaddressmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "ipAddress",
            width: 120
        }, {
            field: "notes",
            width: 400
        }, {
            field: "deactivationDate",
            hidden: true
        }, {}];
        return columns;
    },

    // @override
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: "ipAddress",
            mobileTopRightFieldName: "deactivationDate",
            mobileMiddleLeftFieldName: null,
            mobileMiddleRightFieldName: null,
            mobileDescriptionFieldName: "notes"
        };
    }
});
