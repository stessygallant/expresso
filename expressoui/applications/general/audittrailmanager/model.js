var expresso = expresso || {};
expresso.applications = expresso.applications || {};
expresso.applications.general = expresso.applications.general || {};
expresso.applications.general.audittrailmanager = expresso.applications.general.audittrailmanager || {};

expresso.applications.general.audittrailmanager.Model = {
    type: {
        type: "string",
        editable: false,
        defaultValue: "auditTrail"
    },
    resourceName: {
        type: "string",
        maxLength: 100,
        editable: false
    },
    resourceId: {
        type: "number",
        editable: false
    },
    resourceFieldName: {
        type: "string",
        maxLength: 50
    },
    oldValue: {
        type: "string",
        nullable: true,
        maxLength: 1000
    },
    newValue: {
        type: "string",
        nullable: true,
        maxLength: 1000
    },

    // TRANSIENT
    fmtResourceFieldName: {
        type: "string",
        transient: true,
        filterable: false
    },
    fmtChange: {
        type: "string",
        transient: true,
        filterable: false
    }
};