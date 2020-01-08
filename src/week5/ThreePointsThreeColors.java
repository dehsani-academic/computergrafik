package week5;

import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

import graphicslib3D.GLSLUtils;

public class ThreePointsThreeColors extends JFrame implements GLEventListener{
    // we need a rendering program for the GPU to process the shaders
    private int rendering_program;
    
    // this are used in order to define objects (in vao)
    // VAO = Vertex Array Object
    private int vao[] = new int[1];
    
    // we use VBOs to store information for each object
    // here we have 1 object, with 2 properties (position and color)
    // we need 1*2 = 2 VBOs
    private int vbo[] = new int[2];
    
    // in order to read shader files
    private GLSLUtils util = new GLSLUtils();

    
    private GLCanvas myCanvas;
    public ThreePointsThreeColors(){
        setTitle("Three Points drawn");
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
        
        // use the program
        gl.glUseProgram(rendering_program);
        
        //gl.glLineWidth(30.0f);
        gl.glPointSize(30.0f);
        
        // use the vbo[0] to feed position into shaders
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        // location = 0 in shader will read 3 points at a time from
        // the vbo[0] buffer
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0); // vbo[0] is now ready to go to location 0
        
        // now same with colors buffer
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        // location = 1 in shader will read 3 points at a time from
        // the vbo[1] buffer
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1); // vbo[1] is now ready to go to location 0

        
        gl.glEnable(GL_DEPTH_TEST);
        
        // be careful: we have THREE points!
        gl.glDrawArrays(GL_POINTS, 0, 3);
        }
    
    public static void main(String[] args)
    { 
        new ThreePointsThreeColors();
    }
    
    
    // CPU will run init first
    public void init(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now
        rendering_program = createShaderProgram();
        
        // we need vertex array objects for the info in shaders
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        
        setupVertices();
    }
    
    
    // we define our vertices with a method to be called by init
    private void setupVertices() {
    	GL4 gl = (GL4) GLContext.getCurrentGL();
    	
    	// we can put more points later
    	float[] point_position = {
    			0.5f, 0.0f, 0.0f,
    			-0.5f, 0.0f, 0.0f,
    			0.0f, 0.5f, 0.0f
    	};
    	
    	float[] point_colors = {
    			1.0f, 0.0f, 0.0f,
    			0.0f, 0.0f, 1.0f,
    			0.0f, 1.0f, 0.0f
    	};
    	
    	gl.glGenBuffers(vbo.length, vbo, 0); // generates the buffers
    	
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); // gets it ready
    	FloatBuffer ptsBuffer = Buffers.newDirectFloatBuffer(point_position); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, ptsBuffer.limit()*4, ptsBuffer, GL_STATIC_DRAW); 
    	
    	// now color information in vbo[1]
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]); // gets it ready
    	FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(point_colors); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, colorBuffer.limit()*4, colorBuffer, GL_STATIC_DRAW); 

    	
    }
    
    
    // We need to define the vertex (point) we will draw
    // and then feed it into the shaders 
    // we store the output as our rendering program in our init method
    public int createShaderProgram() {
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now

        // now we read from the shader files with GLSLUtils
        String vshaderSource[] = util.readShaderSource("vert_5a.shader");
        String fshaderSource[] = util.readShaderSource("frag_5a.shader");
        
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