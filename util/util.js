var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * This is an utility module. It contains some utility methods.
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.util.Util = (function () {

    /**
     * Get the parameter value from the url.
     * If there is no value, but the parameter is defined, it returns "true"
     * @param parameterName parameter name
     * @param [href] by default, window.location.search
     * @returns {string|boolean} value of the parameter, or true if parameter is defined but no value
     */
    var getUrlParameter = function (parameterName, href) {
        href = href || window.location.search;
        if (href.startsWith("&") || href.startsWith("?")) {
            href = href.substring(1);
        }
        var pageURL = decodeURIComponent(href);
        var urlVariables = pageURL.split("&");

        for (var i = 0; i < urlVariables.length; i++) {
            var param = urlVariables[i].split("=");

            if (param[0] === parameterName) {
                return param[1] === undefined ? true : param[1];
            }
        }
        return undefined;
    };

    /**
     * Return a map of all query parameters in the URL
     * @param [href] by default, window.location.search
     * @returns {*}
     */
    var getUrlParameters = function (href) {
        href = href || window.location.search.substring(1);
        var urlVariables = href.split("&");

        var parameters = {};
        for (var i = 0; i < urlVariables.length; i++) {
            var param = urlVariables[i].split("=");
            if (param[0]) {
                parameters[param[0]] = (param[1] === undefined ? true : decodeURIComponent(param[1]));
            }
        }
        return parameters;
    };

    /**
     *
     */
    var getBrowser = function () {
        if ((navigator.userAgent.indexOf("Opera") || navigator.userAgent.indexOf('OPR')) != -1) {
            return "Opera";
        } else if (navigator.userAgent.indexOf("Edg") != -1) {
            // Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36 Edg/94.0.992.50
            return "Edge";
        } else if (navigator.userAgent.indexOf("Chrome") != -1) {
            // Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36
            return "Chrome";
        } else if (navigator.userAgent.indexOf("Safari") != -1) {
            return "Safari";
        } else if (navigator.userAgent.indexOf("Firefox") != -1) {
            return "Firefox";
        } else if ((navigator.userAgent.indexOf("MSIE") != -1) || (!!document.documentMode == true)) {
            return "MSIE"
        } else {
            return null;
        }
    };

    /**
     *
     * @returns {*}
     */
    var getFirstBrowserLanguage = function () {
        var nav = window.navigator,
            browserLanguagePropertyKeys = ['language', 'browserLanguage', 'systemLanguage', 'userLanguage'],
            i,
            language;

        // support for HTML 5.1 "navigator.languages"
        if (Array.isArray(nav.languages)) {
            for (i = 0; i < nav.languages.length; i++) {
                language = nav.languages[i];
                if (language && language.length) {
                    break;
                }
            }
        }

        // support for other well known properties in browsers
        if (!language) {
            for (i = 0; i < browserLanguagePropertyKeys.length; i++) {
                language = nav[browserLanguagePropertyKeys[i]];
                if (language && language.length) {
                    break;
                }
            }
        }

        if (language) {
            if (language.indexOf("-") != -1) {
                language = language.substring(0, language.indexOf("-"));
            }
        }

        //console.log("browserLanguage [" + language + "]");
        return language;
    };

    /**
     * Generate a unique id
     */
    var guid = function () {
        function s4() {
            return Math.floor((1 + Math.random()) * 0x10000)
                .toString(16)
                .substring(1);
        }

        return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
            s4() + '-' + s4() + s4() + s4();
    };

    /**
     *
     * @param unsafe
     * @returns {*}
     */
    var escapeHTML = function (unsafe) {
        if (unsafe) {
            return unsafe.replace(/[&<"']/g, function (m) {
                switch (m) {
                    case '&':
                        return '&amp;';
                    case '<':
                        return '&lt;';
                    case '"':
                        return '&quot;';
                    default:
                        return '&#039;';
                }
            });
        } else {
            return unsafe;
        }
    };

    /**
     * <code>
     pad(10, 4);      // 0010
     pad(9, 4);       // 0009
     pad(123, 4);     // 0123
     pad(10, 4, '-'); // --10
     </code>
     * @param n
     * @param width
     * @param z
     * @return {*}
     */
    var pad = function (n, width, z) {
        z = z || '0';
        n = n + '';
        return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
    };

    /**
     * Copy the text into the clipboard
     * @param text
     */
    var copyToClipboard = function (text) {
        var $temp = $("<input>").appendTo($("body"));
        $temp.val(text).select();
        document.execCommand("copy");
        $temp.remove();
    };

    /**
     * Scroll a table if needed
     * @param $tbody
     * @param scrollRows
     */
    var scrollTable = function ($tbody, scrollRows) {
        // default value
        scrollRows = scrollRows || 10;

        if ($tbody.hasScrollBar().vertical) {
            var h = $tbody.find("tr:visible:first").outerHeight(true);
            var offset = $tbody[0].scrollTop + (h * scrollRows);
            if (($tbody[0].scrollTop + $tbody.height() + 10 /* pixels to avoid limit issue */) >= $tbody[0].scrollHeight) {
                offset = 0;
            }
            // console.log($tbody[0].scrollTop + "+" + $tbody.height() + ">=" + $tbody[0].scrollHeight);

            $tbody.stop().animate({
                scrollTop: offset
            }, 100);
        }
    };

    /**
     *
     * @param hex
     * @param lum
     * @return {string}
     */
    var lightenDarkenColor = function (hex, lum) {

        // validate hex string
        hex = String(hex).replace(/[^0-9a-f]/gi, '');
        if (hex.length < 6) {
            hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
        }
        lum = lum || 0;

        // convert to decimal and change luminosity
        var rgb = "#", c, i;
        for (i = 0; i < 3; i++) {
            c = parseInt(hex.substr(i * 2, 2), 16);
            c = Math.round(Math.min(Math.max(0, c + (c * lum)), 255)).toString(16);
            rgb += ("00" + c).substr(c.length);
        }

        return rgb;
    };

    /**
     *
     * @param color
     * @param percent
     * @return {string}
     */
    function shadeColor(color, percent) {
        var f = parseInt(color.slice(1), 16), t = percent < 0 ? 0 : 255, p = percent < 0 ? percent * -1 : percent,
            R = f >> 16, G = f >> 8 & 0x00FF, B = f & 0x0000FF;
        return "#" + (0x1000000 + (Math.round((t - R) * p) + R) * 0x10000 + (Math.round((t - G) * p) + G) * 0x100 + (Math.round((t - B) * p) + B)).toString(16).slice(1);
    }

    /**
     *
     * @param hexcolor
     * @returns {string}
     */
    var getContrast = function (hexcolor) {
        var r = parseInt(hexcolor.substr(1, 2), 16);
        var g = parseInt(hexcolor.substr(2, 2), 16);
        var b = parseInt(hexcolor.substr(4, 2), 16);
        var yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return (yiq >= 35) ? 'black' : 'white';
    };

    /**
     * debouncing function from John Hann
     * http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/
     * @param func
     * @param threshold
     * @param execAsap
     * @returns {function} debounced
     */
    var debounce = function (func, threshold, execAsap) {
        var timeout;

        return function debounced() {
            var obj = this, args = arguments;

            function delayed() {
                if (!execAsap)
                    func.apply(obj, args);
                timeout = null;
            }

            if (timeout)
                clearTimeout(timeout);
            else if (execAsap)
                func.apply(obj, args);

            timeout = setTimeout(delayed, threshold || 100);
        };
    };


    /**
     * Function to make a tree from a flat list with a parentId
     * @param data The flat list to convert
     * @param parentIdField The id of the parent
     * @param idField The id of the object
     * @param rootLevel The level at which we should start
     * @param [expanded] if true, the tree will be expanded (default is false)
     * @returns []
     */
    var makeTreeFromFlatList = function (data, parentIdField, idField, rootLevel, expanded) {
        var hash = {};
        idField = idField || "id";
        parentIdField = parentIdField || "id";
        rootLevel = rootLevel || null;

        for (var i = 0; i < data.length; i++) {
            var item = data[i];
            var id = item[idField];
            var parentId = item[parentIdField] || null;

            hash[id] = hash[id] || [];
            hash[parentId] = hash[parentId] || [];

            item.items = hash[id];
            hash[parentId].push(item);

            item.expanded = expanded;
        }
        return hash[rootLevel];
    };

    // return public properties and methods
    return {
        // public methods
        getUrlParameter: getUrlParameter,
        getUrlParameters: getUrlParameters,
        guid: guid,
        escapeHTML: escapeHTML,

        pad: pad,
        lightenDarkenColor: lightenDarkenColor,
        shadeColor: shadeColor,
        getContrast: getContrast,
        scrollTable: scrollTable,
        debounce: debounce,
        getBrowser: getBrowser,
        getFirstBrowserLanguage: getFirstBrowserLanguage,
        copyToClipboard: copyToClipboard,
        makeTreeFromFlatList: makeTreeFromFlatList
    }
}());
