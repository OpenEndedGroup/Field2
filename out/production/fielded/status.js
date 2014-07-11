var statusBox = $("<div class='Field-status'></div")
statusBox.appendTo($("body"))
statusBox.html("<span class='highlighted'>Connected</span> Field remote session")

console.log(statusBox.text())

statusBox.css("opacity", "0.3")
statusBox.css("opacity", "1")

function setStatus(text) {
    statusBox.css("transition", "opacity 0s")
    statusBox.css("opacity", 1)
    statusBox.html(text)
    setTimeout(function () {
        statusBox.css("transition", "opacity 1s")
        statusBox.css("opacity", 0.2)
    }, 1000)
}

setStatus("<span class='highlighted'>Connected</span> Field remote session")


_messageBus.subscribe("status", function (d, e) {
    setStatus(d)
})