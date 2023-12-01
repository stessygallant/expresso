expresso.applications.security.jobtitlemanager.info.Form = expresso.layout.resourcemanager.Form.extend({

    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);

        var _this = this;
        this.bindOnChange($window.find("[name=infoType]"), function () {
            var infoType = this.dataItem();
            if (infoType) {
                expresso.util.UIUtil.hideField($window.find(".input-value"));
                expresso.util.UIUtil.hideField($window.find("[name='default" + infoType.id.capitalize() + "']"), false);
            }
        });
    }
});
