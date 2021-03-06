#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;

in float attenuationFactor;

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
	
	vec3 ambient = ((globalAmbient * material.ambient) + attenuationFactor *(light.ambient * material.ambient)).xyz;
	vec3 diffuse = attenuationFactor * light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
	vec3 specular = attenuationFactor * light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess);
	
	fragColor =  vec4((ambient + diffuse + specular), 1.0);
}




