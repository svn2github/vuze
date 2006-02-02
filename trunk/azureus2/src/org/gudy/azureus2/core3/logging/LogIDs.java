/**
 * 
 */
package org.gudy.azureus2.core3.logging;

/** Enumeration of Log IDs (Component IDs) used in Logger
 * 
 * @author TuxPaper
 *
 * @note Idea from http://java.sun.com/developer/Books/shiftintojava/page1.html#replaceenums
 */
public class LogIDs implements Comparable {

	private final String name;

	// Ordinal of next suit to be created
	private static int nextOrdinal = 0;

	// Assign an ordinal to this suit
	private final int ordinal = nextOrdinal++;

	private LogIDs(String name) {
		this.name = name;
	}

	public String toString() {
		return this.name;
	}

	public int compareTo(Object o) {
		return ordinal - ((LogIDs) o).ordinal;
	}

	// LogIDs. Prefix would be redundant, since this class is the prefix

	public final static LogIDs LOGGER = new LogIDs("logger");

	public final static LogIDs NWMAN = new LogIDs("nwman");

	public final static LogIDs NET = new LogIDs("net");

	public final static LogIDs PEER = new LogIDs("peer");

	public final static LogIDs CORE = new LogIDs("core");

	public final static LogIDs DISK = new LogIDs("disk");

	public final static LogIDs PLUGIN = new LogIDs("plug");

	public final static LogIDs TRACKER = new LogIDs("tracker");

	public final static LogIDs GUI = new LogIDs("GUI");

	public final static LogIDs STDOUT = new LogIDs("stdout");

	public final static LogIDs STDERR = new LogIDs("stderr");

	public final static LogIDs ALERT = new LogIDs("alert");

	public final static LogIDs CACHE = new LogIDs("cache");

	public final static LogIDs PIECES = new LogIDs("pieces");
}
