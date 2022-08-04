(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoFilter = Widget.extend({
        $element: undefined,
        $rootFilterDiv: undefined,
        $visualDiv: undefined,
        $sourceDiv: undefined,

        // list of available fields
        fields: undefined,

        // operators
        operators: {
            common: ["eq", "neq", "isNull", "isNotNull"],
            stringOnly: ["startsWith", "doesNotStartWith", "endsWith", "contains", "doesNotContain",
                "isEmpty", "isNotEmpty", "trimCompare", "neqTrimCompare", "equalsIgnoreCase"],
            dateOnly: ["timestampEquals", "sameDayEquals", "truncGt", "truncLt", "truncLte", "truncGte"],
            dateAndNumber: ["gt", "lt", "lte", "gte"],
            stringAndNumber: ["in", "notIn"]
        },

        // options
        options: {
            name: "ExpressoFilter",

            // dataSource: {
            //   resourceName:
            //   resourcePath:
            //   resourceManager:
            //   appClassFieldMap:
            // }
            dataSource: undefined
        },
        // events: ["change"],

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);
            this.setOptions(options);

            //console.log("Options: " + JSON.stringify(this.options));
            this.$element = $(element);

            // convert
            this._convertElementToWidget();

            // add event listeners
            var _this = this;
            this.$visualDiv.on("click", ".exp-widget-filter-add-group-button", function () {
                var $group = $(this).closest(".exp-widget-filter-group");
                _this._addGroup($group);
            });
            this.$visualDiv.on("click", ".exp-widget-filter-add-rule-button", function () {
                var $group = $(this).closest(".exp-widget-filter-group");
                _this._addRule($group);
            });
            this.$visualDiv.on("click", " .exp-widget-filter-remove-button", function () {
                var $rule = $(this).closest(".exp-widget-filter-rule");
                var $group = $(this).closest(".exp-widget-filter-group");
                var $toBeRemoved = $rule.length ? $rule : $group;
                expresso.util.UIUtil.buildYesNoWindow(expresso.Common.getLabel("confirmTitle"),
                    expresso.Common.getLabel("confirmDeleteElement")).done(function () {
                    $toBeRemoved.remove();
                });
            });

            // highlight the closest group
            this.$visualDiv.on("mouseenter", ".exp-widget-filter-group", function () {
                // remove the highlight on all groups
                _this.$visualDiv.find(".exp-widget-filter-group").removeClass("exp-highlight-widget-filter-group");

                // put the highlight on the current group
                $(this).addClass("exp-highlight-widget-filter-group");
            });
            this.$visualDiv.on("mouseleave", ".exp-widget-filter-group", function () {
                // remove the highlight on the current group
                $(this).removeClass("exp-highlight-widget-filter-group");

                // put the highlight on the parent
                $(this).parent().closest(".exp-widget-filter-group").addClass("exp-highlight-widget-filter-group");
            });

            // init data source
            this.setDataSource(this.options.dataSource);
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            // wrap the input
            this.$element.wrap("<div class='k-widget exp-widget-filter'>" +
                "  <div class='exp-widget-filter-tabstrip'>" +
                "       <ul><li class='k-state-active' data-tab-class='exp-widget-filter-visual'>" + expresso.Common.getLabel("visualFilter") + "</li>" +
                "           <li  data-tab-class='exp-widget-filter-source'>" + expresso.Common.getLabel("sourceFilter") + "</li></ul>" +
                "       <div class='exp-widget-filter-visual'></div>" +
                "       <div class='exp-widget-filter-source' contenteditable='true'></div>" +
                "  </div>" +
                "</div>");

            this.$rootFilterDiv = this.$element.closest(".exp-widget-filter");
            //this.$rootFilterDiv = this.$element.parent();

            this.$element.hide();
            this.$visualDiv = this.$rootFilterDiv.find(".exp-widget-filter-visual");
            this.$sourceDiv = this.$rootFilterDiv.find(".exp-widget-filter-source");

            // add tabs for visual and code
            this.$rootFilterDiv.find(".exp-widget-filter-tabstrip").kendoTabStrip({
                animation: false,
                activate: function (e) {
                    var $li = $(e.item);
                    var tabClass = $li.data("tabClass");
                    if (tabClass == "exp-widget-filter-visual") {
                        // get the source and convert it to visual
                        var code = _this.$sourceDiv.text();
                        if (code.trim()) {
                            try {
                                code = JSON.parse(code);
                            } catch (e) {
                                console.error("Cannot convert filter value [" + code + "]", e);
                                code = null;
                            }
                        } else {
                            code = null;
                        }
                        _this.value(code);
                    } else { // exp-widget-filter-source
                        // get the visual and convert it to source
                        _this.$sourceDiv.text(JSON.stringify(_this.value()));
                    }
                }
            });

            // put the widget instance on the element
            this.$element.data("kendoExpressoFilter", this);
        },

        /**
         *
         * @param $div
         * @returns {*}
         * @private
         */
        _addGroup: function ($div) {
            var $group = $("<div class='exp-widget-filter-group'>" +
                "  <span class='fa fa-trash exp-widget-filter-remove-button'></span>" +
                "  <div>" +
                "    <button class='k-button exp-button exp-widget-filter-add-group-button' title='addFilterGroup' type='button'>" +
                "      <span class='fa fa-plus'><span data-text-key='addFilterGroup'></span></span>" +
                "    </button>" +
                "  </div>" +
                "  <div>" +
                "   <button class='k-button exp-button exp-widget-filter-add-rule-button' title='addFilterRule' type='button'>" +
                "      <span class='fa fa-plus'><span data-text-key='addFilterRule'></span></span>" +
                "   </button>" +
                "  </div>" +
                "    <select class='exp-widget-filter-logic'>" +
                "      <option value='and'>and</option>" +
                "      <option value='or'>or</option>" +
                "    </select>" +
                "</div>");
            expresso.Common.localizePage($group, expresso.Labels);
            expresso.util.UIUtil.buildDropDownList($group.find(".exp-widget-filter-logic"));
            return $group.appendTo($div);
        },

        /**
         *
         * @param $div
         * @returns {*}
         * @private
         */
        _addRule: function ($div) {
            var _this = this;

            var valueTemplate = "  <input name='exp-widget-filter-value' class='exp-widget-filter-value k-textbox'>";
            var $rule = $("<div class='exp-widget-filter-rule'>" +
                "  <select class='exp-widget-filter-field'></select>" +
                "  <select class='exp-widget-filter-operator'></select>" +
                valueTemplate +
                "  <span class='fa fa-trash exp-widget-filter-remove-button'></span>" +
                "</div>");
            expresso.Common.localizePage($rule, expresso.Labels);

            // convert operators
            // expresso.util.UIUtil.buildDropDownList($rule.find(".exp-widget-filter-operator"));
            $rule.find(".exp-widget-filter-operator").kendoDropDownList({
                change: function () {
                    var $currentRule = this.element.closest(".exp-widget-filter-rule");
                    var $value = $currentRule.find("input[name='exp-widget-filter-value']");

                    var value = this.value();
                    if (value) {
                        switch (value) {
                            case "isNull":
                            case "isNotNull":
                            case "isTrue":
                            case "isFalse":
                                $value.setval(null);
                                expresso.util.UIUtil.hideField($value, true, false);
                                break;
                            default:
                                expresso.util.UIUtil.hideField($value, false, false);
                                break;
                        }
                        expresso.util.UIUtil.setFieldReadOnly($value, false, false);
                    } else {
                        $value.setval(null);
                        expresso.util.UIUtil.setFieldReadOnly($value, true, false);
                    }
                }
            });

            // convert fields
            // expresso.util.UIUtil.buildDropDownList($rule.find(".exp-widget-filter-field"), this.fields);
            $rule.find(".exp-widget-filter-field").kendoComboBox({
                dataSource: new kendo.data.DataSource({data: this.fields}),
                dataValueField: "name",
                dataTextField: "label",
                valuePrimitive: true,
                filter: "contains",
                syncValueAndText: false,
                change: function () {
                    // console.log("Change field [" + this.value() + "] [" + this.text() + "]");

                    var $currentRule = this.element.closest(".exp-widget-filter-rule");
                    var kendoOperator = $currentRule.find("select.exp-widget-filter-operator").data("kendoDropDownList");
                    var $value = $currentRule.find("input[name='exp-widget-filter-value']");

                    // remove special value widget
                    var $dateValue = $currentRule.find("input[name='exp-widget-filter-value'].exp-widget-filter-date-value");
                    if ($dateValue.length) {
                        // reinsert the value
                        var $dateWidget = $dateValue.closest(".k-widget");
                        $(valueTemplate).insertAfter($dateWidget);

                        // delete the widget
                        $dateWidget.remove();
                    }

                    // set default state for value input
                    expresso.util.UIUtil.hideField($value, false, false);
                    $value.val(null);
                    $value.attr("type", "text");

                    var value = this.value();
                    if (!value) {
                        // get the custom text as value
                        value = this.text();

                        // set the value equals to the custom text
                        this.value(value);
                    }

                    if (value) {
                        var dataItem = this.dataItem();
                        var operatorDataSource = [];
                        if (dataItem && dataItem.type) {
                            // console.log("dataItem", dataItem);
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.common);
                            switch (dataItem.type) {
                                case "string":
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.stringOnly);
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.stringAndNumber);
                                    $value.attr("type", "text");
                                    break;

                                case "number":
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.dateAndNumber);
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.stringAndNumber);
                                    $value.attr("type", "number");
                                    break;

                                case "date":
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.dateOnly);
                                    operatorDataSource.push.apply(operatorDataSource, _this.operators.dateAndNumber);

                                    $value.setval(null);
                                    $value.addClass("exp-widget-filter-date-value").removeClass("k-textbox");

                                    // allow text (not only date). Ex: TODAY, FIRST_DAY_OF_MONTH, etc
                                    var options = [{id: "TODAY", label: expresso.Common.getLabel("today")},
                                        {id: "YESTERDAY", label: expresso.Common.getLabel("yesterday")},
                                        {id: "TOMORROW", label: expresso.Common.getLabel("tomorrow")},
                                        {id: "LASTWEEK", label: expresso.Common.getLabel("lastWeek")},
                                        {id: "THISWEEK", label: expresso.Common.getLabel("thisWeek")},
                                        {id: "NEXTWEEK", label: expresso.Common.getLabel("nextWeek")},
                                        {id: "LASTMONTH", label: expresso.Common.getLabel("lastMonth")},
                                        {id: "THISMONTH", label: expresso.Common.getLabel("thisMonth")},
                                        {
                                            id: "FIRST_DAY_OF_MONTH",
                                            label: expresso.Common.getLabel("firstDayOfMonth")
                                        },
                                        {
                                            id: "LAST_DAY_OF_MONTH",
                                            label: expresso.Common.getLabel("lastDayOfMonth")
                                        },
                                        {
                                            id: "FIRST_DAY_OF_YEAR",
                                            label: expresso.Common.getLabel("firstDayOfYear")
                                        },
                                        {id: "LAST_DAY_OF_YEAR", label: expresso.Common.getLabel("lastDayOfYear")},
                                        {id: "LAST_SUNDAY", label: expresso.Common.getLabel("lastSunday")},
                                        {id: "LAST_MONDAY", label: expresso.Common.getLabel("lastMonday")}];
                                    $value.kendoComboBox({
                                        dataSource: new kendo.data.DataSource({data: options}),
                                        dataValueField: "id",
                                        dataTextField: "label",
                                        valuePrimitive: true,
                                        filter: "contains",
                                        syncValueAndText: true
                                    });
                                    break;

                                case "boolean":
                                    operatorDataSource = ["isTrue", "isFalse"]; // only possible value
                                    $value.setval(null);
                                    expresso.util.UIUtil.hideField($value, true, false);
                                    break;

                                default:
                                    // convert the value to default
                                    $value.attr("type", "text");
                                    break;
                            }
                        } else {
                            // put back all operators are we do not know the type
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.common);
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.stringOnly);
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.stringAndNumber);
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.dateOnly);
                            operatorDataSource.push.apply(operatorDataSource, _this.operators.dateAndNumber);
                        }

                        // set the operator data source
                        kendoOperator.value(null);
                        kendoOperator.setDataSource(new kendo.data.DataSource({data: operatorDataSource}));

                        // operator and value are writable
                        // expresso.util.UIUtil.setFieldReadOnly($currentRule.find("select.exp-widget-filter-operator"), false, false);
                        expresso.util.UIUtil.setFieldReadOnly($value, false, false);
                    } else {
                        // operator and value are readonly
                        kendoOperator.value(null);
                        kendoOperator.setDataSource(new kendo.data.DataSource({data: []}));
                        //expresso.util.UIUtil.setFieldReadOnly($currentRule.find("select.exp-widget-filter-operator"), true, false);
                        expresso.util.UIUtil.setFieldReadOnly($value, true, false);
                    }
                }
            });

            return $rule.appendTo($div);
        },

        /**
         * Get filter for the group
         * @param $group
         * @private
         */
        _getFilter: function ($group) {
            var _this = this;
            var filter;
            if ($group && $group.length) {
                var $rules = $group.children(".exp-widget-filter-rule");
                var $groups = $group.children(".exp-widget-filter-group");
                if ($rules.length || $groups.length) {
                    filter = {
                        logic: $group.children(".exp-widget-filter-logic").find("select.exp-widget-filter-logic").getval(),
                        filters: []
                    };
                    $rules.each(function () {
                        var $rule = $(this);
                        var field = $rule.find("select.exp-widget-filter-field").getval();
                        var operator = $rule.find("select.exp-widget-filter-operator").getval();
                        var value = $rule.find("input[name='exp-widget-filter-value']").getval();
                        if (field) {
                            // special case for boolean
                            if (operator == "isTrue") {
                                operator = "eq";
                                value = true;
                            } else if (operator == "isFalse") {
                                operator = "eq";
                                value = false;
                            }

                            // empty value are null
                            if (!value && value !== 0 && value !== false) {
                                value = null;
                            }
                            filter.filters.push({field: field, operator: operator, value: value});
                        }
                    });
                    $groups.each(function () {
                        var $subGroup = $(this);
                        var subGroupFilter = _this._getFilter($subGroup);
                        if (subGroupFilter) {
                            filter.filters.push(subGroupFilter);
                        }
                    });
                }
            }
            return filter;
        },

        /**
         * Add the filter in the group
         * @param $div
         * @param filter
         * @private
         */
        _addFilter: function ($div, filter) {
            if (filter) {
                if (filter.logic && filter.filters) {
                    var $group = this._addGroup($div);
                    $group.children(".exp-widget-filter-logic").find("select.exp-widget-filter-logic").setval(filter.logic);
                    var _this = this;
                    $.each(filter.filters, function () {
                        _this._addFilter($group, this);
                    });
                } else if (filter.field) {
                    var $rule = this._addRule($div);
                    $rule.find("select.exp-widget-filter-field").setval(filter.field);

                    // special case for boolean
                    var operator = filter.operator;
                    if (filter.value === true) {
                        operator = "isTrue";
                    } else if (filter.value === false) {
                        operator = "isFalse";
                    }
                    $rule.find("select.exp-widget-filter-operator").setval(operator);
                    $rule.find("input[name='exp-widget-filter-value']").setval(filter.value);
                }
            }
        },

        /**
         *
         * @param v
         * @returns {[]}
         */
        value: function (v) {
            if (v === undefined) {
                // getter
                return this._getFilter(this.$visualDiv.children(".exp-widget-filter-group"));
            } else {
                //setter

                // remove all previous filters
                this.$visualDiv.children(".exp-widget-filter-group").remove();
                this._addFilter(this.$visualDiv, v);

                // there must be at least one group
                if (!this.$visualDiv.children(".exp-widget-filter-group").length) {
                    if (this.options.dataSource) {
                        this._addGroup(this.$visualDiv);
                    }
                }
            }
        },

        /**
         *
         * @param options
         */
        setOptions: function (options) {
            $.extend(true, this.options, options);
        },

        /**
         *
         * @param dataSource
         */
        setDataSource: function (dataSource) {
            this.options.dataSource = dataSource;

            // remove all previous filters
            this.$visualDiv.children(".exp-widget-filter-group").remove();

            // add the first group
            if (dataSource) {

                // build an array with fields
                this.fields = [];
                for (var p in JSON.parse(JSON.stringify(dataSource.appClassFieldMap))) {
                    if (p != "type") {
                        var field = dataSource.appClassFieldMap[p];
                        field.label = field.label || field.name;
                        this.fields.push(field);
                    }
                }
                this.fields.sort(function (a, b) {
                    return a["label"].localeCompare(b["label"]);
                });
                this._addGroup(this.$visualDiv);
            }
        },

        /**
         *
         */
        destroy: function () {
            this.$visualDiv = null;
            this.$element = null;

            Widget.fn.destroy.call(this);
        }
    });

    ui.plugin(ExpressoFilter);
}(jQuery, window.kendo));
