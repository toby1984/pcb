package de.codesourcery.pcb.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;

import de.codesourcery.pcb.model.Part;
import de.codesourcery.pcb.ui.PartEditorPanel.EditorMode;

public class MainFrame extends JFrame 
{

	private final Part part = new Part();	
	private final PartEditorPanel editor = new PartEditorPanel( part );

	public interface IModel<T> {
		
		public void setObject(T value);
		
		public T getObject();
	}
	
	public void init() 
	{
		setPreferredSize( new Dimension( 640, 480 ) );
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		getContentPane().setLayout( new GridBagLayout() );
		
		// add toolbar
		final JToolBar toolbar = new JToolBar( JToolBar.HORIZONTAL );
		
		final JToggleButton moveButton = new JToggleButton( "Move" );
		final JToggleButton drawButton = new JToggleButton( "Draw" );
		final JToggleButton selectButton = new JToggleButton( "Select" );
		final JToggleButton addPortButton = new JToggleButton( "Add port" );
		
		final JCheckBox snapToGrid = new JCheckBox("Snap to grid?");
		final JCheckBox onlyRightAngles = new JCheckBox("Only right angles?");
		final JCheckBox showGuide = new JCheckBox("Show guide?");
		
		System.out.println("Only right angles: "+editor.isOnlyRightAngles());
		onlyRightAngles.setSelected( editor.isOnlyRightAngles() );
		showGuide.setSelected( editor.isShowGuide() );
		snapToGrid.setSelected( editor.isSnapToGrid());
		
		toolbar.setFocusable( false );
		toolbar.setRequestFocusEnabled( false );		
		
		final List<JComponent> toolbarItems = new ArrayList<>();
		
		toolbarItems.addAll( Arrays.asList( moveButton , drawButton , selectButton, addPortButton ) );
		toolbarItems.addAll( Arrays.asList( snapToGrid , onlyRightAngles , showGuide ) );
		
		toolbarItems.forEach( button -> button.setFocusable( false ) );
		toolbarItems.forEach( button -> button.setRequestFocusEnabled( false ) );
		
		toolbarItems.forEach( toolbar::add );
		
		final JToggleButton selectedButton;
		switch ( editor.getMode() ) 
		{
			case EDIT_OUTLINE  : selectedButton = drawButton; break;
			case MOVE  : selectedButton = moveButton; break;
			case SELECT: selectedButton = selectButton; break;
			default:
				throw new RuntimeException("Unhandled mode: "+editor.getMode());
		}
		
		selectedButton.setSelected( true );
		
		final Consumer<JToggleButton> activateButton = button ->  
		{
			toolbarItems.stream().filter( but -> but instanceof JToggleButton && (but instanceof JCheckBox) == false && but != button ).forEach( but -> ((JToggleButton) but).setSelected( false ) );
		};
		
		activateButton.accept( selectedButton );
		
		showGuide.addActionListener( ev -> editor.setShowGuide( showGuide.isSelected() ) );
		
		onlyRightAngles.addActionListener( ev -> editor.setOnlyRightAngles( onlyRightAngles.isSelected() ) );
		snapToGrid.addActionListener( ev -> editor.setSnapToGrid( snapToGrid.isSelected() ) );
		
		moveButton.addActionListener( ev -> 
		{
			if ( moveButton.isSelected() ) 
			{
				activateButton.accept( moveButton );
				editor.setMode( EditorMode.MOVE );
			}
		});
		drawButton.addActionListener( ev -> 
		{
			if ( drawButton.isSelected() ) 
			{
				activateButton.accept( drawButton );
				editor.setMode( EditorMode.EDIT_OUTLINE);
			}
		});
		selectButton.addActionListener( ev -> 
		{
			if ( selectButton.isSelected() ) 
			{
				activateButton.accept( selectButton );
				editor.setMode( EditorMode.SELECT );
			}
		});		
		addPortButton.addActionListener( ev -> 
		{
			if ( addPortButton.isSelected() ) 
			{
				activateButton.accept( addPortButton );
				editor.setMode( EditorMode.EDIT_PORTS );
			}
		});				

		GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.gridwidth = 1;
		cnstrs.gridheight = 1;
		cnstrs.gridx = 0;
		cnstrs.gridy = 0;		
		cnstrs.weightx = 1;
		cnstrs.weighty = 0.05d;
		cnstrs.fill = GridBagConstraints.BOTH;		
		
		getContentPane().add( toolbar , cnstrs );
		
		// editor 
		
		cnstrs = new GridBagConstraints();
		cnstrs.gridwidth = 1;
		cnstrs.gridheight = 1;
		cnstrs.gridx = 0;
		cnstrs.gridy = 1;
		cnstrs.weightx = 1;
		cnstrs.weighty = 0.95;
		cnstrs.fill = GridBagConstraints.BOTH;
		getContentPane().add( editor , cnstrs );
		
		// setup
		pack();
		setLocationRelativeTo( null );
		setVisible( true );
		editor.setFocusable( true );
		editor.setRequestFocusEnabled( true );
		editor.requestFocusInWindow();
	}
}