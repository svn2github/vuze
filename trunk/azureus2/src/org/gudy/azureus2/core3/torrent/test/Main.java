/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent.test;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.*;

public class 
Main
{
	static int TT_ENCODE		= 1;
	static int TT_DECODE		= 2;
	static int TT_CREATE		= 3;
	
	static void
	usage()
	{
		System.err.println( "Usage: encode|decode|create" );
		
		System.exit(1);
	}
	
	public static void
	main(
		String[]	args )
	{
		int	test_type= 0;
		
		if ( args.length != 1 ){
			
			usage();
		}
		
		if ( args[0].equalsIgnoreCase( "encode" )){
			
			test_type = TT_ENCODE;
			
		}else if ( args[0].equalsIgnoreCase( "decode" )){
			
			test_type = TT_DECODE;
			
		}else if ( args[0].equalsIgnoreCase( "create" )){
			
			test_type = TT_CREATE;
						
		}else{
			
			usage();
		}
		
		try{
			if ( test_type == TT_ENCODE ){
				
				File f = new File("C:\\temp\\test.torrent");
						
				TOTorrent torrent = TOTorrentFactory.deserialiseFromFile( f );
			
				torrent.print();
			
				torrent.serialiseToFile( new File("c:\\temp\\test2.torrent"));
			
			}else if ( test_type == TT_DECODE ){
				
					 
				File f = new File("c:\\temp\\test.torrent" );
			
				TOTorrent torrent = TOTorrentFactory.deserialiseFromFile( f );
			
				torrent.print();		
				
			}else if ( test_type == TT_CREATE ){
			
				TOTorrentProgressListener list = new TOTorrentProgressListener()
					{
						public void
						reportProgress(
							int		p )
						{
							System.out.println( "" + p );
						}
					};
				
				boolean	do_file = false;
				
				TOTorrent t;
				
				if ( do_file ){
					
					t = TOTorrentFactory.createFromFileOrDir( new File("c:\\temp\\test.wmf"), 1024*10, new URL( "http://beaver:6969/announce" ), list );			
					
				}else{
	
					t = TOTorrentFactory.createFromFileOrDir( new File("c:\\temp\\scans"), 1024*256, new URL("http://beaver:6969/announce" ), list);
				}
				
				t.print();
				
				t.serialiseToFile( new File("c:\\temp\\test.torrent" ));						 
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}
	}
}