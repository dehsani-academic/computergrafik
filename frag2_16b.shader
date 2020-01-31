#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec4 shadow_coord;

out vec4 fragColor;

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

const int numPtLights = 2;
uniform vec4 pLightPos[numPtLights];
uniform vec4 pLightCol[numPtLights];
uniform vec3 pLightAtt;

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
				+ light.ambient * material.ambient;
				
	if (inShadow != 0.0)
	{	fragColor += light.diffuse * material.diffuse * max(cosTheta,0.0)
				+ light.specular * material.specular
				* pow(max(cosPhi,0.0), material.shininess);
	}
	
	// get color contribution from pt lights
	for (int i = 0; i < numPtLights ; i++){
	
		vec4 currentPos = pLightPos[i];
		vec3 currentL = currentPos.xyz/currentPos.w - varyingVertPos;
		vec3 currentH = normalize( normalize(currentL) + normalize(-varyingVertPos) );
	
		cosTheta = dot(currentL,N);
		cosPhi = dot(currentH,N);

	
		
		vec3 vectorD = currentPos.xyz/currentPos.w - varyingVertPos;
		float d = length(vectorD);
		float currentAF = 1/(pLightAtt[0] + pLightAtt[1]*d + pLightAtt[2]*d*d);
	
		
		fragColor += currentAF * pLightCol[i] * material.ambient;
		fragColor += currentAF * pLightCol[i] * material.diffuse * max(cosTheta,0.0);
		fragColor += currentAF * pLightCol[i] * material.specular * pow(max(cosPhi,0.0), material.shininess);
		
		
	}
	
}




