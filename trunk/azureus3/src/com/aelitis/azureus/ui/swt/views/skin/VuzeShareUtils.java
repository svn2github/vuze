package com.aelitis.azureus.ui.swt.views.skin;

import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeShareable;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.cnetwork.impl.ContentNetworkVuze;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.ISelectedVuzeFileContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.shells.friends.SharePage;
import com.aelitis.azureus.ui.swt.shells.friends.ShareWizard;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.DataSourceUtils;

public class VuzeShareUtils
{
	private long DATE_CANSHARENONVUZECN = new GregorianCalendar(2009, 2, 14).getTimeInMillis();

	private static VuzeShareUtils instance;

	public static VuzeShareUtils getInstance() {
		if (null == instance) {
			instance = new VuzeShareUtils();
		}
		return instance;
	}

		/**
		 * Used by EMP
		 * @deprecated
		 * @param content
		 * @param referer
		 */
	
	public void 
	shareTorrent(
		ISelectedContent content, String referer) 
	{
		shareContent( content, null, referer );
	}
	
	public void
	shareContent(
		ISelectedContent 	content, 
		final VuzeBuddy[] defaultSelectedBuddies,
		String 				referer )
	{
		if (content instanceof SelectedContentV3) {
			SelectedContentV3 sc = (SelectedContentV3) content;
			shareContent(sc, defaultSelectedBuddies, referer);
		} else if (content instanceof SelectedContent) {
			SelectedContent sc = (SelectedContent) content;
			shareContent(new SelectedContentV3(sc), defaultSelectedBuddies, referer);
		}else if ( content instanceof ISelectedVuzeFileContent ){
			
			shareVuzeFile((ISelectedVuzeFileContent)content, defaultSelectedBuddies, referer );
			
		}else{
			
			Debug.out( "No share method defined for " + content );
		}
	}

	public void 
	shareContent(
		final SelectedContentV3 	content,
		final VuzeBuddy[] defaultSelectedBuddies,
		final String 				referer ) 
	{
		
		final DownloadManager dm = content.getDownloadManager();
		
		if (!canShare(content)) {
			Debug.out("Tried to share " + content.getHash()
					+ " but not shareable");
			return;
		}

		PlatformBuddyMessenger.startShare(referer,
				content.isPlatformContent() ? content.getHash() : null);

		if (!VuzeBuddyManager.isEnabled()) {
			VuzeBuddyManager.showDisabledDialog();
			return;
		}

			//TODO : Gudy : make sure that this private detection method is reliable enough
		
		if ( dm != null	&& (TorrentUtils.isReallyPrivate(content.getTorrent()))) {
			Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "v3.share.private",
					(String[]) null);
			return;
		}


		VuzeShareable	shareable = 
			new	VuzeShareable()
			{
				public String
				getHash()
				{
					return( content.getHash());
				}
				
				public String
				getDisplayName()
				{
					return( content.getDisplayName());
				}
				
				public String
				getThumbURL()
				{
					return( content.getThumbURL());
				}
				
				public boolean
				isPlatformContent()
				{
					return( content.isPlatformContent());
				}
				
				public String 
				getPublisher() 
				{
					if ( dm == null ){
						
						return( null );
					}
					
					return( PlatformTorrentUtils.getContentPublisher(dm.getTorrent()));
				}
				
				public long 
				getSize() 
				{
					if ( dm == null ){
						
						return( 0 );
					}
					
					return( dm.getSize());
				}
				
				public byte[]
				getImageBytes()
				{
					return( content.getImageBytes());
				}
				
				public boolean
				canPlay()
				{
					return( content.canPlay());
				}
				
				public TOTorrent 
				getTorrent() 
				{
					return(content.getTorrent());
				}
				
				public DownloadManager 
				getDownloadManager() 
				{
					return( content.getDownloadManager());
				}
				
				public DownloadUrlInfo 
				getDownloadInfo() 
				{
					return( content.getDownloadInfo());
				}
			};

		doShare( shareable, defaultSelectedBuddies, referer );
	}
	
	public void 
	shareVuzeFile(
		final ISelectedVuzeFileContent 		content,
		final VuzeBuddy[] defaultSelectedBuddies,
		final String 						referer ) 
	{
		
		PlatformBuddyMessenger.startShare( referer, null );

		if (!VuzeBuddyManager.isEnabled()){
			
			VuzeBuddyManager.showDisabledDialog();
			
			return;
		}

		VuzeShareable	shareable = 
			new	VuzeShareable()
			{
				public String
				getHash()
				{
					return( content.getHash());
				}
				
				public String
				getDisplayName()
				{
					return( content.getDisplayName());
				}
				
				public String
				getThumbURL()
				{
					return( null );
				}
				
				public boolean
				isPlatformContent()
				{
					return( false );
				}
				
				public String 
				getPublisher() 
				{
					return( null );
				}
				
				public long 
				getSize() 
				{
					return( 0 );
				}
				
				public byte[]
				getImageBytes()
				{
					return( null );
				}
				
				public boolean
				canPlay()
				{
					return( false );
				}
				
				public TOTorrent 
				getTorrent() 
				{
					return( content.getTorrent());
				}
				
				public DownloadManager 
				getDownloadManager() 
				{
					return( null );
				}
				
				public DownloadUrlInfo 
				getDownloadInfo() 
				{
					return( null );
				}
			};

		doShare( shareable, defaultSelectedBuddies, referer );
	}
	
	protected void
	doShare(
		final VuzeShareable	shareable,
		final VuzeBuddy[] defaultSelectedBuddies,
		final String		referer )
	{
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				try {
					//sharePage.setShareItem(currentContent, referer);

					ShareWizard wizard = new ShareWizard(
							UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell(),
							SWT.DIALOG_TRIM | SWT.RESIZE);
					wizard.setText("Vuze - Wizard");
					wizard.setSize(500, 550);

					SharePage newSharePage = (SharePage) wizard.getPage( SharePage.ID );
				
					newSharePage.setShareItem( shareable, referer );
					
					/*
					 * Opens a centered free-floating shell
					 */

					UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
					
					if (null == uiFunctions) {
						/*
						 * Centers on the active monitor
						 */
						Utils.centreWindow(wizard.getShell());
					} else {
						/*
						 * Centers on the main application window
						 */
						Utils.centerWindowRelativeTo(wizard.getShell(),
								uiFunctions.getMainShell());
					}

					wizard.open();

					if (defaultSelectedBuddies != null) {
						for (VuzeBuddy vuzeBuddy : defaultSelectedBuddies) {
							if (vuzeBuddy instanceof VuzeBuddySWT) {
								newSharePage.addBuddy((VuzeBuddySWT) vuzeBuddy);
							}
						}
					}


				} catch (Throwable e) {
					Debug.printStackTrace(e);
				}
			}
		});
	}

	public boolean canShare(Object datasource) {
		TOTorrent torrent = DataSourceUtils.getTorrent(datasource);
		if (torrent == null) {
			if (DataSourceUtils.getHash(datasource) != null) {
				return true;
			}
			return false;
		}
		
		long id = PlatformTorrentUtils.getContentNetworkID(torrent);
		if (id == ContentNetwork.CONTENT_NETWORK_UNKNOWN) {
			return true;
		}
		if (SystemTime.getCurrentTime() >= DATE_CANSHARENONVUZECN) {
			return true;
		}
		ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
				id);
		return cn == null || cn.getID() == ContentNetworkVuze.CONTENT_NETWORK_VUZE;
	}

}
