
var vec = function (x, y, z, w) {
    if (arguments.length == 2) {
        return new THREE.Vector2(x, y)
    }
    else if (arguments.length == 3) {
        return new THREE.Vector3(x, y, z)
    }
    else if (arguments.length == 4) {
        return new THREE.Vector4(x, y, z, w)
    }
    else throw new Error(" can't make a vector out of " + arguments.length + " arguments")
}

vec.__doc__ = "Makes a Vector of various dimensions. For example vec(1,2) makes a THREE.Vector2(1,2), while vec(1.0,0,8,4) makes a THREE.Vector4(1.0,0,8,4). This is simply less typing."

var stage = new Proxy(new Map(), idempotencyHandler(function (n, is, was) {

    if (was != null && was.stroked && is != null && !is.stroked)
        (new FLine()).bindToName_s(n)
    if (was != null && was.filled && is != null && !is.filled)
        (new FLine()).bindToName_f(n)
    if (was != null && was.pointed && is != null && !is.pointed)
        (new FLine()).bindToName_p(n)

    if (is && !is.isFLine) return

    if (!is)
        throw Error("can't add null or undefined to the stage under name '" + n + "'")

    if (is.stroked)
        is.bindToName_s(n)
    if (is.filled)
        is.bindToName_f(n)
    if (is.pointed)
        is.bindToName_p(n)

    is.description = () => {
        return "from line " + n + "with " + n.nodes.length + " instructions"
    }

    if (window.viewport && window.viewport.render)
        window.viewport.render()

}));

var findByName = (name) => {
    var found = undefined
    traverseAll(SCENE, x => {
        if (x.name == name && !found) found = x
    })
    return found
}

findByName.__doc__ = "Finds an object in the scene by exactly matching the name. Returns the first object that matches."

var findBySearchingFor = (name) => {
    var found = undefined
    traverseAll(SCENE, x => {
        if (new RegExp(name, "i").test(x.name) && !found) found = x
    })
    return found
}

findBySearchingFor.__doc__ = "Finds an object in the scene by searching for a part of a name. Returns the first object that matches."

var findBySearchingPathFor = (name) => {
    buildPath()
    var found = undefined
    traverseAll(SCENE, x => {
        if (x.__path__ && new RegExp(name, "i").test(x.__path__) && !found) found = x
    })
    return found
}

findBySearchingPathFor.__doc__ = "Finds an object in the scene by searching for a part of the 'path name' of the object. Returns the first object that matches."

var clearAll = () => {
    editor.clear()
    window.meshes = {}
}

clearAll.__doc__ = "Clears everything in the scene"

__superglobal.clearAll = clearAll

var addTextureTo = (url, target) => {

    if (target.isFLine)
    {
        if (target.fill) addTextureTo(url, target.fill)
        if (target.stroke) addTextureTo(url, target.stroke)
        if (target.point) addTextureTo(url, target.point)
        return
    }

    if (target.mesh)
        return addTextureTo(url, target.mesh)

    if (!target.material)
        throw new Error(" don't know how to add a map to '"+target+"'")


    var a = new Image()
    var errorOnHand = bugError(" couldn't load " + url)
    a.onerror = (e) => {
        handleError("loading image called '"+url+"'", errorOnHand)
    }    
    a.src = url

    var tex = new THREE.Texture(a)
    target.material.map = tex
    target.material.needsUpdate = true
    tex.needsUpdate = true

    return tex
}

__superglobal.addTextureTo = addTextureTo
addTextureTo.__doc__ = "addTextureTo(url, target) tries to add an image texture map to `target` which can be an FLine or a mesh"


var traverseAll = (at, callback) => {
    callback(at)
    if (at.children) {
        var children = at.children;
        for (var i = 0, l = children.length; i < l; i++) {
            traverseAll(children[i], callback);
        }
    }
    if (at.material) {
        traverseAll(at.material, callback)
    }
}

