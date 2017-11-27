// standard library for Field
var Vec3 = Java.type('field.linalg.Vec3')
var Vec4 = Java.type('field.linalg.Vec4')
var Vec2 = Java.type('field.linalg.Vec4')
var FLine = Java.type('field.graphics.FLine')

var V = function (x,y,z,w) {
    _.out(arguments.length)
    if (arguments.length==0) return new Vec4(0,0,0,1)
    if (arguments.length==1) return new Vec4(x,x,x,1)
    if (arguments.length==2) return new Vec2(x,y)
    if (arguments.length==3) return new Vec3(x,y,z)
    if (arguments.length==4) return new Vec4(x,y,z,w)
    throw "can't make a vector given this ("+arguments.length+") number of arguments"
}

