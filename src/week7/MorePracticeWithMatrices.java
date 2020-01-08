package week7;

import graphicslib3D.Matrix3D;
import graphicslib3D.MatrixStack;
import graphicslib3D.Vector3D;

import static java.lang.System.out ;

public class MorePracticeWithMatrices {

	public static void main(String[] args) {

		Matrix3D myMatrix = new Matrix3D(); // here the identity Ma
		
		myMatrix.setElementAt(0, 0, 1);
		myMatrix.setElementAt(0, 1, 2);

	    myMatrix.setElementAt(0, 2, 3);
	    myMatrix.setElementAt(0, 3, 4);

        myMatrix.setElementAt(2, 2, 2);
        
        

        //out.println(myMatrix);
        
        
        // a bit of practice with the stack (useful for animation)
        MatrixStack myStack = new MatrixStack(20); // room for a stack of 20 matrices
        //out.println(myStack.peek()); // useful for debugging;
        myStack.multMatrix(myMatrix); // this multiplies only the top matrix in the stack!!!!
        myStack.pushMatrix(); // now we have another copy in level 2
        myStack.popMatrix(); // now we only have one copy in level 1
        //out.println(myStack.peek());
      
        
        myStack = new MatrixStack(20); // room for a stack of 20 matrices
        // make a copy of the identity
        myStack.pushMatrix();
        // now let's work on level 2
        myStack.multMatrix(myMatrix);
        myStack.popMatrix();
        
        //out.println(myStack.peek());
        out.println(myStack);
        
        
       
        

        
        
        
		
		
		

		
	}

}
