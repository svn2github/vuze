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

import com.aelitis.azureus.core.dht.vivaldi.maths.Coordinates;

public class HeightCoordinatesImpl implements Coordinates {
  
  protected float x,y,h;
  
  public HeightCoordinatesImpl(float x, float y, float h) {
    this.x = x;
    this.y = y;
    this.h = h;
  }
  
  public HeightCoordinatesImpl(HeightCoordinatesImpl copy) {
    this.x = copy.x;
    this.y = copy.y;
    this.h = copy.h;
  }
  
  public Coordinates add(Coordinates other) {
    HeightCoordinatesImpl o = (HeightCoordinatesImpl) other;
    return new HeightCoordinatesImpl(x+o.x,y+o.y,h+o.h);
  }
  
  public Coordinates sub(Coordinates other) {
    HeightCoordinatesImpl o = (HeightCoordinatesImpl) other;
    return new HeightCoordinatesImpl(x-o.x,y-o.y,h+o.h);
  }
  
  public Coordinates scale(float scale) {
    return new HeightCoordinatesImpl(scale * x,scale * y ,scale * h);
  }

  public float measure() {
    return (float) (Math.sqrt(x * x + y * y) + h);
  }
  
  public float distance(Coordinates other) {
    return this.sub(other).measure();
  }
  
  public Coordinates unity() {
    float measure = this.measure();
    if(measure == 0) return new HeightCoordinatesImpl(0,0,0);
    return this.scale(1/measure);
  }
}
