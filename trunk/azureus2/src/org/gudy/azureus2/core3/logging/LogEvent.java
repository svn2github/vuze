/**
 * 
 */
package org.gudy.azureus2.core3.logging;

import java.util.Date;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.torrent.TOTorrent;

import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * Container to hold Log Event information.
 * 
 * @note There are no constructors without Log ID as a parameter. This is
 *       intentional, as all log events should have a log id.
 * @author TuxPaper
 */

public class LogEvent {
	// log types
	public static final int LT_INFORMATION = 0;

	public static final int LT_WARNING = 1;

	public static final int LT_ERROR = 3;
	
	/** Date and Time this event occurred */
	public Date timeStamp = new Date();

	/** A list of events that this entry is related to */
	public Object[] relatedTo;

	/** Log ID, categorizing the event */
	public LogIDs logID;

	/** Type of entry, usually one of Event.LT_* constants */
	public int entryType;

	/** Text of the event */
	public String text;

	/** Error related to event */
	public Throwable err = null;

	public LogEvent(Object[] relatedTo, LogIDs logID, int entryType, String text) {
		this.logID = logID;
		this.entryType = entryType;
		this.text = text;
		this.relatedTo = relatedTo;
	}

	public LogEvent(Object relatedTo, LogIDs logID, int entryType, String text) {
		this(new Object[] { relatedTo }, logID, entryType, text);
	}


	public LogEvent(LogIDs logID, int entryType, String text) {
		this(null, logID, entryType, text);
	}

	public LogEvent(Object[] relatedTo, LogIDs logID, String text) {
		this(relatedTo, logID, LT_INFORMATION, text);
	}

	public LogEvent(Object relatedTo, LogIDs logID, String text) {
		this(new Object[] { relatedTo }, logID, LT_INFORMATION, text);
	}

	public LogEvent(LogIDs logID, String text) {
		this(null, logID, LT_INFORMATION, text);
	}

	// Throwables

	public LogEvent(Object[] relatedTo, LogIDs logID, int entryType, String text, Throwable e) {
		this(relatedTo, logID, entryType, text);
		this.err = e;
	}
	public LogEvent(Object[] relatedTo, LogIDs logID, String text, Throwable e) {
		this(relatedTo, logID, LT_ERROR, text, e);
	}
	
	public LogEvent(Object relatedTo, LogIDs logID, String text, Throwable e) {
		this(new Object[] { relatedTo }, logID, text, e);
	}

	public LogEvent(LogIDs logID, int entryType, String text, Throwable e) {
		this(null, logID, entryType, text, e);
	}
	
	public LogEvent(LogIDs logID, String text, Throwable e) {
		this(null, logID, text, e);
	}
}
