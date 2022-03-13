package library.rays

import library.collision.Arbiter.Companion.isPointInside
import library.dynamics.Body
import library.geometry.Circle
import library.geometry.Polygon
import library.math.Matrix2D
import library.math.Vectors2D
import testbed.Camera
import testbed.ColourSettings
import java.awt.Graphics2D
import java.awt.geom.Path2D
import kotlin.math.asin
import kotlin.math.atan2

/**
 * A class for generating polygons that can mimic line of sight around objects and cast shadows.
 */
class ShadowCasting
/**
 * Constructor
 *
 * @param startPoint Origin of projecting rays.
 * @param distance   The desired distance to project the rays.
 */(private var startPoint: Vectors2D, private val distance: Int) {
    /**
     * Setter for start point.
     *
     * @param startPoint Returns start point.
     */
    fun setStartPoint(startPoint: Vectors2D) {
        this.startPoint = startPoint
    }

    private val rayData = ArrayList<RayAngleInformation>()

    /**
     * Updates the all projections in world space and acquires information about all intersecting rays.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjections(bodiesToEvaluate: ArrayList<Body>) {
        rayData.clear()
        for (B in bodiesToEvaluate) {
            if (isPointInside(B, startPoint)) {
                rayData.clear()
                break
            }
            if (B.shape is Polygon) {
                val poly1 = B.shape as Polygon
                for (v in poly1.vertices) {
                    val direction = poly1.orient.mul(v, Vectors2D()).addi(B.position).subtract(startPoint)
                    projectRays(direction, bodiesToEvaluate)
                }
            } else {
                val circle = B.shape as Circle
                val d = B.position.subtract(startPoint)
                val angle = asin(circle.radius / d.length())
                val u = Matrix2D(angle)
                projectRays(u.mul(d.normalize(), Vectors2D()), bodiesToEvaluate)
                val u2 = Matrix2D(-angle)
                projectRays(u2.mul(d.normalize(), Vectors2D()), bodiesToEvaluate)
            }
        }
        rayData.sortWith { lhs: RayAngleInformation, rhs: RayAngleInformation ->
            rhs.aNGLE.compareTo(lhs.aNGLE)
        }
    }

    /**
     * Projects a ray and evaluates it against all objects supplied in world space.
     *
     * @param direction        Direction of ray to project.
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    private fun projectRays(direction: Vectors2D, bodiesToEvaluate: ArrayList<Body>) {
        val m = Matrix2D(0.001)
        m.transpose().mul(direction)
        for (i in 0..2) {
            val ray = Ray(startPoint, direction, distance)
            ray.updateProjection(bodiesToEvaluate)
            rayData.add(RayAngleInformation(ray, atan2(direction.y, direction.x)))
            m.mul(direction)
        }
    }

    /**
     * Debug draw method for all polygons generated and rays.
     *
     * @param g             Graphics2D object to draw to
     * @param paintSettings Colour settings to draw the objects to screen with
     * @param camera        Camera class used to convert points from world space to view space
     */
    fun draw(g: Graphics2D, paintSettings: ColourSettings, camera: Camera) {
        for (i in rayData.indices) {
            val ray1 = rayData[i].rAY
            val ray2 = rayData[if (i + 1 == rayData.size) 0 else i + 1].rAY
            g.color = paintSettings.shadow
            val s = Path2D.Double()
            val worldStartPoint = camera.convertToScreen(startPoint)
            s.moveTo(worldStartPoint.x, worldStartPoint.y)
            if (ray1.rayInformation != null) {
                val point1 = camera.convertToScreen(ray1.rayInformation!!.coord)
                s.lineTo(point1.x, point1.y)
            }
            if (ray2.rayInformation != null) {
                val point2 = camera.convertToScreen(ray2.rayInformation!!.coord)
                s.lineTo(point2.x, point2.y)
            }
            s.closePath()
            g.fill(s)
        }
    }

    /**
     * Getter for number of rays projected.
     *
     * @return Returns size of raydata.
     */
    val noOfRays: Int
        get() = rayData.size
}

/**
 * Ray information class to store relevant data about rays and any intersection found specific to shadow casting.
 */
internal class RayAngleInformation
/**
 * Constructor to store ray information.
 *
 * @param ray   Ray of intersection.
 * @param angle Angle the ray is set to.
 */(
    /**
     * Getter for RAY.
     *
     * @return returns RAY.
     */
    val rAY: Ray,
    /**
     * Getter for ANGLE.
     *
     * @return returns ANGLE.
     */
    val aNGLE: Double
)