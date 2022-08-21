expresso.applications.security.jobtitlemanager.Preview = expresso.layout.resourcemanager.Preview.extend({
    getContents: function () {
        var contents = [];

        contents.push.apply(contents, [
            {title: "associatedRoles", contentUrl: "roles"}]);

        if (expresso.Security.isUserInRole("JobTitleManager.admin")) {
            contents.push.apply(contents, [
                {title: "approbationAmounts", contentUrl: "approbationamount"},
                {title: "users", contentUrl: "users"}]);
        }

        return contents;
    }
});
