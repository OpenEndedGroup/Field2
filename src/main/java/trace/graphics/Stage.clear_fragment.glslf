#version 410
layout(location=0) out vec4 _output;

uniform vec4 color;

void main()
{
    _output = color;
}
