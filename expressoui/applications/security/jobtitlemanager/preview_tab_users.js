expresso.applications.security.jobtitlemanager.PreviewTabUsers = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);
        this.loadSiblingResourceManager(eval(expresso.Common.getSiteName() + ".config.Applications.UserManager"),
            {masterIdProperty: "jobTitleId", activeOnly: true}
        );
    }
});

