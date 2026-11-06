#version 120
uniform float farEdge;
uniform vec3 fogColor;
varying vec3 terrainColor;
varying float rangeDistance;
void main() {
    float coverage = (1.0 - smoothstep(farEdge - 32.0, farEdge, rangeDistance));
    vec2 pixel = mod(floor(gl_FragCoord.xy), 4.0);
    float threshold = mod(pixel.x * 2.0 + pixel.y * 3.0, 4.0) * 4.0 + mod(pixel.x + pixel.y * 2.0, 4.0);
    if (coverage * 16.0 <= threshold) discard;
    float haze = smoothstep(farEdge * 0.65, farEdge * 0.98, rangeDistance);
    gl_FragColor = vec4(mix(terrainColor, fogColor, haze), 1.0);
}
