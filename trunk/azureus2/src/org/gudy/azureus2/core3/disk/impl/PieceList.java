/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core3.disk.impl;

import java.util.List;

/**
 * @author Moti
 *
 * PieceList contains a list of pieces; it also provides accessor and
 * utility methods.
 */
public class PieceList {
	
	final private PieceMapEntry[] pieces;	
	final private int[] cumulativeLengths;
	
	static public PieceList 
	convert(
		List pieceList) 
	{
		return new PieceList((PieceMapEntry[])pieceList.toArray(new PieceMapEntry[pieceList.size()]));	
	}
	
	protected
	PieceList(
		PieceMapEntry[] _pieces) 
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
	
	public PieceMapEntry get(int index) {
		return pieces[index];	
	}
	
	public int getCumulativeLengthToPiece(int index) {
		return cumulativeLengths[index];	
	}
}
