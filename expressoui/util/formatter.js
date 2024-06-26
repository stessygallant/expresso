var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * This is an utility module to format date, resource number, etc
 * It uses the Javascript Module encapsulation pattern to provide public and private properties.
 */
expresso.util.Formatter = (function () {
    var DATE_FORMAT = {
        DATE_ONLY: "yyyy-MM-dd",
        DATE_TIME: "yyyy-MM-dd HH:mm",
        TEXTUAL_DATE_ONLY: "dddd, yyyy-MM-dd",
        TEXTUAL_DATE_TIME: "dddd, dd MMMM HH:mm:ss",
        DATE_TIME_SEC: "yyyy-MM-dd HH:mm:ss",
        TIME_ONLY: "HH:mm",
        TIME_SEC_ONLY: "HH:mm:ss"
    };

    /**
     * Format a date to a string
     * @param d the date to format
     * @param format as defined in DATE_FORMAT
     * @returns {*} a date formatted as a string
     */
    var formatDate = function (d, format) {
        if (!format) format = DATE_FORMAT.DATE_ONLY;

        // convert it to date if needed
        if (typeof d === "string") {
            if (format == DATE_FORMAT.DATE_ONLY) {
                d = parseDate(d);
            } else {
                d = parseDateTime(d);
            }
        }

        return kendo.toString(d, format);
    };

    /**
     * Parse a string into a date.
     * @param dateString refer to new Date(dateString);
     */
    var parseDate = function (dateString) {
        var d = parseDateTime(dateString);

        if (d && d instanceof Date) {
            return d.dateOnly();
        } else {
            return null;
        }
    };

    /**
     * Parse a string into a date.
     * @param dateString refer to new Date(dateString);
     */
    var parseDateTime = function (dateString) {
        var d;
        if (dateString) {
            if (typeof dateString === "string") {
                if (dateString.length >= 24) {
                    // standard format from Expresso backend
                    // yyyy-MM-ddTHH:mm:ssXXX"
                    // 2019-01-31T00:00:00-05:00

                    // If you forget to put the JAXBDateAdapter on your setter, then you will
                    // get the Java default output (with milliseconds)
                    // yyyy-MM-ddTHH:mm:ss.SSSXXX"
                    // 2019-01-31T00:00:00.123-05:00

                    d = new Date(dateString);
                } else if (dateString.length == 19) {
                    d = kendo.parseExactDate(dateString, ["yyyy-MM-dd HH:mm:ss", "yyyy-MM-ddTHH:mm:ss"]);
                } else if (dateString.length == 16) {
                    d = kendo.parseExactDate(dateString, "yyyy-MM-dd HH:mm");
                } else if (dateString.length == 10) {
                    d = kendo.parseExactDate(dateString, "yyyy-MM-dd");
                } else if (dateString.length == 8) {
                    d = kendo.parseExactDate(dateString, "HH:mm:ss");
                } else if (dateString.length == 5) {
                    d = kendo.parseExactDate(dateString, "HH:mm");
                } else {
                    console.trace("Date format not supported [" + dateString + "]");
                }
            } else {
                // assume to be a date
                d = dateString;
            }
        }
        return d;
    };

    /**
     * Format the seconds in datetime format
     * @param seconds
     * @param [options] {format: <datetime format>}
     * @returns {string}
     */
    var formatSeconds = function (seconds, options) {
        if (!isNaN(seconds) && seconds > 0) {
            options = options || {};

            var t = new Date(1970, 0, 1); // Epoch
            t.setSeconds(seconds); // local time
            //t.setTime(seconds * 1000);

            if (!options.format) {
                options.format = "";
                if (options.showSeconds !== false) {
                    options.format = seconds > 60 ? "ss" : "ss"; // cannot have only one s
                }
                if (seconds > 60) {
                    options.format = (seconds > (60 * 60) ? "mm" : "m") + ":" + options.format;
                }
                if (seconds > (60 * 60)) {
                    options.format = (seconds >= (60 * 60 * 24) ? "HH" : "H") + ":" + options.format;
                }

                if (seconds >= (60 * 60 * 24)) {
                    var nbrDays = Math.floor(seconds / (60 * 60 * 24));
                    options.format = "'" + nbrDays + "d'" + " " + options.format;
                }
            }

            // console.log("format: " + options.format + " - " + t);
            return kendo.toString(t, options.format);
        } else {
            return "";
        }
    };

    // return public properties and methods
    return {
        // PUBLIC PROPERTIES
        DATE_FORMAT: DATE_FORMAT,

        // DATE
        formatDate: formatDate,
        parseDate: parseDate,
        parseDateTime: parseDateTime,
        formatSeconds: formatSeconds
    }
}());

