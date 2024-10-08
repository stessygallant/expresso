var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * This is an utility module. It contains some utilities method to build common UI widgets.
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.util.UIUtil = (function () {
        var MAXIMUM_RESULTS = 50;

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
                    case "expressodatepicker":
                        widget = $element.data("kendoExpressoDatePicker");
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
                    case "expressodateselector":
                        widget = $element.data("kendoExpressoDateSelector");
                        break;
                    case "expressopicturepicker":
                        widget = $element.data("kendoExpressoPicturePicker");
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
         * Convert (or create) a KendodUI combo box to a server side filtering combo box
         * @param $input the jQuery SELECT element to convert
         * @param resourceURL url for the resource
         * @param [customOptions]
         * @returns {*} the KendoUI ComboBox widget
         */
        var buildComboBox = function ($input, resourceURL, customOptions) {
            var $deferred = $.Deferred();

            // if the DOM element is not present, return immediately
            if (!$input || !$input.length) {
                // the input may be removed because of permission
                return $deferred.resolve();
            }
            var fieldName = $input.attr("name");

            // patch: if the input has .k-textbox class, remove it
            if ($input.hasClass("k-textbox")) {
                $input.removeClass("k-textbox");
            }

            var cb = $input.data("kendoComboBox");
            if (cb) {
                alert("Do not define the comboxbox if using server side filtering [" + fieldName +
                    ":" + $input.attr("class") + "]");
                return $deferred.reject();
            }

            // make sure that the user has access to the resource
            var url;
            var resourceName;
            if (resourceURL.startsWith("/")) {
                //  static URL. This is used by report in CEZinc
                url = expresso.Common.getWsResourcePathURL() + resourceURL;
            } else {
                url = expresso.Common.getWsResourcePathURL() + "/" + resourceURL + "/search";
                resourceName = getResourceNameFromURL(resourceURL);

                if (resourceName && !expresso.Common.isUserAllowed(resourceName, "read")) {
                    console.warn("Hiding server side combo box because user does not have read access to [" + resourceName + "]");
                    $input.closest("div").hide();
                    return $deferred.resolve();
                }
            }

            // Custom Options
            // - triggerChangeOnInit
            // - filter
            // - resource
            // - field
            // - model
            // - value
            // - nullable
            // - dataTextField
            // - avoidSorting
            // - cascadeFrom
            // - cascadeFromField
            customOptions = customOptions || {};

            // if field is defined, get the options from the field too
            if (customOptions.field) {
                $.extend(customOptions, customOptions.field);
                if (customOptions.field.reference) {
                    $.extend(customOptions, customOptions.field.reference);
                }
            }
            //console.log(fieldName + " - customOptions", customOptions);

            // custom label
            var dataTextField = customOptions.dataTextField || "label";

            // minimum length before an auto search
            var minLength = customOptions.minLength || 3;

            // create the combo box
            var searchEvent = {};
            cb = $input.kendoComboBox({
                autoBind: true,
                dataValueField: "id",
                dataTextField: dataTextField,
                valuePrimitive: true,
                filter: "contains",
                suggest: false, // auto select first option.
                highlightFirst: true, // the first suggestion will be automatically highlighted. (selected on enter)
                syncValueAndText: false, // avoid auto adding new text
                value: retrieveCurrentValue($input, customOptions),
                minLength: minLength,  // avoid filtering for at least n characters
                delay: 200, // default is 200
                enforceMinLength: true, // if true, on clear, do not show all options
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
                                var filter = customOptions.filter;
                                if (filter) {
                                    if (typeof filter === "function") {
                                        filter = filter();
                                    }
                                    data = expresso.Common.buildKendoFilter(filter, null, true);
                                }

                                // this is used the first time to load the default value
                                if (!searchEvent.initialized) {
                                    searchEvent.initialized = true;
                                    data.id = retrieveCurrentValue($input, customOptions);

                                    if (!data.id) {
                                        data.id = -1;
                                    }
                                }
                                // console.log(fieldName + " - search", searchEvent);
                                searchEvent.dirty = false;
                                return data;
                            }
                        }
                    },
                    schema: {
                        parse: function (response) {
                            if (!response.data) {
                                response = {
                                    data: response,
                                    searchText: searchEvent.searchText,
                                    tooManyResults: false
                                }
                            }

                            // console.log(fieldName + " - parse response [" + (response.searchText || "") + "]");
                            searchEvent.skippingResponse = false;

                            // verify if token is the last token. If not, ignore this response
                            // The combobox does not send a new request until it gets the response
                            // to the previous request.
                            // But we need this code if we manually send a search request
                            if (searchEvent.searchText && (response.searchText || "").trim() != (searchEvent.searchText || "").trim()) {
                                // console.log("Ignoring response searchText [" + response.searchText + "] <> expected [" + searchEvent.searchText + "]");
                                searchEvent.skippingResponse = true;
                                return [];
                            }

                            var dataItems = response.data;
                            if (dataTextField != "label") {
                                for (var i = 0; i < dataItems.length; i++) {
                                    dataItems[i].label = dataItems[i][dataTextField];
                                }
                            }

                            // always sort on the label
                            if (!customOptions.avoidSorting) {
                                dataItems.sort(function (r1, r2) {
                                    if (r1[dataTextField] && r2[dataTextField]) {
                                        return r1[dataTextField].localeCompare(r2[dataTextField]);
                                    } else {
                                        return 1;
                                    }
                                });
                            }

                            // verify if there is a null element at the end
                            if (response.tooManyResults) {
                                var tooManyResults = {id: 0};
                                tooManyResults[dataTextField] = expresso.Common.getLabel("tooManyResults");
                                dataItems.push(tooManyResults);
                            }

                            return dataItems;
                        }
                    }
                },
                error: function (e) {
                    // console.error(fieldName + " - error event", e);
                },
                select: function (e) {
                    // console.log(fieldName + " - select event - dataItem[" + (e.dataItem ? e.dataItem.id : null) + "]");
                },
                filtering: function (e) {
                    // calling this.value() will trigger this event
                    var text = ((e.filter && e.filter.value) || "");
                    // console.log(fieldName + " - filtering event text[" + text + "]", e);
                    if (text) {
                        searchEvent.searchText = text;
                    } else {
                        // console.log(fieldName + " - filtering preventDefault", e);
                        e.preventDefault();
                    }
                },
                open: function () {
                    searchEvent.popupOpened = true;
                },
                close: function () {
                    searchEvent.popupOpened = false;
                },
                dataBound: function () {
                    // console.log(fieldName + " - dataBound event - forceSelect[" + searchEvent.forceSelect + "] inputText[" +
                    //     cb.input.val() + "] data: " + cb.dataSource.data().length);
                    highlightField($input, null, null, true);

                    // do not select if user still typing
                    if (searchEvent.forceSelect) {
                        // console.log("*** forceSelect");
                        if (cb.dataSource.data().length == 0) {
                            // console.log("No result");
                            if (searchEvent.skippingResponse) {
                                cb.close();
                            } else {
                                highlightField($input);
                            }
                        } else if (cb.dataSource.data().length == 1) {
                            // console.log("Auto select");
                            searchEvent.forceSelect = false;
                            searchEvent.searchText = null;
                            cb.select(0);
                            cb.close();
                            cb.trigger("change", {expressoUserTriggered: true});

                            // otherwise the clear button will not work after
                            cb._blur();
                        } else {
                            // console.log("Multiple results");
                            searchEvent.forceSelect = false;
                            searchEvent.searchText = null;
                        }
                    } else if (cb.dataSource.data().length == 0 && cb.input.val()) {
                        if (!searchEvent.dirty) {
                            // we cannot do it: it will clear the input while the user is still typing slowly
                            // reset text
                            // cb.input.val("");
                            highlightField($input);
                        }
                    }
                },
                change: onChangeEvent($input, customOptions, resourceURL)
            }).data("kendoComboBox");

            // trigger at init
            cb.dataSource.one("change", function () {
                // console.log(fieldName + " - dataSource change");
                $deferred.resolve(cb);
                triggerChangeEvent($input, cb, customOptions);
            });

            // Enter, Tab
            cb.input[0].addEventListener("keydown", function (e) {

                // if it is a local datasource (after a setDataSource by the user), then return
                if (!cb.dataSource.options || !cb.dataSource.options.serverFiltering) {
                    return;
                }

                if (e.key == "Enter" /*|| e.key == "Tab"*/) {
                    searchEvent.forceSelect = true;
                    searchEvent.searchText = this.value;

                    if (searchEvent.dirty) {
                        e.stopPropagation(); // otherwise it will assign the text for the value and trigger change

                        // must have minimum length
                        var searchString = this.value || "";
                        if (searchString.length < minLength) {
                            searchString = searchString.paddingLeft(Array(minLength + 1).join(" "));
                        }
                        // console.log("******** Enter/tab Force search [" + searchString + "]");
                        cb.search(searchString);
                    } else {
                        // console.log("******** Skipping Enter/tab (not dirty) [" + this.value + "]");
                        if (searchEvent.popupOpened) {
                            // if already displayed, let the ENTER does the job
                            searchEvent.forceSelect = false;
                            searchEvent.searchText = null;
                        } else {
                            e.stopPropagation(); // otherwise it will assign the text for the value and trigger change
                        }
                    }
                } else {
                    // reset flag
                    searchEvent.forceSelect = false;
                    searchEvent.searchText = null;

                    // this.value does not contain the last character
                    // console.log("Key [" + e.key + "]: " + (e.key && e.key.length == 1));
                    if (e.key && e.key.length == 1) {
                        searchEvent.dirty = true;
                    }
                }
            }, true); // call first

            // on focus, select the text
            cb.input.on("focus", function () {
                // console.log("Focus [" + cb.value() + "]");
                $(this).select();
            });

            // on blur, if the text is not valid, remove it
            cb.input.on("blur", function () {
                // console.log("Blur this.value[" + this.value + "] cb.value[" + cb.value() + "] dataItem[" + cb.dataItem() +
                //     "]", searchEvent);
                if (searchEvent.forceSelect) {
                    // console.log("do not blur");
                } else {
                    if (cb.value() && !cb.dataItem()) {
                        // console.log("Clear value on blur");
                        $input.setval(null);
                    } else if (this.value && !cb.value()) {
                        // console.log("Clear text on blur");
                        $(this).val("");
                    }
                }
            });

            // force search when opening the popup
            cb.input.closest(".k-combobox").find(".k-select")[0].addEventListener("click", function (e) {
                // if it is a local datasource (after a setDataSource by the user), then return
                if (!cb.dataSource.options || !cb.dataSource.options.serverFiltering) {
                    return;
                }

                if (!searchEvent.popupOpened) {
                    e.preventDefault();
                    e.stopPropagation();
                    // console.log(fieldName + " - click popup [" + searchEvent.searchText + "] data:" + cb.dataSource.data().length);
                    // trigger a search (3 spaces minimum)
                    cb.search("   ");
                }
            }, true); // first

            // PATCH: the clear button is not userTriggerred, but it must be if the client is validating e.userTriggered
            cb.input.closest(".k-combobox").find(".k-clear-value").on("click", function () {
                cb.expressoPreviousValue = undefined;
                cb.trigger("change", {expressoUserTriggered: true});
            });

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

            var kendoPrompt = $windowDiv.kendoPrompt({
                title: title,
                content: text,
                value: customOptions.value || "",
                width: customOptions.width || 350,
                messages: {
                    okText: customOptions.okText || expresso.Common.getLabel("save"),
                    cancel: customOptions.cancel || expresso.Common.getLabel("cancel")
                }
            }).data("kendoPrompt");

            // display the window
            kendoPrompt.open();

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

            var $deferred = $.Deferred();
            kendoPrompt.result.done(function (data) {
                if (customOptions.mandatory && !data) {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("dataMandatory"));
                    $deferred.reject();
                } else {
                    $deferred.resolve(data);
                }
            }).fail(function () {
                $deferred.reject();
            });

            return $deferred;
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

            var fontSize = getDefaultFontSize($form[0]) || 12;
            //console.log("fontSize: " + fontSize);

            var percFactor = fontSize / 12; // 12px if the default font size
            //console.log("percFactor: " + percFactor);

            var sideMargin = 10 * percFactor;
            // special case
            if (options.width == "full") {
                sideMargin = 0;
            }

            var width = options.width;
            var $browserWindow = $(window);
            var singleColumnWidth = 420 * percFactor;
            var maxWidth = Math.min($browserWindow.width() - sideMargin, 1500 * percFactor);

            if (!width) {
                if ($form.hasClass("exp-form-max-width")) {
                    width = maxWidth;
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
            } else {
                if (typeof width === "string" && width.endsWith("px")) {
                    width = parseInt(width.substring(0, width.length - 2)) * percFactor;
                } else if (width == "100%" || width == "max" || width == "full") {
                    width = maxWidth;
                }
            }

            // make sure not to be larger than the window
            width = Math.min(maxWidth, width);
            // console.log("width: " + width);
            return width;
        };

        /**
         *
         * @param el
         * @returns {number}
         */
        var getEmSize = function (el) {
            return parseFloat(window.getComputedStyle(el, "").fontSize.match(/(\d+(\.\d*)?)px/)[1]);
        };

        /**
         *
         * @param parentElement
         * @returns {number}
         */
        var getDefaultFontSize = function (parentElement) {
            parentElement = parentElement || document.body;
            var div = document.createElement('div');
            div.style.width = "1000em";
            parentElement.appendChild(div);
            var pixels = div.offsetWidth / 1000;
            parentElement.removeChild(div);
            return pixels;
        };

        /**
         *
         * @param $windowContent
         * @param options
         */
        var setWindowDimension = function ($windowContent, options) {
            // console.log("setWindowDimension");

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
            if (options.width == "full") {
                leftMargin = 0;
            }

            // calculate the maximum height for the window
            // do not use the outerHeight for the titleBar (it will report 0). Add margin for it
            var titleBarMargin = 15;
            var windowMargin = 8; // allow x pixels on each top and bottom
            if (options.height == "full") {
                windowMargin = 0;
            }

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
            if (height == "100%" || height == "max" || height == "full" || $userRootDivContent.hasClass("exp-form-max-height")) {
                $formWrapper.height(maxHeight);
                options.top = 0;
            } else if (height) {
                $formWrapper.height(height);
            }
            //console.log("$formWrapper.height(): " + $formWrapper.height());

            var maxTopMargin = Math.max(0, ($browserWindow.height() - $formWrapper.height()) / 2 -
                ($titleBar.height() + titleBarMargin + (windowMargin * 2)) - 15);
            // console.log("maxTopMargin: " + maxTopMargin);
            var topMargin = options.top;
            // console.log("topMargin: " + topMargin);
            if ((!topMargin && topMargin !== 0) || topMargin > maxTopMargin) {
                topMargin = maxTopMargin;
            }

            // if the class mark exp-form-top is defined, put the window at the top
            if (topMargin < 0 || $userRootDivContent.hasClass("exp-form-top")) {
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
            kendoWindow.unbind("resize").bind("resize", onResize);
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
                confirmationOnClose: false,
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
                close: function (e) {
                    // console.log("buildWindow - close " + e.userTriggered + ":" + options.confirmationOnClose);
                    var window = this;
                    if (e && e.userTriggered && options.confirmationOnClose) {
                        // avoid closing
                        e.preventDefault();

                        expresso.util.UIUtil.buildYesNoWindow(expresso.Common.getLabel("confirmTitle"),
                            expresso.Common.getLabel("confirmWindowClosing")).done(function () {
                            window.close();
                        });
                    }
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
                    if (options.autoFocus) {
                        window.setTimeout(function () {
                            var $firstInput = $windowDiv.find(":input:visible:not([readonly]):enabled:first");
                            $firstInput.focus();
                        }, 500);
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
                    // console.log("Window was resized");
                    if (options.resizeDebonced) {
                        options.resizeDebonced();
                    }
                },
                maximize: function () {
                    // console.log("Window was maximized");
                    if (options.resizeDebonced) {
                        options.resizeDebonced();
                    }
                },
                restore: function () {
                    // console.log("Window was restored");
                    if (options.resizeDebonced) {
                        options.resizeDebonced();
                    }
                }
            }).data("kendoWindow");

            // register a listener on the buttons
            $windowDiv.find(".k-edit-buttons button").on("click", function (e) {
                e.preventDefault();
                expresso.util.UIUtil.showLoadingMask($windowDiv, true, "exp-window-buttons");
                var saveResult;
                var $button = $(this);
                if ($button.hasClass("exp-window-save-button")) {
                    if (options.save) {
                        // if the save method returns true, close the window
                        saveResult = options.save.call($windowDiv);
                        if (saveResult === false) {
                            // do not close
                            expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                        } else if (saveResult === true || !saveResult) {
                            expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                            kendoWindow.close();
                        } else { // promise
                            //  do not close
                            saveResult.done(function () {
                                kendoWindow.close();
                            }).always(function () {
                                expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                            });
                        }
                    } else {
                        expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                        kendoWindow.close();
                    }
                } else {
                    if (options.buttonClicked) {
                        saveResult = options.buttonClicked.call($button, e, $windowDiv, options);
                        if (saveResult === false) {
                            // do not close
                            expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                        } else if (saveResult === true || !saveResult) {
                            expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                            kendoWindow.close();
                        } else { // promise
                            //  do not close
                            saveResult.done(function () {
                                kendoWindow.close();
                            }).always(function () {
                                expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
                            });
                        }
                    } else {
                        expresso.util.UIUtil.showLoadingMask($windowDiv, false, "exp-window-buttons");
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
                widget = widget.data("kendoDropDownList");
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
            // find the name of the attribute
            var resource = customOptions.resource;
            var inputName = $input.attr("name");
            var bindNameId = getBindName($input);
            var attName;
            var objectDefaultValue = {}; // default value for the object
            if (bindNameId && bindNameId.endsWith("Id")) {
                // get the object for the ID
                attName = bindNameId.substring(0, bindNameId.length - 2);
                if (customOptions.model && customOptions.model.fields[attName] && customOptions.model.fields[attName].defaultValue) {
                    objectDefaultValue = customOptions.model.fields[attName].defaultValue;
                }
            }

            return function (e) {
                e = e || {};
                e.sender = e.sender || {};
                if (e.userTriggered === undefined) {
                    e.userTriggered = e.sender._userTriggered;
                }
                // force userTriggered
                if (e.expressoUserTriggered) {
                    e.userTriggered = true;
                    e.sender._userTriggered = true;
                }

                var widget = getKendoWidget($input);

                // remove the highlight
                highlightField($input, null, null, true);

                var value;
                if (widget.value) {
                    value = widget.value();
                }

                var dataItem;
                if (widget.dataItem) {
                    // Combobox, DropDownList
                    dataItem = widget.dataItem();
                }
                if (!dataItem && e.sender.treeview) {
                    // DropDownTree
                    dataItem = e.sender.treeview.dataItem(e.sender.treeview.select());
                }

                /**
                 * Inline method to trigger client change event
                 * @param ev
                 */
                var triggerChange = function (ev) {
                    if (resource && attName) {
                        // perform only if done by a user (otherwise the object is already loaded by default)
                        // console.log("triggerChange: " + attName, ev);
                        if (ev.userTriggered) {
                            // console.log("Setting [" + attName + "]", dataItem);
                            // console.log("Setting [" + attName + "]", objectDefaultValue);
                            // do not allow to set null: it will crash the grid (ex: equipment.equipmentNo)
                            resource.set(attName, $.extend(true, JSON.parse(JSON.stringify(dataItem || {})), objectDefaultValue));
                        }
                    }

                    if (customOptions.change) {
                        // PATCH: because KendoUI will trigger twice the same event on blur,
                        // we need to keep the previous value and do not fire the event
                        // if the value is the same
                        if (widget.expressoPreviousValue === undefined || widget.expressoPreviousValue != value) {
                            widget.expressoPreviousValue = value;
                            // console.log("Calling change event listener - userTriggered: " + ev.userTriggered);
                            customOptions.change.call(widget, ev);
                        }
                    }
                };

                // console.log(inputName + " - change event" + /* bindNameId[" + bindNameId + "]" + */ " value[" + value + "] dataItem[" + (dataItem ? dataItem.id : null) + "] userTriggered: " + e.userTriggered);
                if (value && !dataItem) {
                    // verify if the value is in the datasource
                    dataItem = widget.dataSource.get(value);
                    if (dataItem) {
                        // select it
                        widget.select(widget.dataSource.indexOf(dataItem));
                        triggerChange.call(widget, e);
                    } else {
                        if (resourceURL) {
                            console.log(inputName + " - We need to get the data item for the value[" + value + "]");
                            expresso.Common.sendRequest(resourceURL + "/" + value, null, null, null, {
                                ignoreErrors: true,
                                waitOnElement: null
                            }).done(function (result) {
                                // add the new dataItem in the datasource
                                dataItem = addDataItemToWidget(result, widget);

                                // then trigger the change
                                triggerChange.call(widget, e);
                            }).fail(function () {
                                // not found
                                // console.log("Id not found [" + value + "]. Setting null");
                                widget.value(null); // this will perform a search
                                highlightField($input);
                                triggerChange.call(widget, e);
                            });
                        } else {
                            // if it userTriggered, the value is not the id, then we cannot search for the id
                            highlightField($input);
                        }
                    }
                } else if (value && dataItem) {
                    // make sure the dataItem is part of the dataSource.
                    if (!widget.dataSource.get(value)) {
                        // console.log(inputName + " - adding dataItem for value[" + value + "]");
                        dataItem = addDataItemToWidget(dataItem, widget);
                    }
                    triggerChange.call(widget, e);
                } else if (!value && dataItem && dataItem.id) {
                    console.warn("value is null but not the dataItem");
                    triggerChange.call(widget, e);
                } else {
                    triggerChange.call(widget, e);
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
                        // console.log($input.attr("name") + " - Trigger change");
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

            var bindNameId = getBindName($input);

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
        };

        /**
         * Utility method to convert a list of string to a datasource
         * @param list
         * @param [labels]
         * @param [dataValueField]
         * @param [dataTextField]
         * @returns {*}
         */
        var convertList = function (list, labels, dataValueField, dataTextField) {
            dataValueField = dataValueField || "id";
            dataTextField = dataTextField || "label";

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

            return expresso.Common.updateDataValues(list, labels);
        };

        /**
         * Set the datasource on the $input (dropdownlist or combobox)
         * @param $input
         * @param data
         * @param [defaultValue]
         * @param [labels]
         */
        var setDataSource = function ($input, data, defaultValue, labels) {
            data = data.data || data;

            var widget = getKendoWidget($input);
            var currentValue = widget.value();
            data = convertList(data, labels);

            // console.log("Setting new datasource on [" + $input[0].name + "]", data);
            widget.value(null);
            widget.setDataSource(new kendo.data.DataSource({data: data}));

            if (defaultValue !== undefined) {
                // console.log("Setting default value [" + defaultValue + "] " + (typeof defaultValue));
                $input.setval(defaultValue);
            } else if (currentValue && widget.dataSource.get(currentValue)) {
                // console.log("Putting back current value [" + currentValue + "]");
                $input.setval(currentValue);
            } else {
                // console.log("Setting null value [" + null + "]");
                $input.setval(null);
            }
        };

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
                // the input may be removed because of permission
                return $deferred.resolve();
            }

            // patch: if the input has .k-textbox class, remove it
            if ($input.hasClass("k-textbox")) {
                $input.removeClass("k-textbox");
            }

            // deal with undefined customOptions
            customOptions = customOptions || {};
            customOptions.fieldValues = customOptions.fieldValues || {};

            var dataValueField = customOptions.dataValueField || "id";
            var dataTextField = customOptions.dataTextField || customOptions.fieldValues.dataTextField || "label";
            var dataTextFieldFunction;
            if (typeof dataTextField === "function") {
                dataTextFieldFunction = dataTextField;
                dataTextField = "label";
            }

            // get the info from the field if defined
            if (customOptions.field && customOptions.field.values) {
                customOptions.nullable = customOptions.nullable || customOptions.field.nullable;
                customOptions.selectFirstOption = customOptions.selectFirstOption || customOptions.field.values.selectFirstOption;
                customOptions.grouping = customOptions.grouping || customOptions.field.values.grouping;
                customOptions.sortField = customOptions.sortField || customOptions.field.values.sortField;
                if (customOptions.triggerChangeOnInit === undefined) {
                    customOptions.triggerChangeOnInit = customOptions.field.values.triggerChangeOnInit;
                }
            }

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

                            if (dataTextFieldFunction) {
                                $.each(response, function () {
                                    var item = this;
                                    item.label = dataTextFieldFunction(item);
                                });
                            }

                            // if the list is only string, build a complete data source
                            response = convertList(response, customOptions.labels, dataValueField, dataTextField);

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
                // build a complete data source
                if (wsListPathOrData) {
                    wsListPathOrData = convertList(wsListPathOrData, customOptions.labels, dataValueField, dataTextField);
                }

                dataSource = {
                    data: wsListPathOrData,
                    group: (customOptions.grouping ? {
                        field: (typeof customOptions.grouping === "string" ?
                            customOptions.grouping : customOptions.grouping.field)
                    } : undefined)
                };
            }

            var initValue = retrieveCurrentValue($input, customOptions);
            // console.log("initValue: " + initValue);
            var defaultOptions = {
                dataValueField: dataValueField,
                dataTextField: dataTextField,
                valuePrimitive: true,
                dataSource: dataSource,
                enable: customOptions.enable,
                value: initValue,
                size: customOptions.size,
                height: customOptions.height || 400,
                filter: (customOptions.grouping && customOptions.grouping.filter !== false ? "contains" : customOptions.inplaceFilter),
                change: onChangeEvent($input, customOptions),
                dataBound: function (/*e*/) {
                    if (customOptions.selectFirstOption === true && !initValue) {
                        // console.log("Selecting first option");
                        $input.data("kendoDropDownList").select(0);
                    }
                }
            };

            // if the option label is null, add an empty one
            if (customOptions.optionLabel === null || typeof customOptions.optionLabel === "string" ||
                (customOptions.field && customOptions.field.nullable) || customOptions.nullable) {
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

            // console.log("OPTIONS: " + JSON.stringify(defaultOptions));
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
                // the input may be removed because of permission
                return $deferred.resolve();
            }

            // make sure it is an input
            if ($input[0].nodeName != "INPUT") {
                // console.warn("You should use <input> for a kendoDropDownTree for [" + $input[0].name + "]. Otherwise $input.val() will return null");
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
                placeholder: customOptions.optionLabel,
                valuePrimitive: true,
                filter: customOptions.showSearch !== false ? customOptions.showSearch || "contains" : undefined,
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

            // PATCH: dataItem from DropDownTree required the selected node
            // by default, is not is provided, provide the one selected from the treeview
            kendoDropDownTree.oriDataItem = kendoDropDownTree.dataItem;
            kendoDropDownTree.dataItem = function (node) {
                if (node === undefined) {
                    return kendoDropDownTree.oriDataItem(kendoDropDownTree.treeview.select());
                } else {
                    return kendoDropDownTree.oriDataItem(node);
                }
            };

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
            customOptions.fieldReference = customOptions.fieldReference || {};

            // because we cannot apply the max-height directly on the multiselect (scroll does not work when readonly)
            // we need to wrap the select element
            $select.wrap("<div class='exp-multiselect-wrap'></div>");

            var dataSource;
            var serverFiltering = (customOptions.serverFiltering !== false);

            var dataValueField = customOptions.dataValueField || "id";
            var dataTextField = customOptions.dataTextField || customOptions.fieldReference.dataTextField || "label";

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

                                    // we need also the first 50 data
                                    data.retrieveIdOnly = false;
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

                            // verify if there is a null element at the end
                            if (response && response.length >= MAXIMUM_RESULTS) {
                                var tooManyEntry = {id: 0};
                                tooManyEntry[dataTextField] = expresso.Common.getLabel("tooManyResults");
                                response.push(tooManyEntry);
                            }

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

            // add support for selectAllButton
            if (customOptions.selectAllButton) {
                var $inputWrap = $select.closest(".exp-input-wrap");
                $inputWrap.addClass("exp-ref-with-buttons");

                var $selectAllButton = $("<button class='exp-ref-button exp-ref-select-all-button k-button' title='" +
                    expresso.Common.getLabel("selectAllButtonTitle") + "'><i class='fa fa-list'></i></button>").appendTo($inputWrap);

                $selectAllButton.on("click", function () {
                    var values = $.map(kendoMultiSelect.dataSource.data(), function (dataItem) {
                        return dataItem[dataValueField];
                    });
                    kendoMultiSelect.value(values);
                    kendoMultiSelect.trigger("change");
                });
            }

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
                    },
                    change: function (/*e*/) {
                    },
                    select: function (/*e*/) {
                        // var dataItem = this.dataItem(e.node);
                        // console.log("TreeView - Selecting: " + this.text(e.node), dataItem);
                        // if () {
                        //     // prevent selection
                        //     e.preventDefault();
                        // }
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
            var $parent = $input.closest(".input-wrap,.exp-input-wrap");
            $parent.addClass("exp-ref-with-buttons");

            var widget = getKendoWidget($input);

            // SEARCH BUTTON
            var $searchButton = $("<button class='exp-ref-button exp-ref-search-button'><i class='fa fa-search'></i></button>");
            $input.closest(".k-widget").after($searchButton);

            if (reference.allowView === false ||
                (reference.allowCreate !== true && (reference.resourceName == "user" || reference.resourceName == "person"))) {
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
                            expresso.Common.loadApplication(reference.resourceManager, {autoEdit: true}).done(function (resourceManager) {
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

                    $viewButton.removeClass("exp-ref-view-button")
                        .removeClass("exp-ref-add-button")
                        .addClass("exp-ref-view-button")
                        .prop("disabled", false);

                    $icon.removeClass("fa-eye")
                        .removeClass("fa-pencil")
                        .removeClass("fa-plus")
                        .addClass("fa-eye");

                    // verify if the user can create the resource
                    if (id && id !== "0") {
                        // console.log("ID[" + id + "]", id);
                        // if (expresso.Common.isUserAllowed(expresso.Common.getResourceSecurityPathFromPath(reference.resourcePath), "update")) {
                        //     expresso.Common.sendRequest(reference.resourcePath + "/verifyActionsRestrictions", null, null, {
                        //         id: id,
                        //         actions: "update"
                        //     }, {waitOnElement: null}).done(function (result) {
                        //         if (result && result["update"]) {
                        //             // keep the eye: pencil may be confusing
                        //             //$icon.removeClass("fa-eye").addClass("fa-pencil");
                        //         }
                        //     });
                        // }
                    } else {
                        if (reference.allowCreate === true && expresso.Common.isUserAllowed(expresso.Common.getResourceSecurityPathFromPath(reference.resourcePath), "create")) {
                            expresso.Common.sendRequest(reference.resourcePath + "/verifyCreationRestrictions", null, null, null,
                                {waitOnElement: null}).done(function (result) {
                                if (result && result.allowed) {
                                    $icon.removeClass("fa-eye").addClass("fa-plus");
                                    $viewButton.removeClass("exp-ref-view-button").addClass("exp-ref-add-button");
                                    // because the readonly process has already passed, we need to verify it now
                                    if ($viewButton.hasClass("readonly")) {
                                        $viewButton.prop("disabled", true);
                                    }
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
                    expresso.util.UIUtil.buildSearchWindow(reference.resourceManager, reference.filter, reference.sort).done(function (dataItem) {
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

                    // if it is an upload, hide it
                    if ($el.data("role") == "upload") {
                        hideField($el);
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
                    var $inputWrap = $el.closest(".input-wrap,.exp-input-wrap");
                    if ($inputWrap.length) {
                        if (readonly) {
                            $inputWrap.addClass("readonly");
                        } else {
                            $inputWrap.removeClass("readonly");
                        }

                        // if there is an action button beside the input (exemple a lookup)
                        $inputWrap.find("button.exp-ref-button:not(.exp-ref-view-button)").prop("disabled", readonly);
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
                // console.trace(el.name);
                if (el.nodeName == "DIV" || el.nodeName == "FIELDSET") {
                    hide ? $el.hide() : $el.show();
                } else if (supportInputWrap && $el.closest(".input-wrap,.exp-input-wrap").length) {
                    hide ? $el.closest(".input-wrap,.exp-input-wrap").hide() : $el.closest(".input-wrap,.exp-input-wrap").show();
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
            $input.closest(".input-wrap,.exp-input-wrap").find("label").text(label);
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
            var $f;
            if (fieldName) {
                $f = $form.find("[name='" + fieldName + "']");
            } else {
                // the first param is the field
                $f = $form;
            }
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
         * @param $input
         * @param options
         */
        var buildInlineGrid = function ($input, options) {
            var $deferred = $.Deferred();
            var resourceManager = options.field ? options.field.inlineGridResourceManager.resourceManager : options.resourceManager;
            var masterResourceManager = options.masterResourceManager;
            var masterResource = options.masterResource;
            var activeOnly = options.field ? options.field.inlineGridResourceManager.activeOnly : activeOnly;
            var query = options.query || {};
            var newResource = !(masterResource && masterResource.id !== undefined && masterResource.id !== null);

            var $div = $("<div class='exp-grid-inline " + $input.attr("class") + "'></div>").appendTo($input.parent());
            $input.appendTo($div);
            $input.hide();

            // if the parent is already created -> inline grid is online (auto sync ON)
            // if the parent is not already created -> inline grid is offline (auto sync OFF)
            expresso.Common.loadApplication(resourceManager, {
                autoSyncGridDataSource: !newResource,
                multipleSelectionEnabled: false,
                activeOnly: activeOnly
            }).done(function (appInstance) {
                $div.data("resourceManager", appInstance);
                if (masterResourceManager) {
                    appInstance.masterResourceManager = masterResourceManager;

                    // when there is a change in subresource (update or create or delete), publish an update on the current resource
                    appInstance.eventCentral.subscribeEvent([appInstance.RM_EVENTS.RESOURCE_UPDATED, appInstance.RM_EVENTS.RESOURCE_CREATED,
                        appInstance.RM_EVENTS.RESOURCE_DELETED], function () {
                        // console.log("INLINEGRID - child has been changed (reloading master and refreshing counts)");
                        if (appInstance.masterResourceManager.currentResource) {
                            //force the refresh of the form
                            // DO NOT DO IT MANUALLY -> appInstance.masterResourceManager.currentResource.dirty = true;
                            appInstance.masterResourceManager.currentResource.set("makeCurrentResourceDirty", true);
                            appInstance.masterResourceManager.sections.grid.reloadCurrentResource();
                        }
                    });
                } else {
                    // we must fake a master current resource
                    appInstance.masterResourceManager.currentResource = {id: (newResource ? -1 : masterResource.id)};
                }
                appInstance.list($div, query).done(function () {
                    $deferred.resolve(appInstance);
                });
            });
            return $deferred;
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
                        text: report.label || expresso.Common.getLabel("report-" + report.name, labels, null, true) ||
                            expresso.Common.getLabel(report.name, labels, null, true) || report.name,
                        spriteCssClass: "fa fa-" + icon + " " + (classReportKey + (report.key || report.name))
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
                                    return r.key ? r.key == reportKey : r.name == reportKey
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
         * @param event
         * @param $img
         */
        var showMaximizedPicture = function ($img, event) {
            var $body = $("body");
            var $window = $(window);
            var windowHeight = $window.height();
            var windowWidth = $window.width();
            var margin = 16; // 1 em

            var $overlay = $body.children(".exp-picture-overlay");
            if (!$overlay.length) {
                $overlay = $("<div class='exp-picture-overlay'><div><img class='exp-picture-full-size' src='' alt=''><span class='exp-close-button'>X</span></div></div>").appendTo($body);
            }

            // z-index must be the greatest
            var maxZIndex = 10000;
            $body.find(".k-window").each(function () {
                var currentZindex = parseInt($(this).css("zIndex"), 10);
                if (currentZindex > maxZIndex) {
                    maxZIndex = currentZindex;
                }
            });
            $overlay.css("z-index", maxZIndex + 1);

            var $imageOverlay = $overlay.find("img");

            // if there is another path for full size, use it
            var $imgParent;
            var path = $img.data("originalSrc") ? $img.data("originalSrc") : $img.attr("src");
            if (path.startsWith("http")) {
                $imageOverlay.attr("src", path);
            } else {
                // uploaded image but not yet saved
                // move the image to the overlay
                $imgParent = $img.parent();
                $imageOverlay.hide();
                $overlay.append($img);
            }

            if (event) {
                // display the maximized picture to the side of the cursor position
                var x = event.clientX;
                //var y = event.clientY;

                var marginFromCursor = 50;
                $overlay.removeClass("center");
                $overlay.css("left", x + marginFromCursor);
                $imageOverlay.css("max-width", windowWidth - x - marginFromCursor - margin);
                $imageOverlay.css("max-height", windowHeight - 2 * margin);
                $overlay.show();

                // when the mouse leaves the thumbnail picture, hide the full size picture
                $img.one("mouseleave", function () {
                    $overlay.hide();
                });

            } else {
                // display full screen with an X to close the overlay
                $overlay.addClass("center");
                $overlay.css("left", 0);
                $imageOverlay.css("max-width", windowWidth - 2 * margin);
                $imageOverlay.css("max-height", windowHeight - 2 * margin);
                $overlay.css("display", "flex");

                // when the close button is click hide the full size picture
                $overlay.find(".exp-close-button").show().one("click", function () {
                    $(this).hide();
                    $overlay.hide();

                    if (!path.startsWith("http")) {
                        // put back the image
                        $imgParent.append($img);
                        $imageOverlay.show();
                    }
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
            window.setTimeout(function () {
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
            if (typeof customOptions === "string") {
                customOptions = {id: customOptions};
            }
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

                    $element.addClass("exp-loading");
                    if (!$element.find(".exp-loading-mask").length || customOptions.id) {
                        return $("<div class='exp-loading-mask'" +
                            (customOptions.id ? " data-mask-id='" + customOptions.id + "'" : "") + ">" +
                            "<div class='k-loading-image'></div><div class='k-loading-color'></div>" +
                            (customOptions.text ? "<span class='exp-loading-text'>" + customOptions.text + "</span>" : "") +
                            "</div>").appendTo($element);
                    }
                } else {
                    $element.removeClass("exp-loading");
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
         *
         * @param $input
         */
        var triggerChange = function ($input) {
            var widget = expresso.util.UIUtil.getKendoWidget($input);
            if (widget) {
                widget.trigger("change");
            } else {
                $input.trigger("change");
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
            var widget = expresso.util.UIUtil.getKendoWidget($input);
            if (widget) {
                widget.bind("change", function (e) {
                    //console.log("bindOnChange (widget) waiting: " + $input[0].name);
                    $waitingPromise.done(function () {
                        // console.log("bindOnChange (widget) DONE: " + $input[0].name, e);
                        if (!e.isDefaultPrevented()) {
                            var dataItem;
                            e.sender = e.sender || {};

                            if ($input.data("role") == "dropdowntree") {
                                // DropdownTree
                                e.sender._userTriggered = !e.expressoTriggered;

                                // only this.value() is available
                                // this.dataItem() has been added in buildDropDownTree
                                dataItem = widget.dataItem();
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
                });
            } else {
                $input.on("change", function (e) {
                    // console.log("bindOnChange waiting: " + $input[0].name);
                    $waitingPromise.done(function () {
                        //console.log("bindOnChange DONE: " + $input[0].name);
                        if (e && e.userTriggered === undefined) {
                            // for radio button, user e.originalEvent
                            if ($input[0].nodeName == "INPUT" && $input.attr("type") == "radio") {
                                e.userTriggered = !!e.originalEvent;
                            }
                        }
                        onChangeCallback.call(e && e.target ? e.target : $input[0], e);
                    });
                });
            }
        };

        /**
         *
         * @param resourceManager
         * @returns {{}}
         */
        var getDocumentUploadCustomData = function (resourceManager) {
            var customData = {};
            customData["resourceId"] = resourceManager.getCurrentResourceId();
            customData["resourceName"] = resourceManager.resourceName;
            customData["resourceSecurityPath"] = resourceManager.getResourceSecurityPath();

            if (resourceManager.appDef.appClass.startsWith("cezinc")) {
                // add the document meta data (CEZinc only)
                var documentFolderPath;
                if (resourceManager.resourceFieldNo && resourceManager.currentResource &&
                    resourceManager.currentResource[resourceManager.resourceFieldNo]) {
                    // use the resourceNo
                    // ex: project/AP-1090, activityLogRequest/0011234
                    documentFolderPath = resourceManager.getResourceSecurityPath() + "/" +
                        resourceManager.currentResource[resourceManager.resourceFieldNo];
                } else {
                    // probably a sub resource
                    documentFolderPath = resourceManager.getRelativeWebServicePath(resourceManager.getCurrentResourceId());
                }
                customData["documentParameter"] = documentFolderPath;
                customData["documentCategoryId"] = resourceManager.options.defaultDocumentCategoryId;
            }
            return customData;
        };

        /**
         *
         * @param $input
         * @returns {*}
         */
        var getBindName = function ($input) {
            var bindName = $input.attr("data-bind");
            if (bindName) {
                if (bindName && bindName.startsWith("value:")) {
                    bindName = bindName.substring("value:".length);
                }
                if (bindName && bindName.startsWith("checked:")) {
                    bindName = bindName.substring("checked:".length);
                }
            } else {
                bindName = $input.attr("name");
            }
            return bindName;
        };

        /**
         * Return the field definition from the model for the input
         * @param $input
         * @param model
         * @return {null|*}
         */
        var getFieldForInput = function ($input, model) {
            var bindName = getBindName($input);

            if (bindName) {
                return model.fields[bindName];
            } else {
                return null;
            }
        };

        /**
         *
         * @param $input
         * @param options
         * @returns
         */
        var buildNewUpload = function ($input, options) {
            // <input name='file' type='file' />

            // make sure the input if defined correctly
            $input.attr("type", "file");

            // the name of the input must be "file"
            $input.attr("data-name", $input.attr("name"));
            $input.attr("name", "file");

            // remove data-bind
            // $input.removeAttr("data-bind");

            // avoid null pointer
            options = options || {};
            options.customData = options.customData || {};
            options.document = options.document || {};

            var kendoUploadOptions = $.extend(true, {}, {
                async: {
                    saveUrl: "define later",
                    removeUrl: null,
                    autoUpload: false,
                    multiple: options.multiple
                },
                upload: function (e) {
                    options = e.sender.options;
                    // console.log("options: " + JSON.stringify(options));

                    expresso.util.UIUtil.showLoadingMask($input, true, {id: "uploadDocument"});

                    // console.log("uploading");
                    var data = {};

                    // add the creation user (this is only mandatory because of the public path)
                    data["creationUserId"] = expresso.Common.getUserProfile().id;

                    // add token if present
                    if (expresso.Security) {
                        data["sessionToken"] = expresso.Security.getSessionToken();
                    }

                    // add data from document
                    $.extend(data, options.document);

                    // add custom data
                    var d = options.customData;
                    if (typeof d === "function") {
                        d = d();
                    }
                    $.extend(data, d);

                    // console.log("Upload data: " + JSON.stringify(data));
                    e.data = data;

                    // we need to use a special path for upload
                    var url = options.url;
                    if (typeof url === "function") {
                        url = url();
                    }
                    e.sender.options.async.saveUrl = url;
                },
                success: function (e) {
                    if (options.onUploaded) {
                        options.onUploaded(e);
                    }
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($input, false, {id: "uploadDocument"});
                },
                error: function (e) {
                    if (e && e.operation == "upload" && e.XMLHttpRequest) {
                        expresso.Common.displayServerValidationMessage(e.XMLHttpRequest);
                    }
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($input, false, {id: "uploadDocument"});
                }
            }, options);

            return $input.kendoUpload(kendoUploadOptions).data("kendoUpload");
        };

        /**
         *
         * @param $window
         * @param $input
         * @param options
         * @returns
         */
        var buildUpload = function ($window, $input, options) {
            // make sure the input if defined correctly
            // <input name='file' type='file' />
            $input.attr("type", "file");

            // avoid null pointer
            options = options || {};
            options.customData = options.customData || {};

            // wrap the input
            if ($window) {
                if ($input.parent().hasClass("input-wrap")) {
                    $input.parent().addClass("exp-upload-div");
                    $input.wrap("<div class='k-content'></div>");
                } else {
                    $input.wrap("<div class='input-wrap exp-upload-div'></div>").wrap("<div class='k-content'></div>");
                }
            }

            var kendoUploadOptions = $.extend(true, {}, {
                async: {
                    saveUrl: "define later",
                    removeUrl: null,
                    autoUpload: false
                },
                multiple: false,
                upload: function (e) {
                    expresso.util.UIUtil.showLoadingMask($window || $input, true, {id: "uploadDocument"});

                    // console.log("uploading");
                    var data = {};

                    // add any custom data
                    if ($window) {
                        $.each($window.find(".exp-form :input").serializeArray(), function () {
                            // do not include input from Widget
                            if (this.name.indexOf("_input") == -1) {
                                var value = this.value;

                                // because by default any input is empty, if not defined, assign null
                                if (value === "") {
                                    value = null;
                                }
                                data[this.name] = value;
                            }
                        });
                    }

                    // add the creation user (this is only mandatory because of the public path)
                    data["creationUserId"] = expresso.Common.getUserProfile().id;

                    // add token if present
                    if (expresso.Security) {
                        data["sessionToken"] = expresso.Security.getSessionToken();
                    }

                    // add custom data
                    var d = options.customData;
                    if (typeof d === "function") {
                        d = d();
                    }
                    $.extend(data, d);

                    //console.log("Upload data: " + JSON.stringify(data));
                    e.data = data;

                    // we need to use a special path for upload
                    var url = options.url;
                    if (typeof url === "function") {
                        url = url();
                    }
                    e.sender.options.async.saveUrl = url;
                },
                success: function (e) {
                    if (options.onUploaded) {
                        options.onUploaded(e);
                    }
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false, {id: "uploadDocument"});
                },
                error: function (e) {
                    if (e && e.operation == "upload" && e.XMLHttpRequest) {
                        expresso.Common.displayServerValidationMessage(e.XMLHttpRequest);
                    }
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false, {id: "uploadDocument"});
                }
            }, options);

            return $input.kendoUpload(kendoUploadOptions).data("kendoUpload");
        };

        // return public properties and methods
        return {
            initializeForm: initializeForm,
            resetForm: resetForm,
            bindOnChange: bindOnChange,
            triggerChange: triggerChange,
            setFormReadOnly: setFormReadOnly,
            setFieldReadOnly: setFieldReadOnly,
            hideField: hideField,
            updateLabel: updateLabel,
            highlightMissingRequiredField: highlightMissingRequiredField, // @deprecated
            highlightField: highlightField,
            displayMissingRequiredFieldNotification: displayMissingRequiredFieldNotification,

            buildDropDownList: buildDropDownList,       // local (array or url) and single select
            buildComboBox: buildComboBox,              // remote and single select
            buildMultiSelect: buildMultiSelect,         // local (array or url)/remote and multiple select
            buildDropDownTree: buildDropDownTree,       // local (array or url) and single select
            buildLookupSelection: buildLookupSelection, // remote multiple select lookup
            addDataItemToWidget: addDataItemToWidget,
            setDataSource: setDataSource,

            buildTreeView: buildTreeView,
            buildCheckBox: buildCheckBox,
            buildRadioButton: buildRadioButton,
            buildUpload: buildUpload,
            buildNewUpload: buildNewUpload,
            buildInlineGrid: buildInlineGrid,
            getDocumentUploadCustomData: getDocumentUploadCustomData,

            getKendoWidget: getKendoWidget,
            destroyKendoWidgets: destroyKendoWidgets,
            getFieldForInput: getFieldForInput,

            buildWindow: buildWindow,
            setWindowDimension: setWindowDimension,
            buildMessageWindow: buildMessageWindow,
            buildPromptWindow: buildPromptWindow,
            buildCommentWindow: buildCommentWindow,
            buildYesNoWindow: buildYesNoWindow,
            buildSearchWindow: buildSearchWindow,
            addSearchButton: addSearchButton,
            getFormWidth: getFormWidth,
            getEmSize: getEmSize,

            buildReportSelector: buildReportSelector,
            showNotification: showNotification,
            showLoadingMask: showLoadingMask,

            showMaximizedPicture: showMaximizedPicture
        };
    }()
);

