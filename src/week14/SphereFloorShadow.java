package week14;



import graphicslib3D.*;
import graphicslib3D.light.*;
import graphicslib3D.shape.Sphere;

import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_ATTACHMENT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_COMPONENT32;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_LEQUAL;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINES;
import static com.jogamp.opengl.GL.GL_NONE;
import static com.jogamp.opengl.GL.GL_POINTS;
import static com.jogamp.opengl.GL.GL_POLYGON_OFFSET_FILL;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL2ES2.GL_COMPARE_REF_TO_TEXTURE;
import static com.jogamp.opengl.GL2ES2.GL_DEPTH_COMPONENT;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_FUNC;
import static com.jogamp.opengl.GL2ES2.GL_TEXTURE_COMPARE_MODE;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.common.nio.Buffers;

import static java.lang.System.out; 



public class SphereFloorShadow extends JFrame implements GLEventListener
{   
    private GLCanvas myCanvas;
    // we use two programs one for each pass
    private int rendering_program1, rendering_program2;

    // we have 1 object (the sphere)
    private int numberObjects = 2;
    private int numberPerObject = 3;
    private int vao[] = new int[numberObjects];
    // for the object we will 3 attributes: position, normal, indices
    // so need 1*3 = 3 buffers
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
    
    // shadow stuff
    private int scSizeX, scSizeY;
    private int [] shadow_tex = new int[1];
    private int [] shadow_buffer = new int[1];
    private Matrix3D lightView_matrix = new Matrix3D();
    private Matrix3D lightProj_matrix = new Matrix3D();
    private Matrix3D shadowMVP1 = new Matrix3D();
    private Matrix3D shadowMVP2 = new Matrix3D();
    private Matrix3D b = new Matrix3D();
    
    private Material sphereMaterial = Material.SILVER; 
    private DistantLight dl= new DistantLight ();
    private float [ ] globalAmbient = new float[ ] { 0.7f, 0.7f, 0.7f, 1.0f };
    
    private GLSLUtils util = new GLSLUtils();
    
    private MatrixStack mvStack = new MatrixStack(20);


    private Sphere mySphere = new Sphere(24);

    public SphereFloorShadow()
    {   setTitle("Directional light on sphere");
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
        
        FPSAnimator animator = new FPSAnimator(myCanvas, 30);
        animator.start();
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
    
    public void installLights(int rendering_program, Matrix3D viewMatrix) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        Vector3D directionVector = new Vector3D(0, -1, 0);
        float [] amb = new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; 
        float [] dif = new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; 
        float [] spec = new float[] { 1.0f, 1.0f, 1.0f, 1.0f }; 
        dl.setAmbient(amb); 
        dl.setDiffuse(dif); 
        dl.setSpecular(spec); 
        dl.setDirection(directionVector.mult(viewMatrix));
        
        // make an array as well to enter into the uniform variables...is there a better way?
        float [] directionArray = new float[] { (float) dl.getDirection().getX(), 
                (float) dl.getDirection().getY(), (float) dl.getDirection().getZ()}; 
        
        // set the current globalAmbient settings
        int globalAmbLoc = gl.glGetUniformLocation(rendering_program, "globalAmbient");
        gl.glUniform4fv(globalAmbLoc, 1, globalAmbient, 0);
        
        // get the locations of the light and material fields in the shader
        int ambLoc = gl.glGetUniformLocation(rendering_program, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(rendering_program, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(rendering_program, "light.specular");
        int dirLoc = gl.glGetUniformLocation(rendering_program, "light.direction");
        
        
        int materialAmbLoc = gl.glGetUniformLocation(rendering_program, "material.ambient");
        int materialDiffLoc = gl.glGetUniformLocation(rendering_program, "material.diffuse");
        int materialSpecLoc = gl.glGetUniformLocation(rendering_program, "material.specular");
        int materialShineLoc = gl.glGetUniformLocation(rendering_program, "material.shininess");
        
        // set the uniform light and material values in the shader
        gl.glUniform4fv(ambLoc, 1, amb, 0);
        gl.glUniform4fv(diffLoc, 1, dif, 0);
        gl.glUniform4fv(specLoc, 1, spec, 0);
        gl.glUniform3fv(dirLoc, 1, directionArray, 0);
        
        gl.glUniform4fv(materialAmbLoc, 1, sphereMaterial.getAmbient(), 0);
        gl.glUniform4fv(materialDiffLoc, 1, sphereMaterial.getDiffuse(), 0);
        gl.glUniform4fv(materialSpecLoc, 1, sphereMaterial.getSpecular(), 0);
        gl.glUniform1f(materialShineLoc, sphereMaterial.getShininess());
        
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
        
        
        gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
        gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);
   
        gl.glDrawBuffer(GL_NONE);
        gl.glEnable(GL_DEPTH_TEST);

        gl.glEnable(GL_POLYGON_OFFSET_FILL);    // for reducing
        gl.glPolygonOffset(2.0f, 4.0f);         //  shadow artifacts
        
        // pass one puts in the computer memory the depth location of the objects
        // from the point of view of the light
        passOne();
        
        gl.glDisable(GL_POLYGON_OFFSET_FILL);   // artifact reduction, continued
              
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
      
        gl.glDrawBuffer(GL_FRONT);
            
        passTwo();
       
    }
    
    public void passOne() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        gl.glUseProgram(rendering_program1);
        
        // need the directional vector to build the light matrix:
        Vector3D directionVector = new Vector3D(0, -1, 0);
        dl.setDirection(directionVector);
        // build the light’s P and V matrices to look-at the origin
        Matrix3D lightMatrix = new Matrix3D();
        Vector3D lightEye = new Vector3D();

        Vector3D dirVector = dl.getDirection().normalize();
        lightEye = center.minus(dirVector.mult(500)) ;

        // from the point of view of the light
        // this should be changed based on given light
        Vector3D lightUp = new Vector3D(1,0,0);
        lightView_matrix = lookAt(lightEye, center, lightUp);
        
        
        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        lightProj_matrix = perspective(0.4f, aspect, 0.1f, 1000.0f);  
        
        lightMatrix.concatenate(lightProj_matrix);
        lightMatrix.concatenate(lightView_matrix);

        shadowMVP1 = new Matrix3D(); // MVP stands for "model-view-projection"
        shadowMVP1.concatenate(lightMatrix);
        int shadow_location = gl.glGetUniformLocation(rendering_program1, "shadowMVP");
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP1.getFloatValues(), 0);
        
