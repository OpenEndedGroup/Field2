package field.graphics;

import field.linalg.Mat4;
import field.linalg.Vec3;

/**
 * Created by marc on 10/18/2016.
 */
public interface StereoCameraInterface {
    Mat4 projectionMatrix(float stereoSide);

    Mat4 view(float stereoSide);
    Vec3 getPosition();

}