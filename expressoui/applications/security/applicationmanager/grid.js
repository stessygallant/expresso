expresso.applications.security.applicationmanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    // @override
    getColumns: function () {
        return [{
            field: "pgmKey",
            width: 250
        },{
            field: "systemApplication"
        }, {
            field: "internalOnly"
        }, {
            field: "description",
            width: 250
        }, {
            field: "parameter",
            width: 250
        }, {
            field: "departmentId",
            width: 150
        }, {
            field: "ownerUser.fullName",
            width: 150
        }, {
            field: "comments"
        }];
    }
});

