expresso.applications.general.systemmessagemanager.Form = expresso.layout.resourcemanager.Form.extend({

    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);
    }
});
