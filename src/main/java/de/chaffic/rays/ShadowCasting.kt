package de.chaffic.rays

import de.chaffic.collision.Arbiter.Companion.isPointInside
import de.chaffic.collision.bodies.CollisionBodyInterface
import de.chaffic.geometry.Circle
import de.chaffic.geometry.Polygon
import de.chaffic.geometry.bodies.TranslatableBody
import de.chaffic.math.Mat2
import de.chaffic.math.Vec2
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
 */(var startPoint: Vec2, private val distance: Double) {

    val rayData = ArrayList<RayAngleInformation>()

    /**
     * Updates the all projections in world space and acquires information about all intersecting rays.
     *
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    fun updateProjections(bodiesToEvaluate: ArrayList<TranslatableBody>) {
        rayData.clear()
        for (B in bodiesToEvaluate) {
            if(B !is CollisionBodyInterface) continue

            if (isPointInside(B, startPoint)) {
                rayData.clear()
                break
            }
            if (B.shape is Polygon) {
                val poly1 = B.shape as Polygon
                for (v in poly1.vertices) {
                    val direction = poly1.orientation.mul(v, Vec2()).plus(B.position).minus(startPoint)
                    projectRays(direction, bodiesToEvaluate)
                }
            } else {
                val circle = B.shape as Circle
                val d = B.position.minus(startPoint)
                val angle = asin(circle.radius / d.length())
                val u = Mat2(angle)
                projectRays(u.mul(d.normalize(), Vec2()), bodiesToEvaluate)
                val u2 = Mat2(-angle)
                projectRays(u2.mul(d.normalize(), Vec2()), bodiesToEvaluate)
            }
        }
        rayData.sortWith { lhs: RayAngleInformation, rhs: RayAngleInformation ->
            rhs.angle.compareTo(lhs.angle)
        }
    }

    /**
     * Projects a ray and evaluates it against all objects supplied in world space.
     *
     * @param direction        Direction of ray to project.
     * @param bodiesToEvaluate Arraylist of bodies to check if they intersect with the ray projection.
     */
    private fun projectRays(direction: Vec2, bodiesToEvaluate: ArrayList<TranslatableBody>) {
        val m = Mat2(0.001)
        m.transpose().mul(direction)
        for (i in 0..2) {
            val ray = Ray(startPoint, direction, distance)
            ray.updateProjection(bodiesToEvaluate)
            rayData.add(RayAngleInformation(ray, atan2(direction.y, direction.x)))
            m.mul(direction)
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
class RayAngleInformation
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
    val ray: Ray,
    /**
     * Getter for ANGLE.
     *
     * @return returns ANGLE.
     */
    val angle: Double
)