package com.aelitis.azureus.ui.swt.browser.listener;

import java.net.URL;

import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;

public class TorrentListener extends AbstractMessageListener
{
    public static final String DEFAULT_LISTENER_ID = "torrent";

  	public static final String OP_LOAD_TORRENT_OLD = "loadTorrent";

  	public static final String OP_LOAD_TORRENT = "load-torrent";
	
    private AzureusCore core;
	private Shell shell;
	
	public TorrentListener(AzureusCore core) {
        this(DEFAULT_LISTENER_ID, core);
	}
	
    public TorrentListener(String id, AzureusCore core) {
        super(id);
        this.core = core;
    }
    
    /**
		 * 
		 */
		public TorrentListener() {
			this(AzureusCoreFactory.getSingleton());
		}

		public void setShell(Shell shell) {
		this.shell = shell;
	}


    public void handleMessage(BrowserMessage message) {
		if ( OP_LOAD_TORRENT.equals(message.getOperationId())
				|| OP_LOAD_TORRENT_OLD.equals(message.getOperationId()) ) {
            loadTorrent(message.getDecodedObject().getString("url"));
        }
        else {
            throw new IllegalArgumentException("Unknown operation: " + message.getOperationId());
        }
	}
	
	private void loadTorrent(String url) {
		try {
			core.getPluginManager().getDefaultPluginInterface().getDownloadManager().addDownload(new URL(url), true);
		} catch (Exception e) {
			Debug.out(e);
		}
	}
}
