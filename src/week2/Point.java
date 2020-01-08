package week2;


import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

public class Point extends JFrame implements GLEventListener{
    private GLCanvas myCanvas;
    
    // this will be used for the GPU to process the shaders
    private int rendering_program;
    
    // in order to define objects
    // VAO = Vertex Array Object
    private int vao[] = new int[1];
    
    public Point(){
        setTitle("Point Program");
        setSize(600, 400);
        setLocation(200, 200);
        
        // this is to ensure compatibilty with Mac or Linux
        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities capable = new GLCapabilities(profile);
        
        // tell the canvas it's capable
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
        
        
        float bkg[] = { 0.0f, 0.0f, 0.0f, 1.0f };
        
        // load buffer with color
        FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
        
        // use the created rendering_program
        gl.glUseProgram(rendering_program);
        
        
        
        // fill the display buffer (GLenum: GL_COLOR buffer, which buffer, last variable is a pointer in C, here a buffer)
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
        
        gl.glPointSize(50.0f);
        gl.glDrawArrays(GL_POINTS, 0, 1);
        
        }
    
	public static void main(String[] args)
	{ 
		new Point();
	}
    
    // CPU will read init method first
    public void init(GLAutoDrawable drawable){
    	GL4 gl = (GL4) GLContext.getCurrentGL();
    	// setup the rendering_program
    	rendering_program = createShaderProgram();
    	
    	// we need the VAOs for the info in the shaders
    	gl.glGenVertexArrays(vao.length, vao, 0);
    	gl.glBindVertexArray(vao[0]);
    }
    
    // this method will be called to create the rendering_program
    // the variable rendering_program will be set to what is returned 
    public int createShaderProgram() {
    	GL4 gl = (GL4) GLContext.getCurrentGL();  // in order to use OpenGL commands
    	
    	
    	// Warning: this next part is stupid
    	// normally shaders are in text files
    	
    	String vshaderSource[] =
    		{
    				"version #430 core \n",
    				"void main(void) \n",
    				"{gl_position = vec4(0.0, 0.0, 0.0, 1.0);} \n",	
    		};
    	
    	String fshaderSource[] =
    		{
    				"version #430 core \n",
    				"out vec4 color; \n",
    				"void main(void) \n",
    				"{color = vec4(0.0, 0.0, 1.0, 1.0);} \n",	
    		};
    	
    	// This next part will be used almost verbatim for our later programs
    	// create shader, read, and compile
    	int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
    	gl.glShaderSource(vShader, 3, vshaderSource, null, 0);
    	gl.glCompileShader(vShader);
    	
    	// create fragment shader, read, and compile
    	int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
    	gl.glShaderSource(fShader, 4, fshaderSource, null, 0);
    	gl.glCompileShader(fShader);
    	
    	// create the rendering program that we need for the output (of this method)
    	int vfprogram = gl.glCreateProgram();
    	gl.glAttachShader(vfprogram, vShader);
    	gl.glAttachShader(vfprogram, fShader);
    	
    	gl.glLinkProgram(vfprogram); // links program to computer
    	
    	// good practice to delete what you dont need
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
