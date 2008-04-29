package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.download.DownloadManager;

public class VuzeShareUtils
{

	private static VuzeShareUtils instance;

	private SharePage sharePage = null;

	public static VuzeShareUtils getInstance() {
		if (null == instance) {
			instance = new VuzeShareUtils();
		}
		return instance;
	}

	public void shareTorrent(DownloadManager dm) {
		System.out.println("Sharing:");//KN: sysout
		System.out.println("\t: " + dm.getDisplayName());//KN: sysout

		dm.getTorrent();

		if (null != sharePage) {
			sharePage.setDownloadManager(dm);
		}
	}

	public SharePage getSharePage() {
		return sharePage;
	}

	public void setSharePage(SharePage sharePage) {
		this.sharePage = sharePage;
	}

}
