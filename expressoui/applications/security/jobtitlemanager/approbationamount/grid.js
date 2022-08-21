expresso.applications.security.jobtitlemanager.approbationamount.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        return [{
            field: "resource.description",
            width: 200
        }, {
            field: "approbationAmount",
            width: 200
        }, {}
        ];
    }
});

