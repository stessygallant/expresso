expresso.applications.security.usermanager.Form = expresso.layout.resourcemanager.Form.extend({
    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);

        var _this = this;

        if (resource.id) {
            if (!expresso.Common.isUserInRole("admin")) {
                expresso.util.UIUtil.setFieldReadOnly($window.find("[name=localAccount]"));
            }

            if (!resource.language) {
                var defaultLanguage = "fr";
                $window.find("[name=language]").setval(defaultLanguage);
                resource.set("language", defaultLanguage);
            }
        }

        if (!expresso.Common.isUserInRole("admin")) {
            expresso.util.UIUtil.hideField($window.find(".admin-only"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=nbrFailedAttempts]"));
        }

        if (!expresso.Common.isUserInRole("UserManager.admin")) {
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=userName]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=extKey]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=nbrFailedAttempts]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=passwordExpirationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=terminationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=deactivationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=userCreationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=creationDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=lastVisitDate]"));
            expresso.util.UIUtil.setFieldReadOnly($window.find("[name=localAccount]"));

            var managedJobTitleIds = expresso.Security.getUserInfo().jobTitle.managedJobTitleIds || [];
            if (!resource.id) {
                // new user: this user is allowed to create only certain user job titles
                _this.sendRequest("jobTitle", null, null, expresso.Common.buildKendoFilter({
                    field: "id",
                    operator: "in",
                    value: managedJobTitleIds.join(",")
                })).done(function (jobTitles) {
                    expresso.util.UIUtil.setDataSource($window.find("[name=jobTitleId]"), jobTitles, resource.jobTitleId);
                });
            } else {
                if (!resource.jobTitleId || !managedJobTitleIds.includes(resource.jobTitleId)) {
                    expresso.util.UIUtil.setFieldReadOnly($window.find("[name=jobTitleId]"));
                    expresso.util.UIUtil.setFieldReadOnly($window.find("[name=managerPersonId]"));
                    expresso.util.UIUtil.setFieldReadOnly($window.find("[name=companyId]"));
                    expresso.util.UIUtil.setFieldReadOnly($window.find("[name=departmentId]"));
                    expresso.util.UIUtil.hideField($window.find("[name=password]"));
                }
            }
        }

        if (!expresso.Common.getSiteNamespace().config.Configurations.localAccount) {
            expresso.util.UIUtil.hideField($window.find("[name=localAccount]"));
        }

        // if the database already contains the list of persons, we must simply create the user
        if (expresso.Common.getSiteNamespace().config.Configurations.supportPersonImportation) {
            // we must specify the person for the user
            if (resource.id) {
                expresso.util.UIUtil.hideField($window.find("[name=personId]"));
            } else {
                if (!expresso.Security.isAdmin()) {
                    expresso.util.UIUtil.hideField($window.find("[name=firstName]"));
                    expresso.util.UIUtil.hideField($window.find("[name=lastName]"));
                }
                this.bindOnChange($window.find("[name=personId]"), function () {
                    var person = this.dataItem();
                    if (person) {
                        $window.find("[name=firstName]").setval(person.firstName);
                        resource.set("firstName", person.firstName);

                        $window.find("[name=lastName]").setval(person.lastName);
                        resource.set("lastName", person.lastName);
                        _this.setUserName(resource);

                        // pull back some fields
                        $window.find("[name=companyId]").setval(person.companyId);
                        resource.set("companyId", person.companyId);

                        $window.find("[name=jobTitleId]").setval(person.jobTitleId);
                        resource.set("jobTitleId", person.jobTitleId);

                        $window.find("[name=departmentId]").setval(person.departmentId);
                        resource.set("departmentId", person.departmentId);

                        $window.find("[name=managerPersonId]").setval(person.managerPersonId);
                        resource.set("managerPersonId", person.managerPersonId);

                        $window.find("[name=email]").setval(person.email);
                        resource.set("email", person.email);

                        $window.find("[name=phoneNumber]").setval(person.phoneNumber);
                        resource.set("phoneNumber", person.phoneNumber);
                    }
                });
            }
        } else {
            expresso.util.UIUtil.hideField($window.find("[name=personId]"));
            if (!resource.id) {
                // create the username based on first and last name
                $window.find("[name=lastName],[name=firstName]").on("change", function () {
                    if (!resource.userName) {
                        _this.setUserName(resource);
                    }
                });
            }
        }
    },

    /**
     *
     * @param resource
     */
    setUserName: function (resource) {
        if (resource && resource.firstName && resource.lastName) {
            var userName = resource.firstName.substring(0, 1).toLowerCase() + resource.lastName.toLowerCase();

            // remove accent
            userName = userName.latinise();

            // exclude all invalid characters
            userName = userName.replace(/[^a-z0-9]/ig, '');

            this.$window.find("[name=userName]").setval(userName);
            resource.set("userName", userName);
        }
    },

    // @override
    getAdditionalRequiredFieldNames: function ($window, resource, action) {
        var requiredFieldNames = [];
        requiredFieldNames.push("email");
        requiredFieldNames.push("userName");
        if (!resource.id && expresso.Common.getSiteNamespace().config.Configurations.supportPersonImportation) {
            if (!expresso.Security.isAdmin()) {
                requiredFieldNames.push("personId");
            }
        }
        return requiredFieldNames;
    }
});
