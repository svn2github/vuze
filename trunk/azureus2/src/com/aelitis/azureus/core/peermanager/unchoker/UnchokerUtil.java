/*
 * Created on Apr 5, 2005
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

package com.aelitis.azureus.core.peermanager.unchoker;

import java.util.*;

import org.gudy.azureus2.core3.peer.PEPeer;

/**
 * Utility collection for unchokers.
 */
public class UnchokerUtil {
  
  private static final int FREE_BYTES = 128*1024;  // 128KB free optimistic unchoke bytes
  
  
  /**
   * Test whether or not the given peer is allowed to be unchoked.
   * @param peer to test
   * @param allow_snubbed if true, ignore snubbed state
   * @return true if peer is allowed to be unchoked, false if not
   */
  public static boolean isUnchokable( PEPeer peer, boolean allow_snubbed ) {
    return peer.getPeerState() == PEPeer.TRANSFERING && !peer.isSeed() && peer.isInterestedInMe() && ( !peer.isSnubbed() || allow_snubbed );
  }
  

  /**
   * Update (if necessary) the given list with the given value while maintaining a largest-value-first (as seen so far) sort order.
   * @param new_value to use
   * @param values existing values array
   * @param new_item to insert
   * @param items existing items
   * @param start_pos index at which to start compare
   */
  public static void updateLargestValueFirstSort( long new_value, long[] values, PEPeer new_item, List items, int start_pos ) {  
    for( int i=start_pos; i < values.length; i++ ) {
      if( new_value >= values[ i ] ) {
        for( int j = values.length - 2; j >= i; j-- ) {  //shift displaced values to the right
          values[j + 1] = values[ j ];
        }
        
        values[ i ] = new_value;
        items.add( i, new_item );
        
        if( items.size() > values.length ) {  //throw away last item if list too large 
          items.remove( values.length );
        }
        
        return;
      }
    }
  }

  
  /**
   * Choose the next peer, optimistically, that should be unchoked.
   * @param all_peers list of peer to choose from
   * @param factor_reciprocated if true, factor in how much (if any) this peer has reciprocated when choosing
   * @param allow_snubbed allow the picking of snubbed-state peers as last resort
   * @return the next peer to optimistically unchoke, or null if there are no peers available
   */
  public static PEPeer getNextOptimisticPeer( ArrayList all_peers, boolean factor_reciprocated, boolean allow_snubbed ) {
    //find all potential optimistic peers
    ArrayList optimistics = new ArrayList();
    for( int i=0; i < all_peers.size(); i++ ) {
      PEPeer peer = (PEPeer)all_peers.get( i );
      
      if( isUnchokable( peer, false ) && peer.isChokedByMe() ) {
        optimistics.add( peer );
      }
    }
    
    if( optimistics.isEmpty() && allow_snubbed ) {  //try again, allowing snubbed peers as last resort
      for( int i=0; i < all_peers.size(); i++ ) {
        PEPeer peer = (PEPeer)all_peers.get( i );
        
        if( isUnchokable( peer, true ) && peer.isChokedByMe() ) {
          optimistics.add( peer );
        }
      }
    }

    if( optimistics.isEmpty() )  return null;  //no unchokable peers avail
    

    //factor in peer reciprocation ratio when picking optimistic peers
    if( factor_reciprocated ) {
      ArrayList ratioed_peers = new ArrayList();
      long[] ratios = new long[ optimistics.size() ];
        
      //order by upload ratio
      for( int i=0; i < optimistics.size(); i++ ) {
        PEPeer peer = (PEPeer)optimistics.get( i );

        float ratio = 1F;  // >1 means we've uploaded more, <1 means we've downloaded more, so =1 means yet undetermined
        long uploaded = peer.getStats().getTotalDataBytesSent();
        
        if( uploaded > FREE_BYTES ) {
          //if we've uploaded more than freely allowed, use their upload-to-download ratio,
          //otherwise we haven't yet uploaded enough to penalize them
          ratio = (float)uploaded / (peer.getStats().getTotalDataBytesReceived() + 1);
        }

        UnchokerUtil.updateLargestValueFirstSort( (long)(ratio * 1000), ratios, peer, ratioed_peers, 0 );  //higher value = worse ratio
      }
      
      //pick out a random subset of the optimistic peers, penalizing those peers who have have reciprocated the least
      optimistics.clear();
      
      Random rand = new Random();
      LinkedList randomized = new LinkedList();
      
      int num_to_pick = 1;
      int round_reset_pos = 2;
      
      for( int i=0; i < ratioed_peers.size(); ) {
        //randomly insert peer into temp list
        int insert_pos = rand.nextInt( randomized.size() + 1 );
        randomized.add( insert_pos, ratioed_peers.get( i ) );
        
        i++;
        
        if( i == round_reset_pos || i == ratioed_peers.size() ) {  //time to make our picks for this round
          for( int x=0; x < num_to_pick; x++ ) {
            if( randomized.isEmpty() )  break;  //might be less peers than num_to_pick at the very end
            optimistics.add( randomized.removeFirst() );
          }

          randomized.clear();
          num_to_pick = num_to_pick * 2;   
          round_reset_pos = round_reset_pos + num_to_pick * 2;
        }
      }
    }
    

    int rand_pos = new Random().nextInt( optimistics.size() );
    PEPeer peer = (PEPeer)optimistics.get( rand_pos );

    return peer;
    
    //TODO:
    //in downloading mode, we would be better off optimistically unchoking just peers we are interested in ourselves,
    //as they could potentially reciprocate. however, new peers have no pieces to share, and are not interesting to
    //us, and would never be unchoked, and thus would never get any data.
    //we could 1) use 2+ opt unchokes, one for interesting and one for just interested, and 2) use a deterministic 
    //method for new peers to get their very first piece from us
  }

}
