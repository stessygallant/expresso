var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.externalapp = expresso.layout.externalapp || {};

expresso.layout.externalapp.ExternalAppBase = expresso.layout.applicationbase.ApplicationBase.extend({
    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.ApplicationBase.fn.initDOMElement.call(this, $domElement);
    },

    // @override
    resizeContent: function () {
        expresso.layout.applicationbase.ApplicationBase.fn.resizeContent.call(this);
        //console.log("resizing iframe");
        var $iframe = this.$domElement;
        $iframe.attr("width", $iframe.parent().width());
        $iframe.attr("height", $iframe.parent().height());
    },

    // @override
    render: function ($div, src) {
        var _this = this;
        var $iframe = $("<iframe class='main-iframe'></iframe>");
        $iframe.appendTo($div);
        _this.initDOMElement($iframe);

        $iframe.attr("src", src);
    },

    // @override
    destroy: function () {
        expresso.layout.applicationbase.ApplicationBase.fn.destroy.call(this);
    }
});
