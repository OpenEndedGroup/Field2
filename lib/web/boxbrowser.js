
var currentSel = null

_messageBus.subscribe("selection.changed", function (d, e) {
    currentSel = d.box
    if (isSelectedTab("info")) {
        selectTab("info") // rerun content manager
    }
})

ensureTabCalled("info", "info", function () {
    var id = currentSel
    if (id)
        return "<iframe style=\"visibility:hidden;\" onload=\"this.style.visibility = 'visible';\" src='/id/" + id + "'></iframe>"
    else
        return ""
})

// var initialPage = "/doc/field/graphics/FLine"
var initialPage = "";

var welcomeOnce = true

ensureTabCalled("?", "help", function () {
    if (welcomeOnce) {
        return "<iframe style=\"visibility:hidden;\" onload=\"this.style.visibility = 'visible';\" src='" + initialPage + "'></iframe>"
        welcomeOnce = false
    }
    return null
})

showInfoURL = function(url)
{
    initialPage = url
    setTabContentTo("help", "<iframe style=\"visibility:hidden;\" onload=\"this.style.visibility = 'visible';\" src='" + url + "'></iframe>")
    selectTab("help")
}

downloadAndOpen= function(t, name)
{
    var link = t.getAttribute("href")
    _field.send("reverse.execution", {code:"var Launch = Java.type('fielded.plugins.Launch'); new Launch().downloadDecompressAndOpen('"+name+"', '"+link+"')"})
}


