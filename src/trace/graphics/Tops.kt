package trace.graphics

import field.app.RunLoop
import field.graphics.*
import field.graphics.util.onsheetui.Label
import field.utility.Dict
import field.utility.Vec4
import field.utility.plusAssign
import fieldbox.boxes.Box
import fieldbox.boxes.Colors
import fieldbox.boxes.FLineDrawing
import fieldbox.boxes.plugins.GraphicsSupport

/*
vec4 first_operator(vec2 tc)
{
	return texture(t_first_operator, vec3(tc.x, 1-tc.y, 0.5)).bgra;
}

vec4 first_operator(vec2 tc, float t)
{
	vec4 a = texture(t_first_operator, vec3(tc.x, 1-tc.y, t-0.5)).bgra;
	vec4 b = texture(t_first_operator, vec3(tc.x, 1-tc.y, t+1-0.5)).bgra;
	return a*(1-fract(t)) + b*fract(t);
}

 */
class Tops {


    // uses global names
    fun synchronizeUniformsAndDeclarations(root: Box) {
        val namesSoFar = mutableMapOf<String, Pair<OffersUniform<*>, Shader?>>()
        root.forEach {
            var a = it.properties.get(Dict.Prop<Any>("textureOperator"))
            if (a != null && a is Boolean && a) {
                val name = it.properties.get(Box.name)
                val fbo = it.properties.get(Dict.Prop<OffersUniform<*>>("fbo"))
                val shader = it.properties.get(Dict.Prop<Shader>("shader"))

                if (name.trim().isEmpty()) {
                    error(it, "Name can't be blank")
                } else if (!validName(name)) {
                    error(it, "Box doesn't have a valid name")
                } else if (namesSoFar.contains(name)) {
                    error(it, "Box has the same name as something else")
                } else if (fbo == null) {
                    error(it, "Box has some compilation problem")
                } else if (fbo !is OffersUniform) {
                    error(it, "Box is malformed")
                } else noError(it)

                if (fbo is OffersUniform) namesSoFar.put(name, fbo to shader)
            }
        }

        root.forEach { box ->

            var a = box.properties.get(Dict.Prop<Any>("textureOperator"))
            var ni = box.properties.get(Dict.Prop<Any>("textureOperator_noInputs"))
            if (ni != null) return@forEach

            if (a != null && a is Boolean && a) {
                val name = box.properties.get(Box.name)
                val fbo = box.properties.get(Dict.Prop<OffersUniform<*>>("fbo"))
                val shader = box.properties.get(Dict.Prop<Shader>("shader"))

                if (shader==null) return@forEach

                var u = "uniform float u_time; "

                namesSoFar.filter { it.key != name }.forEach {

                    if (it.value.first is FBO) {
                        u += "uniform sampler2D t_" + it.key + "; "
                        u += accessSampler(it.key)
                        shader.defaultBundle.set<Texture>("t_" + it.key) {

                            if (GraphicsContext.getContext() != null) {
                                (it.value.first as FBO).perform(-1)
                            }

                            it.value.first.uniform
                        }
                    } else if (it.value.first is TextureArray) {
                        u += "uniform sampler2DArray t_" + it.key + "; "
                        u += accessSamplerArray(it.key)
                        shader.defaultBundle.set<Texture>("t_" + it.key) {

                            if (GraphicsContext.getContext() != null) {
                                (it.value.first as TextureArray).perform(-1)
                            }

                            it.value.first.uniform
                        }
                    }
                }

                shader.defaultBundle.set<Float>("u_time", { RunLoop.time.get().toFloat() })

                val previous = box.properties.get(Dict.Prop<String>("_uniform_decl_"))
                if (u != previous) {
                    box.properties.put(Dict.Prop<String>("_uniform_decl_"), u)

                    println(" here we go ... --------")

                    GraphicsSupport.reload(box, mutableListOf(shader))

                    println(" -------- done ")

                }

            }
        }
    }

