var SimpleCanvas = Java.type('field.graphics.util.onsheetui.SimpleCanvas')
_.setClass(SimpleCanvas.class)
_.clear()

var get = () => { return (_.${property} - ${startRange}) /(${endRange} - ${startRange}) }
var set = (originally, previously, now) => {
    if (now<0) now = 0
    if (now>1) now = 1
    var v = ${startRange} + (${endRange} - ${startRange})*now
    _.replace.${property} = v
    _.label.e = v.toFixed(2)
    return now
}

_.label.w = "${property} ="
_.label.e = (_.${property}).toFixed(2)

makeSlider(_, _.frame.w, _.frame.h, get, set)