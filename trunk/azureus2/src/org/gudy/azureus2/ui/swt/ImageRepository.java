/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

/**
 * @author Olivier
 * 
 */
public class ImageRepository {

  private static HashMap images;

  static {
    images = new HashMap();
  }

  public static void loadImagesForSplashWindow(Display display) {
    loadImage(display, "org/gudy/azureus2/ui/icons/azureus.png", "azureus");
    loadImage(display, "org/gudy/azureus2/ui/splash/azureus.jpg", "azureus_splash");
  }
  
  public static void loadImages(Display display) {
    loadImage(display, "org/gudy/azureus2/ui/icons/close.png", "close");
    loadImage(display, "org/gudy/azureus2/ui/icons/tray.png", "tray");    
    loadImage(display, "org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
    loadImage(display, "org/gudy/azureus2/ui/icons/folder.gif", "folder");
    loadImage(display, "org/gudy/azureus2/ui/icons/root.png", "root");
    loadImage(display, "org/gudy/azureus2/ui/icons/dictionary.png", "dict");
    loadImage(display, "org/gudy/azureus2/ui/icons/list.png", "list");
    loadImage(display, "org/gudy/azureus2/ui/icons/integer.png", "int");
    loadImage(display, "org/gudy/azureus2/ui/icons/string.png", "string");
    loadImage(display, "org/gudy/azureus2/ui/icons/data.png", "data");
    loadImage(display, "org/gudy/azureus2/ui/icons/ipfilter.png", "ipfilter");
    loadImage(display, "org/gudy/azureus2/ui/icons/start2.png", "start");
    loadImage(display, "org/gudy/azureus2/ui/icons/stop2.png", "stop");
    loadImage(display, "org/gudy/azureus2/ui/icons/downloadBar.png", "downloadBar");
    loadImage(display, "org/gudy/azureus2/ui/icons/delete.png", "delete");
    loadImage(display, "org/gudy/azureus2/ui/icons/lock.png", "lock");
    
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/open.gif", "cb_open");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/open_no_default.gif", "cb_open_no_default");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/open_folder.gif", "cb_open_folder");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/open_url.gif", "cb_open_url");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/new.gif", "cb_new");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/up.gif", "cb_up");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/down.gif", "cb_down");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/run.gif", "cb_run");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/start.gif", "cb_start");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/stop.gif", "cb_stop");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/remove.gif", "cb_remove");
    loadImage(display, "org/gudy/azureus2/ui/icons/coolbar/host.gif", "cb_host");
    
  }

  
  public static Image loadImage(Display display, String res, String name){
    return loadImage(display,res,name,255);
  }
  
  public static Image loadImage(Display display, String res, String name,int alpha) {
    Image im = getImage(name);
    if(null == im) {
      InputStream is = ImageRepository.class.getClassLoader().getResourceAsStream(res);
      if(null != is) {
        if(alpha == 255) {
          im = new Image(display, is);          
        } else {
          ImageData icone = new ImageData(is);
          icone.alpha = alpha;
          im = new Image(display,icone);
        }
        images.put(name, im);
      } else {
        System.out.println("ImageRepository:loadImage:: Resource not found: " + res);
      }
    }
    return im;
  }

  public static InputStream 
  getImageAsStream(
  	String	name ) 
  {
      return(ImageRepository.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/icons/" + name));
  }
  
  public static void unLoadImages() {
    Iterator iter = images.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      im.dispose();
    }
  }

  public static Image getImage(String name) {
    return (Image) images.get(name);
  }

  /**
     * Gets an image for a file associated with a given program
     *
     * @param program the Program
     */
  public static Image getIconFromProgram(Program program) {
    Image image = (Image) images.get(program);
    if (image == null) {
      if (program != null) {

        ImageData imageData = program.getImageData();
        if (imageData != null) {
          image = new Image(null, imageData,imageData.getTransparencyMask());
          images.put(program, image);
        }
      }
    }
    if (image == null) {
      image = (Image) images.get("folder");
    }
    return image;
  }
}
