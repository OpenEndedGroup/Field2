console.log(" **Field is booting up** ");

var _field = {};
var frame = 0
var lastUpdatedAt = -1
var thatImageBuffer
var SCENE
var CAMERA
var _r
var globlSourceCache
var FIELD
var globalCodeName

__beginService()

_field.log = function (s) {
    _field.send("log", s);
};

_field.error = function (s) {
    _field.send("error", encodeURIComponent(s));
};

_field.evalRequestWS = function (id, onupdate) {

    _field.id = id;

    _field.pathname = new URL(document.URL).hostname
    var alternative = getQueryVariable("field")
    if (alternative)
        _field.pathname = alternative;

    FIELD = _field.pathname
    _field.socket = new WebSocket("wss://" + _field.pathname);
    // _field.socket = new WebSocket("ws://" + _field.pathname + ":8090");

    _field.socket.binaryType = "arraybuffer";

    _field.send = function (address, obj) {
        var t = JSON.stringify({
            address: address,
            payload: obj,
            from: _field.id
        })
        // console.log("SEND ", obj)
        _field.socket.send(t)
    };

    _field.socket.onopen = function (e) {
        _field.isOpen = true
        _field.send("initialize", id)
    };

    print = (x) => {
        if (__sandbox__ && __sandbox__.__returnTo)
        {
            _field.send(__sandbox__.__returnTo, { message: safe(betterPretty(x, 2, "JSON", false)), kind: "SUCCESS" })
        }
        else
        {
            _field.send(_field.globalReturnTo, { message: safe(betterPretty(x, 2, "JSON", false)), kind: "SUCCESS" })
        }
    }

    console.log(" -- setting up logging support ")
    __superglobal.print = print
    __superglobal.log = print

    window.print = print
    window.log = print
    
    _field.socket.onmessage = function (e) {

        // this needs to be per geometry not globally and not per channel
        // if (lastUpdatedAt == frame)
        //     return

        // lastUpdatedAt = frame

        if (ArrayBuffer.prototype.isPrototypeOf(e.data)) {
            _field.arraybuffer = e.data
            // console.log(e.data)
            var d = new DataView(e.data)
            // console.log("-> d= ")
            // console.log(d)
            if (d.byteLength < 64) return
            var dim = d.getUint32(0, true)
            var typeCode = String.fromCharCode(d.getUint16(4, true))
            var nameLen = d.getUint8(6, true)
            var elementDim = d.getUint32(7, true)

            // console.log(d)
            // console.log(dim)
            // console.log(elementDim)
            // console.log(typeCode)
            // console.log(nameLen)

            var name = ""
            for (var q = 0; q < nameLen; q++) {
                name += String.fromCharCode(d.getUint16(11 + q * 2, true))
            }

            // console.log(" channel is :")
            // console.log(name)

            updateBuffer(e.data, d, name, typeCode, dim, elementDim, 128)
        } else {
            let edata = JSON.parse(e.data)
            _field.globalReturnTo = edata.returnTo
            // console.log(" setting globla return to to ", edata.returnTo)
            _field.__var0 = edata.__var0
            _r = undefined
            __superglobal._r = undefined // this means that writes to _r go into window.
            __superglobal._field = _field

            var code = edata.code + "\n//# sourceURL=" + edata.codeName + "\n";
            let sourceCache = {};
            sourceCache[edata.codeName] = edata.codeName
            globlSourceCache = sourceCache
            context = edata.codeName.split("|")[1]
            //console.log(context)


            globalCodeName = edata.codeName
            
            // console.log("CODE", code)
            // try {
            var ret
            if (edata.noSandbox)
                ret = window.eval(code);
            else
            {
                if (edata.code.trim().split("\n").length<2)
                {
                    console.log(" single line run")
                    ret = runInSandbox(context, code);
                }
                else {
                    console.log(" multi-line generator run", edata.code.trim().split("\n").length, edata.code.trim())
                    
                    ret = runInSandbox_generator(context, code);
                    if (ret)
                    {
                        var gen = ret()
                        var v = gen.next()
                        ret = v.value
                        if (!v.done)
                        {
                            console.log(" generator is go ")
                            
                            var task = new Task(edata.codeName, function () { return "SKIP" }, gen.next.bind(gen), function () { }, function () {
                                _field.send(edata.returnTo, { message: "OK", kind: "RUN-STOP", altName:"" })
                            }, function (e) {
                                StackTrace.fromError(e, { sourceCache: sourceCache }).then(stack => {
        
                                    // altFileName = filename
                                    altLineNumber = stack[0].lineNumber || -1
                        
                                    for (var s of stack) {
                                        if (s.fileName && s.fileName.startsWith("box")) {
                                            console.log(" possible box filename output :", s.fileName, s.lineNumber)
                                            // altFileName = s.fileName
                                            altLineNumber = s.lineNumber
                                            break;
                                        }
                                    }
                        
                                    _field.send(edata.returnTo, { message: e.message, line: (altLineNumber || -1), kind: "ERROR" })                
                                    console.log(stack)
                                })
                            })
                            if (typeof (edata.timeStart) != 'undefined')
                                task.configureTimeRemapping(edata.timeStart, edata.timeEnd)

                            _field.send(edata.returnTo, { message: "OK", kind: "RUN-START", altName:"" })
                        }
                    }
                
                }
            }

            if (ret)
                // _field.send(edata.returnTo, { message: safe(pretty(ret, 2, "PRINT", true) ), kind: "SUCCESS" })
                _field.send(edata.returnTo, { message: /*safe*/"<json>" + (betterPretty(ret, 2, "JSON", false)), kind: "SUCCESS" })
            else
                _field.send(edata.returnTo, { message: "OK", kind: "SUCCESS" })



            if (typeof (_r) != 'undefined' && edata.launchable) {
                if (_r.length == 3) {
                    var task = new Task(edata.codeName, _r[0], _r[1], _r[2], function () {
                        _field.send(edata.returnTo, { message: "OK", kind: "RUN-STOP", altName:"" })
                    }, function (e) {
                        StackTrace.fromError(e, { sourceCache: sourceCache }).then(stack => {
                            // altFileName = filename
                            altLineNumber = stack[0].lineNumber || -1
                
                            for (var s of stack) {
                                if (s.fileName && s.fileName.startsWith("box")) {
                                    console.log(" possible box filename output :", s.fileName, s.lineNumber)
                                    // altFileName = s.fileName
                                    altLineNumber = s.lineNumber
                                    break;
                                }
                            }
                
                            _field.send(edata.returnTo, { message: e.message, line: (altLineNumber || -1), kind: "ERROR" })                
                            console.log(stack)
                        })
                    }
                    )
                    if (typeof (edata.timeStart) != 'undefined')
                        task.configureTimeRemapping(edata.timeStart, edata.timeEnd)
                }
                else {
                    var task = launch(_r, "", false)
                    if (typeof (edata.timeStart) != 'undefined')
                        task.configureTimeRemapping(edata.timeStart, edata.timeEnd)

                }

                _field.send(edata.returnTo, { message: "OK", kind: "RUN-START", altName:"" })

            }
            if (onupdate) onupdate()
            // }
            // catch (e) {
            // 	if (e.name && e.name=="SyntaxError") throw e // syntax errors have no line number for eval

            // 	StackTrace.fromError(e, { sourceCache: sourceCache }).then(stack => {
            // 		_field.send(edata.returnTo, { message: e.message, line: stack[0].lineNumber, kind: "ERROR" })
            // 		console.log(stack)
            // 	})
            // }
        }

        // _field.send("alive", id)
    };

    _field.socket.onclose = function (e) {
        // _messageBus.publish("status", "<span class='error'>Disconnected from Field</span>");
        document.title = "Field / DISCONNECTED (reload the page?)";
        _field.isOpen = false
        
        // start it up again ?
        setTimeout(() => {
            console.log(" trying to reconnect on close")
            if (!_field.isOpen)
             _field.evalRequestWS(_field.id)
        }, 1000)
    };

    _field.socket.onerror = function (e) {
        // _messageBus.publish("status", "<span class='error'>Disconnected from Field</span>");
        document.title = "Field / DISCONNECTED (reload the page?)";
        _field.isOpen = false
        

        // // start it up again
        // setTimeout(() => {
        //     console.log(" trying to reconnect on error")
        //     if (!_field.isOpen)
        //         _field.evalRequestWS(_field.id)
        // }, 1000)
    }
};

