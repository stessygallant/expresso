var expresso = expresso || {};

/**
 * This is a utility module. It contains some utility methods.
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.Common = (function () {
    var HTTP_CODES = {
        OK: 200, // Response to a successful request.
        CREATED: 201, // Response to a POST that results in a creation.
        NO_CONTENT: 204, // Response to a successful request that won't be returning a body.
        RESET_CONTENT: 205, //

        NOT_MODIFIED: 304, // Used when HTTP caching headers are in play.

        BAD_REQUEST: 400, // The request is malformed.
        UNAUTHORIZED: 401, // When no or invalid authentication details are provided.
        FORBIDDEN: 403, // When authenticated user doesn't have access to the resource.
        NOT_FOUND: 404, // When a non-existent resource is requested.
        CONFLICT: 409, //
        PRECONDITION_FAILED: 412, // When the version does not match

        UNPROCESSABLE_ENTITY: 422, // Used for validation errors.
        LOCKED: 423, // Used for locked account
        EMAIL_TOKEN_REQUIRED: 424, // Failed Dependency
        UPGRADE_REQUIRED: 426, // Web service versions has been updated
        PASSWORD_EXPIRED: 460, // Password is expired
        CUSTOM_UNAUTHORIZED: 451, // Expresso Custom (to avoid browser to pop up the basic auth window)
        SERVER_ERROR: 500, // When an exception is thrown while processing the request
        BAD_GATEWAY: 502, // this happens when uploading a document when the session is expired
        SERVICE_UNAVAILABLE: 503 // Service is unavailable
    };

    var SCREEN_MODES = {
        DESKTOP: "desktop",
        TABLET: "tablet",
        PHONE: "phone"
    };

    var language = undefined;
    var siteName = undefined;
    var siteNamespace = undefined;
    var serverEnv = undefined;
    var screenMode = undefined;
    var fontRatio = undefined;

    var doNotDisplayAjaxErrorMessageFlag = false;
    var reloading = false;
    var authenticationPath = undefined;

    // reference the current labels used by the current application
    var currentRequestLabels = undefined;

    // script cache
    var scriptsCache = {};

    // application cache
    var applicationsCache = {};

    /**
     * Helper method to create a filter
     * @param criteria  list of criteria for the filter (object or array)
     * @param [options] custom options for the Kendo Query
     * @param [returnObject] return an object instead of a string (default is false)
     * @returns {*} a Kendo UI compatible filter
     */
    var buildKendoFilter = function (criteria, options, returnObject) {
        var kendoQuery = {
            sort: undefined,
            activeOnly: undefined,
            countOnly: undefined,
            hierarchical: undefined,
            pageSize: undefined,
            skip: undefined,
            filter: {
                logic: "and",
                filters: []
            }
        };

        // set the user options
        $.extend(true, kendoQuery, options || {});

        // if it is a function, execute it
        if (typeof criteria === "function") {
            criteria = criteria();
        }

        // avoid null issue
        criteria = criteria || [];

        // if we receive a complete object filter
        if (criteria.filters) {
            kendoQuery.filter.logic = criteria.logic || kendoQuery.filter.logic;
            kendoQuery.activeOnly = (criteria.activeOnly !== undefined ? criteria.activeOnly : kendoQuery.activeOnly);
            kendoQuery.countOnly = (criteria.countOnly !== undefined ? criteria.countOnly : kendoQuery.countOnly);

            criteria = criteria.filters;
        }

        // if it is not an array, convert it to an array of 1 element
        if (!$.isArray(criteria)) {
            criteria = [criteria];
        }

        for (var i = 0, l = criteria.length; i < l; i++) {
            var criterion = criteria[i];

            // the object could be a simple {key:value, key2:value2, ...}, or it could be {operator: o, field: f, value: v}
            if (!criterion) {
                // null, just skip it
            } else if (criterion.field || criterion.filters) {
                kendoQuery.filter.filters.push(criterion);
            } else {
                for (var p in criterion) {
                    if (p != "countOnly" && p != "activeOnly" && p != "hierarchical") {
                        kendoQuery.filter.filters.push({field: p, operator: "eq", value: criterion[p]});
                    } else {
                        // set them at the query level
                        if (criterion[p] !== undefined) {
                            kendoQuery[p] = criterion[p];
                        }
                    }
                }
            }
        }
        if (returnObject) {
            return {query: JSON.stringify(kendoQuery)};
        } else {
            return "query=" + encodeURIComponent(JSON.stringify(kendoQuery));
        }
    };


    /**
     * Utility method to add a filter to the
     * @param filter the filter parent
     * @param f the new filter to be added to filter
     * @param [customFilterFlag] flag to indicate if the filter is custom (master, etc). Default is false
     */
    var addKendoFilter = function (filter, f, customFilterFlag) {
        if (f) {
            // make sure filter is a complete filter
            if ($.isArray(filter)) {
                filter = {logic: "and", filters: filter};
            } else if (!filter.filters) {
                filter = {logic: "and", filters: []};
            }

            // if it is a function, execute it
            if (typeof f === "function") {
                f = f.call(this.resourceManager);
            }

            if (f.filters) {
                if (filter.logic == "or" || (f.logic == "or" || (f.field && f.logic))) {
                    // add a complete filter object
                    filter.filters.push(f);
                } else {
                    // only add the filters
                    filter.filters.push.apply(filter.filters, f.filters);
                }
            } else if (f.field !== undefined) {
                // add a simple filter
                filter.filters.push(f);
            } else if ($.isArray(f)) {
                // add all filters in the array to the filter array
                for (var i = 0; i < f.length; i++) {
                    filter = expresso.Common.addKendoFilter(filter, f[i]);
                }
            } else {
                // simple {field:value}
                for (var o in f) {
                    filter.filters.push({field: o, operator: "eq", value: f[o]});
                }
            }
        }

        if (customFilterFlag) {
            setCustomFilterFlag(f);
        }
        return filter;
    };

    /**
     *
     * @param filter
     */
    var setCustomFilterFlag = function (filter) {
        if (filter) {
            if (filter.field) {
                filter.customFilterFlag = true;
            } else if (filter.filters) {
                for (var i = 0; i < filter.filters.length; i++) {
                    setCustomFilterFlag(filter.filters[i]);
                }
            }
        }
    };

    /**
     * Remove a filter from the list of filters
     * @param filter
     * @param field
     */
    var removeKendoFilter = function (filter, field) {
        if (filter.filters) {
            filter.filters = $.grep(filter.filters, function (f) {
                expresso.Common.removeKendoFilter(f, field);
                if (f.filters) {
                    return f.filters.length;
                } else {
                    return f.field != field;
                }
            });
        } else if ($.isArray(filter)) {
            for (var i = filter.length - 1; i >= 0; i--) {
                if (filter[i].field == field) {
                    filter.splice(i, 1);
                } else {
                    expresso.Common.removeKendoFilter(filter[i], field);
                }
            }
        }
    };


    /**
     * Executed before the server response is used. Use it to preprocess or parse each item.
     *
     * @param item  item (row) to be preprocessed if needed
     * @param model
     * @param objectsNeededForColumns
     * @returns {*} return the item (modified)
     */
    var parseResponseItem = function (item, model, objectsNeededForColumns) {

        // for each field ending with Id, we need to create an object as KendoUI will throw an exception
        // if the object is not defined
        item.derived = item.derived || {};

        var fields = model.fields;
        for (var f in fields) {
            if (f) {
                var parentItem = item;
                var fieldName = f;
                if (f.indexOf(".") != -1) {
                    // get to the bottom object
                    var props = f.split(".");
                    for (var i = 0; i < props.length - 1; i++) {
                        if (!parentItem[props[i]]) {
                            parentItem[props[i]] = {};
                        }
                        parentItem = parentItem[props[i]];
                    }
                    fieldName = props[props.length - 1];
                }

                // avoid undefined (Kendo Grid does not support undefined attribute)
                if (parentItem[fieldName] === undefined) {
                    parentItem[fieldName] = null;
                }

                if (fieldName.endsWith("Id")) {
                    // for each Id, add a default object if not defined
                    var objectName = fieldName.substring(0, fieldName.length - 2);
                    parentItem[objectName] = parentItem[objectName] || {};
                } else if (fields[f].type == "date" || f == "date" || (
                    !fields[f].type &&
                    (f.endsWith("Date") || f.endsWith("DateTime") || f.endsWith("Timestamp")))) {
                    parentItem[fieldName] = expresso.util.Formatter.parseDateTime(parentItem[fieldName]);
                }
            }
        }

        // for each column, if there is a reference to an object, make sure the object exists to avoid null issue
        if (objectsNeededForColumns) {
            $.extend(true, item, objectsNeededForColumns);
        }

        return item;
    };

    /**
     * remove from the resource all fields that should not be saved
     * @param resource
     * @param [model]
     * @return {{}} the purged resource
     */
    var purgeResource = function (resource, model) {
        var props = {};
        var f, value, valueType;

        if (model) {
            // Purge the resource and retain only attributes that are in the app_class
            for (f in model.fields) {
                if (model.fields.hasOwnProperty(f)) {
                    var field = model.fields[f];
                    if (field) {
                        // make sure you can save it and the field does not contain dot
                        if (field.transient !== true && f.indexOf(".") == -1) {
                            value = resource[f];
                            valueType = $.type(value);

                            // only primitive type and array can be saved
                            if (valueType === "boolean" || valueType === "number" || valueType === "string" ||
                                valueType === "date" || valueType === "array" || valueType === "null") {
                                //console.log("Adding [" + f + "] to message");
                                if (value !== undefined) {
                                    // always trim string
                                    if (valueType === "string" && value && value.trim && field.trim !== false) {
                                        value = value.trim();
                                    }
                                    props[f] = value;
                                } else {
                                    props[f] = field.defaultValue;
                                }

                                // prevent sending 0 for Id
                                if (f.endsWith("Id") && !props[f]) {
                                    props[f] = null;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (f in resource) {
                value = resource[f];
                valueType = $.type(value);

                // only primitive type and array can be saved
                if (value !== undefined && valueType === "boolean" || valueType === "number" || valueType === "string" ||
                    valueType === "date" || valueType === "array" || valueType === "null") {

                    // remove any field that contains a dot
                    if (f.indexOf(".") != -1) {
                        continue;
                    }

                    // remove KendoUI attributes
                    if (f === "dirty" || f === "idField" || f === "_defaultId" || f === "uid") {
                        continue;
                    }

                    //console.log("Adding [" + f + "] to message");
                    props[f] = value;

                    // prevent sending 0 for Id
                    if (f.endsWith("Id") && !props[f]) {
                        props[f] = null;
                    }
                }
            }
        }
        return props;
    };

    /**
     * Method to configure jQuery Ajax calls globally
     */
    var initAjax = function () {

        // handle any AJAX issues
        $(document).ajaxError(function (event, jqxhr, settings, exception) {
                // console.log("jqxhr.alreadyProcessed: " + jqxhr.alreadyProcessed + "doNotDisplayAjaxErrorMessageFlag: "
                //     + doNotDisplayAjaxErrorMessageFlag + " " + JSON.stringify(jqxhr));
                if (!jqxhr.alreadyProcessed) {

                    if (doNotDisplayAjaxErrorMessageFlag) {
                        if (jqxhr.status == HTTP_CODES.UPGRADE_REQUIRED ||
                            jqxhr.status == HTTP_CODES.UNAUTHORIZED ||
                            jqxhr.status == HTTP_CODES.CUSTOM_UNAUTHORIZED) {
                            window.location.reload();
                        }
                    } else {
                        if (expresso.Common.getSiteNamespace().config.Configurations.customErrorMessage && expresso.Common.getSiteNamespace().config.Configurations.customErrorMessage.includes(jqxhr.status)) {
                            //Custom error handling
                            displayServerValidationMessage(jqxhr);
                        } else if (jqxhr.status == HTTP_CODES.UNAUTHORIZED || jqxhr.status == HTTP_CODES.CUSTOM_UNAUTHORIZED) {
                            // 401 Unauthorized - When no or invalid authentication details are provided
                            if (expresso.Common.getSiteNamespace().config.Configurations.authenticationFailureRedirect) {
                                window.location.href = expresso.Common.getSiteNamespace().config.Configurations.authenticationFailureRedirect; // || "/";
                            } else {
                                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("expiredSession")).done(function () {
                                    window.location.reload();
                                });
                            }
                        } else if (jqxhr.status == HTTP_CODES.FORBIDDEN) {
                            // 403 Forbidden - When authenticated user doesn't have access to the resource.
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("userUnauthorized"));
                        } else if (jqxhr.status == HTTP_CODES.BAD_REQUEST) {
                            // 400 Bad Request - The request is malformed.
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("invalidRequest"));
                        } else if (jqxhr.status == HTTP_CODES.NOT_FOUND) {
                            // 404 Not Found - When a non-existent resource is requested.
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("resourceUnavailable"));
                        } else if (jqxhr.status == HTTP_CODES.PRECONDITION_FAILED) {
                            // 412 Precondition Failed - When the rpcVersion does not match
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("wrongEntityVersion", currentRequestLabels));
                        } else if (jqxhr.status == HTTP_CODES.LOCKED) {
                            // 423 Locked - When the account is locked
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("accountBlocked"));
                        } else if (jqxhr.status == HTTP_CODES.SERVICE_UNAVAILABLE) {
                            // 503 Maintenance
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("serviceUnavailable"));
                        } else if (jqxhr.status == HTTP_CODES.PASSWORD_EXPIRED) {
                            // 460 Password expired
                            // do nothing. The framework will handle it
                        } else if (jqxhr.status == HTTP_CODES.UPGRADE_REQUIRED) {
                            // 426 Upgrade Required - Must reload the client

                            // force reload
                            if (!reloading) {
                                reloading = true;
                                expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("newApplicationVersion")).done(function () {
                                    window.location.reload();
                                });
                            }
                        } else if (jqxhr.status >= 400 && jqxhr.status < 500 || jqxhr.status == HTTP_CODES.UNPROCESSABLE_ENTITY) {
                            // 422 Unprocessable Entity - Used for validation errors.
                            displayServerValidationMessage(jqxhr);
                        } else {
                            // 500 Server error – When an exception is thrown while processing the request
                            console.error("Caught an error from XHR: " + JSON.stringify(jqxhr), exception);
                            // support team already got an email for this
                            expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("applicationProblem"));
                        }
                    }
                }
            }
        );
    };

    /**
     * Display a validation message from the server
     * @param jqxhr
     */
    var displayServerValidationMessage = function (jqxhr) {
        var params, description;
        try {
            // it should be a JSON response
            var validationMessage = JSON.parse(jqxhr.responseText);
            description = validationMessage.description;
            params = validationMessage.params;
        } catch (err) {
            // if not, keep the text directly
            description = jqxhr.responseText;
        }

        // try to get the label (if not, keep the current text for backward compatibility)
        description = expresso.Common.getLabel(description, currentRequestLabels, params, true) || description;
        return expresso.util.UIUtil.buildMessageWindow(description, {type: "error"});
    };

    /**
     *
     * @param url
     * @param [action]
     * @param [format]
     */
    var sendDownloadRequest = function (url, action, format) {
        action = action || "download";
        format = format || "pdf";

        url += (url.indexOf("?") == -1 ? "?" : "&") + "action=" + action;

        // help the browser to guess the document format
        url += "&_=" + new Date().getTime() + "&_format=." + format;

        // Send impersonate if provided
        if (getImpersonateUser()) {
            url += "&impersonate=" + getImpersonateUser();
        }

        console.log("Download [" + url + "]");
        var $href = $("<a href='" + url + "' target='_blank' hidden></a>");
        $href.appendTo("body")[0].click();
        $href.remove();
    };

    /**
     * Execute the report
     *
     * @param report
     * @param resourceName
     * @param params
     * @param customParamPage
     */
    var executeReport = function (report, resourceName, params, customParamPage) {
        // update Google Analytics
        expresso.Common.sendAnalytics({hitType: "pageview", page: report.name, params: params});

        // redirect this method to the Main application
        if (expresso.Main) {
            expresso.Main.executeReport(report, resourceName, params, customParamPage);
        } else {
            siteNamespace.Main.executeReport(report, resourceName, params, customParamPage);
        }
    };

    /**
     * Refer to jQuery getScript.
     * Add a version to the URL for caching
     * @param path
     * @param [cache] by default, false (do not cache)
     * @returns {*} a Ajax promise
     */
    var getScript = function (path, cache) {
        path += (path.indexOf("?") != -1 ? "&" : "?") + "ver=" + siteNamespace.config.Configurations.version;

        if (cache === true && scriptsCache[path]) {
            // do not load the same script multiple times
            return $.Deferred().resolve();
        } else {
            var options = {
                dataType: "script",
                cache: true,
                url: path
            };

            // Use $.ajax() since it is more flexible than $.getScript
            // Return the jqXHR object, so we can chain callbacks
            return $.ajax(options)
                .done(function () {
                    if (cache === true) {
                        scriptsCache[path] = true;
                    }
                })
                .fail(function (jqxhr) {
                    if (path.indexOf("labels") != -1) {
                        // if a labels file does not exist, it is not a problem: we will load the default one
                        jqxhr.alreadyProcessed = true;
                    } else {
                        // this is usually a development issue
                        console.error("Error loading script [" + path + "]", jqxhr);
                    }
                });
        }
    };

    /**
     *
     * @param $div
     * @param labels
     */
    var localizePage = function ($div, labels) {
        if (labels) {
            // find all  data-text-key="key" and replace the inner text
            $div.find("[data-text-key]").each(function () {
                var $el = $(this);
                var key = $el.data("text-key");
                var text = getLabel(key, labels);
                $el.text(text);
            });

            // replace title
            $div.find("[title]").each(function () {
                var $el = $(this);
                var key = $el.attr("title");

                if (key) {
                    var text = getLabel(key, labels, null, true);
                    if (text !== undefined) {
                        $el.attr("title", text);
                    }
                }
            });

            // replace placeholder
            $div.find("[placeholder]").each(function () {
                var $el = $(this);
                var key = $el.attr("placeholder");
                if (key) {
                    var text = getLabel(key, labels, null, true);
                    if (text !== undefined) {
                        $el.attr("placeholder", text);
                    }
                }
            });

            // replace label text
            $div.find("label").each(function () {
                var $label = $(this);
                if (!$label.text()) {
                    var key = $label.data("input-name");
                    if (key) {
                        // get the label text
                        var labelText = getLabel(key, labels);
                        $label.text(labelText);

                        // if the label has a text for the title, use it
                        if (!$label.attr("title") && getLabel(key + "Help", labels, null, true)) {
                            $label.attr("title", getLabel(key + "Help", labels));
                            $label.addClass("help");
                        }
                    }
                }
            });

            // localize button
            $div.find("[type=button],[type=submit]").each(function () {
                var $button = $(this);
                var key = $button.attr("name");
                if (key) {
                    // get the label text
                    var labelText = getLabel(key, labels);
                    $button.text(labelText);
                }
            });
        }
    };

    /**
     *
     * @param sm  Possibilities: [desktop|tablet|phone]
     */
    var setScreenMode = function (sm) {
        screenMode = sm;

        $("body")
            .removeClass("exp-screen-mode-mobile")
            .removeClass("exp-screen-mode-desktop")
            .removeClass("exp-screen-mode-tablet")
            .removeClass("exp-screen-mode-phone")
            .addClass("exp-screen-mode-" + screenMode);

        // reset the font ratio
        fontRatio = undefined;
        getFontRatio();
    };

    /**
     * Get the language
     * Possibitities: [desktop|tablet|phone]
     * @return {string}
     */
    var getScreenMode = function () {
        if (screenMode == undefined) {
            var $window = $(window);
            // detect screen mode [desktop|tablet|phone]
            var sm = expresso.Common.SCREEN_MODES.DESKTOP;
            if (expresso.util.Util.getUrlParameter("screenMode")) {
                sm = expresso.util.Util.getUrlParameter("screenMode");
            } else {
                if ($("html").hasClass("k-mobile")) {
                    if ($window.width() < 500 || $window.height() < 500) {
                        sm = expresso.Common.SCREEN_MODES.PHONE;
                    } else {
                        sm = expresso.Common.SCREEN_MODES.TABLET;
                    }
                }
            }
            // expresso.util.UIUtil.buildMessageWindow($window.width() + ":" + $window.height() + ":" +
            //     $("html").hasClass("k-mobile") + ":" + sm + ($window.width() < 500) + ":" + ($window.height() < 500));
            // console.log($window.width() + "X" + $window.height() + " - " + sm + " (" + navigator.userAgent + ")");
            setScreenMode(sm);
        }
        return screenMode;
    };

    /**
     * Get the current font size ratio compared to the default 12px
     */
    var getFontRatio = function () {
        if (fontRatio === undefined) {
            var $span = $("<span>_</span>").appendTo($("body"));
            var h = $span.height();
            var fontSize = h - 2;
            fontRatio = fontSize / 12;
            // console.log("Default font size: " + fontSize + "  ratio: " + fontRatio);
            $span.remove();
        }
        return fontRatio;
    };

    /**
     * Get the language
     */
    var getLanguage = function () {
        return language;
    };

    /**
     * @param [lang]
     * @return {*} promises when the labels are loaded
     */
    var setLanguage = function (lang) {
        if (!language || lang != language) {
            lang = lang ||
                expresso.util.Util.getUrlParameter("lang") ||
                expresso.util.Util.getFirstBrowserLanguage();

            var supportedLanguages = expresso.Common.getSiteNamespace().config.Configurations.supportedLanguages || ["en", "fr"];

            if ($.inArray(lang, supportedLanguages) == -1) {
                lang = expresso.Common.getSiteNamespace().config.Configurations.defaultLanguage;
            }

            // set the language
            language = lang;
            console.log("Using language [" + language + "]");

            var locale = lang + "-CA";
            window.locale = locale;

            // load labels
            return $.when(
                // load the framework common labels
                loadLabels("expresso"),

                // load the menu labels
                loadLabels("config", "menu-labels"),

                // load the application common labels
                loadLabels("config"),

                // load Kendo UI messages
                getScript(kendoUIPath + "/js/messages/kendo.messages." + locale + ".min.js"),

                // load Kendo UI culture
                getScript(kendoUIPath + "/js/cultures/kendo.culture." + locale + ".min.js")
            ).done(function () {
                // extend the common labels with the site labels
                var siteLabels = eval(expresso.Common.getSiteName() + ".config.Labels");
                $.extend(expresso.Labels, siteLabels);

                console.log("Setting Kendo culture [" + window.locale + "]");
                kendo.culture(locale);
            });
        }
    };

    /**
     *
     * @param app application name or application definition
     * @returns {string}
     */
    var getApplicationPath = function (app) {
        var appPath = null;
        app = getApplication(app);
        if (app && app.appClass) {
            if (app.appClass.startsWith("expresso")) {
                // remove only the application name (last)
                appPath = app.appClass.split(".").slice(0, -1).join('/');
            } else {
                // remove common (first namespace) and the application name (last)
                appPath = app.appClass.split(".").slice(1, -1).join('/');
            }
        }
        return appPath;
    };

    /**
     *
     * @param appNameOrPath application name, application definition or application path
     * @returns {string}
     */
    var getApplicationNamespace = function (appNameOrPath) {
        var appNamespace = appNameOrPath;
        if (appNameOrPath) {
            if (typeof appNameOrPath == "string" && appNameOrPath.indexOf('/') != -1) {
                // if there is the filename, remove it
                if (appNameOrPath.indexOf('.') != -1) {
                    appNameOrPath = appNameOrPath.substring(0, appNameOrPath.lastIndexOf('/'));
                }
                // app is the path
                appNamespace = appNameOrPath.replace(/\//g, '.');
                if (!appNamespace.startsWith("expresso")) {
                    // add the name of the site
                    appNamespace = expresso.Common.getSiteName() + "." + appNamespace;
                }
            } else {
                // get the definition of the app
                var app = getApplication(appNameOrPath);
                if (app && app.appClass) {
                    appNamespace = app.appClass;
                    // remove the application name
                    appNamespace = appNamespace.substring(0, appNamespace.lastIndexOf('.'));
                }
            }
        }
        return appNamespace;
    };

    /**
     *
     * @param appNamespace application namespace
     */
    var createApplicationNamespace = function (appNamespace) {
        if (appNamespace) {
            var subNamespaces = appNamespace.split("\.");
            var namespace = window;
            subNamespaces.forEach(function (ns) {
                // console.log("Creating [" + ns + "]");
                namespace[ns] = namespace[ns] || {};
                namespace = namespace[ns];
            });
        }
    };

    /**
     *
     * @param path
     * @param [filename] by default, labels
     * @param [lang]
     * @param [stopIfFail]
     * @return {*}
     */
    var loadLabels = function (path, filename, lang, stopIfFail) {
        path = path || "";
        filename = filename || "labels";
        if (lang === undefined) {
            lang = language;
        }
        var $deferred = $.Deferred();

        // if there is a default file for language and this is the default language, do not use
        // lang in the label file path
        if (lang == expresso.Common.getSiteNamespace().config.Configurations.defaultLanguage &&
            expresso.Common.getSiteNamespace().config.Configurations.defaultFileLanguage) {
            lang = null;
        }

        var labelFullFileName = path + "/" + filename + (lang ? "_" + lang : "") + ".js";
        // console.log("Loading label file [" + labelFullFileName + "]");

        // before evaluating, make sure the namespace already exists
        var namespace = getApplicationNamespace(path);
        createApplicationNamespace(namespace);

        // get the label.js file
        getScript(labelFullFileName).done(function () {
            try {
                var labels = namespace + ".Labels";
                // console.log("Labels [" + labels + "]");
                labels = eval(labels);
            } catch (e) {
                console.error("Errors parsing labels [" + labels + "]");
            }
            $deferred.resolve(labels);
        }).fail(function () {
            if (stopIfFail) {
                $deferred.reject();
            } else {
                // if it cannot find the language file, get the default one
                loadLabels(path, filename, null, true).done(function (labels) {
                    $deferred.resolve(labels);
                }).fail(function (jqxhr) {
                    console.warn("Cannot load labels [" + labelFullFileName + "]", jqxhr);
                    $deferred.reject();
                });
            }
        });

        return $deferred;
    };

    /**
     * Load an HTML page into a DIV
     * Refer to jQuery load.
     * Add a version to the URL for caching
     * @param $div
     * @param path
     * @param [labels] labels to be used for localization
     * @param [enhanceWidgets] tru by default
     * @returns {*} a Ajax promise
     */
    var loadHTML = function ($div, path, labels, enhanceWidgets) {
        path += (path.indexOf("?") != -1 ? "&" : "?") + "ver=" + siteNamespace.config.Configurations.version;

        var $deferred = $.Deferred();
        $div.load(path, function () {

            if (enhanceWidgets !== false) {
                // convert to a custom form
                $div.kendoExpressoForm({labels: labels || expresso.Labels}).data("kendoExpressoForm").ready().done(function () {
                    // then return the $div to the promise
                    $deferred.resolve($div);
                });
            } else {
                // then return the $div to the promise
                $deferred.resolve($div);
            }
        });
        return $deferred;
    };

    var setAuthenticationPath = function (path) {
        //console.log("authenticationPath=" + path);
        authenticationPath = path;
    };

    var getAuthenticationPath = function () {
        return authenticationPath;
    };

    /**
     * Set the current labels for the Ajax request
     * @param labels
     */
    var setCurrentRequestLabels = function (labels) {
        currentRequestLabels = labels;
    };

    /**
     * Send a REST request to the server. But first, verify that the user is allowed to send this request
     * @param path resource path
     * @param [action] action on the resource (CRUD or custom)
     * @param [data] data to be sent in the body
     * @param [queryString]
     * @param [options]
     * @returns {*} a Ajax promise
     */
    var sendRequest = function (path, action, data, queryString, options) {

        // avoid null issue
        options = options || {};

        // set default labels
        options.labels = options.labels || expresso.Labels;

        // when displaying a progress, do not show the spinner
        if (options.showProgress) {
            options.waitOnElement = null;

            if (options.showProgress === true) {
                options.showProgress = $("body");
            }

            // add flag to the query
            queryString = queryString || {};
            if (typeof queryString === "string") {
                queryString += "&useProgressSender=true";
            } else {
                queryString["useProgressSender"] = true;
            }
        }

        // do not display the spinner when we ignore the errors (usually full screen mode)
        if (doNotDisplayAjaxErrorMessageFlag || options.ignoreErrors) {
            options.waitOnElement = null;
        }

        // display a wait progress bar
        var $domElement = options.waitOnElement;
        if ($domElement === undefined) {
            $domElement = $("body");
        }

        if ($domElement) {
            // console.log("Send request loading mask [" + path + "]");
            expresso.util.UIUtil.showLoadingMask($domElement, true);
        }

        if (!action) {
            action = "read";
        }

        // convert the merge action
        if (action == "merge") {
            if (data && data.id) {
                action = "update";
                path += "/" + data.id;
            } else if (data) {
                action = "create";
            }
        }

        if (queryString && (typeof queryString !== "string")) {
            // try to convert the object to string
            queryString = $.param(queryString);
        }

        var method = options.method;
        if (!method) {
            switch (action) {
                case "create":
                    method = "POST";
                    break;
                case "read":
                    method = "GET";
                    break;
                case "update":
                    method = "PUT";
                    break;
                case "delete":
                    method = "DELETE";
                    break;
                default:
                    method = "POST";
                    if (action !== "upload") {
                        queryString = (queryString ? queryString + "&" : "") + "action=" + action;
                    }
                    break;
            }
        }

        // ty to determine the content type based on the action and the data
        var contentType = options.contentType;
        if (!contentType) {
            var body;
            switch (action) {
                case "read":
                case "delete":
                    // no content
                    break;
                case "create":
                case "update":
                    if (data && $.isPlainObject(data)) {
                        contentType = "application/json; charset=utf-8";
                        body = JSON.stringify(data);
                    } else {
                        contentType = "application/x-www-form-urlencoded; charset=utf-8";
                        body = data;
                    }
                    break;
                default:
                    contentType = "application/x-www-form-urlencoded; charset=utf-8";
                    body = data;
                    break;
            }
        } else {
            if (contentType.indexOf("json") != -1) {
                body = JSON.stringify(data);
            } else {
                body = data;
            }
        }

        // build the URL
        var url;
        if (options.public) {
            //console.log("OK");
            url = getWsPublicPathURL();
        } else if (action === "upload") {
            action = "create";
            url = getWsUploadPathURL();
        } else {
            url = getWsResourcePathURL();
        }
        if (path.startsWith("http") || path.startsWith("//") || path.startsWith("mailto")) {
            options.skipUserAllowed = true;
            url = path;
        } else {
            url += (path.startsWith("/") ? path : "/" + path);
        }

        // WARNING
        // queryString may have been encoded by $.param which uses encodeURIComponent
        // if we encode the URI using encodeURI, it will double encode the %
        // if the user is manually creating the queryString, it has the responsibility to encode it
        // console.log("queryString: [" + queryString + "]");

        url += (queryString ? (url.indexOf("?") == -1 ? "?" : "&") + queryString : "");
        // console.log(method + " URL: [" + url + "]  contentType [" + contentType + "]");

        var $kendoProgressBarDiv;
        var kendoProgressBar;
        var kendoProgressBarText = {text: expresso.Common.getLabel("initialization", options.labels)};
        if (options.showProgress) {
            $kendoProgressBarDiv = $("<div class='exp-progress-bar-div'><div class='progress-bar'></div><div class='progress-back'></div></div>").appendTo(options.showProgress);
            kendoProgressBar = $kendoProgressBarDiv.find(".progress-bar").kendoProgressBar({
                type: "percent",
                animation: {
                    duration: 100
                },
                change: function () {
                    this.progressStatus.text(kendoProgressBarText.text);
                }
            }).data("kendoProgressBar");
        }

        // if the user is not allowed, reject it and display an error message
        var resourceName = null;
        var $deferredLogin = $.Deferred();
        if (options.public || options.skipUserAllowed) {
            $deferredLogin.resolve();
        } else {
            resourceName = "";
            var s = path.split("/");
            for (var i = 0; i < s.length /* && i < 3 */; i += 2) {
                if (i != 0) {
                    resourceName += "/";
                }
                resourceName += s[i];
            }
            //console.log("********* [" + path + "] [" + resourceName + "]");

            if (isUserAllowed(resourceName, action, false)) {
                $deferredLogin.resolve();
            } else {
                console.warn("User not allowed: resourceName[" + resourceName + "] action[" + action + "]");
                if (options.ignoreErrors) {
                    // ok
                } else {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("missingPrivileges", options.labels));
                }
                $deferredLogin.reject();
            }
        }

        var progressFailure;
        var $deferredComplete = $.Deferred();
        $deferredLogin.done(function () {
            var $deferred = $.ajax(url, {
                method: method,
                contentType: contentType,
                data: body,
                xhr: function () {
                    // get the native XmlHttpRequest object
                    var xhr = $.ajaxSettings.xhr();

                    if (options.showProgress) {
                        // set the onprogress event handler
                        xhr.onprogress = function () {
                            try {
                                var s = xhr.responseText;

                                // if multiple responses has been sent, take only the last one
                                s = s.substring(s.lastIndexOf('{')).trim();
                                s = JSON.parse(s);

                                // then set the value and the text
                                if (s.errorMessage) {
                                    progressFailure = expresso.Common.getLabel(s.errorMessage,
                                        options.labels, {params: s.params},
                                        true) || s.errorMessage;
                                } else {
                                    kendoProgressBar.value(s.progress);
                                    kendoProgressBarText.text = expresso.Common.getLabel(s.message,
                                        options.labels, {params: s.params},
                                        true) || s.message;
                                }
                            } catch (e) {
                                // ignore
                            }
                        };

                        //Download progress
                        // xhr.addEventListener("progress", function (evt) {
                        //     console.log("****************3");
                        //     //options.progress(xhr.responseText);
                        // }, false);
                    }
                    // return the customized object
                    return xhr;
                }
            }).fail(function (jqXHR) {
                // do not process the error and do not display the alert box
                if (options.ignoreErrors /*&& jqXHR.status != HTTP_CODES.UNAUTHORIZED  we cannot do it */) {
                    jqXHR.alreadyProcessed = true;
                } else {
                    // if there is labels in the options, use it in AjaxSetup
                    currentRequestLabels = options.labels;
                }
            });

            // remove the progress bar when request is completed (success or fail)
            $deferred
                .done(function (result) {
                    expresso.util.UIUtil.showLoadingMask($domElement, false);
                    if (options.showProgress) {
                        if (progressFailure) {
                            expresso.util.UIUtil.buildMessageWindow(progressFailure, {type: "error"});
                        } else {
                            kendoProgressBarText.text = expresso.Common.getLabel("requestSuccess", options.labels);
                        }
                    }
                    $deferredComplete.resolve(result);
                })
                .fail(function (jqXHR) {
                    expresso.util.UIUtil.showLoadingMask($domElement, false);
                    if (options.showProgress) {
                        kendoProgressBarText.text = expresso.Common.getLabel("requestFailure", options.labels);
                    }
                    $deferredComplete.reject(jqXHR);
                })
                .always(function () {

                    // DO NOT hide the loading mask here otherwise the if the application
                    // call a sendRequest in the .done(), the second loading mask will not appear

                    if (options.showProgress) {
                        // make sure to trigger the change event
                        kendoProgressBar.value(99.9);
                        kendoProgressBar.value(100);
                        window.setTimeout(function () {
                            kendoProgressBar.destroy();
                            kendoProgressBar = null;
                            $kendoProgressBarDiv.remove();
                            $kendoProgressBarDiv = null;
                            kendoProgressBarText = null;
                        }, 2000);
                    }

                    // update Google Analytics
                    if (action && action != "read") {
                        var p = resourceName || path;
                        var gaFields = {
                            hitType: 'event',
                            eventCategory: p,
                            eventAction: action,
                            eventLabel: action + " " + p
                        };
                        expresso.Common.sendAnalytics(gaFields);
                    }
                });

        }).fail(function () {
            expresso.util.UIUtil.showLoadingMask($domElement, false);
        });

        return $deferredComplete;
    };

    /**
     *
     * @param gaFields
     */
    var googleAnalyticsInitialized = false;
    var sendAnalytics = function (gaFields) {

        // send only if needed
        if (!expresso.util.Util.getUrlParameter("publicUser")) {

            // update Google Analytics
            if (expresso.Common.getSiteNamespace().config.Configurations.googleAnalyticsTrackingNumber) {
                if (expresso.Common.isProduction()) {
                    try {
                        if (!googleAnalyticsInitialized) {
                            googleAnalyticsInitialized = true;
                            ga('create', {
                                trackingId: expresso.Common.getSiteNamespace().config.Configurations.googleAnalyticsTrackingNumber,
                                cookieDomain: 'auto',
                                name: 't0', // default tracker name
                                userId: expresso.Common.getUserInfo().id
                            });
                        }

                        // always set the page for the subsequent event hit
                        if (gaFields.hitType == "pageview") {
                            ga('set', 'page', gaFields.page);
                        }

                        //console.log("Sending analytics to Google", gaFields);
                        delete gaFields["params"]; // do not send params to Google
                        ga('send', gaFields);
                    } catch (e) {
                        console.warn("Cannot send Google Analytics");
                    }
                }
            } else if (expresso.Common.getSiteNamespace().config.Configurations.analyticsResourceService) {
                // post the data to the service
                if (gaFields.hitType == "pageview") {
                    var analyticsData = {
                        type: "log",
                        userName: expresso.Common.getUserInfo().userName,
                        applicationName: expresso.util.Util.getUrlParameter("app"),
                        url: gaFields.page,
                        parameters: gaFields.params ? (typeof gaFields.params === "string" ? gaFields.params : $.param(gaFields.params)) : null,
                        timeStamp: expresso.util.Formatter.formatDate(new Date(), expresso.util.Formatter.DATE_FORMAT.DATE_TIME_SEC)
                    };
                    sendRequest(expresso.Common.getSiteNamespace().config.Configurations.analyticsResourceService, "create",
                        analyticsData, null, {ignoreErrors: true});
                }
            }
        }
    };

    /**
     *
     * @return {string} the site name
     */
    var getSiteName = function () {
        return siteName;
    };

    /**
     *
     * @return {string} the site name space
     */
    var getSiteNamespace = function () {
        return siteNamespace;
    };

    /**
     *
     * @return {*} the server environment
     */
    var getServerEnv = function () {
        if (!serverEnv) {
            console.log("UI hostname [" + window.location.hostname + "]");
            serverEnv = siteNamespace.config.Configurations.serverEnv[window.location.hostname];
            if (!serverEnv) {
                // try with the port
                var hostport = window.location.hostname + ":" + window.location.port;
                console.log("UI hostport [" + hostport + "]");
                serverEnv = siteNamespace.config.Configurations.serverEnv[hostport];
                if (!serverEnv) {
                    // try using the path
                    var pathname = window.location.pathname.substring(1);
                    pathname = pathname.substring(0, pathname.indexOf('/'));
                    console.log("UI pathname [" + pathname + "]");
                    serverEnv = siteNamespace.config.Configurations.serverEnv[pathname];

                    if (!serverEnv) {
                        alert("Cannot determine the server path [" + window.location.hostname + ":" + window.location.port + "]");
                    }
                }
            }
            console.log("Using server environment " + JSON.stringify(serverEnv));
        }
        return serverEnv;
    };

    /**
     *
     * @return {string} the URL for the Web services
     */
    var getWsBasePathURL = function () {
        if (!serverEnv) {
            getServerEnv();
        }
        return serverEnv.wsBasePathURL;
    };

    /**
     *
     * @returns {string}
     */
    var getWsResourcePathURL = function () {
        return getWsBasePathURL() + "/" + authenticationPath;
    };

    /**
     *
     * @return {string|*} the URL for upload documet
     */
    var getWsUploadPathURL = function () {
        if (authenticationPath == "sso") {
            // path for Google Chrome and Kerberos
            return getWsBasePathURL() + "/upload";
        } else {
            return getWsBasePathURL() + "/" + authenticationPath;
        }
    };

    /**
     *
     * @return {string|*} the URL for upload documet
     */
    var getWsPublicPathURL = function () {
        return getWsBasePathURL() + "/public";
    };

    /**
     *
     * @return {string} the URL for the Web services
     */
    var getEnv = function () {
        if (!serverEnv) {
            getServerEnv();
        }
        return serverEnv.env;
    };

    /**
     * @return {boolean} true if the environment is production
     */
    var isProduction = function () {
        return (getEnv() == "prod");
    };

    /**
     * If true, if there is any Ajax error, it will not be displayed on sendRequest
     * @param display
     */
    var doNotDisplayAjaxErrorMessage = function (display) {
        doNotDisplayAjaxErrorMessageFlag = display;
    };

    /**
     * Get the application as defined in the applications.js file
     * @param appName
     * @return {*}
     */
    var getApplication = function (appName) {
        var app;
        if (typeof appName === "string") {
            if (siteNamespace.config.Applications && siteNamespace.config.Applications.appNameMap) {
                app = siteNamespace.config.Applications.appNameMap[appName];
            }
        } else {
            app = appName;
        }
        return app;
    };

    /**
     * getApplicationNameMap
     */
    var getApplicationNameMap = function () {
        return siteNamespace.config.Applications.appNameMap;
    };

    var clearApplicationCache = function () {
        try {
            Object.keys().forEach(function (key) {
                applicationsCache[key].destroy();
            });
        } catch (err) {
            // ignore
        }
        applicationsCache = {};
    };

    /**
     *
     * @param appName
     * @param [resource] {id:?} if you want to open a resource. No id means a new resource
     * @param [cacheApplication] by default, false. If true, application will not be destroyed
     * @returns {{}|jQuery}
     */
    var displayForm = function (appName, resource, cacheApplication) {
        var $deferred = $.Deferred();
        var $applicationDeferred = $.Deferred();

        if (cacheApplication && applicationsCache[appName]) {
            $applicationDeferred.resolve(applicationsCache[appName]);
        } else {
            loadApplication(appName).done(function (resourceManager) {
                if (resourceManager) {
                    if (cacheApplication) {
                        applicationsCache[appName] = resourceManager;
                    }
                    $applicationDeferred.resolve(resourceManager);
                } else {
                    $applicationDeferred.reject();
                }
            });
        }

        $applicationDeferred.done(function (resourceManager) {
            resourceManager.displayForm(resource)
                .done(function (updatedResource) {
                    $deferred.resolve(updatedResource);
                })
                .fail(function () {
                    $deferred.reject();
                })
                .always(function () {
                    if (!cacheApplication) {
                        // wait for the form to be closed and destroyed
                        window.setTimeout(function () {
                            if (resourceManager) {
                                resourceManager.destroy();
                                resourceManager = null;
                            }
                        }, 2000);
                    }
                });
        }).fail(function () {
            $deferred.reject();
        });
        return $deferred;
    };

    /**
     *
     * @param appName
     * @param title title of the window
     * @param [appOptions] options for the applications
     * @param [windowOptions] options for the Expresso window
     * @returns {*}
     */
    var displayResourceManager = function (appName, title, appOptions, windowOptions) {
        var $deferred = $.Deferred();

        appOptions = appOptions || {};
        windowOptions = windowOptions || {};

        var className = appName.replace(".", "-") + "-resource-manager-div";

        var resourceManager;
        expresso.util.UIUtil.buildWindow("<div class='" + className + "'></div>", {
            top: windowOptions.top || undefined,
            height: windowOptions.height || "max",
            width: windowOptions.width || "max",
            title: title || windowOptions.title || "",
            saveButtonLabel: windowOptions.saveButtonLabel || expresso.Common.getLabel("close"),
            open: function () {
                var $windowDiv = $(this);
                var $div = $windowDiv.find("." + className);
                $div.css("height", "100%");
                expresso.Common.loadApplication(appName, appOptions).done(function (rm) {
                    resourceManager = rm;
                    if (appOptions.labels) {
                        resourceManager.labels = $.extend({}, resourceManager.labels, appOptions.labels);
                    }
                    resourceManager.list($div, {});
                });
            },
            close: function () {
                var $windowDiv = $(this);
                if (windowOptions.close) {
                    windowOptions.close.call($windowDiv, resourceManager);
                }
                if (resourceManager) {
                    resourceManager.destroy();
                    resourceManager = null;
                }
                $deferred.resolve();
            }
        });

        return $deferred;
    };

    /**
     *
     * @param appName
     * @param title title of the window
     * @param [appOptions] options for the applications
     * @param [windowOptions] options for the Expresso window
     * @returns {*}
     */
    var displayApplication = function (appName, title, appOptions, windowOptions) {
        var $deferred = $.Deferred();

        // avoid null pointer
        appOptions = appOptions || {};
        windowOptions = windowOptions || {};
        var className = appName.replace(".", "-") + "-application-div";

        var appInstance;
        expresso.util.UIUtil.buildWindow("<div class='" + className + "'></div>", {
            top: windowOptions.top || undefined,
            height: windowOptions.height || "max",
            width: windowOptions.width,
            title: title || windowOptions.title || "",
            saveButtonLabel: windowOptions.saveButtonLabel || expresso.Common.getLabel("close"),
            buttons: windowOptions.buttons || undefined,
            destroyOnClose: windowOptions.destroyOnClose,
            confirmationOnClose: windowOptions.confirmationOnClose,
            open: function () {
                var $windowDiv = $(this);
                var $div = $windowDiv.find("." + className);
                $div.css("height", "100%");
                expresso.Common.loadApplication(appName, appOptions, $div).done(function (application) {
                    appInstance = application;
                    appInstance.render(true).done(function () {
                        $div.children(".exp-application-base").css("overflow", "auto");
                        if (windowOptions.open) {
                            windowOptions.open.call($windowDiv, appInstance);
                        }
                    });
                });
            },
            save: function () {
                var $windowDiv = $(this);
                if (windowOptions.save) {
                    return windowOptions.save.call($windowDiv, appInstance);
                }
            },
            close: function () {
                var $windowDiv = $(this);
                if (windowOptions.close) {
                    windowOptions.close.call($windowDiv, appInstance);
                }
                if (appInstance) {
                    appInstance.destroy();
                    appInstance = null;
                }
                $deferred.resolve();
            }
        });

        return $deferred;
    }

    /**
     *
     * @param url
     * @param [title]
     */
    var displayUrl = function (url, title) {
        var $deferred;
        if (url.indexOf('#') != -1 && url.indexOf('(') != -1 && url.indexOf('-') != -1 && url.indexOf(')') != -1) {
            // url format: baseUrl#<application>(<keyField>-<keyValue>)
            var s = url.substring(url.indexOf('#') + 1);
            var appName = s.substring(0, s.indexOf("("));
            var keyField = s.substring(s.indexOf("(") + 1, s.indexOf("-"));
            var keyValue = s.substring(s.indexOf("-") + 1, s.indexOf(")"));
            var id = s.substring(s.lastIndexOf("(") + 1, s.lastIndexOf(")"));

            // display application
            console.log("[" + appName + "] [" + keyField + "] [" + keyValue + "] [" + id + "]");

            if (appName.endsWith("Manager")) {
                // display Form
                $deferred = $.Deferred();
                loadApplication(appName).done(function (resourceManager) {
                    resourceManager.displayForm({id: id}).done(function () {
                        $deferred.resolve();
                    });
                });
            } else {
                $deferred = displayApplication(appName, title, {
                    queryParameters: {
                        id: id
                    }
                });
            }
        } else {
            window.open(url, "_blank");
            $deferred = $.Deferred().resolve();
        }
        return $deferred;
    };

    /**
     * Load the application at the path. Then instantiate the application and
     * pass the new instance to the callback
     * @param appName name of the application (or the application definition)
     * @param [options] options for the resource manager
     * @param [$containerDiv] div where to put the $domElement
     * @param [masterResourceManager]  the master resource manager
     * @param [siblingResourceManager]  the sibling resource manager
     * @returns {*} a Promise when the script is loaded and the instance is created
     */
    var loadApplication = function (appName, options, $containerDiv, masterResourceManager, siblingResourceManager) {
        var $deferred = $.Deferred();

        expresso.util.UIUtil.showLoadingMask($("body"), true, {id: "loadApplication" + appName});

        var appDef = getApplication(appName);
        // console.log("loadApplication: " + appDef.appClass + " parent:" + appDef.parent + " masterResourceManager:" + masterResourceManager);

        if (appDef && appDef.appClass) {

            // if the application has a master application that need to be loaded, load it first
            var masterDeferred = $.Deferred();
            if (!masterResourceManager && appDef.parent) {
                loadApplication(appDef.parent).done(function (ancestor) {
                    ancestor.loadedBySubApplication = true;
                    masterDeferred.resolve(ancestor);
                });
            } else {
                // no master parent to be loaded
                masterDeferred.resolve(masterResourceManager);
            }

            // when the master is loaded (if needed)
            masterDeferred.done(function (masterResourceManager) {
                // find the application path from the appClass
                var appPath = getApplicationPath(appDef);

                // make sure the namespace is created
                var namespace = getApplicationNamespace(appPath);
                createApplicationNamespace(namespace);

                getScript(appPath + "/app_class.js").done(function () {
                    try {
                        var appInstance = eval("new " + appDef.appClass + "('" + appPath + "')");

                        // assign the container (usually for application loaded in window)
                        appInstance.$containerDiv = $containerDiv;

                        // register the app def to the instance
                        appInstance.appDef = appDef;

                        // configure the instance (we must assign the options here to retain methods)
                        if (appInstance.setOptions) {
                            appInstance.setOptions(options);
                        }

                        // for backward compatibility
                        if (appInstance.setCustomOptions) {
                            appInstance.setCustomOptions(options);
                        }

                        // keep a reference on the master resource manager
                        if (appInstance.setMasterResourceManager) {
                            appInstance.setMasterResourceManager(masterResourceManager);
                        }

                        // keep a reference on the sibling resource manager
                        if (appInstance.setSiblingResourceManager) {
                            appInstance.setSiblingResourceManager(siblingResourceManager);
                        }

                        // if the application is displayed in the $containerDiv, it means it is a master view
                        if ($containerDiv || (masterResourceManager === undefined)) {
                            appInstance.displayAsMaster = true;
                        }

                        // then initialize the data for the application
                        appInstance.initData().done(function () {
                            $deferred.resolve(appInstance);
                        }).fail(function () {
                            $deferred.reject();
                        });
                    } catch (ex) {
                        console.trace(ex);
                        alert("Cannot instantiate the app_class: " + ex);
                        $deferred.reject();
                    }
                });
            });
        } else {
            console.warn("Cannot load application [" + appName + "]");
            $deferred.reject();
        }

        // always remove the loading mask
        $deferred.always(function () {
            expresso.util.UIUtil.showLoadingMask($("body"), false, {id: "loadApplication" + appName});
        });

        return $deferred;
    };

    /**
     *
     * @param app
     * @param appName
     * @param customOptions
     */
    var addApplicationToHistory = function (app, appName, customOptions) {
        customOptions = customOptions || {};

        var params = $.extend({}, customOptions.queryParameters);
        params["app"] = appName;

        if (expresso.Common.getImpersonateUser()) {
            params["impersonate"] = expresso.Common.getImpersonateUser();
        }

        // delete any url parameters for login, forget password
        delete params.loginToken;
        delete params.securityToken;
        delete params.userName;
        delete params.autoLoginUserName;
        // delete params.lang;

        // cezinc only
        delete params.menuItemSecurityProfile;

        // add the queryParameters to the URL
        var url = window.location.pathname;
        var queryParameters = $.param(params).replace(/\+/g, "%20");
        if (queryParameters) {
            url += "?" + queryParameters;
        }
        var title = (app && app.title) ? app.title : "";
        window.history.pushState(null, title, url);
        // window.history.replaceState(null, title, url);

        // update the title
        document.title = title;

        // update Google Analytics
        expresso.Common.sendAnalytics({hitType: "pageview", page: appName});
    };

    /**
     * Helper method to process the Application class:<br>
     * - Provide a reverse lookup (from appClass to Application Name)<br>
     * - Add a parent to sub manager
     */
    var processApplicationList = function () {

        // Provide a reverse lookup (from appClass to Application Name)
        var appClassMap = {};
        var appNameMap = {};

        function buildAppMaps(root, rootName) {
            if ($.isPlainObject(root)) {
                for (var appName in root) {
                    var absoluteAppName = (rootName ? rootName + "." : "") + appName;
                    var appDef = root[appName];
                    if (appDef && appDef.appClass) {
                        appDef.absoluteAppName = absoluteAppName;
                        appClassMap[appDef.appClass] = appDef;
                        appNameMap[absoluteAppName] = appDef;
                    }

                    // verify if there is sub manager
                    buildAppMaps(appDef, absoluteAppName);
                }
            }
        }

        buildAppMaps(siteNamespace.config.Applications);
        siteNamespace.config.Applications.appNameMap = appNameMap;
        siteNamespace.config.Applications.appClassMap = appClassMap;

        // Add a parent to sub manager
        function addParentToSubManager(parentApp) {
            if ($.isPlainObject(parentApp)) {
                for (var subAppName in parentApp) {
                    var subApp = parentApp[subAppName];
                    if (subApp && subApp.appClass) {
                        // continue going down the sub managers
                        addParentToSubManager(subApp);

                        // then add the reference to the parent
                        if (parentApp.appClass) {
                            subApp.parent = parentApp;
                        }
                    }
                }
            }
        }

        addParentToSubManager(siteNamespace.config.Applications);
    };

    /**
     * If you need to add an application after the initialization, you must use this method
     * @param appName
     * @param appDef
     */
    var addApplication = function (appName, appDef) {
        siteNamespace.config.Applications[appName] = appDef;
        siteNamespace.config.Applications.appNameMap[appName] = appDef;
        if (appDef.appClass) {
            siteNamespace.config.Applications.appClassMap[appDef.appClass] = appDef;
        }
    };

    /**
     * Load a resource manager.
     * @param resourceManagerDef name of the application (or the application definition).
     * @param [options] options for the resource manager
     * @param [$containerDiv] div where to put the $domElement
     * @param [masterResourceManager] the master resource manager, so it means that we do not need to load the ancestor
     * @returns {*} a promise when the resource manager is loaded
     */
    var loadResourceManager = function (resourceManagerDef, options, $containerDiv, masterResourceManager) {
        console.warn("DEPRECATED loadResourceManager. Use loadApplication instead");
        return loadApplication(resourceManagerDef, options, $containerDiv, masterResourceManager);
    };

    /**
     * Get the label for the key.
     * @param key
     * @param [labels] default is expresso.Labels
     * @param [params]
     * @param [nullIfNoExist] default is false
     * @param [shortLabel] default is false
     * @return {string} the label
     */
    var getLabel = function (key, labels, params, nullIfNoExist, shortLabel) {
        if (!key) {
            return "*[" + key + "]*";
        }

        labels = labels || expresso.Labels || {};

        var oriKey = key;
        var label = labels[key];

        if (label === undefined) {
            // if there is a composed name
            if (key.indexOf(".") != -1) {
                var lastKey = key.substring(key.lastIndexOf(".") + 1);

                // handle title, description, label, fullName, name
                if (lastKey == "title" || lastKey == "description" || lastKey == "label" || lastKey == "fullName" ||
                    lastKey == "name" || lastKey == "pgmKey" || lastKey == "fullDescription") {
                    // take the previous key
                    var previousKey = key.substring(0, key.lastIndexOf("."));
                    if (previousKey.indexOf(".") != -1) {
                        previousKey = previousKey.substring(previousKey.lastIndexOf(".") + 1);
                    }
                    key = previousKey;
                } else {
                    key = lastKey;
                }

                label = labels[key];
            }
        }

        if (label === undefined) {
            // if it is a Labels, try with Ids
            if (key.endsWith("Labels")) {
                key = key.substring(0, key.length - "Labels".length) + "Ids";
            }
            label = labels[key];
        }

        if (label === undefined) {
            // if it is an Id, use the entity name only
            if (key.endsWith("Id")) {
                key = key.substring(0, key.length - 2);
            }
            label = labels[key];
        }

        if (label === undefined) {
            // if it is an Ids, use the entity name only
            if (key.endsWith("Ids")) {
                key = key.substring(0, key.length - 3);
            }
            label = labels[key];
        }

        if (label === undefined) {
            if (key.endsWith("Status")) {
                label = labels["status"];
            } else if (key.endsWith("Type")) {
                label = labels["type"];
            } else if (key.endsWith("Priority")) {
                label = labels["priority"];
            } else if (key.endsWith("Date")) {
                label = labels["date"];
            }
        }

        if (label === undefined) {
            var k;
            if (key.endsWith("Title")) {
                k = key.substring(0, key.length - "Title".length);
                label = labels[k];
            } else if (key.endsWith("Label")) {
                k = key.substring(0, key.length - "Label".length);
                label = labels[k];
            }
        }

        if (label && typeof label !== "string") {
            // label could be an object with a short and a long label
            if (label.shortLabel) {
                label = (shortLabel ? label.shortLabel : label.label);
            }
        }

        if (label === undefined && !nullIfNoExist) {
            label = "*[" + oriKey + "]*";
        }

        // if there are parameters, replace them
        if (params && label) {
            for (var p in params) {
                label = label.replace(new RegExp("\\{" + p + "\\}", 'g'), params[p]);
            }
        }

        return label;
    };

    /**
     *
     * @param wsPath
     * @param [filter]
     * @param [labels]
     * @returns {*}
     */
    var getValues = function (wsPath, filter, labels) {
        if (filter) {
            if (typeof filter === "function") {
                filter = filter();
            }
            filter = expresso.Common.buildKendoFilter(filter);
        }

        return expresso.Common.sendRequest(wsPath, null, null, filter,
            {waitOnElement: null}).then(function (result) {
            return expresso.Common.updateDataValues(result, labels);
        });
    };

    /**
     * Helper method to update the values array
     * @param va array to push the values
     * @param data array of data
     * @param [field] Field to use for the description (default is "label")
     * @param [allValuesLabel] if not null, defined the description for the all values
     * @param [labels] if not null, used for list translation
     */
        // @Deprecated use updateDataValues instead
    var updateValues = function (va, data, field, allValuesLabel, labels) {
            if (!va) {
                va = [];
            }

            if (va.length) {
                // already initialized
                return;
            }

            if (data.data) {
                data = data.data;
            }

            // if there is a label for "ALL", put it at first
            if (allValuesLabel !== undefined) {
                data.unshift({
                    id: null,
                    label: allValuesLabel
                });
            }

            // try to get the type labels (if defined)
            var typeLabels;
            if (data.length && data[0].type) {
                // a type is defined by an object
                typeLabels = expresso.Common.getLabel(data[0].type + "Labels", labels, true);
            }

            for (var i = 0; i < data.length; i++) {
                var d = data[i];
                if (field) {
                    if (typeof field === "string") {
                        d.label = d[field];
                    } else {
                        d.label = field(d);
                    }
                }

                // NOTE
                // value,text: is mandatory for Grid filtering using combobox
                // id,label: used by combobox and dropdownlist in form.
                d.value = d.id;

                // try to get the label (if not, keep the current text for backward compatibility)
                if (typeLabels && d.pgmKey && typeof d.pgmKey === "string" && isNaN(parseInt(d.pgmKey))) {
                    d.label = typeLabels[d.pgmKey] || d.label;
                }
                d.text = d.label;

                va.push(d);
            }
            return va;
        };

    /**
     * Helper method to update the values array
     * @param data array of data
     * @param [labels] if not null, used for list translation
     */
    var updateDataValues = function (data, labels) {
        if (data && data.data) {
            data = data.data;
        }

        // if not data, return now
        if (!data || !data.length) {
            return [];
        }

        // try to get the type labels (if defined)
        var typeLabels;
        if (data.length && data[0].type) {
            // a type is defined by an object
            typeLabels = expresso.Common.getLabel(data[0].type + "Labels", labels, true);
        }

        for (var i = 0; i < data.length; i++) {
            var d = data[i];

            if (typeof d !== "object") {
                // this is a simple array
                // [1,2,3,etc]
                // ["A","B","C",etc]
                d = {
                    id: d,
                    label: d
                };
                data[i] = d;
            }

            if (typeof d.id === "object") {
                // this is a fix for this problem: String{"muid"} is not equal to "muid"
                d.id = "" + d.id;
            }

            // backward compatibility
            if (d.value) {
                d.id = d.value;
            }
            if (d.text) {
                d.label = d.text;
            }

            // NOTE
            // value,text: is mandatory for Grid filtering using combobox
            // id,label: used by combobox and dropdownlist in form.
            d.value = d.id;

            // try to get the label (if not, keep the current text for backward compatibility)
            if (typeLabels && d.pgmKey && typeof d.pgmKey === "string" && isNaN(parseInt(d.pgmKey))) {
                d.label = typeLabels[d.pgmKey] || d.label;
            }
            d.text = d.label;
        }
        return data;
    };

    /**
     * Get the resource name from the Web Service path.
     * Basically, it takes only even path in the URI
     * @param wsPath
     * @returns resourceSecurityPath
     */
    var getResourceSecurityPathFromPath = function (wsPath) {
        var resourceSecurityPath = "";
        var s = wsPath.split("/");
        for (var i = 0; i < s.length; i += 2) {
            if (i != 0) {
                resourceSecurityPath += "/";
            }
            resourceSecurityPath += s[i];
        }
        //console.log("********* [" + wsPath + "] [" + resourceSecurityPath + "]");
        return resourceSecurityPath;
    };


    /**
     *
     * @param resourceSecurityPath
     * @param action
     * @param [displayMessage] true is you want a message to be displayed
     * @returns {boolean}
     */
    var isUserAllowed = function (resourceSecurityPath, action, displayMessage) {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.isUserAllowed(resourceSecurityPath, action, displayMessage);
        } else {
            return siteNamespace.Main.isUserAllowed(resourceSecurityPath, action, displayMessage);
        }
    };

    /**
     * Returns true if the user is in role
     */
    var isUserInRole = function (role) {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.isUserInRole(role);
        } else {
            return siteNamespace.Main.isUserInRole(role);
        }
    };

    /**
     * Returns true if the user is a power user
     */
    var isPowerUser = function () {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.isPowerUser();
        } else {
            return siteNamespace.Main.isPowerUser();
        }
    };

    /**
     * Returns true if the user is a superuser
     */
    var isAdmin = function () {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.isAdmin();
        } else {
            return siteNamespace.Main.isSuperUser();
        }
    };

    /**
     *
     * @returns {*}
     */
    var getUserInfo = function () {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.getUserInfo();
        } else {
            return siteNamespace.Main.getUserInfo();
        }
    };

    /**
     *
     * @returns {*}
     */
    var getImpersonateUser = function () {
        // redirect this method to the Main application
        if (expresso.Security) {
            return expresso.Security.getImpersonateUser();
        } else {
            return siteNamespace.Main.getImpersonateUser();
        }
    };

    /**
     * Load the application preferences
     * @param applicationName
     * @return {*} promise when the request is done
     */
    var loadApplicationPreferences = function (applicationName) {
        if (expresso.util.Util.getUrlParameter("publicUser")) {
            // do not load
            return $.Deferred().resolve({preferences: {}});
        } else {
            // get the user preferences
            var preferenceFilter = expresso.Common.buildKendoFilter({"application": applicationName});
            return expresso.Common.sendRequest("user/" + expresso.Common.getUserInfo().id + "/preference", null,
                null, preferenceFilter, {waitOnElement: null}
            ).then(function (appPreferences) {

                //console.log("Got appPreferences", appPreferences);

                // Get the preferences for the application
                var applicationPreferences;
                if (appPreferences && appPreferences.total == 1) {
                    applicationPreferences = appPreferences.data[0];

                    // as the preferences are stored as a JSON string, parse it
                    //console.log(userPreferences.preferences);
                    if (applicationPreferences.preferences) {
                        applicationPreferences.preferences = JSON.parse(applicationPreferences.preferences);

                        // TO BE REMOVED LATER - backward compatibility
                        if ($.isArray(applicationPreferences.preferences)) {
                            var gridPreferences = [];
                            $.each(applicationPreferences.preferences, function (index, pref) {
                                gridPreferences.push(pref.gridFilter);
                            });
                            applicationPreferences.preferences = {gridPreferences: gridPreferences};
                        }

                        // TO BE REMOVED LATER - backward compatibility
                        if (applicationPreferences.preferences.gridFilters) {
                            applicationPreferences.preferences.gridPreferences = applicationPreferences.preferences.gridFilters;
                            delete applicationPreferences.preferences.gridFilters;
                        }

                        // TO BE REMOVED LATER - backward compatibility
                        if (applicationPreferences.preferences.gridPreferences) {
                            if ($.isArray(applicationPreferences.preferences.gridPreferences)) {
                                gridPreferences = {};
                                $.each(applicationPreferences.preferences.gridPreferences, function (index, pref) {
                                    gridPreferences[pref.name] = pref;
                                });
                                applicationPreferences.preferences.gridPreferences = gridPreferences;
                            }
                        }

                    } else {
                        applicationPreferences.preferences = {};
                    }
                } else {
                    applicationPreferences = {
                        id: undefined,
                        type: "userPreference",
                        userId: expresso.Common.getUserInfo().id,
                        application: applicationName,
                        preferences: {}
                    };
                }
                return applicationPreferences;
            });
        }
    };

    /**
     * Save the application preferences
     * @param applicationPreferences
     * @return {*} promise when the request is done
     */
    var saveApplicationPreferences = function (applicationPreferences) {
        // to avoid issue, we need to duplicate the preferences
        applicationPreferences = $.extend(true, {}, applicationPreferences);

        // first we need to stringify the preferences (saved as varchar in backend)
        applicationPreferences.preferences = JSON.stringify(applicationPreferences.preferences);

        // save the preference
        return expresso.Common.sendRequest("user/" + expresso.Common.getUserInfo().id + "/preference" +
            (applicationPreferences.id ? "/" + applicationPreferences.id : ""),
            applicationPreferences.id ? "update" : "create", applicationPreferences).then(function (updatedApplicationPreferences) {
            // the parse the preferences to use it as JSON
            updatedApplicationPreferences.preferences = JSON.parse(updatedApplicationPreferences.preferences);
            return updatedApplicationPreferences;
        });
    };


    /**
     * Notify the user when there is a newer version available
     */
    var initCheckVersion = function () {
        // only for production environment
        if (expresso.Common.isProduction()) {
            // if the main application if fullScreen, message cannot be displayed
            if (!doNotDisplayAjaxErrorMessageFlag && !expresso.util.Util.getUrlParameter("nomenu")) {
                // get the configuration file on the server
                var currentVersion = expresso.Common.getSiteNamespace().config.Configurations.version;
                $.ajax({
                    url: "config/configurations.js",
                    cache: false
                }).done(function () {
                    var serverVersion = expresso.Common.getSiteNamespace().config.Configurations.version;
                    if (currentVersion != serverVersion) {
                        console.log("Version: " + currentVersion + " -> " + serverVersion);
                        var $notification = $("<span class='exp-version-update-notification'></span>").appendTo($("body"));
                        var kendoNotification = $notification.kendoNotification({
                            autoHideAfter: 0,
                            position: {
                                top: 5,
                                right: 5
                            },
                            hide: function () {
                                window.location.reload();
                            }
                        }).data("kendoNotification");
                        kendoNotification.show(expresso.Common.getLabel("versionWarning"), "warning");

                        // add the class
                        kendoNotification.getNotifications().addClass("exp-version-update-notification");

                        // once displayed, no need to verify again the version
                    } else {
                        // verify again in n seconds
                        window.setTimeout(initCheckVersion, (expresso.Common.isProduction() ? 2 * 60 : 10) * 1000);
                    }
                }).fail(function (jqXHR) {
                    jqXHR.alreadyProcessed = true;
                    console.error("Unable to check version on server");

                    // verify again in 5 minutes
                    window.setTimeout(initCheckVersion, 5 * 60 * 1000);
                });
            }
        }
    };

    /**
     * Method called when the DOM is ready
     * @param name name of the site. To be used to find configuration and menu file.
     */
    var init = function (name) {

        // if IE, do not allow
        if (navigator.userAgent.indexOf('MSIE') !== -1) {
            /* Microsoft Internet Explorer detected in. */
            //expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("msieNotSupported"));
            alert(expresso.Common.getLabel("msieNotSupported"));
            return $.Deferred().reject();
        }

        siteName = name;
        siteNamespace = window[siteName] || {};

        // set the default for Ajax calls
        initAjax();

        // The navigator properties is not configured properly (no difference between a PC or the tablet)
        // Kendo scroll does not work on the tablet because of it
        kendo.support.kineticScrollNeeded = kendo.support.kineticScrollNeeded || (/iPad|iPhone|iPod/.test(navigator.platform) ||
                (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) ||
            expresso.util.Util.getUrlParameter("screenMode") == "tablet" ||
            expresso.util.Util.getUrlParameter("screenMode") == "phone";

        // resize section on window resize
        var $window = $(window);
        $window.on('orientationchange', function () {
            $window.trigger("resize")
        });
        $window.on('resize', function () {
            // init screen mode
            screenMode = undefined;
            getScreenMode();
        }).trigger("resize");

        return $.when(
            // set the default language
            setLanguage().done(function () {
                if (siteNamespace.config.Applications) {
                    // parse the application list and build maps for faster access
                    processApplicationList();
                }

                // if the user does not have the latest version, notify it
                initCheckVersion();
            })
        );
    };

    // return public properties and methods
    return {
        // PUBLIC properties
        HTTP_CODES: HTTP_CODES,
        SCREEN_MODES: SCREEN_MODES,

        // PUBLIC methods
        init: init,
        loadApplication: loadApplication,
        loadResourceManager: loadResourceManager,
        addApplicationToHistory: addApplicationToHistory,
        displayApplication: displayApplication,
        displayUrl: displayUrl,
        displayForm: displayForm,
        displayResourceManager: displayResourceManager,

        getScript: getScript,
        loadHTML: loadHTML,
        loadLabels: loadLabels,
        localizePage: localizePage,
        getLabel: getLabel,
        updateValues: updateValues, // @Deprecated
        updateDataValues: updateDataValues,
        getValues: getValues,
        setLanguage: setLanguage,
        getLanguage: getLanguage,
        getScreenMode: getScreenMode,
        setScreenMode: setScreenMode,
        getFontRatio: getFontRatio,

        buildKendoFilter: buildKendoFilter,
        removeKendoFilter: removeKendoFilter,
        addKendoFilter: addKendoFilter,

        sendRequest: sendRequest,
        purgeResource: purgeResource,
        parseResponseItem: parseResponseItem,
        displayServerValidationMessage: displayServerValidationMessage,

        executeReport: executeReport,
        sendDownloadRequest: sendDownloadRequest,

        getServerEnv: getServerEnv,
        getWsBasePathURL: getWsBasePathURL,
        getWsResourcePathURL: getWsResourcePathURL,
        getWsUploadPathURL: getWsUploadPathURL,
        getWsPublicPathURL: getWsPublicPathURL,
        setAuthenticationPath: setAuthenticationPath,
        getAuthenticationPath: getAuthenticationPath,
        getEnv: getEnv,
        isProduction: isProduction,

        getSiteName: getSiteName,
        getSiteNamespace: getSiteNamespace,
        clearApplicationCache: clearApplicationCache,
        createApplicationNamespace: createApplicationNamespace,
        getApplicationNamespace: getApplicationNamespace,
        getApplicationPath: getApplicationPath,
        getApplication: getApplication,
        addApplication: addApplication,
        getApplicationNameMap: getApplicationNameMap,
        doNotDisplayAjaxErrorMessage: doNotDisplayAjaxErrorMessage,
        setCurrentRequestLabels: setCurrentRequestLabels,
        sendAnalytics: sendAnalytics,

        loadApplicationPreferences: loadApplicationPreferences,
        saveApplicationPreferences: saveApplicationPreferences,

        // security
        getResourceSecurityPathFromPath: getResourceSecurityPathFromPath,
        isUserAllowed: isUserAllowed,
        isUserInRole: isUserInRole,
        isPowerUser: isPowerUser,
        isAdmin: isAdmin,
        getUserInfo: getUserInfo,
        getImpersonateUser: getImpersonateUser
    }
}());
