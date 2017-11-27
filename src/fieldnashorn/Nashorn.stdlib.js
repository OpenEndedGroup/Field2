var Vec3 = Java.type('field.linalg.Vec3');
var System = Java.type('java.lang.System');
var Asta = Java.type('fielded.live.Asta');

var __h__ = new Asta();
_.asta = __h__.options();

var __MINUS__ = Asta.__MINUS__;
var __PLUS__ = Asta.__PLUS__;
var __MULTIPLY__ = Asta.__MULTIPLY__;
var __NUMBER_LITERAL__ = (start, end, def) => __h__.__LNC__(start, end, def);

_.sourceTransformer = __h__.transformer(_)
