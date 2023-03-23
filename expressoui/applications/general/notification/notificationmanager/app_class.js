expresso.applications.general.notification.notificationmanager.NotificationManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "notification"
            },
            resourceName: {
                type: "string",
                maxLength: 200
            },
            resourceId: {
                type: "number",
                nullable: true
            },
            resourceTitle: {
                type: "string",
                maxLength: 200
            },
            resourceExtKey: {
                type: "string",
                nullable: true,
                unique: true,
                maxLength: 200
            },
            resourceUrl: {
                type: "string",
                nullable: true,
                maxLength: 2000
            },
            description: {
                type: "string",
                maxLength: 2000
            },
            resourceStatusPgmKey: {
                type: "string",
                maxLength: 50
            },
            notes: {
                type: "string",
                maxLength: 200
            },
            serviceDescription: {
                type: "string",
                maxLength: 100
            },
            availableActions: {
                type: "string",
                nullable: true,
                maxLength: 200
            },
            userId: {
                type: "number",
                reference: true
            },
            notifiedUserId: {
                type: "number",
                reference: "user"
            },
            requestedDate: {
                type: "date",
                nullable: true
            },
            requesterUserId: {
                type: "number",
                nullable: true,
                reference: "user"
            },
            performedAction: {
                type: "string",
                nullable: true,
                maxLength: 20
            },
            performedActionUserId: {
                type: "number",
                nullable: true,
                reference: "user"
            },
            performedActionDate: {
                type: "date",
                nullable: true,
                timestamp: true
            },
            notifiableServiceClassName: {
                type: "string",
                maxLength: 1000
            },
            deactivationDate: {
                type: "date",
                nullable: true,
                timestamp: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "notification", fields, {
            preview: false
        });
    }
});
