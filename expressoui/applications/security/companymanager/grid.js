expresso.applications.security.companymanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        return [{
            field: "name",
            width: 200
        }, {
            field: "billingCode",
            width: 120
        }, {
            field: "city",
            width: 150
        }, {
            field: "address"
        }, {
            field: "extKey",
            width: 140
        }];
    }
});

