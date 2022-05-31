expresso.applications.security.usermanager.info = expresso.applications.security.usermanager.info || {};

expresso.applications.security.usermanager.info.InfoManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "userInfo"
            },
            userId: {
                type: "number",
                reference: true
            },
            roleInfoId: {
                type: "number",
                reference: {
                    resourceName: "roleInfo",
                    resourcePath: "role/0/info"
                }
            },
            numberValue: {
                type: "number",
                nullable: true
            },
            stringValue: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            textValue: {
                type: "string",
                nullable: true,
                maxLength: 16777215
            },
            dateValue: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "info", fields, {
            grid: true,
            form: true
        });
    }
});
