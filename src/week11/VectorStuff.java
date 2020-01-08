package week11;

import graphicslib3D.Vector3D;

public class VectorStuff {

    public static void main(String[] args) {
        
        Vector3D a = new Vector3D(0.3,0.2,0);
        Vector3D b = new Vector3D(0.1,0.1,0);
        
        Vector3D c = a.cross(b);
        System.out.println(c);

    }

}
