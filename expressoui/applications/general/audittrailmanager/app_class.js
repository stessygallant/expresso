var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.general = expresso.applications.general || {};
expresso.applications.general.audittrailmanager = expresso.applications.general.audittrailmanager || {};

expresso.applications.general.audittrailmanager.AuditTrailManager = expresso.layout.resourcemanager.ResourceManager.extend({

    multipleSelectionEnabled: false,

    // @override
    init: function (applicationPath) {
        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "auditTrail", null, {
            grid: true,
            form: true
        });
    }
});
