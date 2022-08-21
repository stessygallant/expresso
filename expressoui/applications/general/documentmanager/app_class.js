expresso.applications.general.documentmanager.DocumentManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var _this = this;
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "document"
            },
            resourceId: {
                type: "number"
            },
            resourceName: {
                type: "string",
                maxLength: 100
            },
            fileName: {
                type: "string",
                anchor: true
            },
            absolutePath: {
                type: "string",
                nullable: true
            },
            description: {
                type: "string",
                nullable: true,
                maxLength: 2000
            },
            documentTypeId: {
                type: "number",
                nullable: true,
                values: {
                    resourcePath: "documentType",
                    filter: function () {
                        return {
                            field: "resourceName",
                            operator: "eq",
                            value: _this.siblingResourceManager.getResourceName()
                        }
                    }
                }
            },
            fromDate: {
                type: "date",
                nullable: true
            },
            toDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "document", fields, {
            preview: false
        });
    }
});
