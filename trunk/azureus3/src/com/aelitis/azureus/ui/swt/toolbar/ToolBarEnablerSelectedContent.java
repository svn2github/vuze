package com.aelitis.azureus.ui.swt.toolbar;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.IconBarEnabler;

import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;

public class ToolBarEnablerSelectedContent implements ISelectedContent {

	private IconBarEnabler enabler;
	
	public ToolBarEnablerSelectedContent(IconBarEnabler enabler) {
		this.enabler = enabler;
	}
	
	public DownloadManager getDownloadManager() {
		return null;
	}

	public TOTorrent getTorrent() {
		return null;
	}
	public String getDisplayName() {
		return "";
	}

	public DownloadUrlInfo getDownloadInfo() {
		return null;
	}

	public String getHash() {
		return null;
	}

	public void setDownloadManager(DownloadManager dm) {

	}

	public void setTorrent(TOTorrent t ){
		
	}
	
	public void setDisplayName(String displayName) {

	}

	public void setDownloadInfo(DownloadUrlInfo downloadInfo) {

	}

	public void setHash(String hash) {

	}
	
	public IconBarEnabler getIconBarEnabler() {
		return enabler;
	}

}
