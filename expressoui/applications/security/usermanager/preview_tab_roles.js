expresso.applications.security.usermanager.PreviewTabRoles = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // get the roles (the section must wait until we get the list)
        var $rolesDiv = $domElement.find(".roles");

        // prevent form submit
        $domElement.find("form").on("submit", function (e) {
            e.preventDefault();
        });

        // show only role matching
        $domElement.find(".search").keyup(function () {
            var text = $(this).val().toLowerCase().latinise();
            $domElement.find("fieldset").filter(function () {
                $(this).toggle($(this).find("legend").text().toLowerCase().latinise().indexOf(text) > -1)
            });
        });

        this.addPromise(expresso.Common.sendRequest("role").done(function (roles) {
            // create a new input checkbox for each roles
            var $currentFieldSet = null;
            var currentApplicationName = null;
            $.each(roles.data, function (index, role) {
                var applicationName = role.label;
                if (applicationName.indexOf('.') != -1) {
                    applicationName = applicationName.substring(0, applicationName.indexOf('.'));
                }

                if (!currentApplicationName || currentApplicationName != applicationName) {
                    currentApplicationName = applicationName;
                    var applicationLabel = expresso.Common.getLabel(currentApplicationName,
                        expresso.Common.getSiteNamespace().config.menu.Labels, null, true);
                    applicationLabel = applicationLabel && applicationLabel.shortLabel ? applicationLabel.shortLabel : applicationLabel;
                    applicationLabel = applicationLabel ? " (" + applicationLabel + ")" : "";
                    $currentFieldSet = $("<fieldset><legend>" + applicationName + applicationLabel + "</legend></fieldset>").appendTo($rolesDiv);
                }
                $currentFieldSet.append(expresso.util.UIUtil.buildCheckBox("roles", role.label, role.id));
            });

            // save on select
            $rolesDiv.find("input").on("click", function () {
                var action = $(this).is(":checked") ? "create" : "delete";
                expresso.Common.sendRequest("user/" + _this.resourceManager.currentResource.id + "/role/" + $(this).val(), action);
            });
        }));
    },

    // @override
    refresh: function (resource) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, resource);
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("user/role", "create");

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (resource && resource.id) {

            // get all roles for this user

            var promises = [];

            /*
             *  get roles from the user role
             */

            // get roles for the jobtitle
            if (resource.jobTitleId) {
                promises.push(expresso.Common.sendRequest("jobtitle/" + resource.jobTitleId + "/role").done(function (roles) {
                    $.each(roles, function (index, value) {
                        // select it and the user cannot unselect it
                        $form.find("input[value=" + value.id + "]").prop("checked", true).prop("disabled", true);
                    });
                }));
            }

            // get roles for the jobtype
            // if (resource.jobTitle && resource.jobTitle.jobTypeId) {
            //     promises.push(expresso.Common.sendRequest("jobtype/" + resource.jobTitle.jobTypeId + "/role").done(function (roles) {
            //         $.each(roles, function (index, value) {
            //             // select it and the user cannot unselect it
            //             $form.find("input[value=" + value.id + "]").prop("checked", true).prop("disabled", true);
            //         });
            //     }));
            // }

            // get roles for the department
            if (resource.departmentId) {
                promises.push(expresso.Common.sendRequest("department/" + resource.departmentId + "/role").done(function (roles) {
                    $.each(roles, function (index, value) {
                        // select it and the user cannot unselect it
                        $form.find("input[value=" + value.id + "]").prop("checked", true).prop("disabled", true);
                    });
                }));
            }

            $.when.apply(null, promises).done(function () {
                expresso.Common.sendRequest("user/" + resource.id + "/role").done(function (roles) {
                    $.each(roles, function (index, value) {
                        var disabled = !allowed;
                        $form.find("input[value=" + value.id + "]").prop("checked", true).prop("disabled", disabled);
                    });
                });
            });
        }
    }
});

