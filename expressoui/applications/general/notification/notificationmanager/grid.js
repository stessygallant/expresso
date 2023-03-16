expresso.applications.general.notification.notificationmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "requesterUser.fullName",
            width: 160
        }, {
            field: "user.fullName",
            width: 140
        }, {
            field: "notifiedUser.fullName",
            width: 160
        }, {
            field: "resourceNo",
            width: 200
        }, {
            field: "requestedDate"
        }, {
            field: "serviceDescription",
            width: 150
        }, {
            field: "description",
            width: 500,
            hidden: true
        }];

        if (expresso.Common.isAdmin()) {
            columns.push.apply(columns, [{
                field: "resourceName",
                width: 120,
                hidden: true
            }, {
                field: "resourceId",
                width: 120,
                hidden: true
            }, {
                field: "resourceExtKey",
                width: 120,
                hidden: true
            }, {
                field: "resourceUrl",
                width: 200,
                hidden: true
            }, {
                field: "resourceStatusPgmKey",
                width: 200
            }, {
                field: "availableActions",
                width: 150,
                hidden: true
            }, {
                field: "notes",
                width: 150,
                hidden: true
            }, {
                field: "performedAction",
                width: 100
            }, {
                field: "performedActionDate"
            }, {
                field: "performedActionUser.fullName",
                width: 160
            }, {
                field: "notifiableServiceClassName",
                width: 300,
                hidden: true
            }]);
        }

        columns.push({});

        return columns;
    }
});
