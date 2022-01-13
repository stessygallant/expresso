var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.mobile = expresso.layout.mobile || {};

/**
 * Base expresso layout for mobile
 */
expresso.layout.mobile.MobileBase = expresso.layout.applicationbase.AbstractApplicationBase.extend({

    // reference to the kendo mobile application
    kendoMobileApplication: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.AbstractApplicationBase.fn.initDOMElement.call(this, $domElement);
    },

    // @override
    onDomElementInitialized: function () {
        var _this = this;
        return expresso.layout.applicationbase.AbstractApplicationBase.fn.onDomElementInitialized.call(this).done(function () {

            // init the title
            var $viewTitle = _this.$domElement.closest(".main-view").find("[data-role=header] .km-view-title");
            $viewTitle.text(_this.appDef.title.toUpperCase());

            // build the application
            _this.kendoMobileApplication = new kendo.mobile.Application($(document.body), {
                layout: "default-layout",
                transition: "slide",
                skin: "nova",
                // icon: {
                //     "" : '@Url.Content("~/content/mobile/AppIcon72x72.png")',
                //     "72x72" : '@Url.Content("~/content/mobile/AppIcon72x72.png")',
                //     "76x76" : '@Url.Content("~/content/mobile/AppIcon76x76.png")',
                //     "114x114" : '@Url.Content("~/content/mobile/AppIcon72x72@2x.png")',
                //     "120x120" : '@Url.Content("~/content/mobile/AppIcon76x76@2x.png")',
                //     "152x152" : '@Url.Content("~/content/mobile/AppIcon76x76@2x.png")'
                // }
            });
        });
    },

    /**
     *
     * @param viewFileName
     */
    loadView: function (viewFileName) {
        var _this = this;
        var viewName = viewFileName.camelCase().capitalize();
        var viewId = viewName + "-" + expresso.util.Util.guid();
        var viewHTMLPath = this.applicationPath + "/" + viewFileName + ".html";
        var viewScriptPath = this.applicationPath + "/" + viewFileName + ".js";
        var viewClass = this.applicationPath.replace(/\//g, '.') + "." + viewName;
        if (!viewClass.startsWith("expresso")) {
            // add the name of the site
            viewClass = expresso.Common.getSiteName() + "." + viewClass;
        }

        // add the view to the body
        var $view = $("<div data-role='view' id='" + viewId + "'></div>").appendTo($("body"));

        // load the HTML file
        expresso.Common.loadHTML($view, viewHTMLPath, this.labels, false).done(function () {

            // load the Javascript file
            expresso.Common.getScript(viewScriptPath).done(function () {
                // instanciate the view object
                var viewObject = eval("new " + viewClass + "(_this)");

                // initialize the view
                _this.initView(viewObject, $view);

                // navigate to the new view
                viewObject.isReady().done(function () {
                    //console.log("Navigating to view " + viewId);
                    _this.kendoMobileApplication.navigate("#" + viewId);
                });
            });
        });
    },

    loadModal: function (action, resource) {
        var _this = this;
        var viewName = "Modal" + action.name.camelCase().capitalize();
        var viewHTMLPath = this.applicationPath + "/" + "modal_" + action.name + ".html";
        var viewScriptPath = this.applicationPath + "/" + "modal_" + action.name + ".js";
        var viewClass = this.applicationPath.replace(/\//g, '.') + "." + viewName;
        if (!viewClass.startsWith("expresso")) {
            // add the name of the site
            viewClass = expresso.Common.getSiteName() + "." + viewClass;
        }

        // add the view to the body
        var $view = $("<div data-role='modalview' style='width: 80%; height: 50%;'></div>").appendTo($("body"));

        // load the HTML file
        expresso.Common.loadHTML($view, viewHTMLPath, this.labels, false).done(function () {

            // load the Javascript file
            expresso.Common.getScript(viewScriptPath).done(function () {

                // instanciate the js object
                var viewObject = eval("new " + viewClass + "(_this)");

                // Patch pour avoir le model du .js.
                viewObject.model = viewObject.getModel();

                expresso.util.Model.initModel(viewObject).done(function () {

                    // Covert input with expresso_mobile_form
                    $view.children("div").kendoExpressoForm({
                        labels: _this.labels,
                        resource: resource,
                        model: viewObject.model
                    });

                    // Header
                    $view.prepend("" +
                        "<div data-role='header'>" +
                        "   <div data-role='navbar'>" +
                        "       <span>" + _this.getLabel(action.name + "WindowTitle") + "</span>" +
                        "       <a class='modalview-close-button' data-role='button' data-align='right'><span class='fa fa-close'></span></a>" +
                        "   </div>" +
                        "</div>");

                    // Footer
                    $view.append("" +
                        "<div data-role='footer'>" +
                        "    <div data-role='navbar'>" +
                        "        <button type='button' data-role='button' data-align='right' class='km-button modalview-cancel-button'><span class='fa fa-cross'></span>Annuler</button>" +
                        "        <button type='button' data-role='button' data-align='right' class='km-primary km-button modalview-action-button'><span class='fa fa-check'></span>Enregistrer</button>" +
                        "    </div>" +
                        "</div>");

                    // Ask Kendo to initialize widgets with data roles
                    kendo.init($view, kendo.mobile.ui, kendo.ui);

                    //Show modal view
                    $view.data("kendoMobileModalView").open();

                    $view.on("click", ".modalview-close-button", function () {
                        $view.data("kendoMobileModalView").close();
                        _this.refreshView();
                    });

                    $view.on("click", ".modalview-cancel-button", function () {
                        $view.data("kendoMobileModalView").close();
                        _this.refreshView();
                    });

                    $view.on("click", ".modalview-action-button", function () {
                        var params = _this.buildFormParams($view, viewObject.model);

                        _this.executionAction(action.name, resource, params).done(function () {
                            $view.data("kendoMobileModalView").close();
                            _this.refreshView();
                        });
                    });
                });
            });
        });
    },

    /**
     *
     * @param $form
     * @param model
     * @return {{}}
     */
    buildFormParams: function ($form, model) {
        var params = {};

        $.each(model.fields, function (index, value) {
            var fieldName = value.name;
            var fieldValue = $form.find("[name='" + value.name + "']").val();

            if (fieldName && fieldValue !== undefined) {
                params[fieldName] = fieldValue;
            }
        });

        return params;
    },

    /**
     * This method will be overridden by the resource manager.
     * Does nothing by default.
     */
    refreshView: function () {
    },

    /**
     *
     * @param view
     * @param $view
     */
    initView: function (view, $view) {
        view.initDOMElement($view);
    },

    /**
     * This method is called when the main application is requested to switch to another application
     */
    destroy: function () {
        expresso.layout.applicationbase.AbstractApplicationBase.fn.destroy.call(this);
    }
});
