////This module displays a help box (next to the status box) that, when clicked pulls up a nifty little menu that displays all current key bindings.

//helpBox
var helpBox = $("<div class='Field-status' id = 'hbox'></div")
helpBox.appendTo($("body"))

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



