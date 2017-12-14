var tabcontent = $(".tabcontent")
var tabtray = $(".tabtray")

var selectedTab = null

var ensureTabCalled = function (externalName, internalName, selectedCallbackHandler) {
    var f = tabcontent.find("#target-" + internalName)
    var b = tabtray.find("#tab-" + internalName)
    if (f.length == 0) {
        f = $("<div class='tab' id='target-" + internalName + "'></div>")
        tabcontent.append(f)
        f.css("display", "none")

        var b = $("<div class='tabbutton' id='tab-" + internalName + "' onclick='selectTab(\"" + internalName + "\")'>" + externalName + "</div>")
        tabtray.append(b)

    }
    if (selectedCallbackHandler)
        b[0].selectedCallbackHandler = selectedCallbackHandler

    if (numTabs() == 1) {
        selectTab(internalName)
    }

    return f[0]
}


var selectTab = function (internalName) {
    var foundContent = null;

    tabcontent.children().each(function (index, value) {
        if (value.id == 'target-' + internalName) {
            foundContent = value
            $(value).css("display", "block")
        }
        else
            $(value).css("display", "none")
    })
    tabtray.children().each(function (index, value) {
        if (value.id == 'tab-' + internalName) {
            $(value).addClass("tabdown")

            selectedTab = internalName
            if (value.selectedCallbackHandler) {
                console.log(" selected callback handler is '" + value.selectedCallbackHandler + "'")
                var h = value.selectedCallbackHandler()
                if (h)
                    $(foundContent).html(h)
            }
        }
        else
            $(value).removeClass("tabdown")
    })

    if (typeof cm !== 'undefined') {
        cm.refresh()
    }

    if (typeof updateAllBrackets !== 'undefined') {
        updateAllBrackets()
    }
}

var setTabContentTo = function(internalName, content)
{
    tabcontent.children().each(function (index, value) {
        if (value.id == 'target-' + internalName) {
            $(value).html(content)
        }
    })
}

var isSelectedTab = function (internalName) {
    return selectedTab == internalName
}

var numTabs = function () {
    return tabcontent.children().length
}

