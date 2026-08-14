#version 150

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
    float LineWidth;
};

uniform sampler2D Sampler0;

in vec4 vertexColor;
flat in vec2 dotCenter;
out vec4 fragColor;

float perceivedLuminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 screenSize = vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy / screenSize;
    vec2 texel = 1.0 / screenSize;

    vec3 average = vec3(0.0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            average += texture(
                Sampler0,
                dotCenter + vec2(x, y) * texel * 2.0
            ).rgb;
        }
    }
    average /= 9.0;

    vec3 background = texture(Sampler0, uv).rgb;
    float threshold = vertexColor.r;
    float transitionWidth = max(vertexColor.g, 1.0 / 255.0);
    float chooseWhite = 1.0 - smoothstep(
        threshold - transitionWidth,
        threshold + transitionWidth,
        perceivedLuminance(average)
    );
    vec3 target = vec3(chooseWhite);
    float strength = clamp(
        vertexColor.a * ColorModulator.a, 0.0, 1.0
    );

    fragColor = vec4(mix(background, target, strength), 1.0);
}
