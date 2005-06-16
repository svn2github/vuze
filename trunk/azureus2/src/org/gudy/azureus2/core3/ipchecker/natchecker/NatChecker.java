/*
 * File    : NatChecker.java
 * Created : 12 oct. 2003 18:46:00
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

package org.gudy.azureus2.core3.ipchecker.natchecker;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.*;

import com.aelitis.azureus.plugins.upnp.*;

/**
 * @author Olivier
 * 
 */
public class NatChecker {

  public static final int NAT_OK = 1;
  public static final int NAT_KO = 2;
  public static final int NAT_UNABLE = 3;
  
  private static final String[] urls = {
      Constants.AELITIS_WEB_SITE + "natcheck.php"       
  };

  public static int 
  test(
  		AzureusCore	azureus_core,
		int port) 
  {	
    String check = "azureus_rand_" + String.valueOf( (int)(Math.random() * 100000) );
    
    NatCheckerServer server = new NatCheckerServer( port, check );
    
    if( !server.isValid() )  return NAT_UNABLE;
    
    if( port < 0 || port > 65535 || port == 6880 )  return NAT_UNABLE;
    

    //do UPnP if necessary
    PluginInterface pi_upnp = azureus_core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

    UPnPMapping new_mapping = null;

    if( pi_upnp != null ) {

      UPnPPlugin upnp = (UPnPPlugin)pi_upnp.getPlugin();

      UPnPMapping mapping = upnp.getMapping( true, port );

      if( mapping == null ) {

        new_mapping = upnp.addMapping( "NAT Tester", true, port, true );

        // give UPnP a chance to work

        try {
          Thread.sleep( 500 );
          
        }
        catch (Throwable e) {

          Debug.printStackTrace( e );
        }
      }
    }

    //run check
    try {
      server.start();
      
      String urlStr = urls[ 0 ] + "?port=" + String.valueOf( port ) + "&check=" + check;
      URL url = new URL( urlStr );
      HttpURLConnection con = (HttpURLConnection)url.openConnection();
      con.connect();
      
      ByteArrayOutputStream message = new ByteArrayOutputStream();
      InputStream is = con.getInputStream();
      
      byte[] data = new byte[ 1024 ];
      
      int nbRead = 0;
      while( nbRead >= 0 ) {
        nbRead = is.read( data );
        if( nbRead >= 0 ) message.write( data, 0, nbRead );
        Thread.sleep( 20 );
      }
      
      Map map = BDecoder.decode( message.toByteArray() );
      int result = ((Long)map.get( "result" )).intValue();
      
      switch( result ) {
        case 0 :
          byte[] reason = (byte[])map.get( "reason" );
          if( reason != null ) {
            LGLogger.log( "NAT CHECK FAILED: " + new String( reason ) );
          }
          return NAT_KO;
        case 1 :
          return NAT_OK;
        default :
          return NAT_UNABLE;
      }
    }
    catch (Exception e) {
      return NAT_UNABLE;
    }
    finally {

      if( new_mapping != null ) {

        new_mapping.destroy();
      }

      server.stopIt();
    }
  }
  
}
