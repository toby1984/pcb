package de.codesourcery.pcb.model;

import java.util.function.Consumer;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;

import de.codesourcery.pcb.model.LineCollection.LineVisitor;

public class Outline 
{
	private LineCollection lines = new LineCollection();
	
	public Outline copy() {
		final Outline result = new Outline();
		result.lines = this.lines.copy();
		return result;
	}
	
	public void transform(Matrix3 mat) 
	{
		lines.transform( mat );
	}
	
	public void visitLines(Consumer<Line> visitor) {
		lines.visitLines( visitor );
	}
	
	public <T> T visitLines(LineVisitor<T> visitor) {
		return lines.visitLines( visitor );
	}
	
	public void addLine(Line line) 
	{
		lines.addLine(line.start ,line.end );
	}

	public void removeLine(Line line) 
	{
		lines.removeLine( line );
	}

	public void compact() {
		lines.compact();
	}
	
	public boolean isClosed() {
		return lines.isClosedOutline();
	}
	
	public boolean contains(Vector2 p) 
	{
		return lines.isPointInside( p );
	}
}