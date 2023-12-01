expresso.applications.security.usermanager.info.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        return [{
            field: "roleInfo.pgmKey",
            width: 200
        }, {
            field: "jobTitleInfo.pgmKey",
            width: 200
        }, {
            field: "dateValue",
            width: 150
        }, {
            field: "numberValue",
            width: 150
        }, {
            field: "stringValue",
            width: 150
        }, {
            field: "textValue"
        }
        ];
    }
});

