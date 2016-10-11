package de.codesourcery.pcb.model;

import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;

import de.codesourcery.pcb.utils.MathUtils;

public class LineCollection 
{
	private int[] startPoints=new int[10];
	private int[] endPoints=new int[10];
	private Vector2[] points = new Vector2[100];
	
	private int pointCount;
	private int lineCount;
	
	public interface LineVisitor<T>
	{
		public boolean visit(Line line);
		
		public T getResult();
	}
	
	public int addPoint(Vector2 p) 
	{
		if ( pointCount == points.length ) 
		{
			final int newSize = points.length + 1 + points.length/2;
			final Vector2[] tmp = new Vector2[ newSize ];
			System.arraycopy( points , 0 , tmp , 0 , points.length );
			points = tmp;
		}
		points[ pointCount++ ] = p;
		return pointCount-1;
	}
	
	public boolean isClosedOutline() 
	{
		if ( lineCount < 3 || pointCount < 3) {
			return false;
		}
		for ( int i = 1 ; i < lineCount ; i++ ) 
		{
			int start1 = startPoints[ i-1 ];
			int end1= endPoints[ i-1 ];
			
			int start2 = startPoints[ i ];
			if ( start2 == end1 ) {
				continue;
			}
			if ( start2 == start1 ) 
			{
				continue;
			}
		}
		return true;
	}
	
	public boolean isPointInside(Vector2 p) 
	{
		final Vector2 lineEnd = new Vector2( 100000 , p.y );
		
		int intersectCount = 0;
		for ( int i = 0 ; i <lineCount;i++ ) 
		{
			if ( MathUtils.intersect( p , lineEnd , points[ startPoints[i] ] , points[ endPoints[i] ] ) ) 
			{
				intersectCount++;
			}
		}
		return (intersectCount&1) != 0;
	}
	
	public void visitLines(Consumer<Line> visitor) 
	{
		final Line tmp = new Line();
		for ( int i = 0 ; i < lineCount ; i++ ) {
			tmp.start = start( i );
			tmp.end = end( i );
			visitor.accept( tmp );
		}
	}
	
	public <T> T visitLines(LineVisitor<T> visitor)
	{
		final Line tmp = new Line();
		for ( int i = 0 ; i < lineCount ; i++ ) 
		{
			tmp.start = start( i );
			tmp.end = end( i );
			if ( ! visitor.visit( tmp ) ) {
				break;
			}
		}
		return visitor.getResult();
	}
	
	/**
	 * Merges points with same coordinates.
	 */
	public void compact() 
	{
		for ( int i = 0 ; i < pointCount ; i++ ) 
		{
again:			
			for ( int j = i+1 ; j < pointCount ; j++ ) 
			{
				if ( points[i].equals( points[j] ) ) 
				{
					for ( int k = 0 ; k < lineCount ; k++ ) 
					{
						if ( startPoints[k] == j ) {
							System.out.println("Fixing starting point of line "+k);
							startPoints[k] = i;
						}
						if ( endPoints[k] == j ) {
							System.out.println("Fixing end point of line "+k);
							endPoints[k] = i;
						}						
					}
					
					System.out.println("Points are equal: "+points[i]+" ("+i+") <-> "+points[j]+" ("+j+")");
					deletePoint( j );
					System.out.println("Deleted point at "+j);

					continue again;
				}
			}
		}
		debug("after compact():" );
	}
	
	public LineCollection copy() 
	{
		final LineCollection result = new LineCollection ();
		result.pointCount = pointCount;
		result.lineCount = lineCount;
		
		result.startPoints = new int[ lineCount ];
		result.endPoints = new int[ lineCount ];
		result.points = new Vector2[ pointCount ];
		
		System.arraycopy( this.startPoints , 0 , result.startPoints , 0 , lineCount );
		System.arraycopy( this.endPoints , 0 , result.endPoints , 0 , lineCount );
		System.arraycopy( this.points, 0 , result.points , 0 , pointCount );
		
		return result;
	}
	
	public void transform(Matrix3 mat) 
	{
		for ( Vector2 p : points ) 
		{
			p.mul( mat );
		}
	}
	
