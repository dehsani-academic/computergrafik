package ua4;



import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_INT;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL4.*;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import graphicslib3D.GLSLUtils;
import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;



public class TwoFloorsAnimateAufgabe1 extends JFrame implements GLEventListener{
    private int rendering_program;
    // we have 2 objects (2 floors)
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
    // see https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/constant-values.html#com.jogamp.opengl.GL.GL_TRIANGLES
    // for the field values of GL_TRIANGLES etc.
    private int PrimType[] = new int[numberObjects];
    private int NumIndices[] = new int[numberObjects];
    
    
    private GLSLUtils util = new GLSLUtils();
    
    private GLCanvas myCanvas;
    private float cameraX, cameraY, cameraZ;
    private float floorX, floorY, floorZ;
    private float ceilX, ceilY, ceilZ;
    
    private MatrixStack mvStack = new MatrixStack(20);
    
    public TwoFloorsAnimateAufgabe1(){
        setTitle("Week 6 - draw 2 floors with auto draw");
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
        
        // create an animator
        // frames per second 50
        FPSAnimator animator = new FPSAnimator(myCanvas, 50);
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
                PrimType[objectInt] = GL_POINTS;
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
    
    
    // init finishes then display is called
    public void display(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL();
        
        
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        
        // not needed if bkg is black
        float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
        FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);

        
        gl.glUseProgram(rendering_program); // loads program from init with the two shaders

        // to be used later to put in uniform variables
        // see uniform variables in fragment shader
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
        
        mvStack.pushMatrix();
        mvStack.translate(ceilX, ceilY, ceilZ);
        double rotateDeg = (double) System.currentTimeMillis()/10.0f;
        mvStack.rotate(rotateDeg, 0, 1, 0);

        
        
        // enter matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);

        
        // now draw
        gl.glEnable(GL_DEPTH_TEST);
        drawObject(0);
        
        mvStack.popMatrix();
        
        mvStack.pushMatrix();
        mvStack.translate(floorX, floorY, floorZ);
        mvStack.rotate(-rotateDeg, 0, 1, 0);

        
        // enter matrices into uniform variables
        gl.glUniformMatrix4fv(mv_loc, 1, false, mvStack.peek().getFloatValues(), 0);
        
        // now draw
        gl.glEnable(GL_DEPTH_TEST);
        drawObject(1);
        
        mvStack.popMatrix(); // gets rid of mv for floor
        mvStack.popMatrix(); // gets of view matrix
        



        



      
        // draw floor 2
        drawObject(1);

    }
    
    public static void main(String[ ] args){
        new TwoFloorsAnimateAufgabe1();
    }
    
    // first init is called
    public void init(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL();
        rendering_program = createShaderProgram();
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 2.0f;
        
        floorX = 0.0f; floorY = -0.5f; floorZ = 0.0f;
        ceilX = 0.0f; ceilY = 0.5f; ceilZ = 0.0f;
        
        // these were earlier in the display method
        // but we need them here before we call initObject
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glGenBuffers(vbo.length, vbo, 0);
        
        // the vertices used to be in setupVertices
        // another common practice is to declare the vertices as
        // global class attributes
        float[] floor_verts = {
                0.5f,  0.0f, -0.5f, 
                -0.5f,  0.0f, -0.5f,
                -0.5f,  0.0f, 0.5f, 
                0.5f,  0.0f, 0.5f
        };
        
        float[] floor_col = {
                0.0f,  0.0f, 1.0f, 
                0.0f,  0.0f, 1.0f,
                0.0f,  0.0f, 1.0f, 
                0.0f,  0.0f, 1.0f
        };

        int[] floor_indices= { 
                0, 1, 2, 0, 2, 3
        }; 
        
        float[] floor2_col = {
                1.0f,  0.0f, 0.0f, 
                1.0f,  0.0f, 0.0f,
                1.0f,  0.0f, 0.0f, 
                1.0f,  0.0f, 0.0f
        };


        
        // init floor using 
        // initObject(int objectInt, float[] vertices, float[] colors, int[] indices,
        //              Types objectType)
        initObject(0, floor_verts, floor_col, floor_indices, Types.TRIANGLES);
        
        // init floor2
        initObject(1, floor_verts, floor2_col, floor_indices, Types.TRIANGLES);

    }
    
    

    
    private int createShaderProgram()
    {   GL4 gl = (GL4) GLContext.getCurrentGL();

        // we will now read from files (see function readShaderSource below)
        String vshaderSource[] = util.readShaderSource("vert_10a.shader");
        String fshaderSource[] = util.readShaderSource("frag_10a.shader");

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

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int
    height) {
        
    }
    
    public void dispose(GLAutoDrawable drawable){
        
    }
    
}