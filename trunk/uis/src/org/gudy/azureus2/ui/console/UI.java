/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.console;


import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIException;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIInstanceFactory;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.UIManagerEventListener;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.console.multiuser.UserManager;
import org.gudy.azureus2.ui.console.multiuser.commands.UserCommand;

/**
 *
 * @author  Tobias Minich
 */
public class 
UI 
	extends org.gudy.azureus2.ui.common.UITemplateHeadless 
	implements IUserInterface, UIInstanceFactory, UIInstance, UIManagerEventListener
{
  
  private ConsoleInput console = null;
  
  /** Creates a new instance of Main */
  /*public UI() {
  }*/
  
  public void init(boolean first, boolean others) {
    super.init(first,others);
    System.setProperty("java.awt.headless", "true");
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    super.startUI();
    PluginInterface pi = UIConst.getAzureusCore().getPluginManager().getDefaultPluginInterface();
    
    UIManager	ui_manager = pi.getUIManager();
    
    ui_manager.addUIEventListener( this );
    
    try{
    	ui_manager.attachUI( this );
    }catch( UIException e ){
    	e.printStackTrace();
    }
    TorrentDownloaderFactory.initManager(UIConst.getGlobalManager(), true, true, COConfigurationManager.getStringParameter("Default save path") );
    
    if ((!isStarted()) || (console == null) || (!console.isAlive())) {
//      ConsoleInput.printconsolehelp(System.out);
      System.out.println();
      console = new ConsoleInput("Main", UIConst.getAzureusCore(), System.in, System.out, Boolean.TRUE);
      if( System.getProperty("azureus.console.multiuser") != null)
      {
    	  UserManager manager = UserManager.getInstance(pi);
    	  console.registerCommand(new UserCommand(manager));
      }
      console.printconsolehelp();
    }
  }
  
  public void openRemoteTorrent(String url) {
	  if (console != null) {
		  console.downloadRemoteTorrent(url);
		  return;
	  }
	  System.out.println( "Downloading torrent from url: " + url );
      TorrentDownloaderFactory.downloadManaged(url);
      return; 
  }
  
  public void openTorrent(String fileName) {
  	if( console != null )
  	{
//  		System.out.println("NOT NULL CONSOLE. CAN PASS STRAIGHT TO IT!");
  		console.downloadTorrent(fileName);
  		return;
  	}
  	else
  	{
//  		System.out.println("NULL CONSOLE");
  	}
    if( fileName.toUpperCase().startsWith( "HTTP://" ) ) {
      System.out.println( "Downloading torrent from url: " + fileName );
      TorrentDownloaderFactory.downloadManaged( fileName );
      return;
    }
    
    try {
      if (!TorrentUtils.isTorrentFile(fileName)) {//$NON-NLS-1$
        Logger.getLogger("azureus2.ui.console").error(fileName+" doesn't seem to be a torrent file. Not added.");
        return;
      }
    } catch (Exception e) {
      Logger.getLogger("azureus2.ui.console").error("Something is wrong with "+fileName+". Not added. (Reason: "+e.getMessage()+")");
      return;
    }
    if (UIConst.getGlobalManager()!=null) {
      try {
      	String downloadDir = COConfigurationManager.getDirectoryParameter("Default save path");
      	System.out.println( "Adding torrent: " + fileName + " and saving to " + downloadDir);
        UIConst.getGlobalManager().addDownloadManager(fileName, downloadDir);
      } catch (Exception e) {
        Logger.getLogger("azureus2.ui.console").error("The torrent "+fileName+" could not be added.", e);
      }
    }
  }
  
	public UIInstance
	getInstance(
		PluginInterface		plugin_interface )
	{
		return( this );
	}

	public void
	detach()
	
		throws UIException
	{
	}
	
	public boolean
	eventOccurred(
		UIManagerEvent	event )
	{
		Object	data = event.getData();
		
		switch( event.getType()){
		
			case UIManagerEvent.ET_SHOW_TEXT_MESSAGE:					// data is String[] - title, message, text
			{
				String[]	bits = (String[])data;
				
				for (int i=0;i<bits.length;i++){
				
					console.out.println( bits[i] );
				}
				
				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE:				// data is File
			{
				openTorrent(((File)data).toString());
				
				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_URL:				// data is Object[]{URL,URL,Boolean} - { torrent_url, referrer url, auto_download}
			{
				openRemoteTorrent(((URL)((Object[])data)[0]).toExternalForm());
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_CREATED:			// data is PluginViewModel (or subtype)
			{
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_CREATED:		// data is PluginConfigModel (or subtype)
			{
				break;
			}
			case UIManagerEvent.ET_COPY_TO_CLIPBOARD:					// data is String
			{
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_DESTROYED:		// data is PluginViewModel (or subtype)
			{
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_DESTROYED:		// data is PluginConfigModel (or subtype)
			{
				break;
			}
			case UIManagerEvent.ET_OPEN_URL:							// data is URL
			{
				break;
			}
			case UIManagerEvent.ET_CREATE_TABLE_COLUMN:				// data is String[] - table_id, cell_id: result is TableColumn
			{
					// need to return a dummy TableColumn as a result?
				
				return( false );

			}
			case UIManagerEvent.ET_ADD_TABLE_COLUMN:					// data is TableColumn previously created
			{
				break;
			}
			case UIManagerEvent.ET_ADD_TABLE_CONTEXT_MENU_ITEM:		// data is TableContextMenuItem
			{
				break;
			}
			case UIManagerEvent.ET_SHOW_CONFIG_SECTION:				// data is String - section id
			{
				event.setResult(new Boolean(false));
				
				break;
			}
			default:
			{
				console.out.println( "Unrecognised UI event '" + event.getType() + "'" );
			}
		}
		
		return( true );
	}
	
	public int promptUser(String title, String text, String[] options,
			int defaultOption) {
		console.out.println("Prompt: " + title);
		console.out.println(text);

		String sOptions = "Options: ";
		for (int i = 0; i < options.length; i++) {
			if (i != 0) {
				sOptions += ", ";
			}
			sOptions += "[" + i + "]" + options[i];
		}
		
		console.out.println(sOptions);
		
		console.out.println("WARNING: Option [" + defaultOption + 
				"] automatically selected. " +
				"Console UI devs need to implement this function!");

		return defaultOption;
	}
	
	/** Not yet supported. **/
	public UIInputReceiver getInputReceiver() {
		return null;
	}
}
