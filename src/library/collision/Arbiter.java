package library.collision;


import library.dynamics.Body;
import library.geometry.Circle;
import library.geometry.Polygon;
import library.utils.Settings;
import library.math.Vectors2D;

public class Arbiter {
    Body A;
    Body B;

    public Arbiter(Body a, Body b) {
        this.A = a;
        this.B = b;
    }

    public final Vectors2D[] contacts = {new Vectors2D(), new Vectors2D()};
    public Vectors2D normal = new Vectors2D();
    public int contactCount = 0;
    public double restitution = 0;

    public void narrowPhase() {
        restitution = Math.min(A.restitution, B.restitution);
        if (A.shape instanceof Circle && B.shape instanceof Circle) {
            circleVsCircle();
        } else if (A.shape instanceof Circle && B.shape instanceof Polygon) {
            circleVsPolygon(A, B);
        } else if (A.shape instanceof Polygon && B.shape instanceof Circle) {
            circleVsPolygon(B, A);
            if (this.contactCount > 0) {
                this.normal.negative();
            }
        } else if (A.shape instanceof Polygon && B.shape instanceof Polygon) {
            polygonVsPolygon();
        }
    }

    private double penetration = 0;

    private void circleVsCircle() {
        Circle ca = (Circle) A.shape;
        Circle cb = (Circle) B.shape;

        Vectors2D normal = B.position.subtract(A.position);

        double distance = normal.length();
        double radius = ca.radius + cb.radius;

        if (distance >= radius) {
            contactCount = 0;
            return;
        }

        this.contactCount = 1;

        if (distance == 0) {
            this.penetration = radius;
            this.normal = new Vectors2D(0, 1);
            this.contacts[0].set(A.position);
        } else {
            this.penetration = radius - distance;
            this.normal = normal.normalize();
            this.contacts[0].set(this.normal.scalar(ca.radius).addi(A.position));
        }
    }

    private void circleVsPolygon(Body a, Body b) {
        Circle A = (Circle) a.shape;
        Polygon B = (Polygon) b.shape;

        //Transpose effectively removes the rotation thus allowing the OBB vs OBB detection to become AABB vs OBB
        Vectors2D distOfBodies = a.position.subtract(b.position);
        Vectors2D polyToCircleVec = B.orient.transpose().mul(distOfBodies);
        double penetration = -Double.MAX_VALUE;
        int faceNormalIndex = 0;

        //Applies SAT to check for potential penetration
        //Retrieves best face of polygon
        for (int i = 0; i < B.vertices.length; i++) {
            Vectors2D v = polyToCircleVec.subtract(B.vertices[i]);
            double distance = B.normals[i].dotProduct(v);

            //If circle is outside of polygon, no collision detected.
            if (distance > A.radius) {
                return;
            }

            if (distance > penetration) {
                faceNormalIndex = i;
                penetration = distance;
            }
        }

        //Get vertex's of best face
        Vectors2D vector1 = B.vertices[faceNormalIndex];
        Vectors2D vector2 = B.vertices[faceNormalIndex + 1 < B.vertices.length ? faceNormalIndex + 1 : 0];

        Vectors2D v1ToV2 = vector2.subtract(vector1);
        Vectors2D circleBodyTov1 = polyToCircleVec.subtract(vector1);
        double firstPolyCorner = circleBodyTov1.dotProduct(v1ToV2);

        Vectors2D v2ToV1 = vector1.subtract(vector2);
        Vectors2D circleBodyTov2 = polyToCircleVec.subtract(vector2);
        double secondPolyCorner = circleBodyTov2.dotProduct(v2ToV1);

        //If first vertex is positive, v1 face region collision check
        //If second vertex is positive, v2 face region collision check
        //Else circle has made contact with the polygon face.
        if (firstPolyCorner <= 0.0) {
            penetration = polyToCircleVec.distance(vector1);

            //Check to see if vertex is within the circle
            if (penetration >= A.radius) {
                return;
            }

            this.penetration = penetration;
            contactCount = 1;
            B.orient.mul(this.normal.set(vector1.subtract(polyToCircleVec).normalize()));
            contacts[0] = B.orient.mul(vector1, new Vectors2D()).addi(b.position);
        } else if (secondPolyCorner <= 0.0) {
            penetration = polyToCircleVec.distance(vector2);

            //Check to see if vertex is within the circle
            if (penetration >= A.radius) {
                return;
            }

            this.penetration = penetration;
            contactCount = 1;
            B.orient.mul(this.normal.set(vector2.subtract(polyToCircleVec).normalize()));
            contacts[0] = B.orient.mul(vector2, new Vectors2D()).addi(b.position);

        } else {
            this.penetration = A.radius - penetration;
            Vectors2D faceNormal = B.normals[faceNormalIndex];
            this.contactCount = 1;
            B.orient.mul(faceNormal, this.normal);
            Vectors2D circleContactPoint = a.position.addi(this.normal.negative().scalar(A.radius));
            this.contacts[0].set(circleContactPoint);
        }
    }

