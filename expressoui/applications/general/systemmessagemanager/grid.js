expresso.applications.general.systemmessagemanager.Grid = expresso.layout.resourcemanager.Grid.extend({

    activeOnly: true,
    
    // @override
    getColumns: function () {
        var columns = [{
            field: "startDate"
        }, {
            field: "endDate"
        }, {
            field: "language",
            width: 100
        }, {
            field: "message"
        }];
        return columns;
    }
});
