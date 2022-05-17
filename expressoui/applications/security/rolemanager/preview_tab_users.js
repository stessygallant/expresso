expresso.applications.security.rolemanager.PreviewTabUsers = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);
    },

    // @override
    refresh: function (resource) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, resource);
        var _this = this;

        var $usersDiv = this.$domElement.find(".tab-users");
        $usersDiv.empty();
        if (resource && resource.id) {
            expresso.Common.sendRequest("user/inrole", null, null, "roleId=" + resource.id).done(function (users) {
                if (users.length) {
                    // sort
                    users.sort(function (u1, u2) {
                        return u1.fullName.localeCompare(u2.fullName);
                    });
                    $.each(users, function () {
                        var user = this;
                        $usersDiv.append("<div>" + user.fullName + "</div>");
                    });
                } else {
                    $usersDiv.append("<div>" + _this.getLabel("noUsers") + "</div>");
                }
            });
        }
    }
});

