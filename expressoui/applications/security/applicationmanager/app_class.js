var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.applicationmanager = expresso.applications.security.applicationmanager || {};

expresso.applications.security.applicationmanager.ApplicationManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 50
            },
            description: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            parameter: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            comments: {
                type: "string",
                nullable: true,
                maxLength: 16777216
            },
            ownerUserId: {
                type: "number",
                nullable: true,
                reference: "user"
            },
            departmentId: {
                type: "number",
                nullable: true,
                values: true
            },
            systemApplication: {
                type: "boolean"
            },
            internalOnly: {
                type: "boolean",
                defaultValue: true
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "application", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  