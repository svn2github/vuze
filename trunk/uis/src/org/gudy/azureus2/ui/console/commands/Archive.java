package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.ui.console.ConsoleInput;



public class Archive extends IConsoleCommand {

	public Archive()
	{
		super("archive", "ar");
	}
	
	public String getCommandDescriptions()
	{
		return("archive\t\tar\tShows and allows the restoration of archived torrents.");
	}
	
	public void printHelpExtra(PrintStream out, List args) {
		out.println("> -----");
		out.println("Subcommands:");
		out.println("list\tl\t\tList archived torrents");
		out.println("restore <num>\tres\t\tRestore archived torrent");
		out.println("delete <num>\tdel\t\tDeletes archived torrent");
		out.println("> -----");
	}
	
	public void 
	execute(
		String commandName, ConsoleInput ci, List<String> args) {
				
		if ( args.size() > 0 ){
			
			PluginInterface pi = ci.getCore().getPluginManager().getDefaultPluginInterface();
			
			DownloadStub[] stubs = pi.getDownloadManager().getDownloadStubs();

			String sub = args.get(0);
			
			int	index = -1;
			
			if ( args.size() > 1 ){
				
				String index_str = args.get(1);
				
				try{
					index = Integer.parseInt( index_str );
					
					index--;
					
					if ( index < 0 || index >= stubs.length ){
											
						index = -1;
					}
					
				}catch( Throwable e ){
				}
				
				if ( index == -1 ){
					
					ci.out.println( "Invalid archive index: " + index_str );
				}
			}
			
			if ( sub.equals( "list" ) || sub.equals( "l" )){
					
				int pos = 1;
				
				ci.out.println( "> -----" );
				
				for ( DownloadStub stub: stubs ){
					
					System.out.println( " " + (pos++) + "\t" + stub.getName());
				}
				
				ci.out.println( "> -----" );
				
			}else if ( index != -1 && ( sub.equals( "restore" ) || sub.equals( "res" ))){
				
				
				
				try{
					Download d = stubs[index].destubbify();
					
					ci.out.println( "> Restore of " + d.getName() + " succeeded." );
					
				}catch( Throwable e ){
					
					ci.out.print( e );
				}
				
			}else if ( index != -1 && ( sub.equals( "delete" ) || sub.equals( "del" ))){	
					
				try{
					DownloadStub stub = stubs[index];
					
					String 	name = stub.getName();
					
					stub.remove();
					
					ci.out.println( "> Delete of " + name + " succeeded." );

				}catch( Throwable e ){
					
					ci.out.print( e );
				}

			}else{
			
				ci.out.println( "Unsupported sub-command: " + sub );
				
				return;
			}
		}else{
			
			printHelp( ci.out, args );
		}
		
	}
}
