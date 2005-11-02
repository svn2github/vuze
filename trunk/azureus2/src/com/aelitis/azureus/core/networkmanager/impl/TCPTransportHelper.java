/*
 * Created on Nov 1, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;



/**
 * 
 */
public class TCPTransportHelper {
	
	private static boolean enable_efficient_io = Constants.JAVA_VERSION.startsWith("1.5");

	private final SocketChannel channel;
	
	public TCPTransportHelper( SocketChannel _channel ) {
		this.channel = _channel;
	}
	

  public long write( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
  	if( channel == null ) {
      Debug.out( "channel == null" );
      return 0;
    }
    
  	if( enable_efficient_io ) {
  		try {
  			return channel.write( buffers, array_offset, length );
  		}
  		catch( IOException ioe ) {
  			//a bug only fixed in Tiger (1.5 series):
  			//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
  			String msg = ioe.getMessage();
  			if( msg != null && msg.equals( "A non-blocking socket operation could not be completed immediately" ) ) {
  				enable_efficient_io = false;
  				LGLogger.logUnrepeatableAlert( LGLogger.AT_WARNING, "WARNING: Multi-buffer socket write failed; switching to single-buffer mode.\nUpgrade to JRE 1.5 (5.0) series to fix this problem!" );
  			}
  			throw ioe;
  		}
  	}
    
  	//single-buffer mode
  	long written_sofar = 0;
  	for( int i=array_offset; i < (array_offset + length); i++ ) {
  		int data_length = buffers[ i ].remaining();
  		int written = channel.write( buffers[ i ] );
  		written_sofar += written;
  		if( written < data_length ) {
  			break;
  		}
  	}
      
  	return written_sofar;
  }

  
  
  public long read( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {  	
    if( channel == null ) {
      Debug.out( "channel == null" );
      return 0;
    }
    
    if( buffers == null ) {
      Debug.out( "read: buffers == null" );
      return 0;
    }
    
    
    long bytes_read = 0;
    
    if( enable_efficient_io ) {
      try{
        bytes_read = channel.read( buffers, array_offset, length );
      }
      catch( IOException ioe ) {
        //a bug only fixed in Tiger (1.5 series):
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
        String msg = ioe.getMessage();
        if( msg != null && msg.equals( "A non-blocking socket operation could not be completed immediately" ) ) {
          enable_efficient_io = false;
          LGLogger.logUnrepeatableAlert( LGLogger.AT_WARNING, "WARNING: Multi-buffer socket read failed; switching to single-buffer mode.\nUpgrade to JRE 1.5 (5.0) series to fix this problem!" );
        }
        
        throw ioe;
      }
    }
    else {
      //single-buffer mode
      for( int i=array_offset; i < (array_offset + length); i++ ) {
        int data_length = buffers[ i ].remaining();
        int read = channel.read( buffers[ i ] );
        bytes_read += read;
        if( read < data_length ) {
          break;
        }
      }
    }    

    if( bytes_read < 0 ) {
      throw new IOException( "end of stream on socket read" );
    }

    return bytes_read;
  }
  

  
  public SocketChannel getSocketChannel(){  return channel;  }
	
}
