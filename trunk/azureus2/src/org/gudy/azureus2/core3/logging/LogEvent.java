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

	/** Peer this event applies to */
	public PEPeer peer;

	/** Torrent this event applies to */
	public TOTorrent torrent;

	/** Log ID, categorizing the event */
	public LogIDs logID;

	/** Type of entry, usually one of Event.LT_* constants */
	public int entryType;

	/** Text of the event */
	public String text;

	/** Error related to event */
	public Throwable err = null;

	public LogEvent(TOTorrent torrent, PEPeer peer, LogIDs logID,
			int entryType, String text) {
		this.torrent = torrent;
		this.peer = peer;
		if (peer != null && torrent == null) {
			DownloadManager dlm = peer.getManager().getDownloadManager();
			if (dlm != null)
				this.torrent = dlm.getTorrent();
		}
		this.logID = logID;
		this.entryType = entryType;
		this.text = text;
	}

	public LogEvent(TOTorrent torrent, PEPeer peer, LogIDs logID,
			String text) {
		this(null, peer, logID, LT_INFORMATION, text);
	}

	public LogEvent(PEPeer peer, LogIDs logID, int entryType, String text) {
		this(null, peer, logID, entryType, text);
	}

	public LogEvent(PEPeer peer, LogIDs logID, String text) {
		this(null, peer, logID, LT_INFORMATION, text);
	}

	public LogEvent(TOTorrent torrent, LogIDs logID, int entryType,
			String text) {
		this(torrent, null, logID, entryType, text);
	}

	public LogEvent(LogIDs logID, int entryType, String text) {
		this(null, null, logID, entryType, text);
	}

	public LogEvent(TOTorrent torrent, LogIDs logID, String text) {
		this(torrent, logID, LT_INFORMATION, text);
	}

	public LogEvent(LogIDs logID, String text) {
		this(null, null, logID, LT_INFORMATION, text);
	}

	// Throwables

	public LogEvent(PEPeer peer, LogIDs logID, String text, Throwable e) {
		this(peer, logID, LT_ERROR, text);
		this.err = e;
	}

	public LogEvent(TOTorrent torrent, LogIDs logID, String text,
			Throwable e) {
		this(torrent, logID, LT_ERROR, text);
		this.err = e;
	}

	public LogEvent(LogIDs logID, String text, Throwable e) {
		this((TOTorrent) null, logID, text, e);
	}

	// This should be (or already is) somewhere else.. like TorrentUtils?
	public static TOTorrent getTorrentFromHash(byte[] hash) {
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager dlm = gm.getDownloadManager(hash);

		return (dlm == null) ? null : dlm.getTorrent();
	}
}
