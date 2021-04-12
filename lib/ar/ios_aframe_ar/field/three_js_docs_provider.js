
all_docs = {}

for(var o of three_js_docs)
{
    if (typeof(o.name) == 'undefined') o.name = "(constructor)"
    if (!(o.inside in all_docs)) all_docs[o.inside] = {}

    all_docs[o.inside][o.name] = o
}
//
// for(var o of fline_js_docs)
// {
//     if (typeof(o.name) == 'undefined') o.name = "(constructor)"
//     if (!(o.inside in all_docs)) all_docs[o.inside] = {}
//
//     all_docs[o.inside][o.name] = o
//
// }

let lastDocProvider = docProviders

dress = (a) => {
    var q = "<span class='inside'>"+a.name+"</span>"
    if (a.args)
    {
        q += "<span class='signature'>("
        for(var aa of a.args)
        {
            q+="<span class='name'>"+aa[1]+"</span>:<span class='type'>"+aa[0]+"</span>, "
        }
        if (a.args.length>0)
            q = q.substring(0, q.length-2)
        q += ")</span>"
    }
    if (a.ret && a.ret!='null')
    {
        q += " &rarr; <span class='return'>"+a.ret+"</span>"
    }
    if (a.doc)
    {
        q += " &mdash; <span class='extradoc'>"+a.doc.replace(/\[.*?:(.+?) (.+?)\]/g, "<b>$2</b>")+"</span>";
    }

    return "<div class='providedDoc'>"+q.trim()+"</div>"
}

docProviders = (className, name, value) => {

    if (className in all_docs)
    {
        var a = all_docs[className]
        if (name in a)
        {
            var dressed =  dress(a[name])
            return dressed
        }
    }

    if (value)
    {
        if (value.__doc__)
        {
            return "<div class='providedDoc'>"+"<span class='extradoc'>"+value.__doc__+"</span></div>";
        }
        if (value.isMesh && className!="Mesh")
        {
            var a = docProviders("Mesh", name, null)
            if (a)
                return a
        }
        if (value.isObject3D && className!="Object3D")
        {
            var a = docProviders("Object3D", name, null)
            if (a)
                return a
        }
        if (value.isBufferGeometry && className!="BufferGeometry")
        {
            var a = docProviders("BufferGeometry", name, null)
            if (a)
                return a
        }
        if (value.isGeometry && className!="Geometry")
        {
            var a = docProviders("Geometry", name, null)
            if (a)
                return a
        }
        if (value.isMaterial && className!="Material")
        {
            var a = docProviders("Material", name, null)
            if (a)
                return a
        }
    }

    return lastDocProvider(className, name, value)
}

