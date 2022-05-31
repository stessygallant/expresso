expresso.applications.general.requiredapprovalmanager.RequiredApprovalManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "requiredApproval"
            },
            resourceName: {
                type: "string",
                maxLength: 100
            },
            resourceId: {
                type: "number"
            },
            resourceNo: {
                type: "string",
                maxLength: 100
            },
            resourceFieldName: {
                type: "string",
                maxLength: 50
            },
            oldValue: {
                type: "string",
                nullable: true,
                maxLength: 2000
            },
            newValue: {
                type: "string",
                nullable: true,
                maxLength: 2000
            },
            newValueReferenceId: {
                type: "number",
                nullable: true
            },
            requiredApprovalStatusId: {
                type: "number",
                values: true,
                defaultValue: "NEW"
            },
            approbationComment: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            approbationUserId: {
                type: "number",
                nullable: true,
                reference: "user"
            },
            approbationDate: {
                type: "date",
                nullable: true,
                timestamp: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "requiredApproval", fields, {
            preview: false
        });
    },

    // @override
    getAvailableActions: function () {
        return [{
            name: "approve",
            showButtonInGridToolbar: true
        }, {
            name: "reject",
            reasonRequested: true,
            showButtonInGridToolbar: true
        }];
    }
});
