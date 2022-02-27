expresso.applications.security.rolemanager.Preview = expresso.layout.resourcemanager.Preview.extend({
    // @override
    getContents: function () {
        var contents = [{title: "users", contentUrl: "users"},
            {title: "applications", contentUrl: "app"},
            {title: "privileges", contentUrl: "priv"}];

        if (expresso.Common.isUserInRole("RoleManager.admin")) {
            contents.push({title: "info", contentUrl: "info"});
        }

        return contents;
    }
});
