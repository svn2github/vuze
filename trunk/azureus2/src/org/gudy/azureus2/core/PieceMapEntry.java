/*
 * Created on Sep 1, 2003 
 */
 
package org.gudy.azureus2.core;


// refactored - Moti
// was a static inner class in DiskManager
// but doesn't need anything from that class.
// More readable and simpler as a separate class
public class PieceMapEntry {
	private FileInfo _file;
	private int _offset;
	private int _length;

	public PieceMapEntry(FileInfo file, int offset, int length) {
		_file = file;
		_offset = offset;
		_length = length;
	}
	public FileInfo getFile() {
		return _file;
	}
	public void setFile(FileInfo file) {
		_file = file;
	}
	public int getOffset() {
		return _offset;
	}
	public int getLength() {
		return _length;
	}

}