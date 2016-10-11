package de.codesourcery.pcb.ui;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;

import javax.swing.JPanel;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.Validate;

import com.badlogic.gdx.math.Vector2;

import de.codesourcery.pcb.model.Line;
import de.codesourcery.pcb.model.LineCollection.LineVisitor;
import de.codesourcery.pcb.model.Part;
import de.codesourcery.pcb.model.Port;

public class PartEditorPanel extends JPanel
{
	public static final int SNAP_RADIUS = 5; // pixels
	
	public static final int PORT_RADIUS = 10; // pixels
	public static final float PORT_CROSSHAIR_SIZE = 1.5f;

	private final Part part;

	// editing
	private Highlight highlight;
	
	private int gridX = 15;
	private int gridY = 15;
	
	private boolean showGuide = true;
	private boolean onlyRightAngles = true;
	private boolean snapToGrid=true;

	public EditorMode currentMode = EditorMode.SELECT;	
	private EditorBehaviour modeImpl = getBehaviour(currentMode);

	private final Vector2 unalignedLastMousePosition = new Vector2();
	private final Vector2 alignedLastMousePosition = new Vector2();
	
	private Selection currentSelection;
	
	private final static float dash1[] = {3.0f};
	private static final Stroke GUIDE_STROKE =  new BasicStroke(1.0f,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, dash1, 0.0f);
	private static final Color GUIDE_COLOR = new Color( 0.2f, 0.2f, 0.2f, 0.1f );

	public static enum EditorMode 
	{
		MOVE,
		SELECT,
		EDIT_OUTLINE,
		EDIT_PORTS;
	}

	interface Draggable 
	{
		public boolean move(float dx,float dy);

		public Vector2 getIgnoredPoint();
	}

	interface Highlight 
	{
		public void render(Graphics2D gfx);

		public boolean isSame(Highlight other);
	}
	
	protected static final class PortHighlight implements Highlight 
	{
		public final Port port;
		
		public PortHighlight(Port port) {
			Validate.notNull(port, "port must not be NULL");
			this.port = port;
		}

		@Override
		public void render(Graphics2D gfx) {
			gfx.setColor(Color.RED);
			renderPort( port , gfx );
		}

		@Override
		public boolean isSame(Highlight other) 
		{
			return other instanceof PortHighlight && this.port.center.equals( ((PortHighlight) other).port.center );
		}
	}
	
	protected final class PortDraggable implements Draggable 
	{
		public final Port port;
		private final Vector2 tmp=new Vector2();
		
		public PortDraggable(Port port) {
			Validate.notNull(port, "port must not be NULL");
			this.port = port;
		}

		@Override
		public boolean move(float dx, float dy) 
		{
			tmp.set( port.center.x + dx , port.center.y + dy );
			
			if ( part.outline.contains( tmp ) ) {
				port.center.add(dx,dy);
				return true;
			}
			return false;
		}

		@Override
		public Vector2 getIgnoredPoint() {
			return port.center;
		}
	}

	protected static class DraggableCorner implements Draggable 
	{
		private final Selection selection;
		private final Corner corner;

		public DraggableCorner(Selection selection, Corner corner) {
			this.selection = selection;
			this.corner = corner;
		}

		public boolean move(float dx,float dy) 
		{
			float minX = selection.topLeft.x;
			float minY = selection.topLeft.y;

			float maxX = selection.bottomRight.x;
			float maxY = selection.bottomRight.y;			
			switch( corner ) 
			{
				case TOP_LEFT:
					minX += dx;
					minY += dy;
					break;
				case BOTTOM_RIGHT:
					maxX += dx;
					maxY += dy;
					break;
				case BOTTOM_LEFT:
					minX += dx;
					maxY += dy;
					break;
				case TOP_RIGHT:
					maxX += dx;
					minY += dy;
					break;
				default:
					throw new RuntimeException("Unhandled switch/case: "+corner);				
			}
			
			float width = maxX - minX;
			float height = maxY - minY;
			
			if ( height >= 5 && width >= 5 ) 
			{
				selection.topLeft.set( minX , minY );
				selection.bottomRight.set( maxX , maxY );
				return true;
			}
			return false;
		}

