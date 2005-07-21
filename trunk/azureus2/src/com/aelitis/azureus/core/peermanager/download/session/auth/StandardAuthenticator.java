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

package com.aelitis.azureus.core.peermanager.download.session.auth;

import java.util.*;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.download.session.*;



public class StandardAuthenticator implements TorrentSessionAuthenticator {
  private final byte[] infohash;
  
  protected StandardAuthenticator( byte[] infohash ) {
    this.infohash = infohash;
  }

  public String getSessionTypeID() {  return TorrentSessionAuthenticator.AUTH_TYPE_STANDARD;  }
  
  
  public byte[] getSessionInfoHash() {
    return infohash;
  }
  
  public Map createSessionSyn() {
    return null;  //no explicit syn info required
  }
  

  public Map verifySessionSyn( Map syn_info ) throws AuthenticatorException {
    return null;  //no explicit ack info required
  }

  
  public void verifySessionAck( Map ack_info ) throws AuthenticatorException {
    //do nothing, always accept ack
  }

  public DirectByteBuffer decodeSessionData( DirectByteBuffer encoded_data ) throws AuthenticatorException {
    return encoded_data;  //keep as is
  }
  

  public DirectByteBuffer encodeSessionData( DirectByteBuffer decoded_data ) throws AuthenticatorException {
    return decoded_data;  //keep as is
  }
}
