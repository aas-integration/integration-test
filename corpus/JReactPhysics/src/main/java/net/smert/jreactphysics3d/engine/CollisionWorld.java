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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.smert.jreactphysics3d.collision.BodyIndexPair;
import net.smert.jreactphysics3d.body.CollisionBody;
import net.smert.jreactphysics3d.collision.BroadPhasePair;
import net.smert.jreactphysics3d.collision.CollisionDetection;
import net.smert.jreactphysics3d.collision.shapes.CollisionShape;
import net.smert.jreactphysics3d.constraint.ContactPointInfo;
import net.smert.jreactphysics3d.mathematics.Transform;

/**
 * This class represent a world where it is possible to move bodies by hand and to test collision between each other. In
 * this kind of world, the bodies movement is not computed using the laws of physics.
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class CollisionWorld {

    // Current body ID
    protected int currentBodyID;

    // Reference to the collision detection
    protected CollisionDetection collisionDetection;

    // All the collision shapes of the world
    protected List<CollisionShape> collisionShapes;

    // List of free ID for rigid bodies
    protected List<Integer> freeBodiesIDs;

    // Broad-phase overlapping pairs of bodies
    protected Map<BodyIndexPair, OverlappingPair> overlappingPairs;

    // All the bodies (rigid and soft) of the world
    protected Set<CollisionBody> bodies;

    // Constructor
    public CollisionWorld() {
        currentBodyID = 0;
        collisionDetection = new CollisionDetection(this);
        collisionShapes = new ArrayList<>();
        freeBodiesIDs = new ArrayList<>();
        overlappingPairs = new HashMap<>();
        bodies = new HashSet<>();
    }

    // Create a new collision shape.
    // First, this methods checks that the new collision shape does not exist yet in the
    // world. If it already exists, we do not allocate memory for a new one but instead
    // we reuse the existing one. The goal is to only allocate memory for a single
    // collision shape if this one is used for several bodies in the world. To allocate
    // memory for a new collision shape, we use the memory allocator.
    protected CollisionShape createCollisionShape(CollisionShape collisionShape) {

        // Check if there is already a similar collision shape in the world
        for (CollisionShape it : collisionShapes) {

            if (collisionShape.equals(it)) {

                // Increment the number of similar created shapes
                it.incrementNumSimilarCreatedShapes();

                // A similar collision shape already exists in the world, so we do not
                // create a new one but we simply return a pointer to the existing one
                return it;
            }
        }

        // A similar collision shape does not already exist in the world, so we create a
        // new one and add it to the world
        CollisionShape newCollisionShape = collisionShape.clone();
        collisionShapes.add(newCollisionShape);

        newCollisionShape.incrementNumSimilarCreatedShapes();

        // Return a pointer to the new collision shape
        return newCollisionShape;
    }

    // Return the next available body ID
    protected int computeNextAvailableBodyID() {

        // Compute the body ID
        int bodyID;
        if (!freeBodiesIDs.isEmpty()) {
            int lastIndex = freeBodiesIDs.size() - 1;
            bodyID = freeBodiesIDs.get(lastIndex);
            freeBodiesIDs.remove(lastIndex);
        } else {
            bodyID = currentBodyID;
            currentBodyID++;
        }

        return bodyID;
    }

    // Remove a collision shape.
    // First, we check if another body is still using the same collision shape. If so,
    // we keep the allocated collision shape. If it is not the case, we can deallocate
    // the memory associated with the collision shape.
    protected void removeCollisionShape(CollisionShape collisionShape) {

        assert (collisionShape.getNumSimilarCreatedShapes() != 0);

        // Decrement the number of bodies using the same collision shape
        collisionShape.decrementNumSimilarCreatedShapes();

        // If no other body is using the collision shape in the world
        if (collisionShape.getNumSimilarCreatedShapes() == 0) {

            // Remove the shape from the set of shapes in the world
            collisionShapes.remove(collisionShape);

            // Compute the size (in bytes) of the collision shape
            // Call the destructor of the collision shape
            // Deallocate the memory used by the collision shape
        }
    }

    // Create a collision body and add it to the world
    public CollisionBody createCollisionBody(Transform transform, CollisionShape collisionShape) {

        // Get the next available body ID
        int bodyID = computeNextAvailableBodyID();

        // Largest index cannot be used (it is used for invalid index)
        assert (bodyID < Integer.MAX_VALUE);

        // Create the collision body
        CollisionBody collisionBody = new CollisionBody(transform, collisionShape, bodyID);

        assert (collisionBody != null);

        // Add the collision body to the world
        bodies.add(collisionBody);

        // Add the collision body to the collision detection
        collisionDetection.addBody(collisionBody);

        // Return the pointer to the rigid body
        return collisionBody;
    }

    // Destroy a collision body
    public void destroyCollisionBody(CollisionBody collisionBody) {

        // Remove the body from the collision detection
        collisionDetection.removeBody(collisionBody);

        // Add the body ID to the list of free IDs
        freeBodiesIDs.add(collisionBody.getBodyID());

        // Call the destructor of the collision body
        // Remove the collision body from the list of bodies
        bodies.remove(collisionBody);

        // Free the object from the memory allocator
    }

    // Return an iterator to the beginning of the bodies of the physics world
    public Set<CollisionBody> getBodies() {
        return bodies;
    }

    // Notify the world about a new broad-phase overlapping pair
    public void notifyAddedOverlappingPair(BroadPhasePair addedPair) {
    }

    // Notify the world about a new narrow-phase contact
    public void notifyNewContact(BroadPhasePair broadPhasePair, ContactPointInfo contactInfo) {
    }

    // Notify the world about a removed broad-phase overlapping pair
    public void notifyRemovedOverlappingPair(BroadPhasePair removedPair) {
    }

    // Update the overlapping pair
    public void updateOverlappingPair(BroadPhasePair pair) {
    }

}
