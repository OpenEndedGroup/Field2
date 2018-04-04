// standard library for Field

_.withOverloading=true // controversial

var Vec3 = Java.type('field.linalg.Vec3');
var Vec4 = Java.type('field.linalg.Vec4')
var Vec2 = Java.type('field.linalg.Vec2')
var Quat = Java.type('field.linalg.Quat')
var FLine = Java.type('field.graphics.FLine')
var System = Java.type('java.lang.System');
var Asta = Java.type('fielded.live.Asta');
var Transform2D = Java.type("field.linalg.Transform2D")
var Anim = Java.type("field.utility.Drivers")
var Math = Java.type("java.lang.Math")

// specifically for UChicago Class
var Mocap = Java.type("trace.mocap.Mocap")
//var Trackers = Java.type("fieldboof.Trackers")
var SoundAnalysis = Java.type("trace.sound.SoundAnalysis")
var Intersections = Java.type("trace.util.Intersections")
var PhysicsSystem = Java.type("trace.physics.PhysicsSystem")
var Boids = Java.type("trace.simulation.Boids")
var Production = Java.type("trace.util.Production")
var RandomCascade = Java.type("trace.random.RandomCascade")
var Random = Java.type("trace.random.Random")
var Rect = Java.type('field.utility.Rect')


var Inject = Java.type("field.graphics.util.onsheetui.Inject")


var __h__ = new Asta();

var __MINUS__ = Asta.__MINUS__;
var __PLUS__ = Asta.__PLUS__;
var __MULTIPLY__ = Asta.__MULTIPLY__;
var __DIVIDE__ = Asta.__DIVIDE__;
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

rotate.__doc__ = "shorthand for creating a transformation that rotates. `rotate(10)` rotates things 10 degrees clockwise. Other numbers of arguments are also accepted: `rotate(10, vec(50, -40))` will rotate 10 degrees around the point `50, -40`"

var scale = function (scale, pivot, pivoty, x) {
    if (arguments.length == 0) return Transform2D.scale(1)
    if (arguments.length == 1) return Transform2D.scale(scale)
    if (arguments.length == 2) return Transform2D.scale(scale, pivot)
    if (arguments.length == 3) return Transform2D.scale(scale, pivot, pivoty)
    if (arguments.length == 4) return Transform2D.scale(scale, pivot, pivoty, x) // mislabelled
    throw "can't make a scale given this (" + arguments.length + ") number of arguments"
}

scale.__doc__ = "shorthand for creating a transformation that scale. `scale(10)` scales things (up) by a factor of 10 . Other numbers of arguments are also accepted: `scale(10, vec(50, -40))` will scale 10x degrees around the point `50, -40`. `scale(10,20)` creates a non-uniform scale that scales 10x in the X direction and 20x in the Y direction."

var translate = function (x, y) {
    if (arguments.length == 0) return Transform2D.translate(0, 0)
    if (arguments.length == 1) return Transform2D.translate(x)
    if (arguments.length == 2) return Transform2D.translate(x, y)
    throw "can't make a translation given this (" + arguments.length + ") number of arguments"
}

rotate.__doc__ = "shorthand for creating a transformation that translates. `rotate(10)` rotates things 10 degrees clockwise. Other numbers of arguments are also accepted: `rotate(10, vec(50, -40))` or even `rotate(10, 50, -40)` will rotate 10 degrees around the point `50, -40`"

var _ft = function() {
    if (_.f)
    {
        return (_.frame.x + _t()*_.frame.w - _.f.frame.x)/_.f.frame.w
    }
    return 0.5
}
_ft.__doc__ = "for 'Box Pair' boxes, this gives you the value of `_t()` with respect to the footage box (the little one under the main box)"

// this is a dreadful hack

_.intersectX = function(x) {
    return new Intersections(_).x(x)
};

_.intersectX.__doc__ = "_.intersectX(55) returns all of the positions where there are `FLine` that intersect with a vertical line at x=55. Only lines that are marked as `.notation=true` are considered. You can write __.intersectX(55) if you want to cover all boxes."

__.intersectX = function(x) {
    return new Intersections(__).x(x)
};

__.intersectX.__doc__ = "_.intersectX(55) returns all of the positions where there are `FLine` that intersect with a vertical line at x=55. Only lines that are marked as `.notation=true` are considered. You can write __.intersectX(55) if you want to cover all boxes."

_.intersectY = function(y) {
    return new Intersections(_).y(y)
};

_.intersectY.__doc__ = "_.intersectY(55) returns all of the positions where there are `FLine` that intersect with a horizontal line at y=55. Only lines that are marked as `.notation=true` are considered. You can write _.intersectY(55) if you want to cover all boxes."

__.intersectY = function(y) {
    return new Intersections(__).y(y)
};

__.intersectY.__doc__ = "_.intersectY(55) returns all of the positions where there are `FLine` that intersect with a horizontal line at y=55. Only lines that are marked as `.notation=true` are considered. You can write __.intersectY(55) if you want to cover all boxes."

var Math_max = function(a, b)
{
    return Math.max(0.0+a, 0.0+b)
}
Math_max.__doc__ = "work around Nashorn's inability to select between Math.max(int, int) and Math.max(double, double)"

var Math_min = function(a, b)
{
    return Math.min(0.0+a, 0.0+b)
}
Math_min.__doc__ = "work around Nashorn's inability to select between Math.min(int, int) and Math.min(double, double)"

__.noLimits = function() {
    Java.type("fieldnashorn.Watchdog").limits=false
}

__.noLimits.__doc__ = "_.noLimit() removes all resource limit checking from Field. This means that loops can take longer than 5 seconds to complete, `_.lines` (and other places where you can put geometry) can take more than 1000 elements"
