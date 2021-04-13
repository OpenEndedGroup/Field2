
var container = {}

const sandboxTarget = {}
const sandboxProxy = new Proxy(sandboxTarget, { has, get, set })

var mixer = null
var updateMixer = () => {
    requestAnimationFrame(updateMixer)

    if (mixer == null && window.SCENE) {
        console.log("auto made mixer")
        mixer = new THREE.AnimationMixer(SCENE)
    }
    if (mixer != null)
        mixer.update(0.02)
}

updateMixer()

var mapDirectory = (dirname, to) => {
    _field.send("files.map", { mapFrom: to, mapTo: dirname })
}

mapDirectory.__doc__ = 'The `mapDirectory` function exposes some part of your desktop computer (the machine running Field) to devices connected to our webserver. For example `mapDirectory("/Users/marc/Desktop", "/assets")`.'

__superglobal.mapDirectory = mapDirectory

var addPointLight = (name) => {

    var color = 0xffffff;
    var intensity = 1;
    var distance = 0;

    var light = new THREE.PointLight(color, intensity, distance);
    light.name = name || 'PointLight';

    // editor.execute( new AddObjectCommand( light ) );
    SCENE.attach[name] = light

    return light
}

addPointLight.__doc__ = "addPointLight(name). Adds a point light to the scene (called “name”). You can move this around with `position`"
__superglobal.addPointLight = addPointLight

var addVideoPlane = (url, name, size = 1) => {
    var video = document.createElement('video');
    video.playsInline = true
    video.crossOrigin = "anonymous";
    video.src = url;
    video.load(); // must call after setting/changing source

    var texture = new THREE.VideoTexture(video)
    texture.format = THREE.RGBFormat;

    texture.minFilter = THREE.LinearFilter;
    texture.magFilter = THREE.LinearFilter;
    texture.generateMipmaps = false;

    var movieMaterial = new THREE.MeshBasicMaterial({ map: texture, overdraw: true, side: THREE.DoubleSide });
    var movieGeometry = new THREE.PlaneGeometry(size * .25 * 16 / 9, size * .25 * 1, 4, 4);
    var movieScreen = new THREE.Mesh(movieGeometry, movieMaterial);
    movieScreen.position.set(0, 0, 0);
    movieScreen.name = name

    // editor.execute( new AddObjectCommand( movieScreen ) );
    SCENE.attach[name] = movieScreen

    movieScreen._fieldSource = {}
    movieScreen.mouseDown = movieScreen._fieldSource.mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));

    return { mesh: movieScreen, material: movieMaterial, video: video, texture: texture, mouseDown: movieScreen.mouseDown }
}
addVideoPlane.__doc__ = "addVideoPlane(url, name, size=1). Adds plane with a video on it into the scene. The plane is size*25cm tall and at the origin. You can move it around with `addVideoPlane(url, name, size=1).mesh.position.x=10`"
__superglobal.addVideoPlane = addVideoPlane


var addImagePlane = (url, name, size = 1) => {
    var texture = new THREE.TextureLoader().load(url)
    texture.format = THREE.RGBFormat;
    texture.minFilter = THREE.LinearFilter;
    texture.magFilter = THREE.LinearFilter;
    texture.generateMipmaps = false;

    var movieMaterial = new THREE.MeshBasicMaterial({ map: texture, overdraw: true, side: THREE.DoubleSide });
    var movieGeometry = new THREE.PlaneGeometry(size * .25 * 16 / 9, size * .25 * 1, 4, 4);
    var movieScreen = new THREE.Mesh(movieGeometry, movieMaterial);
    movieScreen.position.set(0, 0, 0);
    movieScreen.name = name

    // editor.execute( new AddObjectCommand( movieScreen ) );
    SCENE.attach[name] = movieScreen

    movieScreen._fieldSource = {}
    movieScreen.mouseDown = movieScreen._fieldSource.mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));

    return { mesh: movieScreen, material: movieMaterial, texture: texture, mouseDown: movieScreen.mouseDown }
}

__superglobal.Image = Image
addImagePlane.__doc__ = "addImagePlane(url, name, size=1). Adds plane with an image on it into the scene. The plane is size*25cm tall and at the origin. You can move it around with `addVideoPlane(url, name, size=1).mesh.position.x=10`"
__superglobal.addImagePlane = addImagePlane

