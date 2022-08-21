var expresso = expresso || {};

/**
 * Main application. It uses the Javascript Module encapsulation pattern
 */
expresso.Main = function () {
    // reference to the current application instance
    var currentAppInstance = undefined;
    var currentAppName = undefined;

    /**
     * Build the menu for the user
     */
    var buildMenu = function () {
        var $main = $("body").find(".main");
        var $mainDiv = $main.find(".main-div");
        var $menu = $main.find(".main-menu");
        var $title = $mainDiv.find(".main-title");
        var $content = $mainDiv.find(".main-content");

        // register a listener for click on icon
        $menu.on("click", ".menu-item", function () {
            var $this = $(this);
            $menu.hide();
            $mainDiv.show();

            // add a title and a deconnection button
            $title.html("<span class='title'>" + $this.find(".menu-text").text() + "</span><a href='' class='logout-button'>" + expresso.Common.getLabel("disconnect") + "</a>");
            $main.find(".logout-button").on("click", function (e) {
                e.preventDefault();
                expresso.Security.logout();
            });

            // load the application
            currentAppName = $this.data("app-name");
            expresso.Common.loadApplication(currentAppName, {}, $content).done(function (appInstance) {
                currentAppInstance = appInstance;
                appInstance.render(true).done(function () {

                });
            });
        });

        // we need to extend the menu System with the menu Applications
        var menus = expresso.Common.getSiteNamespace().config.Menu;
        var menuLabels = expresso.Common.getSiteNamespace().config.menu.Labels;

        // build the menu
        for (var g = 0, l = menus.length; g < l; g++) {
            var menuGroup = menus[g];
            for (var i = 0, l2 = menuGroup.items.length; i < l2; i++) {
                var menuItem = menuGroup.items[i];

                // make sure the user is allowed
                var app = expresso.Common.getApplication(menuItem.appName);
                if (app && app.allowed) {
                    $("<div class='menu-item' data-app-name='" + menuItem.appName + "' style='background-color:" + menuItem.previewColor + "'>" +
                        "<div class='menu-initial'><span class='initial'>" + menuItem.previewInitial + "</span></div>" +
                        "<div class='menu-text'><span class='full-name'>" + menuLabels[menuItem.appName] + "</span></div>" +
                        "</div>").appendTo($menu);
                }
            }
        }
    };

    /**
     * Initialization of the UI (menu and application)
     */
    var initUI = function () {
        var $main = $("body").find(".main");
        var $mainDiv = $main.find(".main-div");
        var $menu = $main.find(".main-menu");

        // build the menu
        buildMenu();

        // Listen to HTML 5 History API to select the correct menu item on navigation
        $(window).on("popstate", function () {
            // always go back to the menu
            $mainDiv.hide();
            $menu.show();
        });
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
                initUI();
            });
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
        getCurrentAppName: getCurrentAppName
    };
}();
