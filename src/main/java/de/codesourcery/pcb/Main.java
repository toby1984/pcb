package de.codesourcery.pcb;

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import de.codesourcery.pcb.ui.MainFrame;

public class Main
{
	public static void main(String[] args) throws InvocationTargetException, InterruptedException 
	{
		SwingUtilities.invokeAndWait( () -> new MainFrame().init() );
	}
}