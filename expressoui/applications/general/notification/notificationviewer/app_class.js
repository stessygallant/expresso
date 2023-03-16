expresso.applications.general.notification.notificationviewer.NotificationViewer = expresso.layout.applicationbase.ApplicationBase.extend({
    notificationTemplate: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.ApplicationBase.fn.initDOMElement.call(this, $domElement);
        var _this = this;
        var $div = this.$domElement.find(".notifications");

        // add listener for action buttons
        $div.on("click", ".buttons button", function () {
            var $button = $(this);
            var action = $button.data("action");
            var $notification = $button.closest(".notification");
            var notification = $notification.data("notification");
            _this.sendRequest("notification/" + notification.id, "execute", {actionSelected: action}, null,
                {waitOnElement: $notification}).done(function () {
                $notification.slideUp();

                // remove 1 from the service badge count
                var $notificationService = $notification.closest(".notification-service");
                var $serviceCountBadge = $notificationService.find("legend .count");
                $serviceCountBadge.data("count", $serviceCountBadge.data("count") - 1);
                $serviceCountBadge.text($serviceCountBadge.data("count"));

                // decrease the notification count in the bell
                var $notificationBell = $("body").find(".notification-bell");
                if ($notificationBell.length) {
                    var $badge = $notificationBell.find(".badge");
                    var count = parseInt($notificationBell.find(".badge").text()) - 1;
                    $badge.text(count);
                    if (count == 0) {
                        // change icon
                        $notificationBell.removeClass("fa-bell").addClass("fa-bell-o");

                        // remove badge
                        $notificationBell.find(".badge").remove();
                    }
                }
            });
        });

        // add listener for resource number
        $div.on("click", ".resource-no", function () {
            var $notification = $(this).closest(".notification");
            var notification = $notification.data("notification");

            if (notification.resourceUrl) {
                expresso.Common.displayUrl(notification.resourceUrl, notification.serviceDescription);
            } else {
                if (_this.getLabel(notification.resourceName + "NoResourceURL", null, true)) {
                    expresso.util.UIUtil.buildMessageWindow(_this.getLabel(notification.resourceName + "NoResourceURL"));
                } else {
                    expresso.util.UIUtil.buildMessageWindow(_this.getLabel("noResourceURL"));
                }
            }
        });

        // add listener for expand/collapse buttons
        $div.on("click", ".notification-expand-icon", function () {
            var $icon = $(this);
            var $notificationService = $icon.closest(".notification-service");
            $notificationService.find(".notification-div").slideToggle();
            if ($icon.hasClass("fa-angle-double-down")) {
                $icon.removeClass("fa-angle-double-down").addClass("fa-angle-double-up");
            } else {
                $icon.removeClass("fa-angle-double-up").addClass("fa-angle-double-down");
            }
        });

        // wait before building the template (form must be converted)
        window.setTimeout(function () {
            var $notificationTemplate = _this.$domElement.find(".notification-template");
            _this.notificationTemplate = kendo.template($notificationTemplate.html());

            _this.refreshView();
        }, 100);
    },

    /**
     *
     */
    refreshView: function () {
        var _this = this;
        var $div = this.$domElement.find(".notifications");

        // utility method the get a valid name from a description
        var purgeServiceDescription = function (serviceDescription) {
            return serviceDescription.replace(/[^a-zA-Z0-9]/g, '');
        };

        // show a loading mask until the end
        var maskId = "notif1";
        expresso.util.UIUtil.showLoadingMask(this.$domElement, true, {id: maskId});
        $.when(
            // get the list notification services
            this.sendRequest("notification/service", null, null, null, {waitOnElement: null}),

            // get my list of notifications
            this.sendRequest("notification/mine", null, null, null, {waitOnElement: null})
        ).done(function (serviceDescriptions, notifications) {

            if (!notifications.length) {
                $div.append("<div class='notification-msg'> " + _this.getLabel("noNotifications") + "</div>");
            } else {
                // build a list of service
                $.each(serviceDescriptions, function () {
                    var serviceDescription = this;
                    var serviceName = purgeServiceDescription(serviceDescription);
                    if (!$div.find(".notification-service[data-service='" + serviceName + "']").length) {
                        $div.append("<fieldset class='notification-service' data-service='" + serviceName + "'>" +
                            "<legend>" + serviceDescription +
                            " (<span class='count'></span>)" +
                            "<i class='fa fa-angle-double-up notification-expand-icon' ></i>" +
                            "</legend><div class='notification-div'></div>" +
                            "</fieldset>");
                    }
                });

                // build each notification
                $.each(notifications, function () {
                    // display the notification
                    var notification = this;
                    var $service = $div.find(".notification-service[data-service='" +
                        purgeServiceDescription(notification.serviceDescription) + "'] .notification-div");

                    // convert data
                    notification.notes = notification.notes || "";
                    notification.requestedDate = expresso.util.Formatter.formatDate(notification.requestedDate, expresso.util.Formatter.DATE_FORMAT.DATE/*_TIME*/);
                    notification.resourceStatusLabel = notification.resourceStatusPgmKey ? (_this.getLabel(notification.resourceName + notification.resourceStatusPgmKey.capitalize(), null, true) ||
                        _this.getLabel(notification.resourceStatusPgmKey, null, true) || _this.getLabel(notification.resourceName + notification.resourceStatusPgmKey.capitalize(), null, true) ||
                        notification.resourceStatusPgmKey.capitalize()) : "";

                    // build the template
                    var $notification = $(_this.notificationTemplate(notification));
                    $notification.data("notification", notification);

                    // hide elements
                    if (!notification.resourceStatusLabel) {
                        $notification.find(".resource-status").hide();
                    }
                    if (!notification.notes) {
                        $notification.find(".notes").hide();
                    }
                    if (!notification.requestedDate) {
                        $notification.find(".requested-date").hide();
                    }

                    // add buttons for each available actions
                    if (notification.availableActions) {
                        var $buttons = $notification.find(".buttons");
                        var actions = notification.availableActions.split(",");
                        $.each(actions, function (index) {
                            var action = this;
                            var title = (_this.getLabel(notification.resourceName + action.capitalize() + "ButtonTitle", null, true) ||
                                _this.getLabel(action + "ButtonTitle")).replace(/'/, "&apos;");
                            var label = _this.getLabel(notification.resourceName + action.capitalize() + "ButtonLabel", null, true) ||
                                _this.getLabel(action + "ButtonLabel");
                            $buttons.append("<button class='k-button exp-button" + (index == 0 ? " k-primary" : "") +
                                "' data-action='" + action + "' title='" + title + "'>" + label + "</button>"
                            );
                        });
                    }

                    // add the new notification
                    $notification.appendTo($service);
                });

                // display the number of notifications in service
                $div.find(".notification-service[data-service]").each(function () {
                    var $notificationService = $(this);
                    var count = $notificationService.find(".notification").length;
                    var text = $notificationService.find("legend").text();

                    var $serviceCountBadge = $notificationService.find("legend .count");
                    if (text.indexOf("À venir") != -1) {
                        $serviceCountBadge.data("count", -1).text("");
                    } else {
                        $serviceCountBadge.data("count", count).text(count);
                    }

                    // hide if count == 0
                    if (count == 0) {
                        $notificationService.hide();
                    }
                });

                // sort according to the number of notifications
                $div.find(".notification-service[data-service]").sort(function (a, b) {
                    var $a = $(a).find("legend");
                    var $b = $(b).find("legend");
                    // sort by count
                    // var ca = $a.find(".count").data("count");
                    // var cb = $b.find(".count").data("count");
                    // return ca > cb ? -1 : (cb > ca ? 1 : ($a.text().localeCompare($b.text())));

                    // sort by name
                    return $a.text().localeCompare($b.text());

                }).appendTo($div);
            }

            // then remove the loading mask
            expresso.util.UIUtil.showLoadingMask(_this.$domElement, false, {id: maskId});
        });
    },

    // @override
    destroy: function () {
        this.notificationTemplate = nulll

        expresso.layout.applicationbase.ApplicationBase.fn.destroy.call(this);
    }
});


