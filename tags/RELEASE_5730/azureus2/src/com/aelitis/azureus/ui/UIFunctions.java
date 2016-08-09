/*
 * Created on Jun 14, 2006 9:02:55 PM
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.aelitis.azureus.ui;


import java.util.Map;

import org.gudy.azureus2.core3.torrent.impl.TorrentOpenOptions;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Jun 14, 2006
 *
 */
public interface UIFunctions
	extends AzureusCoreComponent
{
	public static final String	MAIN_WINDOW_NAME 		= System.getProperty( "azureus.window.title", Constants.APP_NAME 		+ " Bittorrent Client" );
	public static final String	MAIN_WINDOW_NAME_PLUS 	= System.getProperty( "azureus.window.title", Constants.APP_PLUS_NAME 	+ " Bittorrent Client" );
		
	public static int STATUSICON_NONE = 0;

	public static int STATUSICON_WARNING = 1;

	public static int STATUSICON_ERROR = 2;

	/** @deprecated Used by azburn */
	public static final int VIEW_MYTORRENTS = 8;
	/** @deprecated Used by rcm */
	public static final int VIEW_CONFIG = 4;
	/** @deprecated Used by rcm */
	public static final int VIEW_DM_DETAILS = 5;

	
	public static final int ACTION_FULL_UPDATE				= 1;	// arg: String - url; response Boolean - ok
	public static final int ACTION_UPDATE_RESTART_REQUEST	= 2;	// arg: Boolean - true->no auto-select response Boolean - ok
	
	public static final int VS_TRAY_ONLY				= 1;		// low-resource minimized state
	public static final int VS_MINIMIZED_TO_TRAY		= 2;		// minimized to tray only
	public static final int VS_MINIMIZED				= 3;		// normal minimized
	public static final int VS_ACTIVE					= 4;		// active

	public int
	getUIType();

	/**
	 * Bring main window to the front
	 */
	void bringToFront();

	/**
	 * Bring main window to the front
	 * 
	 * @param tryTricks: try tricks to force it to the top
	 *
	 * @since 3.0.1.7
	 */
	void bringToFront(boolean tryTricks);
	
	public int getVisibilityState();
	
	/**
	 * Change/Refresh the language of the UI
	 */
	void refreshLanguage();

	/**
	 * 
	 */
	void refreshIconBar();

	
	/**
	 * @param string
	 */
	void setStatusText(String string);

	void setStatusText(int statustype, String string, UIStatusTextClickListener l);

	/**
	 * Request the UI be shut down.
	 * 
	 * @return true - request granted, UI is being shut down
	 *         false - request denied (example: password entry failed)
	 */
	boolean dispose(boolean for_restart, boolean close_already_in_progress);

	boolean viewURL(String url, String target, int w, int h, boolean allowResize,
			boolean isModal);

	boolean viewURL(String url, String target, double wPct, double hPct,
			boolean allowResize, boolean isModal);

	void viewURL(String url, String target, String sourceRef);


	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption);

	public void promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean bRememberByDefault, int autoCloseInMS, UserPrompterResultListener l);
	
	/**
	 * Retrieves the class that handles periodically updating the UI
	 * 
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public UIUpdater getUIUpdater();

	/**
	 * @deprecated Use {@link #getMDI()}
	 */
	void openView(int viewID, Object datasource);
	
	void doSearch(String searchText );
	
	void doSearch(String searchText, boolean toSubscribe );

	public void
	installPlugin(
		String			plugin_id,
		String			resource_prefix,
		actionListener	listener );
	
	/**
	 * 
	 * @param action_id
	 * @param args
	 * @param listener
	 */
	public void
	performAction(
		int				action_id,
		Object			args,
		actionListener	listener );
	
	interface 
	actionListener
	{
		public void
		actionComplete(
			Object		result );
	}

	/**
	 * Retrieve the MDI (Sidebar, TabbedMDI)
	 * @return
	 */
	public MultipleDocumentInterface getMDI();		

	/**
	 * Might launch the old-school Mr Slidey
	 */
	void forceNotify(int iconID, String title, String text, String details,
			Object[] relatedObjects, int timeoutSecs);	
	
	public void
	runOnUIThread(
		int			ui_type,
		Runnable	runnable );
	
	public boolean
	isProgramInstalled(
		String		extension,
		String		name );
	
	public void
	openRemotePairingWindow();
	
	public void
	playOrStreamDataSource(
		Object 		ds, 
		String 		referal,
		boolean 	launch_already_checked, 
		boolean 	complete_only );

	public static final String 	OTO_DEFAULT_TO_STOPPED			= "defaultStopped";		// Boolean
	public static final boolean OTO_DEFAULT_TO_STOPPED_DEFAULT	= false;
	
	public static final String 	OTO_FORCE_OPEN					= "forceOpen";			// Boolean
	public static final boolean OTO_FORCE_OPEN_DEFAULT			= false;
	
	public static final String 	OTO_SILENT						= "silent";				// Boolean
	public static final boolean OTO_SILENT_DEFAULT				= false;
	
	public static final String 	OTO_HIDE_ERRORS					= "hideErrors";			// Boolean
	public static final boolean OTO_HIDE_ERRORS_DEFAULT			= false;
	
	/**
	 * Opens the Torrent Add Options Window, if configured to
	 * 
	 * @param force  Override configuration, show it!
	 * @return true if torrent was added
	 */
	public boolean addTorrentWithOptions(boolean force, TorrentOpenOptions torrentOptions);

	public boolean addTorrentWithOptions(TorrentOpenOptions torrentOptions, Map<String,Object> addOptions );

	public void showErrorMessage(String keyPrefix, String details, String[] textParams); 
	
	public void showCreateTagDialog(TagReturner tagReturner);
	
	public int adjustPXForDPI( int px );
	
	public interface 
	TagReturner 
	{
		public void returnedTags(Tag[] tags);
	}
}
