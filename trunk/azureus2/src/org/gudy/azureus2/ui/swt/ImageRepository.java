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
    //loadImage(display, "org/gudy/azureus2/ui/icons/tray.png", "tray");
    loadImage(display, "org/gudy/azureus2/ui/icons/Azureus_big.png", "tray");
    loadImage(display, "org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
    loadImage(display, "org/gudy/azureus2/ui/icons/folder.gif", "folder");
    loadImage(display, "org/gudy/azureus2/ui/icons/root.png", "root");
    loadImage(display, "org/gudy/azureus2/ui/icons/dictionary.png", "dict");
    loadImage(display, "org/gudy/azureus2/ui/icons/list.png", "list");
    loadImage(display, "org/gudy/azureus2/ui/icons/integer.png", "int");
    loadImage(display, "org/gudy/azureus2/ui/icons/string.png", "string");
    loadImage(display, "org/gudy/azureus2/ui/icons/data.png", "data");
    loadImage(display, "org/gudy/azureus2/ui/icons/ipfilter.png", "ipfilter");
    loadImage(display, "org/gudy/azureus2/ui/icons/start.gif", "start");
    loadImage(display, "org/gudy/azureus2/ui/icons/stop.gif", "stop");
    loadImage(display, "org/gudy/azureus2/ui/icons/bar.gif", "downloadBar");
    loadImage(display, "org/gudy/azureus2/ui/icons/delete.gif", "delete");
    loadImage(display, "org/gudy/azureus2/ui/icons/lock.gif", "lock");
    loadImage(display, "org/gudy/azureus2/ui/icons/host.gif", "host");
    loadImage(display, "org/gudy/azureus2/ui/icons/publish.gif", "publish");
    loadImage(display, "org/gudy/azureus2/ui/icons/run.gif", "run");
    loadImage(display, "org/gudy/azureus2/ui/icons/details.gif", "details");
    loadImage(display, "org/gudy/azureus2/ui/icons/up.gif", "up");    
    loadImage(display, "org/gudy/azureus2/ui/icons/down.gif", "down");
    loadImage(display, "org/gudy/azureus2/ui/icons/top.gif", "top");
    loadImage(display, "org/gudy/azureus2/ui/icons/bottom.gif", "bottom");
    loadImage(display, "org/gudy/azureus2/ui/icons/recheck.gif", "recheck");
    loadImage(display, "org/gudy/azureus2/ui/icons/export.gif", "export");
    loadImage(display, "org/gudy/azureus2/ui/icons/move.gif", "move");
    loadImage(display, "org/gudy/azureus2/ui/icons/add_tracker.gif", "add_tracker");
    loadImage(display, "org/gudy/azureus2/ui/icons/edit_trackers.gif", "edit_trackers");
    loadImage(display, "org/gudy/azureus2/ui/icons/columns.gif", "columns");
    loadImage(display, "org/gudy/azureus2/ui/icons/speed.gif", "speed");
    loadImage(display, "org/gudy/azureus2/ui/icons/openFolder16x12.gif", "openFolderButton");
    loadImage(display, "org/gudy/azureus2/ui/icons/forcestart.gif", "forcestart");
    
    //ToolBar Icons
    
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/open.gif", "cb_open");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/open_no_default.gif", "cb_open_no_default");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/open_folder.gif", "cb_open_folder");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/open_url.gif", "cb_open_url");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/new.gif", "cb_new");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/up.gif", "cb_up");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/down.gif", "cb_down");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/top.gif", "cb_top");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/bottom.gif", "cb_bottom");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/run.gif", "cb_run");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/start.gif", "cb_start");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/stop.gif", "cb_stop");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/remove.gif", "cb_remove");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/host.gif", "cb_host");
    loadImage(display, "org/gudy/azureus2/ui/icons/toolbar/publish.gif", "cb_publish");
        
    //Status icons    
    loadImage(display, "org/gudy/azureus2/ui/icons/status/ok.gif", "st_ok");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/ko.gif", "st_ko");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/stopped.gif", "st_stopped");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_tracker.gif", "st_no_tracker");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_remote.gif", "st_no_remote");
    
    loadImage(display, "org/gudy/azureus2/ui/icons/status/ok_selected.gif", "st_ok_selected");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/ko_selected.gif", "st_ko_selected");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/stopped_selected.gif", "st_stopped_selected");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_tracker_selected.gif", "st_no_tracker_selected");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_remote_selected.gif", "st_no_remote_selected");
    
    loadImage(display, "org/gudy/azureus2/ui/icons/status/explain.gif", "st_explain");
    
    loadImage(display, "org/gudy/azureus2/ui/icons/donation.jpg","donation");
    loadImage(display, "org/gudy/azureus2/ui/icons/popup.png","popup");
    loadImage(display, "org/gudy/azureus2/ui/icons/error.gif","error");
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
    Image image = null;
    
    try{
    	image =(Image) images.get(program);
    
	    if (image == null) {
	      if (program != null) {
	
	        ImageData imageData = program.getImageData();
	        if (imageData != null) {
	          image = new Image(null, imageData,imageData.getTransparencyMask());
	          images.put(program, image);
	        }
	      }
	    }
    }catch( Throwable e ){
    	// seen exceptions thrown here, due to images.get failing in Program.hashCode
    	// ignore and use default icon
    }
    
    if (image == null) {
      image = (Image) images.get("folder");
    }
    return image;
  }
}
