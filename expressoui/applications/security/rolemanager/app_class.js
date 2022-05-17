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
            },

            /**
             * ManyToMany
             */
            departmentIds: {
                multipleSelection: true,
                reference: "department",
                nullable: true
            },
            userIds: {
                multipleSelection: true,
                reference: "user",
                nullable: true
            },
            jobTitleIds: {
                multipleSelection: true,
                reference: "jobTitle",
                nullable: true
            },
            jobTypeIds: {
                multipleSelection: true,
                reference: "jobType",
                nullable: true
            },
            applicationIds: {
                multipleSelection: true,
                reference: "application",
                nullable: true
            },
            privilegeIds: {
                multipleSelection: true,
                reference: "privileges",
                nullable: true
            },

            /**
             * Labels
             */
            departmentLabels: {
                type: "string",
                transient: true
            },
            userLabels: {
                type: "string",
                transient: true
            },
            jobTitleLabels: {
                type: "string",
                transient: true
            },
            jobTypeLabels: {
                type: "string",
                transient: true
            },
            applicationLabels: {
                type: "string",
                transient: true
            },
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "role", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  