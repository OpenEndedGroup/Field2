// standard library for Field

var Vec3 = Java.type('field.linalg.Vec3');
var Vec4 = Java.type('field.linalg.Vec4')
var Vec2 = Java.type('field.linalg.Vec2')
var Quat = Java.type('field.linalg.Quat')
var FLine = Java.type('field.graphics.FLine')
var System = Java.type('java.lang.System');
var Asta = Java.type('fielded.live.Asta');
var Transform2D = Java.type("field.linalg.Transform2D")

var __h__ = new Asta();

var __MINUS__ = Asta.__MINUS__;
var __PLUS__ = Asta.__PLUS__;
var __MULTIPLY__ = Asta.__MULTIPLY__;
var __NUMBER_LITERAL__ = (start, end, def) => __h__.__LNC__(start, end, def);

_.sourceTransformer = __h__.transformer(_)

var vec = function (x, y, z, w) {
    if (arguments.length == 0) return new Vec4(0, 0, 0, 1)
    if (arguments.length == 1) return new Vec4(x, x, x, 1)
    if (arguments.length == 2) return new Vec2(x, y)
    if (arguments.length == 3) return new Vec3(x, y, z)
    if (arguments.length == 4) return new Vec4(x, y, z, w)
    throw "can't make a vector given this (" + arguments.length + ") number of arguments"
}

vec.__doc__ = "shorthand for creating a vector. `vec(1,2,3)` makes a `Vec3`, while `vec(4,-2.3)` makes a `Vec2` etc."

var rotate = function (angle, pivotx, pivoty) {
    if (arguments.length == 0) return Transform2D.rotate(0)
    if (arguments.length == 1) return Transform2D.rotate(angle)
    if (arguments.length == 2) return Transform2D.rotate(angle, pivotx)
    if (arguments.length == 3) return Transform2D.rotate(angle, pivotx, pivoty)
    throw "can't make a rotation given this (" + arguments.length + ") number of arguments"
}

rotate.__doc__ = "shorthand for creating a transformation that rotates. `rotate(10)` rotates things 10 degrees clockwise. Other numbers of arguments are also accepted: `rotate(10, vec(50, -40))` will rotate 10 degrees around the point `50, 40`"

var scale = function (scale, pivot, pivoty, x) {
    if (arguments.length == 0) return Transform2D.scale(1)
    if (arguments.length == 1) return Transform2D.scale(scale)
    if (arguments.length == 2) return Transform2D.scale(scale, pivot)
    if (arguments.length == 3) return Transform2D.scale(scale, pivot, pivoty)
    if (arguments.length == 4) return Transform2D.scale(scale, pivot, pivoty, x) // mislabelled
    throw "can't make a scale given this (" + arguments.length + ") number of arguments"
}

scale.__doc__ = "shorthand for creating a transformation that scale. `scale(10)` scales things (up) by a factor of 10 . Other numbers of arguments are also accepted: `scale(10, vec(50, -40))` will scale 10x degrees around the point `50, 40`. `scale(10,20)` creates a non-uniform scale that scales 10x in the X direction and 20x in the Y direction."

var translate = function (x, y) {
    if (arguments.length == 0) return Transform2D.translate(0, 0)
    if (arguments.length == 1) return Transform2D.translate(x)
    if (arguments.length == 2) return Transform2D.translate(x, y)
    throw "can't make a translation given this (" + arguments.length + ") number of arguments"
}

rotate.__doc__ = "shorthand for creating a transformation that translates. `rotate(10)` rotates things 10 degrees clockwise. Other numbers of arguments are also accepted: `rotate(10, vec(50, -40))` will rotate 10 degrees around the point `50, 40`"

