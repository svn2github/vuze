/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core;

import java.util.List;

/**
 * @author Moti
 *
 * PieceList contains a list of pieces; it also provides accessor and
 * utility methods.
 */
public class PieceList {
	
	final private PieceMapEntry[] pieces;	
	final private long[] cumulativeLengths;
	
	static public PieceList convert(List pieceList) {
		return new PieceList((PieceMapEntry[])pieceList.toArray(new PieceMapEntry[pieceList.size()]));	
	}
	
	public PieceList(PieceMapEntry[] pieces) {
		this.pieces = pieces;
		this.cumulativeLengths = new long[pieces.length];
		
		initializeCumulativeLengths();
	}

	private void initializeCumulativeLengths() {
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
	
	public long getCumulativeLengthToPiece(int index) {
		return cumulativeLengths[index];	
	}
}
