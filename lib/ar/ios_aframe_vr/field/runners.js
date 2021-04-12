
__tasks = {}

var _timeNow = 0
var _timeAt = 0
var _isRunning = false
var _timePerTime = 0


var __setTime = (timeNow, isRunning) => {
    if (!_isRunning || !isRunning) {
        _timeNow = timeNow
        _isRunning = isRunning
        _timeAt = performance.now()
        _timePerTime = 0
    }
    else {
        // TODO: interpolation
        _timePerTime = (timeNow - _timeNow) / (performance.now() - _timeAt)
        _timeAt = performance.now()
        _isRunning = true
        _timeNow = timeNow
    }
}

var _t = () => {
    var T = !_isRunning ? _timeNow : (_timeNow + (performance.now() - _timeAt) * _timePerTime)

    if (typeof (currentTask) != 'undefined') {
        T = currentTask.remapTime(T)
    }

    return T;
}


function isPrimitive(o){return typeof o!=='object'||null}

class Task {
    constructor(name, beginning, middle, end, done, reportErrorTo, oneShotByDefault) {
        this.name = name
        this.state = 0
        this.reportErrorTo = reportErrorTo
        __tasks[name] = this
        this.beginning = beginning
        this.middle = [middle]
        this.end = end
        this.done = done

        this.timeStart = undefined
        this.timeEnd = undefined
        this.oneShotByDefault = oneShotByDefault

        this.__sandbox_id__ = window.__sandbox_id__
        this.__sandbox__ = window.__sandbox__
    
    }

    configureTimeRemapping(timeStart, timeEnd) {
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }

    remapTime(T) {
        if (typeof (this.timeStart) == 'undefined' || typeof (this.timeEnd) == 'undefined') return T
        return Math.max(0, Math.min(1, (T - this.timeStart) / (this.timeEnd - this.timeStart)))
    }

    run() {
        if (this.state == 1 && this.middle.length == 0) this.state = 2

        if (this.state == 0) {
            this.state = 1

            window.__sandbox_id__ = this.__sandbox_id__
            window.__sandbox__ = this.__sandbox__
            var next = this.beginning()

            if (next == "SKIP") {
                this.run()
            }
        }
        else if (this.state == 1) {
            if (this.middle.length > 0) {
                // console.log(" -- pop? ", this.middle)
                var q = this.middle.pop()
                // todo: error handling
                // console.log(" -- tick ", this.middle, q)
                try {
                    window.__sandbox_id__ = this.__sandbox_id__
                    window.__sandbox__ = this.__sandbox__
                    var next = q()

                    if (typeof (next) == 'undefined') {
                        if (this.oneShotByDefault) {
                            return
                        }
                        else {
                            this.middle.push(q)
                        }
                        return
                    }
                    if (!isPrimitive(next) && 'value' in next) {
                        if (typeof (next.value) == 'undefined' && !next.done) {
                            this.middle.push(q)
                            return
                        }
                        if (typeof (next.value) == 'undefined' && next.done) {
                            console.log(" bailing out", this.middle)
                            return
                        }
                        if (!next.done)
                            this.middle.push(q)

                        next = next.value
                    }

                    if (next == true) {
                        this.middle.push(q)
                        return
                    }
                    if (next.done) {
                        // generator, at the end
                        return
                    }
                    if (next == false) {
                        return
                    }
                    // q is a generator function (factory) not a generator
                    if (q instanceof (function* () { }).constructor) {
                        var ret = next.next()
                        if (!ret.done) {
                            if (this.oneShotByDefault) {

                            }
                            else {
                                this.middle.push(q)
                            }
                            this.middle.push(next.next.bind(next))
                        }
                        return
                    }
                    if (next instanceof Function) {
                        if (this.oneShotByDefault) {

                        }
                        else
                        {
                            this.middle.push(q)
                        }
                        this.middle.push(next)
                        return
                    }

                    // console.log(" at end ", this.middle)
                } catch (error) {
                    this.reportErrorTo(error)
                }

            }
        }
        else if (this.state == 2) {
            delete __tasks[this.name]

            this.state = 3

            window.__sandbox_id__ = this.__sandbox_id__
            window.__sandbox__ = this.__sandbox__
            this.end()
            this.done()

        }

    }

    stop() {
        console.log(" setting state to 2")
        this.state = 2
    }

}

var currentTask = undefined

__service = function () {
    for (var o of Object.keys(__tasks)) {
        currentTask = __tasks[o]
        __tasks[o].run()
    }
    currentTask = undefined
}

var __serviceCount = 0
var __lastServiceAt = 0

__beginService = function (service) {
    var s = function (timestamp, xrframe) {
        __lastServiceAt = Date.now()
        window.LASTXRFRAME = xrframe
        if (__serviceCount == service)
        {
            try{
                AFRAME.scenes[0].renderer.xr.getSession().requestAnimationFrame(s)
                // console.log("XRFRAME ", xrframe)
            }
            catch {
                requestAnimationFrame(s)
            }
            __service()
        }
    }
    s()
}

__kickoffService = () => {
    console.log(" -- service thread watchdog is running ")
    setTimeout(__kickoffService, 100)
    if (Date.now() - __lastServiceAt> 500)
    {
        console.log(" -- service thread seems dead, trying to restart --")
        __serviceCount ++
        __beginService(__serviceCount)
    }
}

__kickoffService()


var over = (time, name, callback) => {
	
	let start = window.Date.now()
	launch( function*(){
		callback(0)
		yield
		while(window.Date.now()-start<time*1000)
		{
			var alpha = (window.Date.now()-start)/(time*1000)
			callback(alpha)
			yield
		}
		callback(1)
	}, name)
}

over.__doc__ = "`over(seconds, name, callback) will call 'callback' with a value that goes from 0 to 1 over the duration 'seconds'"

var launch = (_r, name = "unnamed", oneShotByDefault = true) => {

    let cn = globalCodeName+name
    let rt = _field.globalReturnTo
    let sc = globlSourceCache

    var task = new Task(cn, function () { return "SKIP" }, _r, function () { }, function () {
        _field.send(rt, { message: "OK", kind: "RUN-STOP", altName:name })
    }, function (e) {
        StackTrace.fromError(e, { sourceCache: globlSourceCache }).then(stack => {

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

            _field.send(rt, { message: e.message, line: (altLineNumber || -1), kind: "ERROR", altName:cn })
            console.log(stack)
        })
    }, oneShotByDefault)

    _field.send(rt, { message: "OK", kind: "RUN-START", altName:name })
    return task;
}

launch.__doc__ = "`launch( () => { ...something... })` starts running a function as part of the animation loop"
