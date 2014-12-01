//This module displays a help box (next to the status box) that, when clicked pulls up a nifty little menu that displays all current key bindings.
// it's currently disabled right now, since we're moving to having the editor in the same window as everything else (including a status bar)

//helpBox
var helpBox = $("<div class='Field-status' id = 'hbox'></div>")

//helpBox.appendTo($("body"))
helpBox.html("Commands &#8963;&#9251;")


//Help Menu
//var helpMenu = $("")

helpBox.css("bottom", "3.5em")
//helpBox.css("position", "relative")
helpBox.css("opacity", "0.3")
helpBox.css("opacity", "1")

function setHelpBox () {
    helpBox.css("transition", "opacity 0s")
    helpBox.css("opacity", 1)
//    helpBox.html(text)
    setTimeout(function () {
        helpBox.css("transition", "opacity 1s")
        helpBox.css("opacity", 1)
    }, 1000)
}

//function to pull up help menu
function showHelpMenu()
{
    goCommands();
}


setHelpBox()


//Highlights box on mouseover
$('#hbox').mouseover(function(){
    setHelpBox();
});

$('#hbox').mousedown(function(){
    showHelpMenu();
});


goCommands = function () {
    _field.sendWithReturn("request.commands", {
            box: cm.currentbox,
            property: cm.currentproperty,
            text: cm.getValue(),
            line: cm.listSelections()[0].anchor.line,
            ch: cm.listSelections()[0].anchor.ch
        },
        function (d, e) {
            var completions = []
            for (var i = 0; i < d.length; i++) {
                d[i].callback = function () {
                    _field.send("call.command", {
                        command: this.call
                    });
                }.bind({
                        "call": d[i].call
                    })
                d[i].callback.remote = 1
                completions.push(d[i])
            }
            completions = completions.concat(globalCommands)
            completions.sort(function (a, b) {
                return a.name < b.name ? -1 : 1;
            })

            completionFunction = function (e) {
                var m = []

                var fuzzyPattern = fuzzy(e);

                for (var i = 0; i < completions.length; i++) {
                    if (completions[i].name.search(fuzzyPattern) != -1) {
                        matched = completions[i].name.replace(fuzzyPattern, replacer);
                        m.push({
                            text: matched + " <span class=doc>" + completions[i].info + "</span>",
                            callback: function () {
                                completions[this.i].callback()
                            }.bind({
                                    "i": i
                                })
                        })
                    }
                }
                return m
            }
            if (completions.length > 0)
                runModal("Commands...", completionFunction, "Field-Modal")
        }
    );
}



