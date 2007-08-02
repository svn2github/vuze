/*
 * Created on Jun 29, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.browser.listener.publish;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.image.FileFormat;
import org.eclipse.swt.widgets.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.txn.Transaction;
import com.aelitis.azureus.ui.swt.utils.ImageResizeException;
import com.aelitis.azureus.ui.swt.utils.ImageResizer;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.util.MapUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;


/**
 * Tracks the progress of creating a torrent during the publish process.
 * 
 * @author dharkness
 * @created Jul 20, 2006
 */
public class PublishTransaction extends Transaction
{
	
	
	private static final String ELEMENTS = "elements";

	public static final String PUBLISH_ATTRIBUTE_KEY = "DIRECTOR PUBLISH";  //TODO this should really be placed in DirectorPlugin.java
	
	private static final int   DEFAULT_IMAGE_BOX_SIZE = 320;
	private static final float DEFAULT_JPEG_QUALITY = 0.85f;
	
	private Shell shell;
	
	private LocalHoster hoster;
	
	private TorrentCreator creator;
	private TorrentCreatorListener creatorListener;
	private File dataFile;
	
	public void setShell(Shell shell) {
		this.shell = shell;
	}
	
	public void setLocalHoster(LocalHoster hoster) {
		this.hoster = hoster;
	}

	/**
     * Creates the transaction. Called by the manager.
     * 
     * @param id unique ID assigned by the manager
     * @param type passed by manager
     * @param context used to access the browser
     */
    public PublishTransaction ( int id , String type , ClientMessageContext context ) {
        super(id, type, context);
    }


    /**
     * Opens a file dialog so the user can choose the file to torrent.
     */
    public void chooseFile ( BrowserMessage message ) {    	
    	FileDialog dialog = new FileDialog(shell);    	
    	String file = dialog.open();    	
    	createTorrentFile(file);
    }

    /**
     * Opens a file dialog so the user can choose the folder to torrent.
     */
    public void chooseFolder ( BrowserMessage message ) {
    	DirectoryDialog dialog = new DirectoryDialog(shell);    	
    	String file = dialog.open();
    	createTorrentFile(file);
    }
    
    protected boolean canceling ( ) {
    	if(creator != null) {
    		creator.cancel();
    		creator.removeListener(creatorListener);
    		creator = null;
    		//Gudy	sendBrowserMessage("torrent","canceled");
    	}
        return true;
    }
    
    /**
     * Opens a file dialog so the user can choose the image to use as a thumbnail
     */
    public void chooseThumbnail(BrowserMessage message) {
    	final int resize_size[] = {DEFAULT_IMAGE_BOX_SIZE,DEFAULT_IMAGE_BOX_SIZE};
    	final float image_quality[] = {DEFAULT_JPEG_QUALITY};
    	Map elements = null; //will be used if several thumbnails are required on a single page
        if ( message.isParamObject() ) {
            Map parameters = message.getDecodedMap();
    		try {
    			resize_size[0] = MapUtils.getMapInt(parameters, "width", 
    					DEFAULT_IMAGE_BOX_SIZE);
    			resize_size[1] = MapUtils.getMapInt(parameters, "height", 
    					DEFAULT_IMAGE_BOX_SIZE);
    			image_quality[0] = ((Number) MapUtils.getMapObject(parameters,
						"quality", new Double(DEFAULT_JPEG_QUALITY), Number.class)).floatValue();
    			if (parameters.containsKey(ELEMENTS)){
    				elements = (Map) parameters.get(ELEMENTS);
    			}
    		} catch(Exception e) {
    			//Possible bad parameters given, use default values
    			e.printStackTrace();
    		}
        }
    	FileDialog dialog = new FileDialog(shell,SWT.OPEN);
    	dialog.setFilterNames(new String[] {"Image Files"});
    	dialog.setFilterExtensions(new String[] {"*.jpg;*.jpeg;*.bmp;*.gif;*.png"});
    	final String fileName = dialog.open();
    	if(fileName != null) {
    		//Run async not to block the UI
			/*Thread runner = 
                new Thread("Thumbnail Creator") {
    				public void run() {*/
    		
    					try {
                            sendBrowserMessage("thumb", "start", elements);
                            
        					File file = new File(fileName);    				
    	    				ResizedImageInfo info = loadAndResizeImage(file,resize_size[0],resize_size[1],1);
    	    				if(info == null) {
    	    					debug("User canceled image resizing");
    	    					sendBrowserMessage("thumb", "clear", elements);
    	    				} else {
	    	    				final String thumbURL = info.url.toString();
	    	    				debug("Size : " + info.data.length);
	    	    				
	    	    				final String encoded = new String(Base64.encode(info.data));
	                            Map params = new HashMap();
	                            params.put("url", thumbURL);
	                            params.put("width", new Long(info.width));
	                            params.put("height", new Long(info.height));
	                            params.put("data", encoded);
	                            if ( elements != null ){
	                            	params.put(ELEMENTS, elements);
	                            }
	                            sendBrowserMessage("thumb", "done", params);
    	    				}
        	    		}
    					catch(ImageResizeException e) {
    						debug("Error resizing image",e);
    						sendBrowserMessage("thumb", "clear", elements);
                            
                            Map params = new HashMap();
                            params.put("message", e.getMessage());
                            sendBrowserMessage("page", "error",params);
    					}
                        catch (Exception e) {
        					debug("Error reading file",e);
                            sendBrowserMessage("thumb", "clear", elements);
                            
                            Map params = new HashMap();
                            params.put("message", "Azureus cannot process this image. Please select another one.");
                            sendBrowserMessage("page", "error",params);
        				}
                        catch (OutOfMemoryError e) {
                        	debug("Error processing the image",e);
                        	
                        	sendBrowserMessage("thumb", "clear", elements);
                            
                            Map params = new HashMap();
                            params.put("message", "Azureus cannot process this image (likely reason is that it is too big). Please select another one.");
                            sendBrowserMessage("page", "error",params);
                        	
                        }
    				/*}
			    };
			runner.setDaemon(true);
			runner.start();    	*/		
    	}
    }

