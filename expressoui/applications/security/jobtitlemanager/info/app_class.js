expresso.applications.security.jobtitlemanager.info.InfoManager = expresso.layout.resourcemanager.ResourceManager.extend({

    // @override
    init: function (applicationPath) {
        console.log("[" + this.getLabel("numberValue") + "]");
        var fields = {
            type: {
                type: "string",
                editable: false,
                defaultValue: "jobTitleInfo"
            },
            jobTitleId: {
                type: "number",
                reference: true
            },
            pgmKey: {
                type: "string",
                unique: true,
                maxLength: 20
            },
            description: {
                type: "string",
                maxLength: 200
            },
            infoType: {
                type: "string",
                maxLength: 50,
                defaultValue: "string",
                values: [{id: "number", label: this.getLabel("numberValue")},
                    {id: "date", label: this.getLabel("dateValue")},
                    {id: "string", label: this.getLabel("stringValue")},
                    {id: "text", label: this.getLabel("textValue")}
                ]
            },
            defaultNumber: {
                type: "number",
                nullable: true
            },
            defaultString: {
                type: "string",
                nullable: true,
                maxLength: 1000
            },
            defaultText: {
                type: "string",
                nullable: true,
                maxLength: 16777215
            },
            defaultDate: {
                type: "date",
                nullable: true
            }
        };

        expresso.layout.resourcemanager.ResourceManager.fn.init.call(this, applicationPath, "info", fields, {
            grid: true,
            form: true
        });
    }
});
