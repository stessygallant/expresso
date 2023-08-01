expresso.applications.security.usermanager.PreviewTabAudittrail = expresso.layout.resourcemanager.PreviewTab.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.PreviewTab.fn.initDOMElement.call(this, $domElement);

        var _this = this;
        this.loadSiblingResourceManager(expresso.Common.getSiteNamespace().config.Applications.AuditTrailManager).done(function (resourceManager) {
            resourceManager.labels = $.extend({}, resourceManager.labels, _this.resourceManager.labels);
        });
    }
});

