expresso.applications.general.requiredapprovalmanager.Form = expresso.layout.resourcemanager.Form.extend({

    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);

        if (!resource.approbationDate) {
            expresso.util.UIUtil.hideField($window.find(".approbation"));
        }
    }
});
