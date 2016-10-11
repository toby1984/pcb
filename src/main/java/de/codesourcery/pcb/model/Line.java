package de.codesourcery.pcb.model;

import org.apache.commons.lang3.Validate;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;

public class Line 
{
	public Vector2 start;
	public Vector2 end;

	public Line(Vector2 start, Vector2 end) 
	{
		Validate.notNull(start, "start must not be NULL");
		Validate.notNull(end, "end must not be NULL");
		this.start = start;
		this.end = end;
	}

	public Line() {
		start = new Vector2();
		end = new Vector2();		
	}

	public float len() {
		return end.dst(start);
	}
	
	public Line copy(boolean copyPoints) 
	{
		if ( copyPoints ) {
			return new Line( this.start.cpy() , this.end.cpy() );
		}
		return new Line( this.start , this.end );
	}
	
	@Override
	public String toString() {
		return "("+start.x+","+start.y+") -> ("+start.x+","+start.y+")";
	}

	public void transform(Matrix3 mat) 
	{
		// TODO: This will break stuff if multiple lines share the same end/start points
		start.mul( mat );
		end.mul( mat );
	}
}