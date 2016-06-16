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

import net.smert.jreactphysics3d.body.CollisionBody;
import net.smert.jreactphysics3d.configuration.Defaults;
import net.smert.jreactphysics3d.constraint.ContactPoint;
import net.smert.jreactphysics3d.mathematics.Transform;
import net.smert.jreactphysics3d.mathematics.Vector3;

/**
 * This class represents the set of contact points between two bodies. The contact manifold is implemented in a way to
 * cache the contact points among the frames for better stability following the "Contact Generation" presentation of
 * Erwin Coumans at GDC 2010 conference (bullet.googlecode.com/files/GDC10_Coumans_Erwin_Contact.pdf). Some code of this
 * class is based on the implementation of the btPersistentManifold class from Bullet physics engine
 * (www.http://bulletphysics.org). The contacts between two bodies are added one after the other in the cache. When the
 * cache is full, we have to remove one point. The idea is to keep the point with the deepest penetration depth and also
 * to keep the points producing the larger area (for a more stable contact manifold). The new added point is always
 * kept.
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class ContactManifold {

    public final static int MAX_CONTACT_POINTS_IN_MANIFOLD = 4;

    // True if the contact manifold has already been added into an island
    private boolean isAlreadyInIsland;

    // First friction raint accumulated impulse
    private float frictionImpulse1;

    // Second friction raint accumulated impulse
    private float frictionImpulse2;

    // Twist friction raint accumulated impulse
    private float frictionTwistImpulse;

    // Number of contacts in the cache
    private int numContactPoints;

    // Pointer to the first body of the contact
    private final CollisionBody body1;

    // Pointer to the second body of the contact
    private final CollisionBody body2;

    // Contact points in the manifold
    private final ContactPoint[] contactPoints = new ContactPoint[MAX_CONTACT_POINTS_IN_MANIFOLD];

    // First friction vector of the contact manifold
    private final Vector3 frictionVector1 = new Vector3();

    // Second friction vector of the contact manifold
    private final Vector3 frictionVector2 = new Vector3();

    // Constructor
    public ContactManifold(CollisionBody body1, CollisionBody body2) {
        isAlreadyInIsland = false;
        frictionImpulse1 = 0.0f;
        frictionImpulse2 = 0.0f;
        frictionTwistImpulse = 0.0f;
        numContactPoints = 0;
        this.body1 = body1;
        this.body2 = body2;
    }

    // Return the index of the contact point with the larger penetration depth.
    // This corresponding contact will be kept in the cache. The method returns -1 is
    // the new contact is the deepest.
    private int getIndexOfDeepestPenetration(ContactPoint newContact) {
        assert (numContactPoints == MAX_CONTACT_POINTS_IN_MANIFOLD);
        int indexMaxPenetrationDepth = -1;
        float maxPenetrationDepth = newContact.getPenetrationDepth();

        // For each contact in the cache
        for (int i = 0; i < numContactPoints; i++) {

            // If the current contact has a larger penetration depth
            if (contactPoints[i].getPenetrationDepth() > maxPenetrationDepth) {
                maxPenetrationDepth = contactPoints[i].getPenetrationDepth();
                indexMaxPenetrationDepth = i;
            }
        }

        // Return the index of largest penetration depth
        return indexMaxPenetrationDepth;
    }

    // Return the index that will be removed.
    // The index of the contact point with the larger penetration
    // depth is given as a parameter. This contact won't be removed. Given this contact, we compute
    // the different area and we want to keep the contacts with the largest area. The new point is also
    // kept. In order to compute the area of a quadrilateral, we use the formula :
    // Area = 0.5 * | AC x BD | where AC and BD form the diagonals of the quadrilateral. Note that
    // when we compute this area, we do not calculate it exactly but we
    // only estimate it because we do not compute the actual diagonals of the quadrialteral. Therefore,
    // this is only a guess that is faster to compute. This idea comes from the Bullet Physics library
    // by Erwin Coumans (http://wwww.bulletphysics.org).
    private int getIndexToRemove(int indexMaxPenetration, Vector3 newPoint) {

        assert (numContactPoints == MAX_CONTACT_POINTS_IN_MANIFOLD);

        float area0 = 0.0f;       // Area with contact 1,2,3 and newPoint
        float area1 = 0.0f;       // Area with contact 0,2,3 and newPoint
        float area2 = 0.0f;       // Area with contact 0,1,3 and newPoint
        float area3 = 0.0f;       // Area with contact 0,1,2 and newPoint

        if (indexMaxPenetration != 0) {
            // Compute the area
            Vector3 vector1 = new Vector3(newPoint).subtract(contactPoints[1].getLocalPointOnBody1());
            Vector3 vector2 = new Vector3(contactPoints[3].getLocalPointOnBody1()).subtract(contactPoints[2].getLocalPointOnBody1());
            Vector3 crossProduct = new Vector3(vector1).cross(vector2);
            area0 = crossProduct.lengthSquare();
        }
        if (indexMaxPenetration != 1) {
            // Compute the area
            Vector3 vector1 = new Vector3(newPoint).subtract(contactPoints[0].getLocalPointOnBody1());
            Vector3 vector2 = new Vector3(contactPoints[3].getLocalPointOnBody1()).subtract(contactPoints[2].getLocalPointOnBody1());
            Vector3 crossProduct = new Vector3(vector1).cross(vector2);
            area1 = crossProduct.lengthSquare();
        }
        if (indexMaxPenetration != 2) {
            // Compute the area
            Vector3 vector1 = new Vector3(newPoint).subtract(contactPoints[0].getLocalPointOnBody1());
            Vector3 vector2 = new Vector3(contactPoints[3].getLocalPointOnBody1()).subtract(contactPoints[1].getLocalPointOnBody1());
            Vector3 crossProduct = new Vector3(vector1).cross(vector2);
            area2 = crossProduct.lengthSquare();
        }
        if (indexMaxPenetration != 3) {
            // Compute the area
            Vector3 vector1 = new Vector3(newPoint).subtract(contactPoints[0].getLocalPointOnBody1());
            Vector3 vector2 = new Vector3(contactPoints[2].getLocalPointOnBody1()).subtract(contactPoints[1].getLocalPointOnBody1());
            Vector3 crossProduct = new Vector3(vector1).cross(vector2);
            area3 = crossProduct.lengthSquare();
        }

        // Return the index of the contact to remove
        return getMaxArea(area0, area1, area2, area3);
    }

    // Return the index of maximum area
    private int getMaxArea(float area0, float area1, float area2, float area3) {
        if (area0 < area1) {
            if (area1 < area2) {
                if (area2 < area3) {
                    return 3;
                } else {
                    return 2;
                }
            } else {
                if (area1 < area3) {
                    return 3;
                } else {
                    return 1;
                }
            }
        } else {
            if (area0 < area2) {
                if (area2 < area3) {
                    return 3;
                } else {
                    return 2;
                }
            } else {
                if (area0 < area3) {
                    return 3;
                } else {
                    return 0;
                }
            }
        }
    }

    // Remove a contact point from the manifold
    private void removeContactPoint(int index) {
        assert (index < numContactPoints);
        assert (numContactPoints > 0);

        // Call the destructor explicitly and tell the memory allocator that
        // the corresponding memory block is now free
        // If we don't remove the last index
        if (index < numContactPoints - 1) {
            contactPoints[index] = contactPoints[numContactPoints - 1];
        }

        numContactPoints--;
    }

    // Add a contact point in the manifold
    public void addContactPoint(ContactPoint contact) {

        // For contact already in the manifold
        for (int i = 0; i < numContactPoints; i++) {

            // Check if the new point point does not correspond to a same contact point
            // already in the manifold.
            float distance = new Vector3(contactPoints[i].getWorldPointOnBody1()).subtract(contact.getWorldPointOnBody1()).lengthSquare();
            if (distance <= Defaults.PERSISTENT_CONTACT_DIST_THRESHOLD * Defaults.PERSISTENT_CONTACT_DIST_THRESHOLD) {

                // Delete the new contact
                return;
                //break;
            }
        }

        // If the contact manifold is full
        if (numContactPoints == MAX_CONTACT_POINTS_IN_MANIFOLD) {
            int indexMaxPenetration = getIndexOfDeepestPenetration(contact);
            int indexToRemove = getIndexToRemove(indexMaxPenetration, contact.getLocalPointOnBody1());
            removeContactPoint(indexToRemove);
        }

        // Add the new contact point in the manifold
        contactPoints[numContactPoints] = contact;
        numContactPoints++;
    }

    // Clear the contact manifold
    public void clear() {
        for (int i = 0; i < numContactPoints; i++) {

            // Call the destructor explicitly and tell the memory allocator that
            // the corresponding memory block is now free
        }
        numContactPoints = 0;
    }

    // Return the first friction accumulated impulse
    public float getFrictionImpulse1() {
        return frictionImpulse1;
    }

    // Set the first friction accumulated impulse
    public void setFrictionImpulse1(float frictionImpulse1) {
        this.frictionImpulse1 = frictionImpulse1;
    }

    // Return the second friction accumulated impulse
    public float getFrictionImpulse2() {
        return frictionImpulse2;
    }

    // Set the second friction accumulated impulse
    public void setFrictionImpulse2(float frictionImpulse2) {
        this.frictionImpulse2 = frictionImpulse2;
    }

    // Return the friction twist accumulated impulse
    public float getFrictionTwistImpulse() {
        return frictionTwistImpulse;
    }

    // Set the friction twist accumulated impulse
    public void setFrictionTwistImpulse(float frictionTwistImpulse) {
        this.frictionTwistImpulse = frictionTwistImpulse;
    }

    // Return the number of contact points in the manifold
    public int getNumContactPoints() {
        return numContactPoints;
    }

    // Return a pointer to the first body of the contact manifold
    public CollisionBody getBody1() {
        return body1;
    }

    // Return a pointer to the second body of the contact manifold
    public CollisionBody getBody2() {
        return body2;
    }

    // Return a contact point of the manifold
    public ContactPoint getContactPoint(int index) {
        assert (index >= 0 && index < numContactPoints);
        return contactPoints[index];
    }

    // Return the first friction vector at the center of the contact manifold
    public Vector3 getFrictionVector1() {
        return frictionVector1;
    }

    // set the first friction vector at the center of the contact manifold
    public void setFrictionVector1(Vector3 frictionVector1) {
        this.frictionVector1.set(frictionVector1);
    }

    // Return the second friction vector at the center of the contact manifold
    public Vector3 getFrictionVector2() {
        return frictionVector2;
    }

    // set the second friction vector at the center of the contact manifold
    public void setFrictionVector2(Vector3 frictionVector2) {
        this.frictionVector2.set(frictionVector2);
    }

    // Return true if the contact manifold has already been added into an island
    public boolean isAlreadyInIsland() {
        return isAlreadyInIsland;
    }

    public void setIsAlreadyInIsland(boolean isAlreadyInIsland) {
        this.isAlreadyInIsland = isAlreadyInIsland;
    }

    // Update the contact manifold
    // First the world space coordinates of the current contacts in the manifold are recomputed from
    // the corresponding transforms of the bodies because they have moved. Then we remove the contacts
    // with a negative penetration depth (meaning that the bodies are not penetrating anymore) and also
    // the contacts with a too large distance between the contact points in the plane orthogonal to the
    // contact normal.
    public void update(Transform transform1, Transform transform2) {

        if (numContactPoints == 0) {
            return;
        }

        // Update the world coordinates and penetration depth of the contact points in the manifold
        for (int i = 0; i < numContactPoints; i++) {
            contactPoints[i].setWorldPointOnBody1(transform1.multiply(contactPoints[i].getLocalPointOnBody1(), new Vector3()));
            contactPoints[i].setWorldPointOnBody2(transform2.multiply(contactPoints[i].getLocalPointOnBody2(), new Vector3()));
            contactPoints[i].setPenetrationDepth(new Vector3(contactPoints[i].getWorldPointOnBody1()).subtract(
                    contactPoints[i].getWorldPointOnBody2()).dot(contactPoints[i].getNormal()));
        }

        float squarePersistentContactThreshold = Defaults.PERSISTENT_CONTACT_DIST_THRESHOLD * Defaults.PERSISTENT_CONTACT_DIST_THRESHOLD;

        // Remove the contact points that don't represent very well the contact manifold
        for (int i = (int) (numContactPoints) - 1; i >= 0; i--) {
            assert (i < (int) (numContactPoints));

            // Compute the distance between contact points in the normal direction
            float distanceNormal = -contactPoints[i].getPenetrationDepth();

            // If the contacts points are too far from each other in the normal direction
            if (distanceNormal > squarePersistentContactThreshold) {
                removeContactPoint(i);
            } else {
                // Compute the distance of the two contact points in the plane
                // orthogonal to the contact normal
                Vector3 projOfPoint1 = new Vector3(contactPoints[i].getWorldPointOnBody1()).add(
                        new Vector3(contactPoints[i].getNormal()).multiply(distanceNormal));
                Vector3 projDifference = new Vector3(contactPoints[i].getWorldPointOnBody2()).subtract(projOfPoint1);

                // If the orthogonal distance is larger than the valid distance
                // threshold, we remove the contact
                if (projDifference.lengthSquare() > squarePersistentContactThreshold) {
                    removeContactPoint(i);
                }
            }
        }
    }

}
