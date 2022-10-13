var expresso = expresso || {};
expresso.Labels = expresso.Labels || {};
$.extend(expresso.Labels, {

    // Common
    important: "!",
    no: "Number",
    id: "ID",
    description: "Description",
    title: "Title",
    extKey: "External code",
    pgmKey: "Code",
    sortOrder: "Sort order",
    status: "Status",
    date: "Date",
    type: "Type",
    priority: "Priority",

    creationDate: "Creation Date",
    lastModifiedDate: "Last Modification",
    deactivationDate: "Deactivation Date",
    approveDate: "Approval Date",
    fromDate: "Start Date",
    toDate: "End Date",
    startDate: "Start Date",
    endDate: "End Date",
    requestedDate: "Requested Date",

    year: "Year",
    month: "Month",
    day: "Day",

    duration: "Duration",
    hours: "Hours",
    quantity: "Quantity",

    creationUser: "Created by",
    lastModifiedUser: "Modified by",
    requestorByUser: "Requested by",
    approveUser: "Approved by",

    application: "Application",
    resource: "Resource",
    action: "Action",
    role: "Role",
    user: "User",
    person: "Person",
    fullName: "Name",
    department: "Department",
    jobTitle: "Title",
    manager: "Manager",
    document: "Document",
    documents: "Documents",
    audittrail: "Audit",
    audits: "Audits",
    comment: "Comment",
    comments: "Comment",
    file: "File",
    fileName: "File",

    // login
    password: "Password",
    userName: "User Name",
    email: "Email",
    lastName: "Last name",
    firstName: "First name",
    company: "Company",
    note: "Note",

    userNamePlaceHolder: "Please enter your userName",
    passwordPlaceHolder: "Please enter your password",
    forgotPassword: "Forgot password?",
    changePassword: "Update password",
    login: "Login",
    logout: "Logout",
    localAccount: "External account",

    // profile
    profile: "User profile",
    phone: "Phone number",
    phoneNumber: "Phone number",
    language: "Language",
    oldPassword: "Current password",
    newPassword: "New password",
    newPasswordConfirmation: "Confirm new password",
    passwordChange: "Update password",
    newUser: "New user? Request your access credentials here",
    newUserRequest: "Access request",
    newUserRequestNotes: "Please justify your request",
    noRecaptcha: "Please check reCaptcha",
    newUserRequestSaved: "Your request has been sent. You will receive an email with your credentials when your request will be approved",
    newUserRequestNotComplete: "Your request is not complete. Please fill in the form",
    emailNotValid: "Email is not a valid email address",
    accountBlocked: "Your account has been blocked",
    serviceUnavailable: "Maintenance in progress",

    forgotPasswordTitle: "Forgot password",
    forgotPasswordText: "Please enter your username",
    forgotPasswordButton: "Send",
    emailPasswordHasBeenSent: "A message has been sent to your email address",
    pleaseEnterUsername: "Please enter your userName",

    fr: "Français",
    en: "English",
    yn_yes: "Yes",
    yn_no: "No",
    tooManyResults: "Too many results...",
    noSelection: "", //"-- None --",
    allSelection: "-- All --",
    allSelection_e: "-- All --",
    selectFilterNone: "-- No value --",

    confirmation: "Confirmation",
    searchMenu: "Search",
    print: "Print",

    // Framework
    save: "Save",
    cancel: "Cancel",
    confirm: "Confirm",
    select: "Select",
    missingFields: "Please fill the missing fields",
    searchPlaceHolder: "Search...",
    checkAll: "Select all",

    message_question: "Question",
    message_info: "Information",
    message_warning: "Warning",
    message_error: "Error",

    // error
    invalidCaptcha: "Cannot validate Captcha",
    invalidCaptchaNotSuccess: "Captcha not valid",
    invalidCaptchaScoreTooLow: "You are a robot",
    invalidUsername: "Invalid userName",
    invalidPassword: "Incorrect password",
    invalidNewStrongPassword: "Invalid password.<br>Rules:<ul><li>At least 8 chars</li></ul>",
    invalidNewStrong15Password: "Invalid password.<br>Rules:<ul><li>At least 15 chars</li></ul>",
    invalidNewSecurePassword: "Invalid password.<br>Rules:<ul><li>At least 8 chars</li><li>Contains at least one lower alpha char</li><li>Contains at least one upper alpha char</li><li>Contains at least one digit</li><li>Contains at least one special char (!@#$%^&-+=_)</li><li>Must be different from the actual password</li></ul>",
    invalidCredentials: "Invalid userName, password or account type",
    passwordsDoNoMatch: "Passwords do not match",
    passwordChanged: "Your password has been changed successfully",
    notLocalAccount: "You cannot change password for this account",
    browserSupport: "This browser does not support desktop notification",
    noEmailHasBeenFound: "There is no email in the selected item",
    tooManyEmails: "Too many emails. However, you can manually paste the list into your mailer (the emails have been copied into your clipboard)",
    wrongEntityVersion: "This entity has been modified by another user/process. Your changes have not been saved. Please retry",
    cannotCreateUser: "You cannot create this user account",
    userIsNotCreator: "You cannot modify this record",
    tooManyRecordsForSelection: "Too many records for selection. You could the Excel button to get a report of all records.",
    userNotAllowedToModify: "You cannot modify this record",
    unableToLocatePrinter: "Printer [{printerName}] is not properly configured on the server",
    unableToResolveIP: "Server cannot determine your computer's name from your IP [{ip}]",
    expiredSession: "Your session has expired",
    constraintViolationException: "You cannot delete the record because of its dependencies",
    availableInternallyOnly: "This application is only available on the local network",
    unexpectedLoginError: "Unexpected error. Please try again later",
    invalidSecurityToken: "Security token is invalid or expired",
    userTerminated: "User is not allowed to login. Please contact your system administrator",
    noUserFound: "User is not found",
    invalidSession: "Session is invalid or expired",

    // form
    saveButtonLabel: "Save",
    createMainButtonLabel: "Enable tabs",
    createdByLabel: "Created by",
    lastModificationLabel: "Last modified by",
    createdByDateLabel: "on",

    creationUserFullName: "Created by",
    lastModifiedUserFullName: "Last modified by",

    // filters
    filterFromDate: "From",
    filterToDate: "Until",
    clearFilter: "Clear",
    filter: "Filter",

    filterRangeToday: "Today",
    filterRangeYesterday: "Yesterday",
    filterRangeLastWeek: "Last week",
    filterRangeThisWeek: "This week",
    filterRangeNextWeek: "Next week",
    filterRangeLastMonth: "Last month",
    filterRangeThisMonth: "This month",
    filterRangeLast3Days: "Last 3 days",
    filterRangeLast7Days: "Last 7 days",
    filterRangeLast30Days: "Last 30 days",
    filterRangeLast365Days: "Last 365 days",
    filterStaticDateRangeType: "Date range",
    filterDynamicDateRangeType: "Dynamic",
    filterNoDateType: "No dates",

    reports: "Reports",
    executeReport: "Execute",
    toogleFullScreen: "Toggle full screen mode",
    lastUpdatedTime: "Last update",

    initialization: "Initialisation",

    // Grid button title
    exportToExcel: "Export grid content to Excel",
    refresh: "Refresh grid content",
    clearFilters: "Clear all filters",
    saveConfiguration: "Save current configuration",
    resetConfiguration: "Reset to default configuration",
    deleteGridPreference: "Delete this configuration",
    selectFavoriteGridPreference: "Select this configuration by default when this application is launched",
    closeColumnMenu: "Close",
    getLink: "Copy the URL of the resource to the clipboard",
    synchronize: "Synchronize the content with the external application",
    process: "Call the process method of the service",
    createNewRecord: "Create new record",
    duplicateRecord: "Duplicate selected record",
    viewRecord: "View selected record",
    modifyRecord: "Modify selected record",
    deleteRecords: "Delete selected records",
    printRecords: "Print selected records",
    sendEmail: "Send email with the selected record",
    uploadDocument: "Upload a document (drag&drop)",
    showActiveRecords: "Show only active records",
    search: "Search",
    enterReason: "Please enter the reason",
    toggleHierarchical: "Toggle between list and tree view",
    expandHierarchical: "Expand/Collapse all",

    confirmTitle: "Confirmation",
    confirmAction: "Are you sure you want to execute this action: ",
    confirmDeleteElement: "Are you sure you want to delete this item?",

    deleteConfirmation: "Are you sure you want to delete this record ?",
    deleteConfirmationMany: "Are you sure you want to delete the {count} selected records ?",

    // Grid button text
    exportToExcelButton: "",
    refreshButton: "",
    clearFiltersButton: "",
    saveConfigurationButton: "",
    getLinkButton: "",
    synchronizeButton: "",
    processButton: "",
    createNewRecordButton: "",
    activateRecordsButton: "",
    deactivateRecordsButton: "",
    duplicateRecordButton: "",
    viewRecordButton: "",
    deleteRecordsButton: "",
    modifyRecordButton: "",
    printRecordsButton: "",
    sendEmailButton: "",
    uploadDocumentButton: "",
    showActiveRecordsButton: "Actives only",
    searchButton: "",
    toggleHierarchicalButton: "",
    expandHierarchicalButton: "",
    collapseHierarchicalButton: "",

    // standard actions
    approveButtonLabel: "Approve",
    approveButtonTitle: "Approve selected records",
    rejectButtonLabel: "Reject",
    rejectButtonTitle: "Reject selected records",
    cancelButtonLabel: "Cancel",
    cancelButtonTitle: "Cancel selected records",
    terminateButtonLabel: "Terminate",
    terminateButtonTitle: "Terminate selected records",

    openButtonLabel: "Open",
    openButtonTitle: "Open selected records",
    closeButtonLabel: "Close",
    closeButtonTitle: "Close selected records",
    lockButtonLabel: "Lock",
    lockButtonTitle: "Lock selected records",
    unlockButtonLabel: "Unlock",
    unlockButtonTitle: "Unlock selected records",
    acceptButtonLabel: "Accept",
    acceptButtonTitle: "Accept selected records",
    refuseButtonLabel: "Refuse",
    refuseButtonTitle: "Refuse selected records",
    importButtonLabel: "Import",
    importButtonTitle: "Import selected records",
    exportButtonLabel: "Export",
    exportButtonTitle: "Export selected records",
    sendButtonLabel: "Send",
    sendButtonTitle: "Send selected records",
    receiveButtonLabel: "Receive",
    receiveButtonTitle: "Receive selected records",
    startButtonLabel: "Start",
    startButtonTitle: "Start selected records",
    stopButtonLabel: "Stop",
    stopButtonTitle: "Stop selected records",
    initButtonLabel: "Initialize",
    initButtonTitle: "Initialize selected records",
    resetButtonLabel: "Reset",
    resetButtonTitle: "Reset selected records",
    processButtonLabel: "Process",
    processButtonTitle: "Process selected records",
    executeButtonLabel: "Execute",
    executeButtonTitle: "Execute selected records",
    expediteButtonLabel: "Expedite",
    expediteButtonTitle: "Expedite selected records",
    submitButtonLabel: "Submit",
    submitButtonTitle: "Submit selected records",
    holdButtonLabel: "Hold",
    holdButtonTitle: "Hold selected records",
    validateButtonLabel: "Validate",
    validateButtonTitle: "Validate selected records",
    reviseButtonLabel: "Revise",
    reviseButtonTitle: "Revise selected records",

    uniqueValidation: " The value [{fieldValue}] must be unique for the field",
    userUnauthorized: "User is unauthorized to execute this action on the resource",
    invalidRequest: "Request is invalid",
    resourceUnavailable: "Requested resource is unavailable.",
    newApplicationVersion: "A new version is available for the application. Updating now.",
    applicationProblem: "There is a problem with the application. Please contact technical support.",
    unknownProblem: "An error occurred. Please try again.",
    missingPrivileges: "Missing privileges to view/update this information",
    requestSuccess: "Request success",
    requestFailure: "Request failed",
    functionUnauthorized: "You are unauthorized to access this function",
    userNotFound: "User does not exist",
    requiredField: "Field [{field}] is required",
    kerberosChromeExcelIssue: "Google Chrome has an issue with huge download file under certain circumstances. Please use Firefox for this operation.",
    excelDownloadInProgress: "Excel file download will begin shortly. IMPORTANT: Export is limited to 10,000 lines.",
    tooManyResultsForHierarchical: "Too many results for a hierarchical view. Please change the selected view or add filters",
    maximumLimit: "Limit reached (max {max})",
    noRecord: "No records",
    record: "record",
    nbrSelectedRecord: "{count} selected",
    requireApprovalNote: "Field '{field}' has been changed",

    january: "January",
    february: "February",
    march: "March",
    april: "April",
    may: "May",
    june: "June",
    july: "July",
    august: "August",
    september: "September",
    october: "October",
    november: "November",
    december: "December",

    monday: "Monday",
    tuesday: "Tuesday",
    wednesday: "Wednesday",
    thursday: "Thursday",
    friday: "Friday",
    saturday: "Saturday",
    sunday: "Sunday",

    today: "Today",
    yesterday: "Yesterday",
    tomorrow: "Tomorrow",
    lastWeek: "Last week",
    thisWeek: "This week",
    nextWeek: "Next week",
    lastMonth: "Last month",
    thisMonth: "This month",
    firstDayOfMonth: "First day of month",
    lastDayOfMonth: "Last day of month",
    firstDayOfYear: "First day of year",
    lastDayOfYear: "Last day of year",
    lastSunday: "Last Sunday",
    lastMonday: "Last Monday",

    // Save filters
    newConfigurationWindowTitle: "New filter",
    newConfigurationWindowText: "Please enter a name for the new filter",
    confirmDeleteConfiguration: "Are you sure that you want to delete the filter [{filterName}]?",


    filterTitle: "Additional filters",
    size: "Size",
    small: "Small",
    medium: "Medium",
    large: "Large",
    printerName: "Printer",

    // standard errors
    infectedFile: "The file is infected. It cannot be uploaded",
    invalidFileExtension: "Allowed file types [{allowedExtensions}]",
    invalidDocumentType: "This file type [{documentType}] is not allowed",

    msieNotSupported: "Microsoft Internet Explorer not supported. Please use another browser.",
    resourceDoesNotExistOrRestricted: "This resource cannot be displayed (it has been deleted or the access is restricted)",
    duplicatedUserName: "User name is already taken",

    // email token
    emailTokenWindowTitle: "Security token",
    emailTokenWindowText: "Entre your security token (6 digits) received by email",
    emailTokenWindowButton: "Validate token",
    pleaseEnterEmailToken: "Please enter your security token",
    emailTokenInvalidTitle: "Invalid token",
    emailTokenInvalidText: "The token is not valid. Do you want to get a new token?",

    userNotAllowedToLoadApplication: "You are not allowed to load this application",

    // filter
    addFilterGroup: "Group",
    addFilterRule: "Rule",
    visualFilter: "Visual",
    sourceFilter: "Source code",

    _: ""
});
