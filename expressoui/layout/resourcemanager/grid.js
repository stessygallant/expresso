﻿var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Grid class
 */
expresso.layout.resourcemanager.Grid = expresso.layout.resourcemanager.SectionBase.extend({


    TOOLBAR_BUTTONS: {
        EXCEL: {template: '<button type="button" class="k-button exp-button exp-always-active-button exp-excel-button" title="exportToExcel"><span class="fa fa-file-excel-o"><span data-text-key="exportToExcelButton"></span></span></button>'}
    },
    // reference to the grid object
    kendoGrid: undefined,

    // when local data, do not sync the data source automatically
    autoSyncGridDataSource: undefined,

    // define if the widget is a TreeList or a Grid
    hierarchical: undefined,

    // flag to display the number of records
    countRecords: true,

    // reference to the configuration object
    gridConfig: undefined,

    // grid columns configurations
    columns: undefined,
    defaultColumns: undefined,

    // map of the column by field name
    columnMap: undefined,

    // data source of the grid
    dataSource: undefined,

    // custom form (if defined by the user)
    customForm: undefined,

    // flag to display only active resources
    activeOnly: undefined,

    // for the deactivation column
    deactivateFieldName: "deactivationDate",

    // keep a list of all selected rows
    selectedRows: undefined,

    // if true, allow multiple selection
    multipleSelectionEnabled: undefined,

    // define the current view
    currentView: "default",

    // contains reference to resource manager used by this grid to be destroyed at the end
    referenceResourceManagers: undefined,

    // define if the grid must use virtual scrolling
    virtualScroll: undefined,

    // by default, duplicate is done server side
    serverSideDuplicate: undefined,

    // the last timestamp when the grid has been updated
    lastUpdateDate: undefined,

    // flag to indicate if we are already loading resources
    loadingResources: false,

    // flag to indicate if we are synching (saving) a resource
    synchingResource: false,

    // previous selection before the add
    previousSelectedResource: null,

    // the object contains a list of objects needed in an entity to avoid null pointer in Kendo Grid
    objectsNeededForColumns: undefined,

    // maximum length for the name of a grid preference
    maxGridFilterNameLength: 15,

    // this filter is always applied
    masterFilter: undefined,

    // reference to the context menu for preferences
    $preferencesMenu: undefined,

    // reference to the context menu for column
    $columnMenu: undefined,

    /**
     * Set the reference to the jquery object for the DOM element
     * @param $domElement reference to the jquery object for the DOM element
     */
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        this.selectedRows = [];

        // register a  listener to display a manager for reference
        var _this = this;
        this.referenceResourceManagers = {};
        $domElement.on("click", "a.reference", function (e) {
            e.preventDefault();
            _this.openReference($(this));
        });

        // register listener for tooltip
        $domElement.on("click", ".tooltip", function (e) {
            e.preventDefault();

            var tooltip = $(this).attr("title");
            expresso.util.UIUtil.buildMessageWindow(tooltip);
        });

        // options could contains configuration
        if (this.resourceManager.options.activeOnly !== undefined) {
            this.activeOnly = this.resourceManager.options.activeOnly;
        }

        // when using local data, do not sync datasource
        this.autoSyncGridDataSource = (this.resourceManager.options.autoSyncGridDataSource !== false);
        if (!this.autoSyncGridDataSource) {
            this.virtualScroll = false;
        }

        if (this.hierarchical === undefined && this.getParentId()) {
            this.hierarchical = true;
            this.multipleSelectionEnabled = false;
        }

        // we cannot support virtual scrolling if there is a group or aggregate
        if (this.virtualScroll === undefined) {
            this.virtualScroll = (!(this.getGroup() || this.getAggregate()) && !this.hierarchical && this.autoSyncGridDataSource);
        }

        if (this.serverSideDuplicate === undefined) {
            this.serverSideDuplicate = expresso.Common.getSiteNamespace().config.Configurations.serverSideDuplicate;
        }

        // defined if we support multiple selection in grid
        if (this.multipleSelectionEnabled === undefined) {
            if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
                if (this.resourceManager.options.multipleSelectionEnabled !== undefined) {
                    this.multipleSelectionEnabled = this.resourceManager.options.multipleSelectionEnabled;
                } else {
                    this.multipleSelectionEnabled = expresso.Common.getSiteNamespace().config.Configurations.supportMultipleGridSelection;
                }
            } else {
                this.multipleSelectionEnabled = false;
            }
        }

        // support auto refresh
        // only for main grid
        if (this.resourceManager.displayAsMaster) {
            if (expresso.Common.getSiteNamespace().config.Configurations.autoRefreshIntervalInSeconds) {
                // refresh the grid every n seconds if no updates have been made
                var autoRefreshIntervalInSeconds = expresso.Common.getSiteNamespace().config.Configurations.autoRefreshIntervalInSeconds;
                this.resourceManager.addInterval(function () {
                    if (_this.lastUpdateDate && (new Date().getTime() - _this.lastUpdateDate.getTime()) / 1000 >= autoRefreshIntervalInSeconds) {
                        //console.log("Reloading grid [" + _this.resourceManager.resourceName + "]");

                        // make sure not to display an error message
                        expresso.Common.doNotDisplayAjaxErrorMessage(true);
                        _this.loadResources().done(function () {
                            // put back the error message
                            expresso.Common.doNotDisplayAjaxErrorMessage(false);
                        });
                    }
                }, autoRefreshIntervalInSeconds / 2);
            }
        }

        // when a resource is updated, verify the button selections
        this.subscribeEvent(this.RM_EVENTS.RESOURCE_UPDATED, function (/*e, resource*/) {
            //console.log(_this.resourceManager.getResourceSecurityPath() + " - subscribeEvent RESOURCE_UPDATED");
            _this.verifySelection();
        });

        // Remove some options
        delete kendo.ui.FilterMenu.fn.options.operators.string.isempty;
        delete kendo.ui.FilterMenu.fn.options.operators.string.isnotempty;
        delete kendo.ui.FilterMenu.fn.options.operators.string.isnull;
        delete kendo.ui.FilterMenu.fn.options.operators.string.isnotnull;
    },

    /**
     * Initialize the grid when all sections all loaded
     */
    initGrid: function () {
        var _this = this;
        var $grid = this.$domElement;
        var i;
        var screenMode = expresso.Common.getScreenMode();

        // set the master filter if defined
        if (this.masterFilter === undefined) {
            this.masterFilter = this.resourceManager.options.filter;
        }

        // if there is a deactivationDate field, set the activeOnly
        if (this.activeOnly === undefined && this.resourceManager.model.fields[this.deactivateFieldName]) {
            this.activeOnly = true;
        }

        // defined if the manager defined a custom form
        this.customForm = this.resourceManager.sections["form"];

        var model = this.resourceManager.model;

        // update fields (cannot be done in ResourceManager because Labels not loaded)
        var field;
        for (var f in model.fields) {
            if (f) {
                field = model.fields[f];
                // add the validation message if needed
                if (field && field.validation && field.validation.required === true) {
                    field.validation.required = {message: this.getLabel("requiredField", {field: this.getLabel(f)})};
                }
            }
        }

        if (screenMode != expresso.Common.SCREEN_MODES.PHONE) {
            // get the columns
            this.columns = this.getColumns();

            // for standard grid, add standard columns
            if (this.addStandardColumns !== false) {
                // if there is no column for Id, put one hidden
                if (this.columns.length == 0 || this.columns[0].field != "id") {
                    this.columns.splice(0, 0, {
                        field: "id",
                        width: 70,
                        format: "{0:#######}",
                        hidden: true
                    });
                }

                // add columns for creationUser, creationDate, lastModifiedUser and lastModifiedDate
                $.each(["creationUserFullName", "creationDate", "lastModifiedUserFullName", "lastModifiedDate"], function () {
                    // try to find the column
                    var field = this;
                    var found = false;
                    for (var i = 0, l = _this.columns.length; i < l; i++) {
                        if (_this.columns[i].field == field) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        _this.columns.push({
                            field: field,
                            width: field.endsWith("Date") ? undefined : 120,
                            hidden: true
                        });
                    }
                });

                // always add an empty column to fill the void
                //this.columns.push({});
            }

            // add the selection checkbox columns
            if (this.multipleSelectionEnabled) {
                this.columns.splice(0, 0, {
                    template: "<input type='checkbox' class='selection k-checkbox' />",
                    width: 24,
                    headerTemplate: "<input type='checkbox' class='select-all k-checkbox' />",
                    reorderable: false
                });
            }

            // if the deactivation field exists in the model, add a column at the end
            //console.log(this.deactivateFieldName + ":" + this.resourceManager.model.fields[this.deactivateFieldName]);
            if (this.resourceManager.model.fields[this.deactivateFieldName] /*&& this.isUserAllowed("delete")*/) {
                //console.log("Adding deactivated column");
                this.columns.push({
                    field: this.deactivateFieldName,
                    headerTemplate: "",
                    template: "#: (" + _this.deactivateFieldName + "?'*':'')#",
                    width: "20px",
                    attributes: {"class": "center"},
                    filterable: false
                });
            }

            // update columns
            // do it reverse to be able to remove some columns
            var removeRestrictedColumn = true;
            for (i = this.columns.length - 1; i >= 0; i--) {
                var column = this.columns[i];

                if (!column.field) {
                    // empty column (usually to span the available space)
                    column.filterable = false;
                    column.sortable = false;
                } else {
                    field = model.fields[column.field];

                    var restrictedField = model.fields[column.field];
                    if (column.field.indexOf('.') != -1) {
                        // sub resource. get the first resource only
                        var fieldName = column.field.substring(0, column.field.indexOf('.')) + "Id";
                        restrictedField = model.fields[fieldName];
                    }

                    // if field is restricted, hide/remove it
                    if (restrictedField && restrictedField.restrictedRole) {
                        if (!expresso.Common.isUserInRole(restrictedField.restrictedRole)) {
                            //console.log("Restricted field[" + column.field + "] role[" + field.restrictedRole + "]");
                            if (removeRestrictedColumn) {
                                this.columns.splice(i, 1);
                                continue;
                            } else {
                                column.hidden = true;
                            }
                        }
                    }

                    // if a field is not filterable, cannot filter nor sort
                    if (field && field.filterable === false) {
                        column.sortable = false;
                        column.filterable = false;
                    }

                    // label are generated by Java: it cannot be sorted
                    if (column.field.endsWith("label")) {
                        column.sortable = false;
                        column.filterable = false;
                    }

                    // Ids column: show Labels (usually values)
                    if (field && column.field.endsWith("Ids")) {
                        // display the labels, but filter on id
                        var labelColumn = column.field.substring(0, column.field.length - 3) + "Labels";
                        column.template = column.template ||
                            "<div class='exp-grid-multiple-lines' title='#=(" + labelColumn
                            + "||'').replace(/,/g, \"\\r\\n\")#'>#=(" + labelColumn + "||'').replace(/,/g, \"<br>\")#</div>";
                    }

                    // Labels column (usually references as persons, etc)
                    if (field && column.field.endsWith("Labels")) {
                        column.template = column.template ||
                            "<div class='exp-grid-multiple-lines' title='#=(" + column.field
                            + "||'').replace(/,/g, \"\\r\\n\")#'>#=(" + column.field + "||'').replace(/,/g, \"<br>\")#</div>";
                    }

                    // remove the filter if it is set to false
                    if (column.filterable !== false) {
                        //  remove the filter
                        column.filterable = column.filterable || {};
                        column.filterable.cell = column.filterable.cell || {};
                        column.filterable.cell.showOperators = column.filterable.cell.showOperators || false;
                        // turn off autocomplete
                        column.filterable.cell.minLength = 9999;
                    }

                    // for each string column, change the filter operator to "contains" instead of equals
                    if (column.filterable !== false && (!field || !field.type || field.type == "string")) {
                        column.filterable = column.filterable || {};
                        column.filterable.cell = column.filterable.cell || {};
                        if (field && field.keyField) {
                            // keep equals by default
                            column.filterable.cell.operator = column.filterable.cell.operator || "eq";
                        } else {
                            column.filterable.cell.operator = column.filterable.cell.operator || "contains";
                        }
                    }

                    // date column
                    if (field && (field.type == "date" || column.field == "date" || column.field.endsWith("Date") ||
                        column.field.endsWith("DateTime") || column.field.endsWith("Timestamp"))) {
                        field.type = "date";
                        if (column.filterable !== false) {
                            column.filterable = column.filterable || {};
                            column.filterable.cell = column.filterable.cell || {};
                            column.filterable.cell.operator = column.filterable.cell.operator || "eq";
                        }
                        column.attributes = column.attributes || {};
                        column.attributes.class = "center";
                        if (field.timeOnly) {
                            column.format = column.format || "{0:HH:mm}";
                            column.width = column.width || 90;
                        } else {
                            column.format = column.format || (field.timestamp ? "{0:yyyy-MM-dd HH:mm}" : "{0:yyyy-MM-dd}");
                            column.width = column.width || (field.timestamp ? 130 : 110);
                        }
                    }

                    if ((field && field.type == "boolean") || column.fieldType == "boolean") {
                        column.template = column.template || "<input type='checkbox' #=" + column.field + " ? 'checked ' : '' # disabled>";
                        column.attributes = column.attributes || {};
                        column.attributes.class = "center";
                        if (column.filterable !== false) {
                            column.filterable = column.filterable || {};
                            column.filterable.messages = {
                                isTrue: expresso.Common.getLabel("yn_yes"),
                                isFalse: expresso.Common.getLabel("yn_no")
                            };
                        }
                        column.width = column.width || 110;
                    }

                    if (field && field.type == "number" && !column.field.endsWith("Id")) {
                        column.attributes = column.attributes || {};
                        column.attributes.class = "number";
                        column.format = column.format || "{0:n" + (field.decimals || 0) + "}";
                    }

                    // assign the title of the column
                    if (column.field && !column.title) {
                        column.title = _this.getLabel(column.field, null, false, true);
                    }

                    // handle mobile tag
                    if (column.phoneOnly && screenMode != expresso.Common.SCREEN_MODES.PHONE) {
                        column.hidden = true;
                    } else if (column.tabletOnly && screenMode != expresso.Common.SCREEN_MODES.TABLET) {
                        column.hidden = true;
                    } else if (column.desktopOnly && screenMode != expresso.Common.SCREEN_MODES.DESKTOP) {
                        column.hidden = true;
                    } else if (column.mobileOnly && screenMode == expresso.Common.SCREEN_MODES.DESKTOP) {
                        column.hidden = true;
                    } else if (column.hidePhone && screenMode == expresso.Common.SCREEN_MODES.PHONE) {
                        column.hidden = true;
                    }

                    // if the column end with No, verify if there is the same resource with Id
                    if (!column.reference && column.reference !== false) {
                        if (field && field.reference && field.reference.resourceManagerDef) {
                            column.reference = {
                                fieldName: field.reference.fieldName,
                                resourceManagerDef: field.reference.resourceManagerDef
                            };
                            //console.log("Adding FIELD reference to the grid", column.reference);
                        } else if (column.field.endsWith("No")) {
                            // try to find the mapping (directly or from a derived)
                            var idFieldName = column.field.substring(
                                (column.field.indexOf(".") != -1 ? column.field.lastIndexOf(".") + 1 : 0),
                                column.field.length - 2) + "Id";
                            var idField = model.fields[idFieldName];

                            // try to remove the last part and add Id
                            if (!idField && column.field.indexOf(".") != -1) {
                                idFieldName = column.field.substring(0, column.field.lastIndexOf(".")) + "Id";
                                idField = model.fields[idFieldName];
                            }

                            //console.log(column.field + " [" + idFieldName + "]", idField);

                            // add a reference to the column (but not if it is a user)
                            if (idField && idField.reference && idField.reference.resourceName != "user") {
                                column.reference = {
                                    fieldName: idField.reference.fieldName,
                                    resourceManagerDef: idField.reference.resourceManagerDef
                                };
                                //console.log("Adding AUTO reference to the grid", column.reference);
                            } else {
                                // console.warn("CANNOT add reference to the grid for column [" + column.field + "]");
                            }
                        }
                    }

                    if (column.reference) {
                        if (typeof column.reference === "string") {
                            column.reference = {
                                fieldName: column.reference,
                                resourceManagerDef: column.reference.capitalize().substring(0, column.reference.length - 2) + "Manager"
                            };
                            //console.log("Adding column reference to the grid", column.reference);
                        }

                        // add a title to get a tooltip
                        var title = column.reference.label;
                        if (title === undefined) {
                            if (column.field.endsWith("No")) {
                                // this does not always exist and it makes KendoUI crashes
                                // title = "#=" + column.field.substring(0, column.field.length - 2) + "Label#";
                            }
                        }

                        // add TTTLE
                        //console.log("*************" + JSON.stringify(column));
                        if (column.reference.resourceManagerDef) {
                            column.template = column.template || "<a class='reference'  href='_'" +
                                " data-reference-id='#=" + column.reference.fieldName + "#'" +
                                " data-reference-manager='" + column.reference.resourceManagerDef + "'" +
                                (title ? " title='" + title + "'" : "") +
                                ">#=(" + column.field + "?" + column.field + ":'')#</a>";
                        }
                    }

                    // add values to the column if defined in the app_class
                    if (field && field.values && field.values.data && !column.values) {
                        column.values = $.extend([], field.values.data);

                        // if field is nullable, add a new values
                        if (field.nullable) {
                            column.values.unshift({
                                id: -1,
                                value: -1,
                                label: _this.getLabel("selectFilterNone"),
                                text: _this.getLabel("selectFilterNone")
                            });
                        }

                        // allow multi selection
                        if (column.filterable !== false) {
                            column.filterable = {multi: true, extra: false}
                        }
                    }

                    // add an anchor if needed
                    if (field && field.anchor) {
                        column.template = column.template || "<a class='document' target='_blank' href='#=absolutePath?absolutePath:\"_\"#'>#=(" + column.field + "?" + column.field + ":'')#</a>";
                    }

                    if ((field && field.phoneNumber) || column.field.indexOf("phoneNumber") != -1 ||
                        column.field.indexOf("PhoneNumber") != -1) {
                        column.template = column.template || "<a href='tel:#=" + column.field + "#'>#=(" + column.field + "?" + column.field + ":'')#</a>";
                    }

                    // fix object references
                    if (column.field.indexOf(".") != -1 && (!column.template || column.template.indexOf(column.field) != -1)) {
                        _this.fixObjectReferences(column.field);
                    }
                }

                // adjust column width according to the font-size
                if (screenMode != expresso.Common.SCREEN_MODES.DESKTOP) {
                    if (column.width) {
                        column.width = column.width * 1.1;
                        // column.width = column.width * expresso.Common.getFontRatio();
                    }
                }
            }
        } else {
            this.initMobileColumn();
        }


        // Get the toolbar
        var toolbar = this.getToolbarButtons();

        // if a separator is the last element in the toolbar, remove it
        if (toolbar.length) {
            var lastElement = toolbar[toolbar.length - 1];
            while (lastElement.template && (lastElement.template.indexOf("exp-toolbar-separator") != -1 ||
                lastElement.template.indexOf("exp-toolbar-marker") != -1)) {
                toolbar.pop();
                lastElement = toolbar.length ? toolbar[toolbar.length - 1] : null;
            }
        }

        if (this.hierarchical) {
            // because the treelist does not support the same toolbar array as the Grid, we need to convert it
            var temp = "";
            for (i = 0; i < toolbar.length; i++) {
                temp += toolbar[i].template;
            }
            toolbar = temp;
        }

        // get the datasource
        this.dataSource = this.getDataSource();

        // build the Grid Config
        var gridConfig = {
            autoBind: false,
            dataSource: _this.dataSource,
            columns: _this.columns,
            toolbar: toolbar,
            batch: false,
            pdf: {
                allPages: true
            },
            excel: {
                allPages: true
            },
            excelExport: function (e) {
                e.workbook.fileName = _this.resourceManager.resourceName + "-" + (new Date()).getTime() + ".xlsx";
            },
            change: function (e) {
                _this.onChange(e);
            },
            filter: function (e) {
                _this.onFilter(e);
            },
            sort: function (e) {
                _this.onSort(e);
            },
            dataBound: function (e) {
                _this.onDataBound(e);
            },
            filterMenuInit: function (e) {
                //console.log("Init filter:", e);
                var model = _this.resourceManager.model;
                for (var f in model.fields) {
                    if (f == e.field) {
                        var field = model.fields[f];
                        if (field.type == "date") {
                            _this.replaceDateFilter(e, field);
                        }
                    }
                }
            }
        };

        // let applications override options
        var gridOptions = this.getGridOptions();
        $.extend(gridConfig, gridOptions);
        this.gridConfig = gridConfig;

        // create the Kendo Widget
        this.kendoGrid = this.createGrid($grid, gridConfig);

        // translate grid header
        expresso.Common.localizePage(this.$domElement.find(".k-grid-toolbar"), this.resourceManager.labels);

        // this does not work
        // validate if the column size is enough large for the title
        // for (i = 0; i < this.columns.length; i++) {
        //     column = this.columns[i];
        //     if (column.title) {
        //         var minimumSize = (column.title.length * 12) + 10;
        //         if (column.width < minimumSize) {
        //             console.warn("Column [" + column.field + "] is too narrow [" + column.width + "] for title [" + column.title + "]. Suggested size [" +
        //                 minimumSize + "]");
        //         }
        //     }
        // }

        // this does not work
        // validate if the column size is enough large for the title
        // this.kendoGrid.thead.find("tr:first-child th a.k-link").each(function () {
        //     var $this = $(this);
        //     this.style.textOverflow = 'initial';
        //     var textWidth = $this.outerWidth(true) + 10;
        //     this.style.textOverflow = 'ellipsis';
        //     var cellWidth = $this.parent().width();
        //
        //     console.log("Column [" + $this.text() + "] textWidth [" + textWidth + "] cellWidth [" + cellWidth + "]");
        //
        //     if (cellWidth < textWidth) {
        //         console.warn("Column [" + $this.text() + "] is too narrow [" + cellWidth + "]. Suggested minimum size [" +
        //             textWidth + "]");
        //     }
        // });

        // patch for Chrome
        // remove autofill on column filter
        this.$domElement.find(".k-grid-header input").each(function () {
            $(this).attr("autocomplete", "off");
        });

        // once the grid is built, register the event handlers
        this.registerButtonEventHandler($grid);

        // listen to error
        this.kendoGrid.dataSource.bind("error", function (e) {
            // if the error is a PRECONDITION_FAILED, refresh the data item
            if (e && e.xhr && e.xhr.status == expresso.Common.HTTP_CODES.PRECONDITION_FAILED) {
                e.xhr.alreadyProcessed = true;
                expresso.util.UIUtil.buildMessageWindow(_this.getLabel("wrongEntityVersion")).done(function () {
                    _this.reloadCurrentResource();
                });
            } else {
                //console.warn(e);
                _this.onSaved(null);
            }
        });

        // find key fields and highlight them
        $.each(this.kendoGrid.columns, function () {
            var column = this;
            if (column && column.field && model && model.fields[column.field]) {
                var field = model.fields[column.field];
                // if the field is a key field, highlight it
                if (field && field.keyField) {
                    // console.log("Found keyField column [" + column.field + "]");
                    _this.$domElement.find(".k-grid-header .k-filter-row [data-field='" + column.field + "'] input")
                        .addClass("exp-key-field");
                }
            }
        });

        // build a map of the columns by name
        this.columnMap = {};
        $.each(this.kendoGrid.columns, function () {
            var column = this;
            if (column && column.field) {
                _this.columnMap[column.field] = column;
            }
        });

        // add the functionality to select the column (only to power user)
        if (expresso.Common.isPowerUser() && expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
            this.addColumnMenu();
        }

        // verify if there is a favorite grid preferences to be loaded
        var gridPreference = this.getApplicationPreferences().favoriteGridPreference ?
            this.getApplicationPreferences().gridPreferences[this.getApplicationPreferences().favoriteGridPreference] :
            null;
        if (gridPreference) {
            this.applyApplicationPreference(gridPreference);
        }
    },

    /**
     * if the column references an object, make sure the object is not null
     * this will be used in parseResponseItem to complete the object
     * if the column has a template and the template does not contains the column, there is no need
     * to add a default object (and doing this will cause a problem for list object in the grid as
     * "transportRoutes.description" with a template to display the list
     * @param fieldName
     */
    fixObjectReferences: function (fieldName) {
        //console.log("Fixing [" + fieldName + "]")
        var model = this.resourceManager.model;

        // initialize the need columns to empty by default
        this.objectsNeededForColumns = this.objectsNeededForColumns || {};

        var currentParentObject = this.objectsNeededForColumns;
        var currentParentField = null;

        while (fieldName.indexOf(".") != -1) {
            var parentFieldName = fieldName.substring(0, fieldName.indexOf("."));

            // 1-for existing data item
            // create an empty object to avoid null issue in parseResponseItem
            currentParentObject[parentFieldName] = currentParentObject[parentFieldName] || {};
            currentParentObject = currentParentObject[parentFieldName];

            // 2-for new data item
            // we need to update the model to create a default object in the fields
            // this is used when a new object is created
            if (!currentParentField) {
                currentParentField = model.fields;
                if (!currentParentField[parentFieldName]) {
                    currentParentField[parentFieldName] = {};
                }
                if (currentParentField[parentFieldName].defaultValue === undefined) {
                    currentParentField[parentFieldName].defaultValue = {};
                }
                currentParentField = currentParentField[parentFieldName].defaultValue;
            } else {
                if (!currentParentField[parentFieldName]) {
                    currentParentField[parentFieldName] = {};
                }
                currentParentField = currentParentField[parentFieldName];
            }

            // get the next sub resource
            fieldName = fieldName.substring(fieldName.indexOf(".") + 1);
        }
    },

    /**
     *
     */
    getMobileColumnTemplate: function () {
        var fields = this.resourceManager.model.fields;
        var mobileColumns = this.getMobileColumns();

        var mobileTemplate = [];
        mobileTemplate.push("<div class='mobile-grid-column'>");

        // the order in this list is important
        $.each(["mobileTopRightFieldName", "mobileNumberFieldName", "mobileMiddleRightFieldName",
            "mobileMiddleLeftFieldName", "mobileDescriptionFieldName"], function () {
            var column = "" + this; // convert to string
            var fieldName = mobileColumns[column];
            if (fieldName) {
                var field = fields[fieldName];
                //console.log("fieldName: " + column + "=" + fieldName, field);

                var clazz;
                switch (column) {
                    case "mobileNumberFieldName":
                        clazz = "mobile-grid-column-number clear";
                        break;
                    case "mobileDescriptionFieldName":
                        clazz = "mobile-grid-column-description";
                        break;
                    case "mobileTopRightFieldName":
                        clazz = "mobile-grid-column-top-right";
                        break;
                    case "mobileMiddleLeftFieldName":
                        clazz = "mobile-grid-column-middle-left clear";
                        break;
                    case "mobileMiddleRightFieldName":
                        clazz = "mobile-grid-column-middle-right";
                        break;
                }

                // only display the field if it exists
                if (field || fieldName.indexOf(".") != -1) {
                    if (field && field.type == "date") {
                        fieldName = "expresso.util.Formatter.formatDate(" + fieldName + ")";
                    }
                    mobileTemplate.push("<span class='" + clazz + "'>#:(" + fieldName + "?" + fieldName + ":'')#</span>");
                }
            }
        });

        mobileTemplate.push("</div>");
        return mobileTemplate.join("");
    },

    /**
     * Init the mobile column (always a single column)
     */
    initMobileColumn: function () {
        // do not support active only
        // this.activeOnly = undefined;

        // get the columns
        // var oriColumns = this.getColumns();
        this.columns = [];

        // only 1 column (full size)
        this.columns.push({
            field: "mobileUniqueField",
            headerTemplate: "",
            template: this.getMobileColumnTemplate(),
            width: "100%",
            //attributes: {"class": "center"},
            filterable: false
        });

        // fix object references in mobile template
        var _this = this;
        var mobileColumns = this.getMobileColumns();
        $.each(["mobileNumberFieldName", "mobileDescriptionFieldName", "mobileTopRightFieldName",
            "mobileMiddleLeftFieldName", "mobileMiddleRightFieldName"], function () {
            var fieldName = mobileColumns[this];
            // console.log("Mobile [" + fieldName + "]");

            if (fieldName && fieldName.indexOf(".") != -1) {
                _this.fixObjectReferences(fieldName);
            }
        });
    },

    /**
     * Return the index of the first column with the field name.
     * @param columnFielName
     * @return {number} index (first is 1) of the column. -1 if not found
     */
    getColumnIndex: function (columnFielName) {
        for (var i = 0, l = this.kendoGrid.columns.length; i < l; i++) {
            var column = this.kendoGrid.columns[i];
            if (column.field == columnFielName) {
                return i + 1;
            }
        }
        return -1;
    },

    /**
     * Create the Kendo Grid widget.
     * Could be overwritten by a subclass to create a TreeList if needed
     *
     * @param $grid
     * @param gridConfig
     */
    createGrid: function ($grid, gridConfig) {
        var kendoGrid;
        if (this.hierarchical) {
            $grid.kendoTreeList(gridConfig);
            kendoGrid = $grid.data("kendoTreeList");
        } else {
            $grid.kendoGrid(gridConfig);
            kendoGrid = $grid.data("kendoGrid");
        }
        return kendoGrid;
    },

    /**
     *
     * @param $grid
     */
    registerButtonEventHandler: function ($grid) {
        var kendoGrid = this.kendoGrid;
        var _this = this;

        if (this.multipleSelectionEnabled) {
            // bind the selection event
            $grid.on("click", ".selection", function () {
                var $row = $(this).closest("tr");
                var dataItem = _this.kendoGrid.dataItem($row);

                if (this.checked) {
                    $row.addClass("k-state-selected");
                    _this.selectedRows.push(dataItem);
                } else {
                    $row.removeClass("k-state-selected");
                    _this.selectedRows = _this.selectedRows.filter(function (el) {
                        return el.id !== dataItem.id;
                    });
                }

                _this.onRowSelected();
            });

            // bind the select all checkbox event {
            $grid.find(".select-all").on("click", function () {
                if (kendoGrid.dataSource.total() > 500) {
                    expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("tooManyRecordsForSelection"));
                } else if (kendoGrid.dataSource.total() > 0) {
                    var checked = this.checked;
                    kendoGrid.dataSource._query({
                        // must request more rows that the total
                        pageSize: kendoGrid.dataSource.total() + 1,
                        page: 1
                    }).done(function () {
                        // reset the selection
                        _this.selectedRows = [];

                        // change properties on checkbox
                        $grid.find(".selection").prop("checked", checked);

                        if (checked) {
                            $grid.find(".selection").each(function () {
                                var $row = $(this).closest("tr");
                                $row.addClass("k-state-selected");
                                var dataItem = _this.kendoGrid.dataItem($row);
                                _this.selectedRows.push(dataItem);
                            });
                        } else {
                            // remove selected class
                            kendoGrid.tbody.find("tr").removeClass("k-state-selected");
                        }

                        _this.onRowSelected();
                    });
                }
            });
        }

        if (this.gridConfig.editable) {
            // add the double click event only if we can update
            $grid.on("dblclick", "tbody>tr", function () {
                if (!$(this).hasClass('k-grid-edit-row')) {
                    _this.performEdit();
                }
            });
        }

        $grid.find(".exp-excel-button").on('click', function (e) {
            e.preventDefault();

            // There is a problem with Google Chrome and Kerberos: when the download is too big,
            // it just stop, no warning
            // var count = _this.kendoGrid.dataSource.total();
            // if (expresso.util.Util.getBrowser() == "Chrome" && /*expresso.Common.getAuthenticationPath() == "sso" &&*/
            //     count > 5000) {
            //     expresso.util.UIUtil.buildMessageWindow(_this.getLabel("kerberosChromeExcelIssue"));
            // } else {
            expresso.util.UIUtil.showNotification(_this.getLabel("excelDownloadInProgress"));
            kendoGrid.saveAsExcel();
            // }
        });

        $grid.find(".exp-process-button").on("click", function (e) {
            e.preventDefault();
            _this.performProcess();
        });

        $grid.find(".exp-sync-button").on("click", function (e) {
            e.preventDefault();
            _this.performSync();
        });

        $grid.find(".exp-refresh-button").on("click", function (e) {
            e.preventDefault();
            _this.performRefresh();
        });

        $grid.find(".exp-clearfilters-button").on("click", function (e) {
            e.preventDefault();
            _this.performClearFilters();
        });

        $grid.find(".exp-saveconfiguration-button").on("click", function (e) {
            e.preventDefault();
            _this.performOpenFilters();
        });

        $grid.find(".exp-create-button").on('click', function (e) {
            e.preventDefault();
            _this.performCreate();
        });

        $grid.find(".exp-duplicate-button").on('click', function (e) {
            e.preventDefault();
            _this.performDuplicate();
        });

        $grid.find(".exp-update-button,.exp-view-button").on('click', function (e) {
            e.preventDefault();
            _this.performEdit();
        });

        $grid.find(".exp-delete-button").on('click', function (e) {
            e.preventDefault();
            _this.performDelete();
        });

        $grid.find(".exp-deactivate-button").on('click', function (e) {
            e.preventDefault();
            _this.performCustomAction("deactivate");
        });

        $grid.find(".exp-print-button").on('click', function (e) {
            e.preventDefault();
            _this.performPrint();
        });

        $grid.find(".exp-email-button").on('click', function (e) {
            e.preventDefault();
            _this.performOpenEmail();
        });

        // add listener to toolbar Link button
        $grid.find(".exp-link-button").on('click', function (e) {
            e.preventDefault();
            _this.performGetLink();
        });

        // add listener for overall search functionality
        var previousSearchTerm;
        $grid.find(".search-overall-input").on("change paste search" /* keyup*/, function (e) {
            var term;

            // when pasting, the value is not yet in the input
            if (e && e.type == "paste" && e.originalEvent && e.originalEvent.clipboardData && e.originalEvent.clipboardData.getData) {
                term = e.originalEvent.clipboardData.getData('Text');
            } else {
                term = $(this).val() || null;
            }

            if (term != previousSearchTerm) {
                previousSearchTerm = term;
                window.setTimeout(function () {
                    // we need to wait  because .val() will not return the new value
                    //console.log("************************: [" + term + "]", e);
                    _this.loadResources();
                }, 100);
            }
        });

        // add listener to toolbar actions
        $.each(this.resourceManager.parseAvailableActions(), function () {
            var action = this;
            if (action.showButtonInGridToolbar && !action.systemAction) {
                $grid.find(".exp-" + action.name + "-button").on('click', function (e) {
                    e.preventDefault();

                    // perform the action on every selected resource
                    if (_this.selectedRows.length || action.resourceCollectionAction) {

                        // if a comment is requested, ask it once and apply to all
                        var $windowDeferred;
                        if (action.reasonRequested) {
                            // display a window to enter a reason
                            $windowDeferred = expresso.util.UIUtil.buildPromptWindow(_this.getLabel("enterReason")).then(function (comment) {
                                return {comment: comment};
                            });
                        } else if (action.beforePerformAction) {
                            var result = action.beforePerformAction.call(_this.resourceManager, _this.resourceManager.currentResource);
                            if (result === true || result === undefined) {
                                // ok
                                $windowDeferred = $.Deferred().resolve(null);
                            } else if (result === false) {
                                $windowDeferred = $.Deferred().reject();
                            } else {
                                // assume a promise
                                $windowDeferred = result;
                            }
                        } else if (expresso.Common.getScreenMode() != expresso.Common.SCREEN_MODES.DESKTOP) {
                            // display a confirmation window
                            $windowDeferred = expresso.util.UIUtil.buildYesNoWindow(_this.getLabel("confirmTitle"),
                                _this.getLabel("confirmAction") +
                                "\"" + _this.getLabel(action.name + "ButtonTitle") + "\" ?");
                        } else {
                            $windowDeferred = $.Deferred().resolve(null);
                        }

                        $.when($windowDeferred)
                            .done(function (data) {
                                var actionPromises = [];
                                expresso.util.UIUtil.showLoadingMask(_this.$domElement, true, {id: "gridPerformAction"});

                                if (action.resourceCollectionAction) {
                                    actionPromises.push(action.performAction.call(_this.resourceManager, null, data).done(function () {
                                        // refresh the grid
                                        _this.loadResources();

                                        if (action.afterPerformAction) {
                                            action.afterPerformAction.call(_this.resourceManager, null);
                                        }
                                    }));
                                } else {
                                    $.each(_this.selectedRows, function (index, resource) {
                                        actionPromises.push(action.performAction.call(_this.resourceManager, resource, data).done(function (updatedResource) {
                                            if (updatedResource && updatedResource.id == resource.id) {
                                                _this.updateResource(resource, updatedResource);
                                            }

                                            if (action.afterPerformAction) {
                                                action.afterPerformAction.call(_this.resourceManager, resource);
                                            }
                                        }));
                                    });
                                }

                                $.when.apply(null, actionPromises).done(function () {
                                    if (_this.resourceManager.sections.preview) {
                                        _this.resourceManager.sections.preview.forceRefresh();
                                    }
                                }).always(function () {
                                    expresso.util.UIUtil.showLoadingMask(_this.$domElement, false, {id: "gridPerformAction"});
                                });
                            });
                    }
                });
            }
        });

        // add listener for anchor
        $grid.on("click", "td a.document", function (e) {
            // need to build the URL
            var $a = $(this);
            var href = $a.attr("href");
            var $row = $a.closest("tr");
            var dataItem = kendoGrid.dataItem($row);
            var fileName = dataItem.fileName.replace(/'/g, '&apos;');

            //console.log("HREF [" + href + "]");
            if (href && href != "_") {
                // ok the browser will open it
            } else {
                e.preventDefault();
                var url = _this.resourceManager.getWebServicePath(dataItem.id) + "/file/" + fileName;
                window.open(url);
            }
        });

        // toggle button to display active only resources
        if (_this.activeOnly !== undefined) {
            $grid.find(".exp-active-only-button input").prop("checked", _this.activeOnly);
            $grid.find(".exp-active-only-button").on('click', function (e) {
                e.preventDefault();
                _this.activeOnly = !_this.activeOnly;
                $grid.find(".exp-active-only-button input").prop("checked", _this.activeOnly);
                _this.loadResources();
            });
        }

        // build the support for the view selector
        if ($grid.find(".view-selector").length) {
            // event handler for views

            $grid.find(".view-selector").kendoDropDownList({
                dataTextField: "text",
                dataValueField: "value",
                dataSource: _this.getViews(),
                value: _this.currentView,
                change: function () {
                    var view = this.dataItem().value;
                    _this.changeView(view);
                }
            });
        }

        // build the support for report selector
        var reports = _this.getReports();
        expresso.util.UIUtil.buildReportSelector(reports, $grid.find(".exp-report-selector"), this.resourceManager.labels, null, function (report) {
            _this.executeReport(report);
        });
    },

    /**
     * This method is called when a row is selected
     */
    onRowSelected: function () {
        //console.log(this.resourceManager.getResourceSecurityPath() + " - onRowSelected: " + this.selectedRows.length);

        this.verifySelection();
    },

    /**
     * Method used to enable the buttons depending on the selected resource
     */
    enableButtons: function () {
        //console.log(this.resourceManager.getResourceSecurityPath() + " - enableButtons");

        var _this = this;
        var $toolbar = this.$domElement.find(".k-grid-toolbar");
        var resource = this.resourceManager.currentResource;

        if ((!resource || !resource.id) && this.selectedRows.length < 2) {
            // new resource
            // disabled all buttons
            $toolbar.find(".exp-button:not(.exp-always-active-button)").prop("disabled", true);

            // view only
            $toolbar.find(".exp-update-button").hide();
            $toolbar.find(".exp-view-button:not(.exp-always-hide)").show();
            this.isCreatable().done(function (allowed) {
                $toolbar.find(".exp-creation-button:not(.exp-single-selection)").prop("disabled", !allowed);
            });
        } else {
            $.when(
                this.isCreatable(resource),
                this.isDeletable(resource),
                this.isUpdatable(resource),
                this.resourceManager.getAvailableActionsWithRestrictions()
            ).done(function () {
                if (!_this.selectedRows) {
                    // grid has been destroyed
                    return;
                }

                // by default, enable all buttons
                $toolbar.find(".exp-button").prop("disabled", false);

                // then disable if needed
                if (!_this.selectedRows.length) {
                    // if there is no row selected, disable buttons
                    $toolbar.find(".exp-multiple-selection").prop("disabled", true);
                    $toolbar.find(".exp-single-selection").prop("disabled", true);
                } else if (_this.selectedRows.length > 1) {
                    // disable single button if more than one is selected
                    $toolbar.find(".exp-single-selection").prop("disabled", true);
                }

                // disable buttons if needed
                _this.isCreatable().done(function (allowed) {
                    if (!allowed) {
                        $toolbar.find(".exp-creation-button").prop("disabled", true);
                    }
                });

                _this.isDeletable(resource).done(function (allowed) {
                    if (!allowed) {
                        $toolbar.find(".exp-delete-button").prop("disabled", true);
                    }
                });

                // if the user does not have the right to update it, the button update does not exists. Keep the view
                if (_this.isUserAllowed("update")) {
                    _this.isUpdatable(resource).done(function (allowed) {
                        //console.log(_this.resourceManager.getResourceSecurityPath() + " isUpdatable DONE");
                        if (allowed) {
                            $toolbar.find(".exp-view-button").hide();
                            $toolbar.find(".exp-update-button:not(.exp-always-hide)").show();
                        } else {
                            $toolbar.find(".exp-update-button").hide();
                            $toolbar.find(".exp-view-button:not(.exp-always-hide)").show();
                        }
                    });
                } else {
                    $toolbar.find(".exp-update-button").hide();
                    $toolbar.find(".exp-view-button:not(.exp-always-hide)").show();
                }

                //  disable action buttons if needed
                _this.resourceManager.getAvailableActionsWithRestrictions()
                    .done(function (actions) {
                        //console.log(_this.resourceManager.getResourceSecurityPath() + " getAvailableActionsWithRestrictions DONE");
                        $.each(actions, function (index, action) {
                            //console.log(_this.resourceManager.getResourceSecurityPath() + " " + action.name + ":" + action.allowed);
                            var $button = $toolbar.find(".exp-" + action.name + "-button");
                            if (_this.selectedRows.length > 1 && $button.hasClass("exp-multiple-selection")) {
                                // multiple selection buttons must remain active if there is multiple selections.
                                $button.prop("disabled", false);
                            } else {
                                $button.prop("disabled", !action.allowed);
                            }
                        });
                    });
            });
        }
    },

    performProcess: function () {
        var _this = this;
        _this.sendRequest(_this.resourceManager.getRelativeWebServicePath(), "process", null, null, {showProgress: true}).done(function (/*results*/) {
            _this.loadResources();
        });
    },

    performSync: function () {
        var _this = this;
        _this.sendRequest(_this.resourceManager.getRelativeWebServicePath(), "sync", null, null, {showProgress: true}).done(function (/*results*/) {
            _this.loadResources();
        });
    },

    performRefresh: function () {
        this.loadResources();
    },

    performClearFilters: function () {
        // rename the filter button
        this.$domElement.find(".exp-saveconfiguration-button .exp-button-label").text("");

        this.$domElement.find(".search-overall-input").val("");

        // clear the filters from the filter section is available
        if (this.resourceManager.sections.filter) {
            this.resourceManager.sections.filter.setFilterParams(null);
        }

        // show all columns
        // var _this = this;
        // $.each(this.kendoGrid.columns, function () {
        //     if (this.hidden) {
        //         _this.kendoGrid.showColumn(this);
        //     }
        // });

        this.loadResources(null, null, true);
    },

    performOpenFilters: function () {
        // get the list of saved filters
        var _this = this;

        // utility method to add a filter to the menu
        var addPreferenceToMenu = function (gridPreference) {
            // add it in the menu
            _this.$preferencesMenu.data("kendoContextMenu").insertAfter({
                text: "<span class='filter-name'>" + gridPreference.name + "</span>" +
                    "<span class='fa fa-times pull-left delete-grid-preference' title='" +
                    _this.getLabel("deleteGridPreference") + "'></span>"
                    + "<span class='fa fa-star pull-right favorite-grid-preference' title='" +
                    _this.getLabel("selectFavoriteGridPreference") + "'></span>",
                encoded: false,
                url: "#"
            }, "li:last-child");
            var $menuItem = _this.$preferencesMenu.find("li:last-child");
            $menuItem.data("gridPreference", gridPreference);
            return $menuItem;
        };

        if (!_this.$preferencesMenu) {
            _this.$preferencesMenu = $("<ul class='menu-preferences'>" +
                "<li><span data-action='save'>" + _this.getLabel("saveConfiguration") + "</span></li>" +
                "<li><span data-action='reset'>" + _this.getLabel("resetConfiguration") + "</span></li>" +
                "<li class='k-separator'></li></ul>").appendTo(_this.$domElement);

            // find the toolbar button and assign an ID
            var guid = expresso.util.Util.guid();
            _this.$domElement.find(".exp-saveconfiguration-button").attr("id", guid);

            // build the menu
            _this.$preferencesMenu.kendoContextMenu({
                orientation: "vertical",
                target: "#" + guid,  // target the button savefilters
                select: function (e) {
                    e.preventDefault();
                    //console.log("Menu item selected", e);

                    var $menuItem = $(e.item);
                    var action = $menuItem.find("span[data-action]").data("action");
                    //console.log("Action: " + action);
                    if (action == "save") {
                        var gridFilter = _this.dataSource.filter();
                        var gridSort = _this.dataSource.sort();

                        // add only the visible columns (it will save the order)
                        var gridColumns = [];
                        $.each(_this.kendoGrid.columns, function () {
                            if (!this.hidden && this.field) {
                                gridColumns.push({field: this.field, width: this.width});
                            }
                        });

                        expresso.util.UIUtil.buildPromptWindow(_this.getLabel("newConfigurationWindowTitle"),
                            _this.getLabel("newConfigurationWindowText"))
                            .done(function (gridPreferenceName) {
                                // add the filters, sort and columns in the app preference
                                var newGridPreference = {
                                    name: gridPreferenceName,
                                    gridColumns: gridColumns,
                                    query: {
                                        filter: gridFilter,
                                        sort: gridSort
                                    }
                                };
                                //console.log("Saving new preference", newGridPreference);

                                if (!_this.getApplicationPreferences().gridPreferences) {
                                    _this.getApplicationPreferences().gridPreferences = {}
                                }
                                _this.getApplicationPreferences().gridPreferences[gridPreferenceName] = newGridPreference;

                                // rename the button
                                var gridButtonName = gridPreferenceName.length > _this.maxGridFilterNameLength ?
                                    gridPreferenceName.substring(0, _this.maxGridFilterNameLength) : gridPreferenceName;
                                _this.$domElement.find(".exp-saveconfiguration-button .exp-button-label").text(gridButtonName);

                                // save the preference
                                _this.saveApplicationPreferences().done(function () {
                                    // add it in the menu
                                    addPreferenceToMenu(newGridPreference);
                                });
                            });
                    } else if (action == "reset") {
                        _this.applyApplicationPreference();

                        // reload the grid
                        _this.loadResources({}, null, true);
                    } else {

                        // A filter has been selected, filter it
                        var selectedGridPreference = $menuItem.data("gridPreference");
                        if (selectedGridPreference) {
                            _this.applyApplicationPreference(selectedGridPreference);

                            // reload the grid
                            _this.loadResources(selectedGridPreference.query, null, true);
                        }
                    }
                }
            });

            // register event listener to delete application preference
            _this.$preferencesMenu.on("click", ".delete-grid-preference", function (e) {
                e.preventDefault();
                e.stopPropagation();

                var $menuItem = $(this).closest("li");
                var gridPreference = $menuItem.data("gridPreference");
                //console.log("Removing grid filter [" + gridPreference.name + "]");

                expresso.util.UIUtil.buildYesNoWindow(_this.getLabel("confirmTitle"), _this.getLabel("confirmDeleteConfiguration", {filterName: gridPreference.name}))
                    .done(function () {
                        // remove the gridPreference from the list of filters in the gridPreference
                        delete _this.getApplicationPreferences().gridPreferences[gridPreference.name];

                        // save the preference
                        _this.saveApplicationPreferences().done(function () {
                            // remove it from the menu
                            _this.$preferencesMenu.data("kendoContextMenu").remove($menuItem);
                        });

                    })
                    .always(function () {
                        _this.$preferencesMenu.data("kendoContextMenu").close();
                    });
            });

            // register event listener to set the favorite application preference
            _this.$preferencesMenu.on("click", ".favorite-grid-preference", function (e) {
                e.preventDefault();
                e.stopPropagation();

                var $menuItem = $(this).closest("li");
                var gridPreference = $menuItem.data("gridPreference");

                // if the selected grid preference is already the favorite, unselect it
                if (_this.getApplicationPreferences().favoriteGridPreference == gridPreference.name) {
                    _this.getApplicationPreferences().favoriteGridPreference = null;
                    $menuItem.find(".selected-favorite").removeClass("selected-favorite");
                } else {
                    _this.getApplicationPreferences().favoriteGridPreference = gridPreference.name;
                    _this.$preferencesMenu.find(".selected-favorite").removeClass("selected-favorite");
                    $menuItem.find(".favorite-grid-preference").addClass("selected-favorite");
                }
                _this.saveApplicationPreferences();
            });

            // insert an item for each filter
            var gridPreferences = _this.getApplicationPreferences().gridPreferences;
            if (gridPreferences) {
                gridPreferences = Object.keys(gridPreferences).map(function (key) {
                    return gridPreferences[key];
                });
                // console.log(gridPreferences);
                if (gridPreferences && gridPreferences.length) {
                    // sort them
                    gridPreferences.sort(function (p1, p2) {
                        if (p1.name.toLowerCase() < p2.name.toLowerCase()) {
                            return -1;
                        } else if (p1.name.toLowerCase() > p2.name.toLowerCase()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    });
                    $.each(gridPreferences, function (index, gridPreference) {
                        // add it in the menu
                        var $menuItem = addPreferenceToMenu(gridPreference);

                        if (gridPreference.name == _this.getApplicationPreferences().favoriteGridPreference) {
                            $menuItem.find(".favorite-grid-preference").addClass("selected-favorite");
                        }
                    });
                }
            }
        }

        // open the menu
        _this.$preferencesMenu.data("kendoContextMenu").open();
    },

    /**
     *
     * @param selectedGridPreference
     */
    applyApplicationPreference: function (selectedGridPreference) {
        var _this = this;
        selectedGridPreference = selectedGridPreference || {};

        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE) {
            // do not apply column preferences on phone
            return;
        }

        // rename the button
        var gridButtonName = selectedGridPreference.name ?
            (selectedGridPreference.name.length > this.maxGridFilterNameLength ?
                selectedGridPreference.name.substring(0, this.maxGridFilterNameLength) : selectedGridPreference.name) : "";
        this.$domElement.find(".exp-saveconfiguration-button .exp-button-label").text(gridButtonName);

        if (!this.defaultColumns) {
            this.defaultColumns = $.extend(true, [], this.columns);
        }

        // for faster response, we must clear the grid first
        this.kendoGrid.tbody.empty();

        var forceHidden = false;
        if (!selectedGridPreference.gridColumns) {
            selectedGridPreference.gridColumns = this.defaultColumns;
            forceHidden = true;
        }

        // display only selected columns
        var columnIndexMap = {};
        $.each(this.kendoGrid.columns, function (index) {
            var column = this;
            if (column && column.field) {
                columnIndexMap[column.field] = index;

                // if inside the gridPreferences, show it
                var visible = ($.grep(selectedGridPreference.gridColumns, function (gridColumn) {
                    return (gridColumn.field == column.field) && (!forceHidden || !gridColumn.hidden);
                }).length > 0);

                //console.log("Column " + column.field + " hidden:" + column.hidden + " visible:" + visible + " index:" + index);
                if (column.hidden && visible) {
                    _this.kendoGrid.showColumn(column);
                } else if (!column.hidden && !visible) {
                    _this.kendoGrid.hideColumn(column);
                }
            }
        });

        // reorder columns
        var columnIndex = this.multipleSelectionEnabled ? 1 : 0; // 0 is for "select all" checkbox
        //console.log("selectedGridPreference.gridColumns", selectedGridPreference.gridColumns);
        $.each(selectedGridPreference.gridColumns, function () {
            var field = this.field;
            var column = _this.columnMap[field];
            if (column) {
                if (columnIndexMap[field] != columnIndex) {
                    //console.log("Moving " + field + " from " + columnIndexMap[field] + " to " + columnIndex);
                    _this.kendoGrid.reorderColumn(columnIndex, column);
                }

                // set the column width
                _this.kendoGrid.resizeColumn(column, this.width);

                columnIndex++;
            } else {
                // this column does not exist anymore
            }
        });
    },

    /**
     *
     * @param $filterMenu
     * @param fieldName
     */
    initDateFilter: function ($filterMenu, fieldName) {
        // if there is already a filter, initialize the input
        var dataSource = this.kendoGrid.dataSource;
        var filter = dataSource.filter();
        if (filter) {
            // utility method to return only filter on columns
            var grepDateFilters = function (filters) {
                return $.grep(filters, function (f) {
                    if (f.field) {
                        return f.field == fieldName;
                    } else {
                        return grepDateFilters(f.filters);
                    }
                });
            };

            // add only filter for columns
            var dateFilters = grepDateFilters(filter.filters);
            $.each(dateFilters, function (i, f) {
                if ($.inArray(f.value, ["TODAY", "YESTERDAY", "LASTWEEK", "THISWEEK", "LASTMONTH", "THISMONTH",
                    "LAST3DAYS", "LAST7DAYS", "LAST30DAYS", "LAST365DAYS"]) != -1) {
                    // set it to dynamic
                    $filterMenu.find("input[name=dateRangeType][value=dynamic]").prop("checked", true);
                    $filterMenu.find("select.date-range-selector").setval(f.value);
                    $filterMenu.find("input.start-date").setval(null);
                    $filterMenu.find("input.end-date").setval(null);
                } else {
                    // set it to static
                    $filterMenu.find("input[name=dateRangeType][value=static]").prop("checked", true);
                    if (f.operator == "gte" || f.operator == "gt" || f.operator == "eq") {
                        $filterMenu.find("input.start-date").setval(f.value);
                    } else {
                        $filterMenu.find("input.end-date").setval(f.value ? expresso.util.Formatter.parseDate(f.value).addDays(-1) : null);
                    }
                }
            });
        }
    },

    /**
     * Replace the date filter with a custom date filter
     * http://dojo.telerik.com/@tsveti/UNijO/22
     * @param e
     * @param field
     */
    replaceDateFilter: function (e, field) {
        var _this = this;
        var $filterMenu = e.container;
        var fieldName = e.field;
        $filterMenu.empty();
        $filterMenu.html(
            "<div class='k-filter-menu-container exp-custom-filter date-range-filter'>" +
            "<div class='static-range'><label>" +
            " <input type='radio' name='dateRangeType' value='static' checked>" +
            _this.getLabel("filterStaticDateRangeType") + "</label>" +
            "  <div class='date-range date-range-static'>" +
            "    <span>" + _this.getLabel("filterFromDate") + "</span>" + "<input class='start-date'/>" +
            "    <span>" + _this.getLabel("filterToDate") + "</span>" + "<input class='end-date'/>" +
            "  </div>" +
            "</div>" +
            "<div class='dynamic-range'><label>" +
            " <input type='radio' name='dateRangeType' value='dynamic'>" +
            _this.getLabel("filterDynamicDateRangeType") + "</label>" +
            "  <div class='date-range date-range-dynamic'>" +
            "   <select class='date-range-selector'>" +
            "      <option value='TODAY'>" + _this.getLabel("filterRangeToday") + "</option>" +
            "      <option value='YESTERDAY'>" + _this.getLabel("filterRangeYesterday") + "</option>" +
            "      <option value='THISWEEK'>" + _this.getLabel("filterRangeThisWeek") + "</option>" +
            "      <option value='LASTWEEK'>" + _this.getLabel("filterRangeLastWeek") + "</option>" +
            "      <option value='NEXTWEEK'>" + _this.getLabel("filterRangeNextWeek") + "</option>" +
            "      <option value='THISMONTH'>" + _this.getLabel("filterRangeThisMonth") + "</option>" +
            "      <option value='LASTMONTH'>" + _this.getLabel("filterRangeLastMonth") + "</option>" +
            "      <option value='LAST3DAYS'>" + _this.getLabel("filterRangeLast3Days") + "</option>" +
            "      <option value='LAST7DAYS'>" + _this.getLabel("filterRangeLast7Days") + "</option>" +
            "      <option value='LAST30DAYS'>" + _this.getLabel("filterRangeLast30Days") + "</option>" +
            "      <option value='LAST365DAYS'>" + _this.getLabel("filterRangeLast365Days") + "</option>" +
            "   </select>" +
            "  </div>" +
            "</div>" +
            "<div class='k-action-buttons'>" +
            "  <button type='submit' class='k-button k-primary'>" + _this.getLabel("filter") + "</button>" +
            "  <button type='reset' class='k-button'>" + _this.getLabel("clearFilter") + "</button>" +
            "</div></div>");

        // convert the date to widget
        $filterMenu.find("input.start-date").kendoDatePicker();
        $filterMenu.find("input.end-date").kendoDatePicker();

        $filterMenu.data("kendoPopup").bind("open", function () {
            // initialize default values for date filter menu
            _this.initDateFilter($filterMenu, fieldName);
        });

        // listen the apply filter
        $filterMenu.find("button[type=submit]").on("click", function (e) {
            e.preventDefault();
            e.stopPropagation();

            // add the filter to the grid filter
            // filter is always a filter object (not  null)
            var dataSource = _this.kendoGrid.dataSource;
            var filter = dataSource.filter() || {logic: "and", filters: []};
            //console.log("1-dataSource.filter(): " + JSON.stringify(filter));

            // first remove the filter if needed
            expresso.Common.removeKendoFilter(filter, fieldName);
            //console.log("2-dataSource.filter() : " + JSON.stringify(filter));

            if (filter.logic == "or") {
                // the first level is "or", we need to change it
                filter = {logic: "and", filters: [filter]};
            }

            // verify the radio selection
            var dateRangeType = $filterMenu.find("input[name=dateRangeType]:checked").val();

            if (dateRangeType == "static") {
                var fromDate = $filterMenu.find("input.start-date").data("kendoDatePicker").value();
                var toDate = $filterMenu.find("input.end-date").data("kendoDatePicker").value();

                if (fromDate) {
                    filter.filters.push({
                        field: fieldName, operator: "gte", value:
                            expresso.util.Formatter.formatDate(fromDate)
                    });
                }
                if (toDate) {
                    filter.filters.push({
                        field: fieldName, operator: "lt", value:
                            expresso.util.Formatter.formatDate(expresso.util.Formatter.parseDate(toDate).addDays(1))
                    });
                }
                //console.log("3-dataSource.filter() : " + JSON.stringify(filter));
            } else { // dynamic
                var rangeSelector = $filterMenu.find("select.date-range-selector").val();
                filter.filters.push({field: fieldName, operator: "eq", value: rangeSelector});
            }

            // apply the filter (perform a request to the backend)
            _this.clearSelection();
            dataSource.filter(filter);

            // close popup
            $filterMenu.data("kendoPopup").close();
        });
    },

    performCreate: function () {
        var _this = this;

        // keep a copy of the previous selection
        this.previousSelectedResource = this.resourceManager.currentResource;

        // send an event to clear the preview
        this.selectedRows = [];
        this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, null);

        if (this.kendoGrid.dataSource.page() != 1) {
            // always go to the page 1
            this.kendoGrid.wrapper.find(".k-scrollbar").scrollTop(0);
            setTimeout(function () {
                _this.kendoGrid.addRow();
            }, 10);
        } else {
            this.kendoGrid.addRow();
        }
    },

    performDuplicate: function () {
        var _this = this;

        // send a request to the backend to duplicate the current resource and then edit it
        if (this.resourceManager.currentResource) {

            var $deferred = $.Deferred();
            if (this.serverSideDuplicate === false) {
                this.previousSelectedResource = undefined;

                var duplicatedResource = JSON.parse(JSON.stringify(this.resourceManager.currentResource));

                duplicatedResource.id = undefined;
                duplicatedResource.creationDate = undefined;
                duplicatedResource.creationUserId = undefined;
                duplicatedResource.creationUserFullName = undefined;
                duplicatedResource.creationUser = undefined;
                duplicatedResource.lastModifiedDate = undefined;
                duplicatedResource.lastModifiedUserId = undefined;
                duplicatedResource.lastModifiedUser = undefined;
                duplicatedResource.lastModifiedUserFullName = undefined;

                this.initializeDuplicatedResource(duplicatedResource);

                $deferred.resolve(duplicatedResource);
            } else {

                this.sendRequest(_this.resourceManager.getRelativeWebServicePath(this.resourceManager.currentResource.id), "duplicate")
                    .done(function (duplicatedResource) {

                        // we need to parse it
                        duplicatedResource = _this.parseResponseItem(duplicatedResource);

                        $deferred.resolve(duplicatedResource);
                    });
            }

            // once the duplicatedResource is done, put it in the grid and edit
            $deferred.done(function (duplicatedResource) {
                // console.log("*************:" + JSON.stringify(duplicatedResource), duplicatedResource);

                // we need to set to null any field in the model not defined in the JSON
                var model = _this.resourceManager.model;
                for (var f in model.fields) {
                    //var field = model.fields[f];
                    if (duplicatedResource[f] == undefined) {
                        duplicatedResource[f] = null;
                    }
                }

                if (_this.kendoGrid.dataSource.page() != 1) {
                    // always go to the page 1
                    _this.kendoGrid.wrapper.find(".k-scrollbar").scrollTop(0);
                }

                // because scrollTop will close the edit window, we need to wait
                setTimeout(function () {
                    // now insert the new duplicated resource and select it
                    _this.kendoGrid.dataSource.pushInsert(0, duplicatedResource);

                    // we need to select the newly inserted resource
                    if (_this.getParentId()) {
                        // in hierarchical grid, we need to search for the new resource as
                        // it is not the first one
                        _this.selectRowById(duplicatedResource.id);

                    } else {
                        // it is always the first row
                        _this.selectFirstRow();
                    }

                    _this.performEdit();
                }, 100);
            });
        }
    },

    performEdit: function () {
        var $row = this.kendoGrid.select();
        if ($row && $row.length) {
            this.kendoGrid.editRow($row);
        }
    },

    performDelete: function () {
        var _this = this;
        if (this.selectedRows.length) {

            var label;
            if (this.selectedRows.length == 1) {
                label = this.getLabel("deleteConfirmation");
            } else {
                label = this.getLabel("deleteConfirmationMany", {count: this.selectedRows.length});
            }
        }
        expresso.util.UIUtil.buildYesNoWindow(_this.getLabel("confirmTitle"), label).done(function () {
            var deletedRows = _this.selectedRows;

            // first unselect the rows
            _this.publishEvent(_this.RM_EVENTS.RESOURCE_SELECTED, null);
            _this.selectedRows = [];

            var i, dataItem;
            var patchForKendoBug = true;
            if (patchForKendoBug && _this.virtualScroll) {

                // there are some problems with Kendo UI Grid when using virtual scrolling to delete rows.
                // in this case, we need to delete them manually
                var promises = [];
                for (i = 0; i < deletedRows.length; i++) {
                    dataItem = deletedRows[i];
                    //console.log("Removing dataitem: " + dataItem.id);
                    promises.push(_this.sendRequest(_this.resourceManager.getRelativeWebServicePath(dataItem.id), "delete"));
                }
                $.when.apply(null, promises).done(function () {
                    _this.loadResources().done(function () {
                        _this.publishEvent(_this.RM_EVENTS.RESOURCE_DELETED, null);
                    });
                });
            } else {

                for (i = 0; i < deletedRows.length; i++) {
                    dataItem = deletedRows[i];
                    // console.log("Removing dataitem: " + dataItem.id);
                    _this.kendoGrid.dataSource.remove(dataItem); // this will cause a grid refresh
                }

                if (_this.autoSyncGridDataSource) {
                    _this.kendoGrid.dataSource.sync()
                        .done(function () {
                            // dataBound will select the first row
                            _this.publishEvent(_this.RM_EVENTS.RESOURCE_DELETED, null);
                            _this.selectFirstRow();
                        })
                        .fail(function () {
                            // reload the grid
                            _this.kendoGrid.dataSource.cancelChanges();
                            _this.loadResources();
                        });
                }
            }
        });
    },

    performCustomAction: function (action) {
        if (this.selectedRows.length) {
            var _this = this;
            var actionPromises = [];
            $.each(this.selectedRows, function () {
                var resource = this;
                actionPromises.push(_this.sendRequest(
                    _this.resourceManager.getRelativeWebServicePath(resource.id), action).done(function (updatedResource) {
                    if (updatedResource) {
                        _this.updateResource(resource, updatedResource);
                    }
                }));
            });

            $.when.apply(null, actionPromises).done(function () {
                if (_this.resourceManager.sections.preview) {
                    _this.resourceManager.sections.preview.forceRefresh();
                }
            });
        }
    },

    performPrint: function () {
        if (this.selectedRows.length) {
            var ids = [];
            for (var i = 0; i < this.selectedRows.length; i++) {
                var dataItem = this.selectedRows[i];
                ids.push(dataItem.id);
            }

            // send print request
            expresso.Common.sendPrintRequest(this.resourceManager.getWebServicePath(), this.resourceManager.resourceName, ids);
        }
    },

    performOpenEmail: function () {
        var _this = this;
        var emails = [];
        var to;
        var bcc;
        var subject = "";
        var body = "";
        var target; // _blank is Web based email

        var promises = [];
        for (var i = 0; i < this.selectedRows.length; i++) {
            var resource = this.selectedRows[i];
            if (_this.resourceManager.model.fields.email) {
                // emails to be sent to the users
                emails.push(resource.email);
            } else {
                // get the links from the server
                promises.push(_this.sendRequest(_this.resourceManager.getRelativeWebServicePath() + "/link",
                    null, null, {id: resource.id}).done(function (result) {
                    body += result.link + "\n";
                }));
            }
        }
        $.when.apply(null, promises).done(function () {
            if (emails.length == 0) {
                to = "";
                subject = _this.getLabel(_this.resourceManager.resourceName) + " " +
                    _this.resourceManager.currentResource[_this.resourceManager.resourceFieldNo];
            } else if (emails.length == 1) {
                to = emails;
            } else {
                to = expresso.Common.getUserInfo().email;
                bcc = emails.join(";");
            }

            // URL has a 2k limit
            if (body.length > 1800) {
                // too large
            } else if (bcc && bcc.length > 1800) {
                expresso.util.Util.copyToClipboard(bcc);
                expresso.util.UIUtil.buildMessageWindow(_this.getLabel("tooManyEmails"));
            } else {
                var mailTo = "mailto:" + to + "?" + (bcc ? "bcc=" + bcc + "&" : "") +
                    "subject=" + (subject ? encodeURIComponent(subject) : " ") +
                    (body ? "&body=" + encodeURIComponent(body) : "");
                console.log(mailTo);

                if ($("html").hasClass("k-ie")) {
                    // IE does not support the trick below
                    //window.open(mailTo, (target ? " target=" + target : ""));
                    window.location.href = mailTo;
                } else {
                    var $href = $("<a href='" + mailTo + "'" + (target ? " target=" + target : "") + "></a>");
                    $href.appendTo("body")[0].click();
                    $href.remove();
                }
            }
        });
    },

    performGetLink: function () {
        var _this = this;

        // Get the link URL from the server
        var resource = this.resourceManager.currentResource;
        _this.sendRequest(_this.resourceManager.getRelativeWebServicePath() + "/link", null, null, {id: resource.id}).done(function (result) {
            var link = result.link;
            var $temp = $("<input>");
            $("body").append($temp);
            $temp.val(link).select();
            document.execCommand("copy");
            $temp.remove();

            // expresso.util.UIUtil.buildMessageWindow("Le lien de la ressource a été copié dans votre presse-papier"); // [" + link + "]");
        });
    },

    /**
     *
     * @param $href
     */
    openReference: function ($href) {
        var _this = this;

        // select the row
        var $row = $href.closest("tr");
        this.kendoGrid.select($row);

        var referenceId = $href.data("reference-id");
        var referenceManager = $href.data("reference-manager");

        // console.log("referenceId", referenceId);
        if (referenceId && !isNaN(referenceId)) {
            if (referenceManager) {
                if (this.referenceResourceManagers[referenceManager]) {
                    this.referenceResourceManagers[referenceManager].displayForm({id: referenceId});
                } else {
                    expresso.Common.loadApplication(referenceManager).done(function (resourceManager) {
                        _this.referenceResourceManagers[referenceManager] = resourceManager;
                        resourceManager.displayForm({id: referenceId});
                    });
                }
            }
        } else {
            // alert("referenceId is not valid [" + referenceId + "]");
        }
    },

    /**
     * Execute the report using the current list of selected resources
     * @param report report to be executed
     */
    executeReport: function (report) {
        var params = {
            format: report.format || "pdf"
        };

        // add custom params
        if (report.params) {
            if (typeof report.params === "function") {
                $.extend(params, (report.params)());
            } else {
                $.extend(params, report.params);
            }
        }

        var resources = this.selectedRows;
        var resourceName = this.resourceManager.resourceName;
        var resourceSecurityPath = this.resourceManager.getResourceSecurityPath();

        if (report.type == "multiple") {
            // get the list ids
            var ids = [];
            for (var i = 0, l = resources.length; i < l; i++) {
                var resource = resources[i];
                ids.push(resource.id);
            }
            params["ids"] = ids.join(",");
        } else if (report.type == "single") {
            if (resources.length > 0) {
                params["id"] = resources[0].id;
                params[resourceName + "Id"] = resources[0].id;
            }
        }

        // add labels
        report.label = report.label || this.getLabel("report-" + report.name);
        report.labels = this.resourceManager.labels;

        // add path
        report.path = report.path || (this.resourceManager.applicationPath + "/report-" + report.name + ".html");
        report.resourceName = this.resourceManager.resourceName;
        report.resourceSecurityPath = this.resourceManager.getResourceSecurityPath();

        expresso.Common.executeReport(report, resourceSecurityPath, params, report.type == "custom");
    },

    /**
     * Change the grid to select a new view (grid)
     * @param view
     */
    changeView: function (view) {
        //console.log("New view [" + view + "]");
        this.currentView = view;

        // first we need to destroy the previous view
        if (this.kendoGrid) {
            // this.clearGrid();

            this.kendoGrid.destroy();
            this.kendoGrid = null;
            this.columns = null;

            // clean the DOM
            this.$domElement.empty();
        }

        // load the grid based on the view
        if (view == "default") {
            // put back the default methods
            if (this.originalMethod) {
                $.extend(this, this.originalMethod);
            }

            // initialize the new grid
            this.initGrid();

            // then reinit the change view selector
            this.$domElement.find("input.view-selector").data("kendoDropDownList").value(view);

            // we need to reload the grid with the current set of projects
            this.loadResources();
        } else {
            var _this = this;
            var gridScriptPath = _this.resourceManager.applicationPath + "/grid_" + view + ".js";
            expresso.Common.getScript(gridScriptPath).done(function () {

                // get the object-class and instantiate the object
                var objectClass = _this.resourceManager.applicationPath.replace(/\//g, '.') + ".Grid" + view.capitalize();
                if (!objectClass.startsWith("expresso")) {
                    // add the name of the site
                    objectClass = expresso.Common.getSiteName() + "." + objectClass;
                }
                var objectInstance = eval("new " + objectClass + "(_this.resourceManager, view)");

                // keep a copy of the default methods
                if (!_this.originalMethod) {
                    // copy all method defined
                    _this.originalMethod = $.extend({}, _this);
                }

                // now overwrite the default method
                $.extend(_this, objectInstance);

                // initialize the new grid
                _this.initGrid();

                // then reinit the change view selector
                _this.$domElement.find("input.view-selector").data("kendoDropDownList").value(view);

                // we need to reload the grid with the current set of projects
                _this.loadResources();
            });
        }

    },

    /**
     * On Grid dataBound event.
     * This event is called:
     *      - on first data load
     *      - for every data load by virtual paging
     *
     *     UID will be the same for the virtual paging.
     *     But UID will not be the same after filter and sort event
     */
    onDataBound: function () {
        var _this = this;
        //console.log("EVENT dataBound [" + this.resourceManager.resourceName + "] - selectedRows:" + this.selectedRows.length);

        // record that the grid has been updated
        this.lastUpdateDate = new Date();

        // customize the row (highlight, etc)
        var gridData = this.kendoGrid.dataSource.view();
        //console.log("Found items in view: " + gridData.length);
        //console.log("Found rows in grid: " + (this.kendoGrid.tbody.find("tr").length));
        for (var i = 0, l = gridData.length; i < l; i++) {
            var dataItem = gridData[i];
            var $row = this.kendoGrid.tbody.find("tr[data-uid='" + dataItem.uid + "']");
            if (dataItem && $row.length) {
                this.customizeRow(dataItem, $row);
            }
        }

        // inline method to reselect rows
        var reselectRows = function () {
            // re-highlight the selected rows
            if (_this.selectedRows.length) {
                $.each(_this.selectedRows, function (index, dataItem) {
                    //console.log("Reselecting " + dataItem.id);
                    var $row = _this.kendoGrid.tbody.find("tr[data-uid='" + dataItem.uid + "']");
                    if ($row.length) {
                        $row.addClass("k-state-selected");
                        $row.find(".selection").prop("checked", true);
                    }
                });
            } else {
                // we must do it here because dataBound is called upon sort and filter event
                // and the UID are not the same after a reload
                _this.selectFirstRow();
            }
        };

        // because TreeList modifies the DOM, we need to wait before we reselect the current resource
        if (this.hierarchical) {
            setTimeout(reselectRows, 10);
        } else {
            reselectRows();
        }

        // display the number of items in the grid header
        if (this.countRecords) {
            var count = this.kendoGrid.dataSource.total();
            var $nbrItemsDiv = this.$domElement.find(".k-grid-toolbar .exp-grid-nbr-items");
            if (!$nbrItemsDiv.length) {
                $nbrItemsDiv = $("<span class='exp-grid-nbr-items'></span>").appendTo(this.$domElement.find(".k-grid-toolbar"));
            }
            $nbrItemsDiv.html(count ? (count > 1 ? count + " enregistrements" : "1 enregistrement") : "Aucun enregistrement");
        }

        // only count the first time because in TreeList, using LazyLoading, we get only the new request count
        this.countRecords = !this.hierarchical;

        // display a tooltips for all text fields
        $.each(this.columns, function () {
            var column = this;
            if (column.tooltip || column.field == "description" || column.field == "title" || column.field == "comment" || column.field == "comments" || column.field == "note"
                || column.field == "notes") {

                // find the real position in the table (user may have changed the columns)
                var $column = _this.$domElement.find(".k-grid-header th[data-field='" + column.field + "']");
                if ($column.length == 1) {
                    var index = $column.index() + 1;
                    //console.log("Adding tooltip on [" + column.field + "] index[" + index + "]");
                    _this.kendoGrid.tbody.find(">tr>td:nth-child(" + index + ")").each(function () {
                        var $this = $(this);
                        if (column.tooltip && column.tooltip !== true) {
                            // get the dataItem
                            var dataItem = _this.kendoGrid.dataItem($this.parent());
                            if (dataItem && dataItem[column.field]) {
                                $this.addClass("tooltip").attr("title", column.tooltip[dataItem[column.field]]);
                            }
                        } else {
                            $this.attr("title", $this.text());
                        }
                    });
                }
            }
        });

        // make sure to resize the grid accordingly
        // because of the text in exp-grid-nbr-items that could wrap, we need to trigger a grid resize
        var gridHeight = this.$domElement.find(".k-grid-toolbar").outerHeight(true) +

            // header
            (this.$domElement.find(".k-grid-header").is(":visible") ?
                this.$domElement.find(".k-grid-header").outerHeight(true) : 0) +

            // footer
            (this.$domElement.find(".k-grid-footer").is(":visible") ?
                this.$domElement.find(".k-grid-footer").outerHeight(true) : 0) +

            // body
            this.$domElement.find(".k-grid-content").outerHeight(true);

        var diff = this.$domElement.height() - gridHeight;
        if (diff) {
            // console.log("Resizing grid to fit [" + this.resourceManager.resourceName + "]: " + diff);
            this.$domElement.find(".k-grid-content").height(this.$domElement.find(".k-grid-content").height() + diff);
            this.resizeContent();
        }
    },

    /**
     * On Grid change event.
     * This event is called:
     *     - On user selection
     *     - On KendoGrid.select() method
     */
    onChange: function () {
        // record that the grid has been updated
        this.lastUpdateDate = new Date();

        // handle the select event and publish it
        var dataItem = this.kendoGrid.dataItem(this.kendoGrid.select());
        //console.log("EVENT change  [" + this.resourceManager.resourceName + "] - id[" + dataItem.id + "] uid[" + dataItem.uid + "]");

        if (dataItem) {
            if (dataItem != this.resourceManager.currentResource) {
                //console.log("EVENT change - Selecting new item id[" + dataItem.id + "] uid[" + dataItem.uid + "]");

                this.selectedRows = [dataItem];
                if (this.multipleSelectionEnabled) {
                    // unselect all
                    this.kendoGrid.tbody.find('.selection').prop('checked', false);
                    this.kendoGrid.tbody.find("tr").removeClass("k-state-selected");
                }

                this.onRowSelected();
            }

            // then select the row (editing a row unselect it)
            var $row = this.kendoGrid.tbody.find("tr[data-uid='" + dataItem.uid + "']");
            $row.addClass("k-state-selected");
            $row.find('.selection').prop('checked', true);

            // let the user to update the grid UI (must recustomize the item here)
            this.customizeRow(dataItem, $row);
        } else {
            this.clearSelection();
        }
    },

    /**
     * On Grid filter event.
     * This event is called:
     *     - On user filtering the DataSource via the filter UI
     */
    onFilter: function () {
        this.selectedRows = [];
    },

    /**
     * On Grid sort event.
     * This event is called:
     *     - On user filtering the DataSource via the sort UI
     */
    onSort: function () {
        // console.log("onSort");
        this.selectedRows = [];
    },

    /**
     * Allow the user to prevent the editing if needed
     * @param e
     */
    onBeforeEdit: function (e) {
        // console.log("EVENT onBeforeEdit", e);
    },

    /**
     * On Grid edit event.
     * This event is called:
     *     - On user selection
     *     - On KendoGrid.editRow() method
     * @param e
     */
    onEdit: function (e) {
        var _this = this;

        //console.log("EVENT edit");
        var dataItem = e.model;

        // ***************************************** START patch for _pristineData
        // there is a bug in KendoUI with Virtual scrolling and Editing
        //
        // we need to handle the close X window and the escape key
        // http://docs.telerik.com/kendo-ui/controls/data-management/grid/appearance#limitations-of-virtual-scrolling
        // http://www.telerik.com/forums/editable-grid-with-virtual-scrolling-and-serverpaging-issues
        // The problem is that the "_pristineData" is always the n (pageSize) first records.
        // It is not updated with the virtual paging
        // Then because the DataSource does not find the current data item in the _pristineData,
        // it thinks that is a new record, then it removes it (splice)

        //console.log(_this.kendoGrid.dataSource._pristineData);
        var useKendoUIPristinePatch = true;
        if (useKendoUIPristinePatch) {
            if (!dataItem.isNew()) {
                // make sure that the original version is in the _pristineData
                var id = dataItem.id;
                var pristine = this.kendoGrid.dataSource._pristineData.find(function (item) {
                    return item.id == id;
                });
                if (!pristine) {
                    //console.log("Cannot find the data for the record. Adding it to _pristineData");
                    this.kendoGrid.dataSource._pristineData.push(dataItem.toJSON());
                }
            }
        }
        // ***************************************** END patch for _pristineData


        if (this.customForm) {
            var $window = $(e.container);
            if (dataItem.isNew() && !dataItem.id) {
                _this.initializeNewResource(dataItem);
            }

            // when the window is closed, verify if there is one row selected
            $window.data("kendoWindow").bind("close", function () {
                // console.log("Kendo Window has been closed");
                // this event is call twice when the window is closed by the Escape key or X button
                var dataItem = _this.kendoGrid.dataItem(_this.kendoGrid.select());
                if (_this.selectedRows.length == 1 && !dataItem) {
                    _this.clearSelection();
                }
            });

            _this.customForm.initForm($window, dataItem);
        }
    },

    /**
     * Make sure not to call this method multiple time
     * @param dataItem
     * @param originalDataItem
     * @param event
     */
    onSaved: function (dataItem, originalDataItem, event) {
        if (this.synchingResource && this.customForm) {
            this.synchingResource = false;
            this.customForm.onSaved(dataItem, originalDataItem);
            if (event) {
                this.publishEvent(event, dataItem);
            }
        }
    },
    /**
     * On Grid save event.
     * This event is called:
     *     - On user selection
     * @param e
     */
    onSave: function (e) {
        //console.log("EVENT Grid-onSave");

        var _this = this;
        // e.model : the data item to which the table row is bound. If e.model.id is null, then a newly created row is being saved.
        // e.preventDefault(); to cancel the save
        if (this.customForm) {
            _this.synchingResource = true;

            var $window = $(e.container);
            var dataItem = e.model;
            var valid = this.customForm.validateResource($window, dataItem);
            if (valid === false) {
                //console.log("fail to save");
                e.preventDefault();
                _this.onSaved(null);
            } else {
                var $deferred;
                var manualSync;
                if (valid === true || valid === undefined) {
                    $deferred = $.Deferred().resolve();
                    manualSync = false;
                } else {
                    // do not allow Kendo to sync automatically
                    // we will call it manually
                    $deferred = valid;
                    e.preventDefault();
                    manualSync = true;
                    //console.log("Waiting onSave");
                }

                // when validation has been done, continue
                $deferred.done(function () {
                    //console.log("GRID - " + _this.resourceManager.resourceName + " - All promises done for saving");

                    // keep a copy of the original resource (before sync)
                    var originalDataItem = dataItem ? JSON.parse(JSON.stringify(dataItem)) : null;

                    if (dataItem.id) {
                        _this.kendoGrid.dataSource.one("sync", function () {
                            _this.onSaved(dataItem, originalDataItem, _this.RM_EVENTS.RESOURCE_UPDATED);
                            originalDataItem = null;
                        });
                    } else {
                        _this.resourceManager.currentResource = null;
                        _this.kendoGrid.dataSource.one("sync", function () {
                            _this.onSaved(dataItem, originalDataItem, _this.RM_EVENTS.RESOURCE_CREATED);
                            originalDataItem = null;

                            // needed? select method will trigger the change event
                            var $row = _this.kendoGrid.tbody.find("tr[data-uid='" + dataItem.uid + "']");
                            _this.kendoGrid.select($row);
                        });
                    }

                    if (manualSync) {
                        _this.kendoGrid.dataSource.sync();
                    }

                    // update Google Analytics
                    var p = _this.resourceManager.getResourceSecurityPath();
                    var action = dataItem.id ? "update" : "create";
                    var gaFields = {
                        hitType: 'event',
                        eventCategory: p,
                        eventAction: action,
                        eventLabel: action + " " + p
                    };
                    expresso.Common.sendAnalytics(gaFields);
                }).fail(function () {
                    //console.log("Cannot save because of validation issue");
                    _this.onSaved(null);
                });
            }
        }
    },

    /**
     * Activate buttons based on the current selections in the grid.
     * Based on the selected rows, it will defined the currentResource and trigger events as needed
     */
    verifySelection: function () {
        //console.log(this.resourceManager.getResourceSecurityPath() + " - verifySelection");

        if (this.selectedRows.length == 0) {
            this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, null);
        } else if (this.selectedRows.length == 1) {
            // we should select the new one
            var dataItem = this.selectedRows[0];

            if (!this.resourceManager.currentResource || this.resourceManager.currentResource.uid != dataItem.uid) {
                //console.log("  Selecting id[" + dataItem.id + "] uid[" + dataItem.uid + "]");
                this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, dataItem);
            }
        } else {
            if (this.resourceManager.currentResource) {
                // cannot have more than one selected at a time
                this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, null);
            }
        }
        this.enableButtons();
    },

    /**
     * Select the row of the grid for the ID (event will be triggered on select)
     * @return {*} the row selected
     */
    selectRowById: function (id) {
        //console.log("selectRowById");
        var $row = null;
        if (this.kendoGrid) {
            //get the dataItem by its ID
            var item = this.kendoGrid.dataSource.get(id);

            //use the dataItem's uid to find its corresponding row
            $row = $("[data-uid='" + item.uid + "']", this.kendoGrid.tbody);
        }
        if ($row && $row.length) {
            //console.log("selecting row", $row);
            this.kendoGrid.select($row);
        } else {
            this.clearSelection();
        }
        return $row;
    },

    /**
     * Select the first row of the grid (event will be triggered on select)
     * @return {*} the row selected
     */
    selectFirstRow: function () {
        //console.log("selectFirstRow");
        var $row = null;
        if (this.kendoGrid) {
            var total = this.kendoGrid.dataSource.total();
            if (total) {
                // select the first row
                $row = this.kendoGrid.tbody.find(">tr:not(.k-grouping-row)").first();
            }
        }
        if ($row && $row.length) {
            //console.log("selecting row", $row);
            this.kendoGrid.select($row);
        } else {
            this.clearSelection();
        }
        return $row;
    },

    /**
     * We cannot do "this.kendoGrid.select(null);"
     * Then we need to clear the selection manually
     */
    clearSelection: function () {
        //console.log(this.resourceManager.getResourceSecurityPath() + " - clearSelection");

        if (this.multipleSelectionEnabled) {
            // unselect all
            this.kendoGrid.tbody.find('.selection').prop('checked', false);
            this.kendoGrid.tbody.find("tr").removeClass("k-state-selected");
        }
        this.selectedRows = [];
        this.onRowSelected();
    },

    /**
     *  reload the resource
     */
    reloadCurrentResource: function () {
        var resource = this.resourceManager.currentResource;
        if (resource && resource.id) {
            var _this = this;
            this.sendRequest(this.resourceManager.getRelativeWebServicePath(resource.id)).done(function (updatedResource) {
                _this.updateResource(resource, updatedResource);
            });
        }
    },

    /**
     * Load the data into the grid
     * @param [query]  query for the load (usually filter and sort)
     * @param [autoEdit] true if the grid should open the form after the load
     * @param [clearFilters] true if we need to clear all filters
     * @returns {*} a promise when the data is loaded
     */
    loadResources: function (query, autoEdit, clearFilters) {
        var _this = this;
        // console.log("CALLING loadResources - " + this.resourceManager.resourceName + ": " + JSON.stringify(query));

        // avoid null issue
        query = query || {};

        this.loadingResources = true;

        // if this is a sub grid and there is no master resource selected, we should
        // not call the server
        // WE NEED TO CALL THE SERVER TO CLEAR THE GRID
        // if (this.resourceManager.masterResourceManager && !this.resourceManager.masterResourceManager.currentResource) {
        //     return $.Deferred().resolve();
        // }

        this.clearSelection();

        var resetSort = clearFilters && this.kendoGrid.options.sortable && (this.kendoGrid.options.sortable.mode == "multi");

        // define the options for the grid
        var dataSourceOptions = {
            page: _this.virtualScroll ? 1 : undefined,
            pageSize: _this.virtualScroll ? _this.getPageSize() : undefined,
            aggregate: _this.dataSource.aggregate(),
            group: _this.dataSource.group(),
            sort: resetSort ? null : (query.sort ? query.sort : _this.dataSource.sort()),
            filter: {
                activeOnly: query.activeOnly,
                logic: "and",
                filters: []
            } // it will be defined below
        };

        // now take care of filters
        if (!clearFilters) {
            expresso.Common.addKendoFilter(dataSourceOptions.filter, this.getGridFilter());
        }

        if (query.filter) {
            // if filter is defined on the method, add it
            expresso.Common.addKendoFilter(dataSourceOptions.filter, query.filter);
        }
        // console.log("1-dataSourceOptions.filter: " + JSON.stringify(dataSourceOptions.filter));

        // if there is a masterFilter defined, always use it
        if (this.masterFilter) {
            var f = this.masterFilter;
            if (typeof f === "function") {
                f = f();
            }
            // console.log("MASTER filter: " + JSON.stringify(f));
            expresso.Common.addKendoFilter(dataSourceOptions.filter, f);
        }

        //console.log("2-this.masterFilter: " + JSON.stringify(this.masterFilter));

        // because Kendo will modify the filter and sort, we need to clone it first
        dataSourceOptions = $.extend(true, {}, dataSourceOptions);
        // console.log("********** " + this.resourceManager.resourceName + " v:" + this.virtualScroll + ": " + JSON.stringify(dataSourceOptions));

        // if virtual scroll, use the query method.
        // but when using local data, we need to force the request, then we need to use "read"
        var $queryDeferred;
        if (this.virtualScroll || this.getParentId()) {
            $queryDeferred = this.kendoGrid.dataSource.query(dataSourceOptions);
        } else {
            $queryDeferred = this.kendoGrid.dataSource.read(dataSourceOptions);
        }

        return $queryDeferred.done(function () {

            if (!_this.autoSyncGridDataSource) {
                // after loading, set offline
                _this.kendoGrid.dataSource.online(false);
            }

            if (autoEdit) {
                // wait for the selection of the first row in dataBound
                //setTimeout(function () {
                var resource = _this.resourceManager.currentResource;

                var id;
                if (!resource) {
                    // if there is a search by id, select it
                    if (dataSourceOptions.filter && dataSourceOptions.filter.filters && dataSourceOptions.filter.filters.length &&
                        dataSourceOptions.filter.filters[0] && dataSourceOptions.filter.filters[0].field &&
                        dataSourceOptions.filter.filters[0].field == "id") {
                        id = dataSourceOptions.filter.filters[0].value;
                        resource = _this.kendoGrid.dataSource.get(id);
                    }
                }

                if (resource) {
                    // trigger the edit command
                    var $row = _this.kendoGrid.tbody.find("tr[data-uid='" + resource.uid + "']");
                    _this.kendoGrid.editRow($row);
                } else {
                    if (id && id != -1) {
                        // this means that the resource has been deleted or the user
                        // does not have access to this resource
                        expresso.util.UIUtil.buildMessageWindow(expresso.Common.getLabel("resourceDoesNotExistOrRestricted"));
                    } else {
                        // create a new resource
                        _this.kendoGrid.addRow();
                    }
                }
                //}, 10);
            }
        }).fail(function (/*jqxhr, settings, exception*/) {
            // on failure
            // console.warn("********** FAIL options", options);
            // console.warn("jqxhr", jqxhr);
            // console.warn("settings", settings);
            // console.warn("exception", exception);
        }).always(function () {
            // console.log("loadResources is done");
            // _this.selectFirstRow();
            _this.loadingResources = false;
        });
    },

    /**
     * Return the data source options of the grid
     * @returns {*} an object with data source options
     */
    getDataSourceOptions: function () {
        var _this = this;

        // create the model object and extend it with the parent Id is defined
        var model = this.resourceManager.model;
        if (this.hierarchical) {
            $.extend(true, model, {
                parentId: this.getParentId(),
                expanded: true
            });

            // make sure that parent id is nullable
            model.fields[this.getParentId()].nullable = true;
        }

        return {
            transport: {
                parameterMap: function (data, operation) {
                    if (operation === "read") {

                        // handle the activeOnly
                        if (data && data.activeOnly === undefined) {
                            data.activeOnly = !(_this.activeOnly === false);
                        }

                        return {query: kendo.stringify(data)};
                    } else {
                        var props = _this.resourceManager.purgeResource(data);
                        var s = JSON.stringify(props);
                        //console.log("PROPS: " + s);
                        return s;
                    }
                },
                read: {
                    dataType: "json",
                    type: "GET",
                    url: function () {
                        return _this.resourceManager.getWebServicePath();
                    }
                },
                create: {
                    dataType: "json",
                    contentType: "application/json; charset=utf-8",
                    type: "POST",
                    url: function () {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.resourceManager.labels);

                        return _this.resourceManager.getWebServicePath();
                    }
                },
                update: {
                    dataType: "json",
                    contentType: "application/json; charset=utf-8",
                    type: "PUT",
                    url: function (e) {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.resourceManager.labels);

                        return _this.resourceManager.getWebServicePath(e.id);
                    }
                },
                destroy: {
                    dataType: "json",
                    type: "DELETE",
                    url: function (e) {
                        // set the current  labels in case of errors
                        expresso.Common.setCurrentRequestLabels(_this.resourceManager.labels);

                        return _this.resourceManager.getWebServicePath(e.id);
                    }
                }
            },
            sort: _this.getSort(),
            schema: {
                model: model,
                data: function (d) {
                    if (d && d.data) {
                        return d.data;
                    } else {
                        return [d];
                    }
                },
                total: "total",
                parse: function (response) {
                    // make sure that the UI is still displayed (user could have moved to another location)
                    if (!_this.resourceManager) {
                        return [];
                    }

                    if (response) {
                        response = _this.parseResponse(response);
                    }
                    return response;
                }
            },
            pageSize: _this.virtualScroll ? _this.getPageSize() : undefined,
            group: _this.getGroup(),
            aggregate: _this.getAggregate(),
            serverPaging: _this.virtualScroll,
            serverFiltering: _this.virtualScroll || !!_this.getParentId(),
            serverSorting: _this.virtualScroll,
            filter: _this.isFilterable() ? _this.getInitialGridFilter() : undefined
        };
    },

    /**
     * Return the data source of the grid
     * @returns {*} a reference to the Data Source
     */
    getDataSource: function () {
        var dataSourceDef = this.getDataSourceOptions();

        var dataSource;
        if (this.hierarchical) {
            dataSource = new kendo.data.TreeListDataSource(dataSourceDef);
        } else {
            dataSource = new kendo.data.DataSource(dataSourceDef);
        }

        return dataSource;
    },

    /**
     * Get the sort properties for the grid.
     * @returns {*} // ex: {field: <tbd>,dir: "desc"|"asc"};
     */
    getSort: function () {
        // console.log(this.resourceManager.options);
        //If sort was already specified return it
        if (this.resourceManager.options && this.resourceManager.options.sort) {
            if (typeof this.resourceManager.options.sort === "function") {
                return this.resourceManager.options.sort();
            } else {
                return this.resourceManager.options.sort;
            }
        }
        return undefined;
    },

    /**
     * Return the columns to be displayed in the grid
     * @returns {*[]} array of columns to be displayed
     */
    getColumns: function () {
        alert("Method [getColumns] must be implemented by the subclass");
        return [];
    },

    /**
     * Return the columns for the mobile (phone) view
     * @returns {*}
     */
    getMobileColumns: function () {
        return {
            mobileNumberFieldName: this.resourceManager.resourceName + "No",
            mobileDescriptionFieldName: "description",
            mobileTopRightFieldName: "person",
            mobileMiddleLeftFieldName: "status",
            mobileMiddleRightFieldName: "date"
        };
    },

    /**
     * Allow the user to overwrite default options
     * @returns {*} a map with initial grid options
     */
    getGridOptions: function () {
        var _this = this;

        var powerUser = expresso.Common.isPowerUser();

        var options = {
            // https://docs.telerik.com/kendo-ui/knowledge-base/open-column-visibility-menu-on-right-click-on-header
            columnMenu: false,
            // columnMenu: {
            //     sortable: false,
            //     filterable: false,
            //     columns: true,
            //     messages: {
            //         //   columns: "Choose columns"
            //     }
            // },
            reorderable: true,
            //navigatable: true, // issue with vitual scrolling
            resizable: true,
            groupable: false,
            pageable: false,
            // pageable: {
            //     input: true,
            //     numeric: false
            // },
            // pageable: {
            //     previousNext: false,
            //     numeric: false,
            //     //refresh: true,
            //     messages: {
            //         display: "{2} enregistrements"
            //     }
            // },
            // mobile settings, but it does not work very well...
            // mobile: (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE ? "phone" : undefined),
            // height: (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE ? 550 : undefined),
            scrollable: {
                virtual: true
            },
            sortable: {
                mode: "single", //powerUser ? "multiple" : "single",
                allowUnsort: false //powerUser
            },
            filterable: {
                mode: powerUser && expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP ? "row, menu" : "row"
            },
            selectable: "row",
            editable: {
                mode: "popup",
                window: {
                    //title: _this.getTitle()

                    // we need to setup the animation here (it has no effect with setOptions)
                    //animation: $("html").hasClass("k-ie") ? false : undefined, // not for IE

                    // we need to define the width here otherwise the content of the window is not resize
                    // correctly with window.setOptions
                    width: _this.customForm ? expresso.util.UIUtil.getFormWidth(_this.customForm.$domElement) : 500,

                    actions: [
                        "Maximize", "Close"
                    ]
                }
            },
            beforeEdit: function (e) {
                _this.onBeforeEdit(e);
            },
            edit: function (e) {
                _this.onEdit(e);
            },
            save: function (e) {
                _this.onSave(e);
            },
            remove: function (e) {
                // never allow KendoUI to remove a record
                console.warn("REMOVING FROM KENDO IS NOT ALLOWED");
                e.preventDefault();
                _this.kendoGrid.cancelChanges();
            }
            // saveChanges: function(e) {
            //     if (!confirm("Are you sure you want to save all changes?")) {
            //         e.preventDefault();
            //     }
            // }
        };

        // if it is a custom form, configure it
        if (this.customForm) {
            options.editable.template = kendo.template(_this.customForm.$domElement.parent().html())
        }

        // for Grid in Preview, do not show by default the filter row
        if (!this.isFilterable()) {
            options.filterable = false;
            //options.sortable = false;
        }

        if (!this.virtualScroll) {
            options.scrollable = true;
        }

        if (this.hierarchical) {

            $.extend(true, options, {
                // allow drag & drop
                editable: {
                    move: true
                },

                // cannot use virtual scrolling for TreeList
                scrollable: true,

                // Fired when the user drops an item. If prevented, the source row will not be moved.
                // use it to cancel the move by the UI (synchronous call)
                drop: function (e) {
                    //console.log("drop", e.source, e.destination, e.valid);
                    var resource = e.source;
                    var parent = e.destination;
                    if (_this.onDragEnd(resource, parent) === false) {
                        //e.preventDefault();
                        e.setValid(false);
                    }
                },

                dragstart: function (e) {
                    // console.log("dragstart", e.source);
                    var resource = e.source;

                    //it's possible to drag an object without selecting it, so we force selection
                    _this.publishEvent(_this.RM_EVENTS.RESOURCE_SELECTED, resource);

                    if (_this.onDragStart(resource) === false) {
                        e.preventDefault();
                    }
                },

                // Fired when the user has finished dragging an item and the model has been updated
                dragend: function (/*e*/) {
                    // must trigger sync now
                    if (!_this.kendoGrid.dataSource.options.autoSync) {
                        _this.kendoGrid.dataSource.sync();
                    }
                }
            });
        }

        return options;
    },

    /**
     * Method called when a resource starts to be dragged
     * @param resource
     * @return {boolean} if false, prevent the drag
     */
    onDragStart: function (resource) {
        return true;
    },

    /**
     * When a resource is dropped onto another resource, this method is called with the resource
     *
     * @param resource
     * @param parent
     * @return {boolean} if false, prevent the drop
     */
    onDragEnd: function (resource, parent) {
        return true;
    },

    /**
     * Return true is the grid should have filters
     * @return {boolean}
     */
    isFilterable: function () {
        var filterable = false;
        if (this.resourceManager.displayAsMaster) {
            filterable = true;
        } else if (this.resourceManager.options && this.resourceManager.options.grid && this.resourceManager.options.grid.filterable) {
            // ok, the sub grid needs a filter row
            filterable = true;
        }
        //console.log(this.resourceManager.resourceName + "(" + this.resourceManager.displayAsMaster + "): " + filterable, this.resourceManager.options);
        return filterable;
    },

    /**
     * Allow the user to overwrite default toolbar buttons
     * @returns [] a table with default buttons
     */
    getToolbarButtons: function () {
        var powerUser = expresso.Common.isPowerUser();
        var needSeparator = false;

        // create the toolbar from the buttons
        var toolbar = [];

        if (this.getViews()) {
            toolbar.push({template: '<input class="view-selector">'});
            needSeparator = true;
        }

        if (powerUser /*&& expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP*/) {
            // add the save filter button
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-saveconfiguration-button" title="saveConfiguration"><span class="fa fa-bars"><span class="exp-button-label" data-text-key="saveConfigurationButton"></span></span></button>'});
            needSeparator = true;
        }

        // add a separator
        if (needSeparator) {
            this.addSeparatorToToolbar(toolbar);
            needSeparator = false;
        }

        // add the process button
        if (this.isUserAllowed("process") && expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-process-button" title="process"><span class="fa fa-gear"><span class="exp-button-label" data-text-key="processButton"></span></span></button>'});
            needSeparator = true;
        }

        // add the synchronize button
        if (this.isUserAllowed("sync")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-sync-button" title="synchronize"><span class="fa fa-exchange"><span class="exp-button-label" data-text-key="synchronizeButton"></span></span></button>'});
            needSeparator = true;
        }

        if (this.isFilterable()) {
            // add the refresh button
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-refresh-button" title="refresh"><span class="fa fa-refresh"><span class="exp-button-label" data-text-key="refreshButton"></span></span></button>'});
            needSeparator = true;
        }

        // add a separator
        if (needSeparator) {
            this.addSeparatorToToolbar(toolbar);
            needSeparator = false;
        }

        // add the default create
        if (this.isUserAllowed("create")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-creation-button exp-create-button" title="createNewRecord"><span class="fa fa-plus"><span class="exp-button-label" data-text-key="createNewRecordButton"></span></span></button>'});
        }
        // mark the end of the addition buttons
        toolbar.push({template: '<span class="exp-toolbar-marker exp-toolbar-marker-addition"></span>'});

        // add the duplicate button
        if (this.isUserAllowed("duplicate")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-single-selection exp-creation-button exp-duplicate-button" title="duplicateRecord"><span class="fa fa-copy"><span class="exp-button-label" data-text-key="duplicateRecordButton"></span></span></button>'});
        }

        // add the view button
        toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-single-selection exp-view-button" title="viewRecord"><span class="fa fa-eye"><span class="exp-button-label" data-text-key="viewRecordButton"></span></span></button>'});

        // add the edit button
        if (this.isUserAllowed("update")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-single-selection exp-update-button" title="modifyRecord"><span class="fa fa-pencil"><span class="exp-button-label" data-text-key="modifyRecordButton"></span></span></button>'});
        }

        // add the deactivate button
        if (this.isUserAllowed("deactivate")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-multiple-selection exp-deactivate-button" title="deactivateRecords"><span class="fa fa-asterisk"><span class="exp-button-label" data-text-key="deactivateRecordsButton"></span></span></button>'});
        }

        // add the destroy button
        if (this.isUserAllowed("delete")) {
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-multiple-selection exp-delete-button" title="deleteRecords"><span class="fa fa-trash"><span class="exp-button-label" data-text-key="deleteRecordsButton"></span></span></button>'});
        }

        // mark the end of the edition buttons
        toolbar.push({template: '<span class="exp-toolbar-marker exp-toolbar-marker-edition"></span>'});

        // add a separator
        this.addSeparatorToToolbar(toolbar);

        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {

            // add the print button
            if (this.isUserAllowed("print")) {
                toolbar.push({template: '<button type="button" class="k-button exp-button exp-multiple-selection exp-print-button" title="printRecords"><span class="fa fa-print"><span class="exp-button-label" data-text-key="printRecordsButton"></span></span></button>'});
                needSeparator = true;
            }

            // add the link button
            if (this.isUserAllowed("link")) {
                toolbar.push({template: '<button type="button" class="k-button exp-button exp-single-selection exp-link-button" title="getLink"><span class="fa fa-link"><span class="exp-button-label" data-text-key="getLinkButton"></span></span></button>'});
                needSeparator = true;
            }

            // add the mail button
            if (this.isUserAllowed("email")) {
                toolbar.push({template: '<button type="button" class="k-button exp-button exp-multiple-selection exp-email-button" title="sendEmail"><span class="fa fa-envelope-o"><span class="exp-button-label" data-text-key="sendEmailButton"></span></span></button>'});
                needSeparator = true;
            }
        }

        // then add the action buttons (if any)
        $.each(this.resourceManager.parseAvailableActions(), function (i, action) {
            if (action.showButtonInGridToolbar && !action.systemAction) {
                // show on mobile only if ok
                if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP || action.showButtonOnMobile) {
                    needSeparator = true;
                    toolbar.push({
                        template: "<button type='button' class='k-button exp-button exp-" + action.name + "-button" +
                            (action.resourceCollectionAction ? " exp-always-active-button" :
                                (action.supportMultipleSelections === false ? " exp-single-selection" : " exp-multiple-selection")) +
                            "' title='" + action.title + "'><span class='fa " + action.icon + "'>" +
                            "<span class='exp-button-label' data-text-key='" + action.label + "'></span></span></button>"
                    });
                }
            }
        });

        // mark the end of the action buttons
        toolbar.push({template: '<span class="exp-toolbar-marker exp-toolbar-marker-action"></span>'});

        // then add a button to show only the active resources
        if (this.activeOnly) {
            // add a separator
            if (needSeparator) {
                this.addSeparatorToToolbar(toolbar);
                needSeparator = false;
            }

            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-active-only-button" title="showActiveRecords"><label class="exp-switch"><input type="checkbox"><div class="exp-slider exp-slider-round"></div></label><span class="exp-button-label" data-text-key="showActiveRecordsButton"></span></button>'});
            needSeparator = true;
        }

        if (this.isFilterable() && this.virtualScroll) {
            // add the clear filter button
            toolbar.push({template: '<button type="button" class="k-button exp-button exp-always-active-button exp-clearfilters-button exp-stack-button" title="clearFilters"><span class="fa-stack"><i class="fa fa-filter fa-stack-1x"></i><i class="fa fa-times fa-stack-1x"></i></span><span class="exp-button-label" data-text-key="clearFiltersButton"></span></button>'});

            // add the search overall input
            toolbar.push({template: "<input type='search' class='k-textbox exp-always-active-button search-overall-input' placeholder='searchPlaceHolder'>"});
            needSeparator = true;
        }

        // mark the end of the filter buttons
        toolbar.push({template: '<span class="exp-toolbar-marker exp-toolbar-marker-filter"></span>'});

        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.DESKTOP) {
            // add a separator
            if (needSeparator) {
                this.addSeparatorToToolbar(toolbar);
                //needSeparator = false;
            }

            // always add the excel button
            toolbar.push(this.TOOLBAR_BUTTONS.EXCEL);

            // build the report selector button if needed (not for sibling resource)
            if (this.getReports() && this.resourceManager.displayAsMaster) {
                toolbar.push({template: '<div class="exp-report-selector"></div>'});
            }
        }

        return toolbar;
    },

    /**
     * Utility method to add a separator in the toolbar
     * @param toolbar
     */
    addSeparatorToToolbar: function (toolbar) {
        // make sure the last entry is not a separator
        var addSeparator = true;
        if (toolbar.length) {
            var lastEntry = toolbar[toolbar.length - 1];
            if (lastEntry && lastEntry.template && lastEntry.template.indexOf("exp-toolbar-separator") != -1) {
                addSeparator = false;
            }
        }
        if (addSeparator) {
            toolbar.push({template: '<span class="exp-toolbar-separator"></span>'});
        }
    },

    /**
     * Utility method to allow user to add a button in a specific section
     * @param toolbar
     * @param buttonTemplate
     * @param [marker]
     */
    addButtonToToolbar: function (toolbar, buttonTemplate, marker) {
        if (marker) {
            // insert the filter right before the end of the marker
            var i;
            for (i = 0; i < toolbar.length; i++) {
                var t = toolbar[i];
                if (t && t.template && t.template.indexOf(marker) != -1) {
                    break;
                }
            }

            // insert at the index
            toolbar.splice(i, 0, buttonTemplate);
        } else {
            toolbar.push(buttonTemplate);
        }
    },

    /**
     * Executed before the server response is used. Use it to preprocess or parse the server response.
     *
     * @param response  response from the server
     * @returns {*} return the response
     */
    parseResponse: function (response) {
        if (response.data && response.data.length !== undefined) {
            for (var i = 0; i < response.data.length; i++) {
                response.data[i] = this.parseResponseItem(response.data[i]);
            }
        } else {
            // this is a simple object
            response = this.parseResponseItem(response);
        }
        return response;

    },

    /**
     * Executed before the server response is used. Use it to preprocess or parse each item.
     *
     * @param item  item (row) to be preprocessed if needed
     * @returns {*} return the item (modified)
     */
    parseResponseItem: function (item) {
        item = expresso.Common.parseResponseItem(item, this.resourceManager.model, this.objectsNeededForColumns);

        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE) {
            item.mobileUniqueField = ""; // create the dummy field
        }
        return item;
    },

    /**
     * For a master resource, it is always true when not hierarchical.
     * For a sub resource, it is creatable if the master resource is updatable
     * @param resource the current selected resource
     * @return {*|PromiseLike<boolean>|Promise<boolean>}
     */
    isCreatable: function (resource) {
        if (this.hierarchical) {
            // verify if the creation is allowed as a child of the current selection
            return this.resourceManager.isActionAllowed("create");
        } else {
            // console.log(this.resourceManager.getResourceSecurityPath() + " - isCreatable");
            if (this.resourceManager.masterResourceManager) {
                if (this.resourceManager.siblingResourceManager || this.resourceManager.displayAsMaster) {
                    // this is sub resource displayed as sibling or as master
                    // by default, if the form is created correctly, we should be able to create a complete sub-resource
                    return this.resourceManager.isActionAllowed("create");
                } else {
                    // if this is a sub manager, it is creatable only if the master resource is selected
                    if (this.resourceManager.masterResourceManager.currentResource || !this.autoSyncGridDataSource) {
                        return this.resourceManager.isActionAllowed("create");
                    } else {
                        return $.Deferred().resolve(false);
                    }
                }
            } else {
                // by default, a master resource is always creatable
                return this.resourceManager.isActionAllowed("create");
            }
        }
    },

    /**
     * @param resource the current selected resource
     * @return {*|PromiseLike<boolean>|Promise<boolean>}
     */
    isUpdatable: function (resource) {
        if (resource && resource.id) {
            return this.resourceManager.isActionAllowed("update");
        } else {
            return $.Deferred().resolve(false);
        }
    },

    /**
     * @param resource (null means multiple resources selected)
     * @return {*|PromiseLike<boolean>|Promise<boolean>}
     */
    isDeletable: function (resource) {
        if (resource) {
            if (resource.id) {
                return this.resourceManager.isActionAllowed("delete");
            } else {
                return $.Deferred().resolve(false);
            }
        } else {
            // allow to delete multiple resources.
            // the backend will protect if any resource cannot be deleted
            return $.Deferred().resolve(true);
        }
    },

    /**
     *  Customize the row according to the resource
     * @param resource
     * @param $row
     */
    customizeRow: function (resource, $row) {
        // by default, none
    },

    /**
     * Get the number of records per page
     */
    getPageSize: function () {
        return 50;
    },

    /**
     * Update a resource
     * @param resource
     * @param updatedResource
     */
    updateResource: function (resource, updatedResource) {
        //console.log(this.resourceManager.getResourceSecurityPath() + " dirty:" + resource.dirty);

        // need to parse the resource first
        updatedResource = this.parseResponseItem(updatedResource);

        if (!resource.dirty) {
            // if the resource is not dirty, then we can update the complete resource

            // this will update the grid immediately
            //console.log(this.resourceManager.resourceName + ": PushUpdate", updatedResource);
            // THIS WILL NOT update the fields in the form
            this.kendoGrid.dataSource.pushUpdate(updatedResource);
        } else {
            // the resource is dirty. This means that we are in a form

            var fields = this.resourceManager.model.fields;
            for (var prop in fields) {

                // do not process derived or subproperties
                if (!fields.hasOwnProperty(prop) || !prop || prop.indexOf(".") != -1) {
                    continue;
                }

                // make sure it is defined (null attribute are not always sent by web services)
                if (updatedResource[prop] === undefined && fields[prop].transient !== true) {
                    updatedResource[prop] = null;
                }

                if (fields[prop].transient || fields[prop].refreshable) {
                    if ((resource[prop] && resource[prop].length > 30) || (updatedResource[prop] && updatedResource[prop].length > 30)) {
                        // do not log into the console a huge amount of data for nothing
                        //console.log("Attribute [" + prop + "] updated");
                    } else {
                        //console.log("Attribute [" + prop + "] updated [" + resource[prop] + "] to [" + updatedResource[prop] + "]");
                    }
                    resource.set(prop, updatedResource[prop]);

                    // sometimes resource.set does not set the value if "editable: false"
                    resource[prop] = updatedResource[prop];
                }
            }
        }

        // DO NOT do it as it will close the form window if opened
        // this.kendoGrid.refresh();

        var $row = this.kendoGrid.tbody.find("tr[data-uid='" + resource.uid + "']");
        this.customizeRow(resource, $row);

        // update buttons state
        this.publishEvent(this.RM_EVENTS.RESOURCE_UPDATED, resource);
    },

    /**
     * Initialiaze the new resource if needed
     * @param newResource
     */
    initializeNewResource: function (newResource) {
        // initialize the parent to the current resource if it is a hierachical data source
        if (this.getParentId()) {
            if (this.previousSelectedResource) {
                newResource.set(this.getParentId(), this.previousSelectedResource.id);

                if (this.getParentId().endsWith("Id")) {
                    var parentAttributeName = this.getParentId().substring(0, this.getParentId().length - 2);
                    newResource.set(parentAttributeName, this.previousSelectedResource);
                }
            }
        }
    },

    /**
     * Initialiaze the duplicated resource if needed
     * @param duplicatedResource
     */
    initializeDuplicatedResource: function (duplicatedResource) {

    },

    /**
     * This method is used to get the initial grid filter. It is used only at the initGrid.
     * It must be used only for Grid header filter.
     * If you have custom filters, you should use getGridFilter
     * @returns {*}
     */
    getInitialGridFilter: function () {
        return undefined;
    },

    /**
     * Return the filters for the grid. This method is called on every loadResources.
     * It could be used to add custom filters (not a filter in the Grid Header)
     */
    getGridFilter: function () {
        var _this = this;

        // same with the filter from the GUI (be careful as it already contains previous filters from the latest query)
        // by default, if there is no filter, add the one from the grid
        var gridFilter = [];

        // add only for master resource manager
        // for sub resource and sibling resource, do not add them: it can cause issue with
        // multiplication of the filter
        if (this.resourceManager.displayAsMaster) {
            var dataSourceFilter = this.dataSource.filter();
            //console.log("dataSourceFilter 1: " + JSON.stringify(dataSourceFilter));

            if (dataSourceFilter) {
                var screenMode = expresso.Common.getScreenMode();
                var addColumnFilterOnly = function (filters) {
                    var columnFilters = [];
                    for (var i = 0, l = filters.length; i < l; i++) {
                        var f = filters[i];
                        if (f.field && (_this.columnMap[f.field] || (screenMode == expresso.Common.SCREEN_MODES.PHONE))) {
                            columnFilters.push(f);
                        } else if (f.filters && f.filters.length) {
                            columnFilters.push(f);
                            f.filters = addColumnFilterOnly(f.filters);
                        } else {
                            // do not add
                        }
                    }
                    return columnFilters;
                };

                // add only filter for columns
                if (dataSourceFilter.logic == "or") {
                    gridFilter = addColumnFilterOnly([dataSourceFilter]);
                } else {
                    gridFilter = addColumnFilterOnly(dataSourceFilter.filters);
                }

                //console.log("dataSourceFilter 2: " + JSON.stringify(gridFilter));
            }

            // get the filters from the filter section if available
            if (this.resourceManager.sections.filter) {
                var filterParams = this.resourceManager.sections.filter.getKendoFilters();
                //console.log("filterParams", filterParams);
                gridFilter.push.apply(gridFilter, filterParams);
            }

            // get the filter from the overall filter if available
            if (this.$domElement.find(".search-overall-input").length) {
                var searchFilterTerm = this.$domElement.find(".search-overall-input").val();
                if (searchFilterTerm) {
                    expresso.Common.addKendoFilter(gridFilter,
                        {field: "searchFilterTerm", operator: "eq", value: searchFilterTerm});
                }
            }
        }

        //console.log("gridFilter: " + JSON.stringify(gridFilter));
        if (gridFilter.length == 1 && gridFilter[0].logic && gridFilter[0].filters && gridFilter[0].filters.length == 0) {
            // remove empty filter [{"logic":"and","filters":[]}]
            gridFilter = [];
        }

        return gridFilter;
    },

    /**
     * Allowed the user to specify a grouping
     * @returns {*}
     */
    getGroup: function () {
        return undefined;
    },

    /**
     * Allowed the user to specify an aggregate
     * @returns {*}
     */
    getAggregate: function () {
        return undefined;
    },


    /**
     * Return the name of the property that defined the parent id
     * @returns {*}
     */
    getParentId: function () {
        return undefined;
    },

    // @override
    resizeContent: function () {
        expresso.layout.resourcemanager.SectionBase.fn.resizeContent.call(this);
        if (this.kendoGrid) {
            try {
                this.kendoGrid.resize();
            } catch (e) {
                // ignore
            }
        }
    },

    /**
     * Application that would like to have the report selector must define this function
     *     Type = [single, multiple, custom]
     *
     * @return *  list of reports
     */
    getReports: function () {
        return null;

        // Exemple
        // return {
        //     description: {
        //         name: "project-cost"
        //         type: "single",
        //         format: "xlsx"
        //     }
        // }
    },

    /**
     * Application that would like to have the view selector must define this function
     * The "default" view must the first one
     * @return [] list of views {value, text}
     */
    getViews: function () {
        return null;

        /*
        Exemple:
        return [{
            value: "default",
            text: "Coût"
        }, {
            value: "progress",
            text: "Progression"
        }];
         */
    },

    /**
     *
     */
    createColumnMenu: function () {
        var _this = this;

        // create the DIV to put the list of columns
        this.$columnMenu = $("<div class='column-menu'><div class='close-button'><span class='fa fa-window-close'></span></div><div class='column-list'></div></div>").appendTo(this.$domElement);
        var $columnList = this.$columnMenu.find(".column-list");

        // add the columns in the DIV
        $.each(this.kendoGrid.columns, function () {
            var column = this;
            if (column.field) {
                var div = $('<div/>');
                var label = $('<label/>');
                var checkbox = $('<input type="checkbox">');

                label.text(column.title);
                label.prepend(checkbox);
                checkbox.data("column", column);
                div.append(label);
                checkbox.on('change', function (e) {
                    var column = $(this).data("column");
                    if (e.target.checked) {
                        _this.kendoGrid.showColumn(column);
                    } else {
                        _this.kendoGrid.hideColumn(column);
                    }
                });
                $columnList.append(div);
            }
        });

        // create the widget
        this.$columnMenu.kendoPopup({
            animation: {
                close: {
                    effects: "fadeOut zoom:out",
                    duration: 300
                },
                open: {
                    effects: "fadeIn zoom:in",
                    duration: 300
                }
            }
        });


        // add a listener on the button to close it
        this.$columnMenu.find(".close-button span").on("click", function (e) {
            e.preventDefault();
            if (_this.$columnMenu) {
                _this.$columnMenu.data('kendoPopup').close();
            }
        });

        // close the popup when the user click elsewhere
        this.$domElement.on("click", function (e) {
            if (_this.$columnMenu && _this.$columnMenu.has(e.target).length <= 0) {
                _this.$columnMenu.data('kendoPopup').close();
            }
        });
    },

    /**
     *
     */
    addColumnMenu: function () {
        var _this = this;


        // add the listener on the header (create the menu only on demand)
        this.kendoGrid.element.find('th').on('contextmenu', function (e) {
            e.preventDefault();
            if (!_this.$columnMenu) {
                _this.createColumnMenu();
            }

            // reset the checkbox according to the current visibility
            _this.$columnMenu.find("[type=checkbox]").each(function () {
                var column = $(this).data("column");
                this.checked = !column.hidden;
            });

            // open and display the popup beside the cursor
            _this.$columnMenu.data('kendoPopup').open();
            $('.k-animation-container').css("top", e.clientY).css('left', e.clientX).css("z-index", 999999);

            // if the window is too tall, set the max-height
            _this.$columnMenu.find(".column-list").css("max-height", ($("body").height() - e.clientY - 40));
        });
    },

    // @override
    destroy: function () {
        //console.log("GRID - destroy [" + this.resourceManager.resourceName + "]");
        if (this.kendoGrid) {
            try {
                this.kendoGrid.destroy();
            } catch (e) {
                console.warn("There is a problem while destroying Kendo Grid", e);
            }
            this.kendoGrid = null;
        }

        //this.dataSource.destroy(); // does not exists
        this.dataSource = null;

        this.gridConfig = null;
        this.customForm = null;
        this.selectedRows = null;
        this.columns = null;
        this.originalMethod = null;
        this.previousSelectedResource = null;
        this.columnMap = null;

        // remove the menu preferences if needed
        if (this.$preferencesMenu) {
            this.$preferencesMenu.data("kendoContextMenu").destroy();
            this.$preferencesMenu.remove();
            this.$preferencesMenu = null;
        }

        if (this.$columnMenu) {
            this.$columnMenu.data("kendoPopup").destroy();
            this.$columnMenu.remove();
            this.$columnMenu = null;
        }

        // destroy all reference resource manager
        if (this.referenceResourceManagers) {
            for (var rm in this.referenceResourceManagers) {
                if (this.referenceResourceManagers.hasOwnProperty(rm) && this.referenceResourceManagers[rm].destroy) {
                    //console.log("************* Destroying " + rm);
                    this.referenceResourceManagers[rm].destroy();
                    this.referenceResourceManagers[rm] = null;
                }
            }
        }
        this.referenceResourceManagers = null;

        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    }
});