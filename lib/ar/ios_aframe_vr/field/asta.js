
promote2 = (x) => {
    if (x.length == 3) return new THREE.Vector2(x[0], x[1])
    if (x.length == 2) return new THREE.Vector2(x[0], x[1])

    if (x.isVector2) return x
    if (x.isVector3) return x
    if (x.isVector4) return x
    throw Error("can't convert " + x + " to a Vector2")
}

promote3 = (x, z = 0) => {
    if (x.length == 3) return new THREE.Vector3(x[0], x[1], x[2])
    if (x.length == 2) return new THREE.Vector3(x[0], x[1], 0)
    if (x.isVector2) return new THREE.Vector3(x.x, x.y, z)
    if (x.isVector3) return x
    if (x.isVector4) return x
    throw Error("can't convert " + x + " to a Vector3")
}

canPromote3 = (x, z = 0) => {
    return (x.isVector2 || x.isVector3 || x.isVector4 || x.length == 3 || x.length == 2)
}


promote4 = (x, z = 0, w = 1) => {
    if (x.isVector2) return new THREE.Vector3(x.x, x.y, z, w)
    if (x.isVector3) return new THREE.Vector3(x.x, x.y, x.z, w)
    if (x.isVector4) return x
    throw Error("can't convert " + x + " to a Vector4")
}

__PLUS__ = (x, y) => {
    var nx = (typeof x == 'number')
    var ny = (typeof y == 'number')

    if (nx && ny)
        return x + y;

    var ux = (typeof x == 'undefined') || x == null  
    var uy = (typeof y == 'undefined') || y == null 

    if (ux)
        throw Error("can't add 'undefined' to " + y)
    if (uy)
        throw Error("can't add " + x + " to 'undefined'")

    if (ny && !nx) {
        if (x.isVector2)
            return new THREE.Vector2(x.x + y, x.y + y);
        if (x.isVector3)
            return new THREE.Vector3(x.x + y, x.y + y, x.z + y);
        if (x.isVector4)
            return new THREE.Vector4(x.x + y, x.y + y, x.z + y, x.w + y);

        if (x.__PLUS__)
            return x.__PLUS__(ny)

            if (typeof x == 'string')
            return x + y

        throw Error("can't add " + x + " to " + y)
    }
    else if (nx && !ny) {
        if (y.isVector2)
            return new THREE.Vector2(x + y.x, x + y.y);
        if (y.isVector3)
            return new THREE.Vector3(x + y.x, x + y.y, x + y.z);
        if (y.isVector4)
            return new THREE.Vector4(x + y.x, x + y.y, x + y.z, x + y.w);

        if (y.__RPLUS__)
            return y.__RPLUS__(x)

        if (typeof y == 'string')
            return x + y


        throw Error("can't add " + x + " to " + y)
    }
    else {
        if (y.isVector2 && canPromote3(x)) {
            x = promote2(x)
            return new THREE.Vector2(x.x + y.x, x.y + y.y);
        }
        if (y.isVector3 && canPromote3(x)) {
            x = promote3(x)
            return new THREE.Vector3(x.x + y.x, x.y + y.y, x.z + y.z);
        }
        if (y.isVector4 && canPromote3(x)) {
            x = promote4(x)
            return new THREE.Vector4(x.x + y.x, x.y + y.y, x.z + y.z, x.w + y.w);
        }

        if (x.isMatrix4 && y.isMatrix4)
            return new THREE.Matrix4().multiply(x).multiply(y) // order?


        if (x.__PLUS__)
            return x.__PLUS__(y)
        if (y.__RPLUS__)
            return y.__RPLUS__(x)

        if (typeof x == 'string')
            return x + y

        if (typeof y == 'string')
            return x + y


        throw Error("can't add " + x + " to " + y)

    }
}

__MULTIPLY__ = (x, y) => {
    var nx = (typeof x == 'number')
    var ny = (typeof y == 'number')

    if (nx && ny)
        return x * y;

    var ux = (typeof x == 'undefined') || x == null 
    var uy = (typeof y == 'undefined') || y == null 

    if (ux)
        throw Error("can't multiply 'undefined' to " + y)
    if (uy)
        throw Error("can't multiply " + x + " to 'undefined'")

    if (ny && !nx) {
        if (x.isVector2)
            return new THREE.Vector2(x.x * y, x.y * y);
        if (x.isVector3)
            return new THREE.Vector3(x.x * y, x.y * y, x.z * y);
        if (x.isVector4)
            return new THREE.Vector4(x.x * y, x.y * y, x.z * y, x.w * y);

        if (x.__MULTIPLY__)
            return x.__MULTIPLY__(y)

        throw Error("can't multiply " + x + " and " + y)
    }
    else if (nx && !ny) {
        if (y.isVector2)
            return new THREE.Vector2(x * y.x, x * y.y);
        if (y.isVector3)
            return new THREE.Vector3(x * y.x, x * y.y, x * y.z);
        if (y.isVector4)
            return new THREE.Vector4(x * y.x, x * y.y, x * y.z, x * y.w);

        if (y.__RMULTIPLY__)
            return y.__RMULTIPLY__(x)

        throw Error("can't multiply " + x + " and " + y)
    }
    else {
        if (y.isVector2 && canPromote3(x)) {
            x = promote2(x)
            return new THREE.Vector2(x.x * y.x, x.y * y.y);
        }
        if (y.isVector3 && canPromote3(x)) {
            x = promote3(x)
            return new THREE.Vector3(x.x * y.x, x.y * y.y, x.z * y.z);
        }
        if (y.isVector4 && canPromote3(x)) {
            x = promote4(x)
            return new THREE.Vector4(x.x * y.x, x.y * y.y, x.z * y.z, x.w * y.w);
        }

        if (x.__MULTIPLY__)
            return x.__MULTIPLY__(y)
        if (y.__RMULTIPLY__)
            return y.__RMULTIPLY__(x)

        throw Error("can't multiply " + x + " and " + y)

    }
}

