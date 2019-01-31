import bezier.Point;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class BezierTest {
    @Test
    void horizTest() {
        var controlPoints = new ArrayList<Point>();
        controlPoints.add(new Point(0, 0, true));
        controlPoints.add(new Point(60, 0, true));

//        var path = Bezier.generateAll(controlPoints); // todo
//
//        for (Point point : path) {
//            System.out.println(point.getTargetVelocity() + "\n"
//                    + point.getLeftVel() + "\n"
//                    + point.getLeftVelLinear() + "\n"
//                    + point.getLeftPos() + "\n"
//            );
//        }
    }
}
