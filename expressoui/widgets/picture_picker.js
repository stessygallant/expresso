(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    //  <input type="file" accept="image/*" capture="environment">
    var ExpressoPicturePicker = Widget.extend({
        $element: undefined,
        $wrapper: undefined,
        $pictureFile: undefined,
        readOnly: false,

        // these options are mandatory
        options: {
            name: "ExpressoPicturePicker",
            readonly: false,

            // custom options
            resourceSecurityPath: undefined,
            resourceName: undefined,
            resourceId: undefined,
            documentTypePgmKey: "PICTURE",
            maxWidth: 800
        },
        events: ["change"],

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            // console.log("Options", this.options);
            this.$element = $(element);

            this.setOptions(options);
            if (!this.options.resourceSecurityPath) {
                this.options.resourceSecurityPath = this.options.resourceName;
            }

            // convert
            this._convertElementToWidget();

            // set initial value
            var initialValue;
            if (options.value !== undefined) {
                initialValue = options.value;
            } else if (this.$element.val()) {
                initialValue = this.$element.val();
            }
            if (initialValue && initialValue.id) {
                this.value(initialValue);
            }
        },

        /**
         *
         */
        _convertElementToWidget: function () {
            var _this = this;

            // wrap the input
            this.$element.wrap("<div class='k-widget exp-picture-picker'></div>");
            this.$wrapper = this.$element.parent();
            this.$element.hide();

            // convert to <input type="file" accept="image/*" capture="environment">
            this.$pictureFile = $("<input type='file' accept='image/*' capture='environment'>").appendTo(this.$wrapper);
            this.$pictureFile.hide();

            this.$wrapper.append("<div class='exp-picture'>" +
                "<img src='' alt=''>" +
                "<span class='fa fa-trash'></span>" +
                "<span class='fa fa-plus fa-2x'></span>" +
                "</div>");

            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $trash = $pictureDiv.find(".fa-trash");
            var $add = $pictureDiv.find(".fa-plus");
            var $img = $pictureDiv.find("img");

            // listen on ADD
            $add.on("click", function () {
                _this.$pictureFile.trigger("click");
            });

            // load the picture
            this.$pictureFile.on("change", function () {
                var file = _this.$pictureFile[0].files[0];
                var imgURL = URL.createObjectURL(file);
                _this._displayPicture(imgURL);

                // save picture
                _this.savePicture(imgURL, file).done(function () {
                    // trigger resource has changed
                    _this.$element.trigger("change");
                    _this.trigger("change", {userTriggered: true});
                });
            });

            // listen on DELETE
            $trash.on("click", function () {
                expresso.util.UIUtil.buildYesNoWindow(expresso.Common.getLabel("deletePictureWindowTitle"),
                    expresso.Common.getLabel("deletePicture"))
                    .done(function () {
                        _this._deletePicture();
                    });
            });

            // display full size picture
            $img.on("click", function () {
                expresso.util.UIUtil.showMaximizedPicture($(this));
            });
        },

        /**
         *
         * @param imgURL
         * @param pictureDocument
         */
        _displayPicture: function (imgURL, pictureDocument) {
            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $trash = $pictureDiv.find(".fa-trash");
            var $add = $pictureDiv.find(".fa-plus");
            var $img = $pictureDiv.find("img");
            var img = $img[0];

            if (imgURL) {
                if (imgURL.startsWith("http")) {
                    // display thumbnail
                    // imgURL += "?thumbnail=true";
                } else {
                    img.onload = function () {
                        URL.revokeObjectURL(imgURL);
                    };
                }
                img.src = imgURL;
                $trash.show();
                $add.hide();
            } else {
                $trash.hide();
                $add.show();
            }

            if (pictureDocument !== undefined) {
                if (pictureDocument) {
                    pictureDocument.url = imgURL;
                }
                $img.data("pictureDocument", pictureDocument);
            }
        },

        /**
         *
         */
        _deletePicture: function () {
            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $img = $pictureDiv.find("img");
            var img = $img[0];
            var $trash = $pictureDiv.find(".fa-trash");
            var $add = $pictureDiv.find(".fa-plus");

            // delete picture
            var pictureDocument = $img.data("pictureDocument");
            if (pictureDocument) {
                expresso.Common.sendRequest("document/" + pictureDocument.id, "delete").done(function () {
                    $img.removeData("pictureDocument");
                });
            }
            img.src = "";
            $trash.hide();
            $add.show();
        },

        /**
         *
         * @param imgURL
         * @param [file]
         * @param [resourceId]
         * @returns {Promise}
         */
        savePicture: function (imgURL, file, resourceId) {

            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $img = $pictureDiv.find("img");
            var img = $img[0];

            var pictureDocument = $img.data("pictureDocument");

            // if resourceId is specified, this means it has been saved later
            if (resourceId) {
                this.options.resourceId = resourceId;
            }

            if (!imgURL) {
                imgURL = this.imgURL;
                file = this.file;
            }

            if (imgURL && imgURL.startsWith("http")) {
                // if it is a URL, already saved
                return $.Deferred().resolve();
            } else if (pictureDocument) {
                // if there is a pictureDocument, it is already saved
                return $.Deferred().resolve();
            } else if (!this.options.resourceId) {
                //  console.log("Need to be saved later");
                this.imgURL = imgURL;
                this.file = file;
                return $.Deferred().reject();
            } else if (file) {
                var formData = new FormData();
                formData.append("type", "document");
                formData.append("resourceSecurityPath", this.options.resourceSecurityPath);
                formData.append("resourceName", this.options.resourceName);
                formData.append("resourceId", this.options.resourceId);
                formData.append("documentTypePgmKey", this.options.documentTypePgmKey);
                formData.append("description", "Picture");
                formData.append("creationUserId", expresso.Security.getUserInfo().id);
                formData.append("fileName", "picture_" + new Date().getTime());
                formData.append("maxWidth", this.options.maxWidth);
                formData.append("file", file);

                var url = expresso.Common.getWsUploadPathURL() + "/document" +
                    "?creationUserName=" + expresso.Common.getUserInfo().userName;
                return $.ajax({
                    url: url,
                    data: formData,
                    type: "POST",
                    cache: false,
                    contentType: false,
                    dataType: false,
                    processData: false,
                    success: function (data) {
                        $img.data("pictureDocument", data);
                    }
                });
            } else {
                return $.Deferred().reject();
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
                return this.$wrapper.find(".exp-picture img").data("pictureDocument");
            } else {
                //setter
                if (v) {
                    var url = expresso.Common.getWsResourcePathURL() + "/document/" + v.id + "/file/" + v.fileName;
                    this._displayPicture(url, v);
                } else {
                    this._displayPicture(null, null);
                }
            }
        },

        /**
         *
         * @param readonly
         */
        readonly: function (readonly) {
            this.readOnly = readonly;

            if (readonly) {
                this.$wrapper.find(".exp-picture .fa-plus").hide();
                this.$wrapper.find(".exp-picture .fa-trash").hide();
            } else {
                this.$wrapper.find(".exp-picture .fa-plus").show();
                this.$wrapper.find(".exp-picture .fa-trash").show();
            }
        },

        /**
         *
         * @param options
         */
        setOptions: function (options) {
            $.extend(true, this.options, options);
        }
    });

    ui.plugin(ExpressoPicturePicker);
}(jQuery, window.kendo));