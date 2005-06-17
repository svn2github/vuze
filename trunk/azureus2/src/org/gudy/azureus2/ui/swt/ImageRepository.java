/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.Constants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * @author Olivier
 *
 */
public class ImageRepository {

  private static Display display;
  private static final HashMap imagesToPath;
  private static final HashMap images;
  private static final HashMap registry;
  private static final String[] noCacheExtList = new String[] {".exe"};
  private static final boolean doNotUseAWTIcon = Constants.isOSX || (Constants.isWindows && !Constants.isWindowsXP);

    static {
    images = new HashMap();
    imagesToPath = new HashMap();
    registry = new HashMap();
  }

  public static void loadImagesForSplashWindow(Display display) {
    ImageRepository.display = display; 
    loadImage(display, "org/gudy/azureus2/ui/icons/a16.png", "azureus");
    loadImage(display, "org/gudy/azureus2/ui/splash/azureus.jpg", "azureus_splash");
  }

  public static void loadImages(Display display) {
    loadImage(display, "org/gudy/azureus2/ui/icons/a32.png", "azureus32");
    loadImage(display, "org/gudy/azureus2/ui/icons/a64.png", "azureus64");
    loadImage(display, "org/gudy/azureus2/ui/icons/a128.png", "azureus128");
    loadImage(display, "org/gudy/azureus2/ui/icons/Azureus_big.png", "tray");
    loadImage(display, "org/gudy/azureus2/ui/icons/dragger.gif", "dragger");
    loadImage(display, "org/gudy/azureus2/ui/icons/folder.gif", "folder");
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
    loadImage(display, "org/gudy/azureus2/ui/icons/greenled.gif", "greenled");
    loadImage(display, "org/gudy/azureus2/ui/icons/redled.gif", "redled");
    loadImage(display, "org/gudy/azureus2/ui/icons/yellowled.gif", "yellowled");
    imagesToPath.put("donation","org/gudy/azureus2/ui/icons/donation.jpg");
    loadImage(display, "org/gudy/azureus2/ui/icons/popup.png","popup");
    loadImage(display, "org/gudy/azureus2/ui/icons/error.gif","error");
    loadImage(display, "org/gudy/azureus2/ui/icons/info.gif","info");
    loadImage(display, "org/gudy/azureus2/ui/icons/warning.gif","warning");
    loadImage(display, "org/gudy/azureus2/ui/icons/subitem.gif","subitem");
    
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

    loadImage(display, "org/gudy/azureus2/ui/icons/status/ok_shared.gif", "st_ok_shared");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/ko_shared.gif", "st_ko_shared");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/stopped_shared.gif", "st_stopped_shared");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_tracker_shared.gif", "st_no_tracker_shared");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/no_remote_shared.gif", "st_no_remote_shared");

    loadImage(display, "org/gudy/azureus2/ui/icons/status/explain.gif", "st_explain");
    loadImage(display, "org/gudy/azureus2/ui/icons/status/shared.gif", "st_shared");

    loadImage(display, "org/gudy/azureus2/ui/icons/statusbar/status_warning.gif", "sb_warning");


    }


  public static Image loadImage(Display display, String res, String name){
    return loadImage(display,res,name,255);
  }

  public static Image loadImage(Display display, String res, String name,int alpha) {
    return loadImage(ImageRepository.class.getClassLoader(),display,res,name,alpha);
  }

  public static Image loadImage(ClassLoader loader,Display display, String res, String name,int alpha) {
    imagesToPath.put(name,res);
    Image im = getImage(name,false);
    if(null == im) {
      InputStream is = loader.getResourceAsStream(res);
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
		
		im = new Image( display, 1, 1 );
		
		images.put(name, im);
      }
    }
    return im;
  }

  public static void unLoadImages() {
    Iterator iter;
    iter = images.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      im.dispose();
    }

    iter = registry.values().iterator();
    while (iter.hasNext()) {
      Image im = (Image) iter.next();
      if(im != null)
        im.dispose();
    }
  }

  public static Image getImage(String name) {
    return getImage(name,true);
  }
  
  private static Image getImage(String name,boolean allowLoading) {
    Image result = (Image) images.get(name);
    if(allowLoading && result == null) {
      String path = (String) imagesToPath.get(name);
      if(path != null) {
        return loadImage(display,path,name);
      }
    }
    return result;
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

  /**
   * @deprecated Does not account for custom or native folder icons
   * @see ImageRepository#getPathIcon(String)
   */
  public static Image
  getFolderImage()
  {
  	return( (Image) images.get("folder"));
  }

    /**
     * <p>Gets a small-sized iconic representation of the file or directory at the path</p>
     * <p>For most platforms, the icon is a 16x16 image; weak-referencing caching is used to avoid abundant reallocation.</p>
     * @param path Absolute path to the file or directory
     * @return The image
     */
    public static Image getPathIcon(final String path)
    {
        try
        {
            final File file = new File(path);

            // workaround for unsupported platforms
            // notes:
            // Mac OS X - Do not mix AWT with SWT (possible workaround: use IPC/Cocoa)
            // Windows < XP - Improper Alpha Channel support

            String key;
            if(file.isDirectory())
            {
                if(doNotUseAWTIcon)
                    return getFolderImage();

                key = file.getPath();
            }
            else
            {
                final int lookIndex = file.getName().lastIndexOf(".");

                if(lookIndex == -1)
                {
                    if(doNotUseAWTIcon)
                        return getFolderImage();

                    key = "?!blank";
                }
                else
                {
                    final String ext =  file.getName().substring(lookIndex);
                    key = ext;

                    if(doNotUseAWTIcon)
                         return getIconFromProgram(Program.findProgram(ext));

                    // case-insensitive file systems
                    for (int i = 0; i < noCacheExtList.length; i++)
                    {
                        if(noCacheExtList[i].equalsIgnoreCase(ext))
                        {
                            key = file.getPath();
                            break;
                        }
                    }
                }
            }

            // this method mostly deals with incoming torrent files, so there's less concern for
            // custom icons (unless user sets a custom icon in a later session)

            // other platforms - try sun.awt
            Image image = (Image)registry.get(key);
            if(image != null)
                return image;

            final Class sfClass = Class.forName("sun.awt.shell.ShellFolder");
            final Object sfInstance = sfClass.getMethod("getShellFolder", new Class[]{File.class}).invoke(null, new Object[]{file});

            final java.awt.Image awtImage = (java.awt.Image)sfClass.getMethod("getIcon", new Class[]{Boolean.TYPE}).invoke(sfInstance, new Object[]{new Boolean(false)});

            final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ImageIO.write((BufferedImage)awtImage, "png", outStream);
            final ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());

            image = new Image(null, inStream);

            // recomposite to avoid artifacts - transparency mask does not work
            final Image dstImage = new Image(Display.getCurrent(), image.getBounds().width, image.getBounds().height);
            GC gc = new GC(dstImage);
            gc.drawImage(image, 0, 0);
            gc.dispose();

            registry.put(key, dstImage);
            image.dispose();

            return dstImage;
        }
        catch (Exception e)
        {
            //Debug.printStackTrace(e);

            // Possible scenario: Method call before file creation
            final int fileSepIndex = path.lastIndexOf(File.separator);
            if(fileSepIndex == path.length() - 1)
            {
                return getFolderImage();
            }
            else
            {
                final int extIndex;
                if(fileSepIndex == -1)
                    extIndex = path.indexOf('.');
                else
                    extIndex = path.substring(fileSepIndex).indexOf('.');

                if(extIndex == -1)
                    return getFolderImage();
                else
                    return getIconFromProgram(Program.findProgram(path.substring(extIndex)));
            }
        }
    }

    /**
     * <p>Gets an image with the specified canvas size</p>
     * <p>No scaling is performed on the original image, and a cached version will be used if found.</p>
     * @param name ImageRepository image resource name
     * @param canvasSize Size of image
     * @return The image
     */
    public static Image getImageWithSize(String name, Point canvasSize)
    {
        String key =
                new StringBuffer()
                    .append(name)
                    .append('.')
                    .append(canvasSize.x)
                    .append('.')
                    .append(canvasSize.y)
                .toString();

        Image newImage = (Image)images.get(key);

        if(newImage == null)
        {
            Image oldImage = getImage(name);

            if(oldImage == null)
                return null;

            newImage = new Image(Display.getCurrent(), canvasSize.x, canvasSize.y);
            GC gc = new GC(newImage);

            int x = Math.max(0, (canvasSize.x - oldImage.getBounds().width)/2);
            int y = Math.max(0, (canvasSize.y - oldImage.getBounds().height)/2);
            gc.drawImage(oldImage, x, y);

            gc.dispose();

            images.put(key, newImage);
        }

        return newImage;
    }
    
    public static void unloadImage(String name) {
      Image img = (Image) images.get(name);
      if(img != null) {
        images.remove(name);
        if(! img.isDisposed())
          img.dispose();        
      }
    }
    
    public static void unloadPathIcon(String path) {
     String key = getKey(path);
     Image img = (Image) registry.get(key);
     if(img != null) {
       registry.remove(key);
       if(! img.isDisposed())
         img.dispose();       
     }
    }
    
    private static String getKey(String path) {
      final File file = new File(path);

      String key;
      if(file.isDirectory())
      {
          key = file.getPath();
      }
      else
      {
          final int lookIndex = file.getName().lastIndexOf(".");
  
          if(lookIndex == -1)
          {
              key = "?!blank";
          }
          else
          {
              final String ext =  file.getName().substring(lookIndex);
              key = ext;
              
              // case-insensitive file systems
              for (int i = 0; i < noCacheExtList.length; i++)
              {
                  if(noCacheExtList[i].equalsIgnoreCase(ext))
                  {
                      key = file.getPath();
                  }
              }
          }
      } 
      
      return key;
    }
    
}
