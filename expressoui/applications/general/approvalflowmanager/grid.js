expresso.applications.general.approvalflowmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "resource.name",
            width: 160
        }, {
            field: "approvalOrder",
            width: 80
        }, {
            field: "jobTitle.description",
            width: 250
        }, {
            field: "maxApprovalLimit",
            width: 100
        }, {
            field: "mandatory"
        }, {
            field: "allowedApprovalKeys",
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
            mobileNumberFieldName: "id",
            mobileTopRightFieldName: "resource.name",
            mobileMiddleLeftFieldName: "approvalOrder",
            mobileMiddleRightFieldName: "maxApprovalLimit",
            mobileDescriptionFieldName: "jobTitle.description"
        };
    }
});
