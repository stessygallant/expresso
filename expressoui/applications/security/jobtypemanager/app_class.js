var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.jobtypemanager = expresso.applications.security.jobtypemanager || {};

expresso.applications.security.jobtypemanager.JobTypeManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "jobType"
            },
            sortOrder: {
                type: "number",
                defaultValue: 1
            },
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 20
            },
            description: {
                type: "string",
                maxLength: 50
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "jobType", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  