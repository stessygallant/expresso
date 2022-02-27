expresso.applications.security.rolemanager.PreviewTabApp = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;
        var $appDiv = $domElement.find(".app");
        this.addPromise(expresso.Common.sendRequest("application").done(function (apps) {
            // create a new input checkbox for each app
            $.each(apps.data, function (index, app) {
                var $cb = expresso.util.UIUtil.buildCheckBox("app", app.pgmKey, app.id);
                $appDiv.append($cb);
            });

            // save on select
            $appDiv.find(":checkbox[name]").on("click", function () {
                var action;
                if ($(this).is(":checked")) {
                    action = "create";
                }
                else {
                    action = "delete";
                }

                expresso.Common.sendRequest("role/" + _this.resourceManager.currentResource.id +
                    "/application/" + $(this).val(), action).done(function () {
                    _this.publishEvent(_this.RM_EVENTS.RESOURCE_UPDATED, _this.resourceManager.currentResource);
                });
            });
        }));
    },

    // @override
    refresh: function (role) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, role);
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("role/application", "create");

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (role && role.id) {
            // get all app for role=user
            expresso.Common.sendRequest("role/" + this.resourceManager.ROLE_USER_ID + "/application").done(function (apps) {
                $.each(apps, function (index, app) {
                    $form.find("input[value=" + app.id + "]").prop("checked", true).prop("disabled", true);
                });

                // get all app for this role
                expresso.Common.sendRequest("role/" + role.id + "/application").done(function (apps) {
                    $.each(apps, function (index, app) {
                        $form.find("input[value=" + app.id + "]").prop("checked", true).prop("disabled", !allowed);
                    });
                });
            });
        }
        else {
            expresso.util.UIUtil.setFormReadOnly($form);
        }
    }
});