    private void polygonVsPolygon() {
        Polygon pa = (Polygon) A.shape;
        Polygon pb = (Polygon) B.shape;

        AxisData aData = new AxisData();
        findAxisOfMinPenetration(aData, pa, pb);
        if (aData.getPenetration() >= 0) {
            return;
        }

        AxisData bData = new AxisData();
        findAxisOfMinPenetration(bData, pb, pa);
        if (bData.getPenetration() >= 0) {
            return;
        }

        int referenceFaceIndex;
        Polygon referencePoly;
        Polygon incidentPoly;
        boolean flip;

        if (selectionBias(aData.getPenetration(), bData.getPenetration())) {
            referencePoly = pa;
            incidentPoly = pb;
            referenceFaceIndex = aData.getReferenceFaceIndex();
            flip = false;
        } else {
            referencePoly = pb;
            incidentPoly = pa;
            referenceFaceIndex = bData.getReferenceFaceIndex();
            flip = true;
        }

        Vectors2D[] incidentFaceVertexes = new Vectors2D[2];
        Vectors2D referenceNormal = referencePoly.normals[referenceFaceIndex];

        //Reference face of reference polygon in local space of incident polygon
        referenceNormal = referencePoly.orient.mul(referenceNormal, new Vectors2D());
        referenceNormal = incidentPoly.orient.transpose().mul(referenceNormal, new Vectors2D());

        //Finds face of incident polygon angled best vs reference poly normal.
        //Best face is the incident face that is the most anti parallel (most negative dot product)
        int incidentIndex = 0;
        double minDot = Double.MAX_VALUE;
        for (int i = 0; i < incidentPoly.vertices.length; i++) {
            double dot = referenceNormal.dotProduct(incidentPoly.normals[i]);

            if (dot < minDot) {
                minDot = dot;
                incidentIndex = i;
            }
        }

        //Incident faces vertexes in world space
        incidentFaceVertexes[0] = incidentPoly.orient.mul(incidentPoly.vertices[incidentIndex], new Vectors2D()).addi(incidentPoly.body.position);
        incidentFaceVertexes[1] = incidentPoly.orient.mul(incidentPoly.vertices[incidentIndex + 1 >= incidentPoly.vertices.length ? 0 : incidentIndex + 1], new Vectors2D()).addi(incidentPoly.body.position);

        //Gets vertex's of reference polygon reference face in world space
        Vectors2D v1 = referencePoly.vertices[referenceFaceIndex];
        Vectors2D v2 = referencePoly.vertices[referenceFaceIndex + 1 == referencePoly.vertices.length ? 0 : referenceFaceIndex + 1];

        //Rotate and translate vertex's of reference poly
        v1 = referencePoly.orient.mul(v1, new Vectors2D()).addi(referencePoly.body.position);
        v2 = referencePoly.orient.mul(v2, new Vectors2D()).addi(referencePoly.body.position);

        Vectors2D refTangent = v2.subtract(v1);
        refTangent.normalize();

        double negSide = -refTangent.dotProduct(v1);
        double posSide = refTangent.dotProduct(v2);
        int np = clip(refTangent.negativeVec(), negSide, incidentFaceVertexes);

        if (np < 2) {
            return;
        }

        np = clip(refTangent, posSide, incidentFaceVertexes);

        if (np < 2) {
            return;
        }

        Vectors2D refFaceNormal = refTangent.normal().negativeVec();

        Vectors2D[] contactVectorsFound = new Vectors2D[2];
        double totalPen = 0;
        int contactsFound = 0;

        for (int i = 0; i < 2; i++) {
            double separation = refFaceNormal.dotProduct(incidentFaceVertexes[i]) - refFaceNormal.dotProduct(v1);
            if (separation <= 0.0 + Settings.EPSILON) {
                contactVectorsFound[contactsFound] = incidentFaceVertexes[i];
                totalPen += -separation;
                contactsFound++;
            }

        }

        Vectors2D contactPoint;
        if (contactsFound == 1) {
            contactPoint = contactVectorsFound[0];
            this.penetration = totalPen;
        } else {
            contactPoint = (contactVectorsFound[1].addi(contactVectorsFound[0])).scalar(0.5);
            this.penetration = totalPen / 2;
        }
        this.contactCount = 1;
        this.contacts[0].set(contactPoint);
        normal.set(flip ? refFaceNormal.negative() : refFaceNormal);
    }

    private int clip(Vectors2D n, double offset, Vectors2D[] face) {
        int num = 0;
        Vectors2D[] out = {
                new Vectors2D(face[0]),
                new Vectors2D(face[1])
        };
        double dist = n.dotProduct(face[0]) - offset;
        double dist1 = n.dotProduct(face[1]) - offset;

        if (dist <= 0.0) out[num++].set(face[0]);
        if (dist1 <= 0.0) out[num++].set(face[1]);

        if (dist * dist1 < 0.0) {
            double interp = dist / (dist - dist1);

            out[num].set(face[1].subtract(face[0]).scalar(interp).addi(face[0]));
            num++;
        }

        face[0] = out[0];
        face[1] = out[1];

        return num;
    }

