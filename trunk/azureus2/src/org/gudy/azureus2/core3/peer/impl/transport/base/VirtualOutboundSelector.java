/*
 * Created on Aug 17, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.peer.impl.transport.base;

import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;

/**
 * This is a virtual selector for making outbound socket channel
 * connects.  It does *not* use a standard java selector for select
 * operations, because it seems to cause kernel panics for dual-cpu
 * mac users under osx.
 */
public class VirtualOutboundSelector {
  private static final int POLL_TIMEOUT = 2*60*1000;  //2min
  private final ArrayList objects_to_poll = new ArrayList();
  private Map channels_selected = new HashMap();

  
  
  protected VirtualOutboundSelector() {
    //blank
  }
  
  
  protected void register( SocketChannel channel ) {
    objects_to_poll.add( new PollObject( channel ) );
  }
  
  
  protected int select() {
    int num_selected = 0;
    //do the poll
    for( int i=0; i < objects_to_poll.size(); i++ ) {
      PollObject po = (PollObject)objects_to_poll.get( i );
      try {
        if( po.channel.finishConnect() ) {
          objects_to_poll.remove( i );
          channels_selected.put( po.channel, null );
          num_selected++;
        }
        else if( SystemTime.getCurrentTime() - po.poll_start_time > POLL_TIMEOUT ) {
          objects_to_poll.remove( i );
          channels_selected.put( po.channel, new String("Connect attempt timed out") );
          num_selected++;
        }
      }
      catch( Throwable t ) {
        objects_to_poll.remove( i );
        channels_selected.put( po.channel, t.getMessage() );
        num_selected++;
      }
    }
    
    //System.out.println( "VSel: num_selected=" + num_selected );
    
    return num_selected;
  }
  

  protected Map getReadySelections() {  return channels_selected;  }
  

  
  private static class PollObject {
    private final SocketChannel channel;
    private final long poll_start_time;
    
    private PollObject( SocketChannel c ) {
      this.channel = c;
      this.poll_start_time = SystemTime.getCurrentTime();
    }
  }
  
  
}
