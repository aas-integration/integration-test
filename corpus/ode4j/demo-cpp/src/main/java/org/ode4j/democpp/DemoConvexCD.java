/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 * Open Dynamics Engine 4J, Copyright (C) 2009-2014 Tilmann Zaeschke     *
 * All rights reserved.  Email: ode4j@gmx.de   Web: www.ode4j.org        *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT.         *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT, ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT for more   *
 * details.                                                              *
 *                                                                       *
 *************************************************************************/
package org.ode4j.democpp;

import org.ode4j.drawstuff.DrawStuff.dsFunctions;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.internal.CollideBoxBox;
import org.ode4j.ode.internal.DxConvex;

import static org.ode4j.cpp.OdeCpp.*;
import static org.ode4j.drawstuff.DrawStuff.*;
import static org.ode4j.ode.OdeMath.*;
import static org.ode4j.ode.internal.cpp4j.C_All.*;


class DemoConvexCD extends dsFunctions {
	//#ifndef M_PI
	//#define M_PI (3.14159265358979323846)
	//#endif
	//private static final double M_PI = 3.14159265358979323846; 

	//<---- Convex Object
	private double planes[]= // planes for a cube
	{
			1.0f ,0.0f ,0.0f ,0.25f,
			0.0f ,1.0f ,0.0f ,0.25f,
			0.0f ,0.0f ,1.0f ,0.25f,
			-1.0f,0.0f ,0.0f ,0.25f,
			0.0f ,-1.0f,0.0f ,0.25f,
			0.0f ,0.0f ,-1.0f,0.25f
			/*
    1.0f ,0.0f ,0.0f ,2.0f,
    0.0f ,1.0f ,0.0f ,1.0f,
    0.0f ,0.0f ,1.0f ,1.0f,
    0.0f ,0.0f ,-1.0f,1.0f,
    0.0f ,-1.0f,0.0f ,1.0f,
    -1.0f,0.0f ,0.0f ,0.0f
			 */
	};
	private final int planecount=6;

	private double points[]= // points for a cube
	{
			0.25f,0.25f,0.25f,  //  point 0
			-0.25f,0.25f,0.25f, //  point 1

			0.25f,-0.25f,0.25f, //  point 2
			-0.25f,-0.25f,0.25f,//  point 3

			0.25f,0.25f,-0.25f, //  point 4
			-0.25f,0.25f,-0.25f,//  point 5

			0.25f,-0.25f,-0.25f,//  point 6
			-0.25f,-0.25f,-0.25f,// point 7 
	};
	private final int pointcount=8;
	private int polygons[] = //Polygons for a cube (6 squares)
	{
			4,0,2,6,4, // positive X Side 0
			4,1,0,4,5, // positive Y Side 1
			4,0,1,3,2, // positive Z Side 2
			4,3,1,5,7, // negative X Side 3
			4,2,3,7,6, // negative Y Side 4
			4,5,4,6,7, // negative Z Side 5
	};
	//----> Convex Object

	private DGeom[] geoms;
	private DGeom[] boxes = new DGeom[2];
	private DGeom[] convex = new DGeom[2];
	private DSpace space;
	private DWorld world;
	private DJointGroup contactgroup;
	/* 
glRotate Matrix:
( xx(1-c)+c	xy(1-c)-zs  xz(1-c)+ys	 0  )
|					    |
| yx(1-c)+zs	yy(1-c)+c   yz(1-c)-xs	 0  |
| xz(1-c)-ys	yz(1-c)+xs  zz(1-c)+c	 0  |
|					    |
(	 0	     0		 0	 1  )
Where	c = cos(angle),	s = sine(angle), and ||( x,y,z )|| = 1
	  (if not, the GL will normalize this vector).
	 */

	private DVector3 geom1pos=new DVector3(0.0,0.250,0.50);
	private DQuaternion geom1quat=new DQuaternion(1,0,0,0);
	//private DQuaternion geom0quat=new DQuaternion(0.7071,0,0.7071,0);

	private boolean DumpInfo=true;
	private int drawmode = DS_WIREFRAME;

	//	private final double fixed_pos_0[]={0.703704,-0.748281,0.249495};
	//	private final DMatrix3C fixed_rot_0 = new DMatrix3(
	//			0.996994,-0.001009,-0.077468,0.000000,
	//			-0.077468,-0.000117,-0.996995,0.000000,
	//			0.000996, 1.000000,-0.000195,0.000000);
	//
	//	private final double fixed_pos_1[]={0.894169,-0.372081,0.249432};
	//	private final DMatrix3C fixed_rot_1 = new DMatrix3(
	//			-0.999461, 0.032777,0.001829,0.000000,
	//			-0.032777,-0.999463,0.000033,0.000000,
	//			0.001829,-0.000027,0.999998,0.000000);

