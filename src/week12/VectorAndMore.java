package week12;

import graphicslib3D.Matrix3D;
import graphicslib3D.Vector3D;
import static java.lang.System.out; 


public class VectorAndMore {
    
    public static void main(String[] args) {
    
        Vector3D vec= new Vector3D(1,2,3);
        //out.println(vec);
        Vector3D negativeVec = vec.mult(-1);
        // out.println(negativeVec);
        // out.println(vec.magnitude());
        Vector3D unitVec = vec.normalize();
        //out.println(unitVec);
        //out.println(unitVec.magnitude());
        
        Vector3D vec2 = new Vector3D(3,-1,5);
        //out.println(vec.dot(vec2));
        
        Vector3D crossProd = vec.cross(vec2);
        //out.println(crossProd);
        
        // for the lookAt method
        Vector3D eye = new Vector3D(0,0,2);
        Vector3D center = new Vector3D(0,0,0);
        Vector3D up = new Vector3D(-0.1,1,0);
        
        Vector3D a = eye.minus(center);
        Vector3D w = a.normalize();
        
        Vector3D u = up.cross(w).normalize();
        Vector3D v = w.cross(u);
        
        // for the last column
        double firstEntry = -u.dot(eye);
        double secondEntry = -v.dot(eye);
        double thirdEntry = -w.dot(eye);

        // lets build the matrix!
        Matrix3D mat = new Matrix3D();
        mat.setRow(0, u);
        mat.setRow(1, v);
        mat.setRow(2, w);
        
        // last column
        Vector3D col = new Vector3D(firstEntry, secondEntry, thirdEntry, 1);
        mat.setCol(3, col);
        
        out.println(mat);
        
        
        

        
        
        
        
        
    
    }
    

}
