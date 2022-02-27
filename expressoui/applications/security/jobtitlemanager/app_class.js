var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.jobtitlemanager = expresso.applications.security.jobtitlemanager || {};

expresso.applications.security.jobtitlemanager.JobTitleManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "jobTitle"
            },
            jobTypeId: {
                type: "number",
                nullable: true,
                values: true
            },
            extKey: {
                type: "string",
                nullable: true,
                unique: true,
                maxLength: 200
            },
            sortOrder: {
                type: "number",
                defaultValue: 1
            },
            pgmKey: {
                type: "string",
                nullable: true,
                unique: true,
                maxLength: 50
            },
            description: {
                type: "string",
                maxLength: 200
            },
            deactivationDate: {
                type: "date",
                nullable: true
            },
            managedJobTitleIds: {
                multipleSelection: true,
                nullable: true,
                reference: {
                    resourceName: "jobTitle"
                }
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "jobTitle", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  