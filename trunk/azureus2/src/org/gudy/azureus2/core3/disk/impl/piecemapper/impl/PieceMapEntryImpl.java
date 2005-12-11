/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core3.disk.impl.piecemapper.impl;

import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;


public class 
PieceMapEntryImpl 
	implements DMPieceMapEntry
{
		/**
		 * This class denotes the mapping of a piece onto a file. Typically a piece can
		 * span multiple files. Each overlapping segment has on of these entries created
		 * for it. 
		 * It identifies the file, the offset within the file, and the length of the chunk
		 */
	
	private DiskManagerFileInfoImpl _file;
	private long 					_offset;
	private int						_length;

	public 
	PieceMapEntryImpl(
		DiskManagerFileInfoImpl 	file, 
		long 						offset, 
		int 						length )
	{
		_file = file;
		_offset = offset;
		_length = length;
	}
	
	public DiskManagerFileInfoImpl getFile() {
		return _file;
	}
	public long getOffset() {
		return _offset;
	}
	public int getLength() {
		return _length;
	}

}