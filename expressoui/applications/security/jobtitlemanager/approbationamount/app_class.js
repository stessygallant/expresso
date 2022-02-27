expresso.applications.security.jobtitlemanager.approbationamount = expresso.applications.security.jobtitlemanager.approbationamount || {};

expresso.applications.security.jobtitlemanager.approbationamount.ApprobationAmountManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "jobTitleApprobationAmount"
            },
            jobTitleId: {
                type: "number"
            },
            approbationAmount: {
                type: "number",
                decimals: 2
            },
            resourceId: {
                type: "number",

                reference: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "approbationAmount", fields, {
            grid: true,
            form: true
        });
    }
});
