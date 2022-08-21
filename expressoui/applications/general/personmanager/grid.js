expresso.applications.general.personmanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    // @override
    getColumns: function () {
        var columns = [{
            field: "firstName",
            width: 140
        }, {
            field: "lastName",
            width: 140
        }, {
            field: "fullName",
            width: 200,
            hidden: true
        }, {
            field: "phoneNumber",
            width: 140
        }, {
            field: "email",
            width: 200
        }, {
            field: "managerId",
            width: 150,
            hidden: true
        }, {
            field: "department.description",
            width: 160
        }, {
            field: "company.name",
            width: 160
        }, {
            field: "jobTitle.description",
            width: 200
        }, {}];
        return columns;
    }
});
