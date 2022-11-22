var expresso = expresso || {};

/**
 * Security Module. It uses the Javascript Module encapsulation pattern
 */
expresso.Security = function () {
    var impersonateUser = undefined;
    var userProfile = undefined;

    // list of privileges for this user (map)
    var privileges = undefined;

    var sessionToken = undefined;
    var ipAddress;
    var internalIpAddress;

    var timeToLoadProfile;

    /**
     * Return true if the user has the role assigned
     * @param role role pgmKey
     * @return {boolean}  true if the user has the role assigned
     */
    var isUserInRole = function (role) {
        if (userProfile && userProfile.userRoles) {
            for (var i = 0; i < userProfile.userRoles.length; i++) {
                if (userProfile.userRoles[i].pgmKey == role ||
                    // admin has all roles
                    userProfile.userRoles[i].pgmKey == "admin") {
                    return true;
                }
            }
        }
        return false;
    };

    /**
     * Returns true if the user is a power user
     */
    var isPowerUser = function () {
        return isUserInRole("poweruser");
    };

    /**
     * Returns true if the user is a super user
     */
    var isAdmin = function () {
        return isUserInRole("admin");
    };

    /**
     * Method to verify if the user is allowed to perform the action on the resource.
     * @param resourceName name of the resource
     * @param [action] action to be performed
     * @param [displayMessage] true if a message should be displayed if user is not allowed
     * @returns {boolean} true if the user is allowed to perform the action on the resource.
     */
    var isUserAllowed = function (resourceName, action, displayMessage) {
        var isAllowed;
        if (!privileges) {
            // if privileges are not yet loaded, let it
            isAllowed = true;
        } else {
            if (!action) {
                action = "read";
            }
            isAllowed = privileges[resourceName.toLowerCase() + "-" + action.toLowerCase()];
            if (!isAllowed) {
                //console.log("isUserAllowed " + resourceName + ":" + action + "=" + isAllowed);
                if (displayMessage) {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("functionUnauthorized"));
                }
            }
        }

        return isAllowed;
    };

    /**
     * Get the user profile
     * @returns {*} the user profile
     */
    var getUserInfo = function () {
        return userProfile;
    };

    /**
     *
     * @returns {*}
     */
    var getImpersonateUser = function () {
        return impersonateUser;
    };

    /**
     *
     * @returns {*}
     */
    var getIpAddress = function () {
        return ipAddress;
    };

    var getTimeToLoadProfile = function () {
        return timeToLoadProfile;
    };

    /**
     *
     * @returns {*}
     */
    var isInternalIpAddress = function () {
        return internalIpAddress;
    };

    /**
     * Load the profile asynchronously
     * @param [userName] if not defined, load "me"
     * @returns {*} a promise when the profile is loaded
     */
    var loadProfile = function (userName) {
        var $deferred = $.Deferred();

        if (userName === undefined) {
            if (expresso.util.Util.getUrlParameter("impersonate")) {
                userName = expresso.util.Util.getUrlParameter("impersonate");
            }
        }
        impersonateUser = userName;

        var $userDeferred;
        if (impersonateUser) {
            $userDeferred = expresso.Common.sendRequest("user", null, null, expresso.Common.buildKendoFilter({userName: impersonateUser}), {
                waitOnElement: null
            }).then(function (result) {
                if (result && result.total) {
                    return result.data[0];
                } else {
                    return null;
                }
            });
        } else {
            $userDeferred = expresso.Common.sendRequest("user/me", null, null, null, {
                waitOnElement: null
            });
        }

        // get the user profile and permissions
        $userDeferred.done(function (profile) {
            if (profile) {
                userProfile = profile;
                //console.log("Profile", userProfile);

                // we have the preferred language of the user, use it
                expresso.Common.setLanguage(profile.language)

                var startTime = new Date();
                $.when(
                    //  load the privileges
                    expresso.Common.sendRequest("user/" + profile.id + "/privilege", null, null, null, {
                        waitOnElement: null
                    }).done(function (privs) {
                        // build the hashmap for fast access
                        privileges = {};
                        for (var i = 0; i < privs.length; i++) {
                            var p = privs[i];
                            // console.log(p.resource.securityPath.toLowerCase() + "-" + p.action.pgmKey.toLowerCase());
                            privileges[p.resource.securityPath.toLowerCase() + "-" + p.action.pgmKey.toLowerCase()] = true;
                        }
                    }),

                    // get all applications that the user is allowed
                    expresso.Common.sendRequest("user/" + profile.id + "/application", null, null, null, {
                        waitOnElement: null
                    }).done(function (apps) {
                        var appDef;

                        // reset all permissions
                        var appNameMap = expresso.Common.getApplicationNameMap();
                        for (var a in appNameMap) {
                            appDef = appNameMap[a];
                            appDef.allowed = false;
                        }

                        var menuLabels = expresso.Common.getSiteNamespace().config.menu.Labels;
                        for (var i = 0; i < apps.length; i++) {
                            var app = apps[i];
                            appDef = expresso.Common.getApplication(app.pgmKey);

                            // set the title of the application for the Browser bar
                            if (appDef) {
                                appDef.application = app;
                                if (!appDef.title) {
                                    appDef.title = expresso.Common.getLabel(app.pgmKey, menuLabels);
                                    //console.log(appName + ": " + appDef.title );
                                }
                                appDef.allowed = true;
                            } else {
                                // this is the case when the app is a URL
                                // insert at runtime the app in the applications
                                expresso.Common.addApplication(app.pgmKey, {
                                    title: expresso.Common.getLabel(app.pgmKey, menuLabels),
                                    allowed: true
                                });
                            }
                        }

                        // print the list of not allowed application
                        // for (  a in appNameMap) {
                        //     appDef = appNameMap[a];
                        //     if (!appDef.allowed) {
                        //         console.log("No access: [" + a + "]");
                        //     }
                        // }
                    }),

                    // favorites
                    expresso.Common.sendRequest("user/" + profile.id + "/config", null, null,
                        expresso.Common.buildKendoFilter({field: "key", operator: "eq", value: "MenuFavorites"}), {
                            waitOnElement: null
                        })
                        .done(function (configs) {
                            if (configs.total > 0) {
                                var config = configs.data[0];
                                userProfile.favoritesId = config.id;
                                userProfile.favorites = JSON.parse(config.value);
                            } else {
                                userProfile.favoritesId = undefined;
                                userProfile.favorites = [];
                            }
                        }),

                    // roles
                    expresso.Common.sendRequest("user/" + profile.id + "/allroles", null, null, null, {
                        waitOnElement: null
                    }).done(function (roles) {
                        //console.log(roles);
                        userProfile.userRoles = roles;
                    }),

                    // user infos
                    expresso.Common.sendRequest("user/" + profile.id + "/info", null, null, null, {
                        waitOnElement: null
                    }).done(function (infos) {
                        userProfile.userInfos = {};
                        $.each(infos.data, function (index, userInfo) {
                            userProfile.userInfos[userInfo.roleInfo.pgmKey] = userInfo;
                        });
                    }),

                    //  get the IP address
                    expresso.Common.sendRequest("support/myIP", null, null, null, {
                        waitOnElement: null
                    }).done(function (result) {
                        ipAddress = result.ipAddress;
                        internalIpAddress = result.internalIpAddress;
                    })
                ).done(function () {
                    var endTime = new Date();
                    timeToLoadProfile = endTime.getTime() - startTime.getTime();
                    console.log("Time to load profile (ms): " + timeToLoadProfile);

                    // review the applications to check if they are allowed
                    if (userProfile.genericAccount) {
                        var appNameMap = expresso.Common.getApplicationNameMap();
                        for (var a in appNameMap) {
                            var appDef = appNameMap[a];
                            if (appDef && appDef.application) {
                                if (appDef.application.internalOnly && !internalIpAddress) {
                                    appDef.allowed = false;
                                    console.log("Removing [" + appDef.title + "] because it is internal only");
                                }
                            }
                        }
                    }
                    $deferred.resolve(userProfile);
                });
            } else {
                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("userNotFound"));
                $deferred.reject();
            }
        });
        return $deferred;
    };

    /**
     *
     * @return
     */
    var getUserFields = function () {
        return $.extend({}, {
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
                //defaultValue: "fr",
                nullable: true,
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
                timestamp: true,
                editable: false
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
                reference: true,
                nullable: true
            }
        }, getPersonFields());
    };

    /**
     *
     * @return
     */
    var getPersonFields = function () {
        return {
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
            managerId: {
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
    };

    /**
     *
     */
    var displayChangePassword = function () {
        var $deferred = $.Deferred();

        // if it is from a link from an email, we get those information
        // otherwise the user is already log in and its password is expired
        var userName = expresso.util.Util.getUrlParameter("userName");
        var securityToken = expresso.util.Util.getUrlParameter("securityToken");

        expresso.util.UIUtil.buildWindow("expresso/ext/change-password.html", {
            width: 400,
            title: expresso.Common.getLabel("passwordChange"),
            save: function () {
                var $windowDiv = $(this);
                var kendoWindow = $windowDiv.data("kendoWindow");
                if ($windowDiv.find("[name=newPassword]").val() != $windowDiv.find("[name=newPasswordConfirmation]").val()) {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("passwordsDoNoMatch"));
                } else {
                    var newPassword = $windowDiv.find("[name=newPassword]").val();
                    expresso.Common.sendRequest("authentication", "validate", {
                        securityToken: securityToken,
                        userName: userName,
                        newPassword: newPassword
                    }, null, {public: true, waitOnElement: $windowDiv})
                        .done(function () {
                            // password change successfully
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("passwordChanged")).always(function () {
                                performLogin(userName, newPassword, null, securityToken).done(function () {
                                    $deferred.resolve();
                                    kendoWindow.close();
                                });
                            });
                        })
                        .fail(function (jqXHR) {
                            if (jqXHR.status == expresso.Common.HTTP_CODES.EMAIL_TOKEN_REQUIRED) {
                                // invalidSecurityToken
                                jqXHR.alreadyProcessed = true;
                                expresso.Common.displayServerValidationMessage(jqXHR).always(function () {
                                    // reload and remove the token from the URL
                                    window.open("/");
                                });
                            }
                        });
                }

                // by default, do not close the window
                return false;
            },
            close: function () {
                if ($deferred.state() != "resolved") {
                    logout();
                }
            }
        });
        return $deferred;
    };

    /**
     *
     */
    var loadProfileWhileDisplaySplashPage = function () {
        var $deferred = $.Deferred();
        var $overlayDiv = $("<div class='overlay-content'></div>").appendTo($("body"));
        expresso.Common.loadHTML($overlayDiv, "expresso/ext/splash.html").done(function ($div) {
            $div.find(".title").show().html(expresso.Common.getLabel("splashTitle"));

            loadProfile().done(function () {
                $deferred.resolve();
                window.setTimeout(function () {
                    $overlayDiv.remove();
                    $overlayDiv = null;
                }, 1000);
            });
        });
        return $deferred;
    };

    /**
     *
     */
    var displayNewUserPage = function () {
        expresso.util.UIUtil.buildWindow("expresso/ext/new-user.html", {
            width: 400,
            //height: 350,
            title: expresso.Common.getLabel("newUserRequest"),
            open: function () {
                //var $windowDiv = $(this);
                // render the captcha
                try {
                    grecaptcha.render("g-recaptcha", {
                        sitekey: expresso.Common.getSiteNamespace().config.Configurations.googleRecaptchaSitekey
                    });
                } catch (e) {
                    console.error(e);
                }
            },
            save: function () {
                var $windowDiv = $(this);
                var firstName = $windowDiv.find("[name=firstName]").val();
                var lastName = $windowDiv.find("[name=lastName]").val();
                var email = $windowDiv.find("[name=email]").val() || "";
                var notes = $windowDiv.find("[name=newUserRequestNotes]").val();
                var company = $windowDiv.find("[name=company]").val();

                var recaptchaResponse = $windowDiv.find("[name='g-recaptcha-response']").val();

                if (!recaptchaResponse) {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("noRecaptcha"));
                    return false;
                } else if (firstName && lastName && email && notes && company) {
                    // regex for email validation
                    var re = /\S+@\S+\.\S+/;
                    if (re.test(email)) {
                        // display the confirmation window
                        expresso.util.UIUtil.buildYesNoWindow(expresso.Common.getLabel("privacyTitle"),
                            expresso.Common.getLabel("privacyNote"), {
                                width: 500,
                                okText: expresso.Common.getLabel("privacyAccept"),
                                cancelText: expresso.Common.getLabel("privacyRefuse")
                            }
                        ).done(function () {
                            expresso.Common.sendRequest("userRequest", "create", {
                                type: "userRequest",
                                firstName: firstName,
                                lastName: lastName,
                                email: email,
                                notes: company + "\n" + notes,
                                recaptchaResponse: recaptchaResponse
                            }, null, {public: true}).done(function () {
                                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("newUserRequestSaved"));
                            });
                        });
                    } else {
                        expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("emailNotValid"));
                        return false;
                    }
                } else {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("newUserRequestNotComplete"));
                    return false;
                }
            }
        });
    };

    /**
     *
     */
    var displayForgotPasswordPage = function () {
        var $deferred = $.Deferred();
        // display a popup to request the username
        expresso.util.UIUtil.buildPromptWindow(expresso.Common.getLabel("forgotPasswordTitle"), expresso.Common.getLabel("forgotPasswordText"),
            {okText: expresso.Common.getLabel("forgotPasswordButton")})
            .done(function (userName) {
                if (userName) {
                    expresso.Common.sendRequest("authentication", "reset", {userName: userName},
                        null, {public: true}).done(function () {
                        expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("emailPasswordHasBeenSent"));
                        $deferred.resolve();
                    });
                } else {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("pleaseEnterUsername"));
                }
            });
        return $deferred;
    };

    /**
     * Display a popup to request the email token
     *
     * @param $deferred
     */
    var displayEmailTokenPage = function ($deferred) {
        if (!$deferred) {
            $deferred = $.Deferred();
        }
        expresso.util.UIUtil.buildPromptWindow(expresso.Common.getLabel("emailTokenWindowTitle"),
            expresso.Common.getLabel("emailTokenWindowText"),
            {okText: expresso.Common.getLabel("emailTokenWindowButton")})
            .done(function (emailToken) {
                if (emailToken) {
                    expresso.Common.sendRequest("authentication", "login", {emailToken: emailToken},
                        null, {ignoreErrors: true})
                        .done(function () {
                            $deferred.resolve();
                        })
                        .fail(function (jqXHR) {
                            // token could be valid but another issue (password expired, etc)
                            if (jqXHR.status != expresso.Common.HTTP_CODES.UNAUTHORIZED) {
                                handleLoginError(jqXHR).done(function () {
                                    $deferred.resolve();
                                }).fail(function () {
                                    $deferred.reject();
                                });
                            } else {
                                // if 401 -> token not valid
                                expresso.util.UIUtil.buildYesNoWindow(expresso.Common.getLabel("emailTokenInvalidTitle"),
                                    expresso.Common.getLabel("emailTokenInvalidText"))
                                    .done(function () {
                                        // sent a new token
                                        expresso.Common.sendRequest("authentication", "login", null,
                                            null, {ignoreErrors: true}).always(function () {
                                            displayEmailTokenPage($deferred);
                                        });
                                    })
                                    .fail(function () {
                                        // user will try to reenter the token
                                        displayEmailTokenPage($deferred);
                                    });
                            }
                        });
                } else {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("pleaseEnterEmailToken")).done(function () {
                        displayEmailTokenPage($deferred);
                    });
                }
            })
            .fail(function () {
                logout();
            });
        return $deferred;
    };

    /**
     *
     */
    var displayLoginPage = function () {
        var $deferred = $.Deferred();

        var $overlayDiv = $("<div class='overlay-content'></div>").appendTo($("body"));
        expresso.Common.loadHTML($overlayDiv, "expresso/ext/login.html").done(function ($div) {

            if (!expresso.Common.getSiteNamespace().config.Configurations.localAccount) {
                $div.find(".authentication-mechanism").hide();
            } else {
                // do not allow password request on SSO
                $div.find("[name=authentication]").on("click", function () {
                    var auth = $div.find("[name=authentication]:checked").val();
                    if (auth == "kerberos") {
                        $div.find(".forgot-password").hide();
                    } else {
                        $div.find(".forgot-password").show();
                    }
                });
            }

            //forgot password
            $div.find(".forgot-password a").on("click", function (e) {
                e.preventDefault();
                displayForgotPasswordPage();
            });

            if (!expresso.Common.getSiteNamespace().config.Configurations.supportNewUserRequest) {
                $div.find(".new-user").hide();
            } else {
                //new user
                $div.find(".new-user a").on("click", function (e) {
                    e.preventDefault();
                    displayNewUserPage();
                });
            }

            // handle the submit
            $div.find(".login-form").on("submit", function (e) {
                e.preventDefault();

                // focus out to avoid multiple submit
                if (document.activeElement) {
                    try {
                        document.activeElement.blur();
                    } catch (ex) {
                        // ignore
                    }
                }

                var auth = $div.find("[name=authentication]:checked").val();
                if (auth == "kerberos") {
                    expresso.Common.setAuthenticationPath("sso");
                } else {
                    expresso.Common.setAuthenticationPath("rest");
                }

                // set credentials for Basic Auth
                var userName = $div.find("[name=userName]").val();
                var password = $div.find("[name=password]").val();
                userName = (userName ? userName.trim() : "");
                password = (password ? password.trim() : "");
                performLogin(userName, password)
                    .done(function () {
                        // success
                        try {
                            if (window.PasswordCredential) {
                                //var c = await navigator.credentials.create({password: e.target});
                                var c = new PasswordCredential(e.target);
                                navigator.credentials.store(c);
                            }
                        } catch (e) {
                            console.warn("Problem with PasswordCredential", e);
                        }

                        // remove the login page and go back to the app
                        $overlayDiv.remove();
                        $overlayDiv = null;
                        $deferred.resolve();
                    });
            });

            // then focus on username
            $div.find("[name=userName]").focus();

            if (expresso.util.Util.getUrlParameter("securityToken")) {
                displayChangePassword().done(function () {
                    // remove the login page and go back to the app
                    $overlayDiv.remove();
                    $overlayDiv = null;
                    $deferred.resolve();
                });
            }
        });

        return $deferred;
    };

    /**
     *
     * @param userName
     * @param password
     * @param loginToken
     * @param emailToken
     */
    var performLogin = function (userName, password, loginToken, emailToken) {
        var data = {_: new Date().getTime()};
        if (emailToken) {
            data["emailToken"] = emailToken;
        }

        var $loginDiv = $("div.login");
        expresso.util.UIUtil.showLoadingMask($loginDiv, true);

        // we cannot use performRequest because we need to add HTTP header
        var $deferred = $.Deferred();
        $.ajax(expresso.Common.getWsResourcePathURL() + "/authentication?action=login", {
            method: "POST",
            data: data,
            beforeSend: function (xhr) {
                var sessionToken = getSessionToken();
                if (sessionToken) {
                    xhr.setRequestHeader("X-Session-Token", sessionToken);
                }
                var credentials;
                if (userName) {
                    credentials = window.btoa(unescape(encodeURIComponent(userName + ":" + password)));
                }
                if (loginToken) {
                    credentials = loginToken;
                }
                if (credentials) {
                    xhr.setRequestHeader("Authorization", "Basic " + credentials);
                }

            }
        }).done(function (data, textStatus, jqXHR) {
            storeSessionToken(jqXHR);
            $deferred.resolve();
        }).fail(function (jqXHR) {
            storeSessionToken(jqXHR);
            handleLoginError(jqXHR, userName).done(function () {
                $deferred.resolve();
            }).fail(function () {
                $deferred.reject();
            });
        }).always(function () {
            expresso.util.UIUtil.showLoadingMask($loginDiv, false);
        });
        return $deferred;
    };

    /**
     *
     * @param jqXHR
     * @param userName
     * @returns {*}
     */
    var handleLoginError = function (jqXHR, userName) {
        // avoid ajaxSetup to handle this error
        jqXHR.alreadyProcessed = true;

        var response;
        try {
            // it should be a JSON response
            response = JSON.parse(jqXHR.responseText); // {description:"",params:{key:value}};
        } catch (err) {
            // if not, keep the text directly
            response = {description: jqXHR.responseText};
        }

        var $deferred = $.Deferred();
        switch (jqXHR.status) {
            case expresso.Common.HTTP_CODES.PASSWORD_EXPIRED: // 460
                $deferred = displayChangePassword();
                break;

            case expresso.Common.HTTP_CODES.EMAIL_TOKEN_REQUIRED: // 424
                $deferred = displayEmailTokenPage();
                break;

            case expresso.Common.HTTP_CODES.LOCKED:  // 423 Locked - When the account is locked
                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("accountBlocked")).done(function () {
                    $deferred.reject();
                });
                break;

            case expresso.Common.HTTP_CODES.UNAUTHORIZED : // 401
                if (!response.description && !userName) {
                    // no action needed. This is the case when testing login for SSO
                    $deferred.reject();
                } else {
                    var message = response.description || "invalidCredentials";
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel(message)).done(function () {
                        $deferred.reject();
                    });
                }
                break;

            default:
                //window.location.reload(true);
                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("unexpectedLoginError")).done(function () {
                    $deferred.reject();
                });
                break;
        }
        return $deferred;
    };

    /**
     * Send a request to be logout and force a reauthentication
     */
    var logout = function () {
        try {
            //  navigator.credentials.preventSilentAccess();
        } catch (e) {
        }

        try {
            // IE has a simple solution for it - API:
            document.execCommand("ClearAuthenticationCache");
        } catch (e) {
        }

        expresso.Common.sendRequest("authentication", "logout", null, null, {ignoreErrors: true})
            .fail(function () {
                // logout will return a 401 code
                window.location.reload(true);
            });
    };

    /**
     *
     * @param options login options
     * @return {*}
     */
    var login = function (options) {

        // first try with SSO if available
        var $ssoDeferred;
        if (expresso.Common.getSiteNamespace().config.Configurations.supportSSO &&
            !expresso.util.Util.getUrlParameter("securityToken") &&
            !expresso.util.Util.getUrlParameter("loginToken")) {
            // first try to use Windows credential SSO login
            expresso.Common.setAuthenticationPath("sso");
            $ssoDeferred = performLogin();
        } else {
            $ssoDeferred = $.Deferred().reject();
        }

        var $loginDeferred = $.Deferred();
        $ssoDeferred.done(function () {
            // success continue using Kerberos SSO
            $loginDeferred.resolve();
        }).fail(function () {
            // then try using rest
            expresso.Common.setAuthenticationPath("rest");
            performLogin(null, null, expresso.util.Util.getUrlParameter("loginToken")).done(function () {
                // success continue using Kerberos rest
                $loginDeferred.resolve();
            }).fail(function () {
                displayLoginPage().done(function () {
                    $loginDeferred.resolve();
                });
            });
        });

        var $deferred = $.Deferred();
        $loginDeferred.done(function () {
            loadProfileWhileDisplaySplashPage().done(function () {
                $deferred.resolve();
            });
        });
        return $deferred;
    };

    /**
     * utility method to retrieve the session token from the header and store it in the local cache
     * @param jqXHR
     */
    var storeSessionToken = function (jqXHR) {
        // keep the session token (needed to send a WS request)
        if (jqXHR && jqXHR.getResponseHeader("X-Session-Token")) {
            sessionToken = jqXHR.getResponseHeader("X-Session-Token");
            // console.log("Setting session token [" + sessionToken + "]");
            if (typeof (Storage) !== "undefined") {
                // console.log("Storing session token [" + sessionToken + "]");
                localStorage.setItem("sessionToken", sessionToken);
            }
        }
    };

    /**
     * Get the session token
     */
    var getSessionToken = function () {
        // always get it from the storage if possible
        // console.log("Getting session token [" + sessionToken + "]");
        if (typeof (Storage) !== "undefined") {
            sessionToken = localStorage.getItem("sessionToken");
            // console.log("Retrieving session token  from storage [" + sessionToken + "]");
        }
        return sessionToken;
    };

    /**
     * Method called when the DOM is ready
     */
    var init = function (name) {

        // set the default for Ajax calls
        $.ajaxSetup({
            //cache: false,
            //contentType: "application/json; charset=utf-8", // always sending JSON
            //dataType: "json", // always getting back JSON
            xhrFields: {
                withCredentials: true // send cookie if needed
            },
            beforeSend: function (xhr) {
                if (userProfile && userProfile.genericAccount) {
                    // do not send the version for system account
                } else {
                    var v = expresso.Common.getSiteNamespace().config.Configurations.version;
                    xhr.setRequestHeader("X-Version", v);
                }

                xhr.setRequestHeader("X-AppName", expresso.Main.getCurrentAppName());
                var sessionToken = getSessionToken();
                if (sessionToken) {
                    xhr.setRequestHeader("X-Session-Token", sessionToken);
                }

                if (impersonateUser) {
                    xhr.setRequestHeader("X-Impersonate-User", impersonateUser);
                }
            }
        });

        // on Javascript error, send an email to the support
        window.onerror = function (msg, url, lineNo, columnNo, error) {
            console.warn("Javascript error: " + msg + (error ? " (" + JSON.stringify(error) + ")" : ""));

            var substring = "script error";
            if (msg.toLowerCase().indexOf(substring) > -1) {
                // When an error occurs in a script, loaded from a different origin,
                // the details of the error are not reported to prevent leaking information
            } else {
                // var message = [
                //     "Application: " + expresso.Main.getCurrentAppName(),
                //     "User: " + (userProfile ? userProfile.label + " (" + userProfile.userName + ") - " + userProfile.email : ""),
                //     "Message: " + msg,
                //     "URL: " + url,
                //     "Line: " + lineNo,
                //     "Column: " + columnNo,
                //     "Error object: " + JSON.stringify(error)
                // ].join("\n<br>");
                // console.error(message);
                // expresso.Common.sendRequest("support/mail", "execute",
                //     "title=" + "Javascript error" + " &message=" + encodeURIComponent(message),
                //     null, {ignoreErrors: true});
            }
            return false;
        };
    };

    // return public properties and methods
    return {
        init: init,
        login: login,
        logout: logout,

        loadProfile: loadProfile,
        isUserAllowed: isUserAllowed,
        isUserInRole: isUserInRole,
        isPowerUser: isPowerUser,
        isAdmin: isAdmin,

        getUserInfo: getUserInfo,
        getImpersonateUser: getImpersonateUser,
        getUserFields: getUserFields,
        getPersonFields: getPersonFields,
        displayLoginPage: displayLoginPage,
        getSessionToken: getSessionToken,

        getIpAddress: getIpAddress,
        isInternalIpAddress: isInternalIpAddress,
        getTimeToLoadProfile: getTimeToLoadProfile
    };
}();
