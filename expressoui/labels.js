var expresso = expresso || {};
expresso.Labels = expresso.Labels || {};
$.extend(expresso.Labels, {

    // Common
    important: "!",
    no: "Nº",
    id: "ID",
    description: "Description",
    title: "Titre",
    extKey: "Code externe",
    pgmKey: "Code",
    sortOrder: "Ordre de tri",
    status: "Statut",
    date: "Date",
    type: "Type",
    priority: "Priorité",

    creationDate: "Date de création",
    lastModifiedDate: "Dernière modification",
    deactivationDate: "Date de désactivation",
    approveDate: "Date d'approbation",
    fromDate: "Date de début",
    toDate: "Date de fin",
    startDate: "Date de début",
    endDate: "Date de fin",
    requestedDate: "Date requise",

    year: "Année",
    month: "Mois",
    day: "Jour",

    duration: "Durée",
    hours: "Heures",
    quantity: "Quantité",

    creationUser: "Créé(e) par",
    lastModifiedUser: "Modifié(e) par",
    requestedByUser: "Demandé(e) par",
    approveUser: "Approuvé(e) par",

    application: "Application",
    resource: "Ressource",
    action: "Action",
    role: "Rôle",
    user: "Utilisateur",
    person: "Personne",
    fullName: "Nom",
    department: "Département",
    jobTitle: "Titre",
    manager: "Gestionnaire",

    document: "Document",
    documents: "Documents",
    audittrail: "Audit",
    audits: "Audits",
    comment: "Commentaire",
    comments: "Commentaire",
    file: "Fichier",
    fileName: "Fichier",

    // login
    password: "Mot de passe",
    userName: "Nom d'utilisateur",
    email: "Courriel",
    lastName: "Nom de famille",
    firstName: "Prénom",
    company: "Compagnie",
    note: "Note",

    userNamePlaceHolder: "Entrer votre nom d'utilisateur",
    passwordPlaceHolder: "Entrer votre mot de passe",
    forgotPassword: "Mot de passe oublié?",
    changePassword: "Changer de mot de passe",
    login: "Connexion",
    logout: "Déconnexion",
    localAccount: {label: "Compte externe", shortLabel: "Externe"},

    // profile
    profile: "Profil de l'utilisateur",
    phone: "Téléphone",
    phoneNumber: "Téléphone",
    language: "Langue",
    oldPassword: "Ancien mot de passe",
    newPassword: "Nouveau mot de passe",
    newPasswordConfirmation: "Confirmation nouveau mot de passe",
    passwordChange: "Changement du mot de passe",
    newUser: "Nouvel utilisateur? Faites une demande d'accès ici",
    newUserRequest: "Demande d'accès",
    newUserRequestNotes: "Indiquez la raison de votre demande",
    newUserRequestSaved: "Votre demande d'accès a été envoyée. Un courriel vous sera envoyé avec vos informations lorsque votre demande sera approuvée.",
    newUserRequestNotComplete: "Votre demande d'accès n'est pas complète. Veuillez compléter tous les champs du formulaire",
    emailNotValid: "L'adresse courriel n'est pas valide",
    accountBlocked: "Votre compte a été bloqué",

    forgotPasswordTitle: "Mot de passe oublié",
    forgotPasswordText: "Veuillez entrer votre nom d'utilisateur",
    forgotPasswordButton: "Envoyer",
    emailPasswordHasBeenSent: "Un message a été envoyé à votre adresse courriel",
    pleaseEnterUsername: "Veuillez entrer votre nom d'utilisateur",

    fr: "Français",
    en: "English",
    yn_yes: "Oui",
    yn_no: "Non",
    tooManyResults: "Trop de résultats...",
    noSelection: "", //"-- Aucun --",
    allSelection: "-- Tous --",
    allSelection_e: "-- Toutes --",
    selectFilterNone: "-- Aucune valeur --",

    // Framework
    save: "Enregistrer",
    confirm: "Confirmer",
    select: "Sélectionner",
    missingFields: "Veuillez compléter les champs manquants",
    confirmation: "Confirmation",
    searchMenu: "Recherche",
    print: "Imprimer",
    searchPlaceHolder: "Recherche...",
    checkAll: "Sélectionner tous",

    message_question: "Question",
    message_info: "Information",
    message_warning: "Avertissement",
    message_error: "Erreur",

    // error
    invalidCaptcha: "Captcha not valid",
    invalidUsername: "Nom d'utilisateur invalide",
    invalidPassword: "Mot de passe incorrect",
    invalidNewStrongPassword: "Mot de passe non valide.<br>Règles:<ul><li>8 à 100 caractères</li></ul>",
    invalidNewStrong15Password: "Mot de passe non valide.<br>Règles:<ul><li>15 à 100 caractères</li></ul>",
    invalidNewSecurePassword: "Mot de passe non valide.<br>Règles:<ul><li>8 à 100 caractères</li><li>Au moins une lettre minuscule</li><li>Au moins une lettre majuscule</li><li>Au moins un chiffre</li><li>Au moins un symbole (!@#$%^&-+=_)</li><li>Doit être différent du mot de passe actuel</li></ul>",
    invalidCredentials: "Nom d'utilisateur, mot de passe ou type de compte non valide",
    passwordsDoNoMatch: "Mots de passe ne sont pas identiques",
    passwordChanged: "Votre mot de passe a été modifié",
    notLocalAccount: "Vous ne pouvez pas changer le mot de passe de ce compte",
    browserSupport: "Ce navigateur ne support pas les notifications",
    noEmailHasBeenFound: "Aucun courriel trouvé pour les enregistrements sélectionnés",
    tooManyEmails: "Trop de courriels. Vous pouvez cependant copier manuellement la liste dans votre application de messagerie (les courriels ont été enregistrés dans votre presse-papier (clipboard))",
    wrongEntityVersion: "Cet enregistrement a été modifié par un autre utilisateur (ou processus). Vos changements n'ont pas été enregistrés. SVP recommencer",
    cannotCreateUser: "Vous ne pouvez pas créer ce compte utilisateur",
    userIsNotCreator: "Vous ne pouvez pas modifier cet enregistrement",
    tooManyRecordsForSelection: "La grille contient trop d'enregistrements pour la sélection. Vous pouvez utiliser le bouton Excel pour sortir une liste complète des enregistrements",
    userNotAllowedToModify: "Vous ne pouvez pas modifier cet enregistrement",
    unableToLocatePrinter: "L'imprimante [{printerName}] n'est pas configurée correctement sur le serveur",
    unableToResolveIP: "Le serveur n'est pas capable de trouver le nom de votre ordinateur à partir de votre IP [{ip}]",
    expiredSession: "Votre session a expirée",
    constraintViolationException: "Vous ne pouvez pas supprimer l'enregistrement parce que celui-ci a des dépendances",
    userNotAllowed: "Vous n'avez pas le droit de faire cette action",
    userNotAllowedToUploadDocument: "Vous n'avez pas le droit de téléverser ce document",
    userNotAllowedToDownloadDocument: "Vous n'avez pas le droit de télécharger des documents sur cette ressource",
    availableInternallyOnly: "Cette application est disponible sur le réseau local seulement",
    unexpectedLoginError: "Erreur lors de la tentative de connexion. Veuillez réessayer plus tard.",
    invalidSecurityToken: "Jeton de sécurité est invalide ou expiré",
    userTerminated: "L'utilisateur est interdit de se connecter. Contactez votre administrateur de système.",
    noUserFound: "Aucun utilisateur trouvé",
    invalidSession: "Votre session est invalide ou expirée",

    // form
    saveButtonLabel: "Enregistrer",
    createMainButtonLabel: "Activer les onglets",
    createdByLabel: "Créé(e) par",
    lastModificationLabel: "Dernière modification par",
    createdByDateLabel: "le",

    creationUserFullName: "Créé(e) par",
    lastModifiedUserFullName: "Modifié(e) par",

    // filters
    filterFromDate: "À partir du",
    filterToDate: "Jusqu'au",
    clearFilter: "Effacer filtre",
    filter: "Filtrer",

    filterRangeToday: "Aujourd'hui",
    filterRangeYesterday: "Hier",
    filterRangeLastWeek: "Semaine passée",
    filterRangeThisWeek: "Cette semaine",
    filterRangeNextWeek: "Semaine prochaine",
    filterRangeLastMonth: "Mois passé",
    filterRangeThisMonth: "Ce mois-ci",
    filterRangeLast3Days: "Derniers 3 jours",
    filterRangeLast7Days: "Derniers 7 jours",
    filterRangeLast30Days: "Derniers 30 jours",
    filterRangeLast365Days: "Derniers 365 jours",
    filterStaticDateRangeType: "Plage de dates",
    filterDynamicDateRangeType: "Dynamique",

    reports: "Rapports",
    executeReport: "Exécuter",
    toogleFullScreen: "Entrer en (ou sortir du) mode plein écran",
    lastUpdatedTime: "Dernière MAJ",

    initialization: "Initialisation",

    // Grid button title
    exportToExcel: "Exporter le contenu de la grille vers Excel",
    refresh: "Rafraîchir le contenu de la grille",
    clearFilters: "Enlever tous les filtres",
    saveConfiguration: "Enregistrer la configuration actuelle",
    resetConfiguration: "Utiliser la configuration par défaut",
    deleteGridPreference: "Effacer cette configuration",
    selectFavoriteGridPreference: "Sélectionner cette configuration par défaut lors du lancement de cette application",
    closeColumnMenu: "Fermer",
    getLink: "Copier le lien URL dans le presse-papier",
    synchronize: "Synchroniser le contenu avec l'application externe",
    process: "Appeler la méthode process du service",
    createNewRecord: "Créer un nouvel enregistrement",
    duplicateRecord: "Dupliquer l'enregistrement sélectionné",
    viewRecord: "Visualiser l'enregistrement sélectionné",
    modifyRecord: "Modifier l'enregistrement sélectionné",
    deleteRecords: "Effacer les enregistrements sélectionnés",
    activateRecords: "Activer les enregistrements sélectionnés",
    deactivateRecords: "Désactiver les enregistrements sélectionnés",
    printRecords: "Imprimer les enregistrements sélectionnés",
    sendEmail: "Envoyer un courriel aux enregistrements sélectionnés",
    uploadDocument: "Télécharger un fichier (drag&drop)",
    showActiveRecords: "Afficher seulement les enregistrements actifs",
    search: "Rechercher",
    enterReason: "Entrer une raison",

    confirmTitle: "Confirmation",
    confirmAction: "Êtes-vous sûr de vouloir exécuter l'action suivante: ",
    confirmDeleteElement: "Êtes-vous sûr de vouloir effacer cet élément?",

    deleteConfirmation: "Êtes-vous sûr de vouloir effacer l'enregistrement sélectionné ?",
    deleteConfirmationMany: "Êtes-vous sûr de vouloir effacer les {count} enregistrements sélectionnés ?",

    // Grid button text
    refreshButton: "",
    clearFiltersButton: "",
    saveConfigurationButton: "",
    getLinkButton: "",
    exportToExcelButton: "",
    synchronizeButton: "",
    processButton: "",
    createNewRecordButton: "",
    duplicateRecordButton: "",
    viewRecordButton: "",
    deleteRecordsButton: "",
    activateRecordsButton: "",
    deactivateRecordsButton: "",
    modifyRecordButton: "",
    printRecordsButton: "",
    sendEmailButton: "",
    uploadDocumentButton: "",
    showActiveRecordsButton: "Actifs seulement",
    searchButton: "",

    // standard actions
    approveButtonLabel: "Approuver",
    approveButtonTitle: "Approuver les enregistrements sélectionnés",
    rejectButtonLabel: "Rejeter",
    rejectButtonTitle: "Rejeter les enregistrements sélectionnés",
    cancelButtonLabel: "Annuler",
    cancelButtonTitle: "Annuler les enregistrements sélectionnés",
    terminateButtonLabel: "Terminer",
    terminateButtonTitle: "Terminer les enregistrements sélectionnés",
    openeButtonLabel: "Ourvir",
    openButtonTitle: "Ourvir les enregistrements sélectionnés",
    closeButtonLabel: "Fermer",
    closeButtonTitle: "Fermer les enregistrements sélectionnés",
    lockButtonLabel: "Barrer",
    lockButtonTitle: "Barrer les enregistrements sélectionnés",
    unlockButtonLabel: "Débarrer",
    unlockButtonTitle: "Débarrer les enregistrements sélectionnés",
    acceptButtonLabel: "Accepter",
    acceptButtonTitle: "Accepter les enregistrements sélectionnés",
    refuseButtonLabel: "Refuser",
    refuseButtonTitle: "Refuser les enregistrements sélectionnés",
    importButtonLabel: "Importer",
    importButtonTitle: "Importer les enregistrements sélectionnés",
    exportButtonLabel: "Exporter",
    exportButtonTitle: "Exporter les enregistrements sélectionnés",
    sendButtonLabel: "Envoyer",
    sendButtonTitle: "Envoyer les enregistrements sélectionnés",
    receiveButtonLabel: "Recevoir",
    receiveButtonTitle: "Recevoir les enregistrements sélectionnés",
    startButtonLabel: "Démarrer",
    startButtonTitle: "Démarrer les enregistrements sélectionnés",
    stopButtonLabel: "Arrêter",
    stopButtonTitle: "Arrêter les enregistrements sélectionnés",
    initButtonLabel: "Initialiser",
    initButtonTitle: "Initialiser les enregistrements sélectionnés",
    resetButtonLabel: "Réinitialiser",
    resetButtonTitle: "Réinitialiser les enregistrements sélectionnés",
    processButtonLabel: "Procéder",
    processButtonTitle: "Procéder les enregistrements sélectionnés",
    executeButtonLabel: "Exécuter",
    executeButtonTitle: "Exécuter les enregistrements sélectionnés",
    expediteButtonLabel: "Expédier",
    expediteButtonTitle: "Expédier les enregistrements sélectionnés",
    submitButtonLabel: "Soumettre",
    submitButtonTitle: "Soumettre les enregistrements sélectionnés",
    holdButtonLabel: "Retenir",
    holdButtonTitle: "Retenir les enregistrements sélectionnés",
    validateButtonLabel: "Valider",
    validateButtonTitle: "Valider les enregistrements sélectionnés",
    reviseButtonLabel: "Réviser",
    reviseButtonTitle: "Réviser les enregistrements sélectionnés",

    uniqueValidation: "La valeur [{fieldValue}] doit être unique pour ce champ",
    userUnauthorized: "Utilisateur non autorisé à exécuter l'action sur la resource.",
    invalidRequest: "La requête n'est pas valide.",
    resourceUnavailable: "La ressource demandée n'est pas disponible.",
    newApplicationVersion: "Une nouvelle version de l'application est disponible. Une mise à jour sera effectuée maintenant.",
    applicationProblem: "Il y a un problème avec l'application. Contacter le support technique.",
    missingPrivileges: "Vous n'avez pas les privilèges requis pour visualiser/modifier cette information",
    requestSuccess: "Requête exécutée avec succès",
    requestFailure: "Échec de la requête",
    functionUnauthorized: "Vous n'êtes pas autorisé(e) à accéder à cette fonction.",
    userNotFound: "L'utilisateur n'existe pas",
    requiredField: "Le champ [{field}] est obligatoire",
    kerberosChromeExcelIssue: "Google Chrome a un problème avec le téléchargement de fichier volumineux dans certaines conditions. Vous pouvez utiliser Firefox pour cette opération",
    excelDownloadInProgress: "Le téléchargement du fichier Excel débutera bientôt. IMPORTANT: l'exportation est limitée à 10 000 lignes.",

    january: "Janvier",
    february: "Février",
    march: "Mars",
    april: "Avril",
    may: "Mai",
    june: "Juin",
    july: "Juillet",
    august: "Août",
    september: "Septembre",
    october: "Octobre",
    november: "Novembre",
    december: "Décembre",

    monday: "Lundi",
    tuesday: "Mardi",
    wednesday: "Mercredi",
    thursday: "Jeudi",
    friday: "Vendredi",
    saturday: "Samedi",
    sunday: "Dimanche",

    // Save filters
    newConfigurationWindowTitle: "Nouvelle configuration",
    newConfigurationWindowText: "Indiquer le nom pour la nouvelle configuration",
    confirmDeleteConfiguration: "Êtes-vous certain de vouloir effacer la configuration [{filterName}]?",

    filterTitle: "Filtres additionnels",
    size: "Grandeur",
    small: "Petit",
    medium: "Moyen",
    large: "Grand",

    // standard errors
    infectedFile: "Le fichier est infecté. Il ne peut pas être téléchargé",
    invalidFileExtension: "Seulement ces types de documents sont permis [{allowedExtensions}]",
    invalidDocumentType: "Ce type de document [{documentType}] n'est pas permis",

    msieNotSupported: "Le navigateur Microsoft Internet Explorer n'est plus supporté. SVP utiliser un autre navigateur.",
    resourceDoesNotExistOrRestricted: "La ressource ne peut pas être affichée (elle a été détruite ou vous n'avez pas accès à cette ressource)",

    // email token
    emailTokenWindowTitle: "Jeton de sécurité",
    emailTokenWindowText: "Entrer votre jeton de sécurité (6 chiffres) que vous avez reçu par courriel",
    emailTokenWindowButton: "Valider le jeton",
    pleaseEnterEmailToken: "Veuillez entrer votre jeton de sécurité",
    emailTokenInvalidTitle: "Jeton non valide",
    emailTokenInvalidText: "le jeton n'est pas valide. Voulez-vous recevoir un nouveau jeton?",

    userNotAllowedToLoadApplication: "Vous n'êtes pas autorisé à afficher cette application",

    // filter
    addFilterGroup: "Groupe",
    addFilterRule:"Règle",
    visualFilter: "Visuel",
    sourceFilter: "Code source",

    _: ""
});