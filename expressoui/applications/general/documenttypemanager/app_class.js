expresso.applications.general.documenttypemanager.DocumentTypeManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "documentType"
            },
            resourceName: {
                type: "string",
                nullable: true,
                maxLength: 100
            },
            sortOrder: {
                type: "number",
                defaultValue: 1,
                decimals: 0
            },
            pgmKey: {
                type: "string",
                unique: false,
                maxLength: 50
            },
            description: {
                type: "string",
                nullable: true,
                maxLength: 50
            },
            deactivationDate: {
                type: "date",
                nullable: true
            },
            confidential: {
                type: "boolean",
                defaultValue: false
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "documentType", fields, {
            preview: false
        });
    }
});
