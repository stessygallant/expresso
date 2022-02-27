expresso.applications.security.usermanager.PreviewTabInfo = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);
        this.loadSubResourceManager(expresso.Common.getSiteNamespace().config.Applications.UserManager.InfoManager);
    }
});

