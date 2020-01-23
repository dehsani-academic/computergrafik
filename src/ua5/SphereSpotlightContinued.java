package ua5;



import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.shape.Sphere;

import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;



public class SphereSpotlightContinued extends JFrame implements GLEventListener
{   private GLCanvas myCanvas;
    private int rendering_program;

    // we have 1 object (the sphere)
    private int numberObjects = 1;
    private int numberPerObject = 4;
    private int vao[] = new int[numberObjects];
    // for the object we will 4 attributes: position, normal, color, indices
    // so need 1*4 = 4 buffers
    // we won't use the normals in this program
    // but they will be needed for lighting later
    private int vbo[] = new int[numberObjects * numberPerObject];
    
    // for each object we can assign a type from the listed GL Primitives
    enum Types {
        TRIANGLES, POINTS, LINES
    }
    
    private int PrimType[] = new int[numberObjects];
    private int NumIndices[] = new int[numberObjects];
    
    private float cameraX, cameraY, cameraZ;
    private float sphLocX, sphLocY, sphLocZ; // where the sphere is located (for model matrix)
    
    private Vector3D up;
    private Vector3D eye;
    private Vector3D center;
    
    private Material sphereMaterial = Material.SILVER; 
    SpotLight sl = new SpotLight(); 
    private float [ ] globalAmbient = new float[ ] { 0.7f, 0.7f, 0.7f, 1.0f }; 
    
    private GLSLUtils util = new GLSLUtils();
    
    private MatrixStack mvStack = new MatrixStack(20);


    private Sphere mySphere = new Sphere(24);

    public SphereSpotlightContinued()
    {   setTitle("basic sphere");
        setSize(600, 400);
        setLocation(200, 200);
    
        // this is new (to ensure compatibility with other OS)
        GLProfile prof = GLProfile.get(GLProfile.GL4);
        GLCapabilities capable = new GLCapabilities(prof);
        // note myCanvas now has the capabilities of GL4
        myCanvas = new GLCanvas(capable);
        
        myCanvas.addGLEventListener(this);
        this.getContentPane().add(myCanvas);
        setVisible(true);
    }



    private Matrix3D perspective(float fovy, float aspect, float n, float f)
    {   float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
        float A = q / aspect;
        float B = (n + f) / (n - f);
        float C = (2.0f * n * f) / (n - f);
        Matrix3D r = new Matrix3D();
        r.setElementAt(0,0,A);
        r.setElementAt(1,1,q);
        r.setElementAt(2,2,B);
        r.setElementAt(3,2,-1.0f);
        r.setElementAt(2,3,C);
        r.setElementAt(3,3,0.0f);
        return r;
    }
    
    private Matrix3D lookAt(Vector3D eye, Vector3D center, Vector3D up)
    {   
        Vector3D a = eye.minus(center);
        Vector3D w = a.normalize();
        
        Vector3D u = up.cross(w);
        u = u.normalize();
        
        Vector3D v = w.cross(u);
        
        Matrix3D m = new Matrix3D();
        m.setRow(0, u);
        m.setRow(1, v);
        m.setRow(2, w);
        
        // for fourth column:
        double first = - u.dot(eye);
        double second = - v.dot(eye);
        double third = - w.dot(eye);
        Vector3D fourthColumn = new Vector3D(first,second,third,1);
        m.setCol(3, fourthColumn);
        
        return m;
    }
    

    
    /**
     * @param objectInt: the object index as used by the VAO (starts at 0...)
     * @param vertices: the vertices used to draw the object
     * @param normals: the normal vectors, pointing outward from surface
     * @param colors: the color of the object
     * @param indices: the indices of the input vertices in order used to draw
     * 
     * @return nothing; sets up the initialization to draw: binding buffers and setting up 
     *      vertexAttributePointers to be used by shaders
     */  
    public void initObject(int objectInt, float[] vertices, float[] normals, int[] indices,
            Types objectType) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        int offset = objectInt * numberPerObject;
        
