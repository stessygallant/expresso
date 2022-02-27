var expresso = expresso || {}
expresso.applications = expresso.applications || {}
expresso.applications.general = expresso.applications.general || {}
expresso.applications.general.systemmessagemanager = expresso.applications.general.systemmessagemanager || {}

expresso.applications.general.systemmessagemanager.SystemMessageManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "systemMessage"
            },
            message: {
                type: "string",
                nullable: true,
                maxLength: 2048
            },
            startDate: {
                type: "date",
                timestamp: true
            },
            endDate: {
                type: "date",
                nullable: true,
                timestamp: true
            },
            language: {
                type: "string",
                nullable: true,
                maxLength: 2
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "systemMessage", fields, {
            form: true,
            grid: true,
            preview: false
        });
    }
});
