/*
 * File    : Tests.java
 * Created : 2 mars 2004
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.core3.upnp;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;

/**
 * @author Olivier
 * 
 */
public class Tests {

  public static void main(String args[]) {   
    ControlPoint ctrlPoint = new ControlPoint();
    ctrlPoint.start();
    DeviceList rootDevList = ctrlPoint.getDeviceList();
    int nRootDevs = rootDevList.size();
    System.out.println(nRootDevs);
    for(int i =0 ; i < nRootDevs ; i++) {
     Device dev = rootDevList.getDevice(i);
     String devName = dev.getFriendlyName();
     System.out.println("[" + i + "] = " + devName );     
    }
    ctrlPoint.stop();
    ctrlPoint.finalize();
  }
}