        // bind the VAOs and VBOs
        gl.glBindVertexArray(vao[objectInt]);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[offset]);
        FloatBuffer ptsBuf = Buffers.newDirectFloatBuffer(vertices);
        gl.glBufferData(GL_ARRAY_BUFFER, ptsBuf.limit()*4, ptsBuf, GL_STATIC_DRAW);  
        
        // set the "location = 0" info in the shaders to the vertices info from vbo[offset]
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[offset+1]);
        FloatBuffer normalBuf = Buffers.newDirectFloatBuffer(normals);
        gl.glBufferData(GL_ARRAY_BUFFER, normalBuf.limit()*4, normalBuf, GL_STATIC_DRAW);  
        // set the "location = 1" info in the shaders to the normal info from vbo[offset+1]
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[offset+2]);
        IntBuffer indBuf = Buffers.newDirectIntBuffer(indices);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indBuf.limit()*4, indBuf, GL_STATIC_DRAW);
        

        
        // get values of the objectType
        switch (objectType) {
            case TRIANGLES:
                PrimType[objectInt] = GL_TRIANGLES;
                break; // dont forget this
            case POINTS:
                PrimType[objectInt] = GL_POINTS;
                break;
            case LINES:
                PrimType[objectInt] = GL_LINES;
                break;
        }

        NumIndices[objectInt] = indices.length;
        
        // safe practice:
        gl.glBindVertexArray(0);
    }
    
    public void installLights(Matrix3D viewMatrix) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        Point3D lightLoc = new Point3D(0.0f, 0.0f, 2.0f); 
        Vector3D directionVector = new Vector3D(0, 0, -1, 0); 
        float [ ] amb = new float[ ] { 1.0f, 0.0f, 0.0f, 1.0f }; 
        float [ ] dif = new float[ ] { 1.0f, 0.0f, 0.0f, 1.0f }; 
        float [ ] spec = new float[ ] { 1.0f, 0.0f, 0.0f, 1.0f }; 
        sl.setAmbient(amb); 
        sl.setDiffuse(dif); 
        sl.setSpecular(spec); 
        sl.setPosition(lightLoc); 
        Matrix3D vectorMat = viewMatrix.inverse().transpose();
        sl.setDirection(directionVector.mult(vectorMat)); 
        sl.setCutoffAngle(25.0f); // how wide the spotlight shines 
        sl.setFalloffExponent(2.0f); // how fast the intensity falls for rays away from center of spotlight 
        float cosCutoff = (float) Math.cos(((sl.getCutoffAngle())*Math.PI)/180.0);

        // see the attenuation formula for a point source light
        sl.setConstantAtt(1.0f); // k_c 
        sl.setLinearAtt(0.1f); // k_l 
        sl.setQuadraticAtt(0.1f); // k_q 
        
        // turn into arrays (better method?)
        float[] attenuationArray = new float[] {
                sl.getConstantAtt(), sl.getLinearAtt(), sl.getQuadraticAtt()
        };
        float [] directionArray = new float[] { (float) sl.getDirection().getX(), 
                (float) sl.getDirection().getY(), (float) sl.getDirection().getZ()}; 

        // convert light’s position to view space, and save it in a float array
        Point3D lightP = sl.getPosition();
        Point3D lightPv = lightP.mult(viewMatrix);
        float [] viewspaceLightPos = new float[] { (float) lightPv.getX(), 
                (float) lightPv.getY(), (float) lightPv.getZ() };

        
        // set the current globalAmbient settings
        int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");
        gl.glProgramUniform4fv(rendering_program, globalAmbLoc, 1, globalAmbient, 0);
        
        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
        int posLoc = gl.glGetUniformLocation(rendering_program, "light.position");
        int materialAmbLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
        int materialDiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
        int materialSpecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
        int materialShineLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");
        
        // for attenuation, add as vec3 = [kc, kl, kq]
        int attLoc = gl.glGetUniformLocation(rendering_program, "light.attenuation");
        // specific to spotlights:
        int spotDirLoc = gl.glGetUniformLocation(rendering_program, "light.spotDirection");
        int cutoffLoc = gl.glGetUniformLocation(rendering_program, "light.spotCosCutoff");
        int expLoc = gl.glGetUniformLocation(rendering_program, "light.spotExponent");
        
        
        // set the uniform light and material values in the shader
        gl.glUniform4fv(ambLoc, 1, sl.getAmbient(), 0);
        gl.glUniform4fv(diffLoc, 1, sl.getDiffuse(), 0);
        gl.glUniform4fv(specLoc, 1, sl.getSpecular(), 0);
        gl.glUniform3fv(posLoc, 1, viewspaceLightPos, 0);
        gl.glUniform4fv(materialAmbLoc, 1, sphereMaterial.getAmbient(), 0);
        gl.glUniform4fv(materialDiffLoc, 1, sphereMaterial.getDiffuse(), 0);
        gl.glUniform4fv(materialSpecLoc, 1, sphereMaterial.getSpecular(), 0);
        gl.glUniform1f(materialShineLoc, sphereMaterial.getShininess());
        
        gl.glUniform3fv(attLoc, 1, attenuationArray, 0);
        gl.glUniform3fv(spotDirLoc, 1, directionArray, 0);
        gl.glUniform1f(cutoffLoc, cosCutoff);
        gl.glUniform1f(expLoc, sl.getFalloffExponent());
        
    }
    
    public void drawObject(int objectInt) {
        int offset = objectInt * numberPerObject;
        
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glBindVertexArray(vao[objectInt]); // need this because we disabled it at end of initObject method
        //gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[offset+2]);
        gl.glDrawElements(PrimType[objectInt], NumIndices[objectInt], GL_UNSIGNED_INT, 0);
    }
    
    
    public void display(GLAutoDrawable drawable)
    {   GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        
        // not needed if bkg is black
        float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
        FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);

        gl.glUseProgram(rendering_program);

        int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
        int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        Matrix3D pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);
        gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);

        // first put the view matrix in the stack
        mvStack.pushMatrix();
        // get the lookAt matrix
        Matrix3D viewMatrix = lookAt(eye,center,up);
        installLights(viewMatrix); 
        mvStack.multMatrix(viewMatrix);
        mvStack.pushMatrix();
        
        // now the model matrix
        mvStack.translate(sphLocX, sphLocY, sphLocZ);
        
        // enter matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);


        
        // now draw
        gl.glEnable(GL_DEPTH_TEST);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        drawObject(0);
        
        mvStack.popMatrix();
        mvStack.popMatrix();
       
    }

    

    public static void main(String[] args) {
        new SphereSpotlightContinued(); 
    }
    
    
    public void init(GLAutoDrawable drawable)
    {   GL4 gl = (GL4) GLContext.getCurrentGL();
        rendering_program = createShaderProgram();

        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 5.0f;
        
        eye = new Vector3D(cameraX,cameraY,cameraZ);
        center = new Vector3D(0,0,0);
        up = new Vector3D(0,1,0);
        
        sphLocX = 0.0f; sphLocY = 0.0f; sphLocZ = 0.0f;
        
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glGenBuffers(vbo.length, vbo, 0);

        
        Vertex3D[] vertices = mySphere.getVertices(); // note the use of Vertex3D class
        int[] indices = mySphere.getIndices();
        
        
        float[] positions = new float[vertices.length*3];
        float[] normals = new float[vertices.length*3];
        
        for (int i=0; i < vertices.length; i++) {
            positions[i*3] = (float) vertices[i].getX();
            positions[i*3+1] = (float) vertices[i].getY();
            positions[i*3+2] = (float) vertices[i].getZ();
            normals[i*3] = (float) vertices[i].getNormalX();
            normals[i*3+1]= (float)vertices[i].getNormalY();
            normals[i*3+2]=(float) vertices[i].getNormalZ();
            
        }
        
        
        
        
        // init sphere using initObject
        initObject(0, positions, normals, indices, Types.TRIANGLES);
        

    }
    
    private int createShaderProgram(){    
        GL4 gl = (GL4) GLContext.getCurrentGL();
    
        // we will now read from files (see function readShaderSource below)
        String vshaderSource[] = util.readShaderSource("vert_14b.shader");
        String fshaderSource[] = util.readShaderSource("frag_14b.shader");
    
        int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
        int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
    
        gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
        gl.glCompileShader(vShader);
    
        gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
        gl.glCompileShader(fShader);
    
        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        gl.glLinkProgram(vfprogram);
    
        gl.glDeleteShader(vShader);
        gl.glDeleteShader(fShader);
        return vfprogram;
    }
    
    
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void dispose(GLAutoDrawable drawable) {}




}
