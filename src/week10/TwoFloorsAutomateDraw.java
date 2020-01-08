package week10;


import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

import com.jogamp.common.nio.Buffers;

public class TwoFloorsAutomateDraw extends JFrame implements GLEventListener{
    // we need a rendering program for the GPU to process the shaders
    private int rendering_program;
    
    private int numberObjects = 2;
    private int numberPerObject = 3;
    private int vao[] = new int[numberObjects];
    // for each object we will 3 attributes: position, color, indices
    // so need 3*2 = 6 buffers
    private int vbo[] = new int[numberObjects * numberPerObject];
    
    // for each object we can assign a type from the listed GL Primitives
    enum Types {
        TRIANGLES, POINTS, LINES
    }
   
    private int PrimType[] = new int[numberObjects];
    private int NumIndices[] = new int[numberObjects];
    
    // we will write the shaders in separate files (as is customary)
    private GLSLUtils util = new GLSLUtils();
    
    private GLCanvas myCanvas;
    
    private MatrixStack mvStack = new MatrixStack(20);
    private float cameraX, cameraY, cameraZ;
   
    
    
    public TwoFloorsAutomateDraw(){
        setTitle("two floors");
        setSize(600, 400);
        setLocation(200, 200);
        
        // this is new (to ensure compatibility with other OS)
        GLProfile prof = GLProfile.get(GLProfile.GL4);
        GLCapabilities capable = new GLCapabilities(prof);
        
        // note myCanvas now has the capabilities of GL4
        myCanvas = new GLCanvas(capable);
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        setVisible(true);
    }
    
    
    // where we place code that draws to the GLCanvas
    public void display(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // note: GL4 is a Java interface to the OpenGL functions
        // any OpenGL function described in the OpenGL documentation can be called
        // from JOGL by preceding it with the name of the GL4 object (here gl)
        
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        
        // not needed here, but good practice
        // bkg is automatically black, but we can change it below
        float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
        FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
        
        // use the program
        gl.glUseProgram(rendering_program);
        
        int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
        int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");
        
        // creates perspective matrix, with fovy=60
        // aspect corresponds to screen dimensions
        // alternatively, we could put the perspective matrix in init()
        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        Matrix3D pMat = perspective(60.0f, aspect, 0.1f, 1000.0f);
        gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);
        
        
        // here we use the stack to build mvMat
        // first view matrix in stack
        mvStack.pushMatrix();
        mvStack.translate(-cameraX, -cameraY, -cameraZ);   

        // copies matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
        
        
        // now draw:
        gl.glEnable(GL_DEPTH_TEST);
 
        // draw floor 
        drawObject(0);

