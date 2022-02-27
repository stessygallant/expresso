expresso.applications.security.jobtitlemanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 120
        }, {
            field: "description",
            width: 300
        }, {
            field: "jobTypeId",
            width: 160
        }, {
            field: "extKey",
            width: 250
        }, {}
        ];
    }
});

