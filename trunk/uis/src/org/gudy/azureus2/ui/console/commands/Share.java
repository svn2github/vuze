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
import java.util.List;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.console.ConsoleInput;

/**
 * @author tobi
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Share implements IConsoleCommand {
  
  public static void commandShare( final ConsoleInput ci, List args ) {
    if( args != null && args.size() == 2 ) {
      String[] sub_commands = new String[ args.size() ];
      args.toArray( sub_commands );
      for( int i=0; i<sub_commands.length; i++ ) {
        sub_commands[ i ] = sub_commands[ i ].trim();
      }
      
      final File path = new File( sub_commands[ 1 ] );
      if( !path.exists() ) {
        ci.out.println( "ERROR: path [" +path+ "] does not exist." );
        return;
      }
      
      if( sub_commands[ 0 ].equalsIgnoreCase( "file" ) ) {
        new AEThread( "shareFile" ) {
          public void runSupport() {
            try {
              ci.azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().addFile( path );
            }
            catch( Throwable e ) {
              ci.out.println( "ERROR: " + e.getMessage() + " ::");
              Debug.printStackTrace( e );
            }
          }
        }.start();
        ci.out.println( "File [" +path+ "] share being processed in background..." );
      }
      else if( sub_commands[ 0 ].equalsIgnoreCase( "folder" ) ) {
        new AEThread( "shareDir" ) {
          public void runSupport() {
            try {
              ci.azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().addDir( path );
            }
            catch( Throwable e ) {
              ci.out.println( "ERROR: " + e.getMessage() + " ::");
              Debug.printStackTrace( e );
            }
          }
        }.start();
        ci.out.println( "Folder [" +path+ "] share being processed in background..." );
      }
      else if( sub_commands[ 0 ].equalsIgnoreCase( "contents" ) ) {
        new AEThread( "shareDirCntents" ) {
          public void runSupport() {
            try {
              ci.azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().addDirContents( path, false );
            }
            catch( Throwable e ) {
              ci.out.println( "ERROR: " + e.getMessage() + " ::");
              Debug.printStackTrace( e );
            }
          }
        }.start();
        ci.out.println( "Folder contents [" +path+ "] share being processed in background..." );
      }
      else if( sub_commands[ 0 ].equalsIgnoreCase( "rcontents" ) ) {
        new AEThread( "shareDirCntentsRec" ) {
          public void runSupport() {
            try {
              ci.azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().addDirContents( path, true );
            }
            catch( Throwable e ) {
              ci.out.println( "ERROR: " + e.getMessage() + " ::");
              Debug.printStackTrace( e );
            }
          }
        }.start();
        ci.out.println( "Recursive folder contents [" +path+ "] share being processed in background..." );
      }
      else {
        ci.out.println( "ERROR: type '" + sub_commands[ 0 ] + "' unknown." );
      }
    }
    else {
      ci.out.println( "> -----" );
      ci.out.println( "[share <type> <path>]" );
      ci.out.println( "type options:" );
      ci.out.println( "file\t\t\tShare a single file." );
      ci.out.println( "folder\t\t\tShare a folder as a single multi-file torrent." );
      ci.out.println( "contents\t\tShare files and sub-dirs in a folder as single and multi-file torrents." );
      ci.out.println( "rcontents\t\tShare files and sub-dir files in a folder as separate torrents." );
      ci.out.println( "> -----" );
    }
  }
  
  
  public static void RegisterCommands() {
    try {
      ConsoleInput.RegisterCommand( "share", Share.class.getMethod( "commandShare", ConsoleCommandParameters ) );
      ConsoleInput.RegisterHelp( "share <type> <path>\t\t\tShare a file or folder(s). Use without parameters to get a list of available options." );
    }
    catch (Exception e) { System.out.println( "command registration error: " ); e.printStackTrace();  }
  }
}
