expresso.applications.security.jobtypemanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 120
        }, {
            field: "description",
            width: 300
        }, {}
        ];
    }
});

