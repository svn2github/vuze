/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * TorrentInfoPNGStream.java
 *
 * Created on 27. September 2003, 01:08
 */

package org.gudy.azureus2.ui.web;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.HashMap;

import javax.imageio.ImageIO;

import org.gudy.azureus2.core.DownloadManager;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentInfoPNGStream extends InputStream {
  
  private BufferedImage img = null;
  private byte[] png = null;
  private int readpointer = 0;
  //private PngEncoder pngenc;
  private ByteArrayOutputStream ba;
  
  /** Creates a new instance of TorrentInfoPNGStream */
  public TorrentInfoPNGStream(HashMap URIvars, DownloadManager dm) {
    if (URIvars.containsKey("kind")) {
      if (URIvars.get("kind").toString().equalsIgnoreCase("pieces")) {
        boolean pieces[] = dm.getPiecesStatus();
        if (pieces == null) {
          CreateNotAvailable();
          return;
        }
        img = new BufferedImage(pieces.length, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        for (int i = 0; i < pieces.length; i++) {
          if (pieces[i])
            gr.setColor(new Color(0, 128, 255));
          else
            gr.setColor(Color.WHITE);
          gr.fillRect(i, 0, 1, 1);
        }
        RenderImage();
      } else if (URIvars.get("kind").toString().equalsIgnoreCase("availability")) {
        int pieces[];
        try {
          pieces = dm.peerManager.getAvailability();
        } catch (Exception e) {pieces = null;}
        if (pieces == null) {
          CreateNotAvailable();
          return;
        }
        img = new BufferedImage(pieces.length, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = img.createGraphics();
        float max = pieces[0];
        for (int i = 0; i < pieces.length; i++) {
          if (pieces[i]>max)
            max = pieces[i];
        }
        for (int i = 0; i < pieces.length; i++) {
          gr.setColor(new Color(1.0f-(pieces[i]/max), 1.0f-(pieces[i]/(2*max)), 1.0f));
          gr.fillRect(i, 0, 1, 1);
        }
        RenderImage();
      } 
    } else
      CreateNotAvailable();
  }
  
  private void CreateNotAvailable() {
    img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
    Graphics2D gr = img.createGraphics();
    //img.setRGB(0, 0, Color.RED.getRGB());
    gr.setColor(Color.RED);
    gr.fillRect(0, 0, 2, 2);
    RenderImage();
  }
  
  private void RenderImage() {
    img.flush();
    ba = new ByteArrayOutputStream();
    try {
      //PngEncoder.encode(img, ba);
      if (!ImageIO.write(img, "png", ba))
        throw new java.io.IOException("Unknown Format");
      png = ba.toByteArray();
    } catch (Exception e) {e.printStackTrace();}
  }
  
  public int read() throws java.io.IOException {
    if ((img != null) && (png != null)) {
      if (available() > 0)
        return png[readpointer++]&0xff;
      else
        return -1;
    } else
      throw new java.io.IOException("Creation of TorrentInfoPNGStream failed.");
  }
  
  public int available() throws java.io.IOException {
    return png.length - readpointer;
  }
  
}
