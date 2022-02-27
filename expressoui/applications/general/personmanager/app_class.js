var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.general = expresso.applications.general || {};
expresso.applications.general.personmanager = expresso.applications.general.personmanager || {};

expresso.applications.general.personmanager.PersonManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        var fields = expresso.Security.getPersonFields();

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "person", fields, {
            form: true,
            grid: true,
            preview: false
        });
    }
});
