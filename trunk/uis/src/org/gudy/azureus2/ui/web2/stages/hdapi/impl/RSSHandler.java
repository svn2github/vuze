/*
 * Created on 27.12.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.web2.stages.hdapi.impl;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.web2.http.request.httpRequest;
import org.gudy.azureus2.ui.web2.http.response.httpOKResponse;
import org.gudy.azureus2.ui.web2.http.response.httpResponse;
import org.gudy.azureus2.ui.web2.stages.hdapi.httpRequestHandlerIF;

import seda.sandStorm.core.BufferElement;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RSSHandler implements httpRequestHandlerIF {
	
	private final String tr = "\n";

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.web2.stages.hdapi.httpRequestHandlerIF#handleRequest(org.gudy.azureus2.ui.web2.http.request.httpRequest)
	 */
	public httpResponse handleRequest(httpRequest req) {
		Date now =new Date();
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		String nowst =format.format(now);
		String response = "<?xml version=\"1.0\" ?>"+tr+
										 "<rss version=\"2.0\">"+tr+
										 "<channel>"+tr+
										 " <title>Azureus Status</title>"+tr+
										 " <description>Azureus Download Status Information. Color of the status character indicates health status. Green name means running, red means error.</description>"+tr+
										 " <link>http://azureus.sourceforge.net</link>"+tr+
										 " <item>"+tr+
										 "  <title>Azureus Status Information "+nowst+"</title>"+tr+
										 "  <pubDate>"+nowst+"</pubDate>"+tr+
										 "  <description><![CDATA[";
		List torrents = UIConst.GM.getDownloadManagers();
		DownloadManager dm;
		int dmstate;
		if (!torrents.isEmpty()) {
			Iterator torrent = torrents.iterator();
			long totalReceived = 0;
			long totalSent = 0;
			long totalDiscarded = 0;
			int connectedSeeds = 0;
			int connectedPeers = 0;
			PEPeerManagerStats ps;
			int nrTorrent = 0;
			while (torrent.hasNext()) {
				dm = (DownloadManager) torrent.next();
				TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
				dmstate = dm.getState();
				try {
					ps = dm.getPeerManager().getStats();
				} catch (Exception e) {
					ps = null;
				}
				if (ps != null) {
					totalReceived += dm.getStats().getDownloaded();
					totalSent += dm.getStats().getUploaded();
					totalDiscarded += ps.getTotalDiscarded();
					connectedSeeds += dm.getNbSeeds();
					connectedPeers += dm.getNbPeers();
				}
				nrTorrent += 1;
				int stat = dm.getHealthStatus();
				response += ((nrTorrent < 10) ? " " : "") + "<b>"+Integer.toString(nrTorrent) + "</b> <span style=\"color:";
				if (stat ==DownloadManager.WEALTH_KO)
					response += "red";
				else if (stat==DownloadManager.WEALTH_NO_REMOTE)
					response += "olive";
				else if (stat==DownloadManager.WEALTH_NO_TRACKER)
					response += "blue";
				else if (stat==DownloadManager.WEALTH_OK)
					response += "green";
				else
					response +="gray";
				response += "\">[";
				if (dmstate == DownloadManager.STATE_INITIALIZING)
					response += "I";
				else if (dmstate == DownloadManager.STATE_ALLOCATING)
					response += "A";
				else if (dmstate == DownloadManager.STATE_CHECKING)
					response += "C";
				else if (dmstate == DownloadManager.STATE_DOWNLOADING)
					response += ">";
				else if (dmstate == DownloadManager.STATE_ERROR)
					response += "E";
				else if (dmstate == DownloadManager.STATE_SEEDING)
					response += "*";
				else if (dmstate == DownloadManager.STATE_STOPPED)
					response += "!";
				else if (dmstate == DownloadManager.STATE_WAITING)
					response += ".";
				else if (dmstate == DownloadManager.STATE_READY)
					response += ":";
				else
					response += "?";
				response += "]</span> ";
				DecimalFormat df = new DecimalFormat("000.0%");

				DownloadManagerStats stats = dm.getStats();

				response += df.format(stats.getCompleted() / 1000.0);
				response += " <span style=\"font-weight:bold";
				if (dmstate ==DownloadManager.STATE_DOWNLOADING)
					response +=";color:green";
				else if (dmstate == DownloadManager.STATE_ERROR)
					response += ";color:red";
				response +="\">";
				if (dmstate == DownloadManager.STATE_ERROR)
					response += dm.getErrorDetails();
				else {
					if (dm.getName() == null)
						response += "?";
					else
						response += dm.getName();
				}
				response += "</span> (" + DisplayFormatters.formatByteCountToKiBEtc(dm.getSize()) + ") <i><b>ETA:" + DisplayFormatters.formatETA(stats.getETA()) + "</b></i><br>\t\tSpeed: ";
				response += DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDownloadAverage()) + " / ";
				response += DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getUploadAverage()) + "\tAmount: ";
				response += DisplayFormatters.formatDownloaded(stats) + " / ";
				response += DisplayFormatters.formatByteCountToKiBEtc(stats.getUploaded()) + "\tConnections: ";
				if (hd == null || !hd.isValid()) {
					response += Integer.toString(dm.getNbSeeds()) + "(?) / ";
					response += Integer.toString(dm.getNbPeers()) + "(?)";
				} else {
					response += Integer.toString(dm.getNbSeeds()) + "(" + Integer.toString(hd.getSeeds()) + ") / ";
					response += Integer.toString(dm.getNbPeers()) + "(" + Integer.toString(hd.getPeers()) + ")";
				}
				response+="<br><br>";
				//out.println(ByteFormatter.nicePrintTorrentHash(dm.getTorrent(),
				// true));
			}
			response += "Total Speed (down/up): " + DisplayFormatters.formatByteCountToKiBEtcPerSec(UIConst.GM.getStats().getDownloadAverage()) + " / " + DisplayFormatters.formatByteCountToKiBEtcPerSec(UIConst.GM.getStats().getUploadAverage())+"<br>";
			response += "Transferred Volume (down/up/discarded): " + DisplayFormatters.formatByteCountToKiBEtc(totalReceived) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalSent) + " / " + DisplayFormatters.formatByteCountToKiBEtc(totalDiscarded)+"<br>";
			response += "Total Connected Peers (seeds/peers): " + Integer.toString(connectedSeeds) + " / " + Integer.toString(connectedPeers)+"<br>";
		} else
			response += "No Torrents<br>";
		response += "]]></description>"+tr+
								" </item>"+tr+
								"</channel>"+tr+
								"</rss>"+tr;
		return new httpOKResponse("application/rss+xml", new BufferElement(response.getBytes()));
	}

}
