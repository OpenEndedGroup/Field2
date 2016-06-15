//This module displays a help box (next to the status box) that, when clicked pulls up a nifty little menu that displays all current key bindings.
// it's currently disabled right now, since we're moving to having the editor in the same window as everything else (including a status bar)

//helpBox
var helpBox = $("<div class='Field-status' id = 'hbox'></div>");

helpBox.appendTo($(".CodeMirror"))
helpBox.html("");

//Help Menu
//var helpMenu = $("")

helpBox.css("bottom", "1.5em");
helpBox.css("position", "absolute")
helpBox.css("opacity", "0.3");
// helpBox.css("text-align", "right");
//helpBox.css("opacity", "1");

function setHelpBox(text) {
    helpBox.css("transition", "opacity 0s");
    helpBox.css("opacity", 1);
    helpBox.html(text)
    setTimeout(function () {
        helpBox.css("transition", "opacity 0.5s");
        helpBox.css("opacity", 0.3)
    }, 1000)
}

//function to pull up help menu
function showHelpMenu() {
    goCommands();
}

setHelpBox();

//Highlights box on mouseover
$('#hbox').mouseover(function () {
    setHelpBox();
});

$('#hbox').mousedown(function () {
    showHelpMenu();
});


