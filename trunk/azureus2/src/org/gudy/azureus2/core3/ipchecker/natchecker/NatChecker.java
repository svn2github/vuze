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

import org.gudy.azureus2.core3.util.BDecoder;

/**
 * @author Olivier
 * 
 */
public class NatChecker {

  public static final int NAT_OK = 1;
  public static final int NAT_KO = 2;
  public static final int NAT_UNABLE = 3;

  public static int test(int port) {
    String check = "azureus_rand_" + (int) (Math.random() * 100000);
    NatCheckerServer server = new NatCheckerServer(port, check);
    if (server.isValid()) {
      try {
        server.start();
        String urlStr = "http://www.gudy.org/azureus/checkNat.php?port=" + port + "&check=" + check;
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        InputStream is = null;
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        is = con.getInputStream();
        //      int length = con.getContentLength();
        //      System.out.println(length);
        byte[] data = new byte[1024];
        int nbRead = 0;
        while (nbRead >= 0) {
          nbRead = is.read(data);
          if (nbRead >= 0)
            message.write(data, 0, nbRead);
          Thread.sleep(20);
        }
        Map map = BDecoder.decode(message.toByteArray());
        int result = ((Long)map.get("result")).intValue();
        switch(result) {
          case 0 :
            return NAT_KO;
          case 1 :
            return NAT_OK;
          default:
            return NAT_UNABLE;            
        }
      }
      catch (Exception e) {       
        return NAT_UNABLE;
      }
      finally {
        server.stopIt();
      }
    }
    else {
      return NAT_UNABLE;
    }
  }
  
  public static void main(String args[]) {
    if(args.length < 1)
      return;
    int n = Integer.parseInt(args[0]);
    System.out.println(test(n));
  }
}
