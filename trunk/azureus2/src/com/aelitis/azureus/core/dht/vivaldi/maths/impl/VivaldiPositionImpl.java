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

/*
 * import org.gudy.azureus2.core3.util.Debug;
 */

import com.aelitis.azureus.core.dht.vivaldi.maths.Coordinates;
import com.aelitis.azureus.core.dht.vivaldi.maths.VivaldiPosition;

/**
 * Vivaldi Papers :
 * http://www.sigcomm.org/sigcomm2004/papers/p426-dabek111111.pdf
 */

public class VivaldiPositionImpl implements VivaldiPosition {
  
  private static final float initial_error = -1.0f;

  private HeightCoordinatesImpl coordinates;
  private float error;
  
  private int nbUpdates;

  public VivaldiPositionImpl(HeightCoordinatesImpl coordinates) {
//    this.coordinates = coordinates;
    this.coordinates = new HeightCoordinatesImpl(0.0f, 0.0f, 10.0f);
    error = initial_error;
    //nbUpdates = (int) (Math.random() * CONVERGE_EVERY);
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

  public synchronized void update(float rtt, Coordinates cj, float ej) {
    if (!(valid(rtt) && (rtt >= 0.0f) && valid(ej) && cj.isValid())) {
      // log error
      return;
    }
    
    if ((error < 0.0f) || (ej >= 0.0f)) {
      // get rtt estimate, error weight distribution, and absolute sample error
      float rtt_estimate = coordinates.distance(cj);
      float error_weight = getErrorWeight(ej);
      float sample_error = (float) (Math.abs(rtt - rtt_estimate) / rtt);
      // now update coordinates and error
      updateCoords(cj, ej, error_weight, rtt - rtt_estimate, rtt);
      updateError(ej, error_weight, sample_error);
    }
    
    if(rtt > 0) {
      nbUpdates++;
      if(nbUpdates >= CONVERGE_EVERY) {
        nbUpdates = 0;
        update(0,new HeightCoordinatesImpl(0,0,0),ERROR_MIN*CONVERGE_FACTOR);
      }
    }
  }

  private void updateCoords(Coordinates _cj, float ej, float error_weight,
      float sample_error, float rtt) {
    HeightCoordinatesImpl cj = (HeightCoordinatesImpl) _cj;

    if (error < 0.0f) {
      // this is our first ping, try to position exactly rtt ms away
      float remaining_rtt = rtt - cj.h;
      if (remaining_rtt <= 5.0f) {
        // fix if his height vector exceeds rtt between us
        remaining_rtt = 5.0f;
      }
      // pick a new height
      float new_height = (float) Math.random() * remaining_rtt;
      remaining_rtt -= new_height;
      // pick some random unit vector
      float[] planar = getUnitVector();
      // now scale by rtt not used by height
      planar[0] *= remaining_rtt; planar[1] *= remaining_rtt;
      // set coordinates exactly rtt ms away from this peer
      coordinates = new HeightCoordinatesImpl(
          cj.x + planar[0], cj.y + planar[1], new_height);
    }
    else {
      float delta = COORD_CONTROL * error_weight;
        Coordinates scaled = coordinates.sub(cj).unity().scale(delta * sample_error);
      coordinates = (HeightCoordinatesImpl) coordinates.add(scaled);
    }
  }

  private void updateError(float ej, float error_weight, float sample_error) {
    if (error < 0.0f) {
      // this is our first ping, initial error should be at least ERROR_MIN
      error = ERROR_MIN + (1.0f - ERROR_MIN)
          * ((sample_error > 1.0f) ? 1.0f : sample_error);
    }
    else if (ej >= 0.0f) {
      // not our first ping, use moving average of error with established node
      float new_err = sample_error * error_weight + error
          * (1.0f - error_weight);
      error = new_err * ERROR_CONTROL + error * (1.0f - ERROR_CONTROL);
      if (error > 1.0f) {
        // error can't exceed 1.0f
        error = 1.0f;
      }
    }
  }
  
  private float[] getUnitVector() {
    float[] unit = {(float) Math.random(), (float) Math.random()};
    float norm = (float) Math.sqrt(unit[0] * unit[0] + unit[1] * unit[1]);
    return new float[] {unit[0] / norm, unit[1] / norm};
  }

  private float getErrorWeight(float ej) {
    float loc_sq = error * error;
    float rmt_sq = ej * ej;
    return (loc_sq / (loc_sq + rmt_sq));
  }

  private boolean valid(float f) {
    return (!(Float.isInfinite(f) || Float.isNaN(f)));
  }

  public void update(float rtt, float[] data) {
    update(rtt, new HeightCoordinatesImpl(data[0], data[1], data[2]),
        data[3]);
  }

  public float estimateRTT(Coordinates coordinates) {
    return this.coordinates.distance(coordinates);
  }

  public float[] toFloatArray() {
    return (new float[] { coordinates.getX(), coordinates.getY(),
        coordinates.getH(), error });
  }

  public void fromFloatArray(float[] data) {
    coordinates = new HeightCoordinatesImpl(data[0], data[1], data[2]);
    error = data[3];
  }

  public String toString() {
    return coordinates + " : " + error;
  }
}
