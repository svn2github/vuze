package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.download.DownloadManager;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;

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

	public void shareTorrent(final DownloadManager dm) {
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				if (null != sharePage) {
					SelectedContent currentContent;
					try {
						currentContent = new SelectedContent(dm);
						currentContent.displayName = PlatformTorrentUtils.getContentTitle2(dm);

						sharePage.setShareItem(currentContent);
					} catch (Exception e) {
					}
				}
			}
		});
	}

	public SharePage getSharePage() {
		return sharePage;
	}

	public void setSharePage(SharePage sharePage) {
		this.sharePage = sharePage;
	}

}
