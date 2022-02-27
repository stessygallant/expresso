expresso.applications.security.usermanager.Form = expresso.layout.resourcemanager.Form.extend({
    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);

        //var _this = this;

        if (resource.id) {
            if (!expresso.Common.isUserInRole("admin")) {
                expresso.util.UIUtil.setFieldReadOnly($window.find("[name=localAccount]"));
            }
        }

        if (!expresso.Common.isUserInRole("admin")) {
            expresso.util.UIUtil.hideField($window.find(".admin-only"));
            expresso.util.UIUtil.hideField($window.find("[name=password]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=nbrFailedAttempts]"));
        }

        if (!expresso.Common.isUserInRole("UserManager.admin")) {
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=companyId]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=departmentId]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=jobTitleId]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=managerId]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=username]"));

            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=extKey]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=nbrFailedAttempts]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=passwordExpirationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=terminationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=deactivationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=userCreationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=creationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=lastVisitDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=localAccount]"));
        }

        if (!expresso.Common.getSiteNamespace().config.Configurations.supportSSO) {
            expresso.util.UIUtil.hideField($window.find("[name=localAccount]"));
        }

        // create the username based on first and last name
        $window.find(":input[name]").on("change", function () {
            var userName = $window.find("[name=userName]").val();
            if (!userName) {
                var firstName = $window.find("[name=firstName]").val();
                var lastName = $window.find("[name=lastName]").val();
                if (firstName && lastName) {
                    userName = firstName.substring(0, 1).toLowerCase() + lastName.toLowerCase();

                    // remove accent
                    userName = userName.latinise();

                    // exclude all invalid characters
                    userName = userName.replace(/[^a-z0-9]/ig, '');

                    $window.find("[name=userName]").setval(userName);
                    resource.set("userName", userName);
                }
            }
        });
    }
});
