#version 430

layout (location=0) in vec3 position;
layout (location=1) in vec3 normal;

out vec3 varyingNormal; // eye-space vertex normal
out vec3 varyingLightDir; // vector pointing to the light
out vec3 varyingVertPos; // vertex position in eye-space
out vec3 varyingHalfVector;
out vec4 shadow_coord;

uniform vec4 globalAmbient;

struct DirlLight
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 direction;
};

struct Material
{ 
	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

uniform DirlLight light;
uniform Material material;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

uniform mat4 shadowMVP;
layout (binding=0) uniform sampler2DShadow shadowTex;


void main(void)
{	
	// output vertex position, light direction, and normal to the rasterizer for interpolation
	varyingVertPos=(mv_matrix * vec4(position,1.0)).xyz;
	varyingLightDir = -light.direction;
	mat4 normMatrix = mat4(transpose(inverse(mv_matrix)));
	varyingNormal=(normMatrix * vec4(normal,1.0)).xyz;
	varyingHalfVector = normalize( normalize(varyingLightDir) + normalize(-varyingVertPos) );
	
	shadow_coord = shadowMVP * vec4(position,1.0);
	
	gl_Position=proj_matrix * mv_matrix * vec4(position,1.0);

} 