var buildPath = () => {
    traverseAll(SCENE, x => {
        if (!x.name) return
        if (x == SCENE) {
            x.__path__ = "/"
        }
        else if (x.parent && x.parent.__path__) {
            x.__path__ = x.parent.__path__ + "/" + x.name
        }
        else {
            x.__path__ = "/" + x.name
        }
    })
}
var findByPath = (name) => {
    var found = undefined
    buildPath()
    traverseAll(SCENE, x => {
        if (x.__path__ == name && !found) found = x
    })
    return found
}

findByPath.__doc__ = "Finds an object in the scene by searching a specific 'path name' of the object. Returns the first object that matches."

var findAFrameByName = (name) => {
    var found = undefined
    traverseAll(AFRAME.scenes[0], x => {
        if (x.getAttribute && x.getAttribute("name")==name && !found)
            found = x
    })
    return found
}

findAFrameByName.__doc__ = "Finds an AFrame element with the name attribute 'name'"

var clearAFrame = (e) => {
    if (!e)
        e = AFRAME

    if (e==AFRAME)
        e = AFRAME.scenes[0]

    var c = e.children
    for(var i=c.length-1;i>=0;i--)
    {
        var x = c[i]
        if (x.getAttribute && x.getAttribute("aframe-injected")==null && x.tagName.toLowerCase()!='canvas')
            e.removeChild(x)
    }
}

clearAFrame.__doc__ = "removes all of the children of an AFRAME element"
__superglobal.clearAFrame = clearAFrame

var aCreate = (name, tag, parent) => {
    el = document.createElement(tag)
    el.setAttribute("name", name)
    
    if (!parent) parent = AFRAME.scenes[0]

    var found = undefined
    traverseAll(parent, x => {
        if (x.getAttribute && x.getAttribute("name")==name && !found)
            found = x
    })
    if (found)
        parent.removeChild(found)

    parent.appendChild(el)
    return el
}

aCreate.__doc__ = "removes all of the children of an AFRAME element"
__superglobal.aCreate = aCreate

Object.defineProperty(Element.prototype, "position", {get() { return this.getAttribute("position")}, set(x){ this.setAttribute("position", x) }})
Object.defineProperty(Element.prototype, "rotation", {get() { return this.getAttribute("rotation")}, set(x){ this.setAttribute("rotation", x) }})
Object.defineProperty(Element.prototype, "scale", {get() { return this.getAttribute("scale")}, set(x){ this.setAttribute("scale", x) }})
Object.defineProperty(Element.prototype, "color", {get() { return this.getAttribute("color")}, set(x){ this.setAttribute("color", x) }})

__superglobal.findByPath = findByPath
__superglobal.findBySearchingPathFor = findBySearchingPathFor
__superglobal.findBySearchingFor = findBySearchingFor
__superglobal.findByName = findByName



stage.add = (x) => {
    stage[x.uid] = x
}

stage.remove = (x) => {
    for (var v of stage.entries()) {
        if (v[1] == x) {
            delete stage[v[0]]
            return true
        }
    }
    return false
}

stage.toString = () => {
    return "stage with " + stage.entries().length + " elements on it"
}

__superglobal.vec = vec
__superglobal.stage = stage

var rotate = function (angle, pivotx, pivoty) {
    if (arguments.length == 0) return new THREE.Matrix4()
    if (arguments.length == 1) return new THREE.Matrix4().makeRotationZ(angle)
    if (arguments.length == 2) return new THREE.Matrix4().makeTranslation(pivotx, 0, 0).multiply(new THREE.Matrix4().makeRotationZ(angle).multiply(new THREE.Matrix4().makeTranslation(-pivotx, 0, 0)))
    if (arguments.length == 3) return new THREE.Matrix4().makeTranslation(pivotx, pivoty, 0).multiply(new THREE.Matrix4().makeRotationZ(angle).multiply(new THREE.Matrix4().makeTranslation(-pivotx, -pivoty, 0)))
    throw new Error("can't make a rotation given this (" + arguments.length + ") number of arguments")
}

rotate.__doc__ = "shorthand for creating a transformation that rotates. `rotate(10)` rotates things 10 degrees clockwise. Other numbers of arguments are also accepted: `rotate(10, vec(50, -40))` will rotate 10 degrees around the point `50, -40`"

