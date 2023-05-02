expresso.applications.general.personmanager.PersonManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "person",
            "expresso.applications.general.personmanager.Model", {
                preview: false
            });
    }
});
