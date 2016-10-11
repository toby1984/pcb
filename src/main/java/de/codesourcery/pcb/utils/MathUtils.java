package de.codesourcery.pcb.utils;

import com.badlogic.gdx.math.Vector2;

public class MathUtils {

	// solution shamelessly adopted from http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect
	
	// Returns 1 if the lines intersect, otherwise 0. In addition, if the lines 
	// intersect the intersection point may be stored in the floats i_x and i_y.
	public static boolean getIntersection(Vector2 p0,Vector2 p1,Vector2 p2,Vector2 p3,Vector2 intersectionPoint)
	{
		final float s1_x = p1.x - p0.x;     
	    final float s1_y = p1.y - p0.y;
	    final float s2_x = p3.x - p2.x;     
	    final float s2_y = p3.y - p2.y;

	    final float s = (-s1_y * (p0.x - p2.x) + s1_x * (p0.y - p2.y)) / (-s2_x * s1_y + s1_x * s2_y);
	    final float t = ( s2_x * (p0.y - p2.y) - s2_y * (p0.x - p2.x)) / (-s2_x * s1_y + s1_x * s2_y);

	    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
	    {
	        // Collision detected
	    	intersectionPoint.x = p0.x + (t * s1_x);
	    	intersectionPoint.y = p0.y + (t * s1_y);
	        return true;
	    }
	    return false;
	}
	
	public static boolean intersect(Vector2 p0,Vector2 p1,Vector2 p2,Vector2 p3)
	{
		final float s1_x = p1.x - p0.x;     
	    final float s1_y = p1.y - p0.y;
	    final float s2_x = p3.x - p2.x;     
	    final float s2_y = p3.y - p2.y;

	    final float s = (-s1_y * (p0.x - p2.x) + s1_x * (p0.y - p2.y)) / (-s2_x * s1_y + s1_x * s2_y);
	    final float t = ( s2_x * (p0.y - p2.y) - s2_y * (p0.x - p2.x)) / (-s2_x * s1_y + s1_x * s2_y);

	    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
	    {
	        return true;
	    }
	    return false;
	}	
}
