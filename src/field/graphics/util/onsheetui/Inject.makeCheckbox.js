var SimpleCanvas = Java.type('field.graphics.util.onsheetui.SimpleCanvas')
_.setClass(SimpleCanvas.class)
_.clear()

get = () => { return _.${property} }
set = (now) => {
    if (now<0) now = 0
    if (now>1) now = 1
    _.replace.${property} = now
    _.label.e = ""+now
    return now
}

_.label.w = "${property} ="
set(get())

checkBox(_, _.frame.w, _.frame.h, get, set)