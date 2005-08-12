/*
 * Created on 22 juin 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.core.dht.vivaldi.maths.impl;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.vivaldi.maths.Coordinates;
import com.aelitis.azureus.core.dht.vivaldi.maths.VivaldiPosition;

/**
 * 
 * Vivaldi Papers :
 * http://www.sigcomm.org/sigcomm2004/papers/p426-dabek111111.pdf
 * 
 */

public class VivaldiPositionImpl implements VivaldiPosition{
  
  private static final float cc = 0.25f;
  private static final float ce = 0.5f;
  private static final float initial_error	= 10f;
  
  private HeightCoordinatesImpl coordinates;
  private float error;
  private int nbUpdates;
  
  public VivaldiPositionImpl(HeightCoordinatesImpl coordinates) {
    this.coordinates = coordinates;
    error = initial_error;
  }
  
  public Coordinates getCoordinates() {
    return coordinates;
  }
  
  public float getErrorEstimate() {
   return error;
  }
  
  public void setErrorEstimate(float error) {
    this.error = error;
   }
  
  public void 
  update(float rtt,Coordinates cj,float ej) 
  {
	  if ( valid(rtt) && valid(ej) && cj.isValid()){
		  
		 // System.out.println( "accepted vivaldi update:" + rtt + "/" + cj + "/" + ej );

	    //Insure we have valid data in input
	    if(rtt < 0) return;
	    if(error + ej == 0) return;
	    
	    //Sample weight balances local and remote error. (1)
	    float w = error / (ej + error);
	    
	    //Real error
	    float re = rtt - coordinates.distance(cj);
	    
	    //Compute relative error of this sample. (2)
	    float es = Math.abs(re) / rtt;
	    
	    //Update weighted moving average of local error. (3)
	    
	    float new_error = es * ce * w + error * (1 - ce * w);
	    
	    	//Update local coordinates. (4)
	    
	    float delta = cc * w;
	    
	    float scale = delta * re;
	    
      HeightCoordinatesImpl random_error = new HeightCoordinatesImpl((float)Math.random()/10,(float)Math.random()/10,0f);
      
	    HeightCoordinatesImpl new_coordinates = (HeightCoordinatesImpl)coordinates.add(coordinates.sub(cj.add(random_error)).unity().scale(scale));
	    
	    if ( valid( new_error ) && new_coordinates.isValid()){
	    	
	    	coordinates = new_coordinates;
	    	
	    	error		= new_error > ERROR_MIN ? new_error : ERROR_MIN;
	    	
	    }else{
	    	
	    	Debug.out( "VivaldiPosition: resetting as invalid: " + 
	    				coordinates + "/" + error + " + " + rtt + "," + cj + "," + ej + "->" + new_coordinates + "/" + new_error );
	    	
	    	coordinates = new HeightCoordinatesImpl(0,0,0);
	    	error		= initial_error;
	    }
      
      if(! cj.atOrigin()) {
        nbUpdates++;
      }
      if(nbUpdates > CONVERGE_EVERY) {
        nbUpdates = 0;
        update(10,new HeightCoordinatesImpl(0,0,0),CONVERGE_FACTOR);
      }
      
	  }else{
		 // System.out.println( "rejected vivaldi update:" + rtt + "/" + cj + "/" + ej );
	  }
  }
  
  private boolean
  valid(
	float	f )
  {
	  return( !(Float.isInfinite( f ) || Float.isNaN( f )));
  }
  
  public void update(float rtt, float[] data ){
	  
	  update( rtt, new HeightCoordinatesImpl( data[0], data[1], data[2] ), data[3] );
  }
  
  public float estimateRTT(Coordinates coordinates) {
    return this.coordinates.distance(coordinates);
  }
  
  public float[] toFloatArray(){
	  return( new float[]{ coordinates.getX(), coordinates.getY(), coordinates.getH(), error });
  }
  
  public void fromFloatArray( float[] data ){
	  
	  coordinates = new HeightCoordinatesImpl( data[0], data[1], data[2] );
	  
	  error			= data[3];
  }
  
  public String toString() {
    return coordinates +  " : " + error;
  }
  
  public boolean equals(Object arg0) {
   if(arg0 instanceof VivaldiPositionImpl) {
     VivaldiPositionImpl other = (VivaldiPositionImpl) arg0;
     if(other.error != error) return false;
     if(! other.coordinates.equals(coordinates)) return false;
     return true;
   }
   return false;
  }
  
}