 	/**
	 * Pulls the modified torrent from the result web page and saves it locally.
	 */
	public void torrentIsReady(BrowserMessage message) {
		String torrent = MapUtils.getMapString(message.getDecodedMap(),
				"torrent", null);
		if (torrent != null) {
			torrentIsReady(torrent);
		}
	}

    protected void torrentIsReady(String strTorrent) {

		try {
			strTorrent = strTorrent.replaceAll("\\n", "");
			debug("data file path = [" + dataFile.getPath() + "]");
			debug("Torrent is ready, size = " + strTorrent.length()
					+ ", content (base64) : " + strTorrent);

			byte[] torrent_data = Base64.decode(strTorrent);

			debug("Torrent Byte Length: " + torrent_data.length /* + ", content : " + new String(torrent_data) */);

			// use PluginInterface since it has nice functions for
			// setting complete and adding a download via Torrent
			PluginInterface pi = PluginInitializer.getDefaultInterface();
			Torrent torrent = pi.getTorrentManager().createFromBEncodedData(
					torrent_data);

			torrent.setDefaultEncoding();
			torrent.setComplete(dataFile);

			final Download download = pi.getDownloadManager().addDownload(torrent,
					null, dataFile);

			PublishUtils.setPublished(download);

			download.setForceStart(true);

			//Transaction is finished
			stop();
		} catch (Throwable t) {
			Debug.out("torrentIsReady", t);
		}
	} 


    private void torrentCreationFailed(Exception cause) {
    	debug("Torrent Creation Failed", cause);
    	
    	sendBrowserMessage("torrent","failed");
    	
    	Map params = new HashMap();
		params.put("message", "Azureus cannot process this file. Please select another file.");
		sendBrowserMessage("page", "error",params);
		
    }
    
    private void createTorrentFile(String file) {
    	if(file != null) {   		
    		dataFile = new File(file);
    		try {
    			PluginInterface pi = PluginInitializer.getDefaultInterface();
    			creator = pi.getTorrentManager().createFromDataFileEx(dataFile,
						new URL("http://xxxxxxxxxxxxxxxxx:6969/announce"), false);

    			creatorListener = new TorrentCreatorListener() {

    				public void complete(Torrent torrent) {    					
    					try {
    						
    						torrent.setDefaultEncoding();
    						
    						debug("local torrent creation complete: " +torrent.getName()+ " : " +torrent.getMagnetURI() );        						    						
    						
    						final String tData = new String( Base64.encode( torrent.writeToBEncodedData() ) );
    						Map params = new HashMap();
                            params.put("data", tData);
    						sendBrowserMessage("torrent", "done", params);
    					}
    					catch (Throwable t) {
    						// TODO: handle exception
    						debug("error encoding torrent", t);
    					}

    				}

    				public void failed(TorrentException cause) {
    					torrentCreationFailed(cause);
    				}

    				public void reportActivity(String activity) {
    					//debug("creation status : " + activity);
    				}

    				public void reportPercentageDone(int percent) {
    					//debug("creation progress : " + percent);
    					Map params = new HashMap();
    					params.put("percent", new Long(percent));
    					sendBrowserMessage("torrent", "progress", params);
    				}

    			};

    			creator.addListener(creatorListener);

    			creator.start();

    		} catch (MalformedURLException e) {
    			
    			torrentCreationFailed(e);
				
    		} catch (TorrentException e) {
    			
    			torrentCreationFailed(e);
				
    		}
            
            Map params = new HashMap();
            params.put("folder", new Boolean(dataFile.isDirectory()));
            params.put("name", dataFile.getName());
            long size = getSize(dataFile);
            params.put("size", new Long(size));
            params.put("size-text", DisplayFormatters.formatByteCountToKiBEtc(size));
            sendBrowserMessage("torrent", "chosen", params);
    	} else {
    		//No file was chosen, cancel the transaction
    		cancel();
            stop();
    	}
    }
    