	// for EDGE-EDGE test
	private final double fixed_pos_0[]={0.0,0.0,0.25};
	private final DMatrix3C fixed_rot_0=new DMatrix3( 1,0,0,0,1,0,0,0,1 );
	private final double fixed_pos_1[]={0.000000,0.450000,0.600000};
	private final DMatrix3C fixed_rot_1=new DMatrix3(0.708311,-0.705472,-0.000000,
			0.516939,0.519297,-0.679785,
			0.480067,0.481293,0.733034);

	@Override
	public void start()
	{
		// adjust the starting viewpoint a bit
		//float xyz[3],hpr[3];
		float[] xyz = new float[3], hpr = new float[3];
		dsGetViewpoint (xyz,hpr);
		hpr[0] += 7;
		dsSetViewpoint (xyz,hpr);
		convex[0]=dCreateConvex (space,
				planes,
				planecount,
				points,
				pointcount,
				polygons);
		convex[1]=dCreateConvex (space,
				planes,
				planecount,
				points,
				pointcount,
				polygons);
		boxes[0]=dCreateBox(space,0.5,0.5,0.5);
		boxes[1]=dCreateBox(space,0.5,0.5,0.5);
		geoms=convex;

        //      #if 0
		//		DMatrix3 m1 = new DMatrix3( 1,0,0,0,0,1,0,0,0,0,1,0 );
		//		DMatrix3 m2 = new DMatrix3( 1,0,0,0,0,1,0,0,0,0,1,0 );
		//		dGeomSetPosition (convex[0],
		//				0.0,
		//				0.0,
		//				0.25);
		//		dGeomSetPosition (convex[1],
		//				geom1pos[0],
		//				geom1pos[1],
		//				geom1pos[2]);
		//		dQtoR (geom0quat, m1);
		//		dGeomSetRotation (convex[0],m1);
		//		dQtoR (geom1quat, m2);
		//		dGeomSetRotation (convex[1],m2);
		//
		//		dGeomSetPosition (boxes[0],
		//				0.0,
		//				0.0,
		//				0.25);
		//		dGeomSetPosition (boxes[1],
		//				geom1pos[0],
		//				geom1pos[1],
		//				geom1pos[2]);
		//		dQtoR (geom0quat, m1);
		//		dGeomSetRotation (boxes[0],m1);
		//		dQtoR (geom1quat, m2);
		//		dGeomSetRotation (boxes[1],m2);
		//		#else
		{
			dGeomSetPosition (convex[0],
					fixed_pos_0[0],
					fixed_pos_0[1],
					fixed_pos_0[2]);
			dGeomSetPosition (convex[1],
					fixed_pos_1[0],
					fixed_pos_1[1],
					fixed_pos_1[2]);
			dGeomSetRotation (convex[0],fixed_rot_0);
			dGeomSetRotation (convex[1],fixed_rot_1);
			dGeomSetPosition (boxes[0],
					fixed_pos_0[0],
					fixed_pos_0[1],
					fixed_pos_0[2]);
			dGeomSetPosition (boxes[1],
					fixed_pos_1[0],
					fixed_pos_1[1],
					fixed_pos_1[2]);
			dGeomSetRotation (boxes[0],fixed_rot_0);
			dGeomSetRotation (boxes[1],fixed_rot_1);
		} //#endif

	}

	//	private int dCollideConvexConvex (dxGeom o1, dxGeom o2, int flags,
	//			dContactGeomBuffer contact, int skip);
	//	private int dCollideBoxBox (dxGeom o1, dxGeom o2, int flags,
	//			dContactGeomBuffer contacts, int skip);

	private void simLoop (boolean pause)
	{
		int contactcount;
		final DVector3 ss = new DVector3(0.02,0.02,0.02);
		DContactGeomBuffer contacts = new DContactGeomBuffer(8);
		if(geoms==convex)
			contactcount = new DxConvex.CollideConvexConvex().dColliderFn(geoms[0],geoms[1],8,contacts);//sizeof(dContactGeom));
		else
			contactcount = new CollideBoxBox().dColliderFn(geoms[0],geoms[1],8,contacts);//sizeof(dContactGeom));

		//fprintf(stdout,"Contact Count %d\n",contactcount);
		DVector3C pos;
		DMatrix3C R;
		dsSetTexture (DS_TEXTURE_NUMBER.DS_WOOD);
		pos = dGeomGetPosition (geoms[0]);
		R = dGeomGetRotation (geoms[0]);
		dsSetColor (0.6f,0.6f,1);
		dsSetDrawMode(drawmode);
		dsDrawConvex(pos,R,planes,
				planecount,
				points,
				pointcount,
				polygons);
		dsSetDrawMode(DS_POLYFILL);
		pos = dGeomGetPosition (geoms[1]);
		R = dGeomGetRotation (geoms[1]);
		dsSetColor (0.4f,1,1);
		dsSetDrawMode(drawmode);
		dsDrawConvex(pos,R,planes,
				planecount,
				points,
				pointcount,
				polygons);
		dsSetDrawMode(DS_POLYFILL);
		/*if (show_contacts) */
		DMatrix3 RI = new DMatrix3();
		RI.setIdentity();
		dsSetColor (1.0f,0,0);
		for(int i=0;i<contactcount;++i)
		{
			if(DumpInfo)
			{
				//DumpInfo=false;
				DContactGeom contact = contacts.get(i);
				fprintf(stdout,"Contact %d Normal %f,%f,%f Depth %f Pos %f %f %f ",
						i,
						contact.normal.get0(),
						contact.normal.get1(),
						contact.normal.get2(),
						contact.depth,
						contact.pos.get0(),
						contact.pos.get1(),
						contact.pos.get2());
				if(contact.g1==geoms[0])
				{
					fprintf(stdout,"Geoms 1 2\n");
				}
				else
				{
					fprintf(stdout,"Geoms 2 1\n");
				}
			}
			dsDrawBox (contacts.get(i).pos,RI,ss);
		}
		if(DumpInfo)
			DumpInfo=false;

	}


