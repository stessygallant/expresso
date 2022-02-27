expresso.applications.security.rolemanager.PreviewTabUsers = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // get the users (the section must wait until we get the list)
        var $usersDiv = $domElement.find(".users");
        this.addPromise(expresso.Common.sendRequest("user").done(function (users) {
            // create a new input checkbox for each users
            $.each(users.data, function (index, value) {
                $usersDiv.append(expresso.util.UIUtil.buildCheckBox("users", value.fullName, value.id));
            });

            //  save on select
            // NOTE: deleting will try to remove a role directly from the user
            // if the role is provided from the deparment or the title, it will do nothing
            $usersDiv.find("input").on("click", function () {
                var action = $(this).is(":checked") ? "create" : "delete";
                expresso.Common.sendRequest("user/" + $(this).val() + "/role/" + _this.resourceManager.currentResource.id, action);
            });

        }));
    },

    // @override
    refresh: function (resource) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, resource);
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("user/role", "create");

        // verify if the user is allowed to modified reserved role
        if (resource && resource.systemRole && !expresso.Common.isUserAllowed("role", "create")) {
            allowed = false;
        }

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (resource && resource.id) {
            // get all users for this role
            expresso.Common.sendRequest("user/inrole", null, null, "roleId=" + resource.id).done(function (users) {
                $.each(users, function (index, value) {
                    $form.find("input[value=" + value.id + "]").prop("checked", true);
                });
            });
        }
        else {
            expresso.util.UIUtil.setFormReadOnly($form);
        }
    }
});