var scale = function (x, y, z, w) {
    if (arguments.length == 0) return new THREE.Matrix4()
    if (arguments.length == 1) return new THREE.Matrix4().makeScale(x, x, x)
    if (arguments.length == 2) return new THREE.Matrix4().makeScale(x, y, 1)
    if (arguments.length == 3) return new THREE.Matrix4().makeScale(x, x, x)
    if (arguments.length == 4) return new THREE.Matrix4().makeTranslation(y, z, w).multiply(new THREE.Matrix4().makeScale(x, x, x).multiply(new THREE.Matrix4().makeTranslation(-y, -z, -w)))
    throw new Error("can't make a scale given this (" + arguments.length + ") number of arguments")
}

scale.__doc__ = "shorthand for creating a transformation that scale. `scale(10)` scales things (up) by a factor of 10 . Other numbers of arguments are also accepted: `scale(10, vec(50, -40))` will scale 10x degrees around the point `50, -40`. `scale(10,20)` creates a non-uniform scale that scales 10x in the X direction and 20x in the Y direction."

var translate = function (x, y, z) {
    if (arguments.length < 4) return new THREE.Matrix4().makeTranslation(x || 0, y || 0, z || 0)
    throw new Error("can't make a translation given this (" + arguments.length + ") number of arguments")
}

translate.__doc__ = "shorthand for creating a transformation that translates. "

var rotate3 = function (axis, angle) {
    if (arguments.length == 0) return new THREE.Matrix4()
    if (arguments.length == 2) {
        if (!axis.isVector3)
            throw "rotate3(axis, angle) expects a Vector3 for the axis"
        return new THREE.Matrix4().makeRotationAxis(axis, angle)
    }
    throw new Error("can't make a rotation3 given this (" + arguments.length + ") number of arguments")
}

rotate3.__doc__ = "shorthand for creating a transformation that rotates in 3d. rotate(axis, angle) where axis is a Vector3 and angle is in radians"

var translate3 = translate

__superglobal.rotate = rotate
__superglobal.scale = scale
__superglobal.translate = translate
__superglobal.rotate3 = rotate3

var c = (x) => {
    if (x == null || isNaN(x)) throw new Error("can't add " + x + " to geometry")
    return x
}

