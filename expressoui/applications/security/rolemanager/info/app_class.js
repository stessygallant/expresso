expresso.applications.security.rolemanager.info = expresso.applications.security.rolemanager.info || {};

expresso.applications.security.rolemanager.info.InfoManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "roleInfo"
            },
            roleId: {
                type: "number",
                reference: true
            },
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 20
            },
            description: {
                type: "string",
                maxLength: 200
            },
            infoType: {
                type: "string",
                maxLength: 50
            },
            defaultNumber: {
                type: "number",
                nullable: true
            },
            defaultString: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            defaultText: {
                type: "string",
                nullable: true,
                maxLength: 16777215
            },
            defaultDate: {
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
