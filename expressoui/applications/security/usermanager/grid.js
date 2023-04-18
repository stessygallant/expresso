expresso.applications.security.usermanager.Grid = expresso.layout.resourcemanager.Grid.extend({
    serverSideDuplicate: false,

    // @override
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: "userName",
            mobileDescriptionFieldName: "fullName",
            mobileTopRightFieldName: "email",
            mobileMiddleLeftFieldName: "userCreationDate",
            mobileMiddleRightFieldName: "lastVisitDate"
        };
    },

    // @override
    getColumns: function () {
        var admin = expresso.Common.isUserInRole("UserManager.admin");

        return [{
            field: "id",
            width: 100,
            hidden: true
        }, {
            field: "lastName",
            width: 120
        }, {
            field: "firstName",
            width: 110
        }, {
            field: "fullName",
            width: 150,
            hidden: true
        }, {
            field: "userName",
            width: 130
        }, {
            field: "userCreationDate"
        }, {
            field: "lastVisitDate"
        }, {
            field: "localAccount",
            hidden: !admin
        }, {
            field: "genericAccount",
            hidden: !admin
        }, {
            field: "jobTitle.description",
            width: 200
        }, {
            field: "department.description",
            width: 120
        }, {
            field: "managerId",
            width: 150,
            hidden: true
        }, {
            field: "company.name",
            width: 150
        }, {
            field: "email",
            width: 300
        }, {
            field: "nbrFailedAttempts",
            width: 100
        }, {
            field: "terminationDate"
        }, {
            field: "note",
            width: 300
        }, {
            // PATCH to avoid autofill in the last column
            field: "unused",
            width: 1
        }, {}
        ];
    },

    // @override
    getSort: function () {
        return {
            field: "userCreationDate",
            dir: "desc"
        };
    },

    // @override
    initializeDuplicatedResource: function (duplicatedResource) {
        duplicatedResource.firstName = null;
        duplicatedResource.lastName = null;
        duplicatedResource.userName = null;
        duplicatedResource.password = null;

        duplicatedResource.extKey = null;
        duplicatedResource.nbrFailedAttempts = 0;
        duplicatedResource.passwordExpirationDate = null;
        duplicatedResource.terminationDate = null;
        duplicatedResource.deactivationDate = null;
        duplicatedResource.userCreationDate = null;
        duplicatedResource.lastVisitDate = null;
    }
});