var betterPretty = (a, b, c, d) => {

    try {
        if (a.description) return a.description()
    }
    catch (e) {
        console.error(" error in tostring for ", a)
        console.error(e)
        console.error(" falling through ")
    }

    return pretty(a, b, c, d).replace(/circular reference to \[object Object\]/g,"'[circular reference]'")
}

var disc = new THREE.TextureLoader().load( '/field/circle.png' );


var newGeometry = function (numVertex, numElements, elementDim, oldMaterial, name) {

    console.log(" new geometry " + numVertex + " " + numElements + " " + elementDim)

    var geometry = new THREE.BufferGeometry();
    if (elementDim > 0)
        geometry.setIndex(new THREE.BufferAttribute(new Uint32Array(numElements * elementDim), elementDim));
    geometry.setAttribute('position', new THREE.BufferAttribute(new Float32Array(numVertex * 3), 3));
    geometry.setAttribute('color', new THREE.BufferAttribute(new Float32Array(numVertex * 4), 4));
    geometry.setAttribute('uv', new THREE.BufferAttribute(new Float32Array(numVertex * 2), 2));
    geometry.setDrawRange(0, 0)

    // geometry.attributes["position"].dynamic = true
    // geometry.attributes["color"].dynamic = true
    // geometry.attributes["uv"].dynamic = true

    var material = (oldMaterial) ? oldMaterial : (elementDim == 0 ? new THREE.PointsMaterial({
        side: THREE.DoubleSide,
        blending: THREE.NormalBlending,
        vertexColors: THREE.VertexColors,
        map: disc,
        transparent: true,
        depthTest: false,
        size: 0.005
    }) : new THREE.MeshBasicMaterial({
        side: THREE.DoubleSide,
        blending: THREE.NormalBlending,
        vertexColors: THREE.VertexColors,
        transparent: true,
        // transparent: true,
        depthTest: false
    }));

    material.name = name + "_material"

    if (elementDim == 3)
        mesh = new THREE.Mesh(geometry, material);
    else if (elementDim == 2)
        mesh = new THREE.LineSegments(geometry, material);
    else if (elementDim == 0)
        mesh = new THREE.Points(geometry, material);

    // mesh.frustumCulled = false

    mesh.name = name
    mesh.frustumCulled = false

    geometry.computeBoundingSphere()
    geometry.name = name
    geometry.frustumCulled = false

    SCENE.attach[name] = mesh

    return mesh
}

