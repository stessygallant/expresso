expresso.applications.general.personmanager.Model = {
    type: {
        type: "string",
        editable: false,
        defaultValue: "person"
    },
    firstName: {
        type: "string",
        maxLength: 50
    },
    lastName: {
        type: "string",
        maxLength: 50
    },
    fullName: {
        type: "string",
        transient: true
    },
    email: {
        type: "string",
        nullable: true,
        maxLength: 200
    },
    phoneNumber: {
        type: "string",
        nullable: true,
        maxLength: 25
    },
    companyId: {
        type: "number",
        nullable: true,
        reference: true
    },
    managerPersonId: {
        type: "number",
        nullable: true,
        reference: "user"
    },
    jobTitleId: {
        type: "number",
        nullable: true,
        reference: true
    },
    departmentId: {
        type: "number",
        nullable: true,
        reference: true
    },
    deactivationDate: {
        type: "date",
        nullable: true
    }

};