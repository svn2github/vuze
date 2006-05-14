/*
 * Created on Mar 15, 2006
 * Created by Alon Rohter
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.peermanager.unchoker;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;



/**
 * 
 */
public class UnchokeSlot {
	
	public static final int UNCHOKE_TYPE_NORMAL     = 0;
	public static final int UNCHOKE_TYPE_OPTIMISTIC = 1;
	
	
	
	private int unchoke_type;
	private PEPeerTransport attached_peer;
	
	
	protected UnchokeSlot( int _unchoke_type ) {
		this.unchoke_type = _unchoke_type;
	}
	
	
	public void setUnchokeType( int _unchoke_type ) {  unchoke_type = _unchoke_type;  }
	
	public int getUnchokeType() {  return unchoke_type;  }
	
	
	protected void attachPeer( PEPeerTransport peer ) {		
		attached_peer = peer;
		UnchokerUtil.performChokeUnchoke( null, peer );
	}
	
	
	protected PEPeerTransport detachPeer() {
		UnchokerUtil.performChokeUnchoke( attached_peer, null );
		PEPeerTransport peer = attached_peer;
		attached_peer = null;
		return peer;
	}
	
	
	public boolean isAttached() {  return attached_peer != null;  }
	
}
