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

  public static void loadImages(Display display) {
    loadImage(display, "org/gudy/azureus2/ui/icons/close.png", "close");
    loadImage(display, "org/gudy/azureus2/ui/icons/tray.png", "tray");
    loadImage(display, "org/gudy/azureus2/ui/icons/azureus.png", "azureus");
    loadImage(display, "org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
    loadImage(display, "org/gudy/azureus2/ui/icons/folder.gif", "folder");
    loadImage(display, "org/gudy/azureus2/ui/icons/root.png", "root");
    loadImage(display, "org/gudy/azureus2/ui/icons/dictionary.png", "dict");
    loadImage(display, "org/gudy/azureus2/ui/icons/list.png", "list");
    loadImage(display, "org/gudy/azureus2/ui/icons/integer.png", "int");
    loadImage(display, "org/gudy/azureus2/ui/icons/string.png", "string");
    loadImage(display, "org/gudy/azureus2/ui/icons/data.png", "data");
    loadImage(display, "org/gudy/azureus2/ui/icons/ipfilter.png", "ipfilter");
  }

  public static Image loadImage(Display display, String res, String name) {
    Image im = getImage(name);
    if(null == im) {
      InputStream is = ClassLoader.getSystemResourceAsStream(res);
      if(null != is) {
        im = new Image(display, is);
        images.put(name, im);
      } else {
        System.out.println("ImageRepository:loadImage:: Resource not found: " + res);
      }
    }
    return im;
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