		@Override
		public Vector2 getIgnoredPoint() {
			return null;
		}
	}
	
	protected static final class Selection implements Draggable 
	{
		public final Vector2 topLeft=new Vector2();
		public final Vector2 bottomRight=new Vector2();

		private final Vector2 tmp = new Vector2();

		@Override
		public boolean move(float dx, float dy) 
		{
			topLeft.add(dx,dy);
			bottomRight.add(dx,dy);
			return true;
		}

		public DraggableCorner getCorner(float x,float y) 
		{
			Corner corner = Corner.TOP_LEFT;
			float minDist,d; 
			minDist = d = getCorner( Corner.TOP_LEFT    ).dst( x , y );

			d = getCorner( Corner.TOP_RIGHT   ).dst( x , y );
			if ( d < minDist ) {
				minDist = d ; corner = Corner.TOP_RIGHT;
			}
			d = getCorner( Corner.BOTTOM_LEFT ).dst( x , y );
			if ( d < minDist ) {
				minDist = d ; corner = Corner.BOTTOM_LEFT;
			}			
			d = getCorner( Corner.BOTTOM_RIGHT).dst( x , y );
			if ( d < minDist ) {
				minDist = d ; corner = Corner.BOTTOM_RIGHT;
			}			
			return minDist < SNAP_RADIUS ? new DraggableCorner(this,corner) : null;
		}

		private Vector2 getCorner(Corner corner) 
		{
			switch( corner ) 
			{
				case BOTTOM_LEFT:
					tmp.set( topLeft.x , bottomRight.y );
					break;
				case BOTTOM_RIGHT:
					tmp.set( bottomRight.x , bottomRight.y );
					break;
				case TOP_LEFT:
					tmp.set( topLeft.x , topLeft.y );
					break;
				case TOP_RIGHT:
					tmp.set( bottomRight.x , topLeft.y );
					break;
				default:
					throw new RuntimeException("Unhandled switch/case: "+corner);
			}
			return tmp;
		}

		@Override
		public Vector2 getIgnoredPoint() {
			return null;
		}

		public boolean contains(Vector2 p) 
		{
			float minX = Math.min( topLeft.x , bottomRight.x );
			float maxX = Math.max( topLeft.x , bottomRight.x );

			float minY = Math.min( topLeft.y , bottomRight.y );
			float maxY = Math.max( topLeft.y , bottomRight.y );

			return minX <= p.x && p.x < maxX &&
					minY <= p.y && p.y < maxY;
		}

		public boolean contains(Line l) 
		{
			return contains( l.start ) && contains( l.end );
		}
	}

	protected final class PointDraggable implements Draggable 
	{
		private final Vector2 point;

		public PointDraggable(Vector2 point) {
			Validate.notNull(point, "point must not be NULL");
			this.point = point;
		}

		@Override
		public boolean move(float dx, float dy) 
		{
			this.point.add( dx, dy );
			
			if ( part.hasPorts() && ! part.allPortsWithinOutline() ) 
			{
				this.point.add( -dx, -dy );
				return false;
			}
			return true;
		}

		@Override
		public Vector2 getIgnoredPoint() {
			return point;
		}
	}

	protected final class LineDraggable implements Draggable 
	{
		private final Line line;

		public LineDraggable(Line line) {
			Validate.notNull(line, "line must not be NULL");
			this.line = line;
		}

		@Override
		public boolean move(float dx, float dy) 
		{
			this.line.start.add( dx, dy );
			this.line.end.add( dx, dy );
			
			if ( part.hasPorts() && ! part.allPortsWithinOutline() ) 
			{
				this.line.start.add( -dx, -dy );
				this.line.end.add( -dx, -dy );
				return false;
			}
			return true;
		}

