expresso.applications.security.resourcemanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "applicationName",
            width: 250,
            hidden: true
        }, {
            field: "name",
            width: 200
        }, {
            field: "masterResource.securityPath",
            filterable: false,
            sortable: false,
            width: 200
        }, {
            field: "path",
            width: 200
        }, {
            field: "master"
        }, {}
        ];
    },

    // @override
    getGroup: function () {
        return {
            field: "applicationName"
        };
    }
});