        // now draw
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        //gl.glDepthFunc(GL_EQUAL);
        drawObject(0);
        drawObject(1);
        
    }
    
    public void passTwo(){
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        gl.glUseProgram(rendering_program2);
        
        shadowMVP2.setToIdentity(); // not necessary 
        shadowMVP2.concatenate(b);
        shadowMVP2.concatenate(lightProj_matrix);
        shadowMVP2.concatenate(lightView_matrix);

        int mv_loc = gl.glGetUniformLocation(rendering_program2, "mv_matrix");
        int proj_loc = gl.glGetUniformLocation(rendering_program2, "proj_matrix");
        int shadow_location = gl.glGetUniformLocation(rendering_program2,  "shadowMVP");
        
        gl.glUniformMatrix4fv(shadow_location, 1, false, shadowMVP2.getFloatValues(), 0);

        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        Matrix3D pMat = perspective(45.0f, aspect, 0.1f, 1000.0f);
        gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);

        // first put the view matrix in the stack
        mvStack.pushMatrix();
        // get the lookAt matrix
        Matrix3D viewMatrix = lookAt(eye,center,up);
        // set up lights based on the current light's position 
        installLights(rendering_program2, viewMatrix); 
        mvStack.multMatrix(viewMatrix);
        mvStack.pushMatrix();
        
        // now the model matrix
        mvStack.translate(sphLocX, sphLocY, sphLocZ);
        
        
        // enter matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);

        
        // now draw
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL_DEPTH_TEST);
        gl.glEnable(GL_CULL_FACE);
        gl.glFrontFace(GL_CCW);
        //gl.glDepthFunc(GL_EQUAL);
        drawObject(0);
        drawObject(1);
        
        mvStack.popMatrix();
        mvStack.popMatrix();
        
    }

    

    public static void main(String[] args) {
        new SphereFloorShadow(); 
    }
    
    
    public void init(GLAutoDrawable drawable)
    {   GL4 gl = (GL4) GLContext.getCurrentGL();
    
    
        createShaderPrograms();
        setupShadowBuffers();
        
        // need this transformation to convert screen (-1 to +1) coordinates
        // to coordinates in a texture, which is 0 to 1
        // it is just a scale and a translation
        b.translate(.5, .5, .5);
        b.scale(.5, .5, .5);
        

        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 10.0f;
        
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
        
        // for the floor goes from -2.5 to 2.5
        float[] floor_positions = {
                -2.5f,  -2.0f, 2.5f, 
                -2.5f,  -2.0f, -2.5f,
                2.5f,  -2.0f, -2.5f,
                2.5f,  -2.0f, 2.5f
        };
        float[] floor_normals = {
                0.0f,  1.0f, 0.0f, 
                0.0f,  1.0f, 0.0f,
                0.0f,  1.0f, 0.0f,
                0.0f,  1.0f, 0.0f
        };
        int[] floor_indices = {
                0, 2, 1, 0, 3, 2
        };
        
        // init floor using initObject
        initObject(1, floor_positions, floor_normals, floor_indices, Types.TRIANGLES);
        

    }
    
    public void setupShadowBuffers()
    {   GL4 gl = (GL4) GLContext.getCurrentGL();
        scSizeX = myCanvas.getWidth();
        scSizeY = myCanvas.getHeight();
    
        gl.glGenFramebuffers(1, shadow_buffer, 0);
    
        gl.glGenTextures(1, shadow_tex, 0);
        gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
                        scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
    }
    
    private void createShaderPrograms()
    {   
        GL4 gl = (GL4) GLContext.getCurrentGL();


        String v1shaderSource[] = util.readShaderSource("vert1_14a.shader");
        String v2shaderSource[] = util.readShaderSource("vert2_14a.shader");
        String f2shaderSource[] = util.readShaderSource("frag2_14a.shader");

        int vShader1 = gl.glCreateShader(GL_VERTEX_SHADER);
        int vShader2 = gl.glCreateShader(GL_VERTEX_SHADER);
        int fShader2 = gl.glCreateShader(GL_FRAGMENT_SHADER);

        gl.glShaderSource(vShader1, v1shaderSource.length, v1shaderSource, null, 0);
        gl.glShaderSource(vShader2, v2shaderSource.length, v2shaderSource, null, 0);
        gl.glShaderSource(fShader2, f2shaderSource.length, f2shaderSource, null, 0);

        gl.glCompileShader(vShader1);
        gl.glCompileShader(vShader2);
        gl.glCompileShader(fShader2);

        rendering_program1 = gl.glCreateProgram();
        rendering_program2 = gl.glCreateProgram();

        gl.glAttachShader(rendering_program1, vShader1);
        gl.glAttachShader(rendering_program2, vShader2);
        gl.glAttachShader(rendering_program2, fShader2);

        gl.glLinkProgram(rendering_program1);
        gl.glLinkProgram(rendering_program2);
    }
    
    
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    public void dispose(GLAutoDrawable drawable) {}




}
