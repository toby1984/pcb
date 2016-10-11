package de.codesourcery.pcb.model;

import java.util.ArrayList;
import java.util.List;

public class Model 
{
	private final List<PartInstance> parts = new ArrayList<>();
	
	public List<PartInstance> getParts() {
		return parts;
	}
}