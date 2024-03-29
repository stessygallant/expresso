Date.prototype.addDays = function (days) {
    var d = new Date(this.valueOf());
    d.setDate(d.getDate() + days);
    return d;
};

Date.prototype.addHours = function (hours) {
    var d = new Date(this.valueOf());
    d.setHours(d.getHours() + hours);
    return d;
};

Date.prototype.addMinutes = function (minutes) {
    var d = new Date(this.valueOf());
    d.setMinutes(d.getMinutes() + minutes);
    return d;
};

Date.prototype.addSeconds = function (seconds) {
    var d = new Date(this.valueOf());
    d.setSeconds(d.getSeconds() + seconds);
    return d;
};

Date.prototype.dateOnly = function () {
    var d = new Date(this.valueOf());
    d.setHours(0, 0, 0, 0);
    return d;
};

Date.prototype.lastSaturday = function () {
    return new Date(this.getFullYear(), this.getMonth(), this.getDate() - this.getDay() - 1);
};

Date.prototype.lastSunday = function () {
    return new Date(this.getFullYear(), this.getMonth(), this.getDate() - this.getDay());
};

Date.prototype.lastMonday = function () {
    return new Date(this.getFullYear(), this.getMonth(), this.getDate() - this.getDay() + 1);
};

var _dateWeekDayNames = {
    fr: ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
    en: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
};

var _dateMonthNames = {
    fr: ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"],
    en: ["January", "February", " March", " April", " May", " June", " July", " August", " September", " October", " November", " December"]
};

/**
 * Get the name of the week day
 * @param [lang] language. Default is window.locale if exists, otherwise "en"
 * @return {Date}
 */
Date.prototype.getWeekDayName = function (lang) {
    lang = lang || window.locale || "en";
    lang = lang.substring(0, 2);
    return _dateWeekDayNames[lang][this.getDay()];
};

/**
 * Get the name of the week day
 * @param [lang] language. Default is window.locale if exists, otherwise "en"
 * @return {Date}
 */
Date.prototype.getMonthName = function (lang) {
    lang = lang || window.locale || "en";
    lang = lang.substring(0, 2);
    return _dateMonthNames[lang][this.getMonth()];
};

Date.prototype.isSameDate = function (date) {
    return this.getFullYear() === date.getFullYear() &&
        this.getMonth() === date.getMonth() &&
        this.getDate() === date.getDate();
};

Date.prototype.isValid = function () {
    return this instanceof Date && !isNaN(this);
};

// Returns the ISO week of the date.
Date.prototype.getISOWeekNumber = function () {
    var date = new Date(this.getTime());
    date.setHours(0, 0, 0, 0);
    // Thursday in current week decides the year.
    date.setDate(date.getDate() + 3 - (date.getDay() + 6) % 7);
    // January 4 is always in week 1.
    var week1 = new Date(date.getFullYear(), 0, 4);
    // Adjust to Thursday in week 1 and count number of weeks from date to week1.
    return 1 + Math.round(((date.getTime() - week1.getTime()) / 86400000
        - 3 + (week1.getDay() + 6) % 7) / 7);
}

// Returns the four-digit year corresponding to the ISO week of the date.
Date.prototype.getWeekYear = function () {
    var date = new Date(this.getTime());
    date.setDate(date.getDate() + 3 - (date.getDay() + 6) % 7);
    return date.getFullYear();
}