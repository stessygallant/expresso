var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.resourcemanager = expresso.layout.resourcemanager || {};

/**
 * Form class
 */
expresso.layout.resourcemanager.Form = expresso.layout.resourcemanager.SectionBase.extend({

    // if the resource is read only
    readOnly: undefined,

    // set to true if this manager needs Drag&Drop support
    fileUploadSupport: false,
    kendoUpload: undefined,

    // this promise is used to listen to a save event
    preventWindowClosing: undefined,
    forceClose: undefined,
    savedDeferred: undefined,
    closedDeferred: undefined,

    // flag to verify if the unique constraints are ok (by default, it is true)
    uniqueConstraintsValidated: undefined,

    // reference to the tooltip widget
    tooltipWidget: undefined,

    // flag when we are saving the main resource only
    savingMainResourceOnly: undefined,

    showTabs: undefined,

    $window: undefined,
    windowOptions: undefined,

    // if defined, use this height for the preview panel
    previewHeightRatio: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.resourcemanager.SectionBase.fn.initDOMElement.call(this, $domElement);

        this.uniqueConstraintsValidated = {};

        this.originalShowTabs = this.showTabs;

        if (!this.previewHeightRatio) {
            this.previewHeightRatio = 0.3;
        }

        // Kendo UI needs that the checkbox be added before it opens the form
        if (this.resourceManager && this.resourceManager.model) {
            var model = this.resourceManager.model;
            $domElement.find(":input").each(function () {
                var $this = $(this);
                var name = $this.attr("name");
                if (name) {
                    var field = model.fields[name];

                    if (field && field.type == "boolean") {
                        if (!$this.attr("type")) {
                            $this.attr("type", "checkbox");
                        }

                        // if this is a boolean, data-bind should be checked: and not value:
                        // var dataBind = $this.attr("data-bind");
                        // if (dataBind && dataBind.indexOf("checked:") == -1) {
                        //     dataBind += ",checked:" + dataBind.substring("value:".length);
                        //     $this.attr("data-bind", dataBind);
                        // }
                    }
                }
            });
        }
    },

    /**
     * Initialize the form template
     * @param $window
     * @param resource
     * @param [windowOptions] could be used to provide the window dimension
     */
    initForm: function ($window, resource, windowOptions) {
        // console.log("FORM - initForm - " + this.resourceManager.resourceName + " [" + (resource ? resource.id : null) + "]");

        var _this = this;
        var kendoWindow = $window.data("kendoWindow");
        var $form = $window.find(".exp-form");
        var model = this.resourceManager.model;
        var screenMode = expresso.Common.getScreenMode();

        // init class attributes
        this.$window = $window;
        this.windowOptions = windowOptions || {};
        this.closedDeferred = $.Deferred();

        // add a flag on the form when it is ready
        var $formReadyPromise = $.Deferred();
        $form.data("formReadyPromise", $formReadyPromise);

        if (!resource.id) {
            // for new resource, if it has a master (or sibling) resource, set the master resource id
            if (this.resourceManager.model.masterIdProperty) {
                var masterResource = this.resourceManager.siblingResourceManager ?
                    this.resourceManager.siblingResourceManager.currentResource :
                    this.resourceManager.masterResourceManager.currentResource;
                if (masterResource) {
                    if (this.resourceManager.model.fields[this.resourceManager.model.masterIdProperty]) {
                        // for backward compatibility: if the masterIdProperty is not editable, set it directly
                        resource[this.resourceManager.model.masterIdProperty] = masterResource.id;
                        //this.$window.find("[name='" + this.resourceManager.model.masterIdProperty + "']").setval(masterResource.id);

                        // set the master resource id (the masterIdProperty must be editable)
                        resource.set(this.resourceManager.model.masterIdProperty, masterResource.id);
                    }
                }
            }
        }

        // convert all element to KendoUI
        $form.kendoExpressoForm({
            labels: this.resourceManager.labels,
            resource: resource,
            model: model
        });
        //this.addPromise($form.data("kendoExpressoForm").ready());

        // we show tabs only if there is tabs
        this.showTabs = this.originalShowTabs;
        if (this.showTabs === undefined && screenMode != expresso.Common.SCREEN_MODES.PHONE) {
            this.showTabs = this.resourceManager.sections["preview"] && this.resourceManager.sections["preview"].contents &&
                this.resourceManager.sections["preview"].contents.length > 0;

            // by default, do not show the tabs on create
            if (!resource.id) {
                this.showTabs = false;
            }
        }

        // if the only content is audit and this is a new resource, do not show it
        if (this.showTabs && !resource.id) {
            //console.log("*********** Contents", this.resourceManager.sections["preview"].contents);
            if (this.resourceManager.sections["preview"].contents.length == 1) {
                var previewName;
                if (typeof this.resourceManager.sections["preview"].contents[0] === "string") {
                    previewName = this.resourceManager.sections["preview"].contents[0]
                } else {
                    previewName = this.resourceManager.sections["preview"].contents[0].contentUrl;
                }

                if (previewName.toLowerCase().indexOf("audit") != -1) {
                    this.showTabs = false;
                }
            }
        }

        if (this.showTabs) {
            // add a div for the preview in the form
            var $formWrapper = $form.wrap("<div class='exp-form-wrapper'></div>").parent();
            var $previewTabs = $("<div class='exp-form-preview'></div>").appendTo($formWrapper);

            var previewHeight = Math.max(200, Math.round(
                Math.min($(window).height(), $form.height()) * this.previewHeightRatio));
            //console.log("previewHeight: " + previewHeight);

            // set the height of the form wrapper
            var formHeight = previewHeight + $form.outerHeight(true) + 35;
            $formWrapper.height(formHeight);

            // console.log("$form.outerHeight(true):" + $form.outerHeight(true));
            // console.log("$previewTabs.outerHeight(true):" + $previewTabs.outerHeight(true));
            // console.log("$formWrapper.height:" + $formWrapper.height());

            // insert the splitter
            $formWrapper.kendoSplitter({
                orientation: "vertical",
                panes: [{
                    collapsible: false
                }, {
                    collapsible: true,
                    size: previewHeight + "px"
                }],
                resize: function () {
                    // _this.resizeContent();
                    _this.resourceManager.sections["preview"].resizeContent();
                }
            });

            // move the preview tabs in the form
            this.resourceManager.$domElement.find(".exp-container-preview").children().appendTo($previewTabs);
            this.resourceManager.sections["preview"].resizeContent();

            // if the preview was not visible up to now, we must call a refresh for the tabs
            if (!this.resourceManager.$domElement.find(".exp-container-preview").is(":visible")) {
                //this.resourceManager.sections["preview"].refresh(resource);
                this.publishEvent(this.RM_EVENTS.RESOURCE_SELECTED, resource);
            }

            // add a button to enable the tabs (create the master resource)
            if (!resource.id) {
                $window.find(".exp-form-preview").append(
                    "<div class='exp-overlay'></div><button class='k-button exp-create-main-button'>" +
                    this.getLabel("createMainButtonLabel") + "</button>");

                $window.find(".exp-form-preview .exp-create-main-button").on("click", function () {
                    _this.createMainResource();
                });
            }
        }

        if (!resource.id) {
            // hide field for new resource
            expresso.util.UIUtil.hideField($form.find(".hide-new,.exp-hide-new"));
            expresso.util.UIUtil.hideField($form.find("[name='deactivationDate']"));
        } else {
            // hide fields for existing resource
            expresso.util.UIUtil.hideField($form.find(".show-new-only,.exp-show-new-only"));

            // by default, hide the deactivationDate if not deactivated
            if (!resource.deactivationDate && this.isUserAllowed("deactivate")) {
                expresso.util.UIUtil.hideField($form.find("[name='deactivationDate']:not(.exp-always-show)"));
            }
        }

        // hide/show the parent field if the master resource manager is available
        if (this.resourceManager.masterResourceManager && this.resourceManager.masterResourceManager.currentResource) {
            // ok
        } else {
            expresso.util.UIUtil.hideField($form.find("[name='" + this.resourceManager.model.masterIdProperty + "']"), false);
        }

        // set the title of the window
        kendoWindow.setOptions({
            // set the name of the resource as title
            title: _this.getLabel(_this.resourceManager.resourcePath, null, true) ||
                _this.getLabel(_this.resourceManager.resourceName)
        });

        // for each combo box, if there is a manager defined, add a search button
        $window.find("[data-role=combobox],[data-role=dropdowntree],[data-role=dropdownlist]").each(function () {
            var $input = $(this);

            // verify the model
            var field = _this.getFieldForInput($input, model);
            if (field && ((field.reference && field.reference.resourceManagerDef) ||
                (field.values && field.values.resourceManagerDef))) {
                expresso.util.UIUtil.addSearchButton($input, field.reference || field.values);
            }
        });

        // if the field is an id, do not allow 0. Set it to null
        $window.find(":input").each(function () {
            if (this.name.endsWith("Id") && $(this).val() === 0) {
                $(this).setval(null);
                console.warn("Attribute [" + this.name + "] is an ID but has value 0 instead of null");
            }
        });

        // apply the security if readOnly
        var $readOnlyDeferred = $.Deferred();
        if (!resource || !resource.id) {
            // new resource are always editable
            $readOnlyDeferred = $readOnlyDeferred.resolve(false);
        } else {
            // verify if the user is allowed update on the resource
            this.resourceManager.isActionAllowed("update")
                .done(function (allowed) {
                    $readOnlyDeferred.resolve(!allowed);
                })
                .fail(function () {
                    // this happen when the request is rejected because there is another request made
                    // perform the request directly
                    _this.sendRequest(_this.resourceManager.getRelativeWebServicePath() + "/verifyActionsRestrictions", null, null,
                        {
                            actions: "update",
                            id: _this.resourceManager.getCurrentResourceId()
                        }, {waitOnElement: null, ignoreErrors: true})
                        .then(function (result) {
                            var allowedUpdate = result["update"];
                            $readOnlyDeferred.resolve(!allowedUpdate);
                        });
                });
        }

        $readOnlyDeferred.done(function (readOnly) {
            //console.log(_this.resourceManager.getResourceSecurityPath() + " FORM: readOnly: " + readOnly);
            _this.readOnly = readOnly;
            if (readOnly) {
                _this.setFormReadOnly($form);
                // remove buttons
                $window.find(".k-grid-update").hide();
            }
        });

        // remove the cancel button
        $window.find(".k-grid-cancel").hide();

        // update the button label
        var saveButtonLabel = this.getLabel("saveButtonLabel");
        $window.find(".k-grid-update").html("<span class='k-icon k-i-check'></span>" + saveButtonLabel);

        // on each button, display a loading mask
        $window.on("click", ".k-edit-buttons .k-button:not(.k-split-button-arrow)", function () {
            if (_this.$window) {
                expresso.util.UIUtil.showLoadingMask(_this.$window, true);
            }
        });

        // apply the restrictions from the model
        $window.find(":input").each(function () {
            var $this = $(this);
            if ($this.prop("readonly")) {
                expresso.util.UIUtil.setFieldReadOnly($this);
            } else {
                var field = _this.getFieldForInput($this, model);
                if (field && typeof field !== "string") {
                    if (field.editable === true && !(field.updatable === false && resource && resource.id)) {
                        // ok
                    } else if (field.editable === false) {
                        expresso.util.UIUtil.setFieldReadOnly($this);
                    } else if (field.updatable === false && resource && resource.id) {
                        expresso.util.UIUtil.setFieldReadOnly($this);
                    } else if (field.transient) {
                        expresso.util.UIUtil.setFieldReadOnly($this);
                    }

                    // if the field is unique, validate it
                    if (field.unique) {
                        // by default, it is validated
                        //console.log("**************** Adding unique field [" + name + "]");
                        _this.uniqueConstraintsValidated[field.name] = $.Deferred().resolve();
                        $this.on("change", function () {
                            //console.log("Updating unique field [" + bindName + "]");
                            _this.verifyUniqueField(resource, $this, field.name);
                        });
                    }

                    // hide the input if the field is restricted
                    if (field.restrictedRole && !expresso.Common.isUserInRole(field.restrictedRole)) {
                        expresso.util.UIUtil.hideField($this);
                    }

                    // highlight the field if the modification will need approval
                    if (field.requireApprovalRole && !expresso.Common.isUserInRole(field.requireApprovalRole)) {
                        // console.log(field.name + " require approval role [" + field.requireApprovalRole + "]");
                        $this.closest(".input-wrap").addClass("exp-require-approval");
                    }
                }
            }
        });

        // patch for date: for new resource only
        // if type=date and the defaultValue is not defined and nullable is not true,
        // then set the date value to current date
        // if you set it directly in app_class, the date will be initialized at the initialization
        // of the app_class and then 2 days later you will still get the same date
        if (!resource.id) {
            for (var f in model.fields) {
                if (f) {
                    var field = model.fields[f];
                    if (field && field.type === "date" && field.defaultValue === undefined &&
                        field.nullable !== true) {
                        resource.set(f, new Date());
                        //console.log(f + ": " + resource[f]);
                    }
                }
            }
        }

        // add tooltip if needed
        this.tooltipWidget = $window.kendoTooltip({
            filter: "label[title]",
            width: 500,
            position: "top"
        }).data("kendoTooltip");

        // add the validation on the fields (maxLength, etc)
        this.addValidationAttributes($form, model, resource);

        // add support if needed
        if (_this.fileUploadSupport) {
            this.addDragAndDropSupport($window, resource);
        }

        // add the creator and the last modified user if present
        if (resource && resource.id && (resource.creationUserFullName || resource.lastModifiedUserFullName)) {
            var s = "<div class='creation-div'>";
            if (resource && resource.creationUserFullName) {
                s += "<span>" + this.getLabel("createdByLabel") + " " + resource.creationUserFullName + " " + this.getLabel("createdByDateLabel") + " " + expresso.util.Formatter.formatDate(resource.creationDate, expresso.util.Formatter.DATE_FORMAT.DATE_TIME) + "</span>";
            }
            if (resource && resource.lastModifiedUserFullName) {
                s += "<span>" + this.getLabel("lastModificationLabel") + " " + resource.lastModifiedUserFullName + " " + this.getLabel("createdByDateLabel") + " " + expresso.util.Formatter.formatDate(resource.lastModifiedDate, expresso.util.Formatter.DATE_FORMAT.DATE_TIME) + "</span>";
            }
            s += "</div>";
            $form.append(s);
        }

        // resize textarea to the content
        if (resource && resource.id) {
            $form.find("textarea").each(function () {
                var $textArea = $(this);
                $textArea.removeAttr("rows");
                this.style.height = "1px";
                // not bigger then 12 lines
                var h = Math.min(this.scrollHeight, 200);

                // not smaller than 2 lines
                h = Math.max(h, 35);

                this.style.height = (h + 10) + "px";
            });
        }

        // handle close (either by saving or by closing (X button or ESCAPE button)
        kendoWindow.bind("close", function (e) {
            _this.onClose(e);
        });

        // for combo box with values (directly defined in the form.html)
        // set Combo box filter to contains
        $window.find("[data-role=combobox][data-source]").each(function () {
            var comboBox = $(this).data("kendoComboBox");
            comboBox.options.filter = "contains";
            comboBox.options.suggest = true;

            comboBox.bind("change", function (e) {
                var widget = e.sender;
                if (widget.value() && widget.select() === -1) {
                    //custom has been selected
                    widget.value(""); //reset widget
                }
            });
        });

        // auto select the text on focus for combobox
        $window.find("input[role=combobox]").on("focus", function () {
            $(this).select();
        });

        // patch: if a SELECT has been defined with readonly, we need to put it readonly
        $window.find("select[readonly]").each(function () {
            expresso.util.UIUtil.setFieldReadOnly($(this));
        });

        // add the actions buttons for the applications
        this.addActionButtons($window, resource);

        // once everything is added to the form, set the dimension
        if (expresso.Common.getScreenMode() == expresso.Common.SCREEN_MODES.PHONE) {
            // for phone mode, always max size
            this.windowOptions.height = "max";
            this.windowOptions.width = "max";
            this.windowOptions.top = 0;
        }

        // make sure to resize and position the window correctly
        expresso.util.UIUtil.setWindowDimension(this.$window, this.windowOptions);

        // now show the form
        window.setTimeout(function () {
            $form.css("visibility", "visible");

            // resize the form
            _this.resizeContent();

            // focus on the first input
            try {
                var $input = $window.find(":input.focus");
                if (!$input.length) {
                    $input = $window.find(":input:visible:not([readonly]):enabled:first");
                }
                $input.focus();
            } catch (e) {
            }
        }, 400);

        // widget will wait on this promise before firing event
        window.setTimeout(function () {
            _this.isReady().done(function () {
                $formReadyPromise.resolve();
            });
        }, 10);
    },

    /**
     *
     * @param $window
     * @param resource
     */
    addActionButtons: function ($window, resource) {
        var _this = this;

        // add action buttons
        _this.resourceManager.getAvailableActionsWithRestrictions().done(function (actions) {
            $.each(actions, function (index, action) {
                if (action.allowed && !action.systemAction && !action.resourceCollectionAction &&
                    action.showButtonInForm !== false) {
                    _this.addButton($window, _this.getLabel(action.label), {
                        primary: action.primary,
                        icon: action.icon
                    }, function () {
                        expresso.util.UIUtil.showLoadingMask(_this.$window, true, {id: "formPerformAction"});

                        var $saveDeferred;
                        if (!resource.id) {
                            // create the main resource
                            $saveDeferred = _this.createMainResource();
                        } else if (action.saveBeforeAction !== false && _this.resourceManager.sections.grid.isUpdatable(resource) &&
                            _this.isUserAllowed("update")) {
                            // only save if the resource is updatable
                            $saveDeferred = _this.save();
                        } else {
                            // make sure the resource is not dirty
                            if (resource.dirtyFields) {
                                var model = _this.resourceManager.model;
                                for (var dirtyField in resource.dirtyFields) {
                                    if (model.fields[dirtyField] && !model.fields[dirtyField].transient) {
                                        console.warn("This field is dirty but will NOT be saved: " +
                                            dirtyField + "= " + resource.dirtyFields[dirtyField]);
                                    }
                                }
                                resource.dirtyFields = {};
                            }
                            resource.dirty = false;

                            // no need to save
                            $saveDeferred = $.Deferred().resolve(resource);
                        }

                        $saveDeferred
                            .done(function (resource) {
                                var $windowDeferred;
                                if (action.reasonRequested) {
                                    // display a window to enter a reason
                                    $windowDeferred = expresso.util.UIUtil.buildPromptWindow(_this.getLabel("enterReason")).then(function (comment) {
                                        return {comment: comment};
                                    });
                                } else if (action.beforePerformAction) {
                                    var result = action.beforePerformAction.call(_this.resourceManager, resource);
                                    if (result === true || result === undefined) {
                                        // ok
                                        $windowDeferred = $.Deferred().resolve(null);
                                    } else if (result === false) {
                                        $windowDeferred = $.Deferred().reject();
                                    } else {
                                        // assume a promise
                                        $windowDeferred = result;
                                    }
                                } else {
                                    // validate the resource for the action
                                    var $validateDeferred = _this.validateResource($window, resource, action.pgmKey);
                                    if ($validateDeferred === true || $validateDeferred === undefined) {
                                        // ok
                                        $windowDeferred = $.Deferred().resolve(null);
                                    } else if ($validateDeferred === false) {
                                        $windowDeferred = $.Deferred().reject();
                                    } else {
                                        // assume a promise
                                        $windowDeferred = $validateDeferred;
                                    }
                                }

                                $windowDeferred
                                    .done(function (data) {
                                        // console.log("FORM - performAction - " + _this.resourceManager.resourceName + " [" + (resource ? resource.id : null) + "]:" + action.name);

                                        action.performAction.call(_this.resourceManager, resource, data)
                                            .done(function (updatedResource) {
                                                if (updatedResource && updatedResource.id == resource.id) {
                                                    // refresh only the resource
                                                    _this.resourceManager.sections.grid.updateResource(resource, updatedResource);
                                                }
                                                if (_this.closedDeferred) {
                                                    _this.closedDeferred.resolve(updatedResource);
                                                    _this.closedDeferred = null;
                                                }
                                                _this.close();

                                                if (action.afterPerformAction) {
                                                    action.afterPerformAction.call(_this.resourceManager, resource);
                                                }

                                                if (_this.resourceManager.sections.preview) {
                                                    _this.resourceManager.sections.preview.forceRefresh();
                                                }
                                            })
                                            .always(function () {
                                                expresso.util.UIUtil.showLoadingMask(_this.$window, false, {id: "formPerformAction"});
                                            });
                                    })
                                    .fail(function () {
                                        expresso.util.UIUtil.showLoadingMask(_this.$window, false, {id: "formPerformAction"});
                                    });
                            })
                            .fail(function () {
                                expresso.util.UIUtil.showLoadingMask(_this.$window, false, {id: "formPerformAction"});
                            })
                    });
                }
            });

            // now selec by default the first one if no one is default
            window.setTimeout(function () {
                //console.log("Primary button is [" + $window.find(".k-edit-buttons .k-button.k-primary:visible").text() + "]");
                if (!$window.find(".k-edit-buttons .k-button.k-primary:visible").length) {
                    //console.log("Primary button not found. Adding primary to [" + $window.find(".k-edit-buttons .k-button:visible:last").text() + "]");
                    $window.find(".k-edit-buttons .k-button:visible:last").addClass("k-primary");
                }
            }, 100);
        });
    },

    /**
     * Return the field definition from the model for the input
     * @param $input
     * @param model
     * @return {null|*}
     */
    getFieldForInput: function ($input, model) {
        var bindName = $input.attr("data-bind");
        if (bindName) {
            if (bindName && bindName.startsWith("value:")) {
                bindName = bindName.substring("value:".length);
            }
            if (bindName && bindName.startsWith("checked:")) {
                bindName = bindName.substring("checked:".length);
            }
        } else {
            bindName = $input.attr("name");
        }

        if (bindName) {
            return model.fields[bindName];
        } else {
            return null;
        }
    },

    /**
     *
     */
    removePreviewOverlay: function () {
        // console.log("FORM - removePreviewOverlay - " + this.resourceManager.resourceName);
        if (this.$window) {
            this.$window.find(".exp-form-preview .exp-overlay").remove();
            this.$window.find(".exp-form-preview .exp-create-main-button").remove();

            expresso.util.UIUtil.showLoadingMask(this.$window, false);
        }
    },

    /**
     * Remove all allocated resources in the initForm
     *
     */
    destroyForm: function () {
        // console.log("FORM - destroyForm - " + this.resourceManager.resourceName);

        this.removePreviewOverlay();

        if (this.savedDeferred && this.savedDeferred.state() == "pending") {
            this.savedDeferred.reject();
        }
        this.savedDeferred = null;

        if (this.closedDeferred && this.closedDeferred.state() == "pending") {
            this.closedDeferred.reject();
        }
        this.closedDeferred = null;

        if (this.showTabs) {
            // put back the preview
            if (this.$window) {
                this.$window.find(".exp-form-preview").children().appendTo(this.resourceManager.$domElement.find(".exp-container-preview"));
                this.resourceManager.sections["preview"].resizeContent();
            }
        }

        if (this.kendoUpload) {
            this.kendoUpload.destroy();
            this.kendoUpload = null;
        }

        if (this.tooltipWidget) {
            this.tooltipWidget.destroy();
            this.tooltipWidget = null;
        }

        if (this.$window && this.$window.data("kendoWindow")) {
            this.forceClose = true;
            this.$window.data("kendoWindow").close();
        }

        // reset flag
        this.forceClose = false;
        this.preventWindowClosing = false;
        this.$window = null;
    },

    // @override
    destroy: function () {
        // console.log("FORM - destroy - " + this.resourceManager.resourceName);
        expresso.layout.resourcemanager.SectionBase.fn.destroy.call(this);
    },

    /**
     * Set the window readonly
     * @param $window
     */
    setFormReadOnly: function ($window) {
        expresso.util.UIUtil.setFormReadOnly($window);
        $window.find(".k-grid-update,k-grid-button").hide();
    },

    /**
     * Add a button to the window
     * @param $window
     * @param label
     * @param options
     * @param onClick
     * @returns {*} tht new button
     */
    addButton: function ($window, label, options, onClick) {
        options = options || {};

        var $button = $("<a class='k-button k-grid-button " + (options["classes"] ? options["classes"] : "") +
            (options.primary ? " k-primary pull-right" : "") + "' href='#'>" +
            (options.icon ? "<span class='fa " + options.icon + "'></span>" : "") + label + "</a>");
        if (onClick) {
            $button.on("click", function (e) {
                e.preventDefault();
                e.stopPropagation();
                onClick();
            });
        }

        var $buttonsDiv = $window.find("div.k-edit-buttons");
        if (options.primary) {
            // remove k-primary to other button
            $buttonsDiv.find(".k-primary").removeClass("k-primary");
        }

        $buttonsDiv.prepend($button);
        return $button;
    },

    /**
     * @param $window
     * @param buttons  [  { id: "option1", text: "Option 1" }, etc. ]
     * @param onClick
     */
    addSplitButton: function ($window, buttons, onClick) {
        var $toolbar = $("<div class='toolbar'></div>");

        var b = buttons.shift();
        $toolbar.kendoToolBar({
            resizable: false,
            items: [
                {
                    type: "splitButton",
                    text: b.text,
                    id: b.id,
                    menuButtons: buttons
                }
            ],
            click: function (e) {
                onClick(e.id);
            }
        });

        var $buttonsDiv = $window.find("div.k-edit-buttons");
        $buttonsDiv.prepend($toolbar);

        $toolbar.width($toolbar.find(".k-split-button").outerWidth(true));
    },

    /**
     *
     * @return {*} a promise when the form is saved
     */
    saveAndClose: function (preventWindowClosing) {
        var _this = this;
        this.preventWindowClosing = preventWindowClosing;
        // console.log("FORM - saveAndClose - " + this.resourceManager.resourceName + " (preventWindowClosing:" + preventWindowClosing + ")");

        // always create a new promise
        if (this.savedDeferred) {
            this.savedDeferred.reject();
            this.savedDeferred = null;
        }
        this.savedDeferred = $.Deferred();

        // simulate a click by the user on the update button
        // this could generate a network call or it could execute the onSaved method immediately
        // in that case, the saveDeferred will be automatically resolve
        setTimeout(function () {
            _this.$window.find(".k-grid-update").trigger("click");
        }, 10);

        return this.savedDeferred;
    },

    /**
     *
     * @return {*} a promise when the form is saved
     */
    save: function () {
        return this.saveAndClose(true);
    },

    /**
     * Close the window
     */
    close: function () {
        // console.log("FORM - close - " + this.resourceManager.resourceName);
        this.destroyForm();
    },

    /**
     * On Window close event
     * @param e
     */
    onClose: function (e) {
        // console.log("FORM - onClose - " + this.resourceManager.resourceName + " forceClose:" + this.forceClose, e);

        if (!e.isDefaultPrevented() && e.userTriggered) {
            e.preventDefault(); // do not allow the close here, it will be close inside close()
            this.close();
        } else {
            if (!this.forceClose) {
                if (this.resourceManager.sections.grid.kendoGrid.dataSource.online()) {
                    e.preventDefault();
                } else {
                    // if the grid is offline, we need to close the form
                    if (!e.isDefaultPrevented()) {
                        this.close();
                    } else {
                        e.preventDefault();
                    }
                }
            }
        }
    },

    /**
     * Create the main resource only (activate the tabs, but do not close the window)
     * @return {promise}
     */
    createMainResource: function () {
        var _this = this;

        // save the main resource
        _this.savingMainResourceOnly = true;
        return _this.save().done(function () {
            _this.removePreviewOverlay();
        }).always(function () {
            _this.savingMainResourceOnly = false;
        });
    },

    /**
     * Set the flag on the form field to be highlighted if the field is missing
     * @param $window
     * @param resource
     * @param fieldName
     */
    verifyMissingRequiredField: function ($window, resource, fieldName) {
        var field = this.resourceManager.model.fields[fieldName];
        if (field && field.inlineGridResourceManager) {
            // verify if there is at least one row in the grid
            var $div = $window.find("[name='" + fieldName + "']").siblings(".exp-grid-inline");
            var inlineGridResourceManager = $div.data("resourceManager");
            var inlineGridDataSource = inlineGridResourceManager.sections.grid.dataSource;
            if (inlineGridDataSource.total() == 0) {
                console.log("Required field is null [" + fieldName + "]");
                $div.addClass("exp-invalid");
                return false;
            } else {
                return true;
            }
        } else {
            var value = resource[fieldName];
            if (value === undefined || value === null || value === "" || (value && value.length == 0)) {
                //$f.attr("validationMessage", field.validation.required.message);
                console.log("Required field is null [" + fieldName + "]");
                expresso.util.UIUtil.highlightField($window, fieldName);
                return false;
            } else {
                return true;
            }
        }
    },

    /**
     * Subclass may overwrite this method to provide additional fields to be verified
     * @param $window
     * @param resource
     * @param [action] action is defined if not a standard save
     * @return {Array}
     */
    getAdditionalRequiredFieldNames: function ($window, resource, action) {
        return [];
    },

    /**
     *
     * @param $window
     * @param resource
     * @param [action] the current action performed (undefined means simple save)
     * @return {boolean}
     */
    verifyMissingRequiredFields: function ($window, resource, action) {
        var missingFields = false;
        var model = this.resourceManager.model;
        for (var f in model.fields) {
            if (f) {
                var field = model.fields[f];
                if (field && field.validation && field.validation.required && field.validation.required.enabled !== false) {
                    if (!this.verifyMissingRequiredField($window, resource, f)) {
                        missingFields = true;
                    }
                }
            }
        }

        // then verify addional fields (if any)
        var _this = this;
        $.each(this.getAdditionalRequiredFieldNames($window, resource, action), function (index, fieldName) {
            if (!_this.verifyMissingRequiredField($window, resource, fieldName)) {
                missingFields = true;
            }
        });

        return missingFields;
    },

    /**
     * Verify if the form can be saved
     * @param $window
     * @param resource resource being saved
     * @param [action] action is defined if not a standard save
     * @return {boolean|promise} false if you want to cancel the save.
     */
    validateResource: function ($window, resource, action) {
        var valid = true;
        var _this = this;

        // reset invalid fields
        $window.find(".exp-invalid").removeClass("exp-invalid");

        // verify email
        $window.find("input[data-type=email]").each(function () {
            var email = this.value || "";
            var re = /\S+@\S+\.\S+/;
            if (!(re.test(email)) || email.trim() != email) {
                $(this).addClass("exp-invalid");
                valid = false;
                //console.log("email: [" + email + "] is not valid");
                expresso.util.UIUtil.buildMessageWindow(_this.getLabel("emailNotValid"));
            }
        });

        // verify required field
        if (this.verifyMissingRequiredFields($window, resource, action)) {
            // at least one field is missing
            valid = false;
        }

        if (!valid) {
            expresso.util.UIUtil.displayMissingRequiredFieldNotification($window);
        }

        // make sure all constraints are validated
        if (valid) {
            // console.log("Unique constraint", this.uniqueConstraintsValidated);
            var promises = [];
            for (var f in this.uniqueConstraintsValidated) {
                // console.log("Unique constraint validated: [" + f + "]");
                promises.push(this.uniqueConstraintsValidated[f]);
            }

            if (promises.length) {
                // return a promise as valid returned flag
                //console.log("Waiting Unique constraint validation");
                valid = $.when.apply(null, promises)
                    .done(function () {
                        // console.log("All uniqueConstraintsValidated done");
                    })
                    .fail(function () {
                        // console.log("Some uniqueConstraintsValidated failed");
                        // expresso.util.UIUtil.buildMessageWindow("Certains champs ont des contraintes uniques non respectées");
                    });
            }
        }

        return valid;
    },

    /**
     * Allow user to perform an action after a resource has been created on modified
     * @param resource that has been saved
     * @param originalResource resource before the save (which refresh the resource)
     */
    onSaved: function (resource, originalResource) {
        // console.log("FORM - onSaved - " + this.resourceManager.resourceName + " [" + (resource ? resource.id : null) + "] preventWindowClosing:" + this.preventWindowClosing);

        // if it is a validation problem, the resource will be null
        if (resource) {
            // now we need to verify if there is inline grid
            if (this.$window) {
                this.$window.find(".exp-grid-inline").each(function () {
                    var inlineGridResourceManager = $(this).data("resourceManager");
                    // console.log("FORM - onSaved - inlineGrid " + inlineGridResourceManager.resourceName);

                    // set the current resource
                    inlineGridResourceManager.masterResourceManager.currentResource = resource;

                    // for each dataItem, we need to set the masterIdProperty
                    var dataSource = $(this).children(".k-grid").data("kendoGrid").dataSource;
                    $.each(dataSource.data(), function () {
                        var dataItem = this;
                        if (!dataItem[inlineGridResourceManager.model.masterIdProperty] ||
                            dataItem[inlineGridResourceManager.model.masterIdProperty] == -1) {
                            dataItem.set(inlineGridResourceManager.model.masterIdProperty, resource.id);
                        }
                    });

                    // this will automatically sync (no need for dataSource.sync())
                    dataSource.online(true);
                });
            }

            if (this.savedDeferred) {
                this.savedDeferred.resolve(resource);
                this.savedDeferred = null;
            }

            if (!this.preventWindowClosing) {
                if (this.closedDeferred) {
                    this.closedDeferred.resolve(resource);
                    this.closedDeferred = null;
                }

                var _this = this;
                window.setTimeout(function () {
                    _this.close();
                }, 10);
            }
        } else {
            if (this.savedDeferred) {
                this.savedDeferred.reject();
                this.savedDeferred = null;
            }
        }

        // always remove the flag
        this.preventWindowClosing = false;

        // remove the loading mask
        if (this.$window) {
            expresso.util.UIUtil.showLoadingMask(this.$window, false);
        }
    },

    /**
     * Get a promise when the form is closed
     * @return {undefined|*} promise that will contain the new resource once saved
     */
    bindOnClose: function () {
        return this.closedDeferred;
    },

    /**
     * Utility method to scroll the form to the top
     */
    scrollToTop: function () {
        this.$window.find(".exp-form").animate({scrollTop: 0}, 'slow');
    },

    /**
     *
     * @param resource the current resource
     * @param $input input field to validate
     * @param fieldName field to validate
     * @return {*} a promise (fail if the value is not unique)
     */
    verifyUniqueField: function (resource, $input, fieldName) {
        var _this = this;
        // verify if the value of the input is unique for the resource
        var $deferred = $.Deferred();

        var value = $input.val();
        if (value || value === 0) {
            var name = $input.attr("name");
            var filter = expresso.Common.buildKendoFilter([{
                field: fieldName,
                operator: "eq",
                value: value
            }, {
                field: "id",
                operator: "neq",
                value: resource.id
            }], {countOnly: true, activeOnly: false});
            this.uniqueConstraintsValidated[fieldName] = $deferred;

            var url = _this.resourceManager.getRelativeWebServicePath();
            _this.sendRequest(url, null, null, filter, {waitOnElement: null}).done(function (result) {
                if (result.total > 0) {
                    $input.addClass("exp-invalid-unique");
                    _this.onUniqueFieldException(resource, name, value).done(function () {
                        $deferred.reject();
                        $input.focus();
                    });
                } else {
                    // constraint is validated
                    $input.removeClass("exp-invalid-unique");
                    $deferred.resolve();
                }
            });
        } else {
            // empty is not valid for unique key. It must be null
            if (value === "") {
                $input.setval(null);
            }
            $deferred.resolve();
        }
        return $deferred;
    },

    /**
     *
     * @param resource
     * @param field
     * @param value
     * @return {*} a promise when the exception is handled
     */
    onUniqueFieldException: function (resource, field, value) {
        return expresso.util.UIUtil.buildMessageWindow(this.getLabel("uniqueValidation", {fieldValue: value}));
    },

    // @override
    sendRequest: function (path, action, data, queryString, options) {
        return expresso.layout.resourcemanager.SectionBase.fn.sendRequest.call(this, path, action, data, queryString, $.extend({}, {waitOnElement: this.$window}, options));
    },

    /**
     * Bind the widget on the "change" event or bind the $input if not a widget
     * @param $input
     * @param onChangeCallback
     */
    bindOnChange: function ($input, onChangeCallback) {
        // we need to wait for the for to be ready
        expresso.util.UIUtil.bindOnChange($input, onChangeCallback, this.isReady());
    },

    /**
     * Sets label,input html5 attributes and css based on the validation attributes of the
     * model
     * @param $domElement
     * @param model
     * @param resource
     */
    addValidationAttributes: function ($domElement, model, resource) {
        $.each(model.fields, function (fieldName) {
            var $f = $domElement.find("[name='" + fieldName + "']");

            //Add validation attributes
            // if (this.validation) {
            //     if (this.validation.required && this.validation.required.enabled !== false) {
            //         $f.attr("required", "required");
            //
            //         if (this.validation.required.message) {
            //             $f.attr("validationMessage", this.validation.required.message);
            //         }
            //
            //         //Check if KendoUI widget because it creates an extra span
            //         if ($f.is("[data-role]")) {
            //             $f = $f.closest('.k-widget');
            //         }
            //
            //         // insert the tooltip and position it
            //         // the second column class does not go to the widget
            //         if ($f.length) {
            //             var $tooltip = $("<span class='k-invalid-msg" + ($f[0].offsetLeft > 400 ? " second-column" : "") + "' data-for='" + fieldName + "'></span>");
            //             $f.after($tooltip);
            //         }
            //     }
            // }

            if (this.maxLength) {
                $f.attr('maxlength', this.maxLength);
            }
        });
    },

    // @override
    resizeContent: function () {
        expresso.layout.resourcemanager.SectionBase.fn.resizeContent.call(this);

        // if the form contains a splitter, force the splitter to resize
        if (this.$window) {

            // must recalculate the max height in case the browser has been resize
            expresso.util.UIUtil.setWindowDimension(this.$window, this.windowOptions);

            var splitter = this.$window.find(".k-splitter").data("kendoSplitter");
            if (splitter) {
                splitter.size(".k-pane:first", splitter.size(".k-pane:first"));
            }
        }
    },

    /**
     *
     * @param $window
     * @param resource
     */
    addDragAndDropSupport: function ($window, resource) {
        // add support if needed
        if ((this.isUserAllowed("create") && !(resource && resource.id)) ||
            (this.isUserAllowed("update") && resource && resource.id)) {
            var _this = this;
            var $fileDiv = $("<div><div class='input-wrap exp-upload-div'><label>" + this.getLabel("document") +
                "</label><div class='k-content'><input name='file' type='file' /></div></div></div>");
            $window.find(".exp-form").append($fileDiv);

            $window.find("[name=file]").kendoUpload({
                async: {
                    saveUrl: "will be defined later",
                    removeUrl: null,
                    autoUpload: false
                },
                multiple: false,
                upload: function (e) {
                    var data = {};
                    $.each($window.find(".exp-form :input").serializeArray(), function () {
                        // do not include input from Widget
                        if (this.name.indexOf("_input") == -1) {
                            var value = this.value;

                            // because by default any input is empty, if not defined, assign null
                            if (value === "") {
                                value = null;
                            }
                            data[this.name] = value;
                        }
                    });

                    // add the creation user
                    data["creationUserId"] = expresso.Common.getUserInfo().id;

                    // add the document meta data
                    if (_this.resourceManager.siblingResourceManager &&
                        _this.resourceManager.appDef.appClass.startsWith("cezinc")) {
                        // CEZinc only
                        data["resourceId"] = _this.resourceManager.siblingResourceManager.currentResource.id;
                        data["resourceName"] = _this.resourceManager.siblingResourceManager.resourceName;
                        data["resourceSecurityPath"] = _this.resourceManager.siblingResourceManager.getResourceSecurityPath();

                        var documentFolderPath;
                        if (_this.resourceManager.siblingResourceManager.currentResource[_this.resourceManager.siblingResourceManager.resourceFieldNo]) {
                            // use the resourceNo
                            // ex: project/AP-1090, activityLogRequest/0011234
                            documentFolderPath = _this.resourceManager.siblingResourceManager.getResourceSecurityPath() + "/" +
                                _this.resourceManager.siblingResourceManager.currentResource[_this.resourceManager.siblingResourceManager.resourceFieldNo];
                        } else {
                            // probably a sub resource
                            documentFolderPath = _this.resourceManager.siblingResourceManager.getRelativeWebServicePath(_this.resourceManager.siblingResourceManager.currentResource.id);
                        }
                        data["documentParameter"] = documentFolderPath;
                    }

                    // add token if present
                    if (expresso.Security) {
                        data["sessionToken"] = expresso.Security.getSessionToken();
                    }

                    //console.log("Upload data: " + JSON.stringify(data));
                    e.data = data;
                },
                success: function (/*e*/) {
                    //  refresh the resource
                    // var updatedResource = e.response;
                    // _this.resourceManager.sections.grid.updateResource(resource, updatedResource);
                    _this.resourceManager.sections.grid.loadResources();

                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false);

                    // close the window
                    _this.destroyForm();
                },
                error: function (e) {
                    // remove the progress
                    expresso.util.UIUtil.showLoadingMask($window, false);
                    expresso.Common.displayServerValidationMessage(e.XMLHttpRequest);
                }
            });

            this.kendoUpload = $window.find("[name=file]").data("kendoUpload");

            // hide the default button for new document
            $window.find(".k-grid-update").hide();

            // add the upload button
            this.addButton($window, this.getLabel("save"), {primary: true}, function () {
                if (!_this.kendoUpload.getFiles().length) {
                    _this.saveAndClose();
                } else {
                    expresso.util.UIUtil.showLoadingMask($window, true);

                    // defined the url base on the current resource
                    var url = _this.resourceManager.getUploadDocumentPath(resource ? resource.id : null);
                    //console.log("url [" + url + "]");
                    _this.kendoUpload.options.async.saveUrl = url;
                    _this.kendoUpload.upload();
                }
            });
        }
    }

});
