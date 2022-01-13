var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Filter class
 */
expresso.layout.resourcemanager.Filter = expresso.layout.resourcemanager.SectionBase.extend({

    // initial filter setting for reset
    initialFilters: undefined,

    // promise when the filter is ready (initializeForm is done)
    filterReadyPromise: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        var _this = this;

        // set default value if needed
        this.initialFilters = {};

        var $div = $domElement;

        $div.prepend("<div class='filter-title'>" +  this.getLabel("filterTitle"), + "</div>");

        var $footer = $("<div class='exp-footer'></div>");
        $div.append($footer);

        // add buttons to the window
        var $buttons = $("<div class='exp-buttons-div'>" + this.getButtons() + "</div>");
        $footer.append($buttons);

        // set event listener on the SUBMIT button
        this.$domElement.on("click", "button.exp-submit-filter-button", function (e) {
            e.preventDefault();
            e.stopPropagation();
            _this.submitFilter();
        });

        // set event listener on the RESET button
        this.$domElement.on("click", "button.exp-reset-filter-button", function (e) {
            e.preventDefault();
            e.stopPropagation();
            _this.resetFilter();
        });

        // execute after the end of this call
        this.filterReadyPromise = $.Deferred();
        setTimeout(function () {
            // when it is ready, take a snapshot
            $.when(_this.isReady()).done(function () {
                _this.initialFilters = _this.getFilterParams();
                _this.filterReadyPromise.resolve();
            });
        }, 10);
    },

    // @override
    getButtons: function () {
        var buttons = "<button type='submit' class='k-primary k-button exp-filter-button exp-submit-filter-button'>Filtrer</button>" +
            "<button type='reset' class='k-button exp-filter-button exp-reset-filter-button'>Réinitialiser</button>";
        return buttons;
    },

    /**
     * Reset the filter section to the initial state
     */
    resetFilter: function () {
        // initialize all filters with the default value
        this.setFilterParams(this.initialFilters);
        this.submitFilter();
    },

    /**
     * Submit the filter form
     */
    submitFilter: function () {
        var _this = this;
        $.when(this.filterReadyPromise).done(function () {
            _this.resourceManager.sections.grid.loadResources();
            // _this.resourceManager.eventCentral.publishEvent(_this.RM_EVENTS.FILTER_CHANGED, data);
        });
    },

    /**
     * By default, get the value of all form elements
     * @return []{*} an object containing the filters.
     */
    getFilterParams: function () {
        return this.$domElement.serializeObject(true);
    },

    /**
     * Set the filters and publish the event
     * @param filterParams
     */
    setFilterParams: function (filterParams) {
        // set the new filter params only if they are valid
        //console.log("setFilterParams: " + JSON.stringify(filterParams));
        expresso.util.UIUtil.resetForm(this.$domElement);
        expresso.util.UIUtil.initializeForm(this.$domElement, filterParams);
    },

    /**
     * @return [] an array of KendoFilters
     */
    getKendoFilters: function () {
        var gridFilters = [];
        var filterParams = this.getFilterParams();
        for (var f in filterParams) {
            gridFilters.push({field: f, operator: "eq", value: filterParams[f]});
        }
        return gridFilters;
    },

    // @override
    destroy: function () {
        this.initialFilters = null;
        this.filterReadyPromise = null;

        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    }
});
