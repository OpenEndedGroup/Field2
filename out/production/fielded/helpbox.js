//This module displays a help box (next to the status box) that, when clicked pulls up a nifty little menu that displays all current key bindings.
var helpBox = $("<div class='Field-status' id='help'></div")
helpBox.appendTo($("body"))
helpBox.html("<input id="clickMe" type="button" value="clickme" onclick="console.log("Hello World");" />")

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
        helpBox.css("opacity", 1)
    }, 1000)
}

setHelpBox("<span class='highlighted'>Help</span>")


_messageBus.subscribe("helpBox", function (d, e) {
    setHelpBox(d)
})