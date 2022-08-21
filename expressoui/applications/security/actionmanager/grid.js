expresso.applications.security.actionmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "sortOrder",
            width: 110
        }, {
            field: "pgmKey",
            width: 200
        }, {
            field: "systemAction"
        }, {}
        ];
    }
});

