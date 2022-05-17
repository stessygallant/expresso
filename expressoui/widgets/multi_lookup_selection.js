(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoMultiLookupSelection = Widget.extend({
        $element: undefined,
        $wrapper: undefined,
        sourceListBox: undefined,
        targetListBox: undefined,
        originalDataItems: undefined,

        data: undefined,
        url: undefined,
        filter: undefined,
        autoLoad: undefined,
        readOnly: false,
        serverSideFiltering: undefined,

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoMultiLookupSelection).
            // The jQuery plugin would be jQuery.fn.kendoExpressoMultiLookupSelection
            name: "ExpressoMultiLookupSelection",
            readonly: false,
            field: undefined, // as defined in the app_class
            dataSource: { // only if field is not defined
                data: undefined,
                url: undefined,
                filter: undefined
            },
            value: undefined,
            dataValueField: "id",
            dataTextField: "label",
            sortable: false,
            autoLoad: false,
            serverSideFiltering: true
        },
        events: ["change"],

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);
            this.setOptions(options);

            //console.log("Options", this.options);
            this.$element = $(element);

            // convert
            this._convertElementToWidget();
        },

        /**
         *
         * @private
         */
        _setInitialValue: function () {
            var initialValue;
            if (this.options.value !== undefined) {
                initialValue = this.options.value;
            } else if (this.$element.val()) {
                initialValue = this.$element.val();
            }
            if (initialValue !== undefined) {
                this.value(initialValue);
            }

            // if (this.options.field && this.options.field.defaultValue) {
            //     targetData.push({id: this.options.field.defaultValue, label: this.options.field.defaultValue});
            // }
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            // wrap the input
            this.$element.wrap("<div class='k-widget exp-multilookupselection'></div>");
            this.$wrapper = this.$element.parent();

            // TARGET SELECT
            var $targetSelect = this.$element;
            var selectedGUID = expresso.util.Util.guid();
            $targetSelect.addClass("exp-lookup-selection").addClass("exp-lookup-selection-target")
                .attr("id", selectedGUID).attr("multiple", "multiple");

            // SOURCE SELECT
            var $sourceSelect = $("<select class='exp-lookup-selection exp-lookup-selection-source' multiple></select>");
            $sourceSelect.insertBefore($targetSelect);

            if (!this.data) {
                this.data = [];

                // INPUT SEARCH
                var $searchInput = $("<input type='search' class='exp-lookup-selection-input k-textbox' placeholder='searchPlaceHolder'>");
                $searchInput.insertBefore($sourceSelect);

                //when we release 'enter' key
                $searchInput.on("keyup search", function (e) {
                    e.preventDefault();
                    if (e.type == "search" || e.keyCode == 13) {
                        var term = $(this).val();
                        _this._search(term);
                    }
                });
            }

            // utility method to be called when there is a change
            var triggerChange = function () {
                // allow the widget to apply the changes
                window.setTimeout(function () {
                    _this._refreshValue(true);
                    if (_this.options.change) {
                        _this.options.change();
                    }
                }, 10);
            }

            // create a source list box
            var sourceDefaultOptions = {
                dataValueField: this.options.dataValueField,
                dataTextField: this.options.dataTextField,
                valuePrimitive: true,
                connectWith: selectedGUID,
                selectable: "multiple",
                draggable: true,
                dropSources: [selectedGUID],
                dataSource: {
                    data: this.data
                },
                dataBound: function () {
                    if (_this.readOnly) {
                        _this.readonly(_this.readOnly);
                    }
                },
                toolbar: {
                    tools: ["transferTo", "transferFrom", "transferAllTo", "transferAllFrom"]
                }
            };

            // create the widget
            this.sourceListBox = $sourceSelect.kendoListBox(sourceDefaultOptions).data("kendoListBox");

            // create target list box
            var targetDefaultOptions = {
                dataValueField: this.options.dataValueField,
                dataTextField: this.options.dataTextField,
                valuePrimitive: true,
                selectable: "multiple",
                add: function () {
                    triggerChange();
                },
                remove: function () {
                    triggerChange();
                },
                dataBound: function () {
                    if (_this.readOnly) {
                        _this.readonly(_this.readOnly);
                    }
                },
                toolbar: _this.options.sortable ? {
                    position: "right",
                    tools: ["moveUp", "moveDown"]
                } : undefined
            };
            this.targetListBox = $targetSelect.kendoListBox(targetDefaultOptions).data("kendoListBox");
            this._setInitialValue();

            // set the readonly
            if (this.options.readonly) {
                this.readonly(this.options.readonly);
            }

            if (this.autoLoad) {
                this._search("");
            }
        },

        /**
         *
         * @param term
         * @private
         */
        _search: function (term) {
            var _this = this;
            if (this.serverSideFiltering) {
                var filter = expresso.Common.buildKendoFilter(this.filter, {pageSize: 1000})
                    + "&term=" + encodeURIComponent(term);
                return expresso.Common.sendRequest(this.url + "/search", null, null, filter,
                    {waitOnElement: this.$wrapper}).done(function (dataItems) {

                    // remove selected target from source
                    var selected = _this.targetListBox.dataItems();
                    dataItems = $.grep(dataItems, function (a) {
                        return $.grep(selected, function (b) {
                            return a.id == b.id;
                        }).length == 0;
                    });

                    _this.sourceListBox.setDataSource(new kendo.data.DataSource({
                        data: expresso.Common.updateDataValues(dataItems, _this.options.labels)
                    }));
                });
            } else {
                if (this.originalDataItems === undefined) {
                    this.originalDataItems = this.sourceListBox.dataItems();
                }

                var dataItems;
                if (term) {
                    dataItems = $.grep(this.originalDataItems, function (d) {
                        return d.label.toLowerCase().indexOf(term.toLowerCase()) != -1;
                    });
                } else {
                    dataItems = this.originalDataItems;
                }
                this.sourceListBox.setDataSource(new kendo.data.DataSource({
                    data: dataItems
                }));
            }
        },

        /**
         *
         * @param v
         * @returns {[]}
         */
        value: function (v) {
            // console.log("value", v);
            var values;
            if (v === undefined) {
                // getter
                return this.$element.val();
            } else {
                //setter
                this.targetListBox.clearSelection();
                if (v === null) {
                    // skip
                } else {
                    if (v.length !== undefined) {
                        values = v;
                    } else if (v.indexOf && v.indexOf(",") != -1) {
                        values = v.split(",");
                    } else {
                        values = [v];
                    }

                    // from the ID, add the object to
                    var _this = this;
                    if (this.data && this.data.length) {
                        // get the object for each data
                        var dataItems = [];
                        _this.$.each(values, function () {
                            var id = this;
                            var dataItem = $.grep(_this.data, function (a) {
                                return a.id == id;
                            })[0];
                            dataItems.push(dataItem);
                        });
                        _this.targetListBox.setDataSource(new kendo.data.DataSource({
                            data: dataItems
                        }));
                    } else {
                        // get the list of objects
                        expresso.Common.sendRequest(this.url, null, null,
                            expresso.Common.buildKendoFilter({"id": Array.from(values)})).done(function (result) {
                            _this.targetListBox.setDataSource(new kendo.data.DataSource({
                                data: expresso.Common.updateDataValues(result.data, _this.options.labels)
                            }));
                        });
                    }
                }
            }
        },

        /**
         * Get the list of selected data items
         */
        dataItems: function () {
            return this.targetListBox.dataItems();
        },

        /**
         *
         * @param [userTriggered] default is false
         * @private
         */
        _refreshValue: function (userTriggered) {
            var dataItems = this.targetListBox.dataItems();
            // console.log(dataItems);

            // if the option does not exist: create and select it
            var _this = this;
            this.$element.find("option").prop("selected", false);
            // select (or unselect) the option
            $.each(dataItems, function () {
                var dataItem = this;
                var $option = _this.$element.find("option[value='" + dataItem[_this.options.dataValueField] + "']");
                if (!$option.length) {
                    $option = $("<option value='" + dataItem[_this.options.dataValueField] + "'>" +
                        dataItem[_this.options.dataTextField] + "</option>").appendTo(_this.$element);
                }
                $option.prop("selected", true);
            });
            this.$element.trigger("change");
            this.trigger("change", {userTriggered: userTriggered});
        },

        /**
         *
         * @param readonly
         */
        readonly: function (readonly) {
            this.readOnly = readonly;
            this.targetListBox.enable(".k-item", !readonly);
            this.sourceListBox.enable(".k-item", !readonly);
            if (readonly) {
                this.$wrapper.find(".k-listbox-toolbar,.exp-lookup-selection-input").hide();
            } else {
                this.$wrapper.find(".k-listbox-toolbar,.exp-lookup-selection-input").show();
            }
        },

        /**
         *
         * @param options
         */
        setOptions: function (options) {
            $.extend(true, this.options, options);

            var reference = (this.options.field ? this.options.field.values || this.options.field.reference : null) || {};
            this.data = (this.options.dataSource ? this.options.dataSource.data : null) || reference.data;
            this.url = (this.options.dataSource ? this.options.dataSource.url : null) || reference.wsPath;
            this.filter = (this.options.dataSource ? this.options.dataSource.filter : null) || reference.filter;
            this.autoLoad = (reference.autoLoad !== undefined ? reference.autoLoad : this.options.autoLoad);
            this.serverSideFiltering = (reference.serverSideFiltering !== undefined ? reference.serverSideFiltering : this.options.serverSideFiltering);
        },

        /**
         *
         * @param dataSource
         */
        setDataSource: function (dataSource) {
            // avoid null
            dataSource = dataSource || [];

            if ($.isArray(dataSource)) {
                dataSource = {data: dataSource};
            }

            // set the source
            this.sourceListBox.setDataSource(
                new kendo.data.DataSource({
                    data: dataSource.data
                }));

            // set the target
            this.targetListBox.setDataSource(
                new kendo.data.DataSource({
                    data: []
                }));
            this._refreshValue();
        },

        /**
         *
         */
        destroy: function () {
            this.$wrapper = null;
            this.$element = null;
            this.data = null;
            this.url = null;
            this.filter = null;

            this.sourceListBox.destroy();
            this.sourceListBox = null;
            //this.targetListBox.destroy();
            this.targetListBox = null;

            Widget.fn.destroy.call(this);
        }
    });

    ui.plugin(ExpressoMultiLookupSelection);
}(jQuery, window.kendo));
