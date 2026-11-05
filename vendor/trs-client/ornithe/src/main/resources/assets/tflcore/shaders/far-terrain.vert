#version 120
uniform vec2 origin;
uniform float sunBrightness;
varying vec3 terrainColor;
varying float rangeDistance;
void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    float illumination = max(gl_MultiTexCoord0.y, gl_MultiTexCoord0.x * sunBrightness);
    terrainColor = gl_Color.rgb * (0.18 + 0.82 * illumination);
    vec2 position = gl_Vertex.xz + origin;
    rangeDistance = max(abs(position.x), abs(position.y));
}
