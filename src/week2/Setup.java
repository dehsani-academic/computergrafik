package week2;


import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;

public class Setup extends JFrame implements GLEventListener{
    private GLCanvas myCanvas;
    public Setup(){
        setTitle("Intro Program");
        setSize(600, 400);
        setLocation(200, 200);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        setVisible(true);
    }
    
    
    // where we place code that draws to the GLCanvas
    public void display(GLAutoDrawable drawable){
        GL4 gl = (GL4) GLContext.getCurrentGL(); // note: GL4 is a Java interface to the OpenGL functions
        // any OpenGL function described in the OpenGL documentation can be called
        // from JOGL by preceding it with the name of the GL4 object (here gl)
        
        
        float bkg[] = { 1.0f, 1.0f, 1.0f, 1.0f };
        
        // load buffer with color
        FloatBuffer bkgBuffer = Buffers.newDirectFloatBuffer(bkg);
        
        // fill the display buffer (GLenum: GL_COLOR buffer, which buffer, last variable is a pointer in C, here a buffer)
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
        }
    
	public static void main(String[] args)
	{ 
		new Setup();
	}
    
    
    public void init(GLAutoDrawable drawable){
        
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int
    height) {
        
    }
    
    public void dispose(GLAutoDrawable drawable){
        
    }
    
}
