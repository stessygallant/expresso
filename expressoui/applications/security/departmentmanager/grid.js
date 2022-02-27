expresso.applications.security.departmentmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 120
        }, {
            field: "description",
            width: 250
        }, {
            field: "company.name",
            width: 150
        }, {
            field: "representativeUser.fullName",
            width: 150
        }, {
            field: "extKey",
            width: 150
        }, {}];
    }
});

