var currentmodal = null;

function runModal(placeholder, getcompletionsfunction, cssclass, initialText, allowAlternative) {

		var w = $(".CodeMirror").width()
		var h = $(".CodeMirror").height()

		var mx= h - 100;

		var shift = ($(window).height()-h)/2
		var shiftx = ($(window).width()-w)/2

    var modal = $("<dialog class='" + cssclass +"'><input spellcheck='false' data-autosize-input='{ \"space\": 10 }' autocomplete='off' placeholder='" + placeholder + "' class='Field-textBox' type='text' name='main'></input><ol style='max-height:"+mx+"'></ol></dialog>");

    modal.appendTo($("body"));

		modal.css("top", -shift);
		modal.css("bottom", shift);

		modal.css("left", -shiftx);
		modal.css("right", shiftx);


    var inputBox = modal.find("input");
    var ol = modal.find("ol");

    if (initialText)
        inputBox.val(initialText);


    function updateCompletions() {
        $(ol).empty();
        var completions = getcompletionsfunction(inputBox.val());

        for (var i = 0; i < completions.length; i++) {
            var num = "";
            if (i == 0) {
                num = "&crarr;"
            } else if (i < 10) {
                num = "^" + i
            }
            var label = $("<li><span class='Field-number'>" + num + "</span>" + completions[i].text + "</li>");
            if (num == "")
                label = $("<li><span class='Field-number' style='opacity:0.0' >&nbsp;</span>" + completions[i].text + "</li>");
            var callback = completions[i].callback;
            label.hover(function () {
                    $(this).css("background", "#444")
                }
                , function () {
                    $(this).css("background", "")
                });
            label.click(function () {
                this.callback(inputBox.val());
                setTimeout(function () {
                    modal[0].close();
                    modal.detach();
                    cm.focus()
                }, 25);
                cm.focus()
            }.bind({"callback": completions[i].callback}));

            ol.append(label)
        }
        return completions
    }

    updateCompletions();

    inputBox.on("input", function (x) {
        updateCompletions()
    });

    function highlightRunAndClose(value, index, event, allowAlternative) {
        var cc = updateCompletions(value);
        if (cc.length > index) {
            cc[index].callback(value);
            $($(ol).children("li")[index]).css("background", "#555");
            event.preventDefault();

            setTimeout(function () {
                modal[0].close();
                modal.detach();
                cm.focus()
            }, 25)
        }
        else if (allowAlternative) {
            allowAlternative(inputBox.val());
            event.preventDefault();
            setTimeout(function () {
                modal[0].close();
                modal.detach();
                cm.focus()
            }, 25)
        }

        setTimeout(function () {
            cm.focus()
        }, 25);
        cm.focus()

    }


    inputBox.on("keydown", function (x) {
        if (x.keyCode == 13) {
            highlightRunAndClose(this.value, 0, x, this.allowAlternative)
        }
        if (x.keyCode == 27) {
            setTimeout(function () {
                cm.focus()
            }, 25);
            cm.focus();
        }
        if (x.keyCode > 48 && x.keyCode < 48 + 10 && x.ctrlKey) {
            highlightRunAndClose(this.value, x.keyCode - 48, x)
        }
    }.bind({"allowAlternative": allowAlternative}));

    modal[0].showModal();

    //    $("input[data-autosize-input]").autosizeInput()
    //
    //    $(modal[0]).width($($(modal[0]).children()[1]).width())

    currentmodal = modal;

    return modal
}

//runModal("running modal...", completme, "Field-modal")

function runModalAtCursor(placeholder, completeme, initialText) {
    var m = runModal(placeholder, completeme, "Field-modal-positioned", initialText);
    var cc = cm.cursorCoords();
    var h = $(m).height();
    if (cc.bottom > $(".CodeMirror").height() / 2)
        $(m).css("top", Math.max(100, cc.bottom - h));
    else {
        $(m).css("top", cc.bottom)
    }

    var p = $(m).position();
    if (p.top + h > $(".CodeMirror").height() - 40) {
        h = $(".CodeMirror").height() - p.top - 100;
        $($(m).children()[1]).height(h)
    }
    $(m).css("left", Math.min($(".CodeMirror").width()-500, cc.right));
}

//runModalAtCursor("banana", completme)