var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.departmentmanager = expresso.applications.security.departmentmanager || {};

expresso.applications.security.departmentmanager.DepartmentManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            pgmKey: {
                type: "string",
                nullable: true,
                unique: true,
                maxLength: 30
            },
            extKey: {
                type: "string",
                nullable: true,
                unique: true,
                maxLength: 200
            },
            description: {
                type: "string",
                maxLength: 200
            },
            sortOrder: {
                type: "number",
                defaultValue: 1
            },
            representativeUserId: {
                type: "number",
                nullable: true,
                reference: "user"
            },
            companyId: {
                type: "number",
                nullable: true,
                reference: true
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "department", fields);
    }
});
                                                                                                                                                                                                                                                                                                                                                                  