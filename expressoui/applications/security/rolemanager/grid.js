expresso.applications.security.rolemanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 350
        },{
            field: "systemRole",
            width: 100
        }, {}
        ];
    },

    // @override
    getSort: function () {
        return [{
            field: "systemRole",
            dir: "desc"
        },{
            field: "pgmKey",
            dir: "asc"
        }];
    }
});

