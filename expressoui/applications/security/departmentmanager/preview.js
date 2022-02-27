expresso.applications.security.departmentmanager.Preview = expresso.layout.resourcemanager.Preview.extend({
    getContents: function () {
        return [
            {title: "associatedRoles", contentUrl: "roles" },
            {title: "users", contentUrl: "users" }
        ];
    }
});

