#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in float attenuationFactor;

in vec4 shadow_coord;

out vec4 fragColor;

uniform vec4 globalAmbient;

struct PositionalLight
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
	vec3 attenuation;
};


struct Material
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

uniform PositionalLight light;
uniform Material material;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

uniform mat4 shadowMVP;
layout (binding=0) uniform sampler2DShadow shadowTex;


void main(void)
{ 
	// normalize the light, normal, and view vectors:
	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-varyingVertPos);
	vec3 H = varyingHalfVector;
	
	// get the angle between the light and surface normal:
	float cosTheta = dot(L,N);
	
	// get angle between the normal and the halfway vector
	float cosPhi = dot(H,N);
	
	float inShadow = textureProj(shadowTex, shadow_coord);
	
	fragColor = globalAmbient * material.ambient
				+ attenuationFactor * light.ambient * material.ambient;
				
	if (inShadow != 0.0)
	{	fragColor += attenuationFactor * light.diffuse * material.diffuse * max(cosTheta,0.0)
				+ attenuationFactor * light.specular * material.specular * pow(max(cosPhi,0.0), material.shininess);
	}
	
}