__DIVIDE__ = (x, y) => {
    var nx = (typeof x == 'number')
    var ny = (typeof y == 'number')

    if (nx && ny)
        return x / y;

    var ux = (typeof x == 'undefined') || x == null 
    var uy = (typeof y == 'undefined') || y == null 

    if (ux)
        throw Error("can't divide 'undefined' and " + y)
    if (uy)
        throw Error("can't divide " + x + " and 'undefined'")

    if (ny && !nx) {
        if (x.isVector2)
            return new THREE.Vector2(x.x / y, x.y / y);
        if (x.isVector3)
            return new THREE.Vector3(x.x / y, x.y / y, x.z / y);
        if (x.isVector4)
            return new THREE.Vector4(x.x / y, x.y / y, x.z / y, x.w / y);
        if (x.__DIVIDE__)
            return x.__DIVIDE__(y)

        throw Error("can't divide " + x + " and " + y)
    }
    else if (nx && !ny) {
        if (y.isVector2)
            return new THREE.Vector2(x / y.x, x / y.y);
        if (y.isVector3)
            return new THREE.Vector3(x / y.x, x / y.y, x / y.z);
        if (y.isVector4)
            return new THREE.Vector4(x / y.x, x / y.y, x / y.z, x / y.w);
        if (y.__RDIVIDE__)
            return y.__RDIVIDE__(x)

        throw Error("can't divide " + x + " and " + y)
    }
    else {
        if (y.isVector2 && canPromote3(x)) {
            x = promote2(x)
            return new THREE.Vector2(x.x / y.x, x.y / y.y);
        }
        if (y.isVector3 && canPromote3(x)) {
            x = promote3(x)
            return new THREE.Vector3(x.x / y.x, x.y / y.y, x.z / y.z);
        }
        if (y.isVector4 && canPromote3(x)) {
            x = promote4(x)
            return new THREE.Vector4(x.x / y.x, x.y / y.y, x.z / y.z, x.w / y.w);
        }

        if (x.__DIVIDE__)
            return x.__DIVIDE__(y)
        if (y.__RDIVIDE__)
            return y.__RDIVIDE__(x)

        throw Error("can't divide " + x + " and " + y)

    }
}


__MINUS__ = (x, y) => {
    var nx = (typeof x == 'number')
    var ny = (typeof y == 'number')

    if (nx && ny)
        return x - y;

    var ux = (typeof x == 'undefined') || x == null 
    var uy = (typeof y == 'undefined') || y == null 

    if (ux)
        throw Error("can't subtract " + y + " from 'undefined'")
    if (uy)
        throw Error("can't subtract 'undefined' from " + x)

    if (ny && !nx) {
        if (x.isVector2)
            return new THREE.Vector2(x.x - y, x.y - y);
        if (x.isVector3)
            return new THREE.Vector3(x.x - y, x.y - y, x.z - y);
        if (x.isVector4)
            return new THREE.Vector4(x.x - y, x.y - y, x.z - y, x.w - y);

        if (x.__MINUS__)
            return x.__MINUS__(y)

        throw Error("can't subtract " + x + " from " + y)
    }
    else if (nx && !ny) {
        if (y.isVector2)
            return new THREE.Vector2(x - y.x, x - y.y);
        if (y.isVector3)
            return new THREE.Vector3(x - y.x, x - y.y, x - y.z);
        if (y.isVector4)
            return new THREE.Vector4(x - y.x, x - y.y, x - y.z, x - y.w);

        if (y.__RMINUS__)
            return y.__RMINUS__(x)

        throw Error("can't subtract " + x + " from " + y)
    }
    else {
        if (y.isVector2 && canPromote3(x)) {
            x = promote2(x)
            return new THREE.Vector2(x.x - y.x, x.y - y.y);
        }
        if (y.isVector3 && canPromote3(x)) {
            x = promote3(x)
            return new THREE.Vector3(x.x - y.x, x.y - y.y, x.z - y.z);
        }
        if (y.isVector4 && canPromote3(x)) {
            x = promote4(x)
            return new THREE.Vector4(x.x - y.x, x.y - y.y, x.z - y.z, x.w - y.w);
        }

        if (x.__MINUS__)
            return x.__MINUS__(y)
        if (y.__RMINUS__)
            return y.__RMINUS__(x)

        throw Error("can't subtract " + x + " from " + y)

    }
}



__superglobal.__PLUS__ = __PLUS__
__superglobal.__MULTIPLY__ = __MULTIPLY__
__superglobal.__MINUS__ = __MINUS__
__superglobal.__DIVIDE__ = __DIVIDE__