    //Finds the incident face of polygon A in local space relative to polygons B position
    public void findAxisOfMinPenetration(AxisData data, Polygon A, Polygon B) {
        double distance = -Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i < A.vertices.length; i++) {
            //Applies polygon A's orientation to its normals for calculation.
            Vectors2D polyANormal = A.orient.mul(A.normals[i], new Vectors2D());

            //Rotates the normal by the clock wise rotation matrix of B to put the normal relative to the local space of polygon B
            //Polygon b is axis aligned and the normal is located according to this in the correct position in local space
            Vectors2D localPolyANormal = B.orient.transpose().mul(polyANormal, new Vectors2D());

            double bestProjection = Double.MAX_VALUE;
            Vectors2D bestVertex = B.vertices[0];

            //Finds the index of the most negative vertex relative to the normal of polygon A
            for (int x = 0; x < B.vertices.length; x++) {
                Vectors2D vertex = B.vertices[x];
                double projection = vertex.dotProduct(localPolyANormal);

                if (projection < bestProjection) {
                    bestVertex = vertex;
                    bestProjection = projection;
                }
            }

            //Distance of B to A in world space space
            Vectors2D distanceOfBA = A.body.position.subtract(B.body.position);

            //Best vertex relative to polygon B in local space
            Vectors2D polyANormalVertex = B.orient.transpose().mul(A.orient.mul(A.vertices[i], new Vectors2D()).addi(distanceOfBA));

            //Distance between best vertex and polygon A's plane in local space
            double d = localPolyANormal.dotProduct(bestVertex.subtract(polyANormalVertex));

            //Records penetration and vertex
            if (d > distance) {
                distance = d;
                bestIndex = i;
            }
        }
        data.setPenetration(distance);
        data.setReferenceFaceIndex(bestIndex);
    }

    public void penetrationResolution() {

    }

    public void solve() {
        Vectors2D contactA = contacts[0].subtract(A.position);
        Vectors2D contactB = contacts[0].subtract(B.position);

        //Relative velocity created from equation found in GDC talk of box2D lite.
        Vectors2D relativeVel = B.velocity.addi(contactB.crossProduct(B.angularVelocity)).subtract(A.velocity).subtract(contactA.crossProduct(A.angularVelocity));

        //Positive = converging Negative = diverging
        double contactVel = relativeVel.dotProduct(normal);

        //Prevents objects colliding when they are moving away from each other.
        //If not, objects could still be overlapping after a contact has been resolved and cause objects to stick together
        if (contactVel >= 0) {
            return;
        }

        double acn = contactA.crossProduct(normal);
        double bcn = contactB.crossProduct(normal);
        double inverseMassSum = A.invMass + B.invMass + (acn * acn) * A.invI + (bcn * bcn) * B.invI;

        double j = -(restitution + 1) * contactVel;
        j /= inverseMassSum;

        //Apply contact impulse
        Vectors2D impulse = normal.scalar(j);
        B.velocity.add(impulse.scalar(B.invMass));
        B.angularVelocity += B.invI * contactB.crossProduct(impulse);

        A.velocity.add(impulse.negativeVec().scalar(A.invMass));
        A.angularVelocity += A.invI * contactA.crossProduct(impulse.negativeVec());

       // applyFriction(inverseMassSum, contactA, contactB);
    }

    private void applyFriction(double inverseMassSum, Vectors2D contactA, Vectors2D contactB) {
        Vectors2D rv = B.velocity.addi(contactB.crossProduct(B.angularVelocity)).subtract(A.velocity).subtract(contactA.crossProduct(A.angularVelocity));

        Vectors2D t = new Vectors2D(rv);
        t = t.add(normal.scalar(-rv.dotProduct(normal))).normalize();

        double jt = -rv.dotProduct(t);
        jt /= inverseMassSum;
        jt /= contactCount;

        Vectors2D tangentImpulse;
        double staticFriction = Math.min(A.staticFriction, B.staticFriction);
        double dynamicFriction = Math.min(A.dynamicFriction, B.dynamicFriction);
        if (StrictMath.abs(jt) < jt * staticFriction) {
            tangentImpulse = t.scalar(jt);
        } else {
            tangentImpulse = t.scalar(jt * -dynamicFriction);
        }

        B.velocity.add(tangentImpulse.scalar(B.invMass));
        B.angularVelocity += B.invI * contactB.crossProduct(tangentImpulse);

        A.velocity.add(tangentImpulse.negativeVec().scalar(A.invMass));
        A.angularVelocity += A.invI * contactA.crossProduct(tangentImpulse.negativeVec());
    }

    private static boolean selectionBias(double a, double b) {
        return a >= b * Settings.BIAS_RELATIVE + a * Settings.BIAS_ABSOLUTE;
    }
}

class AxisData {
    private double penetration;
    private int referenceFaceIndex;

    AxisData() {
        penetration = -Double.MAX_VALUE;
        referenceFaceIndex = 0;
    }

    public void setPenetration(double value) {
        penetration = value;
    }

    public void setReferenceFaceIndex(int value) {
        referenceFaceIndex = value;
    }

    public double getPenetration() {
        return penetration;
    }

    public int getReferenceFaceIndex() {
        return referenceFaceIndex;
    }
}