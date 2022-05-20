var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Filter class
 */
expresso.layout.resourcemanager.Filter = expresso.layout.resourcemanager.SectionBase.extend({

    // @override
    initDOMElement: function ($domElement, options) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        this.$domElement.append("<div class='exp-filter-title'></div><div class='exp-filter-content'></div>");
        this.$domElement.find(".exp-filter-title").text(options && options.title ? options.title : "");
    },

    /**
     * @return [] an array of KendoFilters
     */
    getFilters: function () {
        alert("getFilters to be implemented by the subclass")
    },

    /**
     *
     * @param filters
     */
    setFilters: function (filters) {
        alert("setFilters to be implemented by the subclass")
    },

    // @override
    resizeContent: function () {
        expresso.layout.resourcemanager.SectionBase.fn.resizeContent.call(this);

        this.$domElement.find(".exp-filter-content").height(this.$domElement.height() -
            this.$domElement.find(".exp-filter-title").outerHeight(true));
    },

    // @override
    destroy: function () {
        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    }
});
