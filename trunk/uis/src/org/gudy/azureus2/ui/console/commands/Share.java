/*
 * Written and copyright 2001-2003 Tobias Minich. Distributed under the GNU
 * General Public License; see the README file. This code comes with NO
 * WARRANTY.
 * 
 * AddFind.java
 * 
 * Created on 23.03.2004
 *
 */
package org.gudy.azureus2.ui.console.commands;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Share extends IConsoleCommand {
	
	public Share()
	{
		super(new String[] {"share"});
	}
	
	public String getCommandDescriptions() {
		return("share <type> <path> [<properties>]\t\t\tShare a file or folder(s). Use without parameters to get a list of available options." );
	}
	
	public void printHelp(PrintStream out, List args) {
		out.println( "> -----" );
		out.println( "[share <type> <path> [<properties>]" );
		out.println( "type options:" );
		out.println( "file           Share a single file." );
		out.println( "folder         Share a folder as a single multi-file torrent." );
		out.println( "contents       Share files and sub-dirs in a folder as single and multi-file torrents." );
		out.println( "rcontents      Share files and sub-dir files in a folder as separate torrents." );
		out.println( "list           List the shares (path not required)");
		out.println( "      <properties> is semicolon separated <name>=<value> list.");
		out.println( "      Defined <name> values are 'category' only");
		out.println( "      For example: share file /tmp/wibble.mp3 category=music");
		out.println( "> -----" );
	}
	
	public void execute(String commandName,	final ConsoleInput ci, List args ) 
	{
		if( args.isEmpty() )
		{
			printHelp(ci.out, args);
			return;
		}
		final ShareManager share_manager;
		try{
			share_manager = ci.azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager();
		}catch( ShareException e ){
			ci.out.println( "ERROR: " + e.getMessage() + " ::");
			Debug.printStackTrace( e );
			return;
		}
		
		final String arg = (String) args.remove(0);
		if ( args.isEmpty() && ("list".equalsIgnoreCase(arg)) ) {
			ShareResource[]	shares = share_manager.getShares();
			if( shares.length == 0 )
				ci.out.println("> No shares found");
			else
			{
				for (int i=0;i<shares.length;i++){
					ci.out.println( "> " + i + ": " + shares[i].getName());
				}
			}
			return;
		} 
		
		final File path = new File( (String) args.get(0) );
		if( !path.exists() ) {
			ci.out.println( "ERROR: path [" +path+ "] does not exist." );
			return;
		}
		
		String	category = null;
		
		if ( args.size() == 2 ){
			String	properties = (String) args.get(1);
			int	pos = properties.indexOf("=");
			
			if ( pos == -1 ){
				ci.out.println( "ERROR: invalid properties string '" + properties + "'" );
				return;
			}
			
			category = properties.substring( pos+1 );
		}
		
		final String		f_category	= category;
			
		new AEThread( "shareFile" ) 
		{
			public void 
			runSupport() 
			{
				try{
					ShareResource resource = share_manager.getShare( path );
					
					if( "file".equalsIgnoreCase( arg ) ) {

						ci.out.println( "File [" +path+ "] share being processed in background..." );

						if ( resource == null ){
								
							resource = share_manager.addFile( path );
						}
							
					}else if( "folder".equalsIgnoreCase( arg ) ) {
						
						ci.out.println( "Folder [" +path+ "] share being processed in background..." );

						if ( resource == null ){
							
							resource = share_manager.addDir( path );	
						}
						
					}else if( "contents".equalsIgnoreCase( arg ) ) {
						
						ci.out.println( "Folder contents [" +path+ "] share being processed in background..." );

						if ( resource == null ){
							
							resource = share_manager.addDirContents( path, false );
						}
							
					}else if( "rcontents".equalsIgnoreCase( arg ) ) {
						
						ci.out.println( "Folder contents recursive [" +path+ "] share being processed in background..." );

						if ( resource == null ){
							
							resource = share_manager.addDirContents( path, true );
						}
					}else{
						
						ci.out.println( "ERROR: type '" + arg + "' unknown." );
						
					}
					
					String	cat = f_category;
					
					if ( resource != null && cat != null ){
						
						if ( cat.length() == 0 ){
							
							cat	= null;
						}
						
						TorrentManager tm = ci.azureus_core.getPluginManager().getDefaultPluginInterface().getTorrentManager();
						
						resource.setAttribute( tm.getAttribute( TorrentAttribute.TA_CATEGORY), cat );
					}
					
					if ( resource != null ){
						
						ci.out.println( "... processing complete" );
					}
				}catch( Throwable e ) {
					
					ci.out.println( "ERROR: " + e.getMessage() + " ::");
					
					Debug.printStackTrace( e );
				}
			}
		}.start();
	}
}
