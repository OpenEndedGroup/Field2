var currentmodal = null;

function runModal(placeholder, getcompletionsfunction, cssclass, initialText, allowAlternative) {
    var modal = $("<dialog class='" + cssclass + "'><input spellcheck='false' data-autosize-input='{ \"space\": 10 }' autocomplete='off' placeholder='" + placeholder + "' class='Field-textBox' type='text' name='main'></input><ol></ol></dialog>")

    modal.appendTo($("body"))

    var inputBox = modal.find("input")
    var ol = modal.find("ol")

    if (initialText)
        inputBox.val(initialText)

    console.log()

    function updateCompletions() {
        $(ol).empty()
        console.log("text is currently " + inputBox.val())
        var completions = getcompletionsfunction(inputBox.val())
        console.log(completions)

        for (var i = 0; i < completions.length; i++) {
            var num = ""
            if (i == 0) {
                num = "&crarr;"
            } else if (i < 10) {
                num = "^" + i
            }
            var label = $("<li><span class='Field-number'>" + num + "</span>" + completions[i].text + "</li>")
            if (num=="")
	            label = $("<li><span class='Field-number' style='opacity:0.0' >&nbsp;</span>" + completions[i].text + "</li>")
            var callback = completions[i].callback
            label.hover(function () {
                $(this).css("background", "#444")
            }, function () {
                $(this).css("background", "")
            })
            label.click(function () {
                callback(inputBox.val())
                modal[0].close()
                modal.detach()
                setTimeout(function () {
                    cm.focus()
                }, 25)
            })

            ol.append(label)
        }
        return completions
    }


    updateCompletions()

    inputBox.on("input", function (x) {
        console.log(this.value)
        updateCompletions()
    })

    function highlightRunAndClose(value, index, event, allowAlternative) {
        var cc = updateCompletions(value)
        if (cc.length > index) {
            cc[index].callback(value)
            $($(ol).children("li")[index]).css("background", "#555")
            event.preventDefault()

            setTimeout(function () {
                modal[0].close()
                modal.detach()
                cm.focus()
            }, 25)
        }
        else if (allowAlternative)
        {
            console.log(" value is "+value);
            allowAlternative(inputBox.val())
            event.preventDefault()
            setTimeout(function () {
                modal[0].close()
                modal.detach()
                cm.focus()
            }, 25)
        }

        setTimeout(function () {
            cm.focus()
        }, 25)
        cm.focus()

    }

    console.log("allowAlternative = "+allowAlternative)

    inputBox.on("keydown", function (x) {
        console.log(x.keyCode)
        if (x.keyCode == 13) {
            highlightRunAndClose(this.value, 0, x, this.allowAlternative)
        }
        if (x.keyCode == 27) {
            setTimeout(function () {
                cm.focus()
            }, 25)
        }
        if (x.keyCode > 48 && x.keyCode < 48 + 10 && x.ctrlKey) {
            highlightRunAndClose(this.value, x.keyCode - 48, x)
        }
    }.bind({"allowAlternative":allowAlternative}))

    modal[0].showModal()

//    $("input[data-autosize-input]").autosizeInput()
//
//    $(modal[0]).width($($(modal[0]).children()[1]).width())

		currentmodal = modal;

    return modal
}


//runModal("running modal...", completme, "Field-modal")


function runModalAtCursor(placeholder, completeme, initialText) {
    var m = runModal(placeholder, completeme, "Field-modal-positioned", initialText)
    var cc = cm.cursorCoords()
    console.log(cc)
    var h = $(m).height()
    if (cc.bottom > $(window).height() / 2)
        $(m).css("top", Math.max(100, cc.bottom-h))
    else
    {
        $(m).css("top", cc.bottom)
    }

    var p = $(m).position()
    console.log(" checking height :"+p.top+" "+h+" "+$(window).height())
    if (p.top+h>$(window).height()-40)
	{
		h = $(window).height()-p.top-100
		console.log(" set height to be "+h);
		$($(m).children()[1]).height(h)
	}
    $(m).css("left", cc.right)
}


//runModalAtCursor("banana", completme)