        // draw ceiling
        drawObject(1);

        
    }
    
    public static void main(String[] args)
    { 
        new TwoFloorsAutomateDraw();
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
    
    /**
     * @param objectInt: the object index as used by the VAO (starts at 0...)
     * @param vertices: the vertices used to draw the object
     * @param colors: the color of the object
     * @param indices: the indices of the input vertices in order used to draw
     * 
     * @return nothing; sets up the initialization to draw: binding buffers and setting up 
     *      vertexAttributePointers to be used by shaders
     */
    public void initObject(int objectInt, float[] vertices, float[] colors, int[] indices,
            Types objectType) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        int offset = objectInt * numberPerObject;
        
        // bind the VAOs and VBOs
        // these were in the setupVertices earlier
        gl.glBindVertexArray(vao[objectInt]);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[offset]);
        FloatBuffer ptsBuf = Buffers.newDirectFloatBuffer(vertices);
        gl.glBufferData(GL_ARRAY_BUFFER, ptsBuf.limit()*4, ptsBuf, GL_STATIC_DRAW);  
        
        // put also the vertexAttributePointers here 
        // before they were separately in the display method
        // set the "location = 0" info in the shaders to the vertices info from vbo[offset]
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[offset+1]);
        FloatBuffer colorBuf = Buffers.newDirectFloatBuffer(colors);
        gl.glBufferData(GL_ARRAY_BUFFER, colorBuf.limit()*4, colorBuf, GL_STATIC_DRAW);  
        // set the "location = 1" info in the shaders to the colors info from vbo[offset+1]
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
                PrimType[objectInt] =GL_POINTS;
                break;
            case LINES:
                PrimType[objectInt] = GL_LINES;
                break;
        }

        NumIndices[objectInt] = indices.length;
        
        // not necessary but safe practice:
        // since there is another object, the next line will
        // prevent further modification of the VAO:
        gl.glBindVertexArray(0);
    }
    
    public void drawObject(int objectInt) {
        int offset = objectInt * numberPerObject;
        
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glBindVertexArray(vao[objectInt]); // need this because we disabled it at end of initObject method
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[offset+2]);
        gl.glDrawElements(PrimType[objectInt], NumIndices[objectInt], GL_UNSIGNED_INT, 0);
    }
    
    
    
    // CPU will run init first
    public void init(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now
        rendering_program = createShaderProgram();
        
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 1.0f;
        setupVertices();
    }
    
    // We define our vertices with a program which is then called by init
    private void setupVertices()
    {   GL4 gl = (GL4) GLContext.getCurrentGL();
        
        float[] floor_verts = {
                0.5f,  -0.5f, -0.5f, 
                -0.5f,  -0.5f, -0.5f,
                -0.5f,  -0.5f, 0.5f, 
                0.5f,  -0.5f, 0.5f
        };
             
        float[] floor_colors = {
                1.0f,  0.0f, 0.0f, 
                1.0f, 0.0f, 0.0f,
                1.0f,  0.0f, 0.0f,
                1.0f, 0.0f, 0.0f
        };
        
        float[] ceiling_verts = {
                0.5f,  0.5f, -0.5f, 
                -0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f, 0.5f, 
                0.5f,  0.5f, 0.5f
        };
             
        float[] ceiling_colors = {
                0.0f,  0.0f, 1.0f, 
                0.0f, 0.0f, 1.0f,
                0.0f,  0.0f, 1.0f,
                0.0f, 0.0f, 1.0f
        };
        
        int[] square_indices= { 
                0, 1, 2, 0, 2, 3
        }; 
        
        
        // we need these here before we call initObject
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glGenBuffers(vbo.length, vbo, 0);
        
        // init floor using 
        // initObject(int objectInt, float[] vertices, float[] colors, int[] indices,
        //              Types objectType)
        initObject(0, floor_verts, floor_colors, square_indices, Types.TRIANGLES);
        
        // init ceiling
        initObject(1, ceiling_verts, ceiling_colors, square_indices, Types.TRIANGLES);

        


    }
    
    // We need to define the vertex (point) we will draw
    // and then feed it into the shaders 
    // we store the output as our rendering program in our init method
    public int createShaderProgram() {
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now

        // we will now read from files (see function readShaderSource below)
        String vshaderSource[] = util.readShaderSource("vert_9a.shader");
        String fshaderSource[] = util.readShaderSource("frag_9a.shader");
        
        // This next part will stay the same for many of our programs
        // create shader, read the shader, and compile
        int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
        gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
        gl.glCompileShader(vShader);
        
        // create fragment shader, read the shader, and compile
        int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
        gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);
        gl.glCompileShader(fShader);
        
        // create rendering program
        // create empty program, attach shaders, then return
        
        int vfprogram = gl.glCreateProgram();
        gl.glAttachShader(vfprogram, vShader);
        gl.glAttachShader(vfprogram, fShader);
        gl.glLinkProgram(vfprogram); // ensures compatibility
        
        
        gl.glDeleteShader(vShader);
        gl.glDeleteShader(fShader);
        
        return vfprogram;
        
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int
    height) {
        
    }
    
    public void dispose(GLAutoDrawable drawable){
        
    }
    
}