package org.gudy.azureus2.ui.console.commands;

import java.io.PrintStream;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.console.ConsoleInput;

import com.aelitis.azureus.core.pairing.*;


public class Pairing extends IConsoleCommand {

	public Pairing()
	{
		super("pairing", "pair");
	}
	
	public String getCommandDescriptions()
	{
		return("pairing\t\trs\tShows and modified the current Vuze remote pairing state.");
	}
	
	public void printHelpExtra(PrintStream out, List args) {
		out.println("> -----");
		out.println("Subcommands:");
		out.println("enable\tEnable remote pairing");
		out.println("disable\tDisable remote pairing");
		out.println("> -----");
	}
	
	public void 
	execute(
		String commandName, ConsoleInput ci, List<String> args) {
		
		PairingManager pm = PairingManagerFactory.getSingleton();
		
		if ( args.size() > 0 ){
			
			String sub = args.get(0);
			
			if ( sub.equals( "enable" )){
				
				pm.setEnabled( true );
				
			}else if ( sub.equals( "disable" )){
				
				pm.setEnabled( false );
				
			}else{
			
				ci.out.println( "Unsupported sub-command: " + sub );
				
				return;
			}
		}
		
		ci.out.println( "Current pairing state:" );
		
		if ( pm.isEnabled()){
			
			ci.out.println( "\tStatus:      " + pm.getStatus());

			try{
				ci.out.println( "\tAccess code: " + pm.getAccessCode());
				
			}catch( Throwable e ){
				
				ci.out.println( "Failed to get access code: " + Debug.getNestedExceptionMessage( e ));
			}
		}else{
			ci.out.println( "\tdisabled" );
		}
	
	}
}
