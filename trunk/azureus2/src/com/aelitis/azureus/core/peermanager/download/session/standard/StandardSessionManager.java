/*
 * Created on Jul 10, 2005
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

package com.aelitis.azureus.core.peermanager.download.session.standard;

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.download.session.*;

public class StandardSessionManager implements TorrentSessionHandler {
  public static final String SESSION_TYPE_ID = "STANDARD";

  private static final StandardSessionManager instance = new StandardSessionManager();
  
  private final HashMap hashes = new HashMap();
  private final AEMonitor hashes_mon = new AEMonitor( "StandardSessionManager" );
  
  
  private StandardSessionManager() {
    TorrentSessionManager.getSingleton().registerIncomingSessionListener( SESSION_TYPE_ID, new TorrentSessionListener(){
      public void torrentSessionRequested( final TorrentSession incoming, Map syn_info ) {
        boolean found = false;
        try{ hashes_mon.enter();
          found = hashes.containsKey( new HashWrapper( incoming.getInfoHash() ) );
        }
        finally{ hashes_mon.exit();  }
        
        if( found ) {
          //send back ack
          incoming.ackSession( null, StandardSessionManager.this );
        }
        else {  //not found
          System.out.println( "unknown session infohash " +ByteFormatter.nicePrint( incoming.getInfoHash(), true ));
          //send failure
          incoming.endSession( "unknown session infohash" );
        }
      }
      
      public void registerSessionInfoHash( byte[] infohash ) {
        try{ hashes_mon.enter();
          hashes.put( new HashWrapper( infohash ), null );
        }
        finally{ hashes_mon.exit();  }
      }
      
      public void deregisterSessionInfoHash( byte[] infohash ) {
        try{ hashes_mon.enter();
          hashes.remove( new HashWrapper( infohash ) );
        }
        finally{ hashes_mon.exit();  }
      }
    });
  }
  
  
  public static StandardSessionManager getSingleton() {  return instance;  }
  
  
  
  //TorrentSessionHandler implementation
  
  public boolean sessionAcked( TorrentSession session, Map info ) {
    return true;  //always accept, as there's no need to verify any specific session info
  }

  
  public void sessionEnded( TorrentSession session, String reason ){
    System.out.println( "session [" +ByteFormatter.nicePrint( session.getInfoHash(), true )+ "] ended: " +reason );
  }
  
}