var addPlane = (name, size = 1) => {
    var movieMaterial = new THREE.MeshLambertMaterial({ overdraw: true, side: THREE.DoubleSide });
    var movieGeometry = new THREE.PlaneGeometry(size * .25 * 16 / 9, size * .25 * 1, 4, 4);
    var movieScreen = new THREE.Mesh(movieGeometry, movieMaterial);
    movieScreen.position.set(0, 0, 0);
    movieScreen.name = name

    // editor.execute( new AddObjectCommand( movieScreen ) );
    SCENE.attach[name] = movieScreen

    movieScreen._fieldSource = {}
    movieScreen.mouseDown = movieScreen._fieldSource.mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));


    return { mesh: movieScreen, material: movieMaterial, mouseDown: movieScreen.mouseDown }
}
addPlane.__doc__ = "addPlane(name, size=1). Adds a plane to the scene. The plane is size*25cm tall and at the origin. You can move it around with `addPlane(url, name, size=1).mesh.position.x=10` etc."
__superglobal.addPlane = addPlane

var addBox = (name, size = 1) => {
    var movieMaterial = new THREE.MeshLambertMaterial({ overdraw: true, side: THREE.DoubleSide });
    var movieGeometry = new THREE.BoxGeometry(size, size, size);
    var movieScreen = new THREE.Mesh(movieGeometry, movieMaterial);
    movieScreen.position.set(0, 0, 0);
    movieScreen.name = name

    // editor.execute( new AddObjectCommand( movieScreen ) );
    SCENE.attach[name] = movieScreen

    movieScreen._fieldSource = {}
    movieScreen.mouseDown = movieScreen._fieldSource.mouseDown = new Proxy(new Map(), idempotencyHandler(function (n, is, was) { }));

    return { mesh: movieScreen, material: movieMaterial, mouseDown: movieScreen.mouseDown }
}
addBox.__doc__ = "addBox(name, size=1). Adds a box to the scene. This plan is 'size' meters along each dimension and centered at the origin. You can move it around with addBox(name).mesh.positon.x=1 etc."
__superglobal.addBox = addBox




var loadFBX = (url, name, callback) => {
    var loader = new THREE.FBXLoader();

    let loaded = {}

    if (window.__sandbox__)
        __sandbox__[name] = loaded
    else
        window[name] = loaded

    loaded.animations = []
    loaded.objects = []


    loaded.play = (looping) => {

        // loaded.stop()

        var lop = true
        if (typeof (looping) != 'undefined')
            lop = looping

        loaded.animations.forEach(x => {
            x.paused = false
            if (lop)
                x.loop = THREE.LoopRepeat
            else
                x.loop = THREE.LoopOnce

            x.timeScale = 1
            x.play()
        })
    }
    loaded.stop = () => {
        loaded.animations.forEach(x => {
            x.stop()
        })
    }
    loaded.pause = () => {
        loaded.apply(loaded.currentTime())
    }

    loaded.currentTime = () => {
        if (loaded.animations[0]) {
            return loaded.animations[0].time
        }
        return 0
    }
    loaded.apply = (t) => {
        loaded.animations.forEach(x => {
            x.paused = false
            x.timeScale = 0
            x.time = t
            x.loop = THREE.LoopRepeat
            x.play()
        })
        mixer.update(0.000)
        loaded.animations.forEach(x => {
            x.timeScale = 0
            x.time = t
            x.loop = THREE.LoopRepeat
            x.paused = true
        })
    }
    loader.load(url, function (object) {
        // object.mixer = new THREE.AnimationMixer( object );
        // mixers.push( object.mixer );

        // var action = object.mixer.clipAction( object.animations[ 0 ] );
        // action.play();

        if (object.animations && object.animations[0]) {
            var q = mixer.clipAction(object.animations[0], object)
            q.timeScale = 1
            q.paused = false
            q.loop = THREE.LoopOnce
            loaded.animations.push(q)
        }

        object.name = name

        // object.traverse( function ( child ) {
        //     if ( child.isMesh ) {
        //         child.castShadow = true;
        //         child.receiveShadow = true;
        //     }
        // } );

        SCENE.add(object);

        if (callback) {

            try {
                callback(loaded)
            }
            catch (error) {
                console.error("error in callback")
                console.error(error)

                StackTrace.fromError(error, {}).then(stack => {

                    for (var s of stack) {
                        if (s.fileName && s.fileName.startsWith("box")) {
                            console.log("(handler error)", { message: error.message, line: s.lineNumber, kind: "ERROR", filename: s.fileName })
                            _field.send("(handler error)", { message: error.message, line: s.lineNumber, kind: "ERROR", filename: s.fileName })
                        }
                    }

                    console.dir(stack)
                })
            }
        }

    });


}

