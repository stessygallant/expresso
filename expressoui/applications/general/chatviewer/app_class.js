expresso.applications.general.chatviewer.ChatViewer = expresso.layout.applicationbase.ApplicationBase.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.applicationbase.ApplicationBase.fn.initDOMElement.call(this, $domElement);
        var _this = this;
        this.$domElement.append("AAAAAAA");

        var $webSocketReady = $.Deferred();

        var webSocket = new WebSocket("ws://localhost:8080/sherpaws/websocket/chat");
        webSocket.onerror = function (event) {
            console.warn("Error on websocket", event);
        };

        webSocket.onopen = function (event) {
            console.log("Now Connection established");
            $webSocketReady.resolve(webSocket);
        };

        webSocket.onmessage = function (event) {
            console.log(event);
            var message = JSON.parse(event.data);
            _this.appendMessage(message.content);
        }

        $webSocketReady.done(function (webSocket) {
            console.log("Sending message");
            var message = {
                from: "tete",
                content: "allo"
            };
            webSocket.send(JSON.stringify(message));
        });
    },

    /**
     *
     * @param message
     */
    appendMessage: function (message) {
        console.log("Got message[" + message + "]");
        this.$domElement.find(".chat-session").append("<div class='message'>" + message + "</div>");
    },

    // @override
    resizeContent: function () {

    },

    // @override
    destroy: function () {


        expresso.layout.applicationbase.ApplicationBase.fn.destroy.call(this);
    }
});