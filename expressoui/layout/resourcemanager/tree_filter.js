var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Tree Filter class
 */
expresso.layout.resourcemanager.TreeFilter = expresso.layout.resourcemanager.Filter.extend({
    // @override
    initDOMElement: function ($domElement, options) {
        expresso.layout.resourcemanager.Filter.fn.initDOMElement.call(this, $domElement, options);
        var _this = this;

        // complete the options
        options = $.extend({}, {
            wsListPathOrData: null, // path the service or array of data
            fieldName: null,  // name of the field used to filter the data
            parentIdFieldName: null, // parent field name
        }, options);

        var $treeView = $("<div class='exp-filter-tree-view'></div>").appendTo(this.$domElement.find(".exp-filter-content"));

        // Build a Tree View
        options = $.extend({}, options, {
            check: function (e) {
                _this.reloadGrid();
            },
            select: function (e) {
                var kendoTreeView = e.sender;

                // must clear all checkboxes
                $treeView.find("[type=checkbox]").prop("checked", false).prop("indeterminate", false);

                // check this one
                $(e.node).find("[type=checkbox]").prop("checked", true);

                // expand it to show the selected node
                // kendoTreeView.expand(e.node);

                _this.reloadGrid();
            }
        });

        $treeView.hide();
        this.addPromise(expresso.util.UIUtil.buildTreeView($treeView, options.wsListPathOrData, options).done(function (kendoTreeView) {
            // we define "extended: true" for all "li" to be able to select all leaves.
            // but at first we want to show them collapse at first
            kendoTreeView.collapse(".k-item");
            $treeView.show();
        }));
    },

    /**
     *
     */
    reloadGrid: function () {
        this.resourceManager.sections.grid.loadResources();
    },

    /**
     * @return [] an array of KendoFilters
     */
    getFilters: function () {
        var _this = this;
        var filters = [];
        var $treeView = this.$domElement.find(".exp-tree-view");
        var treeView = $treeView.data("kendoTreeView");
        var $selectedNodes = $treeView.find("[type=checkbox]:checked");
        if ($selectedNodes.length) {
            $selectedNodes.each(function () {
                var dataItem = treeView.dataItem($(this).closest("li"));
                filters.push({field: _this.fieldName, operator: "eq", value: dataItem.id});
            });
            // console.log(filters);
            return {logic: "or", filters: filters};
        } else {
            return null;
        }
    },

    // @override
    setFilters: function (filters) {
        // TODO
    },

    // @override
    destroy: function () {

        expresso.layout.resourcemanager.Filter.fn.destroy.call(this);
    }
});