class FLine {
    constructor() {
        this.nodes = []
        this.uvs = []

        this.bSegments = null
        this.numM = 0
        this.stroked = true
        this.filled = false
        this.isFLine = true
        this.uid = THREE.Math.generateUUID()
        this.mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));
    }

    moveTo(x, y, z = 0) {
        this.numM++
        if (typeof (y) != 'undefined')
            this.nodes.push(['m', c(x), c(y), c(z)])
        else
            this.nodes.push(['m', c(x.x), c(x.y), c(x.z)])

        this.uvs.push([0, 0])
        return this
    }

    lineTo(x, y, z = 0) {
        if (this.nodes.length == 0) return this.moveTo(...arguments)

        if (typeof (y) != 'undefined')
            this.nodes.push(['l', c(x), c(y), c(z)])
        else
            this.nodes.push(['l', c(x.x), c(x.y), c(x.z) || 0])
        this.uvs.push([0, 0])
        return this
    }

    uv(u,v)
    {
        this.uvs[this.uvs.length-1][0] = u
        this.uvs[this.uvs.length-1][1] = v
        return this
    }

    cubicTo() {
        if (this.nodes.length == 0) return this.moveTo(...arguments)
        if (arguments.length == 3)
            this.nodes.push(['c',
                c(arguments[0].x), c(arguments[0].y), c(arguments[0].z) || 0,
                c(arguments[1].x), c(arguments[1].y), c(arguments[1].z) || 0,
                c(arguments[2].x), c(arguments[2].y), c(arguments[2].z) || 0])
        else if (arguments.length == 6)
            this.nodes.push(['c',
                c(arguments[0]), c(arguments[1]), 0,
                c(arguments[2]), c(arguments[3]), 0,
                c(arguments[4]), c(arguments[5]), 0])
        else if (arguments.length == 9)
            this.nodes.push(['c',
                c(arguments[0]), c(arguments[1]), c(arguments[2]),
                c(arguments[3]), c(arguments[4]), c(arguments[5]),
                c(arguments[6]), c(arguments[7]), c(arguments[8])])
        else throw Error(" can't cubicTo() with " + arguments.length + " arguments, only 3 vectors, 6 numbers (for 2d) or 9 numbers (for 3d)")

        this.uvs.push([0,0])
        return this
    }

    buildSegments() {
        if (this.bSegments = null || this.bSegments.length != this.nodes.length) {
            this.bSegments = []
            this.at = null
            for (var i = 0; i < this.nodes.length; i++) {
                if (this.nodes[i][0] == 'l') {
                    var X = this.nodes[i][1]
                    var Y = this.nodes[i][2]
                    var Z = this.nodes[i][3]
                    this.bSegments.push(new Bezier(
                        at[0],
                        at[1],
                        at[2],
                        at[0] + (X - at[0]) / 3,
                        at[1] + (Y - at[1]) / 3,
                        at[2] + (Z - at[2]) / 3,
                        at[0] + 2 * (X - at[0]) / 3,
                        at[1] + 2 * (Y - at[1]) / 3,
                        at[2] + 2 * (Z - at[2]) / 3,
                        X, Y, Z))
                    this.at = [this.nodes[i][1], this.nodes[i][2], this.nodes[i][3]]
                }
                else
                    if (this.nodes[i][0] == 'c') {
                        var X = this.nodes[i][1]
                        var Y = this.nodes[i][2]
                        var Z = this.nodes[i][3]
                        this.bSegments.push(new Bezier(
                            at[0],
                            at[1],
                            at[2],
                            this.nodes[i][1],
                            this.nodes[i][2],
                            this.nodes[i][3],
                            this.nodes[i][4],
                            this.nodes[i][5],
                            this.nodes[i][6],
                            this.nodes[i][7],
                            this.nodes[i][8],
                            this.nodes[i][9]))

                        this.at = [this.nodes[i][7], this.nodes[i][8], this.nodes[i][9]]
                    }
                    else
                        if (this.nodes[i][0] == 'm') {
                            var X = this.nodes[i][1]
                            var Y = this.nodes[i][2]
                            var Z = this.nodes[i][3]
                            this.at = [X, Y, Z]
                        }
            }
        }
        return this.bSegments
    }


    byTransforming(t) {
        const unwrap3 = (n) => {
            if (n && n.isVector3)
                return [n.x, n.y, n.z]
            if (n && n.isVector2)
                return [n.x, n.y, 0]
            if (n && n.isVector4)
                return [n.x, n.y, n.z]
            if (n.length == 1)
                return [n.x, n.y, n.z || 0]
            if (n.length < 3)
                return [n[0], n[1], 0]
            return n
        }

        var f = new FLine()
        f.nodes = this.nodes.map(x => {
            if (x[0] == 'm')
                return ['m', ...unwrap3(t(x[1], x[2], x[3]))]
            if (x[0] == 'l')
                return ['l', ...unwrap3(t(x[1], x[2], x[3]))]
            if (x[0] == 'c')
                return ['c', ...unwrap3(t(x[1], x[2], x[3])), ...unwrap3(t(x[4], x[5], x[6])), ...unwrap3(t(x[7], x[8], x[9]))]
        })

        f.uvs = this.uvs.map(x => x)
        f.mouseDown = this.mouseDown
        return f
    }

    __PLUS__(v) {
        if (canPromote3(v)) {
            var vv = promote3(v)
            return this.byTransforming((x, y, z) => [x + vv.x, y + vv.y, z + vv.z])
        }
        else if (v.isMatrix4) {
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(v))
        }
        else throw new Error(" can't add " + v + " and FLine")
    }

    __RPLUS__(v) {
        return __PLUS__(v)
    }

    __MINUS__(v) {
        if (canPromote3(v)) {
            var vv = promote3(v)
            return this.byTransforming((x, y, z) => [x - vv.x, y - vv.y, z - vv.z])
        }
        else if (v.isMatrix4) {
            var vv = new THREE.Matrix4().getInverse(v)
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(vv))
        }
        else throw new Error(" can't subract " + v + " and FLine")
    }

    __RMINUS__(v) {
        if (canPromote3(v)) {
            var vv = promote3(v)
            return this.byTransforming((x, y, z) => [-x + vv.x, -y + vv.y, -z + vv.z])
        }
        else if (v.isMatrix4) {
            var vv = new THREE.Matrix4().getInverse(v)
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(vv))
        }
        else throw new Error(" can't subtract " + v + " and FLine")
    }

    __MULTIPLY__(v) {
        if (typeof (v) == 'number')
            return this.byTransforming((x, y, z) => [x * v, y * v, z * v])

        if (canPromote3(v)) {
            var vv = promote3(v, 1)
            return this.byTransforming((x, y, z) => [x * vv.x, y * vv.y, z * vv.z])
        }
        else if (v.isMatrix4) {
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(v))
        }
        else throw new Error(" can't multiply " + v + " and FLine")
    }

    __RMULTIPLY__(v) {
        return __MULTIPLY__(v)
    }

    __DIVIDE__(v) {
        if (typeof (v) == 'number')
            return this.byTransforming((x, y, z) => [x / v, y / v, z / v])

        if (canPromote3(v)) {
            var vv = promote3(v, 1)
            return this.byTransforming((x, y, z) => [vv.x / x, vv.y / x, vv.z / x])
        }
        else if (v.isMatrix4) {
            var vv = new THREE.Matrix4().getInverse(v)
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(vv))
        }
        else throw new Error(" can't divide " + v + " and FLine")
    }

    __RDIVIDE__(v) {
        if (typeof (v) == 'number')
            return this.byTransforming((x, y, z) => [v / x, v / y, v / z])

        if (canPromote3(v)) {
            var vv = promote3(v, 1)
            return this.byTransforming((x, y, z) => [vv.x / x, vv.y / y, vv.z / z])
        }
        else if (v.isMatrix4) {
            var vv = new THREE.Matrix4().getInverse(v)
            return this.byTransforming((x, y, z) => new THREE.Vector3(x, y, z).applyMatrix4(vv))
        }
        else throw new Error(" can't divide " + v + " and FLine")

    }

    split() {
        if (this.numM == 1) return [this]

        var pieces = []
        var pieces_uv = []
        var piece = []
        var piece_uv = []
        
        for (var i = 0; i < this.nodes.length; i++) {
            if (this.nodes[i][0] == 'm') {
                piece = [this.nodes[i]]
                pieces.push(piece)

                piece_uv = [this.uvs[i]]
                pieces_uv.push(piece_uv)
            }
            else {
                piece.push(this.nodes[i])
                piece_uv.push(this.uvs[i])
            }
        }

        var ret = []
        for(var i=0;i<pieces.length;i++)
        {
            var f = new FLine()
            f.nodes = pieces[i]
            f.uvs = pieces_uv[i]
            f.numM = 1
            ret.push(f)
        }
        return ret
    }

    flatten() {
        var pieces = []
        var pieces_uv = []
        var splits = []
        for (var i = 0; i < this.nodes.length; i++) {
            var n = this.nodes[i]
            var uv = this.uvs[i]
            if (n[0] == 'm') {
                if (i != 0) splits.push(pieces.length / 3)
                pieces.push(n[1], n[2], n[3])
                pieces_uv.push(uv[0], uv[1])
            }
            if (n[0] == 'l')
            {
                pieces.push(n[1], n[2], n[3])
                pieces_uv.push(uv[0], uv[1])
            }
            if (n[0] == 'c') {
                var was = [pieces[pieces.length - 3], pieces[pieces.length - 2], pieces[pieces.length - 1]]
                var was_uv = [pieces_uv[pieces_uv.length - 2], pieces_uv[pieces_uv.length - 1]]
                this.cubicSegment(...was, n[1], n[2], n[3],
                    n[4], n[5], n[6],
                    n[7], n[8], n[9], pieces, was_uv[0], was_uv[1], uv[0], uv[1], pieces_uv)
            }
        }
        return { pieces: pieces, splits: splits, pieces_uv: pieces_uv}
    }

    cubicSegment(x, y, z, cx1, cy1, cz1, cx2, cy2, cz2, tx, ty, tz, into, lu, lv, u, v, into_uv) {
        var num = 30
        for (var i = 0; i < num; i++) {
            var a = (i + 1) / num
            var uu = lu+(u-lu)*a
            var vv = lv+(v-lv)*a

            var oma = 1 - a
            var oma2 = oma * oma
            var oma3 = oma * oma2
            var a2 = a * a
            var a3 = a * a2

            var ax = x * oma3 + cx1 * oma2 * a + cx2 * oma * a2 + tx * a3
            var ay = y * oma3 + cy1 * oma2 * a + cy2 * oma * a2 + ty * a3
            var az = z * oma3 + cz1 * oma2 * a + cz2 * oma * a2 + tz * a3
            into.push(ax, ay, az)
            into_uv.push(uu,vv)
        }
    }


    _interpretColor(c) {
        if (c.isColor) {
            return vec(c.r, c.g, c.b, 1)
        }
        if (typeof (c.x) != 'undefined') {
            if (typeof (c.w) != 'undefined') {
                return c
            }
            return vec(c.x, c.y, c.z, c.w)
        }
        else {
            if (c.length == 4)
                return vec(c[0], c[1], c[2], c[3])
            else if (c.length == 3)
                return vec(c[0], c[1], c[2], 1)
        }
        throw Error(" can't interpret " + c + " as a color")
    }

    bindToName_s(name) {
        var index = 0

        var fp = this.flatten() // we certainly dont want to do this twice if we are also doing points and triangles

        var flat = fp.pieces
        var splits = fp.splits

        var ab = new ArrayBuffer(4 * flat.length)
        var fa = new Float32Array(ab)
        fa.set(flat, 0) // or this

        // then its going to be something like
        updateBuffer(ab, null, name + "_s", 'V', flat.length / 3, 2, 0)

        var color = this._interpretColor(this.color || this.strokeColor || vec(1, 0, 0, 1))

        var ab = new ArrayBuffer(4 * (flat.length / 3) * 4)
        var write = new Float32Array(ab)
        for (var i = 0; i < flat.length / 3; i++) {
            write[4 * i + 0] = color.x;
            write[4 * i + 1] = color.y;
            write[4 * i + 2] = color.z;
            write[4 * i + 3] = color.w;
        }
        updateBuffer(ab, null, name + "_s", 'C', flat.length / 3, 2, 0)

        var ab = new ArrayBuffer(4 * ((flat.length / 3) * 2 - (splits.length)))
        var write = new Int32Array(ab)
        var splitCursor = 0
        var index = 0
        for (var i = 0; i < flat.length / 3 - 1; i++) {
            if (splitCursor >= splits.length || i != splits[splitCursor] - 1) {
                write[2 * index + 0] = i
                write[2 * index + 1] = i + 1
                index++
            }
            else if (i == splits[splitCursor] - 1) {
                splitCursor++
            }
        }
        this.stroke = updateBuffer(ab, null, name + "_s", 'E', flat.length / 3, 2, 0)

        this.stroke._fieldSource = this

        if (typeof (this.strokeOpacity) != 'undefined') {
            this.stroke.material.opacity = this.opacity
        }
        else if (typeof (this.opacity) != 'undefined') {
            this.stroke.material.opacity = this.opacity
        }
        else {
            this.stroke.material.opacity = color.w
        }
    }

    bindToName_p(name) {
        var index = 0

        var fp = this.flatten() // we certainly dont want to do this twice if we are also doing points and triangles

        var flat = fp.pieces
        var splits = fp.splits

        var ab = new ArrayBuffer(4 * flat.length)
        var fa = new Float32Array(ab)
        fa.set(flat, 0) // or this

        // then its going to be something like
        updateBuffer(ab, null, name + "_p", 'V', flat.length / 3, 0, 0)

        var color = this._interpretColor(this.color || this.strokeColor || vec(1, 0, 0, 1))

        var ab = new ArrayBuffer(4 * (flat.length / 3) * 4)
        var write = new Float32Array(ab)
        for (var i = 0; i < flat.length / 3; i++) {
            write[4 * i + 0] = color.x;
            write[4 * i + 1] = color.y;
            write[4 * i + 2] = color.z;
            write[4 * i + 3] = color.w;
        }
        this.points = updateBuffer(ab, null, name + "_p", 'C', flat.length / 3, 0, 0)

        if (typeof (this.fillOpacity) != 'undefined') {
            this.points.material.opacity = this.opacity
        }
        else if (typeof (this.opacity) != 'undefined') {
            this.points.material.opacity = this.opacity
        }
        else {
            this.points.material.opacity = color.w
        }
    }

    bindToName_f(name) {
        var everything = this.split().map(x => x.flatten()).filter(x => x.pieces.length > 8).map(x => {
            return {x:x, result:Tess2.tesselate({ contours: [x.pieces], windingRule: Tess2.WINDING_ODD, elementType: Tess2.POLYGONS, polySize: 3, vertexSize: 3 })}
        })

        var vertexCount = 0
        var elementCount = 0
        for (var e of everything) {
            vertexCount += e.result.vertices.length / 3
            elementCount += e.result.elements.length / 3
        }

        var ab = new ArrayBuffer(4 * vertexCount * 3)
        var fa = new Float32Array(ab)
        var index = 0
        var vertexOffsets = []
        for (var e of everything) {
            vertexOffsets.push(index / 3)
            fa.set(e.result.vertices, index)
            index += e.result.vertices.length
        }

        updateBuffer(ab, null, name + "_f", 'V', vertexCount, 3, 0)

        var ab = new ArrayBuffer(4 * vertexCount * 2)
        var ft = new Float32Array(ab)
        var c = 0
        for (var e=0;e<everything.length;e++) {
            for(var q=0;q<everything[e].result.vertexIndices.length;q++)
            {
                var index = everything[e].result.vertexIndices[q]
                ft[c++] = everything[e].x.pieces_uv[index*2+0]
                ft[c++] = everything[e].x.pieces_uv[index*2+1]
            }
        }            

        updateBuffer(ab, null, name + "_f", 'T', vertexCount, 3, 0)
        
        var color = this._interpretColor(this.color || this.fillColor || vec(1, 0.5, 0.5, 0.5))
        var ab = new ArrayBuffer(4 * vertexCount * 4)
        var write = new Float32Array(ab)
        for (var i = 0; i < vertexCount; i++) {
            write[4 * i + 0] = color.x;
            write[4 * i + 1] = color.y;
            write[4 * i + 2] = color.z;
            write[4 * i + 3] = color.w;
        }
        updateBuffer(ab, null, name + "_f", 'C', vertexCount, 3, 0)

        var ab = new ArrayBuffer(4 * elementCount * 3)
        var write = new Int32Array(ab)
        var splitCursor = 0
        var index = 0
        var eo = 0
        for (var e of everything) {
            var vo = vertexOffsets[eo]
            for (var q of e.result.elements) {
                write[(index++)] = q + vo
            }
            eo++
        }

        this.fill = updateBuffer(ab, null, name + "_f", 'E', vertexCount, 3, 0)

        this.fill._fieldSource = this

        if (typeof (this.fillOpacity) != 'undefined') {
            this.fill.material.opacity = this.opacity
        }
        else if (typeof (this.opacity) != 'undefined') {
            this.fill.material.opacity = this.opacity
        }
        else {
            this.fill.material.opacity = color.w
        }

    }

}

__superglobal.FLine = FLine
__superglobal.Tess2 = Tess2


var intersectLine = (origin, direction) => {
	var meshes = []
	SCENE.traverse( x=> {
		if (x.isMesh)
			meshes.push(x)
	})
    var ray = new THREE.Raycaster(origin, direction, 0.0001, 1000)
    ray.linePrecision = 0.001
    return ray.intersectObjects( meshes, false);
}