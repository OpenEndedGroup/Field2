// standard library for Field

var Vec3 = Java.type('field.linalg.Vec3');
var Vec4 = Java.type('field.linalg.Vec4')
var Vec2 = Java.type('field.linalg.Vec2')
var Quat = Java.type('field.linalg.Quat')
var FLine = Java.type('field.graphics.FLine')
var System = Java.type('java.lang.System');
var Asta = Java.type('fielded.live.Asta');

var __h__ = new Asta();

var __MINUS__ = Asta.__MINUS__;
var __PLUS__ = Asta.__PLUS__;
var __MULTIPLY__ = Asta.__MULTIPLY__;
var __NUMBER_LITERAL__ = (start, end, def) => __h__.__LNC__(start, end, def);

_.sourceTransformer = __h__.transformer(_)

var V = function (x,y,z,w) {
    if (arguments.length==0) return new Vec4(0,0,0,1)
    if (arguments.length==1) return new Vec4(x,x,x,1)
    if (arguments.length==2) return new Vec2(x,y)
    if (arguments.length==3) return new Vec3(x,y,z)
    if (arguments.length==4) return new Vec4(x,y,z,w)
    throw "can't make a vector given this ("+arguments.length+") number of arguments"
}