    private fun accessSampler(key: String): String {
        return """
            vec4 ${key}(vec2 tc)
            {
            	return texture(t_${key}, tc);
            } 

            vec4 ${key}(float tc_x, float tc_y)
            {
                return ${key}(vec2(tc_x, tc_y)); 
            }

            vec4 ${key}(vec2 tc, float t)
            {
            	vec4 a = texture(t_${key}, tc);
                return a;
            }

            vec4 ${key}(float tc_x, float tc_y, float tc_z)
            {
                return ${key}(vec2(tc_x, tc_y), tc_z); 
            }

        """.trimIndent().replace("\n", " ");
    }

    private fun accessSamplerArray(key: String): String {
        return """
            vec4 ${key}(vec2 tc)
            {
            	return texture(t_${key}, vec3(tc, 0)).bgra;
            }

            vec4 ${key}(float tc_x, float tc_y)
            {
                return ${key}(vec2(tc_x, tc_y)); 
            }

            vec4 ${key}(vec2 tc, float t)
            {
                vec4 a = texture(t_${key}, vec3(tc.x, 1-tc.y, t-0.5)).bgra;
                vec4 b = texture(t_${key}, vec3(tc.x, 1-tc.y, t+1-0.5)).bgra;
                return a*(1-fract(t)) + b*fract(t);
            }
            
            vec4 ${key}(float tc_x, float tc_y, float tc_z)
            {
                return ${key}(vec2(tc_x, tc_y), tc_z); 
            }
            
        """.trimIndent().replace("\n", " ");
    }

    private fun validName(name: String): Boolean {
        if (name.isEmpty()) return false
        name.toCharArray().forEachIndexed { i, c ->
            if (i == 0) if (!c.isJavaIdentifierStart()) return false
            if (!c.isJavaIdentifierPart()) return false

        }
        return true
    }

    fun error(b: Box, text: String) {
        if (b.properties.get(FLineDrawing.frameDrawing)==null) return
        b.properties.putToMap(FLineDrawing.frameDrawing, "__tops_error__", java.util.function.Function<Box, FLine> {

            val frame = it.properties.get(Box.frame)

            var f = FLine()

            val shim = 5

            val O = Math.max(20.0, Math.min(frame.w, frame.h) / 3.0);
            f.moveTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h - O - shim).toDouble())
            f.lineTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h + shim).toDouble())
            f.lineTo((frame.x + frame.w - O - shim).toDouble(), (frame.y + frame.h + shim).toDouble())
            f.lineTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h - O - shim).toDouble())

            f.attributes += StandardFLineDrawing.filled to true
            f.attributes += StandardFLineDrawing.stroked to true
            f.attributes += StandardFLineDrawing.fillColor to Vec4(0.5, 0, 0, -0.75)
            f.attributes += StandardFLineDrawing.strokeColor to Vec4(0.5, 0, 0, 0.75)

            f
        })

        (b.asMap_get("label") as MutableMap<String, String>).put("s", text)
    }

    fun noError(b: Box) {
        if (b.properties.get(FLineDrawing.frameDrawing)==null) return
        b.properties.putToMap(FLineDrawing.frameDrawing, "__tops_error__", java.util.function.Function<Box, FLine> {

            val frame = it.properties.get(Box.frame)

            var f = FLine()

            val shim = 5

            val O = Math.max(20.0, Math.min(frame.w, frame.h) / 3.0);
            f.moveTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h - O - shim).toDouble())
            f.lineTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h + shim).toDouble())
            f.lineTo((frame.x + frame.w - O - shim).toDouble(), (frame.y + frame.h + shim).toDouble())
            f.lineTo((frame.x + frame.w + shim).toDouble(), (frame.y + frame.h - O - shim).toDouble())

            f.attributes += StandardFLineDrawing.filled to true
            f.attributes += StandardFLineDrawing.color to Vec4(Colors.executionColor.x, Colors.executionColor.y, Colors.executionColor.z, 0.3)
            f.attributes += StandardFLineDrawing.strokeColor to Vec4(Colors.executionColor.x, Colors.executionColor.y, Colors.executionColor.z, 0.5)
            f
        })
        (b.asMap_get("label") as MutableMap<String, String>).put("s", "")
    }
}