var meshes = {}

var default_maxVertex = 5
var default_maxElement = 1000

var geometryForName = function (name, dim, elementDim) {

    var oldMaterial = 0

    if (meshes[name]) {
        var r = meshes[name].geometry
        if (r.attributes["position"].count < dim) {

            oldMaterial = meshes[name].material
            SCENE.remove(meshes[name])

            // fallthrough
            console.log(" geometry is too small for name " + name + " resizing " + r.attributes["position"].count + " < " + dim)
        }
        else {
            if (meshes[name].parent==null) // become disconnected somehow
            {
                SCENE.attach[name] = meshes[name]
            }
            return r
        }
    }

    console.log(" new geometry for name " + name)

    var d = 0
    if (name.endsWith("_f"))
        d = 3
    else if (name.endsWith("_s"))
        d = 2
    else if (name.endsWith("_p"))
        d = 0

    meshes[name] = newGeometry(Math.max(dim * 2, default_maxVertex), default_maxElement, d, oldMaterial, name)

    return meshes[name].geometry
}

var updateBuffer = function (rawdata, data, name, typeCode, dim, elementDim, dataOffset) {

    //console.log(" TYPE "+typeCode+" / "+name)

    if (typeCode === 'x') {
        console.log(" got texture upgrade request")
        // upgrade to texture if needs be

        var a = new Image()
        a.src = URL.createObjectURL(new Blob([rawdata.slice(dataOffset)]))
        var tex = new THREE.Texture(a)
        thatImageBuffer = tex
        URL.revokeObjectURL(a.src)

        console.log(" upgrading material ")
        var material = new THREE.MeshBasicMaterial({
            map: tex,
            side: THREE.DoubleSide,
            blending: THREE.AdditiveBlending,
            vertexColors: THREE.VertexColors,
            transparent: true,
            depthTest: false
        })

        if (!meshes[name])
            geometryForName(name, 10, 10)

        meshes[name].material = material

        tex.needsUpdate = true

        return meshes[name]

    }

    geometry = geometryForName(name, dim, elementDim)

    if (typeCode === 'V') {
        // console.log("vertex")
        geometry.attributes["position"].array.set(new Float32Array(rawdata, dataOffset))
        geometry.attributes["position"].needsUpdate = true;
        geometry.attributes["position"].updateRange = { offset: 0, count: dim * 3 }

        if (name.endsWith("_p"))
            geometry.setDrawRange(0, dim)

    } else if (typeCode === 'T') {
        geometry.attributes["uv"].array.set(new Float32Array(rawdata, dataOffset))
        geometry.attributes["uv"].needsUpdate = true;
        geometry.attributes["uv"].updateRange = { offset: 0, count: dim * 2 }
    }
    else if (typeCode === 'C') {
        geometry.attributes["color"].array.set(new Float32Array(rawdata, dataOffset))
        geometry.attributes["color"].needsUpdate = true;
        geometry.attributes["color"].updateRange = { offset: 0, count: dim * 4 }
    }
    else if (typeCode === 'E' && (elementDim > 0)) {
        var te = new Uint32Array(rawdata, dataOffset)
        // console.log(" checking element size " + te.length + " " + (geometry.index.count) + " " + (geometry.index.itemSize) + " " + geometry.index.array.length)
        if (geometry.index.array.length < te.length) {
            // console.log(" had to rebuild elements " + geometry.index.count + " * " + geometry.index.itemSize + " < " + te.length)
            geometry.setIndex(new THREE.BufferAttribute(new Uint32Array(te.length * 2), elementDim));
        }
        geometry.index.array.set(te)
        geometry.index.count = te.length
        geometry.index.needsUpdate = true;
        geometry.index.updateRange = { offset: 0, count: te.length }
        geometry.setDrawRange(0, te.length)
    }

    geometry.computeBoundingSphere()

    return meshes[name]
}

