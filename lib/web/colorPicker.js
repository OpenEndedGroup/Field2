function addColorPicker() {
    var widget_div = makeColorPicker(getCurrentColor, setCurrentColor);
    var selection_start = cm.getCursor(true);
    var selection_end = cm.getCursor(false);

    if ($(widget_div).is(":visible")) {
        $(widget_div).show();
    } else {
        var widget = cm.addLineWidget(selection_end.line, widget_div);
    }
}

function makeColorPicker(getCurrentValue, setCurrentValue) {
    var count = document.getElementsByClassName("Field-remoteColor").length;
    var widget_div = $('<div class="Field-remoteColor"><div class="Field-closebox">&#x2715;</div><form><input type="text" id="color' + count + '" name="color" value="' + getCurrentValue() + '" /></form><div class="colorpicker" id="colorpicker' + count + '"></div></div>')[0];

    var farb = $.farbtastic($(widget_div).find(".colorpicker"), $(widget_div).find('input[type=text]'));

    $('body').on('keypress', ".Field-remoteColor", function (e) {
        var code = e.keyCode || e.which;
        if (code == 13) {
            e.preventDefault();
            setCurrentValue(farb.color);
        }
    });

    var closeBox = $($(widget_div).find(".Field-closeBox"));

    closeBox.click(function () {
        $(widget_div).remove()
    });

    return widget_div;
}

function getCurrentColor() {
    return cm.getSelection();
}

function setCurrentColor(v) {
    cm.replaceSelection(v)
}