		@Override
		public Vector2 getIgnoredPoint() {
			return null;
		}
	}	

	protected static final class PointHighlight implements Highlight {

		private Vector2 point;

		public PointHighlight(Vector2 point) {
			Validate.notNull(point, "point must not be NULL");
			this.point = point;
		}

		@Override
		public void render(Graphics2D gfx) 
		{
			renderSelectedPoint(point,gfx);
		}

		@Override
		public boolean isSame(Highlight other) 
		{
			if ( other instanceof PointHighlight) {
				return this.point.equals( ((PointHighlight) other).point );
			}
			return false;
		}
	}

	protected static final class LineHighlight implements Highlight {

		private Line line;

		public LineHighlight(Line line) {
			Validate.notNull(line, "line must not be NULL");
			this.line = line;
		}

		@Override
		public void render(Graphics2D gfx) 
		{
			gfx.setColor( Color.RED );
			gfx.drawLine( (int) line.start.x , (int) line.start.y ,(int)  line.end.x , (int) line.end.y );
		}

		@Override
		public boolean isSame(Highlight other) 
		{
			if ( other instanceof LineHighlight) 
			{
				final LineHighlight o = (LineHighlight) other;
				return this.line.start.equals( o.line.start ) && this.line.end.equals( o.line.end ) ||
						this.line.start.equals( o.line.end ) && this.line.end.equals( o.line.start );
			}
			return false;
		}
	}	

	interface EditorBehaviour 
	{
		public void render(Graphics2D gfx);
		
		// keyboard listener
		public boolean keyPressed(KeyEvent e);
		
		public boolean keyReleased(KeyEvent e);
		
		// mouse listener
		public boolean mouseMoved(MouseEvent e);
		public boolean mouseDragged(java.awt.event.MouseEvent e); 
		public boolean mousePressed(java.awt.event.MouseEvent e); 
		public boolean mouseReleased(java.awt.event.MouseEvent e); 		
		
		public void assertCanActivated() throws IllegalStateException;
		
		public void onStateExit();
	}

	static enum Corner 
	{
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT;
	}
	
	protected abstract class AbstractEditorBehaviour implements EditorBehaviour {

		@Override
		public boolean keyPressed(KeyEvent e) {
			return false;
		}
		
		@Override
		public boolean keyReleased(KeyEvent e) {
			return false;
		}
		
		@Override
		public void assertCanActivated() throws IllegalStateException {
		}
		
		@Override
		public void onStateExit() {
		}
	}

	protected final class SelectionState extends AbstractEditorBehaviour
	{
		private final Vector2 previousPoint = new Vector2();
		private boolean isDragging;
		private Draggable draggable;

		@Override
		public boolean mouseMoved(MouseEvent e) 
		{
			boolean repainted=false;
			if ( ! isDragging && currentSelection != null ) 
			{
				final DraggableCorner corner = currentSelection.getCorner( e.getX(), e.getY() );
				repainted = setHighlight( corner == null ? null : new PointHighlight( currentSelection.getCorner( corner.corner ) ) );
			}
			return repainted;
		}

		@Override
		public boolean mousePressed(MouseEvent e) 
		{
			if ( isLeftButton( e ) && ! isDragging ) 
			{
				isDragging = true;
				previousPoint.set( e.getX() , e.getY() );

				if ( currentSelection != null ) 
				{
					final Draggable corner = currentSelection.getCorner( e.getX() , e.getY() );
					if ( corner != null ) // move corner
					{
						draggable = corner;
						return false;
					}
					
					if ( currentSelection.contains( new Vector2(e.getX() , e.getY() ) ) ) { // move whole selection
						draggable = currentSelection;
						return false;
					}
				}

				currentSelection = new Selection();
				currentSelection.topLeft.set( e.getX() , e.getY() );
				currentSelection.bottomRight.set( e.getX() , e.getY() );
				draggable = new DraggableCorner( currentSelection , Corner.BOTTOM_RIGHT );
			}
			return false;
		}

