package bezier

import bezier.units.Seconds
import javafx.scene.control.Alert
import utils.Config

object GraphicalBezier {
    @JvmStatic
    fun generateSpline(controlPoint: ArrayList<Point>): ArrayList<Point> {
        val pathXYCoords = Bezier.generatePureXY(controlPoint)
        val times = trapezoidalTimes(pathXYCoords, controlPoint)
        val path = Bezier.generateWithVel(controlPoint, times, Config.maxVel().inchesPerSecond(), Config.maxAccel().inchesPerSecondSquared(), Config.timeStep())
        return Bezier.motion(path, Config.width())
    }

    @JvmStatic
    private fun trapezoidalTimes(xyCoords: ArrayList<Point>, controlPoints: ArrayList<Point>): ArrayList<Seconds> {
        return try {
            Bezier.trapezoidalTimes(xyCoords, controlPoints, Config.maxVel().inchesPerSecond(), Config.maxAccel().inchesPerSecondSquared())
        } catch (e: IllegalArgumentException) {
            val message = e.message!!
            val alert = Alert(Alert.AlertType.ERROR)
            alert.headerText = "Error"
            alert.headerText = "Impossible Physics"
            alert.contentText = message
            alert.showAndWait()
            arrayListOf()
        }
    }
}