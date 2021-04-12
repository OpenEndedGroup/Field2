ternServer = new tern.Server({defs:[__ecmascript,__browser]})

ternCompleteMe = function(at, filetext)
{
    var ret = []
    
    ternServer.request({query:{type:"completions", includeKeywords:true, types:true, docs:true, file:"#0", end:at},files:[{type:"full", name:"here", text:filetext}]}, function(e, r){
        try{
            console.log(e)
            console.log(r)
            for(var ii of r.completions)
            {
                console.log(ii.type)
                ret.push(completionFor(ii.name, ii.name.substr(r.end-r.start, ii.name.length-(r.end-r.start)), ""+ii.type))
            }
        }
        catch(error)
        {
            console.log("--tern failed--")        
        }
    })
    
    return ret
}

expressionAt = function(at, filetext)
{
    var cx = new tern.Context()
    
    return tern.withContext(cx, function(){
        var p = tern.parse(filetext)
        tern.analyze(p)
        var n = tern.findExpressionAround(p, at, at)
        if (n && n.node) return [n.node.start, n.node.end]
        else return null
    })
}

completeMe = function(at, filetext, allowExecution)
{
    var r = []

    if (allowExecution)
    {
        var theExpression = expressionAt(at, filetext)
        if (theExpression!=null)
        {

            var text = filetext.substr(theExpression[0], 1+theExpression[1]-theExpression[0])
            console.log("the text "+text)

            if (text.indexOf(".")!=-1)
            {
                predot = text.substr(0, text.lastIndexOf("."))
                postdot = text.substr(text.lastIndexOf("."))
                if (postdot.startsWith(".") && postdot.length>1) postdot = postdot.substr(1)
                try {
                    console.log("first completion "+predot+" ||| "+postdot)
                    var r1 = _completeMe(predot.trim(), postdot.trim())
                    console.log(r1)
                    r = r.concat(r1)
                } catch (error) {                
                    console.log(error)
                }
            }
            else
            {
                try {
                    var r1 = _completeMe(text.trim(), "")
                    r = r.concat(r1)
                } catch (error) {                
                    console.log(error)
                }
            }
        }
    }

    console.log("INTROSPECTION contributes:")
    console.log(r)
    
    if (r.length==0)
    try {
        
        var r2 = ternCompleteMe(at, filetext)
        console.log("TERN contributes:")
        console.log(r2);
        if (r2)
            r = r.concat(r2);
    } catch (error) {
        console.log("--tern failed--")        
    }

    console.log(" returning final completions", r)

    return r
}

docProviders = (className, name, value) => undefined

_completeMe = function(predot, postdot)
{
    var ret = []
    console.log(" completeme "+predot+" "+postdot)
    
    if (postdot.startsWith(".")) postdot = postdot.substring(1)
    
    if (postdot=="") {
        for(var o in __sandbox__)
        {
            if ((""+o).startsWith(predot))
            {
                ret.push(completionFor(""+o, (""+o).substr(predot.length), __sandbox__[o], false, docProviders(name, ""+o,  __sandbox__[o])))
            }            
        }        
        for(var o in __superglobal)
        {
            if ((""+o).startsWith(predot))
            {
                ret.push(completionFor(""+o, (""+o).substr(predot.length), __superglobal[o], false, docProviders(name, ""+o,  __superglobal[o])))
            }            
        }        
    }
    //else 
    {
        // var inside = window.eval.call(window, predot)

        var inside = compileCode(predot)(undefined)

        var name;

        if (inside==null || typeof(inside)=='undefined') return ret

        if (inside && inside.constructor)
            name = inside.constructor.name;

        var done = new Set()

        console.log("live completion on "+inside+" / "+postdot)
        for(var o in inside)
        {
            console.log("key :"+o)
            if ((""+o).startsWith(postdot))
            {
                done.add(o)
                ret.push(completionFor(""+o, (""+o).substr(postdot.length), inside[o], false, docProviders(name, ""+o, inside)))
            }            
        } 

        for(var o of Object.getOwnPropertyNames(Object.getPrototypeOf(inside)))
        {
            if (done.has(o)) continue
            if ((""+o).startsWith(postdot))
            {
                try{
                    ret.push(completionFor(""+o, (""+o).substr(postdot.length), inside[o], false, docProviders(name, ""+o, inside)))
                }
                catch(e)
                {}
            }            

        }
        
        
    }

    console.log(" done ", ret)

    return ret
}

safe = function(n)
{
    if (n && n.length && n.length>120000) return "[... "+(""+n).substring(0, 500)+" +"+(n.length)+" characters long ...]";
    else if (typeof(n)=='undefined') return n;
    else return "<json>"+n;
}

safe2 = function(n)
{
    if (n && n.length && n.length>400) return "[... <i>+"+(""+n).substring(0, 390)+" +"+(n.length)+"</i> ...]";
    else if (typeof(n)=='undefined') return n;
    else return "<json>"+n;
}

function isPrimitive(arg) {
    var type = typeof arg;
    return arg == null || (type != "object" && type != "function");
}

myPretty = function(obj, indent, type, showFunctions)
{
    if (obj==null || typeof(obj) == 'undefined')
        return "<span class='type'>[unset]</span>"
        
    var h = ""
    if (typeof(obj) != 'undefined' && obj!=null)
        if (obj.__prototype__)
            h = ""+obj.__prototype__+" "


    return "<span class='type'>"+h+"</span>" + (isPrimitive(obj) ? (""+obj) : pretty(obj, indent, type, showFunctions));
}

completionFor = function(fullString, remains, obj, isDoc, extraDoc)
{
    var f = myPretty(obj, 2, "JSON", false)

    if (f=="[object Function]") f = ""

    f = "<span class='doc'>"+f+"</span>"
    if (typeof(obj)=='undefined') 
        f = ""

    console.log(" about to finalize compltion ", safe2(f))

    return {title:fullString, add:remains, from:safe2(f), 
    doc:(isDoc || 0), extraDoc:(extraDoc || "")}
}