var SCENE, CAMERA

var enterScene = (_scene, _camera, updateCallback, preupdateCallback) => {

    SCENE = _scene;
    CAMERA = _camera;

    SCENE.description = () => {
        return "SCENE"
    }
    CAMERA.description = () => {
        return "CAMERA"
    }

    SCENE.name = "Root Scene"

    __superglobal.SCENE = SCENE
    __superglobal.CAMERA = CAMERA
    __superglobal.THREE = THREE
    // __superglobal.editor = editor

    SCENE.attach = new Proxy(new Map(), idempotencyHandler(function (n, is, was) {
        if (was) {
            if (preupdateCallback)
                preupdateCallback()

            SCENE.remove(was)
            if (updateCallback)
                updateCallback()
        }
        if (is) {
            if (!was)
                if (preupdateCallback)
                    preupdateCallback()

            if (!is.name) is.name = n
            SCENE.add(is)
            if (updateCallback)
                updateCallback()

            is.description = () => "attached '" + n + "'"
        }
    }));
}

window.onerror = (message, filename, lineno, colno, e) => {
    console.log(" global error handler called ")
    console.log(message, filename, lineno, colno, e)

    console.log(e);

    // if (e && e.__proto__.name === "SyntaxError") {
    // 	_field.send(_field.globalReturnTo, { message: message, line: lineno, kind: "ERROR" })
    // }
    // else
    if (e)
        StackTrace.fromError(e, { sourceCache: globlSourceCache }).then(stack => {

            altFileName = filename
            altLineNumber = stack[0].lineNumber || -1

            for (var s of stack) {
                if (s.fileName && s.fileName.startsWith("box")) {
                    console.log(" possible box filename output :", s.fileName, s.lineNumber)
                    altFileName = s.fileName
                    altLineNumber = s.lineNumber
                    break;
                }
            }

            _field.send(_field.globalReturnTo, { message: e.message, line: (altLineNumber || -1), kind: "ERROR" })

            console.log(stack)
        })
    else {
        console.log(" no actual error object")
        _field.send(_field.globalReturnTo, { message: message, line: lineno, kind: "ERROR" })
    }
    if (e) {
        console.log(" about to send field error " + e.message + " on line :" + e.stack)
        // _field.error(e.message + " on line:" + e.stack);
    }
};


function getQueryVariable(variable) {
    var query = window.location.search.substring(1);
    var vars = query.split('&');
    for (var i = 0; i < vars.length; i++) {
        var pair = vars[i].split('=');
        if (decodeURIComponent(pair[0]) == variable) {
            return decodeURIComponent(pair[1]);
        }
    }
    console.log('Query variable %s not found', variable);
}


var onscreenshot = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));
__superglobal.onscreenshot = onscreenshot

// called from iOS browser when screenshot is taken (which seems literally the only way to access the camera while doing AR on iOS)
_field.screenshot = (dataurl) => {
    dispatchTo(dataurl, onscreenshot, "onscreenshot");
}

var mouseUp = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));
var mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));
var mouseMove = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));
var dblClick =  new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));

__superglobal.mouseUp = mouseUp
__superglobal.mouseDown = mouseDown
__superglobal.dblClick = dblClick
__superglobal.mouseMove = mouseMove


__superglobal.launch = launch
__superglobal.over = over


