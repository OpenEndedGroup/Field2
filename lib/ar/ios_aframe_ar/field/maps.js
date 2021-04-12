
function isFunction(functionToCheck) {
    return functionToCheck && {}.toString.call(functionToCheck) === '[object Function]';
}

var idempotencyHandler = function (displace) {
    return {
        get(target, name) {
            if (name === Symbol.iterator)
                return target[Symbol.iterator].bind(target)
            if (typeof (name) === Symbol)
                return target[name]
            if (name in target) {
                var v = target[name]
                if (isFunction(v)) {
                    return v.bind(target)
                }
                return v
            }

            return target.get(name)
        },

        set(target, name, value) {
            var eq = target.get(name) != value

            displace(name, value, target.get(name))
            if (eq) {
                target.set(name, value)
            }
            return true
        },

        deleteProperty(target, name) {
            if (target.has(name)) {
                displace(name, target.get(name), undefined)
                target.delete(name)
            }
        }
    }
}

var sandboxProxies = new Map()
var sandboxTargets = new Map()

var __superglobal = {}
__superglobal.window = window

// standard imports
__superglobal.Math = Math
__superglobal.console = console


__superglobal.document = document
__superglobal.navigator = navigator
__superglobal._t = _t
__superglobal.__tasks = __tasks

var dispatchTo = function (event, map, name) {
    var deletme = []
    for (var m of map) {
        try {
            m[1](event)
        }
        catch (error) {
            console.log(" exception thrown in '" + name + "' event handler")
            console.dir(error)

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
}




const sandbox_edges = new Map()

function __clearEdges() {
    sandbox_edges.clear()
}

function __addEdge(from, to) {
    if (!(sandbox_edges.has(to))) sandbox_edges.set(to, new Set())
    sandbox_edges.get(to).add(from)
}

function __removeEdge(from, to) {
    if (!(sandbox_edges.has(to))) return;
    sandbox_edges.get(to).delete(from)
}

const underscores = new Map()

function getUnderscore(at) {
    if (typeof (at) == 'undefined') return undefined
    if (!(underscores.has(at))) {
        let m = new Map()
        underscores.set(at, m)
        m.set("__id__", at)
        return m
    }
    return underscores.get(at)
}

function traverse(callback, at, seen) {
    if (seen.has(at)) return
    var term = callback(getUnderscore(at))
    if (typeof (term) != 'undefined') return term

    var next = sandbox_edges.get(at)
    seen.add(at)

    if (typeof (next) != 'undefined')
        for (var o of next) {
            var term = traverse(callback, o, seen);
            if (typeof (term) != 'undefined') return term
        }
    return undefined
}


function underscore_has(target, key) {
    return typeof (traverse(_ => {
        if (_.has(key)) return true
        return undefined
    }, __sandbox_id__, new Set())) != 'undefined'
}


function underscore_get(target, key) {
    var found = (traverse(_ => {
        if (_.has(key))
        {
            var found =  _.get(key)

            if (typeof (found) != 'undefined') {
                if (found.needsContext) {
                    return found(getUnderscore(__sandbox_id__), _)
                }
            }

            return found
        }
        return undefined
    }, __sandbox_id__, new Set()))


    return found
}

function underscore_set(target, key, value) {
    getUnderscore(__sandbox_id__).set(key, value)
}

function underscore_root_has(target, key) {
    return typeof (traverse(_ => {
        if (_.has(key)) return true
        return undefined
    }, "root", new Set())) != 'undefined'
}

function underscore_root_get(target, key) {
    return (traverse(_ => {
        if (_.has(key)) return _.get(key)
        return undefined
    }, "root", new Set()))
}

function underscore_root_set(target, key, value) {
    getUnderscore("root").set(key, value)
}


const _ = new Proxy(new Map(), { has: underscore_has, get: underscore_get, set: underscore_set })
const __ = new Proxy(new Map(), { has: underscore_root_has, get: underscore_root_get, set: underscore_root_set })
__superglobal._ = _
__superglobal.__ = __

var beginner = (target, foundAt) => {
    return (name) => {
        if (__sandbox_id__ && __sandbox_id__==foundAt.get("__id__")) throw new Error("can't call _.begin() inside itself")
        _field.send("util.begin", {id:foundAt.get("__id__"), name:name})
    }
}
beginner.needsContext = true
__.begin = beginner

var ender = (target, foundAt) => {
    return (name) => {
        _field.send("util.end", {id:foundAt.get("__id__"), name:name})
    }
}
ender.needsContext = true
__.end = ender


function has(target, key) {
    // console.log("HAS, ", target, key, key in __superglobal)
    return /*!(key in window) && */!(key in __superglobal) /*&& key in target*/
}

function get(target, key) {
    // console.log("GET, ", target, key, key in __superglobal)
    // if (key in window) return window[key]
    if (key in __superglobal) return __superglobal[key]

    return target[key]
}

function set(target, key, value) {
    // console.log("SET, ", target, key, value , key in __superglobal)
    if (key in __superglobal) return __superglobal[key] = value

    return target[key] = value
}



function compileCode(src) {
    const srcWrap = 'with (__sandbox__) {' + src + '\n}'


    return function (sandbox) {
        if (sandbox) {
            if (!sandboxProxies.has(sandbox)) {
                const sandboxTarget = {}
                const sandboxProxy = new Proxy(sandboxTarget, { has, get, set })
                sandboxProxies.set(sandbox, sandboxProxy)
                sandboxTargets.set(sandbox, sandboxTarget)
            }

            window.__sandbox__ = sandboxProxies.get(sandbox)
            window.__sandbox_id__ = sandbox
            window.__sandbox__.__sandbox_id__ = sandbox
            window.__sandbox__.__returnTo = window._field.globalReturnTo
        }
        else {
            window.__sandbox_id__ = undefined
        }

        // can't use function here, we want the completion value
        console.log(" eval " + srcWrap)
        return window.eval.call(window, srcWrap)
    }
}

function runInSandbox(uid, code) {
    return compileCode(code)(uid)
}


function compileCode_generator(src) {
    const srcWrap = '__map__ = function*(){with (__sandbox__) {' + src + '\n}}'


    return function (sandbox) {
        if (sandbox) {
            if (!sandboxProxies.has(sandbox)) {
                const sandboxTarget = {}
                const sandboxProxy = new Proxy(sandboxTarget, { has, get, set })
                sandboxProxies.set(sandbox, sandboxProxy)
                sandboxTargets.set(sandbox, sandboxTarget)
            }

            window.__sandbox__ = sandboxProxies.get(sandbox)
            window.__sandbox_id__ = sandbox
            window.__sandbox__.__returnTo = window._field.globalReturnTo
        }
        else {
            window.__sandbox_id__ = undefined
        }

        return window.eval.call(window, srcWrap)
    }
}

function runInSandbox_generator(uid, code) {
    return compileCode_generator(code)(uid)
}

// e.g
/*
box = {}
compileCode("var banana=Math.random()")(box)
box
*/



