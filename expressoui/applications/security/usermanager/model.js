expresso.applications.security.usermanager.Model = {
    type: {
        type: "string",
        editable: false,
        defaultValue: "user"
    },
    userName: {
        type: "string",
        nullable: true,
        unique: true,
        maxLength: 50
    },
    password: {
        type: "string",
        nullable: true,
        maxLength: 100
    },
    extKey: {
        type: "string",
        nullable: true,
        //unique: true,
        maxLength: 100
    },
    note: {
        type: "string",
        nullable: true,
        maxLength: 1000
    },
    language: {
        type: "string",
        defaultValue: "fr",
        // nullable: true,
        maxLength: 2
    },
    localAccount: {
        type: "boolean",
        defaultValue: true
    },
    genericAccount: {
        type: "boolean"
    },
    passwordExpirationDate: {
        type: "date",
        nullable: true
    },
    lastVisitDate: {
        type: "date",
        nullable: true,
        timestamp: true
    },
    nbrFailedAttempts: {
        type: "number"
    },
    terminationDate: {
        type: "date",
        timestamp: true,
        nullable: true
    },
    userCreationDate: {
        type: "date",
        timestamp: true,
        nullable: true
    },

    // personId is not null when the user is created for an existing person
    personId: {
        type: "number",
        reference: {
            allowCreate: true
        },
        nullable: true
    },

    extendsModel: {
        model: "expresso.applications.general.personmanager.Model",
        nullable: true
    }
};

