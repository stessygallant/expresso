expresso.applications.general.documentmanager.Form = expresso.layout.resourcemanager.Form.extend({
    deprecatedFileUploadSupport: true,

    // @override
    initForm: function ($window, resource) {
        expresso.layout.resourcemanager.Form.fn.initForm.call(this, $window, resource);

        // hide if not document types are defined for the resource
        if (!(this.resourceManager.model.fields["documentTypeId"].values.data &&
            this.resourceManager.model.fields["documentTypeId"].values.data.length > 0)) {
            expresso.util.UIUtil.hideField($window.find("[name=documentTypeId]"));
        }

        if (!resource.id) {
            //console.log("res",this.resourceManager.siblingResourceManager.currentResource);
            resource.set("resourceName", this.resourceManager.siblingResourceManager.getResourceName());
            resource.set("resourceId", this.resourceManager.siblingResourceManager.currentResource.id);
        }

        if (!this.resourceManager.options.showDates) {
            expresso.util.UIUtil.hideField($window.find("[name=fromDate]"));
            expresso.util.UIUtil.hideField($window.find("[name=toDate]"));
        }
    }
});
