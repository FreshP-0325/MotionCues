#version 150

uniform sampler2D BackgroundSampler;
uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float LuminanceThreshold;
uniform float TransitionWidth;

in vec4 vertexColor;
flat in vec2 dotCenter;
out vec4 fragColor;

float perceivedLuminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec2 texel = 1.0 / ScreenSize;

    // Average a small neighborhood so textured blocks do not make one dot
    // flicker between light and dark on adjacent fragments.
    vec3 average = vec3(0.0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            average += texture(BackgroundSampler, dotCenter + vec2(x, y) * texel * 2.0).rgb;
        }
    }
    average /= 9.0;

    vec3 background = texture(BackgroundSampler, uv).rgb;
    float luminance = perceivedLuminance(average);
    float chooseWhite = 1.0 - smoothstep(
        LuminanceThreshold - TransitionWidth,
        LuminanceThreshold + TransitionWidth,
        luminance
    );
    vec3 target = vec3(chooseWhite);
    float strength = clamp(vertexColor.a * ColorModulator.a, 0.0, 1.0);

    fragColor = vec4(mix(background, target, strength), 1.0);
}