	@Override
	public void command (char cmd)
	{
		// note: 0.0174532925 radians = 1 degree
		DQuaternion q = new DQuaternion();
		@SuppressWarnings("unused")
		boolean changed = false;
		switch(cmd)
		{
		case 'w':
			geom1pos.add0(0.05);
			changed = true;
			break;
		case 'a':
			geom1pos.add1(-0.05);
			changed = true;
			break;
		case 's':
			geom1pos.add0(-0.05);
			changed = true;
			break;
		case 'd':
			geom1pos.add1(+0.05);
			changed = true;
			break;
		case 'e':
			geom1pos.add2(-0.05);
			changed = true;
			break;
		case 'q':
			geom1pos.add2(+0.05);
			changed = true;
			break;
		case 'i':
			dQFromAxisAndAngle (q, 0, 0, 1,0.0174532925);
			dQMultiply0(geom1quat,geom1quat,q);
			changed = true;
			break;
		case 'j':
			dQFromAxisAndAngle (q, 1, 0, 0,0.0174532925);
			dQMultiply0(geom1quat,geom1quat,q);
			changed = true;
			break;
		case 'k':
			dQFromAxisAndAngle (q, 0, 0, 1,-0.0174532925);
			dQMultiply0(geom1quat,geom1quat,q);
			changed = true;
			break;
		case 'l':
			dQFromAxisAndAngle (q, 1, 0, 0,-0.0174532925);
			dQMultiply0(geom1quat,geom1quat,q);
			changed = true;
			break;
		case 'm':
			//(drawmode!=DS_POLYFILL) ? drawmode=DS_POLYFILL : drawmode=DS_WIREFRAME;
			if (drawmode!=DS_POLYFILL) { 
				drawmode=DS_POLYFILL; 
			} else {
				drawmode=DS_WIREFRAME;
			}
			break;
		case 'n':
			if (geoms!=convex) geoms=convex; else geoms=boxes;
			if(geoms==convex)
			{
				printf("CONVEX------------------------------------------------------>\n");
			}
			else
			{
				printf("BOX--------------------------------------------------------->\n");
			}
			break;
		default:
			dsPrint ("received command %d (`%c')\n",cmd,cmd);     
		}
		//		#if 0
		//		dGeomSetPosition (geoms[1],
		//				geom1pos[0],
		//				geom1pos[1],
		//				geom1pos[2]);
		//		dQtoR (geom1quat, m);
		//		dGeomSetRotation (geoms[1],m);
		//	    if(changed)
		//	    {
		//
		//	            printf("POS: %f,%f,%f\n",geom1pos[0],geom1pos[1],geom1pos[2]);
		//	            printf("ROT:\n%f,%f,%f,%f\n%f,%f,%f,%f\n%f,%f,%f,%f\n",
		//	            m[0],m[1],m[2],m[3],
		//	            m[4],m[5],m[6],m[7],
		//	            m[8],m[9],m[10],m[11]);
		//	    }
		//		#endif
		DumpInfo=true;
	}


	public static void main(String[] args) {
		new DemoConvexCD().demo(args);
	}

	private void demo(String[] args) {
		// setup pointers to callback functions
		//dsFunctions fn = this;
		//fn.version = DS_VERSION;
		//  fn.start = &start;
		//  fn.step = &simLoop;
		//  fn.command = command;
		//  fn.stop = 0;
		//fn.path_to_textures = DRAWSTUFF_TEXTURE_PATH;	// uses default
		world = dWorldCreate();
		space = dHashSpaceCreate (null);
		contactgroup = dJointGroupCreate (0);

		// run simulation
		dsSimulationLoop (args,400,400,this);
		dJointGroupDestroy (contactgroup);
		dSpaceDestroy (space);
		dWorldDestroy (world);
	}

	@Override
	public void step(boolean pause) {
		simLoop(pause);
	}

	@Override
	public void stop() {
		// Nothing
	}
}
