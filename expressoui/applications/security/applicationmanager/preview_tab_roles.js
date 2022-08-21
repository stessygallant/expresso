expresso.applications.security.applicationmanager.PreviewTabRoles = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // get the roles (the section must wait until we get the list)
        var $rolesDiv = $domElement.find(".roles");
        this.addPromise(expresso.Common.sendRequest("role").done(function (roles) {
            // create a new input checkbox for each roles
            $.each(roles.data, function (index, value) {
                $rolesDiv.append(expresso.util.UIUtil.buildCheckBox("roles", value.description, value.id));
            });

            // save on select
            $rolesDiv.find("input").on("click", function () {
                var action = $(this).is(":checked") ? "create" : "delete";
                expresso.Common.sendRequest("application/" + _this.resourceManager.currentResource.id +
                    "/role/" + $(this).val(), action);
            });

        }));
    },

    // @override
    refresh: function (resource) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, resource);
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("application/role", "create");

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (resource && resource.id) {
            // get all roles for this application
            expresso.Common.sendRequest("application/" + resource.id + "/role").done(function (roles) {
                $.each(roles, function (index, value) {
                    $form.find("input[value=" + value.id + "]").prop("checked", true);
                });
            });
        }
        else {
            expresso.util.UIUtil.setFormReadOnly($form);
        }
    }
});