__superglobal.loadFBX = loadFBX

loadJSONScene = (url, name) => {
    var loader = new THREE.ObjectLoader();

    let loaded = {}

    if (window.__sandbox__)
        __sandbox__[name] = loaded
    else
        window[name] = loaded

    loader.load(
        // resource URL
        url,

        function (obj) {
            loaded.scene = obj
            SCENE.add(obj);
        },
        // onProgress callback
        function (xhr) {
            console.log((xhr.loaded / xhr.total * 100) + '% loaded');
        },

        // onError callback
        function (err) {
            console.error('An error happened');
        }
    );
}

__superglobal.loadJSONScene = loadJSONScene


loadPointCloudPLY = (url, name, callback) => {

    let loaded = {}

    if (window.__sandbox__)
        __sandbox__[name] = loaded
    else
        window[name] = loaded

    var loader = new THREE.PLYLoader()
    loader.setPropertyNameMapping({
        diffuse_red: 'red',
        diffuse_green: 'green',
        diffuse_blue: 'blue'
    });

    loader.load(url, (geometry) => {
        geometry.sourceType = "ply";
        geometry.sourceFile = url;

        if (geometry.index == null) {
            var material = new THREE.PointsMaterial({
                side: THREE.DoubleSide,
                blending: THREE.NormalBlending,
                vertexColors: THREE.VertexColors,
                map: disc,
                transparent: true,
                depthTest: false,
                size: 0.1
            })

            var mesh = new THREE.Points(geometry, material);
            mesh.name = name;
            editor.execute(new AddObjectCommand(mesh));
            loaded.mesh = mesh

        }
        else {
            var material = new THREE.MeshStandardMaterial();
            var mesh = new THREE.Mesh(geometry, material);
            mesh.name = name;
            editor.execute(new AddObjectCommand(mesh));
            loaded.mesh = mesh

        }

        try {
            callback(loaded)
        }
        catch (error) {
            console.error("error in callback")
            console.error(error)

            StackTrace.fromError(error, {}).then(stack => {

                for (var s of stack) {
                    if (s.fileName && s.fileName.startsWith("box")) {
                        console.log("(handler error)", { message: error.message, line: s.lineNumber, kind: "ERROR", filename: s.fileName })
                        _field.send("(handler error)", { message: error.message, line: s.lineNumber, kind: "ERROR", filename: s.fileName })
                    }
                }

                console.dir(stack)
            })
        }
    }, (progress) => { }, (error) => { })


}

__superglobal.loadPointCloudPLY = loadPointCloudPLY

loadGLTF = (url, name) => {
    // Instantiate a loader
    var loader = new THREE.GLTFLoader();

    let loaded = {}

    if (window.__sandbox__)
        __sandbox__[name] = loaded
    else
        window[name] = loaded

    // Optional: Provide a DRACOLoader instance to decode compressed mesh data
    THREE.DRACOLoader.setDecoderPath('/examples/js/libs/draco');
    loader.setDRACOLoader(new THREE.DRACOLoader());
    // Load a glTF resource
    loader.load(
        // resource URL
        url,

        // called when the resource is loaded
        function (gltf) {


            // scene.add( gltf.scene );

            console.log(gltf.animations); // Array<THREE.AnimationClip>
            console.log(gltf.scene); // THREE.Scene
            console.log(gltf.scenes); // Array<THREE.Scene>
            console.log(gltf.cameras); // Array<THREE.Camera>
            console.log(gltf.asset); // Object


            loaded.meshes = []
            gltf.scene.traverse(x => {
                if (x.isMesh)
                    loaded.meshes.push(x)
            })
            loaded.animations = gltf.animations
            loaded.scene = gltf.scene
            loaded.scenes = gltf.scenes
            loaded.cameras = gltf.cameras
            loaded.asset = gltf.asset

            loaded.play = () => {
                loaded.animations.forEach(x => {
                    var q = mixer.clipAction(x)
                    q.timeScale = 1
                    q.paused = false
                    q.play()
                })
            }
            loaded.stop = () => {
                loaded.animations.forEach(x => {
                    var q = mixer.clipAction(x)
                    q.timeScale = 1
                    q.paused = false
                    q.stop()
                })
            }
            loaded.apply = (t) => {
                loaded.animations.forEach(x => {
                    var q = mixer.clipAction(x)
                    q.paused = false
                    q.timeScale = 0
                    q.time = t
                    q.play()
                })
                mixer.update(0.000)
                loaded.animations.forEach(x => {
                    var q = mixer.clipAction(x)
                    q.timeScale = 0
                    q.time = t
                    q.paused = true
                })
            }


            editor.execute(new AddObjectCommand(gltf.scene));


        },
        // called while loading is progressing
        function (xhr) {

            console.log((xhr.loaded / xhr.total * 100) + '% loaded');

        },
        // called when loading has errors
        function (error) {

            console.log('An error happened');
            console.log(error)

        }
    );
}

