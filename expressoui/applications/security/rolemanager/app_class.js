var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.rolemanager = expresso.applications.security.rolemanager || {};

expresso.applications.security.rolemanager.RoleManager = expresso.layout.resourcemanager.ResourceManager.extend({

    ROLE_USER_ID: 1,

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "role"
            },
            systemRole: {
                type: "boolean"
            },
            sortOrder: {
                type: "number",
                defaultValue: 99
            },
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 50
            },
            description: {
                type: "string",
                transient: true,
                maxLength: 100
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "role", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  