package fieldbox.boxes.plugins

import fieldbox.boxes.Box


interface ShaderPreprocessor2
{
    fun translateVertexLine(line: Int): Int
    fun translateFragmentLine(line: Int): Int
    fun translateGeometryLine(line: Int): Int

    fun processVertexSource(b: Box, v: String): String
    fun processFragmentSource(b: Box, v: String): String
    fun processGeometrySource(b: Box, v: String): String

    fun beginProcess(b: Box)
    fun initialVertexShaderSource(b: Box): String
    fun initialGeometryShaderSource(b: Box): String
    fun initialFragmentShaderSource(b: Box): String



}