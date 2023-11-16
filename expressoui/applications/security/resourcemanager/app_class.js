expresso.applications.security.resourcemanager.ResourceManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "resource"
            },
            master: {
                type: "boolean"
            },
            masterResourceId: {
                type: "number",
                nullable: true,
                reference: "resource"
            },
            applicationId: {
                type: "number",
                // nullable: true,
                reference: true
            },
            name: {
                type: "string",
                maxLength: 100
            },
            path: {
                type: "string",
                maxLength: 100
            },
            deactivationDate: {
                type: "date",
                nullable: true
            },

            applicationName: {
                type: "string",
                transient: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "resource", fields);
    },

    // @override
    getAvailableActions: function () {
        var _this = this;
        return [
            {
                name: "publish",
                icon: "fa-folder-open",
                showButtonInGridToolbar: true,
                resourceCollectionAction: true,
                performAction: function () {
                    // return _this.sendRequest("resource", "publish", $.param({id: _this.getCurrentResourceId()}));
                    // TO DO how to get the files?
                    // window.open();
                    return $.Deferred().resolve();
                }
            }
        ];
    }
});
                                                                                                                                                                                                                                                                                                                                                                  