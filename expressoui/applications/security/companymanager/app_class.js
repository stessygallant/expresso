var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.security = expresso.applications.security || {};
expresso.applications.security.companymanager = expresso.applications.security.companymanager || {};

expresso.applications.security.companymanager.CompanyManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "company"
            },
            extKey: {
                type: "string",
                maxLength: 100,
                nullable: true,
                unique: true
            },
            billingCode: {
                type: "string",
                maxLength: 100,
                nullable: true,
                unique: true
            },
            name: {
                type: "string",
                maxLength: 250
            },
            city: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            address: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            deactivationDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "company", fields, {
            grid: true,
            form: true
        });
    }
});
                                                                                                                                                                                                                                                                                                                                                                  