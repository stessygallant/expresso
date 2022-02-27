expresso.applications.security.usermanager.Preview = expresso.layout.resourcemanager.Preview.extend({
    // @override
    getContents: function () {
        var contents = [];

        if (expresso.Common.isUserInRole("UserManager.admin")) {
            contents.push({title: "roles", contentUrl: "roles"});
        }

        if (expresso.Common.isUserInRole("admin")) {
            contents.push({title: "infos", contentUrl: "info"});
        }

        return contents;
    }
});

