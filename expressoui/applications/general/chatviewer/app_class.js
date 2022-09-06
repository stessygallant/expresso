expresso.applications.general.chatviewer.ChatViewer = expresso.layout.applicationbase.ApplicationBase.extend({
    webSocket: undefined,

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.ApplicationBase.fn.initDOMElement.call(this, $domElement);
        var _this = this;

        var $webSocketReady = $.Deferred();

        var path = expresso.Common.getWsBasePathURL();
        path = path.substring("http".length); // keep the "s"
        this.webSocket = new WebSocket("ws" + path + "/websocket/chat");
        this.webSocket.onerror = function (event) {
            console.warn("Error on websocket", event);
        };

        this.webSocket.onopen = function (event) {
            // console.log("Connection established");
            $webSocketReady.resolve();
        };

        this.webSocket.onmessage = function (event) {
            // if the chat is hidden, show it
            _this.$domElement.closest(".chat-viewer-div").show();

            var message = JSON.parse(event.data);
            _this.appendMessage(message);
        }

        $webSocketReady.done(function () {
            _this.sendMessage("Connected", false);
        });

        this.$domElement.find(".response").kendoEditor({
            resizable: {
                content: false
            },
            tools: [
                "bold",
                "italic",
                "underline",
                "foreColor",
                "backColor"
            ],
            execute: function (e) {
                if (e.name == "insertparagraph") {
                    _this.sendResponse();
                }
            }
        });

        // listen to the send button
        this.$domElement.find(".send-response-div").on("click", function () {
            _this.sendResponse();
        });

        this.$domElement.find(".close-button").on("click", function () {
            $(this).closest(".chat-viewer-div").hide();
            _this.clean();
        });

        this.$domElement.on("mouseenter", function () {
            window.setTimeout(function () {
                _this.$domElement.find(".chat-session .messages .new-message").removeClass("new-message");
            }, 2000);
        });
    },

    /**
     *
     */
    sendResponse: function () {
        var kendoEditor = this.$domElement.find(".response").data("kendoEditor");
        var content = kendoEditor.value();
        // console.log("content [" + content + "]");
        this.sendMessage(content);

        // on Chrome, this needs to be delayed
        window.setTimeout(function () {
            kendoEditor.value(null);
        }, 1);
    },

    /**
     *
     * @param content
     * @param append Append to the chat session. Default is true.
     */
    sendMessage: function (content, append) {
        if (content) {
            var message = {
                fromUserId: expresso.Security.getUserInfo().id,
                from: expresso.Security.getUserInfo().fullName,
                content: content.trim(),
                date: new Date()
            };
            this.webSocket.send(JSON.stringify(message));
            if (append !== false) {
                this.appendMessage(message, true);
            }
            return message;
        } else {
            return null;
        }
    },

    /**
     *
     * @param message
     * @param sentMessage true if the message has been sent by the user
     */
    appendMessage: function (message, sentMessage) {
        console.log("Got message", message);
        // create the message
        var $latestMessage = $("<div class='message new-message " + (sentMessage ? "sent-message" : "") + "'>" +
            "  <span class='from'>" + message.from + "</span>" +
            "  <span class='date'>" + (expresso.util.Formatter.formatDate(message.date,
                expresso.util.Formatter.DATE_FORMAT.DATE_TIME) || message.date) + "</span>" +
            "  <span class='content'>" + message.content + "</span>" +
            "</div>").appendTo(this.$domElement.find(".chat-session .messages"));

        if (this.$domElement.find(".chat-session .messages").outerHeight(true) > this.$domElement.find(".chat-session").height()) {
            // scroll to the bottom
            var scrollingElement = this.$domElement.find(".chat-session")[0];
            scrollingElement.scrollTop = scrollingElement.scrollHeight;
        }
    },

    /**
     *
     */
    clean: function () {
        this.$domElement.find(".chat-session .messages").empty();
    },

    // @override
    resizeContent: function () {

    },

    // @override
    destroy: function () {

        expresso.layout.applicationbase.ApplicationBase.fn.destroy.call(this);
    }
});