__superglobal.loadGLTF = loadGLTF

function loadJSON(url, name, added) {
    return fetch(url).then(r => {
        return r.json();
    }).then(data => {
        handleJSON(data, name, added);
    })
}

function handleJSON(data, name, added) {

    if (data.metadata === undefined) { // 2.0
        data.metadata = { type: 'Geometry' };
    }

    if (data.metadata.type === undefined) { // 3.0
        data.metadata.type = 'Geometry';
    }

    if (data.metadata.formatVersion !== undefined) {
        data.metadata.version = data.metadata.formatVersion;
    }

    switch (data.metadata.type.toLowerCase()) {

        case 'buffergeometry':

            var loader = new THREE.BufferGeometryLoader();
            var result = loader.parse(data);

            var mesh = new THREE.Mesh(result);

            result.traverse(x => {
                if (x.isMesh) {
                    if (added) {
                        added(x)
                    }
                    if (typeof (x.name) == 'undefined' || x.name == null || x.name == "")
                        x.name = name
                    else
                        x.name = name + x.name
                }
            })
            editor.execute(new AddObjectCommand(mesh));

            break;

        case 'geometry':

            var loader = new THREE.JSONLoader();
            // loader.setResourcePath(scope.texturePath);
            loader.setResourcePath('');

            var result = loader.parse(data);

            var geometry = result.geometry;
            var material;

            if (result.materials !== undefined) {

                if (result.materials.length > 1) {

                    material = new THREE.MultiMaterial(result.materials);

                } else {

                    material = result.materials[0];

                }

            } else {

                material = new THREE.MeshStandardMaterial();

            }

            // geometry.sourceType = "ascii";
            // geometry.sourceFile = file.name;

            var mesh;

            if (geometry.animation && geometry.animation.hierarchy) {

                mesh = new THREE.SkinnedMesh(geometry, material);

            } else {

                mesh = new THREE.Mesh(geometry, material);

            }

            if (typeof (mesh.name) == 'undefined' || mesh.name == null || mesh.name == "")
                mesh.name = name
            else
                mesh.name = name + mesh.name

            if (added) added(mesh)
            editor.execute(new AddObjectCommand(mesh));

            break;

        case 'object':

            var loader = new THREE.ObjectLoader();
            loader.setResourcePath('');

            var result = loader.parse(data);

            if (result instanceof THREE.Scene) {

                result.traverse(x => {
                    if (x.isMesh) {
                        if (added) {
                            added(x)
                        }
                        if (typeof (x.name) == 'undefined' || x.name == null || x.name == "")
                            x.name = name
                        else
                            x.name = name + x.name
                    }
                })
                editor.execute(new SetSceneCommand(result));
            } else {

                result.traverse(x => {
                    if (x.isMesh) {
                        if (added) {
                            added(x)
                        }
                        if (typeof (x.name) == 'undefined' || x.name == null || x.name == "")
                            x.name = name
                        else
                            x.name = name + x.name
                    }
                })
                editor.execute(new AddObjectCommand(result));

            }

            break;

        case 'app':

            editor.fromJSON(data);

            break;

    }

}



var shaderMaterial = (vertexShader, fragmentShader, uniformmap) => {
    return new THREE.RawShaderMaterial({
        vertexShader: vertexShader,
        fragmentShader: fragmentShader,
        transparent: true,
        uniforms: uniformmap
    })
}

