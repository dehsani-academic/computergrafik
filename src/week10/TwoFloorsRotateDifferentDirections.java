package week10;


import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;

import com.jogamp.common.nio.Buffers;

public class TwoFloorsRotateDifferentDirections extends JFrame implements GLEventListener{
    // we need a rendering program for the GPU to process the shaders
    private int rendering_program;
    
    // this are used in order to define objects (in vao)
    // VAO = Vertex Array Object
    private int vao[] = new int[2];
    // we will also use a VBO (Vertex Buffer Object to store info 
    // such as position, color, etc., one buffer for each such attribute
    private int vbo[] = new int[6];
    
    // we will write the shaders in separate files (as is customary)
    private GLSLUtils util = new GLSLUtils();
    
    private GLCanvas myCanvas;
    
    private MatrixStack mvStack = new MatrixStack(20);
    private float cameraX, cameraY, cameraZ;
    
    
    public TwoFloorsRotateDifferentDirections(){
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
        
        FPSAnimator animator = new FPSAnimator(myCanvas, 50);
        animator.start();
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
        
        double rotateDeg = (double)(System.currentTimeMillis())/25.0;;
        mvStack.pushMatrix();
        mvStack.rotate(rotateDeg, 0, 1, 0);

        // copies matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
        
        // we need vertex array objects for the info in shaders
        gl.glGenVertexArrays(vao.length, vao, 0);
        
        
        gl.glBindVertexArray(vao[0]);
        
        // use vbo[0] to feed position info into shaders
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        // as before use vertexAttributePointers to point to the locations in the shaders
        // set the "location = 0" info in the shaders to the point info from vbo[0]
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        // now same with vbo[1]
        // set the "location = 1" info in the shaders to the point info from vbo[1] (colors)
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        
        // now draw:
        gl.glEnable(GL_DEPTH_TEST);
        // each object was described by 6 indices
        int numIndices = 6;
        // draw floor 
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[2]);
        gl.glDrawElements(GL_TRIANGLES, numIndices, GL_UNSIGNED_INT, 0);
        
        gl.glBindVertexArray(0); // just to be safe
        
        mvStack.popMatrix(); // now only 2 levels, second with translation (camera)
        
        
        mvStack.pushMatrix();
        mvStack.rotate(-rotateDeg, 0, 1, 0);
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
        
        
        // now draw the ceiling
        gl.glBindVertexArray(vao[1]);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        
        // now draw:
        gl.glEnable(GL_DEPTH_TEST);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[5]);
        gl.glDrawElements(GL_TRIANGLES, numIndices, GL_UNSIGNED_INT, 0);
        
        mvStack.popMatrix();
        mvStack.popMatrix();
        
    }
    
    public static void main(String[] args)
    { 
        new TwoFloorsRotateDifferentDirections();
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
    
    
    // CPU will run init first
    public void init(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now
        rendering_program = createShaderProgram();
        
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 2.0f;
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
        
        
        gl.glGenBuffers(vbo.length, vbo, 0); // generate the (six!) buffers

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); // bind the first buffer
        FloatBuffer floorPtsBuf = Buffers.newDirectFloatBuffer(floor_verts);
        gl.glBufferData(GL_ARRAY_BUFFER, floorPtsBuf.limit()*4, floorPtsBuf, GL_STATIC_DRAW); // add data to buffer     
        
        // now do the same to collect the color data
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer floorColorBuf = Buffers.newDirectFloatBuffer(floor_colors);
        gl.glBufferData(GL_ARRAY_BUFFER, floorColorBuf.limit()*4, floorColorBuf, GL_STATIC_DRAW);   

        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        IntBuffer squareIndicesBuf = Buffers.newDirectIntBuffer(square_indices);
        gl.glBufferData(GL_ARRAY_BUFFER, squareIndicesBuf.limit()*4, squareIndicesBuf, GL_STATIC_DRAW);  
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]); 
        FloatBuffer ceilingPtsBuf = Buffers.newDirectFloatBuffer(ceiling_verts);
        gl.glBufferData(GL_ARRAY_BUFFER, ceilingPtsBuf.limit()*4, ceilingPtsBuf, GL_STATIC_DRAW);     
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        FloatBuffer ceilingColorBuf = Buffers.newDirectFloatBuffer(ceiling_colors);
        gl.glBufferData(GL_ARRAY_BUFFER, ceilingColorBuf.limit()*4, ceilingColorBuf, GL_STATIC_DRAW);   

        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
        gl.glBufferData(GL_ARRAY_BUFFER, squareIndicesBuf.limit()*4, squareIndicesBuf, GL_STATIC_DRAW);  
        


    }
    
    // We need to define the vertex (point) we will draw
    // and then feed it into the shaders 
    // we store the output as our rendering program in our init method
    public int createShaderProgram() {
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now

        // we will now read from files (see function readShaderSource below)
        String vshaderSource[] = util.readShaderSource("vert_10a.shader");
        String fshaderSource[] = util.readShaderSource("frag_10a.shader");
        
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