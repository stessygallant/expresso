expresso.applications.security.usermanager.UserManager = expresso.layout.resourcemanager.ResourceManager.extend({
    // @override
    init: function (applicationPath) {
        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "user",
            "expresso.applications.security.usermanager.Model");
    },

    // @override
    getAvailableActions: function () {
        return [{
            name: "unlock",
            showButtonInGridToolbar: true
            // }, {
            //     name: "send",
            //     showButtonInGridToolbar: true,
            //     showButtonInForm: false,
            //     icon: "fa-handshake-o"
        }];
    }
});
