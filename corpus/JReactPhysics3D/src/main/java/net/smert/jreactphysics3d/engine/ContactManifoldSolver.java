/*
 * ReactPhysics3D physics library, http://code.google.com/p/reactphysics3d/
 * Copyright (c) 2010-2013 Daniel Chappuis
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not claim
 *    that you wrote the original software. If you use this software in a
 *    product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 *
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 *
 * 3. This notice may not be removed or altered from any source distribution.
 *
 * This file has been modified during the port to Java and differ from the source versions.
 */
package net.smert.jreactphysics3d.engine;

import net.smert.jreactphysics3d.mathematics.Matrix3x3;
import net.smert.jreactphysics3d.mathematics.Vector3;

/**
 * Contact solver internal data structure to store all the information relative to a contact manifold.
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class ContactManifoldSolver {

    // True if the body 1 is allowed to move
    public boolean isBody1Moving;

    // True if the body 2 is allowed to move
    public boolean isBody2Moving;

    // Index of body 1 in the constraint solver
    public int indexBody1;

    // Index of body 2 in the constraint solver
    public int indexBody2;

    // Number of contact points
    public int numContacts;

    // Mix friction coefficient for the two bodies
    public float frictionCoefficient;

    // First friction direction impulse at manifold center
    public float friction1Impulse;

    // Second friction direction impulse at manifold center
    public float friction2Impulse;

    // Twist friction impulse at contact manifold center
    public float frictionTwistImpulse;

    // Matrix K for the first friction constraint
    public float inverseFriction1Mass;

    // Matrix K for the second friction constraint
    public float inverseFriction2Mass;

    // Matrix K for the twist friction constraint
    public float inverseTwistFrictionMass;

    // Mix of the restitution factor for two bodies
    public float restitutionFactor;

    // Inverse of the mass of body 1
    public float massInverseBody1;

    // Inverse of the mass of body 2
    public float massInverseBody2;

    // Pointer to the external contact manifold
    public ContactManifold externalContactManifold;

    // Contact point constraints
    public final ContactPointSolver[] contacts = new ContactPointSolver[ContactManifold.MAX_CONTACT_POINTS_IN_MANIFOLD];

    // Inverse inertia tensor of body 1
    public final Matrix3x3 inverseInertiaTensorBody1 = new Matrix3x3();

    // Inverse inertia tensor of body 2
    public final Matrix3x3 inverseInertiaTensorBody2 = new Matrix3x3();

    // Point on body 1 where to apply the friction constraints
    public final Vector3 frictionPointBody1 = new Vector3();

    // Point on body 2 where to apply the friction constraints
    public final Vector3 frictionPointBody2 = new Vector3();

    // First friction direction at contact manifold center
    public final Vector3 frictionVector1 = new Vector3();

    // Second friction direction at contact manifold center
    public final Vector3 frictionVector2 = new Vector3();

    // Average normal vector of the contact manifold
    public final Vector3 normal = new Vector3();

    // Old 1st friction direction at contact manifold center
    public final Vector3 oldFrictionVector1 = new Vector3();

    // Old 2nd friction direction at contact manifold center
    public final Vector3 oldFrictionVector2 = new Vector3();

    // Cross product of r1 with 1st friction vector
    public final Vector3 r1CrossT1 = new Vector3();

    // Cross product of r1 with 2nd friction vector
    public final Vector3 r1CrossT2 = new Vector3();

    // Cross product of r2 with 1st friction vector
    public final Vector3 r2CrossT1 = new Vector3();

    // Cross product of r2 with 2nd friction vector
    public final Vector3 r2CrossT2 = new Vector3();

    // R1 vector for the friction constraints
    public final Vector3 r1Friction = new Vector3();

    // R2 vector for the friction constraints
    public final Vector3 r2Friction = new Vector3();

}
