﻿var expresso = expresso || {};

/**
 * Main application. It uses the Javascript Module encapsulation pattern
 */
expresso.Main = function () {

    // reference to the DOM element of the single page
    var $mainDiv = undefined;
    var $contentDiv = undefined;
    var $titleDiv = undefined;
    var $menuDiv = undefined;

    // reference to the current application instance
    var currentAppInstance = undefined;
    var currentAppName = undefined;

    /**
     * Build the menu for the user
     */
    var buildMenu = function ($menu) {

        // we need to extend the menu System with the menu Applications
        var menus = expresso.Common.getSiteNamespace().config.Menu;
        var menuLabels = expresso.Common.getSiteNamespace().config.menu.Labels;

        // if the menu already exists, destroy it
        if ($menu.data("kendoPanelBar")) {
            $menu.data("kendoPanelBar").destroy();
            $menu.empty();
        }

        $menuDiv.find(".search-menu")
            .attr("placeholder", expresso.Common.getLabel("searchMenu")) // add placeholder text
            .off()  // remove event listener before adding new ones
            .on("change paste keyup search", function () {
                // put back on menu
                $menu.find("li").show();

                var text = $(this).val();
                //console.log("**** [" + text + "]");
                if (text) {
                    text = text.toLowerCase();
                    // filter out all non-matching item
                    $menu.find("li span[data-app]").each(function () {
                        var $span = $(this);
                        var $li = $span.closest("li");
                        var menuLabel = $span.text().toLowerCase();
                        var appName = $span.data("app").toLowerCase();
                        if (menuLabel.indexOf(text) == -1 && appName.indexOf(text) == -1) {
                            $li.hide();
                        }
                    });

                    // then hide any li with no more visible li
                    $menu.find(">li").each(function () {
                        var $li = $(this);
                        if (!$li.find("li:visible").length) {
                            $li.hide();
                        }
                    });

                    // hide the favorites menu
                    $menu.find(".favorites").closest("li").hide();
                }
            });

        // build the menu
        var menuDataSource = [];

        // inline function to build a menu item for Kendo Panel Bar
        var buildMenuItem = function (item) {
            var menuItem;

            if (item.group) {
                menuItem = {
                    text: "<span>" + "<span class='icon fa fa-" + item.icon + "'></span>" + "<span class='text " + (item.class ? item.class : "") + "'>" +
                        menuLabels[item.group] + "</span>" + "</span>",
                    encoded: false,
                    expanded: true,
                    items: []
                };

                // build sub items
                $.each(item.items, function (index, subItem) {
                    menuItem.items.push(buildMenuItem(subItem));
                });
            } else if (item.url) {
                // link
                item.icon = item.icon || "fa-external-link";
                var url = item.url;
                if (!url.startsWith("http") && !url.startsWith("mailto") && item.location != "local") {
                    url = expresso.Common.getWsResourcePathURL() + url;
                }
                menuItem = {
                    text: "<span>" + "<span class='icon fa'></span>" + "<span class='text' data-app='" + item.appName + "'>" +
                        menuLabels[item.appName] + "</span>" + "</span>",
                    url: url,
                    encoded: false
                };
            } else {
                var menuText = menuLabels[item.appName];
                if (menuText && typeof menuText !== "string") {
                    menuText = menuText.shortLabel;
                }
                menuItem = {
                    text: "<span>" + "<span class='icon fa fa-star'></span>" + "<span class='text' data-app='" + item.appName + "'>" +
                        menuText + "</span>" + "</span>",
                    encoded: false
                };
            }

            return menuItem;
        };

        // inline function to find a menu item
        var findMenuItem = function (menu, appName) {
            var i, l, item;
            if ($.isArray(menu)) {
                for (i = 0, l = menu.length; i < l; i++) {
                    item = findMenuItem(menu[i], appName);
                    if (item) {
                        return item;
                    }
                }
            } else if (menu.items) {
                for (i = 0, l = menu.items.length; i < l; i++) {
                    item = findMenuItem(menu.items[i], appName);
                    if (item) {
                        return item;
                    }
                }
            } else if (menu.appName == appName) {
                return menu;
            }
            return null;
        };

        // inline function to determine if the menu item is in the favorite
        var isFavorite = function (appName) {
            var favorites = expresso.Security.getUserProfile().favorites;
            for (var i = 0, l = favorites.length; i < l; i++) {
                if (favorites[i] == appName) {
                    return true;
                }
            }
            return false;
        };

        // put favorite menu first
        if (expresso.Security.getUserProfile().favorites.length) {
            var menuFavorites = {group: "Favorites", icon: "star", items: [], class: "favorites"};
            $.each(expresso.Security.getUserProfile().favorites, function (index, fav) {
                var item = findMenuItem(menus, fav);
                if (item) {
                    menuFavorites.items.push(item);
                }
            });
            menuDataSource.push(buildMenuItem(menuFavorites));
        }

        // then add others menu
        $.each(menus, function (index, menu) {
            menuDataSource.push(buildMenuItem(menu));
        });

        // build the menu
        $menu.kendoPanelBar({
            dataSource: menuDataSource
        });

        // add favorites icon on mouse over and handle click on icon
        $menu.find(".k-link .fa").click(function (e) {
            e.preventDefault();
            e.stopPropagation();

            // if the user click on the star, add or remove the app from the favorite

            // update the favorite list
            var $this = $(this);
            var appName = $this.next().data("app");
            if (appName) {
                var favorite = isFavorite(appName);
                if (favorite) {
                    // remove favorite
                    var index = expresso.Security.getUserProfile().favorites.indexOf(appName);
                    if (index !== -1) {
                        expresso.Security.getUserProfile().favorites.splice(index, 1);
                    }
                } else {
                    // add new favorite
                    expresso.Security.getUserProfile().favorites.push(appName);
                }

                // rebuild the menu
                buildMenu($menu);

                // save favorites
                saveFavorites();
            }
        });

        // we need to remove all applications that the user does not have access
        $menu.find("[data-app]").each(function () {
            var $this = $(this);
            var appName = $this.data("app");
            if (expresso.Common.getApplication(appName) && expresso.Common.getApplication(appName).allowed) {
                // ok
            } else {
                $this.closest("li").remove();
            }
        });

        // find all empty group and remove then
        $menu.find("ul:empty").remove();

        // find all empty menuitem and remove then
        $menu.children("li").each(function () {
            // if they are empty, remove then
            var $this = $(this);
            if (!$this.find("[data-app]").length && !$this.find("a.k-link").length) {
                $this.remove();
            }
        });

        // all link should target=_blank
        $menu.find("a.k-link").attr("target", "_blank");

        // if not in prod, highlight the menu
        if (!expresso.Common.isProduction()) {
            $menu.css("background-color", "#2cff0042");
        }

        // handle event when item is selected
        // there is no clean way to associate an action to an menu item
        // we will use the data source to get the associated action based on the text
        $menu.data("kendoPanelBar").bind("select", function (e) {
            var $menuItem = $(e.item);
            var appName = $menuItem.children(".k-link").find("[data-app]").data("app");
            var href = $menuItem.children("a.k-link");

            // must have an appName but must not be a href
            if (appName && !href.length) {
                // verify if the user can load this application
                if (expresso.Common.getApplication(appName) && expresso.Common.getApplication(appName).allowed) {
                    // load the application
                    // keeps the paramaters on the URL
                    var options = {
                        queryParameters: expresso.util.Util.getUrlParameters()
                    };
                    delete options.queryParameters.app;
                    delete options.queryParameters.id;
                    loadMainApplication(appName, options);
                } else {
                    // no access
                }
            } else {
                // do nothing.
            }
        });

        // Add support for Drag&Drop for favorites
        supportDragDropFavorites($menu);
    };

    /**
     *
     */
    var supportDragDropFavorites = function ($menu) {
        var $favorites = $menu.find(".favorites").closest("li").find(".k-item");
        $favorites
            .attr("draggable", "true")
            .on("dragstart", function (e) {
                var $link = $(e.target);
                var appName = $link.find("[data-app]").data("app");
                console.log("Moving [" + appName + "]");
                var dataTransfer = e.dataTransfer || e.originalEvent.dataTransfer;
                dataTransfer.setData("text", appName);
            });

        $favorites
            .on("dragover", function (e) {
                e.preventDefault(); // this allow the drop
            })
            .on("dragenter", function (e) {
                var $target = $(e.target);
                if ($target.hasClass("text")) {
                    $target.css("border-top", "2px dotted red");
                }
            })
            .on("dragleave", function (e) {
                var $target = $(e.target);
                if ($target.hasClass("text")) {
                    $target.css("border-top", "");
                }
            })
            .on("drop", function (e) {
                e.preventDefault();
                var dataTransfer = e.dataTransfer || e.originalEvent.dataTransfer;
                var appName = dataTransfer.getData("text");
                var $target = $(e.target);
                var targetAppName;
                if ($(e.target).hasClass("text")) {
                    targetAppName = $target.data("app");
                    $target.css("border-top", "");
                    $target = $target.closest(".k-item");
                } else {
                    targetAppName = $target.find("[data-app]").data("app");
                }

                // move the item
                $target.parent().find("[data-app='" + appName + "']").closest(".k-item").insertBefore($target);

                // modify favorites
                var sourceIndex = expresso.Security.getUserProfile().favorites.indexOf(appName);
                var targetIndex = expresso.Security.getUserProfile().favorites.indexOf(targetAppName);
                console.log("Moved [" + appName + "] from [" + sourceIndex + "] to [" + targetIndex + "] (" + targetAppName + ")");

                // remove at the current position
                expresso.Security.getUserProfile().favorites.splice(sourceIndex, 1);

                // insert at the new position
                expresso.Security.getUserProfile().favorites.splice(targetIndex < sourceIndex ? targetIndex : targetIndex - 1,
                    0, appName);

                saveFavorites();
            });
    };

    /**
     * save favorites
     */
    var saveFavorites = function () {
        var config = {
            type: "userConfig",
            id: expresso.Security.getUserProfile().favoritesId,
            userId: expresso.Security.getUserProfile().id,
            key: "MenuFavorites",
            value: JSON.stringify(expresso.Security.getUserProfile().favorites)
        };
        expresso.Common.sendRequest("user/" + expresso.Security.getUserProfile().id + "/config" +
            (expresso.Security.getUserProfile().favoritesId ? "/" + expresso.Security.getUserProfile().favoritesId : ""),
            (expresso.Security.getUserProfile().favoritesId ? "update" : "create"), config).done(function (updatedConfig) {
            expresso.Security.getUserProfile().favoritesId = updatedConfig.id;
        });
    };

    /**
     * If a hash is defined on the URL, auto select the menu item
     */
    var getDefaultApplication = function () {
        var appName;
        var options = {};

        // first the application from the hash
        if (window.location.hash) {
            appName = window.location.hash.substring(1);

            // if the hash contain options (...), strim them
            if (appName.indexOf('(') != -1) {
                appName = appName.substring(0, appName.indexOf('('));
            }

            // if there is arguments on the hash, use it for the application
            if (window.location.hash.indexOf("(") != -1) {
                var hash = window.location.hash;
                hash = hash.substring(hash.indexOf('(') + 1);
                var field = hash.substring(0, hash.indexOf('-'));
                var value = hash.substring(hash.indexOf('-') + 1, hash.indexOf(')'));
                var s = hash.substring(hash.indexOf(')') + 1);
                if (s.length) {
                    var id = s.substring(s.lastIndexOf("(") + 1, s.lastIndexOf(")"));
                    options.queryParameters = options.queryParameters || {};
                    options.queryParameters.id = id;
                }

                $.extend(options, {
                    autoEdit: true,
                    initialFilter: {field: field, value: value, operator: "eq"}
                });
            }
        }

        // then from the app URL query parameter
        if (!appName) {
            if (expresso.util.Util.getUrlParameter("app")) {
                appName = expresso.util.Util.getUrlParameter("app");
            }
        }

        // verify if the user has a default application
        if (!appName) {
            //console.log("expresso.Security.getUserProfile().userInfos", expresso.Security.getUserProfile().userInfos);
            if (expresso.Security.getUserProfile().userInfos && expresso.Security.getUserProfile().userInfos['defaultApplication'] &&
                expresso.Security.getUserProfile().userInfos['defaultApplication'].stringValue) {
                appName = expresso.Security.getUserProfile().userInfos['defaultApplication'].stringValue;
            }
        }

        // otherwise the default application from the config
        if (!appName) {
            appName = expresso.Common.getSiteNamespace().config.Configurations.defaultApplication;
        }

        if (!appName) {
            // get the first application in the menu
            $("ul.main-menu [data-app]:first").each(function () {
                appName = $(this).data("app");
            });
        }

        // select the application in the menu
        var $menu = $(".main-menu");
        var $appItem = $menu.find("[data-app='" + appName + "']").first().parent().parent();

        console.log("Auto Loading [" + appName + "]");
        if ($appItem && $appItem.length) {
            // $menu.data("kendoPanelBar").select($appItem);
        }
        return {appName: appName, options: options};
    };


    /**
     *
     */
    var verifySystemMessages = function () {
        var refreshDelayInSeconds = 1 * 60; // n minutes
        var $body = $("body");

        // get the messages
        expresso.Common.sendRequest("systemMessage", null, null,
            null, {ignoreErrors: true, waitOnElement: null}).done(function (systemMessages) {
            if (systemMessages.total > 0) {
                // get the first one
                var systemMessage = systemMessages.data[0].message;

                // make room for the message
                if (!$body.find("div.exp-system-message").length) {
                    $body.append("<div class='exp-system-message'><span class='message'>" + systemMessage + "</span><span class='close-button'>X</span></div>");
                    $body.find("div.exp-system-message .close-button").on("click", function () {
                        $body.find("div.exp-system-message").slideUp();
                    });
                } else {
                    $body.find("div.exp-system-message .message").text(systemMessage);
                }
            } else {
                $body.find("div.exp-system-message").remove();
            }
        }).always(function () {
            window.setTimeout(verifySystemMessages, refreshDelayInSeconds * 1000);
        });
    };

    /**
     *
     */
    var registerProfileViewer = function () {

        // add event handler on the profile menu
        $(".main .user-div").on("click", ".settings", function (e) {
            e.preventDefault();

            // load the profile window
            var profilePage = expresso.Common.getSiteNamespace().config.Configurations.profileHtmlPage ?
                expresso.Common.getSiteNamespace().config.Configurations.profileHtmlPage : "expresso/ext/profile.html";

            var buttons = ["<button type='button' class='k-button rm-logout-button'>" + expresso.Common.getLabel("logout") + "</button>"];

            expresso.util.UIUtil.buildWindow(profilePage, {
                title: expresso.Common.getLabel("profile"),
                top: 5, // always put the window at the top (let place for user info)
                buttons: buttons,
                open: function () {
                    var $windowDiv = this;

                    var $form = $windowDiv.find("form");
                    var userProfile = expresso.Security.getUserProfile();

                    $windowDiv.find("[name=firstName]").setval(userProfile.firstName);
                    $windowDiv.find("[name=lastName]").setval(userProfile.lastName);
                    $windowDiv.find("[name=email]").setval(userProfile.email);
                    $windowDiv.find("[name=phoneNumber]").setval(userProfile.phoneNumber);
                    $windowDiv.find("[name=language]").setval(userProfile.language);

                    //set the password ONLY if it's not sso authentification
                    if (expresso.Common.getAuthenticationPath() == "sso" || !userProfile.localAccount) {
                        $windowDiv.find("[name=password]").closest("div").hide();
                    }

                    // if the user is a local user, do not allow change the profile
                    if (userProfile.genericAccount) {
                        expresso.util.UIUtil.setFormReadOnly($form);
                    }
                },
                save: function () {
                    var $windowDiv = this;

                    // save profile
                    var profile = $.extend({}, expresso.Security.getUserProfile(), {
                        firstName: $windowDiv.find("[name=firstName]").val(),
                        lastName: $windowDiv.find("[name=lastName]").val(),
                        email: $windowDiv.find("[name=email]").val(),
                        phoneNumber: $windowDiv.find("[name=phoneNumber]").val(),
                        language: $windowDiv.find("[name=language]").val()
                    });

                    profile = expresso.Common.purgeResource(profile);

                    if ($windowDiv.find("[name=password]").val()) {
                        profile.password = $windowDiv.find("[name=password]").val();
                    }

                    return expresso.Common.sendRequest("user/" + expresso.Security.getUserProfile().id, "update", profile).done(function (profile) {
                        if (profile.language != expresso.Security.getUserProfile().language) {
                            //expresso.Common.setLanguage(profile.language);
                            location.reload();
                        } else {
                            $.extend(expresso.Security.getUserProfile(), profile);
                        }
                    });
                },
                buttonClicked: function (e) {
                    if (e) {
                        e.preventDefault();
                    }
                    expresso.Security.logout();
                }
            });
        });
    };

    /**
     * init the list of notifications
     */
    var initNotifications = function () {
        if (expresso.Common.isProduction() && expresso.Security.isUserInRole("NotificationViewer.viewer")) {
            var $div = $(".main .user-div");

            // add a listener to the tooltip
            $("body").on("click", ".notification-tooltip", function () {
                $(this).hide();
                loadMainApplication("NotificationViewer");
            });

            // add a listener to the bell
            var $notificationBell = $div.find(".notification-bell");
            $notificationBell.on("click", function () {
                loadMainApplication("NotificationViewer");
            });

            // check notifications now (in 2 seconds)
            window.setTimeout(function () {
                checkNotifications($notificationBell);
            }, 2 * 1000);

            // then check notifications every n minutes
            window.setInterval(function () {
                checkNotifications($notificationBell);
            }, (expresso.Common.isProduction() ? 10 : 1) * 60 * 1000);
        }
    };

    /**
     * @param $notificationBell
     */
    var checkNotifications = function ($notificationBell) {
        expresso.Common.sendRequest("notification/mine", null, null, null,
            {waitOnElement: null, ignoreErrors: true}).done(function (notifications) {
            // update the bell
            if (notifications.length) {
                // if new notification, show tooltip
                var ago15Minutes = new Date().addMinutes(-15);
                var countNewNotifications = $.grep(notifications, function (n) {
                    return expresso.util.Formatter.parseDateTime(n.creationDate) > ago15Minutes;
                }).length;
                var displayTooltip = $notificationBell.hasClass("fa-bell-o") || countNewNotifications;

                if (displayTooltip) {
                    $notificationBell.removeClass("fa-bell-o").addClass("fa-bell");
                    $notificationBell.find(".badge").text(notifications.length);

                    // displayTooltip
                    var $notification = $("<div class='notification-tooltip'>" +
                        (countNewNotifications ?
                            expresso.Common.getLabel("newNotifications", null,
                                {quantity: countNewNotifications}) + "<br>" : "") +
                        expresso.Common.getLabel("notifications", null, {quantity: (notifications.length - countNewNotifications)}) +
                        "</div>").appendTo($("body"));

                    // display the notifications for a few seconds then remove it
                    window.setTimeout(function () {
                        $notification.fadeOut(1000);
                        window.setTimeout(function () {
                            $notification.remove();
                        }, 2 * 1000);
                    }, 5 * 1000);
                }

                $.each(notifications, function () {
                    var notification = this;
                    if (notification.important) {
                        expresso.util.UIUtil.buildMessageWindow(notification.resourceTitle + " - " + notification.description);
                    }
                });
            } else {
                if ($notificationBell.hasClass("fa-bell")) {
                    $notificationBell.removeClass("fa-bell").addClass("fa-bell-o");
                    $notificationBell.find(".badge").text("");
                }
            }
        });
    };

    /**
     * init the list of notifications
     */
    var initDuties = function () {
        if (expresso.Security.isUserInRole("DutyUserManager.user") && !expresso.Security.isAdmin()) {
            var $div = $(".main .user-div");

            window.setTimeout(function () {
                expresso.Common.sendRequest("duty/mine", null, null, null,
                    {waitOnElement: null, ignoreErrors: true}).done(function (duties) {
                    if (duties.length) {
                        $div.append("<div class='duties'><button class='k-button exp-button on-duty-button'>" + expresso.Common.getLabel("duties") + "</button></div>");

                        resizeContent();

                        // Utility method
                        var displayOnDutyDialog = function () {
                            expresso.Common.displayApplication("DutyController", expresso.Common.getLabel("duties"), {}, {
                                height: 200,
                                width: 800
                            }).done(function () {
                                // refresh the button
                                // $div.find(".duties .on-duty-button").addClass("on-duty");
                            });
                        };

                        // listen on duty
                        $div.find(".duties .on-duty-button").on("click", function (e) {
                            e.preventDefault();
                            displayOnDutyDialog();
                        });

                        // display the duties dialog if not already on duty
                        expresso.Common.sendRequest("duty/0/user", null, null, null,
                            {waitOnElement: null, ignoreErrors: true}).done(function (dutyUsers) {
                            var myUserId = expresso.Security.getUserProfile().id;
                            var onDuty = $.grep(dutyUsers.data, function (d) {
                                return d.userId == myUserId;
                            }).length > 0;
                            if (!onDuty) {
                                displayOnDutyDialog();
                            }
                        });
                    }
                });
            }, 2 * 1000);
        }
    };

    var resizeContent = function () {
        var $body = $("body");
        var $mainHeader = $body.find(".main-header");
        var $menu = $menuDiv.find(".main-menu");
        var $searchMenuDiv = $menuDiv.find(".search-menu-div");
        var $userDiv = $menuDiv.find(".user-div");

        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
            $menu.height($menuDiv.height() - $mainHeader.outerHeight(true) - $userDiv.outerHeight(true) - $searchMenuDiv.outerHeight(true));
        } else {
            $menu.height($menuDiv.height() - $userDiv.outerHeight(true) - $searchMenuDiv.outerHeight(true));
        }

        // console.log("$body: " + $body.height());
        // console.log("$mainHeader: " + $mainHeader.height());
        // console.log("$menuDiv: " + $menuDiv.height());
        // console.log("$searchMenuDiv: " + $searchMenuDiv.height());
        // console.log("$userDiv: " + $userDiv.height());
        // console.log("$menu result: " + $menu.height());
    };

    /**
     * Initialization of the UI (menu and application)
     */
    var initUI = function () {
        // the content must fit the window (set the height of the window)
        var $body = $("body");
        $mainDiv = $body.find(".main");
        $titleDiv = $mainDiv.find(".main-title .title");
        $contentDiv = $mainDiv.find(".main-content");
        $menuDiv = $mainDiv.find(".main-menu-div");

        var $mainDiv2 = $mainDiv.find(".main-div");
        var $menu = $mainDiv.find(".main-menu");
        var $footerDiv = $mainDiv.find(".main-footer");
        var $userDiv = $mainDiv.find(".user-div");

        var noMenu = expresso.util.Util.getUrlParameter("noMenu");

        if (!noMenu) {
            // build the menu
            buildMenu($menu);
        } else {
            // hide menu
            $menuDiv.hide();
            $mainDiv2.css("left", 0).width("100%");
        }

        // initialize the footer
        $footerDiv.html(expresso.Common.getLabel("footerDisclaimer"));

        // initialize the version section
        var $versionDiv = $body.find(".version");  // 2 versions: in the title (tablet) and the footer (desktop)
        $versionDiv.html("v" + expresso.Common.getSiteNamespace().config.Configurations.version);

        $versionDiv.on("click", function () {
            // display release notes report
            expresso.Main.sendReportRequest("releasenotes", "request", null);
        });

        // initialize the user profile section
        var userName = expresso.Security.getUserProfile().firstName + " " + expresso.Security.getUserProfile().lastName;
        if (userName.length > 18) {
            userName = userName.substring(0, 18);
        }
        $userDiv.find(".username").text(userName);

        // listen for logo
        $body.on("click", ".logo", function () {
            // reload page no query parameter
            window.location = window.location.href.split("?")[0];
        });

        // // display the logs file on demand
        // $infoDiv.find(".fa-fire").on("click", function () {
        //     sendRequest("support/logs", "execute", {logFileName: siteName + "ws"}).done(function () {
        //         expresso.util.UIUtil.buildMessageWindow("Un courriel a été envoyé au support technique pour le Portail.");
        //     });
        // });

        // if it is a super user, allow it to change user
        if (!noMenu && expresso.Security.isAdmin()) {
            // allow the superuser to change the user
            $userDiv.find(".username").off().on("click", function (e) {
                e.preventDefault();
                var $userSelector = $("<div class='exp-user-selector'><select></select></div>").appendTo($body);
                expresso.util.UIUtil.buildComboBox($userSelector.find("select"), "user", {
                    triggerChangeOnInit: false,
                    change: function () {
                        var user = this.dataItem();
                        // load the new profile and reinit the application
                        expresso.Security.loadUserProfile(user ? user.userName : null).done(function () {
                            $userDiv.find(".username").text(expresso.Security.getUserProfile().firstName + " " + expresso.Security.getUserProfile().lastName);
                            buildMenu($menu);

                            // select the application
                            var appName = expresso.util.Util.getUrlParameter("app");
                            if (expresso.Common.getApplication(appName) && expresso.Common.getApplication(appName).allowed) {
                                loadMainApplication(appName);
                            } else {
                                loadMainApplication(null);
                            }
                        });
                    }
                });
            });
        }

        // Listen to HTML 5 History API to select the correct menu item on navigation
        $(window).on("popstate", function () {
            var state = history.state;
            var appName = state ? state.appName : null;
            var options = state ? state.options : null;
            var appItem = $menu.find("[data-app=" + appName + "]").first().parent().parent();
            if (appItem) {
                $menu.data("kendoPanelBar").select(appItem);
                loadMainApplication(appName, options);
            }
        });

        // on window resize
        $(window).smartresize(function () {
            resizeContent();
        });
        $(window).trigger("resize");

        if (!noMenu) {
            if (expresso.Common.getSiteNamespace().config.Configurations.supportSystemMessage) {
                verifySystemMessages();
            }

            if (expresso.Common.getSiteNamespace().config.Configurations.supportNotifications) {
                initNotifications();
            }

            if (expresso.Common.getSiteNamespace().config.Configurations.supportDuties) {
                initDuties();
            }
        }

        // now we can display the application
        var defaultApplication = getDefaultApplication();
        if (defaultApplication) {
            // add the URL query parameters to the options
            defaultApplication.options.queryParameters = defaultApplication.options.queryParameters || {};
            $.extend(defaultApplication.options.queryParameters, expresso.util.Util.getUrlParameters());
            loadMainApplication(defaultApplication.appName, defaultApplication.options);
        }

        // listen button to hide/show menu
        var $showMenuButton = $mainDiv.find(".main-settings .show-menu-button");
        $showMenuButton.on("click", function () {
            // show/hide menu
            if ($menuDiv.is(":visible")) {
                $menuDiv.hide();
            } else {
                $menuDiv.show();
                resizeContent();
            }
        });

        // listen button to switch to tablet
        var $switchTabletButton = $mainDiv.find(".main-settings .switch-tablet-button");
        $switchTabletButton.attr("title", expresso.Common.getLabel("switchToTablet"));
        $switchTabletButton.on("click", function () {
            window.location = window.location + "&screenMode=tablet";
        });

        // now we can show the main div
        $mainDiv.show();
    };

    /**
     * Method called when the DOM is ready
     * @param name name of the site. To be used to find configuration and menu file.
     */
    var init = function (name) {
        expresso.Security.init(name);

        // initialize the Util with the current site
        expresso.Common.init(name).done(function () {
            expresso.Security.login({}).done(function () {
                displaySplashPage().done(function () {
                    initUI();
                });
            });

            // register a listener to the profile button
            registerProfileViewer();
        });
    };

    /**
     *
     * @param report
     * @param resourceName
     * @param params
     * @param [customParamPage] true if there is a custom param page
     */
    var executeReport = function (report, resourceName, params, customParamPage) {
        if (customParamPage) {
            expresso.util.UIUtil.buildWindow(report.path, {

                labels: report.labels,
                title: report.label,
                saveButtonLabel: expresso.Common.getLabel("executeReport"),
                save: function () {
                    var $windowDiv = $(this);

                    var serializeForm = function ($form) {
                        var obj = {};
                        $.each($form.find(":input[name]"), function () {
                            var name = this.name;
                            if (name) {
                                if (this.type == "checkbox" || this.type == "radio") {
                                    if (this.checked) {
                                        obj[name] = this.value;
                                    }
                                } else {
                                    obj[name] = this.value;
                                }
                            }
                        });
                        return obj;
                    };

                    // get all inputs
                    var formParams = serializeForm($windowDiv);
                    params = $.extend({}, params || {}, formParams);
                    sendReportRequest(report.name, resourceName, params);
                }
            });
        } else {
            sendReportRequest(report.name, resourceName, params || {});
        }
    };

    /**
     *
     * @param resourceSecurityPath
     * @returns {string}
     */
    var getResourceNameFromResourceSecurityPath = function (resourceSecurityPath) {
        if (resourceSecurityPath && resourceSecurityPath.indexOf('/') != -1) {
            if (resourceSecurityPath.startsWith("/")) {
                resourceSecurityPath = resourceSecurityPath.substring(1);
            }
            var s = resourceSecurityPath.split("/");
            var resourceName = s[0];
            for (var i = 1; i < s.length; i++) {
                resourceName += s[i].capitalize();
            }
            return resourceName;
        } else {
            return resourceSecurityPath;
        }
    };

    /**
     * Send a request to the server to print the report
     * @param reportName
     * @param resourceSecurityPath
     * @param params
     * @param [target] by default, blank
     * @param [useForm] by default, true
     */
    var sendReportRequest = function (reportName, resourceSecurityPath, params, target, useForm) {
        var url, p;

        target = target || "_blank";
        params = params || {};
        params.format = params.format || "pdf";
        var resourceName = getResourceNameFromResourceSecurityPath(resourceSecurityPath);

        if (expresso.Security.isUserAllowed(resourceSecurityPath, "read" /* download */, true)) {
            if (useForm !== false) {
                url = expresso.Common.getWsResourcePathURL() + "/report?action=execute" +
                    "&_=" + new Date().getTime() + "&_format=." + params.format;

                var $form = $("<form method='post' action='" + url + "' target='" + target + "' hidden></form>");
                for (p in params) {
                    $form.append("<input name='" + p + "' value='" + params[p] + "'>");
                }

                // add mandatory parameter
                $form.append("<input name='reportName' value='" + reportName + "'>");
                $form.append("<input name='resourceName' value='" + resourceName + "'>");
                $form.append("<input name='resourceSecurityPath' value='" + resourceSecurityPath + "'>");

                // now add the token as a param
                $form.append("<input name='sessionToken' value='" + expresso.Security.getSessionToken() + "'>");

                $form.appendTo("body").submit();
                window.setTimeout(function () {
                    $form.remove();
                }, 1000);
            } else {
                url = expresso.Common.getWsResourcePathURL() + "/report/execute" + "?reportName=" +
                    encodeURIComponent(reportName) + "&resourceName=" + encodeURIComponent(resourceName)
                    + "&resourceSecurityPath=" + encodeURIComponent(resourceSecurityPath);
                for (p in params) {
                    url += "&" + p + "=" + encodeURIComponent(params[p]);
                }

                // to help the browser, add the report format to the end of the url
                params.format = params.format || "pdf";
                url += "&_=" + new Date().getTime() + "&_format=." + params.format;
                // console.log(url);

                var $href = $("<a href='" + url + "' target='" + target + "' hidden></a>");
                $href.appendTo("body")[0].click();
                $href.remove();
            }
        }
    };

    /**
     *
     */
    var displaySplashPage = function () {
        var $deferred = $.Deferred();
        if (!expresso.Common.getSiteNamespace().config.Configurations.supportWelcomeMessage ||
            expresso.Common.getAuthenticationPath() != "sso") {
            $deferred.resolve();
        } else {
            var labels = expresso.Labels;
            var $overlayDiv = $("<div class='overlay-content'></div>").appendTo($("body"));
            expresso.Common.loadHTML($overlayDiv, "expresso/ext/splash.html", labels).done(function ($div) {
                //$div.find(".title").show().html(expresso.Common.getLabel("splashTitle"));

                var today = new Date();

                var filter = [];
                filter.push({
                    logic: "and",
                    filters: [{
                        logic: "or",
                        filters: [{
                            logic: "and",
                            filters: [{
                                field: "startDate",
                                operator: "lte",
                                value: expresso.util.Formatter.formatDate(today)
                            }, {
                                field: "endDate",
                                operator: "gte",
                                value: expresso.util.Formatter.formatDate(today)
                            }]
                        }, {
                            logic: "and",
                            filters: [{
                                field: "startDate",
                                operator: "lte",
                                value: expresso.util.Formatter.formatDate(today)
                            }, {
                                field: "endDate",
                                operator: "eq",
                                value: null
                            }]
                        }]
                    }, {
                        logic: "or",
                        filters: [{
                            field: "language",
                            operator: "eq",
                            value: null
                        }, {
                            field: "language",
                            operator: "eq",
                            value: expresso.Common.getLanguage()
                        }]
                    }]
                });

                expresso.Common.sendRequest("welcomeMessage", null, null,
                    expresso.Common.buildKendoFilter(filter))
                    .done(function (welcomeMessages) {
                        // when all information about the user and his profile are loaded, initialize the UI

                        var displayTimeInSeconds = 0;

                        // display message only for internal employee
                        if (welcomeMessages.data.length > 0) {
                            var welcomeMessage = welcomeMessages.data[0];

                            displayTimeInSeconds = welcomeMessage.duration;

                            if (welcomeMessage.message) {
                                $div.find(".message").append("<p>" + welcomeMessage.message + "</p>");
                            }

                            $div.find(".message-file").height($div.height() - ($div.find(".logo").outerHeight(true) +
                                +$div.find(".message").outerHeight(true) + 70)); // 70 is margin

                            if (welcomeMessage.fileName) {
                                var imageUrl = expresso.Common.getWsResourcePathURL() + "/welcomeMessage/" + welcomeMessage.id + "/file/" + welcomeMessage.fileName;
                                $div.find(".message-file").append("<img src='" + imageUrl + "' alt=''>");
                            }

                            // hide the title
                            $div.find(".title").hide();
                        }

                        // show the content page after the delay
                        var messageTimeout = window.setTimeout(function () {
                            $div.off();
                            $overlayDiv.remove();
                            $deferred.resolve();
                        }, displayTimeInSeconds * 1000);

                        // add a skip message functionnality
                        $div.one("click", function () {
                            clearTimeout(messageTimeout);
                            $overlayDiv.remove();
                            $deferred.resolve();
                        });
                    });
            });
        }
        return $deferred;
    };

    /**
     * Load the application at the path. Then instantiate the application
     * @param appName name of the application
     * @param [options] optional options for the application
     * @returns {*} a Promise when the script is loaded
     */
    var loadMainApplication = function (appName, options) {
        options = options || {};
        // console.log("loadMainApplication [" + appName + "]", options);

        if (expresso.Common.getScreenMode() != expresso.Common.SCREEN_MODES.DESKTOP) {
            $menuDiv.hide();
        }

        var app = expresso.Common.getApplication(appName);
        expresso.Common.doNotDisplayAjaxErrorMessage(false);

        // give a chance to the application to terminate
        if (currentAppInstance) {

            // hide the content immediately (otherwise there is a alight delay between the click and the changes)
            $contentDiv.hide();
            $titleDiv.text("");

            destroyCurrentAppInstance();
        }

        // load the application
        if (app) {
            // for main application only, verify if the user is allowed to display it
            if (!app.allowed && !expresso.Security.isAdmin() && appName.indexOf(".") == -1) {
                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("userNotAllowedToLoadApplication", null,
                    {title: app.title}));
            } else {
                // then load the new application
                if (app.options) { // if defined in the applications.js
                    options = $.extend(options || {}, app.options);
                }
                console.log("Loading main application [" + appName + "]", options);
                return expresso.Common.loadApplication(app, options, $contentDiv).done(function (appInstance) {
                    currentAppInstance = appInstance;
                    currentAppName = appName;

                    expresso.Common.addApplicationToHistory(app, appName, options);

                    // now show the content
                    $contentDiv.show();

                    // get the title of the application
                    var menuText = expresso.Common.getSiteNamespace().config.menu.Labels[appName];
                    if (menuText && typeof menuText !== "string") {
                        menuText = menuText.label; // use the full text if available
                    }
                    //console.log(appName + ": " + menuText);
                    $titleDiv.text(menuText);

                    // render the application to the content div
                    appInstance.render(true).done(function () {
                        if (options && options.queryParameters &&
                            options.queryParameters["fullScreen"] !== undefined) {
                            appInstance.setFullScreenMode();
                        }

                        if (options && (options["noMenu"] || (options.queryParameters &&
                            options.queryParameters["noMenu"]))) {
                            appInstance.hideMenu();
                        }
                    });
                });
            }
        }
    };

    /**
     *
     */
    var destroyCurrentAppInstance = function () {
        if (currentAppInstance) {
            //console.log("Destroying currentAppInstance [" + currentAppName + "]");
            try {
                currentAppInstance.destroy();
            } catch (e) {
                // cannot do much
                //console.error(e);
            }
        }
        currentAppInstance = null;
        currentAppName = null;

        // clean the memory
        expresso.Common.getSiteNamespace().applications = undefined;
        expresso.Common.clearApplicationCache();

        // empty the content
        $contentDiv.empty();

        // then also remove any div added by the application
        $("body").children("div.hidden").each(function () {
            var $div = $(this);
            expresso.util.UIUtil.destroyKendoWidgets($div);
            $div.remove();
        });
    };

    /**
     *
     * @return {*}
     */
    var getCurrentAppName = function () {
        return currentAppName;
    };

    return {
        // PUBLIC methods
        init: init,
        loadMainApplication: loadMainApplication,
        executeReport: executeReport,
        sendReportRequest: sendReportRequest,
        getCurrentAppName: getCurrentAppName
    };
}();
