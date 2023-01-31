expresso.applications.general.notification.notificationmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "user.fullName",
            width: 120
        }, {
            field: "resourceNo",
            width: 120
        }, {
            field: "requestedDate"
        }, {
            field: "serviceDescription",
            width: 150
        }, {
            field: "description",
            width: 500
        }];

        if (expresso.Common.isAdmin()) {
            columns.push.apply(columns, [{
                field: "resourceName",
                width: 120
            }, {
                field: "resourceId",
                width: 120
            }, {
                field: "resourceExtKey",
                width: 120
            }, {
                field: "resourceUrl",
                width: 200
            }, {
                field: "availableActions",
                width: 150
            }, {
                field: "notifiedUser.fullName",
                width: 160,
                hidden: true
            }, {
                field: "requesterUser.fullName",
                width: 160
            }, {
                field: "performedAction",
                width: 120
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
