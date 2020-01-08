package week6;

import graphicslib3D.Matrix3D;
import graphicslib3D.Vector3D;

import static java.lang.System.out ;

public class PracticeWithMatrices {

	public static void main(String[] args) {

		Matrix3D translateMatrix = new Matrix3D();
		translateMatrix.translate(2, 0, -1);

		Vector3D originalPosition = new Vector3D(1,1,4,1);
		Vector3D translatedPosition = originalPosition.mult(translateMatrix);
		
		Matrix3D rotateMatrix = new Matrix3D();
		double rotateDeg = (double) 45;
		rotateMatrix.rotateZ(rotateDeg);
		//out.println(rotateMatrix);
		
		// class problem: rotate (somehow) the point
		// (5,0,0) to the point (0,0,5); which rotation matrix is needed?
		originalPosition = new Vector3D(5,0,0,1);
		rotateMatrix = new Matrix3D();
		rotateDeg = (double) -90;
		rotateMatrix.rotateY(rotateDeg);
		Vector3D rotatedPosition = originalPosition.mult(rotateMatrix);
		//out.println(rotatedPosition);
		
		// example with concactenation
		originalPosition = new Vector3D(1,0,0,1);
		Matrix3D togetherMatrix = new Matrix3D();
		translateMatrix = new Matrix3D();
		translateMatrix.translate(4, 0, 0);
		togetherMatrix.concatenate(rotateMatrix);
		togetherMatrix.concatenate(translateMatrix);
		//out.println(togetherMatrix);
		Vector3D outputVector = originalPosition.mult(togetherMatrix);
		//out.println(outputVector);
		
		
		// last example
		// setup and initialization
		originalPosition = new Vector3D(1,0,0,1);
		togetherMatrix = new Matrix3D();
		rotateMatrix = new Matrix3D();
		Matrix3D rotate2Matrix = new Matrix3D();
		translateMatrix = new Matrix3D();
		
		// M1
		translateMatrix.translate(4, 0, 0);
		// M2
		rotateDeg = (double) 90;
		rotateMatrix.rotateZ(rotateDeg);
		// M3
		rotateDeg = (double) 90;
		rotate2Matrix.rotateX(rotateDeg);
		togetherMatrix.concatenate(rotate2Matrix);
		togetherMatrix.concatenate(rotateMatrix);
		togetherMatrix.concatenate(translateMatrix);
		
		outputVector = originalPosition.mult(togetherMatrix);
		out.println(outputVector);

		
		
		
		

		
	}

}
