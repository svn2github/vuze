/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core3.disk.impl.piecemapper.impl;

import java.util.List;

import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;

/**
 * @author Moti
 *
 * PieceList contains a list of pieces; it also provides accessor and
 * utility methods.
 */
public class 
PieceListImpl
	implements DMPieceList
{
	
	final private PieceMapEntryImpl[] pieces;	
	final private int[] cumulativeLengths;
	
	static public PieceListImpl 
	convert(
		List pieceList) 
	{
		return new PieceListImpl((PieceMapEntryImpl[])pieceList.toArray(new PieceMapEntryImpl[pieceList.size()]));	
	}
	
	protected
	PieceListImpl(
		PieceMapEntryImpl[] _pieces) 
	{
		pieces = _pieces;
		cumulativeLengths = new int[pieces.length];
		
		initializeCumulativeLengths();
	}

	private void 
	initializeCumulativeLengths() 
	{
		int runningLength = 0;
		for (int i = 0; i < pieces.length; i++) {
			runningLength += pieces[i].getLength();
			cumulativeLengths[i] = runningLength;
		}
	}
	
	public int size() {
		return pieces.length;	
	}
	
	public boolean isEmpty() {
		return size() == 0;	
	}
	
	public DMPieceMapEntry get(int index) {
		return pieces[index];	
	}
	
	public int getCumulativeLengthToPiece(int index) {
		return cumulativeLengths[index];	
	}
}
