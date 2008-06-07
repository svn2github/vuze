package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.Constants;

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

	public void shareTorrent(final SelectedContent currentContent,
			final String referer) {
		if (Constants.DISABLE_BUDDIES_BAR) {
			return;
		}
		PlatformBuddyMessenger.startShare(referer,
				currentContent.isPlatformContent() ? currentContent.getHash() : null);

		if (!VuzeBuddyManager.isEnabled()) {
			VuzeBuddyManager.showDisabledDialog();
			return;
		}
		
		//TODO : Gudy : make sure that this private detection method is reliable enough
		if (currentContent.getDM() != null
				&& (TorrentUtils.isReallyPrivate(currentContent.getDM().getTorrent()))) {
			Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "v3.share.private",
					(String[]) null);
			return;
		}

		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				if (null != sharePage) {
					try {
						sharePage.setShareItem(currentContent, referer);
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