		@Override
		public boolean mouseDragged(MouseEvent e) 
		{
			if ( isDragging ) 
			{
				float dx = e.getX() - previousPoint.x;
				float dy  = e.getY() - previousPoint.y;

				if ( draggable.move( dx , dy ) ) {
					previousPoint.set( e.getX() , e.getY() );
				}
				repaint();
				return true;
			}
			return false;
		}

		@Override
		public boolean mouseReleased(MouseEvent e) 
		{
			if ( isLeftButton( e ) && isDragging ) 
			{
				isDragging = false;
			}
			return false;
		}

		@Override
		public void render(Graphics2D gfx) 
		{
			if ( currentSelection == null ) {
				return;
			}
			final Vector2 p0 = currentSelection.topLeft;
			final Vector2 p1 = currentSelection.bottomRight;

			float minX = Math.min( p0.x ,p1.x );
			float maxX = Math.max( p0.x ,p1.x );

			float minY = Math.min( p0.y ,p1.y );
			float maxY = Math.max( p0.y ,p1.y );

			final int width = (int) (maxX - minX);
			final int height = (int) (maxY - minY);
			gfx.setColor(Color.RED);
			gfx.drawRect( (int) minX , (int) minY , width , height );

			part.outline.visitLines( l -> {

				if ( currentSelection.contains( l ) ) 
				{
					draw( l , gfx );
				} 
				else if ( currentSelection.contains( l.start ) ) {
					renderSelectedPoint( l.start , gfx );
				}
				else if ( currentSelection.contains( l.end ) ) {
					renderSelectedPoint( l.end , gfx );
				}
			});
		}
	}

	protected final class EditPortsState extends AbstractEditorBehaviour 
	{

		@Override
		public void render(Graphics2D gfx) {
			
		}
		
		@Override
		public void assertCanActivated() throws IllegalStateException 
		{
			if ( ! part.outline.isClosed() ) 
			{
				throw new IllegalStateException("Part needs to have a closed outline in order to add ports");
			}
		}

		@Override
		public boolean mouseMoved(MouseEvent e) 
		{
			if ( part.outline.contains( alignedLastMousePosition ) ) {
				setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			} else {
				setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			}
			final PortHighlight highlight = getPortHighlight( unalignedLastMousePosition.x ,unalignedLastMousePosition.y );
			return setHighlight( highlight );
		}

		@Override
		public boolean mouseDragged(MouseEvent e) 
		{
			if ( part.outline.contains( alignedLastMousePosition ) ) {
				setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			} else {
				setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			}			
			return false;
		}

		@Override
		public boolean mousePressed(MouseEvent e) {
			return false;
		}

		@Override
		public boolean mouseReleased(MouseEvent e) 
		{
			if ( part.outline.contains( alignedLastMousePosition ) ) 
			{
				final Port port = new Port();
				port.center.set( alignedLastMousePosition );
				part.addPort( port );
				repaint();
				return true;
			}
			return false;
		}
		
