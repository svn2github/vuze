/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core3.disk.impl;

import org.gudy.azureus2.core3.disk.*;

// refactored - Moti
// was a static inner class in DiskManager
// but doesn't need anything from that class.
// More readable and simpler as a separate class
public class PieceMapEntry {
	private DiskManagerFileInfo _file;
	private int _offset;
	private int _length;

	public PieceMapEntry(DiskManagerFileInfo file, int offset, int length) {
		_file = file;
		_offset = offset;
		_length = length;
	}
	public DiskManagerFileInfo getFile() {
		return _file;
	}
	public void setFile(DiskManagerFileInfo file) {
		_file = file;
	}
	public int getOffset() {
		return _offset;
	}
	public int getLength() {
		return _length;
	}

}