#version 430

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;

out vec3 varyingNormal; // eye-space vertex normal
out vec3 varyingLightDir; // vector pointing to the light
out vec3 varyingVertPos; // vertex position in eye-space
out vec3 varyingHalfVector;
out float attenuationFactor;

uniform vec4 globalAmbient;

struct SpotLight
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
	vec3 spotDirection;
	float spotCosCutoff;
	float spotExponent;
	vec3 attenuation;
};

struct Material
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

uniform SpotLight light;
uniform Material material;


uniform mat4 mv_matrix;
uniform mat4 proj_matrix;


void main(void)
{	
	// output vertex position, light position, and normal to the rasterizer for interpolation
	varyingVertPos=(mv_matrix * vec4(position,1.0)).xyz;
	varyingLightDir = light.position - varyingVertPos;
	mat4 normMatrix = mat4(transpose(inverse(mv_matrix)));
	varyingNormal=(normMatrix * vec4(normal,1.0)).xyz;
	varyingHalfVector = normalize( normalize(varyingLightDir) + normalize(-varyingVertPos) );
	
	vec3 vectorD = light.position - varyingVertPos;
	float d = length(vectorD);
	attenuationFactor = 1/(light.attenuation[0] + light.attenuation[1]*d + light.attenuation[2]*d*d);
	
    // See if point on surface is inside cone of illumination
	float spotDot = dot(-normalize(vectorD), normalize(light.spotDirection)) ;
	float spotAttenuation = 1.0f;

    if (spotDot < light.spotCosCutoff){
        spotAttenuation = 0.0; // light adds no contribution
    }
    else {
        spotAttenuation = pow(spotDot, light.spotExponent);
    }

    // Combine the spotlight and distance attenuation.
    attenuationFactor *= spotAttenuation;
	
	gl_Position=proj_matrix * mv_matrix * vec4(position,1.0);

} 


