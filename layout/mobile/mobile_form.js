var expresso = expresso || {};
expresso.layout = expresso.layout || {};
expresso.layout.mobile = expresso.layout.mobile || {};

/**
 * Base class for any mobile form
 */
expresso.layout.mobile.MobileForm = expresso.layout.mobile.MobileView.extend({

    // @override
    initDOMElement: function ($domElement) {
        expresso.layout.mobile.MobileView.fn.initDOMElement.call(this, $domElement);
    },

    /**
     *
     * @param resource
     * @param model
     * @param options
     */
    initForm: function (resource, model, options) {
        var $form = this.$domElement.children("div");

        $form.kendoExpressoForm({
            labels: this.resourceManager.labels,
            resource: resource,
            model: model
        });

        if (!resource.id) {
            $form.find(".hide-new").each(function () {
                var $this = $(this);
                expresso.util.UIUtil.hideField($this);
            });
        }

        // TODO Form read only for now
        expresso.util.UIUtil.setFormReadOnly($form);

        // TODO Add default buttons
        //this.$domElement.append("<div class='button-bar'><button type='button' class='k-button'>Annuler</button><button type='button' class='k-button k-primary'>Enregistrer</button><div>")
    },

    /**
     * Bind the widget on the "change" event or bind the $input if not a widget
     * @param $input
     * @param onChangeCallback
     */
    bindOnChange: function ($input, onChangeCallback) {
        // we need to wait for the for to be ready
        expresso.util.UIUtil.bindOnChange($input, onChangeCallback);
    }
});
