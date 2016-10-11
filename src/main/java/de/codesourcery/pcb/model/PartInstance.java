package de.codesourcery.pcb.model;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;

public class PartInstance 
{
	public Part part;
	public final Vector2 position = new Vector2();
	public float rotation = 0;
	
	private final Matrix3 matrix = new Matrix3();
	
	public PartInstance() {
		updateTransform();
	}
	
	public void updateTransform() 
	{
		Matrix3 tmp = new Matrix3();
		tmp.setToRotation( rotation );
		tmp.mul( new Matrix3().setToTranslation( position ) );
		matrix.set( tmp );
	}
}