var labelEvent = (e, doIntersection=false) => {
    var ray = new THREE.Raycaster()
    ray.setFromCamera(e.coordinates, CAMERA)
    ray.linePrecision = 0.001
    e.ray = ray.ray

    e.hit = undefined
    if (window.FRAME) {
        hit = FRAME.hitTestNoAnchor(e.coordinates.x / 2 + 0.5, -e.coordinates.y / 2 + 0.5)
        if (hit) {
            e.hit = new THREE.Vector3(hit[0].modelMatrix[12], hit[0].modelMatrix[13], hit[0].modelMatrix[14])
        }
        else {
            e.hit = undefined
        }
    }

    if (doIntersection)
    {
        ray.far = 1000
        ray.near = 0.00001

        e.intersects = ray.intersectObjects( SCENE.children );
    }

}

var addTouchCallbacks = (target, gate) => {
    target.addEventListener('mouseup', (e) => {
        if (gate && !gate(e)) return
        console.log("up", e)
        // e.stopImmediatePropagation()
        x = 2 * (e.offsetX / e.target.clientWidth) - 1;
        y = -2 * (e.offsetY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e)
        dispatchTo(e, mouseUp, "mouseUp");

    });
    target.addEventListener('mousedown', (e) => {
        if (gate && !gate(e)) return
        // e.stopImmediatePropagation()
        x = 2 * (e.offsetX / e.target.clientWidth) - 1;
        y = -2 * (e.offsetY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e, true)

        for(var ee of e.intersects)
        {
            if (ee.object._fieldSource)
            {
                if (ee.object._fieldSource.mouseDown)
                {
                    dispatchTo(e, ee.object._fieldSource.mouseDown, "FLine mouseDown");
                }
            }
        }

        dispatchTo(e, mouseDown, "mouseDown");
    });
    target.addEventListener('dblclick', (e) => {
        if (gate && !gate(e)) return
        // e.stopImmediatePropagation()
        x = 2 * (e.offsetX / e.target.clientWidth) - 1;
        y = -2 * (e.offsetY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e, false)
        dispatchTo(e, dblClick, "dblClick");
    });

    target.addEventListener('mousemove', (e) => {
        if (gate && !gate(e)) return
        // e.stopImmediatePropagation()
        x = 2 * (e.offsetX / e.target.clientWidth) - 1;
        y = -2 * (e.offsetY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e)
        dispatchTo(e, mouseMove, "mouseMove");
    });

    target.addEventListener('touchend', (e) => {
        if (gate && !gate(e)) return
        // e.stopImmediatePropagation()
        if (e.touches && e.touches[0]) {
            e.clientX = e.touches[0].clientX
            e.clientY = e.touches[0].clientY
        }
        x = 2 * (e.clientX / e.target.clientWidth) - 1;
        y = -2 * (e.clientY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e)
        dispatchTo(e, mouseUp, "mouseUp")
    });


    let tapedTwice = false;
    

    target.addEventListener('touchstart', (e) => {
        if (gate && !gate(e)) return

        if(!tapedTwice) {
            tapedTwice = true;
            setTimeout( function() { tapedTwice = false; }, 300 );
        }
        else
        {
            // e.stopImmediatePropagation()
            if (e.touches && e.touches[0]) {
                e.clientX = e.touches[0].clientX
                e.clientY = e.touches[0].clientY
            }
            x = 2 * (e.clientX / e.target.clientWidth) - 1;
            y = -2 * (e.clientY / e.target.clientHeight) + 1;
            e.coordinates = new THREE.Vector2(x, y)
            labelEvent(e, false)
            dispatchTo(e, dblClick, "dblClick")
            return
        }
        
        // e.stopImmediatePropagation()
        if (e.touches && e.touches[0]) {
            e.clientX = e.touches[0].clientX
            e.clientY = e.touches[0].clientY
        }
        x = 2 * (e.clientX / e.target.clientWidth) - 1;
        y = -2 * (e.clientY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e, true)

        for(var ee of e.intersects)
        {
            if (ee.object._fieldSource)
            {
                if (ee.object._fieldSource.mouseDown)
                {
                    dispatchTo(e, ee.object._fieldSource.mouseDown, "FLine mouseDown");
                }
            }
        }

        dispatchTo(e, mouseDown, "mouseDown")

    });
    target.addEventListener('touchmove', (e) => {
        if (gate && !gate(e)) return
        // e.stopImmediatePropagation()
        if (e.touches && e.touches[0]) {
            e.clientX = e.touches[0].clientX
            e.clientY = e.touches[0].clientY
        }
        x = 2 * (e.clientX / e.target.clientWidth) - 1;
        y = -2 * (e.clientY / e.target.clientHeight) + 1;
        e.coordinates = new THREE.Vector2(x, y)
        labelEvent(e)
        dispatchTo(e, mouseMove, "mouseMove")
    });
}