		@Override
		public void onStateExit() 
		{
			setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ));
		}
	}
	
	protected final class MoveState extends AbstractEditorBehaviour
	{
		private boolean isDragging;
		private Draggable draggedItem;

		private final Vector2 previousPoint = new Vector2();
		
		@Override
		public boolean mouseMoved(MouseEvent e) 
		{
			if ( ! isDragging ) {
				return maybeHighlight( true , draggedItem );
			}
			return false;
		}

		@Override
		public boolean mouseDragged(MouseEvent e) 
		{
			if ( isDragging ) 
			{
				float dx = e.getX() - previousPoint.x;
				float dy = e.getY() - previousPoint.y;

				final boolean moved = draggedItem.move( dx , dy );
				if ( ! maybeHighlight( draggedItem ) ) {
					repaint();
				}
				if ( moved ) {
					previousPoint.set( e.getX() , e.getY() );
				}
				return true;
			}
			return false;
		}

		@Override
		public boolean mousePressed(MouseEvent e) 
		{
			if ( isLeftButton( e ) && ! isDragging ) 
			{
				float x = unalignedLastMousePosition.x;
				float y = unalignedLastMousePosition.y;
				final Vector2 point = getSnapPoint( x , y , null );
				draggedItem = point == null ? null : new PointDraggable( point );
				if ( draggedItem == null ) {
					Line line = getSnapLine( x , y );
					draggedItem = line == null ? null : new LineDraggable( line );
				}
				if ( draggedItem == null ) 
				{
					PortHighlight port = getPortHighlight( unalignedLastMousePosition.x , unalignedLastMousePosition.y );
					if ( port != null ) {
						draggedItem = new PortDraggable( port.port );
					}
				}
				if ( draggedItem != null ) 
				{
					isDragging = true;
					previousPoint.set( e.getX() , e.getY() );
				}
			}
			return false;
		}

		@Override
		public boolean mouseReleased(MouseEvent e) 
		{
			if ( isLeftButton( e ) && isDragging ) 
			{
				part.outline.compact();
				draggedItem = null;
				isDragging = false;
			}
			return false;
		}

		@Override
		public void render(Graphics2D gfx) 
		{
		}
	}

	protected final class EditOutlineState extends AbstractEditorBehaviour
	{
		private boolean isDrawingLine;
		private Line line;

		@Override
		public boolean mouseMoved(MouseEvent e) 
		{
			if ( ! isDrawingLine ) {
				return maybeHighlight( true , null );
			}
			return false;
		}		
		
		@Override
		public boolean keyReleased(KeyEvent e) 
		{
			if ( ! isDrawingLine && e.getKeyCode() == KeyEvent.VK_DELETE ) 
			{
				final Highlight highlight = findHighlight( unalignedLastMousePosition.x , unalignedLastMousePosition.y , true , null );
				if ( highlight instanceof LineHighlight) 
				{
					part.outline.removeLine( ((LineHighlight) highlight).line );
					setHighlight( null );
					repaint();
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean mouseDragged(MouseEvent e) 
		{
			if ( isDrawingLine ) 
			{
				float x = e.getX();
				float y = e.getY();
				
				if ( onlyRightAngles ) 
				{
					final float dx = Math.abs( x - line.start.x ); 
					final float dy = Math.abs( y - line.start.y );					
					if ( dx >= dy ) {
						y = line.start.y;
					} else {
						x = line.start.x;
					}
				}
				line.end.set( x , y );
				if ( ! maybeHighlight( null ) ) {
					repaint();
				}
				return true;
			}
			
			return false;
		}

		@Override
		public boolean mousePressed(MouseEvent e) 
		{
			if ( isLeftButton(e) && ! isDrawingLine ) 
			{
				isDrawingLine = true;
				final Vector2 point = getSnapPoint( e.getX() , e.getY() );
				if ( point != null ) {
					line = new Line( point , new Vector2( e.getX() , e.getY() ) );
				} else {
					line = new Line();
					line.start.set( e.getX() , e.getY() );
					line.end.set( e.getX() , e.getY() );
				}
			}
			return false;
		}

		@Override
		public boolean mouseReleased(MouseEvent e) 
		{
			if ( isLeftButton(e) && isDrawingLine ) 
			{
				final Vector2 point = getSnapPoint( e.getX() , e.getY() );
				if ( point != null ) {
					line.end = point;
				}

				if ( line.len() > 1 ) 
				{
					part.outline.addLine( line );
				}
				line = null;
				isDrawingLine = false;
				repaint();
				return true;
			} 
			return false;
		}

		@Override
		public void render(Graphics2D gfx) 
		{
			if ( isDrawingLine ) {
				drawLine(line,Color.GREEN,gfx);
			}
		}		
	}	

	private final MouseAdapter mouseListener = new MouseAdapter() 
	{
		@Override
		public void mouseMoved(MouseEvent e) 
		{
			unalignedLastMousePosition.set( e.getX() , e.getY() );
			
			final MouseEvent alignedEvent = maybeAlignToGrid(e);
			alignedLastMousePosition.set( alignedEvent.getX() , alignedEvent.getY() );
			final boolean repainted = modeImpl.mouseMoved(alignedEvent);
			if ( showGuide && ! repainted ) {
				repaint();
			}
		}

		@Override
		public void mouseDragged(java.awt.event.MouseEvent e) 
		{
			unalignedLastMousePosition.set( e.getX() , e.getY() );			
			
			final MouseEvent alignedEvent = maybeAlignToGrid(e);
			alignedLastMousePosition.set( alignedEvent.getX() , alignedEvent.getY() );
			
			final boolean repainted = modeImpl.mouseDragged(alignedEvent);
			if ( showGuide && ! repainted ) {
				repaint();
			}			
		}
		
		@Override
		public void mousePressed(java.awt.event.MouseEvent e) 
		{
			modeImpl.mousePressed( maybeAlignToGrid(e) );
		}

		@Override
		public void mouseReleased(java.awt.event.MouseEvent e) 
		{
			modeImpl.mouseReleased( maybeAlignToGrid(e) );
		}
		
		private MouseEvent maybeAlignToGrid(MouseEvent input) 
		{
			if ( ! snapToGrid ) {
				return input;
			}
			final MouseEvent copy = SerializationUtils.clone( input );
			final int alignedX = (input.getX() / gridX) * gridX;
			final int alignedY = (input.getY() / gridY) * gridY;
			
			try {
				final Field x = copy.getClass().getDeclaredField("x");
				x.setAccessible(true);
				x.set( copy , alignedX );
				
				final Field y = copy.getClass().getDeclaredField("y");
				y.setAccessible(true);
				y.set( copy , alignedY );
			} 
			catch(Exception e) {
				throw new RuntimeException("Something bad happened",e);
			}
			return copy;
		}		
	};

	private boolean isLeftButton(MouseEvent ev) {
		return ev.getButton() == MouseEvent.BUTTON1;
	}

	@SuppressWarnings("unused")
	private boolean isRightButton(MouseEvent ev) {
		return ev.getButton() == MouseEvent.BUTTON3;
	}	

	/**
	 * 
	 * @param e
	 * @param ignoredPoint
	 * 
	 * @return <code>true</code> if <code>repaint()</code> has been called
	 */
	private boolean maybeHighlight(Draggable ignoredPoint) 
	{
		return maybeHighlight(false,ignoredPoint);
	}

	/**
	 * 
	 * @param e
	 * @param highlightLines
	 * @param ignoredPoint
	 * @return <code>true</code> if <code>repaint()</code> has been called
	 */
	private boolean maybeHighlight(boolean highlightLines,Draggable ignoredPoint) 
	{
		
		final Highlight newHighlight = findHighlight( unalignedLastMousePosition.x , unalignedLastMousePosition.y , highlightLines , ignoredPoint);
		return setHighlight( newHighlight );
	}
	
	private Highlight findHighlight(float x , float y , boolean highlightLines,Draggable ignoredPoint) 
	{
		final Vector2 point = getSnapPoint( x , y , ignoredPoint);
		if ( point != null ) {
			return new PointHighlight( point );
		}
		if ( highlightLines ) {
			final Line line = getSnapLine( x , y );
			if ( line != null ) {
				return new LineHighlight( line );
			}
		}
		PortHighlight port  = getPortHighlight( x , y );
		if ( port != null ) {
			return new PortHighlight( port.port );
		}
		return null;
	}

	/**
	 * 
	 * @param newHighlight
	 * @return <code>true</code> if <code>repaint()</code> has been called
	 */
	private boolean setHighlight(Highlight newHighlight) 
	{
		if ( newHighlight == null ) 
		{
			if ( highlight != null ) {
				highlight = null;
				repaint();
				return true;
			}
		} 
		else 
		{
			if ( highlight == null || ! highlight.isSame( newHighlight ) ) 
			{
				highlight = newHighlight;
				repaint();
				return true;
			}
		}
		return false;
	}

	private Vector2 getSnapPoint(int px,int py) 
	{
		return getSnapPoint(px,py,null);
	}

	private Line getSnapLine(float px , float py) 
	{
		return part.outline.visitLines( new LineVisitor<Line>() 
		{
			private Line result;
			private float minDistance = Float.MAX_VALUE;
			
			@Override
			public boolean visit(Line line) 
			{
				float d = distance( line , px , py );
				if ( d < SNAP_RADIUS && ( result == null || d < minDistance ) )  {
					result = line.copy(false);
					minDistance = d;
				}
				return true;
			}

			@Override
			public Line getResult() {
				return result;
			}
		});
	}

	private float distance(Line line , float px,float py) 
	{
		// solution adopted from http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
		// (C) by http://stackoverflow.com/users/167531

		final Vector2 start = line.start;
		final Vector2 end = line.end;
		final Vector2 p = new Vector2( px , py );

		final float length = start.dst2( end ); 
		if (length == 0.0) { 
			return p.dst(start); 
		}
		final Vector2 startToEnd = end.cpy().sub(start);
		final float t = Math.max(0, Math.min(1, p.cpy().sub(start).dot( startToEnd ) / length ) );
		final Vector2 projection = start.cpy().add( startToEnd.scl(t) );
		return p.dst( projection );
	}

	private Vector2 getSnapPoint(float px,float py,Draggable draggable) 
	{
		final Vector2 ignoredPoint = draggable == null ? null : draggable.getIgnoredPoint();

		final LineVisitor<Vector2> visitor = new LineVisitor<Vector2>() {
			
			Vector2 result = null;
			float minDistance = Float.MAX_VALUE;
			
			@Override
			public boolean visit(Line l) 
			{
				float dStart = l.start.dst( px , py );
				float dEnd = l.end.dst( px , py );
				if ( result == null ) 
				{
					if ( dStart < SNAP_RADIUS && dStart < dEnd && ignoredPoint != l.start ) {
						result = l.start;
						minDistance = dStart;
					} else if ( dEnd < SNAP_RADIUS && ignoredPoint != l.end ){
						result = l.end;
						minDistance = dEnd;					
					}
				} 
				else 
				{
					if ( dStart < dEnd && dStart < minDistance && ignoredPoint != l.start ) {
						result = l.start;
						minDistance = dStart;
					} else if ( dEnd < minDistance && ignoredPoint != l.end ) {
						result = l.end;
						minDistance = dEnd;					
					}
				}
				return true;
			}
			
			@Override
			public Vector2 getResult() {
				return result;
			}
		};
		return part.outline.visitLines( visitor );
	}

	public PartEditorPanel(Part part) 
	{
		this.part = part;
		addMouseMotionListener( mouseListener );
		addMouseListener( mouseListener );
		setRequestFocusEnabled( true );
		requestFocus();
		
		addKeyListener( new KeyAdapter() 
		{
			@Override
			public void keyReleased(KeyEvent e) 
			{
				modeImpl.keyReleased(e);
			}
		});
	}

	@Override
	protected void paintComponent(Graphics gfx) 
	{
		final Graphics2D g = (Graphics2D) gfx;
		super.paintComponent(g);

		if ( showGuide ) 
		{
			final Stroke old = g.getStroke();
			g.setStroke( GUIDE_STROKE );
			try {
			g.setColor( GUIDE_COLOR );
			
			g.drawLine( (int) alignedLastMousePosition.x , 0 , (int) alignedLastMousePosition.x , getHeight() );
			g.drawLine( 0 , (int) alignedLastMousePosition.y , getWidth() , (int) alignedLastMousePosition.y );
			} finally {
				g.setStroke( old );
			}
		}
		
		// render outline
		g.setColor( Color.BLUE );
		part.outline.visitLines( line -> draw( line , g) );

		// render port
		g.setColor( Color.BLUE );
		part.visitPorts( port -> 
		{
			renderPort(port,g);
		});

		if ( highlight != null ) 
		{
			highlight.render( (Graphics2D) g);
		}

		modeImpl.render( (Graphics2D) g );
	}

	private static void renderPort(Port port,final Graphics2D g) 
	{
		final float size = PORT_RADIUS*PORT_CROSSHAIR_SIZE;		
		g.drawLine( (int) ( port.center.x - size) , (int) port.center.y ,    (int) ( port.center.x + size) , (int) port.center.y ) ;
		g.drawLine( (int) port.center.x , (int) (port.center.y - size) , (int) port.center.x , (int) ( port.center.y + size) ) ;
		g.drawArc( (int) (port.center.x - PORT_RADIUS/2f) , (int) (port.center.y - PORT_RADIUS/2f) , PORT_RADIUS , PORT_RADIUS , 0 , 360 );
	}

	private void drawLine(Line line,Color color,Graphics gfx) 
	{
		gfx.setColor( color );
		draw( line , gfx );
	}

	private void draw(Line line , Graphics g) {
		g.drawLine( (int) line.start.x , (int) line.start.y ,(int) line.end.x , (int) line.end.y );
	}

	public void setMode(EditorMode mode) 
	{
		Validate.notNull(mode, "mode must not be NULL");
		final EditorBehaviour tmp = getBehaviour( mode );
		tmp.assertCanActivated(); 
		this.currentMode = mode;
		this.modeImpl = tmp;
		this.highlight = null;
		repaint();		
	}

	private EditorBehaviour getBehaviour(EditorMode mode) 
	{
		switch( mode ) 
		{
			case EDIT_OUTLINE:
				return new EditOutlineState();
			case MOVE:
				return new MoveState();				
			case SELECT:
				return new SelectionState();		
			case EDIT_PORTS:
				return new EditPortsState();
			default:
				throw new IllegalArgumentException("Unhandled switch/case: "+mode);
		}
	}

	public EditorMode getMode() {
		return currentMode;
	}
	
	private PortHighlight getPortHighlight(float px,float py) 
	{
		final Port[] closest = {null};
		final float[] distance = {0};
		
		part.visitPorts( port -> 
		{
			float dx = port.center.x - px;
			float dy = port.center.y - py;
			final float d = (float) Math.sqrt( dx*dy +dy*dy );
			if ( closest[0] == null || d < distance[0] ) 
			{
				closest[0] = port;
				distance[0] = d;
			}
		});
		return closest[0] != null && distance[0] <= SNAP_RADIUS ? new PortHighlight( closest[0] ) : null;
	}

	private static void renderSelectedPoint(Vector2 point,Graphics2D gfx) 
	{
		gfx.setColor( Color.RED );
		final int r = 10;
		final int p1x = (int) (point.x - r/2);
		final int p1y = (int) (point.y - r/2);
		gfx.drawArc( p1x , p1y , r , r , 0 , 360 ); 	
	}
	
	public void setSnapToGrid(boolean snapToGrid) {
		this.snapToGrid = snapToGrid;
	}
	
	public void setOnlyRightAngles(boolean onlyRightAngles) {
		this.onlyRightAngles = onlyRightAngles;
	}
	
	public boolean isOnlyRightAngles() {
		return onlyRightAngles;
	}
	
	public boolean isSnapToGrid() {
		return snapToGrid;
	}
	
	public void setShowGuide(boolean showGuide) {
		this.showGuide = showGuide;
		repaint();
	}
	
	public boolean isShowGuide() {
		return showGuide;
	}
}