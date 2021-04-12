var pretty = function (jsObject, indentLength, outputTo, fullFunction) {
    var indentString,
        newLine,
        newLineJoin,
        TOSTRING,
        TYPES,
        valueType,
        repeatString,
        prettyObject,
        prettyObjectJSON,
        prettyObjectPrint,
        prettyArray,
        functionSignature,
        pretty,
        visited;

    TOSTRING = Object.prototype.toString;

    TYPES = {
        "undefined": "undefined",
        "number": "number",
        "boolean": "boolean",
        "string": "string",
        "[object Function]": "function",
        "[object RegExp]": "regexp",
        "[object Array]": "array",
        "[object Date]": "date",
        "[object Error]": "error"
    };

    if (!Object.keys) {
        Object.keys = (function () {
            "use strict";
            var hasOwnProperty = Object.prototype.hasOwnProperty,
                hasDontEnumBug = !({
                    toString: null
                }).propertyIsEnumerable("toString"),
                dontEnums = [
                    "toString",
                    "toLocaleString",
                    "valueOf",
                    "hasOwnProperty",
                    "isPrototypeOf",
                    "propertyIsEnumerable",
                    "constructor"
                ],
                dontEnumsLength = dontEnums.length;

            return function (obj) {
                if (typeof obj !== "function" && (typeof obj !== "object" || obj === null)) {
                    throw new TypeError("Object.keys called on non-object");
                }

                var result = [],
                    prop, i;

                for (prop in obj) {
                    if (hasOwnProperty.call(obj, prop)) {
                        result.push(prop);
                    }
                }

                if (hasDontEnumBug) {
                    for (i = 0; i < dontEnumsLength; i++) {
                        if (hasOwnProperty.call(obj, dontEnums[i])) {
                            result.push(dontEnums[i]);
                        }
                    }
                }
                return result;
            };
        }());
    }

    valueType = function (o) {
        var type = TYPES[typeof o] || TYPES[TOSTRING.call(o)] || (o ? "object" : "null");
        return type;
    };

    repeatString = function (src, length) {
        var dst = "",
            index;
        for (index = 0; index < length; index += 1) {
            dst += src;
        }

        return dst;
    };

    protectLength = function(s)
    {
        if (s && s.length>5000) return "[\'...\']"
        return s
    }

    prettyObjectJSON = function (object, indent) {
        var value = [];

        indent += indentString;
        Object.keys(object).forEach(function (property) {
            if (!property.startsWith("_"))
                value.push(indent + "\"" + property + "\": " + pretty(object[property], indent));
        });

        return protectLength(value.join(newLineJoin) + newLine);
    };

    prettyObjectPrint = function (object, indent) {
        var value = [];

        indent += indentString;
        Object.keys(object).forEach(function (property) {
            if (!property.startsWith("_"))
                value.push(indent + property + ": " + pretty(object[property], indent));
        });
        return protectLength(value.join(newLineJoin) + newLine);
    };

    prettyArray = function (array, indent) {
        var index,
            length = array.length,
            value = [];

        indent += indentString;
        for (index = 0; index < length; index += 1) {
            value.push(pretty(array[index], indent, indent));
        }

        return protectLength(value.join(newLineJoin) + newLine);
    };

    functionSignature = function (element) {
        var signatureExpression,
            signature;

        element = element.toString();
        signatureExpression = new RegExp("function\\s*.*\\s*\\(.*\\)");
        if (signatureExpression.test(element))
        {
            signature = signatureExpression.exec(element);
            signature = signature ? signature[0] : "[object Function]";
        }
        else
        {
            signature = (""+element).split("\n")[0].trim()
            if (signature.endsWith("{")) signature = signature.substring(0, signature.length-1).trim();
        }

        return fullFunction ? element : "\"" + signature + "\"";
    };

    pretty = function (element, indent, fromArray) {
        var type;

        type = valueType(element);
        fromArray = fromArray || "";
        if (visited.indexOf(element) === -1) {
            switch (type) {
            case "array":
                visited.push(element);
                return fromArray + "[" + newLine + prettyArray(element, indent) + indent + "]";

            case "boolean":
                return fromArray + (element ? "true" : "false");

            case "date":
                return fromArray + "\"" + element.toString() + "\"";

            case "number":
                return fromArray + element;

            case "object":
                visited.push(element);
                if (fromArray.length>1000)
                    return fromArray + "{}"
                else
                    return fromArray + "{" + newLine + prettyObject(element, indent) + indent + "}";

            case "string":
                return fromArray + JSON.stringify(element);

            case "function":
                return fromArray + functionSignature(element);

            case "undefined":
                return fromArray + "undefined";

            case "null":
                return fromArray + "null";

            default:
                if (element.toString) {
                    return fromArray + "\"" + element.toString() + "\"";
                }
                return fromArray + "<<<ERROR>>> Cannot get the string value of the element";
            }
        }
        if (element.toString)
            return fromArray + "circular reference to " + element.toString();
        else {
            console.log(" fell through", element)
            return fromArray + "circular reference ";
        }
    };

    if (jsObject) {
        if (indentLength === undefined) {
            indentLength = 4;
        }

        outputTo = (outputTo || "print").toLowerCase();
        indentString = repeatString(outputTo === "html" ? "&nbsp;" : " ", indentLength);
        prettyObject = outputTo === "print" ? prettyObjectPrint : prettyObjectJSON;
        newLine = outputTo === "html" ? "<br/>" : "\n";
        newLineJoin = "," + newLine;
        visited = [];
        return pretty(jsObject, "") + newLine;
    }

    return ""+jsObject;
};
