var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Tree Filter class
 */
expresso.layout.resourcemanager.TreeFilter = expresso.layout.resourcemanager.Filter.extend({
    wsListPathOrData: null, // path the service or array of data
    fieldName: null,  // name of the field used to filter the data
    parentIdFieldName: null, // parent field name

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.Filter.fn.initDOMElement.call(this, $domElement);
        var _this = this;

        var $treeView = $("<div class='exp-filter-tree-view'></div>").appendTo(this.$domElement.find(".exp-filter-content"));

        // Build a Tree View
        var options = {
            parentIdFieldName: this.parentIdFieldName,
            check: function () {
                _this.reloadGrid();
            },
            select: function (e) {

                // must clear all checkboxes
                _this.clearFilters();

                // check this one
                $(e.node).find("[type=checkbox]").prop("checked", true);

                // then reload the grid
                _this.reloadGrid();
            }
        };

        $treeView.hide();
        this.addPromise(expresso.util.UIUtil.buildTreeView($treeView, this.wsListPathOrData, options).done(function (kendoTreeView) {
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
        this.resourceManager.reloadGrid();
    },

    /**
     * @return [] an array of KendoFilters
     */
    getFilters: function () {
        var _this = this;
        var filters = [];
        var $treeView = this.$domElement.find(".exp-filter-tree-view");
        var treeView = $treeView.data("kendoTreeView");
        var $selectedNodes = $treeView.find("[type=checkbox]:checked"); //,[type=checkbox]:indeterminate
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
        // console.log("setFilters", filters);

        // first clear all filters
        this.clearFilters();

        // then get the applicable filter
        var treeFilter = this.getTreeFilter(filters);
        if (treeFilter) {
            // console.log(treeFilter);
            var $treeView = this.$domElement.find(".exp-filter-tree-view");
            var treeView = $treeView.data("kendoTreeView");

            // check each checkbox
            $.each(treeFilter.filters, function () {
                var f = this;

                $treeView.find("li.k-item").each(function () {
                    var $li = $(this);
                    var dataItem = treeView.dataItem($li);
                    if (dataItem.id == f.value) { // value is the id of the dataItem
                        // expand
                        // treeView.expandTo(f.value);

                        // check each checkbox
                        $li.find("> :not(.k-group) [type=checkbox]").prop("checked", true);
                    }
                });
            });

            // now we need to set indeterminate when not all children in the group are selected
            $treeView.find("[type=checkbox]:checked").each(function () {
                var $checkbox = $(this);
                var $parentLi = $checkbox.closest("li.k-item");
                var $parentUl = $parentLi.closest("ul.k-group");
                if ($parentUl.length) {
                    $checkbox.parents("li.k-item").each(function () {
                        var $li = $(this);
                        $li.find("> div [type=checkbox]").each(function () {
                            var $parentCheckbox = $(this);
                            // console.log("checkbox: " + $parentCheckbox.is(":checked") + ":" + $parentCheckbox.prop("indeterminate"));
                            if (!$parentCheckbox.is(":checked")) {
                                $parentCheckbox.prop("indeterminate", true);
                            }
                        });
                    });
                }
            });
        }
    },

    /**
     *
     */
    getTreeFilter: function (filter) {
        var _this = this;
        var i, subFilter;

        if (filter) {
            // get the filter for the fieldName
            if (filter.logic == "or" && filter.filters) {
                for (i = 0; i < filter.filters.length; i++) {
                    subFilter = filter.filters[i];
                    if (subFilter.field == _this.fieldName && !subFilter.filters) {
                        // got it
                        return filter;
                    }
                }
            }

            // if not found, go to children
            if (filter.filters) {
                for (i = 0; i < filter.filters.length; i++) {
                    subFilter = filter.filters[i];
                    var f = _this.getTreeFilter(subFilter);
                    if (f) {
                        return f;
                    }
                }
            }
        }
        return null;
    },

    /**
     *
     */
    clearFilters: function () {
        var $treeView = this.$domElement.find(".exp-filter-tree-view");
        var treeView = $treeView.data("kendoTreeView");

        // select none
        treeView.select(null);

        // must clear all checkboxes
        $treeView.find("[type=checkbox]").prop("checked", false).prop("indeterminate", false);
    },

    // @override
    destroy: function () {

        expresso.layout.resourcemanager.Filter.fn.destroy.call(this);
    }
});
