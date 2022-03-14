package de.chaffic.dynamics

import de.chaffic.collision.AxisAlignedBoundingBox
import de.chaffic.joints.Joint
import de.chaffic.math.Vec2
import kotlin.math.pow

/**
 * Class for creating a world with iterative solver structure.
 *
 * @param gravity The strength of gravity in the world.
 */
class World(var gravity: Vec2 = Vec2()) {

    var bodies = ArrayList<Body>()

    /**
     * Adds a body to the world
     *
     * @param b Body to add.
     * @return Returns the newly added body.
     */
    fun addBody(b: Body): Body {
        bodies.add(b)
        return b
    }

    /**
     * Removes a body from the world.
     *
     * @param b The body to remove from the world.
     */
    fun removeBody(b: Body) {
        bodies.remove(b)
    }

    @JvmField
    var joints = ArrayList<Joint>()

    /**
     * Adds a joint to the world.
     *
     * @param j The joint to add.
     * @return Returns the joint added to the world.
     */
    fun addJoint(j: Joint): Joint {
        joints.add(j)
        return j
    }

    /**
     * Removes a joint from the world.
     *
     * @param j The joint to remove from the world.
     */
    fun removeJoint(j: Joint) {
        joints.remove(j)
    }

    var contacts = ArrayList<de.chaffic.collision.Arbiter>()

    /**
     * The main time step method for the world to conduct an iteration of the current world call this method with a desired time step value.
     *
     * @param dt Timestep
     */
    fun step(dt: Double) {
        contacts.clear()
        broadPhaseCheck()
        semiImplicit(dt)

        //Correct positional errors from the discrete collisions
        for (contact in contacts) {
            contact.penetrationResolution()
        }
    }

    /**
     * Semi implicit euler integration method for the world bodies and forces.
     *
     * @param dt Timestep
     */
    private fun semiImplicit(dt: Double) {
        //Applies tentative velocities
        applyForces(dt)
        solve()

        //Integrate positions
        for (b in bodies) {
            if (b.invMass == 0.0) {
                continue
            }
            b.position.add(b.velocity.scalar(dt))
            b.orientation = b.orientation + dt * b.angularVelocity
            b.force[0.0] = 0.0
            b.torque = 0.0
        }
    }

    /**
     * Applies semi-implicit euler and drag forces.
     *
     * @param dt Timestep
     */
    private fun applyForces(dt: Double) {
        for (b in bodies) {
            if (b.invMass == 0.0) {
                continue
            }
            applyLinearDrag(b)
            if (b.affectedByGravity) {
                b.velocity.add(gravity.scalar(dt))
            }
            b.velocity.add(b.force.scalar(b.invMass).scalar(dt))
            b.angularVelocity += dt * b.invInertia * b.torque
        }
    }

    /**
     * Method to apply all forces in the world.
     */
    private fun solve() {
        /*
        Resolve joints
        Note: this is removed from the iterations at this stage as the application of forces is different.
        The extra iterations on joints make the forces of the joints multiple times larger equal to the number of iterations.
        Early out could be used like in the collision solver
        This may change in the future and will be revised at a later date.
        */
        for (j in joints) {
            j.applyTension()
        }

        //Resolve collisions
        for (i in 0 until Physics.ITERATIONS) {
            for (contact in contacts) {
                contact.solve()
            }
        }
    }

    /**
     * Applies linear drag to a body.
     *
     * @param b Body to apply drag to.
     */
    private fun applyLinearDrag(b: Body?) {
        val velocityMagnitude = b!!.velocity.length()
        val dragForceMagnitude = velocityMagnitude * velocityMagnitude * b.linearDampening
        val dragForceVector = b.velocity.normalized.scalar(-dragForceMagnitude)
        b.applyForce(dragForceVector)
    }

    /**
     * A discrete Broad phase check of collision detection.
     */
    private fun broadPhaseCheck() {
        for (i in bodies.indices) {
            val a = bodies[i]
            for (x in i + 1 until bodies.size) {
                val b = bodies[x]

                //Ignores static or particle objects
                if (a.invMass == 0.0 && b.invMass == 0.0 || a.particle && b.particle) {
                    continue
                }
                if (AxisAlignedBoundingBox.aabbOverlap(a, b)) {
                    narrowPhaseCheck(a, b)
                }
            }
        }
    }

    /**
     * If broad phase detection check passes, a narrow phase check is conducted to determine for certain if two objects are intersecting.
     * If two objects are, arbiters of contacts found are generated
     *
     * @param a
     * @param b
     */
    private fun narrowPhaseCheck(a: Body, b: Body) {
        val contactQuery = de.chaffic.collision.Arbiter(a, b)
        contactQuery.narrowPhase()
        if (contactQuery.contactCount > 0) {
            contacts.add(contactQuery)
        }
    }

    /**
     * Clears all objects in the current world
     */
    fun clearWorld() {
        bodies.clear()
        contacts.clear()
        joints.clear()
    }

    /**
     * Applies gravitational forces between to objects (force applied to centre of body)
     */
    fun gravityBetweenObj() {
        for (a in bodies.indices) {
            val bodyA = bodies[a]
            for (b in a + 1 until bodies.size) {
                val bodyB = bodies[b]
                val distance = bodyA.position.distance(bodyB.position)
                val force = 6.67.pow(-11.0) * bodyA.mass * bodyB.mass / (distance * distance)
                var direction: Vec2? = Vec2(bodyB.position.x - bodyA.position.x, bodyB.position.y - bodyA.position.y)
                direction = direction!!.scalar(force)
                val oppositeDir = Vec2(-direction.x, -direction.y)
                bodyA.force.plus(direction)
                bodyB.force.plus(oppositeDir)
            }
        }
    }
}