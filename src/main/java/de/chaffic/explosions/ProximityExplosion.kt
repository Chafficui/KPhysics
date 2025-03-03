package de.chaffic.explosions

import de.chaffic.dynamics.bodies.PhysicalBodyInterface
import de.chaffic.geometry.bodies.TranslatableBody
import de.chaffic.math.Vec2

/**
 * Models proximity explosions.
 */
class ProximityExplosion
/**
 * Constructor.
 *
 * @param epicentre The epicentre of the explosion.
 * @param proximity    The proximity in which bodies are effected.
 */(private var epicentre: Vec2, val proximity: Int) : Explosion {
    /**
     * Sets the epicentre to a different coordinate.
     *
     * @param v The vector position of the new epicentre.
     */
    override fun setEpicentre(v: Vec2) {
        epicentre = v
    }

    fun getEpicentre(): Vec2 {
        return epicentre
    }

    private var bodiesEffected = ArrayList<TranslatableBody>()

    /**
     * Updates the arraylist to reevaluate what bodies are effected/within the proximity.
     *
     * @param bodiesToEvaluate Arraylist of bodies in the world to check.
     */
    override fun update(bodiesToEvaluate: ArrayList<TranslatableBody>) {
        bodiesEffected.clear()
        for (b in bodiesToEvaluate) {
            val blastDist = b.position.minus(epicentre)
            if (blastDist.length() <= proximity) {
                bodiesEffected.add(b)
            }
        }
    }

    val linesToBodies = ArrayList<Vec2?>()

    /**
     * Updates the lines to body array for the debug drawer.
     */
    fun updateLinesToBody() {
        linesToBodies.clear()
        for (b in bodiesEffected) {
            linesToBodies.add(b.position)
        }
    }

    /**
     * Applies blast impulse to all effected bodies centre of mass.
     *
     * @param blastPower Blast magnitude.
     */
    override fun applyBlastImpulse(blastPower: Double) {
        for (b in bodiesEffected) {
            if(b !is PhysicalBodyInterface) continue

            val blastDir = b.position.minus(epicentre)
            val distance = blastDir.length()
            if (distance == 0.0) return

            //Not physically correct as it should be blast * radius to object ^ 2 as the pressure of an explosion in 2D dissipates
            val invDistance = 1 / distance
            val impulseMag = blastPower * invDistance
            b.applyLinearImpulse(blastDir.normalize().scalar(impulseMag))
        }
    }
}