var reloadShaderMaterial = (rawShaderMaterial, vertexShader, fragmentShader, callback) => {
    rawShaderMaterial.vertexShader = vertexShader
    rawShaderMaterial.fragmentShader = fragmentShader
    rawShaderMaterial.needsUpdate = true
    rawShaderMaterial.uniformsNeedUpdate = true
    setTimeout(() => {
        if (!rawShaderMaterial.program) return;

        if (rawShaderMaterial.program.diagnostics && !rawShaderMaterial.program.diagnostics.runnable) {
            var e = "Errors on program reload : " + rawShaderMaterial.program.diagnostics.programLog
            var v = rawShaderMaterial.program.diagnostics.vertexShader.log
            var f = rawShaderMaterial.program.diagnostics.fragmentShader.log

            callback({ type: "error", message: e, v: v, f: f })
        }
        else {
            callback({ type: "success" })
        }
    }, 200)
}


var default_vertexShader = `precision highp float;
precision highp int;
// those two lines just need to be there at the start

// this is an 'attribute' - it is associated
// with each _vertex_
attribute vec3 position;

// similarily this is a texture coordinate
// that's on a vertex
attribute vec2 uv;
// this is an _output_. It's computed here and sent
// onwards to the fragment shader
varying vec2 vUv;

// same as uv above
attribute vec3 normal;
varying vec3 vNormal;

// these are _uniforms_: they don't vary from vertex
// to vertex. These are associated whith the whole
// mesh. These are matrices that define where the camera
// is and what it can see
uniform mat3 normalMatrix;
uniform mat4 modelViewMatrix;
uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

void main()
{
    // we just copy the input texture coordinate
    // to the outuput
    vUv = uv;

    // we transform the _position_ by the camera
    gl_Position = modelViewMatrix * vec4( position, 1.0);
    
    // and project it onto the screen
    gl_Position = projectionMatrix  * gl_Position;
    
    // and figure out what this does to the normal
    vNormal = normalMatrix * normal;
}`
var default_fragmentShader = `precision highp float;
precision highp int;
// the stuff above just needs to be there

// these are _inputs_ from the vertex shader
// you'll see declarations just like these 
// there
varying vec3 vNormal;
varying vec3 pos;
varying vec2 vUv;

// this is the declaration for a texture map called 'map
//uniform sampler2D map;

void main()
{
    // and this is how we look up a texture
    // given texture coordinates 'vUv'
    //vec4 m = texture2D(map, vUv);

    // in Javascript we'd call this 'vec'
    // but here we have to say vec4
    // this is our translucent red
    gl_FragColor = vec4(1,0,0,0.5);
}`

var knownShaders = new Map()

var processUniformMap = (um) => {
    var r = {}
    for (var q in um)
        r[q] = { value: um[q] }
    return r
}
var newShaderForBox = (uniformMap = {}) => {

    if (_.__shader__) {
        _.__shader__.uniforms = processUniformMap(uniformMap)
        _.__shader__.vertexShader = _.__shader__.vertexShader + " "
        return _.__shader__
    }

    _.__shader__ = shaderMaterial(default_vertexShader, default_fragmentShader, processUniformMap(uniformMap))

    var UUID = window.__sandbox_id__
    knownShaders.set(UUID, _.__shader__)

    _field.send("request.shaderSupport", {
        defaultVertex: default_vertexShader,
        defaultFragment: default_fragmentShader,
        boxID: window.__sandbox_id__, reloadCallback: UUID
    })

    return _.__shader__
}

__superglobal.newShaderForBox = newShaderForBox

var __reloadShader = (uid, v, f) => {
    reloadShaderMaterial(knownShaders.get(uid), v, f, (contents) => {
        if (contents.type == 'success') {
            contents.boxID = uid
            _field.send("status.shaderSupport", contents)
        }
        else {
            contents.boxID = uid
            _field.send("status.shaderSupport", contents)
        }
    })
}

var __vt__ = null

var liveVideoTexture = () => {
    if (__vt__) return __vt__
    var video = window.document.createElement('video');
    video.width = 1920;
    video.height = 1080;
    video.autoplay = true;

    window.navigator.getUserMedia({ video: { width: 1920, height: 1080 } }, function (stream) {
        video.srcObject = stream
    }, function (error) {
        console.log("Failed to get a stream due to", error);
    });

    return __vt__ = new THREE.VideoTexture(video)
}

var loadTexture = (url) => {
    return new THREE.TextureLoader().load(url)
}

__superglobal.liveVideoTexture = liveVideoTexture
__superglobal.loadTexture = loadTexture