	public void addLine(Vector2 start,Vector2 end) 
	{
		Validate.notNull(start, "start must not be NULL");
		Validate.notNull(end, "end must not be NULL");

		if ( start.equals( end ) ) {
			throw new IllegalArgumentException("Line must not have length 0 (start == end)");
		}
		
		int startIdx = -1;
		int endIdx = -1;
		for (int i = 0; i < points.length; i++) 
		{
			Vector2 p = points[i];
			if ( startIdx == -1 && p == start ) {
				startIdx = i;
			}
			if ( endIdx == -1 && p == end ) {
				endIdx = i;
			}
		}
		
		if ( startIdx != -1 && endIdx != -1 ) 
		{
			for ( int i =0 ; i < lineCount ; i++ ) {
				
				if ( startPoints[i] == startIdx && endPoints[i] == endIdx ) {
					return; // line already added
				}
			}
		}
		
		if ( startIdx == -1 ) {
			startIdx = addPoint( start );
		}
		if ( endIdx == -1 ) {
			endIdx = addPoint( end );
		}
		
		if ( lineCount == startPoints.length ) 
		{
			startPoints = copyAndResize(startPoints);
			endPoints = copyAndResize(endPoints);
		}
		startPoints[ lineCount ] = startIdx;
		endPoints[ lineCount ] = endIdx;
		lineCount++;
	}
	
	private int[] copyAndResize(int[] input) 
	{
		final int newLen = input.length+1+input.length/2;
		final int[] tmp = new int[ newLen ];
		System.arraycopy(input, 0 , tmp , 0 , input.length );
		return tmp;
	}
	
	public Vector2 start(int lineIdx) {
		return points[ startPoints[lineIdx] ];
	}
	
	public Vector2 end(int lineIdx) {
		return points[ endPoints[ lineIdx ] ];
	}

	public void removeLine(Line line) 
	{
		for ( int i = 0 ; i < lineCount ; i++ ) 
		{
			Vector2 start = points[ startPoints[i]];
			Vector2 end = points[ endPoints[i]];
			
			if ( start.equals( line.start ) && end.equals( line.end ) ) 
			{
				removeLine( i );
				return;
			}			
		}
	}	
	
	private void removeLine(int lineIdx) 
	{
		debug("About to remove line with idx "+lineIdx);
		
		final int startIdx = startPoints[lineIdx];
		final int endIdx = endPoints[lineIdx];
		for ( int i = lineIdx + 1 ; i < lineCount ; i++ ) 
		{
			startPoints[i-1] = startPoints[i]; 
			endPoints[i-1] = endPoints[i]; 
		}
		lineCount--;
		
		// remove unreferenced points
		boolean startReferenced = false;
		boolean endReferenced = false;
		for ( int i = 0 ; i < lineCount ; i++ ) 
		{
			int start = startPoints[i];
			int end = endPoints[i];
			if ( start == startIdx || end == startIdx ) {
				startReferenced = true;
			}
			if ( start == endIdx || end == endIdx ) {
				endReferenced = true;
			}			
		}
		
		if ( startReferenced && ! endReferenced ) 
		{
			// end point is no longer referenced
			deletePoint(endIdx);
		} 
		else if ( ! startReferenced && endReferenced ) 
		{
			// start point is no longer referenced
			deletePoint(startIdx);			
		} 
		else if ( ! startReferenced && ! endReferenced ) 
		{
			// both start and end point are no longer referenced
			if ( startIdx < endIdx ) 
			{
				deletePoint( startIdx );
				deletePoint( endIdx-1 );
			} 
			else if ( startIdx > endIdx ) 
			{
				deletePoint( endIdx );
				deletePoint( startIdx-1 );				
			} else {
				throw new IllegalStateException("Start == end ??");
			}
		}
		debug("After line removal");
	}
	
	private void debug(String message) 
	{
		System.out.println("\n############################\n# "+message+"\n#######################\n");
		System.out.println("Line count: "+lineCount+" / point count: "+pointCount);
		System.out.println("Start points: "+ArrayUtils.toString( ArrayUtils.subarray( startPoints , 0  , lineCount ) ) );
		System.out.println("End   points: "+ArrayUtils.toString( ArrayUtils.subarray( endPoints , 0  , lineCount ) ) );
		System.out.println("Points      : "+ArrayUtils.toString( ArrayUtils.subarray( points , 0  , pointCount ) ) );
	}
	
	private void deletePoint(final int pointIdxToDelete) 
	{
		for ( int i = pointIdxToDelete+1 ; i < pointCount ; i++ ) {
			points[i-1] = points[i];
		}
		
		// fix lines
		for ( int i = 0 ; i < lineCount ; i++ ) 
		{
			if ( startPoints[i] >= pointIdxToDelete ) {
				startPoints[i]--;
			}
			if ( endPoints[i] >= pointIdxToDelete ) {
				endPoints[i]--;
			}				
		}
		pointCount--;
	} 
}