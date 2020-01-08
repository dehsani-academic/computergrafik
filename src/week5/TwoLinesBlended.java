package week5;

import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

import graphicslib3D.GLSLUtils;

public class TwoLinesBlended extends JFrame implements GLEventListener{
    // we need a rendering program for the GPU to process the shaders
    private int rendering_program;
    

    // two VAOs (2 objects); one for each line
    private int vao[] = new int[2];
    
    // we use VBOs to store information for each object
    // here we have 2 object2, with 2 properties (position and color)
    // we need (number of objects) * (number of properties for each) = 2 * 2 = 4 VBOs
    private int vbo[] = new int[4];
    
    // in order to read shader files
    private GLSLUtils util = new GLSLUtils();

    
    private GLCanvas myCanvas;
    public TwoLinesBlended(){
        setTitle("A blended line");
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
        
        gl.glLineWidth(30.0f);
        // gl.glPointSize(30.0f);
        
        
        // draw line 1 
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

        //gl.glEnable(GL_DEPTH_TEST);

        gl.glDrawArrays(GL_LINES, 0, 2);
        
        // draw line 2 
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0); 
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);

        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1); 

        gl.glEnable(GL_DEPTH_TEST);

        gl.glDrawArrays(GL_LINES, 0, 2);
        }
    
    public static void main(String[] args)
    { 
        new TwoLinesBlended();
    }
    
    
    // CPU will run init first
    public void init(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // so we can use gl commands now
        rendering_program = createShaderProgram();
        
        setupVertices();
    }
    
    
    // we define our vertices with a method to be called by init
    private void setupVertices() {
    	GL4 gl = (GL4) GLContext.getCurrentGL();
    	
    	// this is for the first line
    	float[] point_position_1 = {
    			-0.5f, 0.0f, 0.0f,
    			 0.0f, 0.0f, 0.0f
    	};
    	
    	// this is for the second line
    	float[] point_position_2 = {
    			 0.0f, 0.0f, 0.0f,
    			 0.5f, 0.0f, 0.0f
    	};
    	
    	// this is for the colors of the first line
    	float[] point_color_1 = {
    			 1.0f, 0.0f, 0.0f,
    			 0.0f, 0.0f, 1.0f
    	};
    	// this is for the colors of the first line
    	float[] point_color_2 = {
    			 0.0f, 0.0f, 1.0f,
    			 0.0f, 1.0f, 0.0f
    	};
    	
        // this was from init earlier; here we need each vao to be connected with
    	// the corresponding vbos
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
    	
    	gl.glGenBuffers(vbo.length, vbo, 0); // generates the buffers
    	
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); // gets it ready
    	FloatBuffer pts1Buffer = Buffers.newDirectFloatBuffer(point_position_1); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, pts1Buffer.limit()*4, pts1Buffer, GL_STATIC_DRAW); 
    	
    	// now color information in vbo[1]
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]); // gets it ready
    	FloatBuffer color1Buffer = Buffers.newDirectFloatBuffer(point_color_1); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, color1Buffer.limit()*4, color1Buffer, GL_STATIC_DRAW); 

    	// good programming practice:
    	// make sure we don't modify the info for vao[0]
    	gl.glBindVertexArray(0);
    	
    	// repeat for the second line
        gl.glBindVertexArray(vao[1]);
    	
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]); // gets it ready
    	FloatBuffer pts2Buffer = Buffers.newDirectFloatBuffer(point_position_2); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, pts2Buffer.limit()*4, pts2Buffer, GL_STATIC_DRAW); 
    	
    	gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]); // gets it ready
    	FloatBuffer color2Buffer = Buffers.newDirectFloatBuffer(point_color_2); // collects data
    	// add data to buffer
    	gl.glBufferData(GL_ARRAY_BUFFER, color2Buffer.limit()*4, color2Buffer, GL_STATIC_DRAW); 

    	
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