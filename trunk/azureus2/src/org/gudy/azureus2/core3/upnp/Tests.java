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

import java.util.*;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.device.*;
import org.cybergarage.upnp.ssdp.*;

/**
 * @author Olivier
 * 
 */
public class 
Tests 
{

	public static void 
	main(String args[]) 
	{   
    	final ControlPoint ctrlPoint = new ControlPoint();
    	
    	ctrlPoint.start();
    
    	SearchResponseListener listener = 
    		new SearchResponseListener() 
    		{
				public void 
				deviceSearchResponseReceived(SSDPPacket packet) 
				{			
    				System.out.println(packet.getUSN() + " - " + packet.getST() + " - " + packet.getLocation());
    
    				processDevices( "", ctrlPoint.getDeviceList());
				}
    		};

    	ctrlPoint.addSearchResponseListener(listener);

    	ctrlPoint.search("upnp:rootdevice");   
    	

  
    }
   
   // ctrlPoint.stop();
    //ctrlPoint.finalize();
    
    protected static void
    processDevices(
   		String		indent,
   		DeviceList	devices )
   	{				  	
		for(int i =0 ; i < devices.size() ; i++){
		
			Device dev = devices.getDevice(i);
			
			String devType = dev.getDeviceType();
				     	
			System.out.println(indent+"device type:" + devType );
				     	
			ServiceList services = dev.getServiceList();
			
			for( int j=0 ; j < services.size() ; j++) {
			
				Service service = services.getService(j);
				
				String serviceType = service.getServiceType();
				
				System.out.println(indent+" - Found service : "+ serviceType );
				      		
				List action_list = service.getActionList();
				      		
				for (int k=0;k<action_list.size();k++){
				      		
					Action	action = (Action)action_list.get(k);
				      			
				    System.out.println( indent + "    - action = " + action.getName());
				    
				   	if ( action.getName().equals( "AddPortMapping" )){
				   		
				        action.setArgumentValue("NewRemoteHost","0");
				        action.setArgumentValue("NewExternalPort","7007");
				        action.setArgumentValue("NewProtocol","TCP");
				        action.setArgumentValue("NewInternalPort","7007");
				        action.setArgumentValue("NewInternalClient","192.168.0.2");
				        action.setArgumentValue("NewEnabled","1");
				        action.setArgumentValue("NewPortMappingDescription","Azureus Port Mapping");
				        action.setArgumentValue("NewLeaseDuration","0");
				        System.out.println(action.postControlAction());
				   	}
				}
			}
				      		      		
			DeviceList	sub_devs = dev.getDeviceList();

			processDevices( indent + "    ", sub_devs );				      		

/*
				      		
				      		if(serviceType.equals("WANIPConnection:1") || serviceType.equals("WANPPPConnection:1")) {
				        		System.out.println("Found a WANIP or WANPPP Connection");

						}
*/
		}
	}
}
