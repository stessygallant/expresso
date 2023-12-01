expresso.applications.security.jobtitlemanager.PreviewTabInfo = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);
        this.loadSubResourceManager(expresso.Common.getSiteNamespace().config.Applications.JobTitleManager.InfoManager);
    }
});

