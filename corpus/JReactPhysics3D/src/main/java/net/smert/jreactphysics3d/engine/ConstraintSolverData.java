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
import net.smert.jreactphysics3d.mathematics.Quaternion;
import net.smert.jreactphysics3d.mathematics.Vector3;

/**
 * This structure contains data from the constraint solver that are used to solve each joint constraint.
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class ConstraintSolverData {

    // True if warm starting of the solver is active
    public boolean isWarmStartingActive;

    // Current time step of the simulation
    public float timeStep;

    // Reference to the bodies orientations
    public final List<Quaternion> orientations;

    // Reference to the bodies positions
    public final List<Vector3> positions;

    // Reference to the map that associates rigid body to their index
    // in the constrained velocities array
    public final Map<RigidBody, Integer> mapBodyToConstrainedVelocityIndex;

    // Array with the bodies angular velocities
    public Vector3[] angularVelocities;

    // Array with the bodies linear velocities
    public Vector3[] linearVelocities;

    // Constructor
    public ConstraintSolverData(List<Vector3> refPositions, List<Quaternion> refOrientations,
            Map<RigidBody, Integer> refMapBodyToConstrainedVelocityIndex) {
        orientations = refOrientations;
        positions = refPositions;
        mapBodyToConstrainedVelocityIndex = refMapBodyToConstrainedVelocityIndex;
        angularVelocities = null;
        linearVelocities = null;
    }

}
