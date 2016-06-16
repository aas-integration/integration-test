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

import java.util.List;
import java.util.Map;
import net.smert.jreactphysics3d.body.RigidBody;
import net.smert.jreactphysics3d.constraint.Joint;
import net.smert.jreactphysics3d.mathematics.Quaternion;
import net.smert.jreactphysics3d.mathematics.Vector3;

/**
 * This class represents the constraint solver that is used to solve constraints between the rigid bodies. The
 * constraint solver is based on the "Sequential Impulse" technique described by Erin Catto in his GDC slides
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
 * last impulstes for the constraints that we already existing at the previous step. This allows the iterative solver to
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
public class ConstraintSolver {

    // Current time step
    private float timeStep;

    // True if the warm starting of the solver is active
    private boolean isWarmStartingActive;

    // Constraint solver data used to initialize and solve the constraints
    private final ConstraintSolverData mConstraintSolverData;

    // Reference to the array of bodies positions (for position error correction)
    private final List<Vector3> positions;

    // Reference to the array of bodies orientations (for position error correction)
    private final List<Quaternion> orientations;

    // Reference to the map that associates rigid body to their index in
    // the constrained velocities array
    private final Map<RigidBody, Integer> mMapBodyToConstrainedVelocityIndex;

    // Array of constrained angular velocities (state of the angular velocities
    // after solving the constraints)
    private Vector3[] angularVelocities;

    // Array of constrained linear velocities (state of the linear velocities
    // after solving the constraints)
    private Vector3[] linearVelocities;

    // Constructor
    public ConstraintSolver(List<Vector3> positions, List<Quaternion> orientations, Map<RigidBody, Integer> mapBodyToVelocityIndex) {
        isWarmStartingActive = true;
        mConstraintSolverData = new ConstraintSolverData(positions, orientations, mapBodyToVelocityIndex);
        this.orientations = orientations;
        this.positions = positions;
        mMapBodyToConstrainedVelocityIndex = mapBodyToVelocityIndex;
        angularVelocities = null;
        linearVelocities = null;
    }

    // Return true if the Non-Linear-Gauss-Seidel position correction technique is active
    //public boolean getIsNonLinearGaussSeidelPositionCorrectionActive();
    // Enable/Disable the Non-Linear-Gauss-Seidel position correction technique.
    public void setIsNonLinearGaussSeidelPositionCorrectionActive(boolean isActive) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // Initialize the constraint solver for a given island
    public void initializeForIsland(float dt, Island island) {

        Profiler.StartProfilingBlock("ConstraintSolver::initializeForIsland()");

        assert (linearVelocities != null);
        assert (angularVelocities != null);
        assert (island != null);
        assert (island.getNumBodies() > 0);
        assert (island.getNumJoints() > 0);

        // Set the current time step
        timeStep = dt;

        // Initialize the constraint solver data used to initialize and solve the constraints
        mConstraintSolverData.timeStep = timeStep;
        mConstraintSolverData.isWarmStartingActive = isWarmStartingActive;

        // For each joint of the island
        Joint[] joints = island.getJoints();
        for (int i = 0; i < island.getNumJoints(); i++) {

            // Initialize the constraint before solving it
            joints[i].initBeforeSolve(mConstraintSolverData);

            // Warm-start the constraint if warm-starting is enabled
            if (isWarmStartingActive) {
                joints[i].warmstart(mConstraintSolverData);
            }
        }
    }

    // Set the constrained velocities arrays
    public void setConstrainedVelocitiesArrays(Vector3[] constrainedLinearVelocities, Vector3[] constrainedAngularVelocities) {
        assert (constrainedLinearVelocities != null);
        assert (constrainedAngularVelocities != null);
        linearVelocities = constrainedLinearVelocities;
        angularVelocities = constrainedAngularVelocities;
        mConstraintSolverData.linearVelocities = linearVelocities;
        mConstraintSolverData.angularVelocities = angularVelocities;
    }

    // Solve the position constraints
    public void solvePositionConstraints(Island island) {

        Profiler.StartProfilingBlock("ConstraintSolver::solvePositionConstraints()");

        assert (island != null);
        assert (island.getNumJoints() > 0);

        // For each joint of the island
        Joint[] joints = island.getJoints();
        for (int i = 0; i < island.getNumJoints(); i++) {

            // Solve the constraint
            joints[i].solvePositionConstraint(mConstraintSolverData);
        }
    }

    // Solve the velocity constraints
    public void solveVelocityConstraints(Island island) {

        Profiler.StartProfilingBlock("ConstraintSolver::solveVelocityConstraints()");

        assert (island != null);
        assert (island.getNumJoints() > 0);

        // For each joint of the island
        Joint[] joints = island.getJoints();
        for (int i = 0; i < island.getNumJoints(); i++) {

            // Solve the constraint
            joints[i].solveVelocityConstraint(mConstraintSolverData);
        }
    }

}
