expresso.applications.security.blockedipaddressmanager.BlockedIPAddressManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "blockedIPAddress"
            },
            ipAddress: {
                type: "string",
                maxLength: 20
            },
            notes: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            deactivationDate: {
                type: "date",
                nullable: true,
                timestamp: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "blockedIPAddress", fields, {
            preview: false
        });
    }
});
