expresso.applications.general.approvalflowmanager.ApprovalFlowManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "approvalFlow"
            },
            resourceId: {
                type: "number",
                reference: true
            },
            jobTitleId: {
                type: "number",
                reference: true
            },
            approvalOrder: {
                type: "number",
                defaultValue: 1,
                decimals: 0
            },
            maxApprovalLimit: {
                type: "number",
                nullable: true,
                defaultValue: null,
                decimals: 0
            },
            allowedApprovalKeys: {
                type: "string",
                nullable: true
            },
            mandatory: {
                type: "boolean"
            },
            deactivationDate: {
                type: "date",
                nullable: true,
                timestamp: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "approvalFlow", fields, {
            preview: false
        });
    }
});
