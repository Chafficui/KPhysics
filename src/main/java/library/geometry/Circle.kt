package library.geometry

import demo.Camera
import demo.ColourSettings
import library.collision.AxisAlignedBoundingBox
import library.math.Vec2
import java.awt.Graphics2D
import java.awt.geom.Ellipse2D

/**
 * Circle class to create a circle object.
 */
class Circle
/**
 * Constructor for a circle.
 *
 * @param radius Desired radius of the circle.
 */(var radius: Double) : Shape() {
    /**
     * Calculates the mass of a circle.
     *
     * @param density The desired density to factor into the calculation.
     */
    override fun calcMass(density: Double) {
        body.mass = StrictMath.PI * radius * radius * density
        body.invMass = if (body.mass != 0.0) 1.0f / body.mass else 0.0
        body.inertia = body.mass * radius * radius
        body.invInertia = if (body.inertia != 0.0) 1.0f / body.inertia else 0.0
    }

    /**
     * Generates an AABB and binds it to the body.
     */
    override fun createAABB() {
        body.aabb = AxisAlignedBoundingBox(Vec2(-radius, -radius), Vec2(radius, radius))
    }

    /**
     * Debug draw method for a circle.
     *
     * @param g             Graphics2D object to draw to
     * @param paintSettings Colour settings to draw the objects to screen with
     * @param camera        Camera class used to convert points from world space to view space
     */
    override fun draw(g: Graphics2D, paintSettings: ColourSettings, camera: Camera) {
        if (body.mass == 0.0) {
            g.color = paintSettings.staticFill
        } else {
            g.color = paintSettings.shapeFill
        }
        val circlePotion = camera.convertToScreen(body.position)
        val drawnRadius = camera.scaleToScreenXValue(radius)
        g.fill(
            Ellipse2D.Double(
                circlePotion.x - drawnRadius,
                circlePotion.y - drawnRadius,
                2 * drawnRadius,
                2 * drawnRadius
            )
        )
        if (body.mass == 0.0) {
            g.color = paintSettings.staticOutLine
        } else {
            g.color = paintSettings.shapeOutLine
        }
        g.draw(
            Ellipse2D.Double(
                circlePotion.x - drawnRadius,
                circlePotion.y - drawnRadius,
                2 * drawnRadius,
                2 * drawnRadius
            )
        )
    }
}