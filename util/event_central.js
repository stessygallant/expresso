var expresso = expresso || {};
expresso.util = expresso.util || {};

/**
 * Implementation of the event central
 */
expresso.util.EventCentral = kendo.Class.extend({

    // name of this EventCentral instance (for logging purpose)
    name: undefined,

    // to publish and subscribe to event, we need a DOM element
    $domElement: undefined,

    // it is possible to enable/disable the event propagation
    enabled: true,

    /**
     * Method called when a new instance is created
     * @param name  reference to the resource manager
     */
    init: function (name) {
        this.name = name.replace("/", "_");
        this.$domElement = $("<div class='ec-" + this.name + "'></div>");
    },

    /**
     * Subscribe to an event.
     * Always use the DOM element of the resource manager as the
     *
     * @param e event type. Could be a single event or an array
     * @param eh event handler
     * @param [onceOnly]
     */
    subscribeEvent: function (e, eh, onceOnly) {
        if ($.isArray(e)) {
            for (var i = 0; i < e.length; i++) {
                if (onceOnly) {
                    this.$domElement.one(e[i], eh);
                }
                else {
                    this.$domElement.on(e[i], eh);
                }
            }
        }
        else {
            if (onceOnly) {
                this.$domElement.one(e, eh);
            }
            else {
                this.$domElement.on(e, eh);
            }
        }
    },

    /**
     * Publish an event
     * @param e event type.
     * @param data data for the event
     */
    publishEvent: function (e, data) {
        if (this.enabled) {
            //console.log("Event [" + e + "] published on central [" + this.name + "]: " + (data ? data.id : null));
            this.$domElement.trigger(e, data);
        }
    },

    destroy: function () {
        $("div.ec-" + this.name).remove();
        this.$domElement = null;
    }
});

