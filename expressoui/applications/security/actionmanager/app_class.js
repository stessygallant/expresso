var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.actionmanager = expresso.applications.security.actionmanager || {};

expresso.applications.security.actionmanager.ActionManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 20
            },
            description: {
                type: "string",
                transient: true,
                maxLength: 100
            },
            sortOrder: {
                type: "number",
                defaultValue: 1
            },
            systemAction: {
                type: "boolean"
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "action", fields, {
            grid: true,
            form: true
        });
    }
});
                                                                                                                                                                                                                                                                                                                                                                  