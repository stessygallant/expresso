(function ($, kendo) {
    var ui = kendo.ui,
        Widget = ui.Widget;

    //  <input type="file" accept="image/*" capture="environment">
    var ExpressoPicturePicker = Widget.extend({
        $element: undefined,
        $wrapper: undefined,
        readOnly: false,

        // these options are mandatory
        resourceSecurityPath: undefined,
        resourceName: undefined,
        resourceId: undefined,
        documentTypePgmKey: "PICTURE",
        maxWidth: 800,

        init: function (element, options) {
            Widget.fn.init.call(this, element, options);

            //console.log("Options", this.options);
            this.$element = $(element);

            if (!this.resourceSecurityPath) {
                this.resourceSecurityPath = this.resourceName;
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
            if (initialValue !== undefined) {
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

            this.$wrapper.append("<div class='exp-picture'>" +
                "<img src='' alt=''>" +
                "<span class='fa fa-trash'></span>" +
                "<span class='fa fa-plus fa-2x'></span>" +
                "</div>");

            var $pictureFile = this.$element;
            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $trash = $pictureDiv.find(".fa-trash");
            var $add = $pictureDiv.find(".fa-plus");
            var $img = $pictureDiv.find("img");
            var img = $img[0];

            // listen on ADD
            $add.on("click", function () {
                $pictureFile.trigger("click");
            });

            // load the picture
            $pictureFile.on("change", function () {
                var file = $pictureFile[0].files[0];
                var imgURL = URL.createObjectURL(file);
                _this._displayPicture(imgURL);

                // save picture
                _this.savePicture(file, imgURL);
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

            // delete picture
            var pictureDocument = $img.data("pictureDocument");
            if (pictureDocument) {
                expresso.Common.sendRequest("document/" + pictureDocument.id, "delete").done(function () {
                    $img.removeData("pictureDocument");
                });
            }
            img.src = "";
            $trash.hide();
        },

        /**
         *
         * @param imgURL
         * @param resourceId
         * @returns {*|Promise<string[] | null>}
         */
        savePicture: function (imgURL, resourceId) {
            var $pictureDiv = this.$wrapper.find(".exp-picture");
            var $img = $pictureDiv.find("img");
            var img = $img[0];

            if (resourceId) {
                this.resourceId = resourceId;
            }
            if (imgURL.startsWith("http")) {
                // if it is a URL, already saved
                return $.Deferred().resolve();
            } else {
                var formData = new FormData();
                formData.append("type", "document");
                formData.append("resourceSecurityPath", this.resourceSecurityPath);
                formData.append("resourceName", this.resourceName);
                formData.append("resourceId", this.resourceId);
                formData.append("documentTypePgmKey", this.documentTypePgmKey);
                formData.append("description", "Picture");
                formData.append("creationUserId", expresso.Security.getUserInfo().id);
                formData.append("fileName", "picture_" + new Date().getTime());
                formData.append("maxWidth", this.maxWidth);
                formData.append("file", img);

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

        options: {
            // the name is what it will appear in the kendo namespace (kendo.ui.ExpressoPicturePicker).
            // The jQuery plugin would be jQuery.fn.kendoExpressoPicturePicker
            name: "ExpressoPicturePicker",
            readonly: false
        }
    });

    ui.plugin(ExpressoPicturePicker);
}(jQuery, window.kendo));