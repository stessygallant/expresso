expresso.applications.security.rolemanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 200
        }, {
            field: "systemRole",
            width: 100,
            hidden: true
        }, {
            field: "userLabels",
            width: 140
        }, {
            field: "departmentLabels",
            width: 120
        }, {
            field: "jobTitleLabels",
            width: 250
        }, {
            field: "jobTypeLabels",
            width: 120
        }, {
            field: "applicationLabels",
            width: 200
        }, {
            field: "description",
            width: 300
        }, {}
        ];
    },

    // @override
    getSort: function () {
        return [{
            field: "systemRole",
            dir: "desc"
        }, {
            field: "pgmKey",
            dir: "asc"
        }];
    }
});

