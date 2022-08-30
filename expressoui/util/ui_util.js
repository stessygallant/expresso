var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * This is an utility module. It contains some utilities method to build common UI widgets.
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.util.UIUtil = (function () {

        /**
         * Get a Kendo UI widget from a DOM element
         *
         * @param $element
         * @returns {*} the widget or null if the element is not a Kendo UI widget
         */
        var getKendoWidget = function ($element) {
            var role = $element.data("role");
            var widget = null;
            if (role) {
                switch (role) {
                    // Kendo UI Widget
                    case "button":
                        widget = $element.data("kendoButton");
                        break;
                    case "dropdownlist":
                        widget = $element.data("kendoDropDownList");
                        break;
                    case "dropdowntree":
                        widget = $element.data("kendoDropDownTree");
                        break;
                    case "numerictextbox":
                        widget = $element.data("kendoNumericTextBox");
                        break;
                    case "combobox":
                        widget = $element.data("kendoComboBox");
                        break;
                    case "datepicker":
                        widget = $element.data("kendoDatePicker");
                        break;
                    case "timepicker":
                        widget = $element.data("kendoTimePicker");
                        break;
                    case "datetimepicker":
                        widget = $element.data("kendoDateTimePicker");
                        break;
                    case "daterangepicker":
                        widget = $element.data("kendoDateRangePicker");
                        break;
                    case "autocomplete":
                        widget = $element.data("kendoAutoComplete");
                        break;
                    case "multiselect":
                        widget = $element.data("kendoMultiSelect");
                        break;
                    case "treeview":
                        widget = $element.data("kendoTreeView");
                        break;
                    case "maskedtextbox":
                        widget = $element.data("kendoMaskedTextBox");
                        break;
                    case "panelbar":
                        widget = $element.data("kendoPanelBar");
                        break;
                    case "progressbar":
                        widget = $element.data("kendoProgressBar");
                        break;
                    case "slider":
                        widget = $element.data("kendoSlider");
                        break;
                    case "grid":
                        widget = $element.data("kendoGrid");
                        break;
                    case "tooltip":
                        widget = $element.data("kendoTooltip");
                        break;
                    case "upload":
                        widget = $element.data("kendoUpload");
                        break;
                    case "editor":
                        widget = $element.data("kendoEditor");
                        break;
                    case "colorpalette":
                        widget = $element.data("kendoColorPalette");
                        break;
                    case "tabstrip":
                        widget = $element.data("kendoTabStrip");
                        break;
                    case "listbox":
                        widget = $element.data("kendoListBox");
                        break;
                    case "textbox":
                        widget = $element.data("kendoTextBox");
                        break;
                    case "switch":
                        widget = $element.data("kendoMobileSwitch");
                        break;
                    case "checkbox":
                        widget = null;
                        break;
                    case "radiogroup":
                        widget = $element.data("kendoRadioGroup");
                        break;
                    case "radiobutton":
                        widget = $element.data("kendoRadioButton");
                        break;

                    // Expresso Custom
                    case "expressolanguageselector":
                        widget = $element.data("kendoExpressoLanguageSelector");
                        break;
                    case "expressotimepicker":
                        widget = $element.data("kendoExpressoTimePicker");
                        break;
                    case "expressomaskedtimepicker":
                        widget = $element.data("kendoExpressoMaskedTimePicker");
                        break;
                    case "expressomaskedtimeunitpicker":
                        widget = $element.data("kendoExpressoMaskedTimeUnitPicker");
                        break;
                    case "expressoform":
                        widget = null;
                        break;
                    case "expressopercentageselector":
                        widget = $element.data("kendoExpressoPercentageSelector");
                        break;
                    case "expressomultiinput":
                        widget = $element.data("kendoExpressoMultiInput");
                        break;
                    case "expressomulticheckbox":
                        widget = $element.data("kendoExpressoMultiCheckbox");
                        break;
                    case "expressomultilookupselection":
                        widget = $element.data("kendoExpressoMultiLookupSelection");
                        break;
                    case "expressofilter":
                        widget = $element.data("kendoExpressoFilter");
                        break;

                    // Cezinc Widget (BACKWARD compatibility only)
                    case "cezincsingleselect":
                        widget = $element.data("kendoCezincSingleSelect");
                        break;
                    case "cezincmultipleselect":
                        widget = $element.data("kendoCezincMultipleSelect");
                        break;
                    case "cezincmaskedtextbox":
                        widget = $element.data("kendoCezincMaskedTextBox");
                        break;

                    default:
                        alert("Not a supported Kendo UI role [" + role + "]");
                        console.trace("Not a supported Kendo UI role [" + role + "]");
                        break;
                }
            }
            return widget;
        };

        /**
         *
         * @param resourceURL
         * @returns {string}
         */
        var getResourceNameFromURL = function (resourceURL) {
            var resourceName = "";
            if (resourceURL.startsWith("/")) {
                resourceURL = resourceURL.substring(1);
            }
            var s = resourceURL.split("/");
            for (var i = 0; i < s.length; i += 2) {
                if (i != 0) {
                    resourceName += "/";
                }
                resourceName += s[i];
            }
            return resourceName;
        };

        /**
         * Convert (or create) a KenodUI combo box to a server side filtering combo box
         * @param $input the jQuery SELECT element to convert
         * @param resourceURL url for the resource
         * @param [customOptions]
         * @returns {*} the KendoUI ComboBox widget
         */
        var buildComboBox = function ($input, resourceURL, customOptions) {
            var $deferred = $.Deferred();
            var maximumResults = 50;

            // if the DOM element is not present, return immediately
            if (!$input || !$input.length) {
                return $deferred.reject();
            }

            // patch: if the input has .k-textbox class, remove it
            if ($input.hasClass("k-textbox")) {
                $input.removeClass("k-textbox");
            }

            var cb = $input.data("kendoComboBox");
            if (cb) {
                alert("Do not define the comboxbox if using server side filtering [" + $input.attr("name") +
                    ":" + $input.attr("class") + "]");
                return $deferred.reject();
            }

            customOptions = customOptions || {};

            // get the info from the field if defined
            if (customOptions.field && customOptions.field.reference) {
                customOptions.nullable = customOptions.nullable || customOptions.field.nullable;
                customOptions.filter = customOptions.filter || customOptions.field.reference.filter;
                customOptions.triggerChangeOnInit = customOptions.triggerChangeOnInit || customOptions.field.reference.triggerChangeOnInit;
                customOptions.cascadeFrom = customOptions.cascadeFrom || customOptions.field.reference.cascadeFrom;
                customOptions.cascadeFromField = customOptions.cascadeFromField || customOptions.field.reference.cascadeFromField;
            }

            var url;
            if (resourceURL.startsWith("/")) {
                // static URL
                url = expresso.Common.getWsResourcePathURL() + resourceURL;
            } else {
                url = expresso.Common.getWsResourcePathURL() + "/" + resourceURL + "/search";

                // make sure that the user has access to the resource
                var resourceName = getResourceNameFromURL(resourceURL);
                if (resourceName && !expresso.Common.isUserAllowed(resourceName, "read")) {
                    console.warn("Hiding server side combo box because user does not have read access to [" + resourceName + "]");
                    $input.closest("div").hide();
                    return $deferred.reject();
                }
            }

            // by default, we use autoBind.
            // but sometimes we may want to avoid it (but you will lose the onchange event on load)
            var autoBind = customOptions.autoBind !== undefined ? customOptions.autoBind :
                (customOptions.field && customOptions.field.autoBind !== undefined ? customOptions.field.autoBind : true);

            // custom label
            var label = "label";
            if (customOptions.field && customOptions.field.reference && customOptions.field.reference.label) {
                label = customOptions.field.reference.label;
                if (typeof label === "function") {
                    label = label();
                }
            }

            var autoSearch = true;
            if (customOptions.autoSearch === false ||
                (customOptions.field && customOptions.field.reference && customOptions.field.reference.autoSearch == false)) {
                autoSearch = false;
            }

            // create the combo box
            //console.log("Creating kendoComboBox for [" + resourceURL + "]");
            var initialized;
            cb = $input.kendoComboBox({
                autoBind: autoBind,
                dataValueField: "id",
                dataTextField: label,
                valuePrimitive: true,
                filter: "contains",
                //suggest: true, // auto select first option
                syncValueAndText: false,
                //highlightFirst: true,
                value: retrieveCurrentValue($input, customOptions),
                minLength: (autoSearch ? 2 : 99),  // avoid filtering for at least n characters
                enforceMinLength: false, // if true, on clear, do not show all options
                cascadeFrom: customOptions.cascadeFrom,
                cascadeFromField: customOptions.cascadeFromField,
                dataSource: {
                    serverFiltering: true,
                    transport: {
                        read: {
                            url: url,
                            data: function () {
                                var data = {};

                                // if a custom filter is defined, use it
                                if (customOptions.filter) {
                                    var filter;
                                    if (typeof customOptions.filter === "function") {
                                        filter = customOptions.filter();
                                    } else {
                                        filter = customOptions.filter;
                                    }
                                    data = expresso.Common.buildKendoFilter(filter, null, true);
                                }

                                // this is used the first time to load the default value
                                if (!initialized) {
                                    initialized = true;
                                    data.id = retrieveCurrentValue($input, customOptions);
                                }
                                return data;
                            }
                        }
                    },
                    schema: {
                        parse: function (response) {
                            //console.log("CB response", response);
                            if (response.data) {
                                response = response.data;
                            }

                            if (customOptions.dataTextField) {
                                if (typeof customOptions.dataTextField === "function") {
                                    var i;
                                    for (i = 0; i < response.length; i++) {
                                        response[i].label = customOptions.dataTextField(response[i]);
                                    }
                                } else {
                                    for (i = 0; i < response.length; i++) {
                                        response[i].label = response[i][customOptions.dataTextField];
                                    }
                                }
                            }

                            // always sort on the label
                            if (!customOptions.avoidSorting) {
                                response.sort(function (r1, r2) {
                                    return r1["label"].localeCompare(r2["label"]);
                                });
                            }

                            // verify if there is a null element at the end
                            if (response && response.length >= maximumResults) {
                                response.push({
                                    id: 0,
                                    label: expresso.Common.getLabel("tooManyResults")
                                });
                            }

                            return response;
                        }
                    }
                },
                change: onChangeEvent($input, customOptions, resourceURL)
            }).data("kendoComboBox");

            // on enter, stop the search and get the only "expected" result
            if (!autoSearch) {
                cb.input.keydown(function (e) {
                    if (e.which == 13 /*ENTER*/) {
                        e.stopPropagation();
                        e.preventDefault();

                        var text = cb.input.val();
                        //console.log("CB input Enter/Tab [" + text + "]");

                        // get the resource
                        expresso.Common.sendRequest(url, null, null, {term: text}, {
                            ignoreErrors: true,
                            waitOnElement: null
                        }).done(function (result) {
                            //console.log("GOT result");
                            if (result && result.length == 1) {
                                //console.log("GOT 1 result", result);
                                var resource = result[0];
                                addDataItemToWidget(resource, cb);
                            }
                        });
                    }
                });
                // make sure to be the first listener for ENTER
                // otherwise KendoUI will stop the event propagation
                var eventList = $._data(cb.input[0], "events");
                eventList.keydown.unshift(eventList.keydown.pop());
            }

            // on focus, select the text
            cb.input.on("focus", function () {
                $(this).select();
            });

            if (autoBind) {
                cb.dataSource.one("change", function () {
                    $deferred.resolve(cb);
                    triggerChangeEvent($input, cb, customOptions);
                });
            } else {
                $deferred.resolve(cb);
                triggerChangeEvent($input, cb, customOptions);
            }

            return $deferred;
        };

        /**
         * Build a message window with one button to close it
         * @param text
         * @param customOptions
         * @return {*} a promise when the dialog is closed
         */
        var buildMessageWindow = function (text, customOptions) {
            var $windowDiv = $("<div class='exp-alert-div'></div>").appendTo($("body"));
            $windowDiv.css("maxHeight", $(window).height() - 200);

            customOptions = $.extend({type: "info"}, customOptions);

            var icon;
            switch (customOptions.type) {
                case "question":
                    icon = "question-circle";
                    break;
                case "warning":
                    icon = "exclamation-triangle";
                    break;
                case "error":
                    icon = "exclamation-circle";
                    break;
                case "info":
                default:
                    icon = "info-circle";
                    break;
            }

            icon = "<i class='fa fa-2x fa-" + icon + " exp-message-" + customOptions.type + "' aria-hidden='true'></i>";
            var title = expresso.Common.getLabel("message_" + customOptions.type);

            var $deferred = $.Deferred();
            $windowDiv.kendoAlert({
                title: customOptions.title || title,
                content: text,
                width: customOptions.width || 300,
                messages: {
                    okText: "OK"
                },
                initOpen: function () {
                    $windowDiv.closest(".k-window").find(".k-window-title").before(icon);
                },
                close: function () {
                    $deferred.resolve();
                }
            }).data("kendoAlert").open();

            return $deferred;
        };

        /**
         * Build a prompt window with 2 buttons to save or cancel
         * @param title
         * @param text
         * @param customOptions
         * @return {*} a promise that contains the data
         */
        var buildPromptWindow = function (title, text, customOptions) {
            var $windowDiv = $("<div class='exp-prompt-div'></div>").appendTo($("body"));
            customOptions = customOptions || {};

            $windowDiv.kendoPrompt({
                title: title,
                content: text,
                value: customOptions.value || "",
                width: customOptions.width || 350,
                messages: {
                    okText: customOptions.okText || expresso.Common.getLabel("save"),
                    cancel: customOptions.cancel || expresso.Common.getLabel("cancel")
                }
            }).data("kendoPrompt").open();

            var $input = $windowDiv.closest(".k-window").find("input.k-textbox");

            // add a listener on the input field
            // if the user press ENTER, submit it
            $input.keypress(function (e) {
                if (e.which == 13) { // ENTER
                    $windowDiv.closest(".k-window").find("button.k-primary").click();
                }
            });

            // focus and select the text
            window.setTimeout(function () {
                try {
                    $input.select();
                    $input.focus();
                } catch (ex) {
                    // ignore
                }
            }, 100);

            return $windowDiv.data("kendoPrompt").result;
        };

        /**
         * Build a comment window
         * @param title
         * @param customOptions
         * @return {*} a promise that contains the data
         */
        var buildCommentWindow = function (title, customOptions) {
            customOptions = customOptions || {};

            var $deferred = $.Deferred();
            var a = [];
            a.push("<form class='exp-form exp-form-single-column'><div class='input-wrap'><textarea class='k-textbox full-length' rows='5'></textarea></div></form>");
            expresso.util.UIUtil.buildWindow(a.join(""), {
                width: "400px",
                //height: "150px",
                title: title,
                open: function () {
                    this.find("textarea").val(customOptions.comment).focus();
                },
                close: function () {
                    if ($deferred.state() == "pending") {
                        $deferred.reject();
                    }
                },
                save: function () {
                    var comment = this.find("textarea").val();

                    if (customOptions.fieldName) {
                        var r = {
                            fieldName: customOptions.fieldName
                        };
                        r[customOptions.fieldName] = comment;
                        $deferred.resolve(r);
                    } else {
                        $deferred.resolve(comment);
                    }
                }
            });

            return $deferred;
        };

        /**
         * Build a prompt window with 2 buttons (yes and no)
         * @param title
         * @param text
         * @param customOptions
         * @return {*} a promise that contains the data
         */
        var buildYesNoWindow = function (title, text, customOptions) {
            var $windowDiv = $("<div class='exp-prompt-div'></div>").appendTo($("body"));
            customOptions = customOptions || {};

            $windowDiv.kendoConfirm({
                title: title,
                content: text,
                width: customOptions.width || 350,
                messages: {
                    okText: customOptions.okText || expresso.Common.getLabel("yn_yes"),
                    cancel: customOptions.cancelText || expresso.Common.getLabel("yn_no")
                }
            }).data("kendoConfirm").open();

            return $windowDiv.data("kendoConfirm").result;
        };


        /**
         * We need to destroy the widgets otherwise there is DOM element leaks
         * @param $div
         */
        var destroyKendoWidgets = function ($div) {
            if ($div && $div.length) {
                $div.find('[data-' + kendo.ns + 'role]').each(function () {
                    var data = $(this).data();
                    for (var key in data) {
                        if (key.indexOf('kendo') === 0 && data[key].destroy) {
                            try {
                                data[key].destroy();
                            } catch (e) {
                                // ignore
                            }
                        }
                    }
                });
            }
        };

        /**
         * Get the form width depending on the FORM definition
         * @param $form
         * @param options
         */
        var getFormWidth = function ($form, options) {
            options = options || {};

            var sideMargin = 10;
            var width = options.width;
            var $browserWindow = $(window);
            var singleColumnWidth = 420;

            if (!width) {
                if ($form.hasClass("exp-form-max-width")) {
                    width = Math.min($browserWindow.width() - 20, 1500);
                } else if ($form.hasClass("exp-form-single-column")) {
                    width = singleColumnWidth;
                } else if ($form.hasClass("exp-form-three-columns")) {
                    width = singleColumnWidth * 3;
                } else if ($form.hasClass("exp-form-four-columns")) {
                    width = singleColumnWidth * 4;
                } else {
                    // default double column
                    width = singleColumnWidth * 2;
                }

                // make sure not to be larger than the window
                width = Math.min($browserWindow.width() - sideMargin, width);
            } else {
                if (typeof width === "string" && width.endsWith("px")) {
                    width = parseInt(width.substring(0, width.length - 2));
                } else if (width == "100%" || width == "max") {
                    // allow 10 pixels on each side
                    width = $browserWindow.width() - (2 * sideMargin);
                }
            }
            return width;
        };

        /**
         *
         * @param $windowContent
         * @param options
         */
        var setWindowDimension = function ($windowContent, options) {

            // avoid null issue
            options = options || {};

            var kendoWindow = $windowContent.data("kendoWindow");

            var $browserWindow = $(window);
            var $window = $windowContent.closest(".k-window");
            var $editButtonDiv = $window.find(".k-edit-buttons");
            var $titleBar = $window.find(".k-window-titlebar");
            var $container = $window.find(".k-edit-form-container");
            var $userRootDivContent;

            // if the form has tabs
            var $formWrapper = $container.children().filter(".exp-form-wrapper");
            if ($formWrapper.length) {
                $userRootDivContent = $formWrapper.children(".exp-form").first();
            } else {
                //if the form has no tabs
                $formWrapper = $container.children().filter(".exp-form");
                if ($formWrapper.length) {
                    $userRootDivContent = $formWrapper;
                } else {
                    // this is a window built by buildWindow
                    $formWrapper = $container.children().filter(".exp-window-content");
                    if ($formWrapper.length) {
                        $userRootDivContent = $formWrapper.children("div,form").first();
                    } else {
                        console.error("Unexpected window structure");
                    }
                }
            }

            //console.log("classes: " + $userRootDivContent.attr("class"));

            // set the new width
            var width = getFormWidth($userRootDivContent, options);
            var leftMargin = ($browserWindow.width() - width) / 2;

            // calculate the maximum height for the window
            // do not use the outerHeight for the titleBar (it will report 0). Add margin for it
            var titleBarMargin = 15;
            var windowMargin = 8; // allow x pixels on each top and bottom
            var maxHeight = $browserWindow.height() - $editButtonDiv.outerHeight(true) - $titleBar.height() -
                (windowMargin * 2) - titleBarMargin;

            // console.log("$browserWindow.height(): " + $browserWindow.height());
            // console.log("$editButtonDiv.outerHeight(true): " + $editButtonDiv.outerHeight(true));
            // console.log("$titleBar.height(): " + $titleBar.height());
            // console.log("maxHeight: " + maxHeight);

            // then set the max height in case the form expand
            $formWrapper.css("max-height", maxHeight);

            // reset the height in case it is already set
            //console.log("$userRootDivContent1: " + $userRootDivContent.height());
            $userRootDivContent.css('height', 'auto');
            //console.log("$userRootDivContent2: " + $userRootDivContent.height());

            // set the height
            var height = options.height;
            if (height == "100%" || height == "max" || $userRootDivContent.hasClass("exp-form-max-height")) {
                $formWrapper.height(maxHeight);
                options.top = 0;
            } else if (height) {
                $formWrapper.height(height);
            }
            //console.log("$formWrapper.height(): " + $formWrapper.height());

            var maxTopMargin = ($browserWindow.height() - $formWrapper.height()) / 2 -
                ($titleBar.height() + titleBarMargin + (windowMargin * 2)) - 15;
            //console.log("maxTopMargin: " + maxTopMargin);
            var topMargin = options.top;
            //console.log("topMargin: " + topMargin);
            if ((!topMargin && topMargin !== 0) || topMargin > maxTopMargin) {
                topMargin = maxTopMargin;
            }

            // if the class mark exp-form-top is defined, put the window at the top
            if (topMargin < 5 || $userRootDivContent.hasClass("exp-form-top")) {
                topMargin = 5;
            }
            //console.log("topMargin: " + topMargin);
            kendoWindow.setOptions({
                resizable: (options.resizable !== false),
                width: width,
                position: {
                    top: topMargin,
                    left: leftMargin
                }
            });
            //kendoWindow.center();

            var onResize = function () {
                // console.log("$browserWindow.height(): " + $browserWindow.height());
                // console.log("$window.height(): " + $window.height());
                // console.log("$windowContent.height(): " + $windowContent.height());
                // console.log("$editButtonDiv.outerHeight(true): " + $editButtonDiv.outerHeight(true));
                //console.log("$formWrapper.height(): " + $formWrapper.height());

                var h = $windowContent.height() - $editButtonDiv.outerHeight(true);
                //console.log("Setting $formWrapper.height(): " + h);

                // we need to add some pixels to avoid having a scrollbar
                if (h == $formWrapper.height()) {
                    // do nothing, otherwise you will shrink the window
                } else {
                    $formWrapper.height(h + 1);
                }
            };

            // bind on resize
            kendoWindow.unbind("resize", onResize).bind("resize", onResize);
        };

        /**
         * Build a resizable window with one button to save.
         * It will open automatically upon the call of this method.
         * It will close automatically on "save".
         * @param content could be a string with HTML content or a path to a html snippet file (relative path)
         * @param customOptions
         * @return {*} the jQuery DOM element for the window
         */
        var buildWindow = function (content, customOptions) {
            var defaultOptions = {
                title: "",
                saveButtonLabel: expresso.Common.getLabel("save"),
                buttons: [],
                width: undefined,
                destroyOnClose: true,
                autoOpen: true,
                autoFocus: true,
                resizable: true,

                // optional for resource
                labels: expresso.Labels,
                model: null,

                // by default, it will try to convert the input to ExpressoForm
                convertToExpressoForm: true,

                // events
                init: null,
                open: null,
                save: null,
                close: null,
                resize: null,
                buttonClicked: null
            };
            var options = $.extend(true, {}, defaultOptions, customOptions);

            var a = [];
            a.push("<div class='k-popup-edit-form k-window-content k-content " + (customOptions.classes ? customOptions.classes : "") + "'>");
            a.push("  <div class='k-edit-form-container'>");
            a.push("    <div class='exp-window-content'></div>");
            a.push("    <div class='k-edit-buttons k-state-default'>");

            // add all custom buttons
            if (customOptions.buttons) {
                $.each(customOptions.buttons, function (index, value) {
                    a.push(value);
                });
            }
            if (options.saveButtonLabel) {
                a.push("      <button type='button' class='k-button k-primary rm-window-save-button exp-window-save-button' >" + options.saveButtonLabel + "</button>");
            }
            a.push("    </div>");
            a.push("  </div>");
            a.push("</div>");
            var $windowDiv = $(a.join("")).appendTo($("body"));

            var windowActions = [];
            if (options.resizable) {
                windowActions.push("Maximize");
            }
            windowActions.push("Close");

            // do not call resize too often
            if (options.resize) {
                options.resizeDebonced = expresso.util.Util.debounce(function () {
                    options.resize.call($windowDiv);
                });
            }

            var initialized = false;
            var kendoWindow = $windowDiv.kendoWindow({
                title: options.title,
                modal: true,
                visible: false,
                resizable: options.resizable,
                actions: windowActions,

                // Triggered when a Window is opened (first called)
                open: function () {
                    // console.log("buildWindow - open");

                    // try to display the window at the top at first,
                    // then on activate, we will review the size and position.
                    // otherwise if we wait on activate, the window will appear at the
                    // bottom and push up the main content, and then move to the center
                    setWindowDimension($windowDiv, $.extend({}, options, {top: options.top || ($("body").height() / 2 - 250)}));
                },
                // Triggered when the content of a Window has finished loading via AJAX (called af open)
                refresh: function () {
                    //console.log("buildWindow - refresh");
                },
                // Triggered when a Window is closed (before deactivate)
                close: function () {
                    //console.log("buildWindow - close");
                },

                // Triggered when a Window has finished its opening animation. (called after open and refresh)
                activate: function () {
                    // console.log("buildWindow - activate");

                    if (!initialized) {
                        initialized = true;
                        if (options.init) {
                            options.init.call($windowDiv);
                        }

                        // set the width and height
                        setWindowDimension($windowDiv, options);
                    }

                    if (options.open) {
                        options.open.call($windowDiv);
                    }

                    // by default, select the first input and set the focus
                    try {
                        if (options.autoFocus) {
                            //console.log("autoFocus"); // + $window.find(":input:visible:not([readonly]):enabled:first").attr("name"));
                            $window.find(":input:visible:not([readonly]):enabled:first").focus();
                        }
                    } catch (e) {
                        // ignore
                    }
                },

                // Triggered when a Window has finished its closing animation. (last event)
                deactivate: function () {
                    //console.log("buildWindow - deactivate");
                    if (options.close) {
                        options.close.call($windowDiv);
                    }

                    if (options.destroyOnClose) {
                        destroyKendoWidgets(this);
                        this.destroy();
                        $windowDiv = null;
                    }
                },
                resize: function () {
                    if (options.resizeDebonced) {
                        options.resizeDebonced();
                    }
                }
            }).data("kendoWindow");

            // register a listener on the buttons
            $windowDiv.find(".k-edit-buttons button").on("click", function (e) {
                e.preventDefault();
                var saveResult;
                var $button = $(this);
                if ($button.hasClass("exp-window-save-button")) {
                    if (options.save) {
                        // if the save method returns true, close the window
                        saveResult = options.save.call($windowDiv);
                        if (saveResult === false) {
                            // do not close
                        } else if (saveResult === true || !saveResult) {
                            kendoWindow.close();
                        } else { // promise
                            //  do not close
                            saveResult.done(function () {
                                kendoWindow.close();
                            });
                        }
                    } else {
                        kendoWindow.close();
                    }
                } else {
                    if (options.buttonClicked) {
                        saveResult = options.buttonClicked.call($button, e, $windowDiv, options);
                        if (saveResult === false) {
                            // do not close
                        } else if (saveResult === true || !saveResult) {
                            kendoWindow.close();
                        } else { // promise
                            //  do not close
                            saveResult.done(function () {
                                kendoWindow.close();
                            });
                        }
                    }
                }
            });

            // to be called when window is loaded and ready to be opened
            var $deferred = $.Deferred();
            var openWindow = function () {
                // protect window against form auto submit (if present)
                $windowDiv.find("form").on("submit", function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                });

                $deferred.resolve($windowDiv);
                if (options.autoOpen) {
                    kendoWindow.open();
                }
            };

            var $contentDiv = $windowDiv.find(".k-edit-form-container .exp-window-content");
            if (!content || typeof content !== "string" || content.startsWith("<")) {
                // this is a static content
                $contentDiv.append(content);

                if (options.convertToExpressoForm) {
                    // we need to process the form
                    $contentDiv.kendoExpressoForm({
                        labels: options.labels,
                        model: options.model
                    }).data("kendoExpressoForm").ready().done(function () {
                        openWindow();
                    });
                } else {
                    openWindow();
                }
            } else {
                // load the content using AJAX
                expresso.Common.loadHTML($contentDiv, content, options.labels).done(function () {
                    // content is already processed by kendoExpressoForm
                    openWindow();
                });
            }

            return $deferred;
        };

        /**
         *
         * @param dataItem
         * @param widget
         * @returns {*} the newly added dataItem
         */
        var addDataItemToWidget = function (dataItem, widget) {
            if (widget && widget.data && widget.data("kendoComboBox")) {
                // we got the jQuery object
                widget = widget.data("kendoComboBox");
            } else if (widget && widget.data && widget.data("kendoDropDownList")) {
                // we got the jQuery object
                widget = widget.data("kendoComboBox");
            }

            // add the new dataItem in the datasource
            dataItem = widget.dataSource.add(dataItem);

            // then select it
            widget.select(widget.dataSource.view().length - 1);

            return dataItem;
        };

        /**
         * Standard definition for on change event for Editor: Combobox, DropDownList, DropDownTree
         *
         * @param $input
         * @param customOptions
         * @param [resourceURL]
         * @return {function(*=): void}
         */
        var onChangeEvent = function ($input, customOptions, resourceURL) {
            //console.log("onChangeEvent - " + $input.attr("name"));

            // find the name of the attribute
            var bindNameId = $input.attr("data-bind");
            if (bindNameId && bindNameId.startsWith("value:")) {
                bindNameId = bindNameId.substring("value:".length);
            } else {
                bindNameId = $input.attr("name");
            }
            var attName;
            var objectDefaultValue = {}; // default value for the object
            if (bindNameId && bindNameId.endsWith("Id")) {
                // get the object for the ID
                attName = bindNameId.substring(0, bindNameId.length - 2);
                if (customOptions.model && customOptions.model.fields[attName] && customOptions.model.fields[attName].defaultValue) {
                    objectDefaultValue = customOptions.model.fields[attName].defaultValue;
                }
            }

            var resource = customOptions.resource;

            return function (e) {
                var value;
                if (this.value) {
                    value = this.value();
                }
                var dataItem;

                if (this.dataItem) {
                    // Combobox, DropDownList
                    dataItem = this.dataItem();
                    // console.log("Change Combobox/DropDownList", dataItem);
                }

                if (!dataItem && e.sender && e.sender.treeview) {
                    // select event from DropDownTree
                    dataItem = e.sender.treeview.dataItem(e.sender.treeview.select());
                    // console.log("Change DropDownTree", dataItem);
                }

                //console.log(bindNameId + " - Change value[" + value + "] data item", dataItem);

                var triggerChange = function (ev) {
                    if (resource && attName) {
                        // perform only if done by a user (otherwise the object is already loaded by default)
                        if (ev && ev.sender && ev.sender._userTriggered) {
                            //console.log("Setting [" + attName + "]", dataItem);
                            // do not allow to set null: it will crash the grid (ex: equipment.equipmentNo)
                            resource.set(attName, dataItem || objectDefaultValue);
                        }
                    }

                    if (customOptions.change) {
                        //console.log("Calling application change event listener");

                        // PATCH: because KendoUI will trigger twice the same event on blur,
                        // we need to keep the previous value and do not fire the event
                        // if the value is the same
                        if (this.previousValue === undefined || this.previousValue != value) {
                            this.previousValue = value;
                            customOptions.change.call(this, ev);
                        }
                    }
                }

                if (value && !dataItem && resourceURL) {
                    var _this = this;
                    var widget = getKendoWidget($input);

                    //console.log(bindNameId + "We need to get the data item for the value[" + value + "]");
                    expresso.Common.sendRequest(resourceURL + "/" + value, null, null, null, {
                        ignoreErrors: true,
                        waitOnElement: null
                    }).done(function (result) {
                        // add the new dataItem in the datasource
                        dataItem = addDataItemToWidget(result, widget);

                        // then trigger the change
                        triggerChange.call(_this, e);
                    });
                } else {
                    triggerChange.call(this, e);
                }
            };
        };

        /**
         * Trigger the change event on init, but wait until the form is ready
         * @param $input
         * @param widget
         * @param customOptions
         */
        var triggerChangeEvent = function ($input, widget, customOptions) {
            // trigger the change
            if (customOptions.triggerChangeOnInit !== false) {
                var $form = $input.closest(".exp-form");
                if ($form.length && $form.data("formReadyPromise")) {
                    var $formReadyPromise = $form.data("formReadyPromise");
                    $formReadyPromise.done(function () {
                        //console.log($input.attr("name") + " - Trigger change");
                        widget.trigger("change", {expressoTriggered: true});
                    });
                } else {
                    widget.trigger("change", {expressoTriggered: true});
                }
            }
        };

        /**
         * This is an utility method to retrieve the current value for a ComboBox or a DropDownList
         * @param $input
         * @param customOptions
         */
        var retrieveCurrentValue = function ($input, customOptions) {
            var value;

            var bindNameId = $input.attr("data-bind");
            if (bindNameId && bindNameId.startsWith("value:")) {
                bindNameId = bindNameId.substring("value:".length);
            } else {
                bindNameId = $input.attr("name");
            }

            // 1- get from the resource
            if (customOptions.resource) {
                value = customOptions.resource[bindNameId];
                if (value && value.length !== undefined && value.join) {
                    // convert the object to Array (this is because KendoUI convert Array to Object
                    // to listen for change
                    //value = JSON.parse(JSON.stringify(value));
                    value = value.join(",");
                }
                //console.log("Getting value from resource[" + bindNameId + "]: " + value);
            }

            // 2- get from the customOptions value
            if (value === undefined) {
                value = customOptions.value;
                //console.log("Getting value from customOptions: " + value);
            }

            // 3- get from the defaultValue from the field
            if (value === undefined) {
                if (customOptions.field) {
                    value = customOptions.field.defaultValue;
                    //console.log("Getting value from field.defaultValue: " + value);
                }
            }

            // 4- get from the defaultValue from the model
            if (value === undefined) {
                if (customOptions.model && customOptions.model.fields && customOptions.model.fields[bindNameId]) {
                    value = customOptions.model.fields[bindNameId].defaultValue;
                    //console.log("Getting value from model defaultValue: " + value);
                }
            }

            // 5- get the value from the DOM Element
            if (value === undefined) {
                if ($input[0].tagName == "INPUT") {
                    value = $input.val();
                    //console.log("Getting value from input: " + value);
                } else if ($input[0].tagName == "SELECT") {
                    if ($input.attr("multiple")) {
                        var values = [];
                        $.each($input.children("option:selected"), function () {
                            values.push($(this).val());
                        });
                        value = values.join(",");
                        //console.log("Getting value from multiple select: " + value);
                    } else {
                        value = $input.children("option:selected").val();
                        //console.log("Getting value from select: " + value);
                    }
                }
            }

            return value;
        }

        /**
         * Build the option list from the rest service and initialize a KendoUI DropDownList
         *
         * @param $input    jquery object of the select element
         * @param wsListPathOrData  URL for the resource OR the data itself
         * @param [customOptions]    custom options to create the dropdownlist
         * @returns {*} a jQuery promise resolved when the list is initialized
         */
        var buildDropDownList = function ($input, wsListPathOrData, customOptions) {
            var $deferred = $.Deferred();

            if (!$input || $input.length == 0) {
                return $deferred.reject();
            }

            // patch: if the input has .k-textbox class, remove it
            if ($input.hasClass("k-textbox")) {
                $input.removeClass("k-textbox");
            }

            // deal with undefined customOptions
            customOptions = customOptions || {};
            var dataValueField = customOptions.dataValueField || "id";
            var dataTextField = customOptions.dataTextField || "label";

            // get the info from the field if defined
            if (customOptions.field && customOptions.field.values) {
                customOptions.nullable = customOptions.nullable || customOptions.field.nullable;
                customOptions.triggerChangeOnInit = customOptions.triggerChangeOnInit || customOptions.field.values.triggerChangeOnInit;
                customOptions.selectFirstOption = customOptions.selectFirstOption || customOptions.field.values.selectFirstOption;
                customOptions.grouping = customOptions.grouping || customOptions.field.values.grouping;
                customOptions.sortField = customOptions.sortField || customOptions.field.values.sortField;
            }

            // utility method to convert a list of string to a datasource
            var convertList = function (list) {
                // if the list is only string, build a complete data source
                if (list && list.length) {
                    var d = list[0];
                    if (typeof d === "string") {
                        var data = [];
                        $.each(list, function () {
                            var i = {};
                            i[dataValueField] = this;
                            i[dataTextField] = this;
                            data.push(i);
                        });
                        list = data;
                    }
                }
                return list;
            };

            var dataSource;
            if (typeof wsListPathOrData === "string") {
                // static URL
                var url = expresso.Common.getWsResourcePathURL() +
                    (wsListPathOrData.startsWith("/") ? "" : "/") +
                    wsListPathOrData;

                dataSource = {
                    transport: {
                        read: {
                            url: url,
                            data: function () {
                                var data = {};

                                // if a custom filter is defined, use it
                                if (customOptions.filter) {
                                    var filter;
                                    if (typeof customOptions.filter === "function") {
                                        filter = customOptions.filter();
                                    } else {
                                        filter = customOptions.filter;
                                    }
                                    data = expresso.Common.buildKendoFilter(filter, null, true);
                                }

                                // this is used the first time to load the default value
                                data.id = retrieveCurrentValue($input, customOptions);

                                return data;
                            }
                        }
                    },
                    schema: {
                        parse: function (response) {
                            if (response.data) {
                                response = response.data;
                            }

                            // if the list is only string, build a complete data source
                            response = convertList(response);

                            if (customOptions.sortField) {
                                // always sort on the label
                                response.sort(function (r1, r2) {
                                    return r1[customOptions.sortField].localeCompare(r2[customOptions.sortField]);
                                });
                            }
                            return response;
                        }
                    },
                    serverSorting: true,
                    group: (customOptions.grouping ? {
                        field: (typeof customOptions.grouping === "string" ?
                            customOptions.grouping : customOptions.grouping.field)
                    } : undefined)
                };
            } else {
                // if the list is only string, build a complete data source
                wsListPathOrData = convertList(wsListPathOrData);

                dataSource = {
                    data: wsListPathOrData,
                    group: (customOptions.grouping ? {
                        field: (typeof customOptions.grouping === "string" ?
                            customOptions.grouping : customOptions.grouping.field)
                    } : undefined)
                };
            }

            var defaultOptions = {
                dataValueField: dataValueField,
                dataTextField: dataTextField,
                valuePrimitive: true,
                dataSource: dataSource,
                enable: customOptions.enable,
                value: retrieveCurrentValue($input, customOptions),
                height: 400,
                filter: (customOptions.grouping && customOptions.grouping.filter !== false ? "contains" : customOptions.inplaceFilter),
                change: onChangeEvent($input, customOptions),
                dataBound: function (/*e*/) {
                    if (customOptions.selectFirstOption === true && !$input.val()) {
                        //console.log("Selecting first option");
                        $input.data("kendoDropDownList").select(0);
                    }
                }
            };

            // if the option label is null, add a empty one
            if (customOptions.optionLabel === null || typeof customOptions.optionLabel === "string" ||
                (customOptions.field && customOptions.field.nullable)) {
                var text = customOptions.optionLabel;
                if (text) {
                    text = expresso.Common.getLabel(text, null, null, true) || text;
                } else {
                    text = expresso.Common.getLabel("noSelection")
                }
                defaultOptions.optionLabel = {};
                defaultOptions.optionLabel[dataValueField] = null;
                defaultOptions.optionLabel[dataTextField] = text;
            }

            //console.log("OPTIONS: " + JSON.stringify(options));
            var kendoDropDownList = $input.kendoDropDownList(defaultOptions).data("kendoDropDownList");

            if (typeof wsListPathOrData === "string") {
                kendoDropDownList.dataSource.one("change", function () {
                    $deferred.resolve(kendoDropDownList);
                    triggerChangeEvent($input, kendoDropDownList, customOptions);
                });
            } else {
                $deferred.resolve(kendoDropDownList);
                triggerChangeEvent($input, kendoDropDownList, customOptions);
            }

            return $deferred;
        };

        /**
         *
         * @param $input
         * @param wsListPathOrData
         * @param customOptions
         * @returns {*}
         */
        var buildDropDownTree = function ($input, wsListPathOrData, customOptions) {
            var $deferred = $.Deferred();
            customOptions = customOptions || {parentId: "parentId"};

            // if the DOM element is not present, return immediately
            if (!$input || !$input.length) {
                return $deferred.reject();
            }

            // make sure it is an input
            if ($input[0].nodeName != "INPUT") {
                console.warn("You should use <input> for a kendoDropDownTree for [" + $input[0].name + "]. Otherwise $input.val() will return null");
            }

            //console.log("buildDropDownTree: " + wsListPathOrData, customOptions);

            var $dataDeferred;
            if (typeof wsListPathOrData === "string") {
                var filter = null;
                if (customOptions.filter) {
                    if (typeof customOptions.filter === "function") {
                        filter = customOptions.filter();
                    } else {
                        filter = customOptions.filter;
                    }

                    filter = expresso.Common.buildKendoFilter(filter);
                }

                $dataDeferred = expresso.Common.sendRequest(wsListPathOrData, null, null, filter).then(function (result) {
                    return result.data;
                });
            } else {
                $dataDeferred = $.Deferred();
                $dataDeferred.resolve(wsListPathOrData);
            }

            var defaultOptions = {
                dataValueField: customOptions.dataValueField || "id",
                dataTextField: customOptions.dataTextField || "label",
                valuePrimitive: true,
                filter: "contains",
                height: 500,
                //checkboxes: true,
                // tagMode: "single",
                dataSource: [],

                change: function () {
                    // PATCH: Kendo does not trigger the change event
                    $input.trigger("change");
                },
                treeview: {
                    // avoid node selection
                    select: function (e) {
                        if (customOptions.allowNodeSelection) {
                            var dataItem = e.sender.dataItem(e.node);
                            if (dataItem) {
                                var allowNodeSelection;
                                if (typeof customOptions.allowNodeSelection === "function") {
                                    allowNodeSelection = customOptions.allowNodeSelection(dataItem);
                                } else {
                                    allowNodeSelection = dataItem[customOptions.allowNodeSelection];
                                }
                                if (allowNodeSelection === false) {
                                    e.preventDefault();

                                    // display a visual effect to indicate that this node cannot be selected
                                    $(e.node).find(">div,>div>span").css("cursor", "not-allowed");
                                }
                            }
                        }
                    }
                }
            };

            // create the widget
            var kendoDropDownTree = $input.kendoDropDownTree(defaultOptions).data("kendoDropDownTree");

            // build the tree
            $dataDeferred.done(function (data) {
                // build the tree from the result

                var parentRootId = customOptions.parentRootId;
                if (typeof parentRootId === "function") {
                    parentRootId = parentRootId();
                }
                var dataSource = expresso.util.Util.makeTreeFromFlatList(data, {
                    parentIdFieldName: customOptions.parentId,
                    parentRootId: parentRootId,
                    expanded: true
                });

                //console.log("dataSource: " + JSON.stringify(dataSource));
                kendoDropDownTree.setDataSource(new kendo.data.HierarchicalDataSource({data: dataSource}));

                // we lost the value because the initial datasource was empty, we need to put it back
                kendoDropDownTree.value(retrieveCurrentValue($input, customOptions));

                // add the change event listener
                kendoDropDownTree.bind("change", onChangeEvent($input, customOptions));

                $deferred.resolve(kendoDropDownTree);
                triggerChangeEvent($input, kendoDropDownTree, customOptions);
            });

            return $deferred;
        };

        /**
         *
         * @param $select
         * @param wsListPathOrData
         * @param [customOptions]
         * @returns {*}
         */
        var buildLookupSelection = function ($select, wsListPathOrData, customOptions) {
            var $deferred = $.Deferred();

            // deal with undefined customOptions
            customOptions = customOptions || {};

            //var $parentDiv = $select.closest("div");

            // TARGET SELECT
            var $targetSelect = $select;
            var selectedGUID = expresso.util.Util.guid();
            $targetSelect.addClass("exp-lookup-selection").addClass("exp-lookup-selection-target").attr("id", selectedGUID).attr("multiple", "multiple");

            // SOURCE SELECT
            var $sourceSelect = $("<select class='exp-lookup-selection exp-lookup-selection-source'></select>");
            $sourceSelect.insertBefore($targetSelect);

            // INPUT SEARCH
            var data;
            if (typeof wsListPathOrData === "string") {
                data = [];

                // create a search field
                var $searchInput = $("<input type='search' class='exp-lookup-selection-input k-textbox' placeholder='" +
                    expresso.Common.getLabel("searchPlaceHolder") + "'>");
                $searchInput.insertBefore($sourceSelect);

                var searchUrl = expresso.Common.getWsResourcePathURL() + (wsListPathOrData.startsWith("/") ? "" : "/") + wsListPathOrData;
                //console.log("*****: " + searchUrl);

                //when we release 'enter' key
                $searchInput.on("keyup", function (e) {
                    e.preventDefault();
                    if (e.keyCode == 13) {
                        var term = $(this).val();
                        var param = {query: JSON.stringify({pageSize: 10000}), term: term};
                        expresso.Common.sendRequest(searchUrl, null, null, param).done(function (result) {
                            //console.log("result", result);

                            $sourceSelect.data("kendoListBox").setDataSource(new kendo.data.DataSource({
                                data: result
                            }));
                        });
                    }
                });
            } else {
                data = wsListPathOrData;
            }
            var dataValueField = customOptions.dataValueField || "id";
            var dataTextField = customOptions.dataTextField || "label";

            // create a source list box
            var defaultOptions = {
                dataValueField: dataValueField,
                dataTextField: dataTextField,
                valuePrimitive: true,
                selectable: "multiple",
                draggable: true,
                connectWith: selectedGUID,
                dropSources: [selectedGUID],
                dataSource: {
                    data: data
                },
                toolbar: {
                    tools: ["transferTo", "transferFrom", "transferAllTo", "transferAllFrom"]
                }
            };

            // create the widget
            var sourceListBox = $sourceSelect.kendoListBox(defaultOptions).data("kendoListBox");

            // create target list box
            var targetData = [];
            //console.log("customOptions.field", customOptions.field);
            if (customOptions.field && customOptions.field.defaultValue) {
                targetData.push({id: customOptions.field.defaultValue, label: customOptions.field.defaultValue});
            }

            var defaultOptions2 = {
                dataValueField: dataValueField,
                dataTextField: dataTextField,
                valuePrimitive: true,
                selectable: "multiple",
                dataSource: {
                    data: targetData
                },
                add: function () {
                    if (customOptions.change) {
                        // allow the widget to apply the changes
                        window.setTimeout(customOptions.change, 10);
                    }
                },
                remove: function () {
                    if (customOptions.change) {
                        // allow the widget to apply the changes
                        window.setTimeout(customOptions.change, 10);
                    }
                },
                toolbar: customOptions.sortable ? {
                    position: "right",
                    tools: ["moveUp", "moveDown"]
                } : undefined
            };
            var targetListBox = $targetSelect.kendoListBox(defaultOptions2).data("kendoListBox");

            // because we use local data source in configuration, we cannot wait for the datasource change event
            $deferred.resolve({sourceListBox: sourceListBox, targetListBox: targetListBox});

            return $deferred;
        };

        /**
         *
         * @param $select
         * @param wsListPathOrData
         * @param [customOptions]
         * @returns {*}
         */
        var buildMultiSelect = function ($select, wsListPathOrData, customOptions) {
            var $deferred = $.Deferred();

            // deal with undefined customOptions
            customOptions = customOptions || {};

            var dataSource;
            var serverFiltering = (customOptions.serverFiltering !== false);

            var dataValueField = customOptions.dataValueField || "id";
            var dataTextField = customOptions.dataTextField || "label";

            if (typeof wsListPathOrData === "string") {
                var url;
                if (wsListPathOrData.startsWith("/")) {
                    url = expresso.Common.getWsResourcePathURL() + wsListPathOrData;
                    serverFiltering = false;
                } else {
                    if (serverFiltering) {
                        url = expresso.Common.getWsResourcePathURL() + "/" + wsListPathOrData + "/search";
                    } else {
                        url = expresso.Common.getWsResourcePathURL() + "/" + wsListPathOrData;
                    }
                }

                var initialized = false;
                dataSource = {
                    serverFiltering: serverFiltering,
                    transport: {
                        read: {
                            url: url,
                            data: function () {
                                var data = {};
                                var filter = {filters: [], logic: "and"};
                                var f;

                                // if a custom filter is defined, use it
                                if (customOptions.filter) {
                                    if (typeof customOptions.filter === "function") {
                                        f = customOptions.filter();
                                    } else {
                                        f = customOptions.filter;
                                    }

                                    filter = expresso.Common.addKendoFilter(filter, f);
                                }

                                // if the resource is not in the first search, it will not appear
                                // this is used the first time to load the default values
                                if (!initialized) {
                                    initialized = true;
                                    data.id = retrieveCurrentValue($select, customOptions);
                                }
                                if (filter.filters.length) {
                                    data.query = JSON.stringify({filter: filter});
                                }
                                return data;
                            }
                        }
                    },
                    schema: {
                        parse: function (response) {
                            if (response.data) {
                                response = response.data;
                            }

                            // always sort on the label
                            response.sort(function (r1, r2) {
                                if (r1[dataTextField]) {
                                    return r1[dataTextField].localeCompare(r2[dataTextField]);
                                } else {
                                    return -1;
                                }
                            });

                            return response;
                        }
                    }
                };
            } else {
                dataSource = wsListPathOrData;
                serverFiltering = false;
            }

            var defaultOptions = {
                dataValueField: dataValueField,
                dataTextField: dataTextField,
                valuePrimitive: true,
                //placeholder: "",
                filter: "contains",
                autoClose: false,
                // minLength: 2,
                // enforceMinLength: false,
                dataSource: dataSource,
                value: retrieveCurrentValue($select, customOptions),

                select: function (/*e*/) {
                    // var dataItem = this.dataItem(e.item);
                    // console.log("MultiSelect - select: " + (dataItem ? dataItem.id : "NULL"));
                },
                change: function (e) {
                    // var dataItem = this.dataItem(e.item);
                    // console.log("MultiSelect - change: " + (dataItem ? dataItem.id : "NULL"));

                    // clear the input
                    e.sender.input.val("");

                    if (customOptions.change) {
                        customOptions.change.call(this, e);
                    }
                },
                dataBound: function (/*e*/) {
                }
            };

            var kendoMultiSelect = $select.kendoMultiSelect(defaultOptions).data("kendoMultiSelect");

            if (serverFiltering) {
                kendoMultiSelect.dataSource.one("change", function () {
                    $deferred.resolve(kendoMultiSelect);
                });
            } else {
                $deferred.resolve(kendoMultiSelect);
            }

            return $deferred;
        };

        /**
         *
         * @param $div DOM element to build the tree view
         * @param wsListPathOrData  URL for the resource OR the data itself
         * @param [customOptions]  custom options for the tree view
         * @returns {*}
         */
        var buildTreeView = function ($div, wsListPathOrData, customOptions) {
            var $deferred = $.Deferred();

            customOptions = customOptions || {};

            var $dataDeferred;
            if (typeof wsListPathOrData === "string") {
                // get the data from the server
                $dataDeferred = expresso.Common.sendRequest(wsListPathOrData).then(function (result) {
                    return result.data;
                });
            } else {
                // we got the data in parameter
                $dataDeferred = $.Deferred().resolve(wsListPathOrData);
            }

            // when the data are ready, process them
            $.when($dataDeferred).done(function (data) {
                // convert data structure
                data = expresso.util.Util.makeTreeFromFlatList(data, $.extend({}, {
                    parentIdFieldName: "parentId",
                    expanded: true
                }, customOptions));

                // build the datasource
                var dataSource = new kendo.data.HierarchicalDataSource({
                    data: data,
                    sort: {
                        field: customOptions.sortField || "label",
                        dir: "asc"
                    }
                });

                var defaultOptions = {
                    dataValueField: customOptions.dataValueField || "id",
                    dataTextField: customOptions.dataTextField || "label",
                    checkboxes: {
                        checkChildren: true
                    },
                    dataSource: dataSource,

                    check: function (/*e*/) {
                        // var dataItem = this.dataItem(e.item);
                        // console.log("MultiSelect - select: " + (dataItem ? dataItem.id : "NULL"));
                    },
                    change: function (/*e*/) {
                        // var dataItem = this.dataItem(e.item);
                        // console.log("MultiSelect - change: " + (dataItem ? dataItem.id : "NULL"));
                    },
                    dataBound: function (/*e*/) {

                    }
                };

                var options = $.extend(true, {}, defaultOptions, customOptions);
                var kendoTreeView = $div.kendoTreeView(options).data("kendoTreeView");

                $deferred.resolve(kendoTreeView);
            });
            return $deferred;
        };

        /**
         * Build a Kendo UI checkbox
         * @param name the name of the input
         * @param label the label description
         * @param value the value of the input
         * @param [checked] true if checked
         * @returns {*}
         */
        var buildCheckBox = function (name, label, value, checked) {
            var uniqueID = expresso.util.Util.guid();
            return $("<div class='checkbox-div input-wrap'><input id='" + uniqueID + "' type='checkbox'" +
                (name ? " name='" + name + "'" : "") + " value='" + value + "' class='k-checkbox' " +
                (checked ? "checked" : "") + ">" +
                "<label for='" + uniqueID + "' class='k-checkbox-label'>" + label + "</label></div>");
        };

        /**
         * Build a Kendo UI radio
         * @param name the name of the input
         * @param label the label description
         * @param value the value of the input
         * @param [checked] true if checked
         * @returns {*}
         */
        var buildRadioButton = function (name, label, value, checked) {
            var uniqueID = expresso.util.Util.guid();
            return $("<div class='radio-div input-wrap'><input id='" + uniqueID + "' type='radio' name='" + name + "' value='" + value + "' class='k-radio' " +
                (checked ? "checked" : "") + ">" +
                "<label for='" + uniqueID + "' class='k-radio-label'>" + label + "</label></div>");
        };

        /**
         * Add a search button on a combo box
         * @param $input
         * @param reference
         */
        var addSearchButton = function ($input, reference) {
            // console.log("addSearchButton to " + $input.attr("name"));

            // add a search button beside the combo box
            var $parent = $input.closest(".input-wrap");
            $parent.addClass("exp-ref-with-buttons");

            var widget = getKendoWidget($input);

            // SEARCH BUTTON
            var $searchButton = $("<button class='exp-ref-button exp-ref-search-button'><i class='fa fa-search'></i></button>");
            $input.closest(".k-widget").after($searchButton);

            if (reference.allowView === false || reference.resourceName == "user" || reference.resourceName == "person") {
                // only 1 button
                $parent.addClass("exp-ref-no-view");
            } else {

                // add view or add buttons
                var $viewButton = $("<button class='exp-ref-button exp-ref-view-button'><i class='fa fa-eye'></i></button>");
                $input.closest(".k-widget").after($viewButton);

                $viewButton.kendoButton({
                    click: function () {
                        var id = widget.value();

                        if (id || reference.allowCreate === true) {
                            expresso.Common.loadApplication(reference.resourceManagerDef, {autoEdit: true}).done(function (resourceManager) {
                                resourceManager.displayForm(id ? {id: id} : null, function ($window, resource) {
                                    // the form is now opened
                                    if (reference.onEdit) {
                                        reference.onEdit.call(widget, $window, resource);
                                    }
                                }).done(function (resource) {
                                    if (resource) {
                                        // get a simple object
                                        resource = JSON.parse(JSON.stringify(resource));
                                    }

                                    if (!id && resource && resource.id) {
                                        // update the combobox with the new resource
                                        addDataItemToWidget(resource, widget);
                                        widget.trigger("change", {expressoUserTriggered: true});
                                    } else {
                                        var dataItem;
                                        if (widget.dataItem) {
                                            dataItem = widget.dataItem();
                                        } else if (widget.treeview) {
                                            dataItem = widget.treeview.dataItem(widget.treeview.select());
                                        }
                                        //console.log("dataItem", dataItem);

                                        if (dataItem && resource && widget.refresh) {
                                            $.extend(true, dataItem, resource);
                                            // refresh the dataItem
                                            widget.refresh();
                                            widget.trigger("change", {expressoUserTriggered: true});
                                        }
                                    }
                                });
                            });
                        }
                    }
                });

                widget.bind("change", function () {
                    var id = this.value();
                    var $icon = $viewButton.find("i.fa");

                    $viewButton.prop("disabled", false);
                    $icon.removeClass("fa-eye")
                        .removeClass("fa-pencil")
                        .removeClass("fa-plus")
                        .addClass("fa-eye");

                    // verify if the user can create the resource
                    if (id && id !== "0") {
                        // console.log("ID[" + id + "]", id);
                        if (expresso.Common.isUserAllowed(expresso.Common.getResourceSecurityPathFromPath(reference.resourcePath), "update")) {
                            expresso.Common.sendRequest(reference.resourcePath + "/verifyActionsRestrictions", null, null, {
                                id: id,
                                actions: "update"
                            }).done(function (result) {
                                if (result && result["update"]) {
                                    // keep the eye: pencil may be confusing
                                    //$icon.removeClass("fa-eye").addClass("fa-pencil");
                                }
                            });
                        }
                    } else {
                        if (reference.allowCreate === true && expresso.Common.isUserAllowed(expresso.Common.getResourceSecurityPathFromPath(reference.resourcePath), "create")) {
                            expresso.Common.sendRequest(reference.resourcePath + "/verifyCreationRestrictions").done(function (result) {
                                if (result && result.allowed) {
                                    $icon.removeClass("fa-eye").addClass("fa-plus");
                                }
                            });
                        } else {
                            $viewButton.prop("disabled", true);
                        }
                    }
                });
            }


            $searchButton.kendoButton({
                click: function () {
                    expresso.util.UIUtil.buildSearchWindow(reference.resourceManagerDef, reference.filter, reference.sort).done(function (dataItem) {
                        // add the dataItem and trigger the change
                        addDataItemToWidget(dataItem, widget);
                        widget.trigger("change", {expressoUserTriggered: true});
                    });
                }
            });
        };

        /**
         * Build a search window, display the grid inside and on save, return the resource
         * @param resourceManagerDef
         * @param [filter]
         * @param [sort]
         * @param [multipleSelectionEnabled] false by default
         * @returns {*}
         */
        var buildSearchWindow = function (resourceManagerDef, filter, sort, multipleSelectionEnabled) {
            //var _this = this;
            var $deferred = $.Deferred();

            // build a window and put it full screen
            var resourceManager;
            expresso.util.UIUtil.buildWindow("<div class='search-div'></div>", {
                top: 5,
                width: "max",
                height: "max",
                title: expresso.Common.getLabel("search"),
                saveButtonLabel: expresso.Common.getLabel("select"),
                autoFocus: false,
                open: function () {
                    var $windowDiv = $(this);

                    // load the manager
                    expresso.Common.loadApplication(resourceManagerDef, {
                        multipleSelectionEnabled: multipleSelectionEnabled === true,
                        filter: filter,
                        sort: sort
                    }).done(function (rm) {
                        resourceManager = rm;
                        resourceManager.displayAsMaster = true;
                        resourceManager.render().done(function () {

                            var grid = resourceManager.sections.grid;

                            // load the resources
                            grid.loadResources();

                            var $gridDiv = grid.$domElement.parent();
                            var $gridDivParent = $gridDiv.parent();

                            // bring to the window the $gridDiv
                            $windowDiv.find(".search-div").append($gridDiv);
                            $gridDiv.height($windowDiv.find(".exp-window-content").css("max-height"));
                            $gridDiv.width("100%");

                            // save the original location in the $windowDiv data
                            $windowDiv.data("gridDiv", $gridDiv);
                            $windowDiv.data("gridDivParent", $gridDivParent);
                        });
                    });
                },
                save: function () {
                    // get the first selected
                    var selectedRows = resourceManager.sections.grid.selectedRows;
                    if (selectedRows && selectedRows.length >= 1) {
                        if (multipleSelectionEnabled) {
                            var dataItems = [];
                            $.each(selectedRows, function () {
                                dataItems.push(JSON.parse(JSON.stringify(this)));
                            });
                            $deferred.resolve(dataItems);
                        } else {
                            var dataItem = selectedRows[0];

                            // return a clean data Item
                            dataItem = JSON.parse(JSON.stringify(dataItem));

                            $deferred.resolve(dataItem);
                        }
                    }
                },
                close: function () {
                    var $windowDiv = $(this);

                    // destroy manager for reference
                    if (resourceManager) {
                        resourceManager.destroy();
                        resourceManager = null;
                    }

                    // put back the $gridDiv
                    var $gridDiv = $windowDiv.data("gridDiv");
                    var $gridDivParent = $windowDiv.data("gridDivParent");
                    $gridDivParent.append($gridDiv);

                    // the user has closed the window, but he did not save it, reject it
                    if ($deferred.state() !== "resolved") {
                        $deferred.reject();
                    }
                }
            });
            return $deferred;
        };

        /**
         * Reset a form (clear all inputs)
         * @param $form jQuery object for the form DOM element
         */
        var resetForm = function ($form) {
            // first reset the form
            $form.find(":input:not(button)").each(function () {
                var $el = $(this);
                if ($el.is("[type=checkbox]") || $el.is("[type=radio]")) {
                    $el.prop("checked", false);
                } else {
                    if ($el.attr("value") !== undefined) {
                        //console.log($el[0].name + ":" + $el.attr("value"));
                        $el.setval($el.attr("value"));
                    } else {
                        $el.setval(null);
                    }
                }
            });
        };

        /**
         * Initialize a form based on the properties object
         * @param $form jQuery object for the form DOM element
         * @param properties object containing the properties
         */
        var initializeForm = function ($form, properties) {
            // first reset the form
            resetForm($form);

            // initialize all filters with the default value
            for (var prop in properties) {
                if (properties.hasOwnProperty(prop) && $.type(properties[prop]) !== "function" && $.type(properties[prop]) !== "object") {
                    //console.log(prop + "=" + properties[prop]);

                    var $el = $form.find("[name=" + prop + "]");

                    if ($el.is("[type=checkbox]") || $el.is("[type=radio]")) {
                        // the property must be an array
                        if (properties[prop]) {
                            if (properties[prop] === true) {
                                $el.prop("checked", true);
                            } else {
                                var a;
                                if (typeof properties[prop] === "string") {
                                    a = properties[prop].split(",");
                                } else {
                                    a = properties[prop];
                                }
                                $.each(a, function (index, value) {
                                    $el.filter("[value=" + value + "]").prop("checked", true);
                                });
                            }
                        }
                    } else {
                        $el.setval(properties[prop]);
                    }
                }
            }
        };

        /**
         * Make the field readonly if needed
         *
         * @param $field jQuery object for the DOM element
         * @param [readonly] default is true
         * @param [setReadOnlyOnInputWrap] default is true. Set it to false if you do not want to set readonly on input-wrap
         */
        var setFieldReadOnly = function ($field, readonly, setReadOnlyOnInputWrap) {

            // if not specified, default is true
            if (readonly === undefined) {
                readonly = true;
            }

            $field.each(function () {
                var $el = $(this);
                //console.log("setFieldReadOnly [" + $field[0].nodeName + "] [" + $field.attr("name") + "]: " + readonly);

                var widget = getKendoWidget($el);
                if (widget) {
                    if (widget.readonly) {
                        widget.readonly(readonly);
                    }
                    if (widget.wrapper) {
                        if (readonly) {
                            widget.wrapper.addClass("readonly");
                        } else {
                            widget.wrapper.removeClass("readonly");
                        }
                    }
                } else if ($el.is(":checkbox") || $el.is(":radio") || $el.is("select") || $el.is("button")) {
                    $el.prop("disabled", readonly);
                } else {
                    //$el.prop("readonly", readonly);
                    if (readonly) {
                        $el.attr("readonly", "readonly");
                    } else {
                        $el.removeAttr("readonly");
                    }
                }

                if (setReadOnlyOnInputWrap !== false) {
                    var $inputWrap = $el.closest(".input-wrap");
                    if ($inputWrap.length) {
                        if (readonly) {
                            $inputWrap.addClass("readonly");
                        } else {
                            $inputWrap.removeClass("readonly");
                        }

                        // if there is an action button beside the input (exemple a lookup)
                        if ($inputWrap.hasClass("exp-ref-with-buttons")) {
                            $inputWrap.find("button.exp-ref-search-button").prop("disabled", readonly);
                        }
                    }
                }
            });
        };

        /**
         * Make the form readonly if needed
         *
         * @param $form jQuery object for the form DOM element
         * @param [readonly] default is true
         */
        var setFormReadOnly = function ($form, readonly) {
            $form.find(":input,button").filter(":not([type=hidden])").each(function () {
                //if (this.name) {
                setFieldReadOnly($(this), readonly);
                //}
            });
        };

        /**
         * Hide a field in the form (hide the wrapper)
         * @param $field jQuery object for the DOM element
         * @param $field
         * @param [hide] default is true
         * @param [supportInputWrap] default is true
         */
        var hideField = function ($field, hide, supportInputWrap) {
            hide = (hide !== false);
            supportInputWrap = (supportInputWrap !== false);
            $field.each(function () {
                var el = this;
                var $el = $(this);
                if (el.nodeName == "DIV" || el.nodeName == "FIELDSET") {
                    hide ? $el.hide() : $el.show();
                } else if (supportInputWrap && $el.closest(".input-wrap").length) {
                    hide ? $el.closest(".input-wrap").hide() : $el.closest(".input-wrap").show();
                } else if ($el.attr("data-role")) {
                    hide ? $el.closest(".k-widget").hide() : $el.closest(".k-widget").show();
                } else {
                    hide ? $el.hide() : $el.show();
                }
            });
        };

        /**
         *
         * @param $input
         * @param label
         */
        var updateLabel = function ($input, label) {
            $input.closest(".input-wrap").find("label").text(label);
        };

        // @Deprecated
        var highlightMissingRequiredField = function ($window, fieldName, clazz, removeHighlight) {
            highlightField($window, fieldName, clazz, removeHighlight);
        };

        /**
         *
         * @param $form
         * @param fieldName
         * @param [clazz]
         * @param [removeHighlight] Removes the highlight when true
         */
        var highlightField = function ($form, fieldName, clazz, removeHighlight) {
            //Check if KendoUI widget because it creates an extra span
            var $f = $form.find("[name='" + fieldName + "']");
            if ($f.is("[data-role]")) {
                $f = $f.closest('.k-widget');
            }

            // if is is a checkbox, highlight the label instead
            if ($f.attr("type") == "checkbox") {
                $f = $f.siblings("label");
            }

            if (removeHighlight) {
                $f.removeClass(clazz || "exp-invalid");
            } else {
                $f.addClass(clazz || "exp-invalid");
            }
        };

        /**
         *
         * @param $window
         */
        var displayMissingRequiredFieldNotification = function ($window) {
            // display a tooltip to fix error
            var $div = $window.find(".k-edit-buttons").find(".notif-div");
            if ($div.length == 0) {
                $div = $("<div class='notif-div'></div>").appendTo($window.find(".k-edit-buttons"));
                $div.kendoNotification({
                    autoHideAfter: 3000,
                    hideOnClick: true,
                    width: 300,
                    height: 30,
                    templates: [{
                        type: "error",
                        template: "<div class='center'>#= message #</div>"
                    }],
                    position: {
                        pinned: true,
                        top: null,
                        left: null,
                        bottom: 20,
                        right: 20
                    }
                });
            }

            $div.data("kendoNotification").error({
                message: expresso.Common.getLabel("missingFields")
            });
        };

        /**
         *
         * @param reports
         * @param $reportSelector
         * @param labels
         * @param options
         * @param executeReportCallback
         */
        var buildReportSelector = function (reports, $reportSelector, labels, options, executeReportCallback) {
            if ($reportSelector.length && reports != null) {
                // from the report definitions, build the menu buttons
                var menuButtons = [];
                var classReportKey = "reportkey-";
                $.each(reports, function (index, report) {
                    var icon;
                    switch (report.type) {
                        case "single":
                            icon = "check";
                            break;
                        case "multiple":
                            icon = "list";
                            break;
                        case "custom":
                        case "noparam":
                        default:
                            icon = "cog";
                            break;
                    }
                    menuButtons.push({
                        text: expresso.Common.getLabel("report-" + report.name, labels, null, true) ||
                            report.label || expresso.Common.getLabel(report.name, labels, null, true) || report.name,
                        spriteCssClass: "fa fa-" + icon + " " + (classReportKey + report.name)
                    });
                });

                // build the report selector
                $reportSelector.kendoToolBar({
                    resizable: false,
                    items: [{
                        type: "splitButton",
                        text: expresso.Common.getLabel("reports", labels),
                        menuButtons: menuButtons,
                        overflow: "never",
                        click: function (e) {
                            // get the class on the span child to determine the report
                            var $span = $(e.target).children("span");
                            var classes = $span.attr("class");
                            if (classes && classes.indexOf(classReportKey) != -1) {
                                var reportKey = classes.substring(classes.indexOf(classReportKey) + classReportKey.length);
                                if (reportKey.indexOf(" ") != -1) {
                                    reportKey = reportKey.substring(0, reportKey.indexOf(" "));
                                }
                                // find the report definition
                                var report = $.grep(reports, function (r) {
                                    return r.name == reportKey
                                })[0];
                                executeReportCallback(report);
                            }
                        }
                    }]
                });
            }
        };

        /**
         *
         * @param text
         * @param [options]
         */
        var showNotification = function (text, options) {
            options = $.extend({}, {
                autoHideAfter: 4000
            }, options);

            var duration = options.autoHideAfter;
            var $popupNotification = $("<span class='popup-notification'></span>)").appendTo($("body"));
            var popupNotification = $popupNotification.kendoNotification(options).data("kendoNotification");
            popupNotification.show(text);

            // after, we need to remove the HTML element
            setTimeout(function () {
                popupNotification.destroy();
                popupNotification = null;
                $popupNotification.remove();
                $popupNotification = null;
            }, duration + 1000);
        };

        /**
         * DO NOT USE  kendo.ui.progress<br>
         *  kendo.ui.progress is used by the Grid by default. If you use it, it will be removed as soon as the grid
         *  will refresh
         *
         * @param $element
         * @param [show]
         * @param [customOptions]
         */
        var showLoadingMask = function ($element, show, customOptions) {
            customOptions = customOptions || {};
            if ($element && $element.length == 1) {
                if (show || show === undefined) {

                    // remove any other loading mask
                    var $loadingMasks = $element.find(".exp-loading-mask");
                    if ($loadingMasks.length) {
                        $loadingMasks.each(function () {
                            showLoadingMask($(this).parent(), false);
                        });
                    }

                    if (!$element.find(".exp-loading-mask").length || customOptions.id) {
                        return $("<div class='k-loading-mask exp-loading-mask'" +
                            (customOptions.id ? " data-mask-id='" + customOptions.id + "'" : "") + ">" +
                            "<div class='k-loading-image'></div><div class='k-loading-color'></div>" +
                            (customOptions.text ? "<span class='exp-loading-text'>" + customOptions.text + "</span>" : "") +
                            "</div>").appendTo($element);
                    }
                } else {
                    $element.children(".exp-loading-mask").each(function () {
                        var $this = $(this);
                        var maskId = $this.data("maskId");
                        if (!maskId || maskId == customOptions.id) {
                            $this.remove();
                        }
                    });
                }
            }
        };

        /**
         * Bind the widget on the "change" event or bind the $input if not a widget
         * @param $input
         * @param onChangeCallback
         * @param $waitingPromise
         */
        var bindOnChange = function ($input, onChangeCallback, $waitingPromise) {
            if (!$waitingPromise) {
                $waitingPromise = $.Deferred().resolve();
            }

            if (!onChangeCallback) {
                console.error("onChangeCallback is mandatory for bindOnChange");
                return;
            }

            // if we need to wait for the form to be ready
            $waitingPromise.done(function () {
                var widget = expresso.util.UIUtil.getKendoWidget($input);
                if (widget) {
                    widget.bind("change", function (e) {
                        // console.log($input.attr("name") +
                        //     " expressoTriggered:" + e.expressoTriggered +
                        //     " expressoUserTriggered:" + e.expressoUserTriggered
                        //     , e);

                        if (!e.isDefaultPrevented()) {
                            var dataItem;
                            e.sender = e.sender || {};

                            if ($input.data("role") == "dropdowntree") {
                                // DropdownTree
                                // only this.value() is available
                                e.sender._userTriggered = !e.expressoTriggered;

                                // get the dataItem from the treeview
                                dataItem = widget.treeview.dataItem(widget.treeview.select());
                            } else {
                                // ComboBox and DropDownList
                                // this.dataItem() and this.value() are available
                                // e.sender._userTriggered is available
                                if (widget.dataItem) {
                                    dataItem = widget.dataItem();
                                }

                                if (e.expressoUserTriggered) {
                                    e.sender._userTriggered = true;
                                }
                            }

                            // use standard userTriggered event
                            if (e.userTriggered === undefined) {
                                e.userTriggered = e.sender._userTriggered;
                            } else {
                                e.sender._userTriggered = e.userTriggered;
                            }
                            onChangeCallback.call(widget, e, dataItem);
                        }
                    });
                } else {
                    $input.on("change", onChangeCallback);
                }
            });
        };

        /**
         *
         * @param $input
         * @param options
         */
        var buildUpload = function ($input, options) {
            var _this = this;

            // make sure the input if defined correctly
            // <input name='file' type='file' />
            $input.attr("type", "file");

            // avoid null pointer
            options = options || {};

            // wrap the input
            $input.wrap("<div><div class='input-wrap exp-upload-div'><div class='k-content'></div></div></div>");
            //<label>" + this.getLabel("document") + "</label>

            var $window = $input.closest(".k-window-content");
            if (!$window.length) {
                $window = $input.closest(".input-wrap").parent();
            }

            var $deferred = $.Deferred();
            $input.kendoUpload({
                async: {
                    saveUrl: options.url || "define later",
                    removeUrl: null,
                    autoUpload: false
                },
                multiple: false,
                upload: function (e) {
                    var data = {};

                    // add the creation user
                    data["creationUserId"] = expresso.Common.getUserInfo().id;

                    // add the document meta data
                    if (options.resourceManager) {
                        if (options.resourceManager.siblingResourceManager &&
                            options.resourceManager.appDef.appClass.startsWith("cezinc")) {
                            // CEZinc only
                            data["resourceId"] = options.resourceManager.siblingResourceManager.currentResource.id;
                            data["resourceName"] = options.resourceManager.siblingResourceManager.resourceName;
                            data["resourceSecurityPath"] = options.resourceManager.siblingResourceManager.getResourceSecurityPath();

                            var documentFolderPath;
                            if (options.resourceManager.siblingResourceManager.currentResource[_this.resourceManager.siblingResourceManager.resourceFieldNo]) {
                                // use the resourceNo
                                // ex: project/AP-1090, activityLogRequest/0011234
                                documentFolderPath = options.resourceManager.siblingResourceManager.getResourceSecurityPath() + "/" +
                                    options.resourceManager.siblingResourceManager.currentResource[options.resourceManager.siblingResourceManager.resourceFieldNo];
                            } else {
                                // probably a sub resource
                                documentFolderPath = options.resourceManager.siblingResourceManager.getRelativeWebServicePath(options.resourceManager.siblingResourceManager.currentResource.id);
                            }
                            data["documentParameter"] = documentFolderPath;
                        }
                    }

                    // add token if present
                    if (expresso.Security) {
                        data["sessionToken"] = expresso.Security.getSessionToken();
                    }

                    // add any data
                    if (options.data) {
                        $.extend(data, options.data);
                    }

                    expresso.util.UIUtil.showLoadingMask($window, true);

                    //console.log("Upload data: " + JSON.stringify(data));
                    e.data = data;
                },
                success: function (/*e*/) {
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false);

                    $deferred.resolve();
                },
                error: function (e) {
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false);
                    expresso.Common.displayServerValidationMessage(e.XMLHttpRequest);
                    $deferred.reject();
                }
            });

            var kendoUpload = $input.data("kendoUpload");

            return {
                kendoUpload: kendoUpload,
                upload: function (resource) {
                    if (options.resourceManager) {
                        var url = options.resourceManager.getUploadDocumentPath(resource.id);
                        //console.log("url [" + url + "]");
                        kendoUpload.options.async.saveUrl = url;
                    }
                    kendoUpload.upload();
                    return $deferred;
                }
            };
        };

        // return public properties and methods
        return {
            initializeForm: initializeForm,
            resetForm: resetForm,
            bindOnChange: bindOnChange,
            setFormReadOnly: setFormReadOnly,
            setFieldReadOnly: setFieldReadOnly,
            hideField: hideField,
            updateLabel: updateLabel,
            highlightMissingRequiredField: highlightMissingRequiredField, // @deprecated
            highlightField: highlightField,
            displayMissingRequiredFieldNotification: displayMissingRequiredFieldNotification,

            buildDropDownList: buildDropDownList,   // local (array or url) and single select
            buildComboBox: buildComboBox,           // remote and single select
            buildMultiSelect: buildMultiSelect,     // local (array or url)/remote and multiple select
            buildDropDownTree: buildDropDownTree,  // local (array or url) and single select
            buildLookupSelection: buildLookupSelection, // remote multiple select lookup
            addDataItemToWidget: addDataItemToWidget,

            buildTreeView: buildTreeView,
            buildCheckBox: buildCheckBox,
            buildRadioButton: buildRadioButton,
            buildUpload: buildUpload,

            getKendoWidget: getKendoWidget,
            destroyKendoWidgets: destroyKendoWidgets,

            buildWindow: buildWindow,
            setWindowDimension: setWindowDimension,
            buildMessageWindow: buildMessageWindow,
            buildPromptWindow: buildPromptWindow,
            buildCommentWindow: buildCommentWindow,
            buildYesNoWindow: buildYesNoWindow,
            buildSearchWindow: buildSearchWindow,
            addSearchButton: addSearchButton,
            getFormWidth: getFormWidth,

            buildReportSelector: buildReportSelector,
            showNotification: showNotification,
            showLoadingMask: showLoadingMask
        };
    }()
);