    private long getSize(File folderFile) {
    	if (folderFile.isFile()) {
    		return folderFile.length();
    	}
    	if (folderFile.isDirectory()) {
    		long size = 0;
    		File[] files = folderFile.listFiles();
    		if (files != null) {
    			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						size += getSize(file);
					}
    		}
    		return size;
    	}
    	return 0;
    }
    
    private class ResizedImageInfo {
    	public URL url;
    	public int width,height;
    	public byte[] data;
    	
    	public ResizedImageInfo(URL url,int width, int height, byte[] data) {
    		this.url = url;
    		this.width = width;
    		this.height = height;
    		this.data = data;
    	}
    }
    
    
	private ResizedImageInfo loadAndResizeImage(final File f, final int width,
			final int height, float quality) throws Exception {
		ImageLoader loader = new ImageLoader();
		final Display display = shell.getDisplay();
		Image source = null;
		try {
			source = new Image(shell.getDisplay(), f.getAbsolutePath());
		} catch (Error e) {
		}
		
		if (source == null) {
			throw new ImageResizeException("Unable to read image.  Please choose another.");
		}

		// If size is already an exact match, and the file isn't too big, use
		// original file
		Rectangle bounds = source.getBounds();
		try {
			if (bounds.width == width && bounds.height == height
					&& f.length() < 60000 && false) {
				URL url = hoster.hostFile(f);
				final FileInputStream fos = new FileInputStream(f);
				byte[] buf = new byte[(int) f.length()];
				fos.read(buf);
				fos.close();

				ResizedImageInfo result = new ResizedImageInfo(url, width, height, buf);
				return result;
			}
		} catch (Exception e) {
			Debug.out(e);
		}

		ImageResizer resizer = new ImageResizer(display, width, height, shell);

		Image output = resizer.resize(source);
		if (output == null)
			return null;
		ImageData data = output.getImageData();
		//Dispose the image
		if (output != null && !output.isDisposed()) {
			output.dispose();
		}

		//debug("final ByteArrayOutputStream baos = new ByteArrayOutputStream();");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		loader.data = new ImageData[] { data
		};
		
		String ext;
		if (SWT.getVersion() >= 3500) {
			// XXX Bug in SWT which borks some PNGs.. thus we can't use PNG saving
			//     at all until they fix it.. See 
			//     https://bugs.eclipse.org/bugs/show_bug.cgi?id=172290
			loader.save(baos, SWT.IMAGE_PNG);
			ext = ".png";
		} else {
			try {
				Class cJPGFF = Class.forName("org.eclipse.swt.internal.image.JPEGFileFormat");

				Constructor jpgConst = cJPGFF.getDeclaredConstructor(new Class[0]);

				jpgConst.setAccessible(true);

				FileFormat format = (FileFormat) jpgConst.newInstance(new Object[0]);

				Field field = cJPGFF.getDeclaredField("encoderQFactor");

				field.setAccessible(true);

				field.setInt(format, (int) (quality * 100));

				Class claLEDataOS = Class.forName("org.eclipse.swt.internal.image.LEDataOutputStream");

				Constructor le_constructor = claLEDataOS.getDeclaredConstructor(new Class[] { OutputStream.class
				});

				le_constructor.setAccessible(true);

				Object le_stream = le_constructor.newInstance(new Object[] { baos
				});

				Method unloadIntoStream = cJPGFF.getMethod("unloadIntoStream",
						new Class[] {
							ImageLoader.class,
							claLEDataOS
						});

				try {
					unloadIntoStream.invoke(format, new Object[] {
						loader,
						le_stream
					});
				} catch (Exception ex) {
					//Too bad for us here
					//However we don't want to try the other way, as it may be an io 
					//exception, ie the stream is corrupted...
					ex.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
				// The reflection way failed, do it the normal way with default 
				// (0.75) quality...
				loader.save(baos, SWT.IMAGE_JPEG);
			}
			ext = ".jpg";
		}


		byte[] bs = baos.toByteArray();

		File fDest = File.createTempFile("thumbnail", ext);
		FileOutputStream fos = new FileOutputStream(fDest);
		fos.write(bs);
		fos.close();

		URL url = hoster.hostFile(fDest);
		ResizedImageInfo result = new ResizedImageInfo(url, width, height, bs);

		return result;
	}

    /*
    private ResizedImageInfo loadAndResizeImage(File f,int thumbnail_size,float quality) throws Exception {
    	
    	debug("BufferedImage src = ImageIO.read(f);");
    	BufferedImage src = ImageIO.read(f);                      
        
        BufferedImage image;
        
        int width = src.getWidth(),height = src.getHeight();
        
        float wRatio = (float)width / (float)thumbnail_size;
        float hRatio = (float)height / (float)thumbnail_size;
        
        
        
        boolean mustResize = false;
        
        if(wRatio >= hRatio && wRatio > 1) {
        	mustResize = true;
        	width = thumbnail_size;
        	height = (int) (src.getHeight() / wRatio);
        }
        if(hRatio > wRatio && hRatio > 1) {
        	mustResize = true;
        	width = (int) (src.getWidth() / hRatio);
        	height = thumbnail_size;
        	
        }
        
        if(mustResize) {
        	debug("mustResize");
        	
        	debug("Image img = src.getScaledInstance(width,height,Image.SCALE_FAST);");
        	Image img = src.getScaledInstance(width,height,Image.SCALE_FAST);
        	
        	img.
        	
        	debug("BufferedImage copy = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);");
        	BufferedImage copy = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        	
        	debug("Graphics2D g2 = copy.createGraphics();");
        	Graphics2D g2 = copy.createGraphics();
        	
        	debug("g2.setColor(Color.white);");
        	g2.setColor(Color.white);
        	
        	debug("g2.fillRect(0,0,width,height);");
        	g2.fillRect(0,0,width,height);
        	
        	debug("g2.drawImage(img, 0, 0, null);");
        	g2.drawImage(img, 0, 0, new ImageObserver() {
        		public boolean imageUpdate(Image arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
        			debug("image update");
        			return true;
        		}
        	});
        	
        	debug("g2.dispose();");
        	g2.dispose();
        	
        	src = copy;
        }
        
        
        debug("File fDest = File.createTempFile(\"thumbnail\",\".jpg\");");
        File fDest = File.createTempFile("thumbnail",".jpg");
        
        debug("final FileOutputStream fos = new FileOutputStream(fDest);");
		final FileOutputStream fos = new FileOutputStream(fDest);
		
		debug("final ByteArrayOutputStream baos = new ByteArrayOutputStream();");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		
		debug("OutputStream os = new OutputStream() {...};");
		OutputStream os = new OutputStream() {
			public void write(int arg0) throws java.io.IOException {fos.write(arg0);baos.write(arg0);};
			public void flush() throws java.io.IOException {fos.flush();baos.flush();};
			public void write(byte[] arg0) throws java.io.IOException {fos.write(arg0);baos.write(arg0);};
			public void write(byte[] arg0, int arg1, int arg2) throws java.io.IOException {fos.write(arg0);baos.write(arg0);};			
		};
		
		ImageOutputStream output = null;
		
		try {
			//Get Writer and set compression
			debug("Iterator iter = ImageIO.getImageWritersByFormatName(\"JPG\");");
			Iterator iter = ImageIO.getImageWritersByFormatName("JPG");
			
			if (iter.hasNext()) {
				
				debug("ImageWriter writer = (ImageWriter)iter.next();");
				ImageWriter writer = (ImageWriter)iter.next();
				
				debug("ImageWriteParam iwp = writer.getDefaultWriteParam();");
				ImageWriteParam iwp = writer.getDefaultWriteParam();
				
				debug("ImageWriteParam iwp = writer.getDefaultWriteParam();");
				
				debug("iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);");
				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);		        
		        
				debug("iwp.setCompressionQuality(quality);");
		        iwp.setCompressionQuality(quality);
		        
		        debug("ImageOutputStream output = new MemoryCacheImageOutputStream(os);");
		        output = new MemoryCacheImageOutputStream(os);
		        
		        debug("writer.setOutput(output);");
		        writer.setOutput(output);
		        
		        debug("IIOImage imageOut =  new IIOImage(src, null, null);");
		        IIOImage imageOut =  new IIOImage(src, null, null);
		        
		        debug("writer.write(null, imageOut, iwp);");
		        writer.write(null, imageOut, iwp);
		    }
        }catch (Exception e) {
        	e.printStackTrace();
		} finally {
			output.close();			
		}
		
		URL url = hoster.hostFile(fDest);
		ResizedImageInfo result = new ResizedImageInfo(url,width,height,baos.toByteArray());
		return result;
        
        
    }
    */
}