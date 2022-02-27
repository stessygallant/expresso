expresso.applications.security.rolemanager.info.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 150
        }, {
            field: "description",
            width: 200
        }, {
            field: "infoType",
            width: 150
        }, {}
        ];
    }
});

