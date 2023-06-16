expresso.applications.security.rolemanager.PreviewTabPriv = expresso.layout.resourcemanager.PreviewTab.extend({

    multipleUpdatePromises: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);
        var _this = this;

        // save on select
        this.$domElement.on("click", ":checkbox[name]", function () {
            var action = $(this).is(":checked") ? "create" : "delete";
            _this.sendRequest("role/" + _this.resourceManager.currentResource.id + "/privilege/" + $(this).val(), action);
        });

        this.addPromise($.when(
            // get all applications
            _this.sendRequest("application"),

            // get all resources
            _this.sendRequest("resource"),

            // get all privileges (functions)
            _this.sendRequest("privilege")
        ).done(function (applications, resources, privileges) {
            // console.log(applications);
            // console.log(resources);
            // console.log(privileges);

            // build the tree structure
            $.each(applications.data, function (index, app) {
                app.resources = [];
                $.each(resources.data, function (index, res) {
                    res.privileges = [];
                    if (res.applicationId == app.id) {
                        app.resources.push(res);
                    }
                    $.each(privileges.data, function (index, priv) {
                        if (priv.resourceId == res.id) {
                            res.privileges.push(priv);
                        }
                    });
                });
            });

            // sort the applications
            applications.data.sort(function (a, b) {
                if (a.pgmKey < b.pgmKey) return -1;
                if (a.pgmKey > b.pgmKey) return 1;
                return 0;
            });

            // APPLICATIONS
            var $privDiv = $domElement.find(".priv");

            // create a new input checkbox for each app
            $.each(applications.data, function (index, app) {

                // for each application, create a new block
                var $appDiv = $("<fieldset><legend>" + app.pgmKey + " (<input type='checkbox'>Tous)</legend></fieldset>").appendTo($privDiv);

                // handle the check all
                $appDiv.find("legend :checkbox").on("click", function () {
                    var selectAll = $(this).is(":checked");

                    // toggle selection for all checkboxes
                    $appDiv.find(":checkbox[name]").each(function () {
                        if ((selectAll && !$(this).is(":checked")) ||
                            (!selectAll && $(this).is(":checked"))) {
                            $(this).trigger("click");
                        }
                    });
                });


                // RESOURCES

                // sort the resources
                app.resources.sort(function (a, b) {
                    if (a.label < b.label) return -1;
                    if (a.label > b.label) return 1;
                    return 0;
                });

                $.each(app.resources, function (index, resource) {

                    // each resource has one block
                    var $resDiv = $("<div><div class='resource-title'>" + resource.label + "</div><div class='action-div'></div></div>").appendTo($appDiv);
                    $resDiv = $resDiv.find(".action-div");

                    // PRIVILEGES

                    // sort the privileges to be always in the same order
                    resource.privileges.sort(function (a, b) {
                        return a.action.sortOrder - b.action.sortOrder;
                    });

                    // create a new input checkbox for each priv
                    $.each(resource.privileges, function (index, priv) {
                        var $cb = expresso.util.UIUtil.buildCheckBox("priv", priv.action.pgmKey, priv.id);
                        $resDiv.append($cb);
                    });
                });
            });
        }));
    },

    // @override
    refresh: function (role) {
        expresso.layout.resourcemanager.PreviewTab.fn.refresh.call(this, role);
        var _this = this;
        var $form = this.$domElement;

        // if user not allowed, disable button
        var allowed = expresso.Common.isUserAllowed("role/privilege", "create");

        // put back all input to be enabled (if allowed)
        $form.find("input").prop("disabled", !allowed);

        if (role && role.id) {
            if (role.pgmKey == "public") {
                // public role does not inherit from user
                // get all priv for this role
                _this.sendRequest("role/" + role.id + "/privilege").done(function (privs) {
                    $.each(privs, function (index, priv) {
                        $form.find("input[value=" + priv.id + "]").prop("checked", true).prop("disabled", !allowed);
                    });
                });
            } else {
                _this.sendRequest("role/" + this.resourceManager.ROLE_USER_ID + "/privilege").done(function (privs) {
                    $.each(privs, function (index, priv) {
                        $form.find("input[value=" + priv.id + "]").prop("checked", true).prop("disabled", true);
                    });
                });

                // get all priv for this role
                _this.sendRequest("role/" + role.id + "/privilege").done(function (privs) {
                    $.each(privs, function (index, priv) {
                        var $checkbox = $form.find("input[value=" + priv.id + "]");
                        $checkbox.prop("checked", true);
                        if (!allowed) {
                            $checkbox.prop("disabled", true);
                        }
                    });
                });
            }
        } else {
            expresso.util.UIUtil.setFormReadOnly($form);
        }
    }
});