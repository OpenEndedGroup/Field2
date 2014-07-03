//This module displays a help box (next to the status box) that, when clicked pulls up a nifty little menu that displays all current key bindings.
var helpBox = $("<div class='Field-status' id='help'></div")
helpBox.appendTo($("body"))
helpBox.html("<span class='highlighted'>Connected</span> Field remote session")

//for Debugging (clearly)
console.log("Help Box Debugging")
console.log(helpBox.text())

helpBox.css("bottom", "3.5em")
helpBox.css("position", "absolute")
helpBox.css("opacity", "0.3")
helpBox.css("opacity", "1")


function setHelpBox(text) {
    helpBox.css("transition", "opacity 0s")
    helpBox.css("opacity", 1)
    helpBox.html(text)
    setTimeout(function () {
        helpBox.css("transition", "opacity 1s")
        helpBox.css("opacity", 0.2)
    }, 1000)
}

setHelpBox("<span class='highlighted'>Help</span>")


_messageBus.subscribe("helpBox", function (d, e) {
    setHelpBox(d)
})