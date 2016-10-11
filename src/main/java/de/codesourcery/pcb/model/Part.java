package de.codesourcery.pcb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;

import com.badlogic.gdx.math.collision.BoundingBox;

public class Part 
{
	public final BoundingBox bounds = new BoundingBox();
	private final List<Port> ports = new ArrayList<>();
	
	private String name;
	
	public final Outline outline = new Outline();

	public void addPort(Port port) 
	{
		Validate.notNull(port, "port must not be NULL");
		this.ports.add( port );
	}
	
	public void visitPorts(Consumer<Port> visitor) 
	{
		ports.forEach( visitor );
	}
	
	public boolean hasPorts() {
		return ! ports.isEmpty();
	}
	
	public boolean allPortsWithinOutline() 
	{
		return ports.stream().allMatch( port -> outline.contains( port.center ) );
	}
}