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
                maxLength: 100,
                updatable: false
            },
            resourceId: {
                type: "number",
                updatable: false
            },
            resourceNo: {
                type: "string",
                maxLength: 100,
                updatable: false
            },
            resourceDescription: {
                type: "string",
                maxLength: 2000,
                nullable: true,
                updatable: false
            },
            additionnalInfo: {
                type: "string",
                maxLength: 2000,
                nullable: true,
                updatable: false
            },
            resourceFieldName: {
                type: "string",
                maxLength: 50,
                updatable: false
            },
            oldValue: {
                type: "string",
                nullable: true,
                maxLength: 2000,
                updatable: false
            },
            newValue: {
                type: "string",
                nullable: true,
                maxLength: 2000,
                updatable: false
            },
            notes: {
                type: "string",
                nullable: true,
                maxLength: 2000
            },
            newValueReferenceId: {
                type: "number",
                nullable: true,
                updatable: false
            },
            requiredApprovalStatusId: {
                type: "number",
                values: true,
                defaultValue: "NEW"
            },
            approbationComment: {
                type: "string",
                nullable: true,
                maxLength: 1000,
                updatable: false
            },
            approbationUserId: {
                type: "number",
                nullable: true,
                reference: "user",
                updatable: false
            },
            approbationDate: {
                type: "date",
                nullable: true,
                timestamp: true,
                updatable: false
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
