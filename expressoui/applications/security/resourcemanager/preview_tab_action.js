expresso.applications.security.resourcemanager.PreviewTabAction = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // get the action (the section must wait until we get the list)
        var $actionDiv = $domElement.find(".actions");
        this.addPromise(expresso.Common.sendRequest("action").done(function (action) {
            // create a new input checkbox for each action
            $.each(action.data, function (index, value) {
                $actionDiv.append(expresso.util.UIUtil.buildCheckBox("action", value.description, value.id));
            });

            // save on select
            $actionDiv.find("input").on("click", function () {
                var $checkbox = $(this);
                if ($checkbox.is(":checked")) {
                    expresso.Common.sendRequest("privilege", "create", {
                        type: "privilege",
                        resourceId: _this.resourceManager.currentResource.id,
                        actionId: $checkbox.val()
                    }).done(function (priv) {
                        // keep the id
                        $checkbox.data("priv-id", priv.id);
                    });
                }
                else {
                    expresso.Common.sendRequest("privilege/" + $checkbox.data("priv-id"), "delete");
                }
            });

        }));
    },

    // @override
    refresh: function (resource) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, resource);
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("privilege", "create");

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (resource && resource.id) {
            // get all actions for this resource
            var queryString = expresso.Common.buildKendoFilter({"resourceId": resource.id});
            expresso.Common.sendRequest("privilege", null, null, queryString).done(function (privs) {
                $.each(privs.data, function (index, value) {
                    // check all actions
                    $form.find("input[value=" + value.actionId + "]").prop("checked", true).data("priv-id", value.id);
                });
            });
        }
        else {
            expresso.util.UIUtil.setFormReadOnly($form);
        }
    }
});

