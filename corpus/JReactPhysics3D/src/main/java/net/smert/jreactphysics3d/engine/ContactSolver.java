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

import java.util.Map;
import net.smert.jreactphysics3d.body.RigidBody;
import net.smert.jreactphysics3d.configuration.Defaults;
import net.smert.jreactphysics3d.constraint.ContactPoint;
import net.smert.jreactphysics3d.mathematics.Mathematics;
import net.smert.jreactphysics3d.mathematics.Matrix3x3;
import net.smert.jreactphysics3d.mathematics.Vector3;

/**
 * This class represents the contact solver that is used to solve rigid bodies contacts. The constraint solver is based
 * on the "Sequential Impulse" technique described by Erin Catto in his GDC slides
 * (http://code.google.com/p/box2d/downloads/list).
 *
 * A constraint between two bodies is represented by a function C(x) which is equal to zero when the constraint is
 * satisfied. The condition C(x)=0 describes a valid position and the condition dC(x)/dt=0 describes a valid velocity.
 * We have dC(x)/dt = Jv + b = 0 where J is the Jacobian matrix of the constraint, v is a vector that contains the
 * velocity of both bodies and b is the constraint bias. We are looking for a force F_c that will act on the bodies to
 * keep the constraint satisfied. Note that from the virtual work principle, we have F_c = J^t * lambda where J^t is the
 * transpose of the Jacobian matrix and lambda is a Lagrange multiplier. Therefore, finding the force F_c is equivalent
 * to finding the Lagrange multiplier lambda.
 *
 * An impulse P = F * dt where F is a force and dt is the timestep. We can apply impulses a body to change its velocity.
 * The idea of the Sequential Impulse technique is to apply impulses to bodies of each constraints in order to keep the
 * constraint satisfied.
 *
 * --- Step 1 ---
 *
 * First, we integrate the applied force F_a acting of each rigid body (like gravity, ...) and we obtain some new
 * velocities v2' that tends to violate the constraints.
 *
 * v2' = v1 + dt * M^-1 * F_a
 *
 * where M is a matrix that contains mass and inertia tensor information.
 *
 * --- Step 2 ---
 *
 * During the second step, we iterate over all the constraints for a certain number of iterations and for each
 * constraint we compute the impulse to apply to the bodies needed so that the new velocity of the bodies satisfy Jv + b
 * = 0. From the Newton law, we know that M * deltaV = P_c where M is the mass of the body, deltaV is the difference of
 * velocity and P_c is the constraint impulse to apply to the body. Therefore, we have v2 = v2' + M^-1 * P_c. For each
 * constraint, we can compute the Lagrange multiplier lambda using : lambda = -m_c (Jv2' + b) where m_c = 1 / (J * M^-1
 * * J^t). Now that we have the Lagrange multiplier lambda, we can compute the impulse P_c = J^t * lambda * dt to apply
 * to the bodies to satisfy the constraint.
 *
 * --- Step 3 ---
 *
 * In the third step, we integrate the new position x2 of the bodies using the new velocities v2 computed in the second
 * step with : x2 = x1 + dt * v2.
 *
 * Note that in the following code (as it is also explained in the slides from Erin Catto), the value lambda is not only
 * the lagrange multiplier but is the multiplication of the Lagrange multiplier with the timestep dt. Therefore, in the
 * following code, when we use lambda, we mean (lambda * dt).
 *
 * We are using the accumulated impulse technique that is also described in the slides from Erin Catto.
 *
 * We are also using warm starting. The idea is to warm start the solver at the beginning of each step by applying the
 * last impulses for the constraints that we already existing at the previous step. This allows the iterative solver to
 * converge faster towards the solution.
 *
 * For contact constraints, we are also using split impulses so that the position correction that uses Baumgarte
 * stabilization does not change the momentum of the bodies.
 *
 * There are two ways to apply the friction constraints. Either the friction constraints are applied at each contact
 * point or they are applied only at the center of the contact manifold between two bodies. If we solve the friction
 * constraints at each contact point, we need two constraints (two tangential friction directions) and if we solve the
 * friction constraints at the center of the contact manifold, we need two constraints for tangential friction but also
 * another twist friction constraint to prevent spin of the body around the contact manifold center.
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class ContactSolver {

    // Beta value for the penetration depth position correction without split impulses
    private final static float BETA = 0.2f;

    // Beta value for the penetration depth position correction with split impulses
    private final static float BETA_SPLIT_IMPULSE = 0.2f;

    // Slop distance (allowed penetration distance between bodies)
    private final static float SLOP = 0.01f;

    // True if we solve 3 friction constraints at the contact manifold center only
    // instead of 2 friction constraints at each contact point
    private boolean isSolveFrictionAtContactManifoldCenterActive;

    // True if the split impulse position correction is active
    private boolean isSplitImpulseActive;

    // True if the warm starting of the solver is active
    private boolean isWarmStartingActive;

    // Current time step
    private float timeStep;

    // Number of contact constraints
    private int numContactManifolds;

    // Contact constraints
    private ContactManifoldSolver[] contactConstraints;

    // Reference to the map of rigid body to their index in the constrained velocities array
    private final Map<RigidBody, Integer> mMapBodyToConstrainedVelocityIndex;

    // Split angular velocities for the position contact solver (split impulse)
    private Vector3[] splitAngularVelocities;

    // Split linear velocities for the position contact solver (split impulse)
    private Vector3[] splitLinearVelocities;

    // Array of linear velocities
    private Vector3[] mLinearVelocities;

    // Array of angular velocities
    private Vector3[] mAngularVelocities;

    // Constructor
    public ContactSolver(Map<RigidBody, Integer> mapBodyToVelocityIndex) {
        isSolveFrictionAtContactManifoldCenterActive = true;
        isSplitImpulseActive = true;
        isWarmStartingActive = true;
        contactConstraints = null;
        mMapBodyToConstrainedVelocityIndex = mapBodyToVelocityIndex;
        splitAngularVelocities = null;
        splitLinearVelocities = null;
        mAngularVelocities = null;
        mLinearVelocities = null;
    }

    // Apply an impulse to the two bodies of a constraint
    private void applyImpulse(Impulse impulse, ContactManifoldSolver manifold) {

        Vector3 velocity = new Vector3();

        // Update the velocities of the bodies by applying the impulse P
        if (manifold.isBody1Moving) {
            velocity.set(impulse.getLinearImpulseBody1()).multiply(manifold.massInverseBody1);
            mLinearVelocities[manifold.indexBody1].add(velocity);
            velocity.set(manifold.inverseInertiaTensorBody1.multiply(impulse.getAngularImpulseBody1(), new Vector3()));
            mAngularVelocities[manifold.indexBody1].add(velocity);
        }
        if (manifold.isBody2Moving) {
            velocity.set(impulse.getLinearImpulseBody2()).multiply(manifold.massInverseBody2);
            mLinearVelocities[manifold.indexBody2].add(velocity);
            velocity.set(manifold.inverseInertiaTensorBody2.multiply(impulse.getAngularImpulseBody2(), new Vector3()));
            mAngularVelocities[manifold.indexBody2].add(velocity);
        }
    }

    // Apply an impulse to the two bodies of a constraint
    private void applySplitImpulse(Impulse impulse, ContactManifoldSolver manifold) {

        Vector3 velocity = new Vector3();

        // Update the velocities of the bodies by applying the impulse P
        if (manifold.isBody1Moving) {
            velocity.set(impulse.getLinearImpulseBody1()).multiply(manifold.massInverseBody1);
            splitLinearVelocities[manifold.indexBody1].add(velocity);
            velocity.set(manifold.inverseInertiaTensorBody1.multiply(impulse.getAngularImpulseBody1(), new Vector3()));
            splitAngularVelocities[manifold.indexBody1].add(velocity);
        }
        if (manifold.isBody2Moving) {
            velocity.set(impulse.getLinearImpulseBody2()).multiply(manifold.massInverseBody2);
            splitLinearVelocities[manifold.indexBody2].add(velocity);
            velocity.set(manifold.inverseInertiaTensorBody2.multiply(impulse.getAngularImpulseBody2(), new Vector3()));
            splitAngularVelocities[manifold.indexBody2].add(velocity);
        }
    }

    // Initialize the contact constraints before solving the system
    private void initializeContactConstraints() {

        // For each contact constraint
        for (int c = 0; c < numContactManifolds; c++) {

            final ContactManifoldSolver manifold = contactConstraints[c];

            // Get the inertia tensors of both bodies
            final Matrix3x3 I1 = manifold.inverseInertiaTensorBody1;
            final Matrix3x3 I2 = manifold.inverseInertiaTensorBody2;

            // If we solve the friction constraints at the center of the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive) {
                manifold.normal.zero();
            }

            // Get the velocities of the bodies
            Vector3 v1 = mLinearVelocities[manifold.indexBody1];
            Vector3 w1 = mAngularVelocities[manifold.indexBody1];
            Vector3 v2 = mLinearVelocities[manifold.indexBody2];
            Vector3 w2 = mAngularVelocities[manifold.indexBody2];

            // For each contact point constraint
            for (int i = 0; i < manifold.numContacts; i++) {

                ContactPointSolver contactPoint = manifold.contacts[i];
                ContactPoint externalContact = contactPoint.externalContact;

                // Compute the velocity difference
                Vector3 deltaV = new Vector3(new Vector3(new Vector3(v2)
                        .add(new Vector3(w2).cross(contactPoint.r2)))
                        .subtract(v1))
                        .subtract(new Vector3(w1).cross(contactPoint.r1));

                contactPoint.r1CrossN.set(new Vector3(contactPoint.r1).cross(contactPoint.normal));
                contactPoint.r2CrossN.set(new Vector3(contactPoint.r2).cross(contactPoint.normal));

                // Compute the inverse mass matrix K for the penetration constraint
                float massPenetration = 0.0f;
                if (manifold.isBody1Moving) {
                    massPenetration += manifold.massInverseBody1
                            + (I1.multiply(contactPoint.r1CrossN, new Vector3()).cross(contactPoint.r1)).dot(contactPoint.normal);
                }
                if (manifold.isBody2Moving) {
                    massPenetration += manifold.massInverseBody2
                            + (I2.multiply(contactPoint.r2CrossN, new Vector3()).cross(contactPoint.r2)).dot(contactPoint.normal);
                }
                contactPoint.inversePenetrationMass = massPenetration > 0.0f ? (1.0f / massPenetration) : 0.0f;

                // If we do not solve the friction constraints at the center of the contact manifold
                if (!isSolveFrictionAtContactManifoldCenterActive) {

                    // Compute the friction vectors
                    computeFrictionVectors(deltaV, contactPoint);

                    contactPoint.r1CrossT1.set(new Vector3(contactPoint.r1).cross(contactPoint.frictionVector1));
                    contactPoint.r1CrossT2.set(new Vector3(contactPoint.r1).cross(contactPoint.frictionVector2));
                    contactPoint.r2CrossT1.set(new Vector3(contactPoint.r2).cross(contactPoint.frictionVector1));
                    contactPoint.r2CrossT2.set(new Vector3(contactPoint.r2).cross(contactPoint.frictionVector2));

                    // Compute the inverse mass matrix K for the friction
                    // constraints at each contact point
                    float friction1Mass = 0.0f;
                    float friction2Mass = 0.0f;
                    if (manifold.isBody1Moving) {
                        friction1Mass += manifold.massInverseBody1
                                + (I1.multiply(contactPoint.r1CrossT1, new Vector3()).cross(contactPoint.r1)).dot(contactPoint.frictionVector1);
                        friction2Mass += manifold.massInverseBody1
                                + (I1.multiply(contactPoint.r1CrossT2, new Vector3()).cross(contactPoint.r1)).dot(contactPoint.frictionVector2);
                    }
                    if (manifold.isBody2Moving) {
                        friction1Mass += manifold.massInverseBody2
                                + (I2.multiply(contactPoint.r2CrossT1, new Vector3()).cross(contactPoint.r2)).dot(contactPoint.frictionVector1);
                        friction2Mass += manifold.massInverseBody2
                                + (I2.multiply(contactPoint.r2CrossT2, new Vector3()).cross(contactPoint.r2)).dot(contactPoint.frictionVector2);
                    }
                    contactPoint.inverseFriction1Mass = friction1Mass > 0.0f ? (1.0f / friction1Mass) : 0.0f;
                    contactPoint.inverseFriction2Mass = friction2Mass > 0.0f ? (1.0f / friction2Mass) : 0.0f;
                }

                // Compute the restitution velocity bias "b". We compute this here instead
                // of inside the solve() method because we need to use the velocity difference
                // at the beginning of the contact. Note that if it is a resting contact (normal
                // velocity bellow a given threshold), we do not add a restitution velocity bias
                contactPoint.restitutionBias = 0.0f;
                float deltaVDotN = deltaV.dot(contactPoint.normal);
                if (deltaVDotN < -Defaults.RESTITUTION_VELOCITY_THRESHOLD) {
                    contactPoint.restitutionBias = manifold.restitutionFactor * deltaVDotN;
                }

                // If the warm starting of the contact solver is active
                if (isWarmStartingActive) {

                    // Get the cached accumulated impulses from the previous step
                    contactPoint.penetrationImpulse = externalContact.getPenetrationImpulse();
                    contactPoint.friction1Impulse = externalContact.getFrictionImpulse1();
                    contactPoint.friction2Impulse = externalContact.getFrictionImpulse2();
                }

                // Initialize the split impulses to zero
                contactPoint.penetrationSplitImpulse = 0.0f;

                // If we solve the friction constraints at the center of the contact manifold
                if (isSolveFrictionAtContactManifoldCenterActive) {
                    manifold.normal.add(contactPoint.normal);
                }
            }

            // If we solve the friction constraints at the center of the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive) {

                manifold.normal.normalize();

                Vector3 deltaVFrictionPoint = new Vector3(
                        new Vector3(new Vector3(v2).add(new Vector3(w2).cross(manifold.r2Friction))).subtract(v1)).subtract(new Vector3(w1).cross(manifold.r1Friction));

                // Compute the friction vectors
                computeFrictionVectors(deltaVFrictionPoint, manifold);

                // Compute the inverse mass matrix K for the friction constraints at the center of
                // the contact manifold
                manifold.r1CrossT1.set(new Vector3(manifold.r1Friction).cross(manifold.frictionVector1));
                manifold.r1CrossT2.set(new Vector3(manifold.r1Friction).cross(manifold.frictionVector2));
                manifold.r2CrossT1.set(new Vector3(manifold.r2Friction).cross(manifold.frictionVector1));
                manifold.r2CrossT2.set(new Vector3(manifold.r2Friction).cross(manifold.frictionVector2));
                float friction1Mass = 0.0f;
                float friction2Mass = 0.0f;
                if (manifold.isBody1Moving) {
                    friction1Mass += manifold.massInverseBody1
                            + (I1.multiply(manifold.r1CrossT1, new Vector3()).cross(manifold.r1Friction)).dot(manifold.frictionVector1);
                    friction2Mass += manifold.massInverseBody1
                            + (I1.multiply(manifold.r1CrossT2, new Vector3()).cross(manifold.r1Friction)).dot(manifold.frictionVector2);
                }
                if (manifold.isBody2Moving) {
                    friction1Mass += manifold.massInverseBody2
                            + (I2.multiply(manifold.r2CrossT1, new Vector3()).cross(manifold.r2Friction)).dot(manifold.frictionVector1);
                    friction2Mass += manifold.massInverseBody2
                            + (I2.multiply(manifold.r2CrossT2, new Vector3()).cross(manifold.r2Friction)).dot(manifold.frictionVector2);
                }
                float frictionTwistMass = manifold.normal.dot(manifold.inverseInertiaTensorBody1.multiply(manifold.normal, new Vector3()))
                        + manifold.normal.dot(manifold.inverseInertiaTensorBody2.multiply(manifold.normal, new Vector3()));
                manifold.inverseFriction1Mass = friction1Mass > 0.0f ? (1.0f / friction1Mass) : 0.0f;
                manifold.inverseFriction2Mass = friction2Mass > 0.0f ? (1.0f / friction2Mass) : 0.0f;
                manifold.inverseTwistFrictionMass = frictionTwistMass > 0.0f ? 1.0f / frictionTwistMass : 0.0f;
            }
        }
    }

    // Compute the first friction constraint impulse
    private Impulse computeFriction1Impulse(float deltaLambda, ContactPointSolver contactPoint) {
        return new Impulse(
                new Vector3(new Vector3(contactPoint.frictionVector1).invert()).multiply(deltaLambda),
                new Vector3(new Vector3(contactPoint.r1CrossT1).invert()).multiply(deltaLambda),
                new Vector3(contactPoint.frictionVector1).multiply(deltaLambda),
                new Vector3(contactPoint.r2CrossT1).multiply(deltaLambda));
    }

    // Compute the second friction constraint impulse
    private Impulse computeFriction2Impulse(float deltaLambda, ContactPointSolver contactPoint) {
        return new Impulse(
                new Vector3(new Vector3(contactPoint.frictionVector2).invert()).multiply(deltaLambda),
                new Vector3(new Vector3(contactPoint.r1CrossT2).invert()).multiply(deltaLambda),
                new Vector3(contactPoint.frictionVector2).multiply(deltaLambda),
                new Vector3(contactPoint.r2CrossT2).multiply(deltaLambda));
    }

    // Compute the two unit orthogonal vectors "t1" and "t2" that span the tangential friction plane
    // for a contact manifold. The two vectors have to be such that : t1 x t2 = contactNormal.
    private void computeFrictionVectors(Vector3 deltaVelocity, ContactManifoldSolver contact) {

        assert (contact.normal.length() > 0.0f);

        // Compute the velocity difference vector in the tangential plane
        Vector3 normalVelocity = new Vector3(contact.normal).multiply(deltaVelocity.dot(contact.normal));
        Vector3 tangentVelocity = new Vector3(deltaVelocity).subtract(normalVelocity);

        // If the velocty difference in the tangential plane is not zero
        float lengthTangenVelocity = tangentVelocity.length();
        if (lengthTangenVelocity > Defaults.MACHINE_EPSILON) {

            // Compute the first friction vector in the direction of the tangent
            // velocity difference
            contact.frictionVector1.set(new Vector3(tangentVelocity).divide(lengthTangenVelocity));
        } else {

            // Get any orthogonal vector to the normal as the first friction vector
            contact.frictionVector1.set(new Vector3(contact.normal).setUnitOrthogonal());
        }

        // The second friction vector is computed by the cross product of the firs
        // friction vector and the contact normal
        contact.frictionVector2.set(new Vector3(contact.normal).cross(contact.frictionVector1).normalize());
    }

    // Compute the two unit orthogonal vectors "t1" and "t2" that span the tangential friction plane
    // for a contact point. The two vectors have to be such that : t1 x t2 = contactNormal.
    private void computeFrictionVectors(Vector3 deltaVelocity, ContactPointSolver contactPoint) {

        assert (contactPoint.normal.length() > 0.0f);

        // Compute the velocity difference vector in the tangential plane
        Vector3 normalVelocity = new Vector3(contactPoint.normal).multiply(deltaVelocity.dot(contactPoint.normal));
        Vector3 tangentVelocity = new Vector3(deltaVelocity).subtract(normalVelocity);

        // If the velocty difference in the tangential plane is not zero
        float lengthTangenVelocity = tangentVelocity.length();
        if (lengthTangenVelocity > Defaults.MACHINE_EPSILON) {

            // Compute the first friction vector in the direction of the tangent
            // velocity difference
            contactPoint.frictionVector1.set(new Vector3(tangentVelocity).divide(lengthTangenVelocity));
        } else {

            // Get any orthogonal vector to the normal as the first friction vector
            contactPoint.frictionVector1.set(new Vector3(contactPoint.normal).setUnitOrthogonal());
        }

        // The second friction vector is computed by the cross product of the firs
        // friction vector and the contact normal
        contactPoint.frictionVector2.set(new Vector3(contactPoint.normal).cross(contactPoint.frictionVector1).normalize());
    }

    // Compute the mixed friction coefficient from the friction coefficient of each body
    private float computeMixedFrictionCoefficient(RigidBody body1, RigidBody body2) {
        // Use the geometric mean to compute the mixed friction coefficient
        return Mathematics.Sqrt(body1.getMaterial().getFrictionCoefficient()
                * body2.getMaterial().getFrictionCoefficient());
    }

    // Compute the collision restitution factor from the restitution factor of each body
    private float computeMixedRestitutionFactor(RigidBody body1, RigidBody body2) {
        float restitution1 = body1.getMaterial().getBounciness();
        float restitution2 = body2.getMaterial().getBounciness();

        // Return the largest restitution factor
        return (restitution1 > restitution2) ? restitution1 : restitution2;
    }

    // Compute a penetration constraint impulse
    private Impulse computePenetrationImpulse(float deltaLambda, ContactPointSolver contactPoint) {
        return new Impulse(
                new Vector3(new Vector3(contactPoint.normal).invert()).multiply(deltaLambda),
                new Vector3(new Vector3(contactPoint.r1CrossN).invert()).multiply(deltaLambda),
                new Vector3(contactPoint.normal).multiply(deltaLambda),
                new Vector3(contactPoint.r2CrossN).multiply(deltaLambda));
    }

    // Clean up the constraint solver
    public void cleanup() {

        if (contactConstraints != null) {
            //delete[] mContactConstraints;
            contactConstraints = null;
        }
    }

    // Initialize the constraint solver for a given island
    public void initializeForIsland(float dt, Island island) {

        Profiler.StartProfilingBlock("ContactSolver::initializeForIsland()");

        assert (island != null);
        assert (island.getNumBodies() > 0);
        assert (island.getNumContactManifolds() > 0);
        assert (splitLinearVelocities != null);
        assert (splitAngularVelocities != null);

        // Set the current time step
        timeStep = dt;

        numContactManifolds = island.getNumContactManifolds();

        contactConstraints = new ContactManifoldSolver[numContactManifolds];
        assert (contactConstraints != null);

        // For each contact manifold of the island
        ContactManifold[] contactManifolds = island.getContactManifolds();
        for (int i = 0; i < numContactManifolds; i++) {

            ContactManifold externalManifold = contactManifolds[i];

            ContactManifoldSolver internalManifold = new ContactManifoldSolver();
            contactConstraints[i] = internalManifold;

            assert (externalManifold.getNumContactPoints() > 0);

            // Get the two bodies of the contact
            RigidBody body1 = externalManifold.getContactPoint(0).getBody1();
            RigidBody body2 = externalManifold.getContactPoint(0).getBody2();

            // Get the position of the two bodies
            Vector3 x1 = body1.getTransform().getPosition();
            Vector3 x2 = body2.getTransform().getPosition();

            // Initialize the internal contact manifold structure using the external
            // contact manifold
            internalManifold.indexBody1 = mMapBodyToConstrainedVelocityIndex.get(body1);
            internalManifold.indexBody2 = mMapBodyToConstrainedVelocityIndex.get(body2);
            internalManifold.inverseInertiaTensorBody1.set(body1.getInertiaTensorInverseWorld());
            internalManifold.inverseInertiaTensorBody2.set(body2.getInertiaTensorInverseWorld());
            internalManifold.isBody1Moving = body1.isMotionEnabled();
            internalManifold.isBody2Moving = body2.isMotionEnabled();
            internalManifold.massInverseBody1 = body1.getMassInverse();
            internalManifold.massInverseBody2 = body2.getMassInverse();
            internalManifold.numContacts = externalManifold.getNumContactPoints();
            internalManifold.restitutionFactor = computeMixedRestitutionFactor(body1, body2);
            internalManifold.frictionCoefficient = computeMixedFrictionCoefficient(body1, body2);
            internalManifold.externalContactManifold = externalManifold;

            // If we solve the friction constraints at the center of the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive) {
                internalManifold.frictionPointBody1.zero();
                internalManifold.frictionPointBody2.zero();
            }

            // For each  contact point of the contact manifold
            for (int c = 0; c < externalManifold.getNumContactPoints(); c++) {
                if (internalManifold.contacts[c] == null) {
                    internalManifold.contacts[c] = new ContactPointSolver();
                }
                ContactPointSolver contactPoint = internalManifold.contacts[c];

                // Get a contact point
                ContactPoint externalContact = externalManifold.getContactPoint(c);

                // Get the contact point on the two bodies
                Vector3 p1 = externalContact.getWorldPointOnBody1();
                Vector3 p2 = externalContact.getWorldPointOnBody2();

                contactPoint.externalContact = externalContact;
                contactPoint.normal.set(new Vector3(externalContact.getNormal()));
                contactPoint.r1.set(new Vector3(p1).subtract(x1));
                contactPoint.r2.set(new Vector3(p2).subtract(x2));
                contactPoint.penetrationDepth = externalContact.getPenetrationDepth();
                contactPoint.isRestingContact = externalContact.getIsRestingContact();
                externalContact.setIsRestingContact(true);
                contactPoint.oldFrictionVector1.set(new Vector3(externalContact.getFrictionVector1()));
                contactPoint.oldFrictionVector2.set(new Vector3(externalContact.getFrictionVector2()));
                contactPoint.penetrationImpulse = 0.0f;
                contactPoint.friction1Impulse = 0.0f;
                contactPoint.friction2Impulse = 0.0f;

                // If we solve the friction constraints at the center of the contact manifold
                if (isSolveFrictionAtContactManifoldCenterActive) {
                    internalManifold.frictionPointBody1.add(p1);
                    internalManifold.frictionPointBody2.add(p2);
                }
            }

            // If we solve the friction constraints at the center of the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive) {

                internalManifold.frictionPointBody1.divide(internalManifold.numContacts);
                internalManifold.frictionPointBody2.divide(internalManifold.numContacts);
                internalManifold.r1Friction.set(new Vector3(internalManifold.frictionPointBody1).subtract(x1));
                internalManifold.r2Friction.set(new Vector3(internalManifold.frictionPointBody2).subtract(x2));
                internalManifold.oldFrictionVector1.set(new Vector3(externalManifold.getFrictionVector1()));
                internalManifold.oldFrictionVector2.set(new Vector3(externalManifold.getFrictionVector2()));

                // If warm starting is active
                if (isWarmStartingActive) {

                    // Initialize the accumulated impulses with the previous step accumulated impulses
                    internalManifold.friction1Impulse = externalManifold.getFrictionImpulse1();
                    internalManifold.friction2Impulse = externalManifold.getFrictionImpulse2();
                    internalManifold.frictionTwistImpulse = externalManifold.getFrictionTwistImpulse();
                } else {

                    // Initialize the accumulated impulses to zero
                    internalManifold.friction1Impulse = 0.0f;
                    internalManifold.friction2Impulse = 0.0f;
                    internalManifold.frictionTwistImpulse = 0.0f;
                }
            }
        }

        // Fill-in all the matrices needed to solve the LCP problem
        initializeContactConstraints();
    }

    // Return true if the split impulses position correction technique is used for contacts
    public boolean isSplitImpulseActive() {
        return isSplitImpulseActive;
    }

    // Activate or Deactivate the split impulses for contacts
    public void setIsSplitImpulseActive(boolean isActive) {
        isSplitImpulseActive = isActive;
    }

    // Activate or deactivate the solving of friction constraints at the center of
    // the contact manifold instead of solving them at each contact point
    public void setIsSolveFrictionAtContactManifoldCenterActive(boolean isActive) {
        isSolveFrictionAtContactManifoldCenterActive = isActive;
    }

    // Set the constrained velocities arrays
    public void setConstrainedVelocitiesArrays(Vector3[] constrainedLinearVelocities, Vector3[] constrainedAngularVelocities) {
        assert (constrainedLinearVelocities != null);
        assert (constrainedAngularVelocities != null);
        mLinearVelocities = constrainedLinearVelocities;
        mAngularVelocities = constrainedAngularVelocities;
    }

    // Set the split velocities arrays
    public void setSplitVelocitiesArrays(Vector3[] splitLinearVelocities, Vector3[] splitAngularVelocities) {
        assert (splitLinearVelocities != null);
        assert (splitAngularVelocities != null);
        this.splitLinearVelocities = splitLinearVelocities;
        this.splitAngularVelocities = splitAngularVelocities;
    }

    // Solve the contacts
    public void solve() {

        Profiler.StartProfilingBlock("ContactSolver::solve()");

        float deltaLambda;
        float lambdaTemp;

        // For each contact manifold
        for (int c = 0; c < numContactManifolds; c++) {

            ContactManifoldSolver contactManifold = contactConstraints[c];

            float sumPenetrationImpulse = 0.0f;

            // Get the constrained velocities
            Vector3 v1 = mLinearVelocities[contactManifold.indexBody1];
            Vector3 w1 = mAngularVelocities[contactManifold.indexBody1];
            Vector3 v2 = mLinearVelocities[contactManifold.indexBody2];
            Vector3 w2 = mAngularVelocities[contactManifold.indexBody2];

            for (int i = 0; i < contactManifold.numContacts; i++) {

                ContactPointSolver contactPoint = contactManifold.contacts[i];

                /**
                 * --------- Penetration ---------
                 */
                // Compute J*v
                Vector3 deltaV = new Vector3(
                        new Vector3(new Vector3(v2).add(new Vector3(w2).cross(contactPoint.r2))).subtract(v1)).subtract(new Vector3(w1).cross(contactPoint.r1));
                float deltaVDotN = deltaV.dot(contactPoint.normal);
                float Jv = deltaVDotN;

                // Compute the bias "b" of the constraint
                float beta = isSplitImpulseActive ? BETA_SPLIT_IMPULSE : BETA;
                float biasPenetrationDepth = 0.0f;
                if (contactPoint.penetrationDepth > SLOP) {
                    biasPenetrationDepth = -(beta / timeStep)
                            * Math.max(0.0f, contactPoint.penetrationDepth - SLOP);
                }
                float b = biasPenetrationDepth + contactPoint.restitutionBias;

                // Compute the Lagrange multiplier lambda
                if (isSplitImpulseActive) {
                    deltaLambda = -(Jv + contactPoint.restitutionBias) * contactPoint.inversePenetrationMass;
                } else {
                    deltaLambda = -(Jv + b) * contactPoint.inversePenetrationMass;
                }
                lambdaTemp = contactPoint.penetrationImpulse;
                contactPoint.penetrationImpulse = Math.max(contactPoint.penetrationImpulse + deltaLambda, 0.0f);
                deltaLambda = contactPoint.penetrationImpulse - lambdaTemp;

                // Compute the impulse P=J^T * lambda
                Impulse impulsePenetration = computePenetrationImpulse(deltaLambda, contactPoint);

                // Apply the impulse to the bodies of the constraint
                applyImpulse(impulsePenetration, contactManifold);

                sumPenetrationImpulse += contactPoint.penetrationImpulse;

                // If the split impulse position correction is active
                if (isSplitImpulseActive) {

                    // Split impulse (position correction)
                    Vector3 v1Split = splitLinearVelocities[contactManifold.indexBody1];
                    Vector3 w1Split = splitAngularVelocities[contactManifold.indexBody1];
                    Vector3 v2Split = splitLinearVelocities[contactManifold.indexBody2];
                    Vector3 w2Split = splitAngularVelocities[contactManifold.indexBody2];
                    Vector3 deltaVSplit = new Vector3(
                            new Vector3(new Vector3(v2Split).add(new Vector3(w2Split).cross(contactPoint.r2))).subtract(v1Split)).subtract(new Vector3(w1Split).cross(contactPoint.r1));
                    float JvSplit = deltaVSplit.dot(contactPoint.normal);
                    float deltaLambdaSplit = -(JvSplit + biasPenetrationDepth) * contactPoint.inversePenetrationMass;
                    float lambdaTempSplit = contactPoint.penetrationSplitImpulse;
                    contactPoint.penetrationSplitImpulse = Math.max(contactPoint.penetrationSplitImpulse + deltaLambdaSplit, 0.0f);
                    deltaLambda = contactPoint.penetrationSplitImpulse - lambdaTempSplit;

                    // Compute the impulse P=J^T * lambda
                    Impulse splitImpulsePenetration = computePenetrationImpulse(deltaLambdaSplit, contactPoint);

                    applySplitImpulse(splitImpulsePenetration, contactManifold);
                }

                // If we do not solve the friction constraints at the center of the contact manifold
                if (!isSolveFrictionAtContactManifoldCenterActive) {

                    /**
                     * --------- Friction 1 ---------
                     */
                    // Compute J*v
                    deltaV = new Vector3(
                            new Vector3(new Vector3(v2).add(new Vector3(w2).cross(contactPoint.r2))).subtract(v1)).subtract(new Vector3(w1).cross(contactPoint.r1));
                    Jv = deltaV.dot(contactPoint.frictionVector1);

                    // Compute the Lagrange multiplier lambda
                    deltaLambda = -Jv;
                    deltaLambda *= contactPoint.inverseFriction1Mass;
                    float frictionLimit = contactManifold.frictionCoefficient * contactPoint.penetrationImpulse;
                    lambdaTemp = contactPoint.friction1Impulse;
                    contactPoint.friction1Impulse = Math.max(-frictionLimit,
                            Math.min(contactPoint.friction1Impulse + deltaLambda, frictionLimit));
                    deltaLambda = contactPoint.friction1Impulse - lambdaTemp;

                    // Compute the impulse P=J^T * lambda
                    Impulse impulseFriction1 = computeFriction1Impulse(deltaLambda, contactPoint);

                    // Apply the impulses to the bodies of the constraint
                    applyImpulse(impulseFriction1, contactManifold);

                    /**
                     * --------- Friction 2 ---------
                     */
                    // Compute J*v
                    deltaV = new Vector3(
                            new Vector3(new Vector3(v2).add(new Vector3(w2).cross(contactPoint.r2))).subtract(v1)).subtract(new Vector3(w1).cross(contactPoint.r1));
                    Jv = deltaV.dot(contactPoint.frictionVector2);

                    // Compute the Lagrange multiplier lambda
                    deltaLambda = -Jv;
                    deltaLambda *= contactPoint.inverseFriction2Mass;
                    frictionLimit = contactManifold.frictionCoefficient * contactPoint.penetrationImpulse;
                    lambdaTemp = contactPoint.friction2Impulse;
                    contactPoint.friction2Impulse = Math.max(-frictionLimit,
                            Math.min(contactPoint.friction2Impulse + deltaLambda, frictionLimit));
                    deltaLambda = contactPoint.friction2Impulse - lambdaTemp;

                    // Compute the impulse P=J^T * lambda
                    Impulse impulseFriction2 = computeFriction2Impulse(deltaLambda, contactPoint);

                    // Apply the impulses to the bodies of the constraint
                    applyImpulse(impulseFriction2, contactManifold);
                }
            }

            // If we solve the friction constraints at the center of the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive) {

                /**
                 * ------ First friction constraint at the center of the contact manifol ------
                 */
                // Compute J*v
                Vector3 deltaV = new Vector3(
                        new Vector3(new Vector3(v2).add(new Vector3(w2).cross(contactManifold.r2Friction))).subtract(v1)).subtract(new Vector3(w1).cross(contactManifold.r1Friction));
                float Jv = deltaV.dot(contactManifold.frictionVector1);

                // Compute the Lagrange multiplier lambda
                deltaLambda = -Jv * contactManifold.inverseFriction1Mass;
                float frictionLimit = contactManifold.frictionCoefficient * sumPenetrationImpulse;
                lambdaTemp = contactManifold.friction1Impulse;
                contactManifold.friction1Impulse = Math.max(-frictionLimit,
                        Math.min(contactManifold.friction1Impulse + deltaLambda, frictionLimit));
                deltaLambda = contactManifold.friction1Impulse - lambdaTemp;

                // Compute the impulse P=J^T * lambda
                Vector3 linearImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.frictionVector1).invert()).multiply(deltaLambda);
                Vector3 angularImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.r1CrossT1).invert()).multiply(deltaLambda);
                Vector3 linearImpulseBody2 = new Vector3(
                        contactManifold.frictionVector1).multiply(deltaLambda);
                Vector3 angularImpulseBody2 = new Vector3(
                        contactManifold.r2CrossT1).multiply(deltaLambda);
                Impulse impulseFriction1 = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseFriction1, contactManifold);

                /**
                 * ------ Second friction constraint at the center of the contact manifol -----
                 */
                // Compute J*v
                deltaV = new Vector3(
                        new Vector3(new Vector3(v2).add(new Vector3(w2).cross(contactManifold.r2Friction))).subtract(v1)).subtract(new Vector3(w1).cross(contactManifold.r1Friction));
                Jv = deltaV.dot(contactManifold.frictionVector2);

                // Compute the Lagrange multiplier lambda
                deltaLambda = -Jv * contactManifold.inverseFriction2Mass;
                frictionLimit = contactManifold.frictionCoefficient * sumPenetrationImpulse;
                lambdaTemp = contactManifold.friction2Impulse;
                contactManifold.friction2Impulse = Math.max(-frictionLimit,
                        Math.min(contactManifold.friction2Impulse
                                + deltaLambda, frictionLimit));
                deltaLambda = contactManifold.friction2Impulse - lambdaTemp;

                // Compute the impulse P=J^T * lambda
                linearImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.frictionVector2).invert()).multiply(deltaLambda);
                angularImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.r1CrossT2).invert()).multiply(deltaLambda);
                linearImpulseBody2 = new Vector3(
                        contactManifold.frictionVector2).multiply(deltaLambda);
                angularImpulseBody2 = new Vector3(
                        contactManifold.r2CrossT2).multiply(deltaLambda);
                Impulse impulseFriction2 = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseFriction2, contactManifold);

                /**
                 * ------ Twist friction constraint at the center of the contact manifol ------
                 */
                // Compute J*v
                deltaV = new Vector3(w2).subtract(w1);
                Jv = deltaV.dot(contactManifold.normal);

                deltaLambda = -Jv * (contactManifold.inverseTwistFrictionMass);
                frictionLimit = contactManifold.frictionCoefficient * sumPenetrationImpulse;
                lambdaTemp = contactManifold.frictionTwistImpulse;
                contactManifold.frictionTwistImpulse = Math.max(-frictionLimit,
                        Math.min(contactManifold.frictionTwistImpulse + deltaLambda, frictionLimit));
                deltaLambda = contactManifold.frictionTwistImpulse - lambdaTemp;

                // Compute the impulse P=J^T * lambda
                linearImpulseBody1 = new Vector3();
                angularImpulseBody1 = new Vector3(new Vector3(contactManifold.normal).invert()).multiply(deltaLambda);
                linearImpulseBody2 = new Vector3();
                angularImpulseBody2 = new Vector3(contactManifold.normal).multiply(deltaLambda);
                Impulse impulseTwistFriction = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseTwistFriction, contactManifold);
            }
        }
    }

    // Store the computed impulses to use them to
    // warm start the solver at the next iteration
    public void storeImpulses() {

        // For each contact manifold
        for (int c = 0; c < numContactManifolds; c++) {

            ContactManifoldSolver manifold = contactConstraints[c];

            for (int i = 0; i < manifold.numContacts; i++) {

                ContactPointSolver contactPoint = manifold.contacts[i];

                contactPoint.externalContact.setPenetrationImpulse(contactPoint.penetrationImpulse);
                contactPoint.externalContact.setFrictionImpulse1(contactPoint.friction1Impulse);
                contactPoint.externalContact.setFrictionImpulse2(contactPoint.friction2Impulse);

                contactPoint.externalContact.setFrictionVector1(contactPoint.frictionVector1);
                contactPoint.externalContact.setFrictionVector2(contactPoint.frictionVector2);
            }

            manifold.externalContactManifold.setFrictionImpulse1(manifold.friction1Impulse);
            manifold.externalContactManifold.setFrictionImpulse2(manifold.friction2Impulse);
            manifold.externalContactManifold.setFrictionTwistImpulse(manifold.frictionTwistImpulse);
            manifold.externalContactManifold.setFrictionVector1(manifold.frictionVector1);
            manifold.externalContactManifold.setFrictionVector2(manifold.frictionVector2);
        }
    }

    // Warm start the solver.
    // For each constraint, we apply the previous impulse (from the previous step)
    // at the beginning. With this technique, we will converge faster towards
    // the solution of the linear system
    public void warmStart() {

        // Check that warm starting is active
        if (!isWarmStartingActive) {
            return;
        }

        // For each constraint
        for (int c = 0; c < numContactManifolds; c++) {

            ContactManifoldSolver contactManifold = contactConstraints[c];

            boolean atLeastOneRestingContactPoint = false;

            for (int i = 0; i < contactManifold.numContacts; i++) {

                ContactPointSolver contactPoint = contactManifold.contacts[i];

                // If it is not a new contact (this contact was already existing at last time step)
                if (contactPoint.isRestingContact) {

                    atLeastOneRestingContactPoint = true;

                    /**
                     * --------- Penetration ---------
                     */
                    // Compute the impulse P = J^T * lambda
                    Impulse impulsePenetration = computePenetrationImpulse(contactPoint.penetrationImpulse, contactPoint);

                    // Apply the impulse to the bodies of the constraint
                    applyImpulse(impulsePenetration, contactManifold);

                    // If we do not solve the friction constraints at the center of the contact manifold
                    if (!isSolveFrictionAtContactManifoldCenterActive) {

                        // Project the old friction impulses (with old friction vectors) into
                        // the new friction vectors to get the new friction impulses
                        Vector3 oldFrictionImpulse = new Vector3(
                                new Vector3(contactPoint.oldFrictionVector1).multiply(contactPoint.friction1Impulse)).add(
                                        new Vector3(contactPoint.oldFrictionVector2).multiply(contactPoint.friction2Impulse));
                        contactPoint.friction1Impulse = oldFrictionImpulse.dot(contactPoint.frictionVector1);
                        contactPoint.friction2Impulse = oldFrictionImpulse.dot(contactPoint.frictionVector2);

                        /**
                         * --------- Friction 1 ---------
                         */
                        // Compute the impulse P = J^T * lambda
                        Impulse impulseFriction1 = computeFriction1Impulse(contactPoint.friction1Impulse, contactPoint);

                        // Apply the impulses to the bodies of the constraint
                        applyImpulse(impulseFriction1, contactManifold);

                        /**
                         * --------- Friction 2 ---------
                         */
                        // Compute the impulse P=J^T * lambda
                        Impulse impulseFriction2 = computeFriction2Impulse(contactPoint.friction2Impulse, contactPoint);

                        // Apply the impulses to the bodies of the constraint
                        applyImpulse(impulseFriction2, contactManifold);
                    }
                } else {  // If it is a new contact point

                    // Initialize the accumulated impulses to zero
                    contactPoint.penetrationImpulse = 0.0f;
                    contactPoint.friction1Impulse = 0.0f;
                    contactPoint.friction2Impulse = 0.0f;
                }
            }

            // If we solve the friction constraints at the center of the contact manifold and there is
            // at least one resting contact point in the contact manifold
            if (isSolveFrictionAtContactManifoldCenterActive && atLeastOneRestingContactPoint) {

                // Project the old friction impulses (with old friction vectors) into the new friction
                // vectors to get the new friction impulses
                Vector3 oldFrictionImpulse = new Vector3(
                        new Vector3(contactManifold.oldFrictionVector1).multiply(contactManifold.friction1Impulse)).add(
                                new Vector3(contactManifold.oldFrictionVector2).multiply(contactManifold.friction2Impulse));
                contactManifold.friction1Impulse = oldFrictionImpulse.dot(contactManifold.frictionVector1);
                contactManifold.friction2Impulse = oldFrictionImpulse.dot(contactManifold.frictionVector2);

                /**
                 * ------ First friction constraint at the center of the contact manifold ------
                 */
                // Compute the impulse P = J^T * lambda
                Vector3 linearImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.frictionVector1).invert()).multiply(contactManifold.friction1Impulse);
                Vector3 angularImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.r1CrossT1).invert()).multiply(contactManifold.friction1Impulse);
                Vector3 linearImpulseBody2 = new Vector3(
                        contactManifold.frictionVector1).multiply(contactManifold.friction1Impulse);
                Vector3 angularImpulseBody2 = new Vector3(
                        contactManifold.r2CrossT1).multiply(contactManifold.friction1Impulse);
                Impulse impulseFriction1 = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseFriction1, contactManifold);

                /**
                 * ------ Second friction constraint at the center of the contact manifold -----
                 */
                // Compute the impulse P = J^T * lambda
                linearImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.frictionVector2).invert()).multiply(contactManifold.friction2Impulse);
                angularImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.r1CrossT2).invert()).multiply(contactManifold.friction2Impulse);
                linearImpulseBody2 = new Vector3(
                        contactManifold.frictionVector2).multiply(contactManifold.friction2Impulse);
                angularImpulseBody2 = new Vector3(
                        contactManifold.r2CrossT2).multiply(contactManifold.friction2Impulse);
                Impulse impulseFriction2 = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseFriction2, contactManifold);

                /**
                 * ------ Twist friction constraint at the center of the contact manifold ------
                 */
                // Compute the impulse P = J^T * lambda
                linearImpulseBody1 = new Vector3();
                angularImpulseBody1 = new Vector3(
                        new Vector3(contactManifold.normal).invert()).multiply(contactManifold.frictionTwistImpulse);
                linearImpulseBody2 = new Vector3();
                angularImpulseBody2 = new Vector3(
                        contactManifold.normal).multiply(contactManifold.frictionTwistImpulse);
                Impulse impulseTwistFriction = new Impulse(linearImpulseBody1, angularImpulseBody1, linearImpulseBody2, angularImpulseBody2);

                // Apply the impulses to the bodies of the constraint
                applyImpulse(impulseTwistFriction, contactManifold);
            } else {  // If it is a new contact manifold

                // Initialize the accumulated impulses to zero
                contactManifold.friction1Impulse = 0.0f;
                contactManifold.friction2Impulse = 0.0f;
                contactManifold.frictionTwistImpulse = 0.0f;
            }
        }
    }

}
