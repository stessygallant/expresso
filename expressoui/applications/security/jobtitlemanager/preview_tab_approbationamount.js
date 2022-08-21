expresso.applications.security.jobtitlemanager.PreviewTabApprobationamount = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        this.loadSubResourceManager(eval(expresso.Common.getSiteName() + ".config.Applications.JobTitleManager.ApprobationAmountManager"));
    }
});

