(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    var ExpressoMultiCheckbox = Widget.extend({
        $element: undefined,
        $wrapper: undefined,

        data: undefined,
        url: undefined,
        filter: undefined,
        autoLoad: undefined,
        readOnly: false,
        activeOnly: true,

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoMultiCheckbox).
            // The jQuery plugin would be jQuery.fn.kendoExpressoMultiCheckbox
            name: "ExpressoMultiCheckbox",
            readonly: false,
            field: undefined, // as defined in the app_class
            dataSource: { // only if field is not defined
                data: undefined,
                url: undefined,
                filter: undefined
            },
            value: undefined,
            autoLoad: true,
            showHeader: true,
            activeOnly: true,
            dataValueField: "id",
            dataTextField: "label"
        },
        events: ["change"],

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);
            this.setOptions(options);

            //console.log("Options: " + JSON.stringify(this.options));
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
                if (this.url) {
                    var values;
                    if (initialValue === null) {
                        values = initialValue;
                    } else if (initialValue.length !== undefined) {
                        values = initialValue;
                    } else if (initialValue.indexOf && initialValue.indexOf(",") != -1) {
                        values = initialValue.split(",");
                    } else {
                        values = [initialValue];
                    }

                    // make sure the values already exists in the datasource
                    var _this = this;
                    var promises = [];
                    $.each(values, function () {
                        var v = this;
                        if (!_this.$wrapper.find(".content input[type=checkbox][value='" + v + "']").length) {
                            // if not found, get it (it works only if the URL is a resource and v an id
                            promises.push(expresso.Common.sendRequest(_this.url + "/" + v).done(function (dataItem) {
                                _this._addNewDataItem(dataItem);
                            }));
                        }
                    });

                    // then set the values
                    $.when.apply(null, promises).then(function () {
                        _this.value(values);
                    });
                } else {
                    this.value(initialValue);
                }
            }
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            // make sure it is a multiple select
            this.$element.attr("multiple", "multiple");

            // wrap the input
            this.$element.hide();
            this.$element.wrap("<div class='k-widget exp-multicheckbox'></div>");
            this.$wrapper = this.$element.parent();

            // create the fieldset
            var $div = $("<div class='exp-multicheckbox-div'><div class='header'></div><div class='content'></div></div>").appendTo(this.$wrapper);

            var $content = $div.children(".content");

            var $header = $div.children(".header");
            if (this.options.showHeader === false) {
                $header.hide();
            } else {
                // add a search input
                var $searchInput = $("<input type='search' class='k-textbox' placeholder='searchPlaceHolder'>").appendTo($header);

                // add the checkall in the legend
                $header.append(expresso.util.UIUtil.buildCheckBox("checkAll", expresso.Common.getLabel("checkAll")));
                var $checkAllCheckbox = $header.find("input[type=checkbox]");

                // listen to the search input
                $searchInput.on("change paste keyup search", function () {
                    var text = this.value.trim().latinize().toLowerCase();

                    // always clear the checkbox
                    $checkAllCheckbox.prop("checked", false);

                    if (text) {
                        // hide is no match with the label
                        $content.find("label").each(function () {
                            var $label = $(this);
                            if ($label.text().latinize().toLowerCase().indexOf(text) == -1) {
                                $label.parent().hide();
                            } else {
                                $label.parent().show();
                            }
                        });
                    } else {
                        // show all
                        $content.find("label").parent().show();
                    }
                });

                // listen to checkAll checkbox
                $checkAllCheckbox.on("click", function () {
                    // select only visible
                    $content.find("input[type=checkbox]:visible").prop("checked", this.checked);
                    _this._refreshValue(true);
                });
            }

            // listen to checkboxes
            $content.on("change", "input[type=checkbox]", function () {
                _this._refreshValue(true);
            });

            // set the readonly
            if (_this.options.readonly) {
                _this.readonly(this.options.readonly);
            }

            // then set the datasource
            this.setDataSource(this).done(function () {
                _this._setInitialValue();
            });
        },

        /**
         *
         * @param dataSource
         */
        setDataSource: function (dataSource) {
            var _this = this;

            // avoid null
            dataSource = dataSource || [];

            // empty all checkboxes
            var $content = this.$wrapper.find(".content");
            $content.empty();

            // handle data, url, filter
            var $dataDeferred;

            if ($.isArray(dataSource)) {
                dataSource = {data: dataSource};
            }
            if (dataSource.data) {
                $dataDeferred = $.Deferred().resolve(dataSource.data);
            } else if (dataSource.url && this.autoLoad) {
                $dataDeferred = expresso.Common.getValues(dataSource.url, dataSource.filter);
            } else {
                // console.warn("No dataSource defined");
                $dataDeferred = $.Deferred().resolve([]);
            }
            return $dataDeferred.done(function (data) {
                $.each(data, function () {
                    _this._addNewDataItem(this);
                });
            });
        },

        /**
         *
         * @param dataItem
         * @private
         */
        _addNewDataItem: function (dataItem) {
            // add the checkboxes
            if (!dataItem.deactivationDate || !this.activeOnly) {
                var $checkboxDiv = expresso.util.UIUtil.buildCheckBox(name, dataItem[this.options.dataTextField] +
                    (dataItem.deactivationDate ? " *" : ""),
                    dataItem[this.options.dataValueField]).appendTo(this.$wrapper.find(".content"));

                // add all possible values to $select
                $("<option value='" + dataItem[this.options.dataValueField] + "'>" +
                    dataItem[this.options.dataTextField] + "</option>").appendTo(this.$element);
                $checkboxDiv.find("input").data("dataItem", dataItem);

                if (this.readOnly) {
                    expresso.util.UIUtil.setFieldReadOnly($checkboxDiv.find("input"));
                }
            }
        },

        /**
         *
         * @param v
         * @returns {[]}
         */
        value: function (v) {
            var values;
            if (v === undefined) {
                // getter
                values = [];
                this.$wrapper.find(".content input[type=checkbox]:checked").each(function () {
                    values.push(parseInt(this.value));
                });
                return values;
            } else {
                //setter
                // uncheck all
                this.$wrapper.find(".content input[type=checkbox]").prop("checked", false);
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

                    var _this = this;
                    $.each(values, function () {
                        _this.$wrapper.find(".content input[type=checkbox][value='" + this + "']").prop("checked", true);
                    });
                    this._refreshValue();
                }
            }
        },

        /**
         * Get the list of selected data items
         */
        dataItems: function () {
            var dataItems = [];
            this.$wrapper.find(".content input[type=checkbox]:checked").each(function () {
                var dataItem = $(this).data("dataItem");
                dataItems.push(dataItem);
            });
            return dataItems;
        },

        /**
         *
         * @param [userTriggered] default is false
         * @private
         */
        _refreshValue: function (userTriggered) {
            var _this = this;
            var values = this.value();
            this.$element.find("option").prop("selected", false);
            // select (or unselect) the option
            $.each(values, function () {
                _this.$element.find("option[value='" + this + "']").prop("selected", true);
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
            expresso.util.UIUtil.setFormReadOnly(this.$wrapper.find(".content"), readonly);
            expresso.util.UIUtil.setFieldReadOnly(this.$wrapper.find(".header input[type=checkbox]"), readonly);

            if (readonly) {
                this.$wrapper.find(".header").hide();
            } else {
                this.$wrapper.find(".header").show();
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
            this.url = (this.options.dataSource ? this.options.dataSource.url : null) || reference.resourcePath;
            this.filter = (this.options.dataSource ? this.options.dataSource.filter : null) || reference.filter;
            this.autoLoad = (reference.autoLoad !== undefined ? reference.autoLoad : this.options.autoLoad);
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

            Widget.fn.destroy.call(this);
        }
    });

    ui.plugin(ExpressoMultiCheckbox);
}(jQuery, window.kendo));
