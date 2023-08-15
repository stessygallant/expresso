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
                        var resourceName;
                        if (_this.siblingResourceManager) {
                            resourceName = _this.siblingResourceManager.getResourceName();
                        } else if (expresso.util.Util.getUrlParameter("resourceName")) {
                            resourceName = expresso.util.Util.getUrlParameter("resourceName");
                        } else {
                            resourceName = null;
                        }

                        return {
                            field: "resourceName",
                            operator: "eq",
                            value: resourceName
                        }
                    }
                }
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "document", fields, {
            preview: false
        });
    }
});
