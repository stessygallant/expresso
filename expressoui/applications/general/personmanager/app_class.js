expresso.applications.general.personmanager.PersonManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = expresso.Security.getPersonFields();

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "person", fields, {
            preview: false
        });
    }
});
