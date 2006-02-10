/*
 * Created on 01.12.2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * 
 */
package org.gudy.azureus2.ui.swt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * URL Transfer type for Drag and Drop of URLs
 * Windows IDs are already functional.
 * 
 * Please use Win32TransferTypes to determine the IDs for other OSes!
 * 
 * @see org.gudy.azureus2.ui.swt.test.Win32TransferTypes
 * @author Rene Leonhardt
 * 
 * @author TuxPaper (require incoming string types have an URL prefix)
 * @author TuxPaper (UTF-8, UTF-16, BOM stuff)
 * 
 * @TODO check FileTransfer.getInstance().isSupportedType() and 
 *        .nativeToJava(..).  Open the file and get the text, check if it's a
 *        real URL.
 */

public class URLTransfer extends ByteArrayTransfer {


	/** We are in the process of checking a string to see if it's a valid URL */
	private boolean bCheckingString = false;
  
  private static URLTransfer _instance = new URLTransfer();

  // these types work on Windows XP: IE6 link=49367/49362, IE6 bookmark=13,
	// Mozilla link (text)=13, Mozilla bookmark=13
  // Opera 7 LINK DRAG & DROP IMPOSSIBLE (just inside Opera)
  private static final String[] supportedTypes = new String[] {
			"UniformResourceLocatorW", 
			"UniformResourceLocator",// IE Dragged Link from page
			"UniformResourceLocator",// IE Dragged Link from URL Bar
			"UniformResourceLocator", 
			"CF_UNICODETEXT", 
			"CF_TEXT",
			"OEM_TEXT",
			};

	private static final int[] supportedTypeIds = new int[] { 
		49476, 
		49318,
		49367, 
		49362, 
		13, 
		1, 
		17 
		}; 
	// 15="CF_HDROP" (File), 
	// 49368="UniformResourceLocator" (File+IE bookmark), 
	// 49458="UniformResourceLocatorW" (IE bookmark)

  public static URLTransfer getInstance() {
    return _instance;
  }
  public void javaToNative(Object object, TransferData transferData) {
    if (object == null || !(object instanceof URLType[]))
      return;

    if (isSupportedType(transferData)) {
      URLType[] myTypes = (URLType[]) object;
      try {
        // write data to a byte array and then ask super to convert to pMedium
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream writeOut = new DataOutputStream(out);
        for (int i = 0, length = myTypes.length; i < length; i++) {
          writeOut.writeBytes(myTypes[i].linkURL);
          writeOut.writeBytes("\n");
          writeOut.writeBytes(myTypes[i].linkText);
        }
        byte[] buffer = out.toByteArray();
        writeOut.close();

        super.javaToNative(buffer, transferData);

      } catch (IOException e) {}
    }
  }
  public Object nativeToJava(TransferData transferData) {

    if (bCheckingString || isSupportedType(transferData)) {
      byte[] buffer = (byte[]) super.nativeToJava(transferData);
      if (buffer == null)
        return null;

      URLType myData = null;
      try {
      	String data;
      	if (buffer.length > 1) {
      		boolean bFirst0 = buffer[0] == 0;
      		boolean bSecond0 = buffer[1] == 0;
  				if (bFirst0 && bSecond0)
  					// This is probably UTF-32 Big Endian.  
  					// Let's hope default constructor can handle it (It can't)
  					data = new String(buffer);
  				else if (bFirst0)
  					data = new String(buffer, "UTF-16BE");
  				else if (bSecond0)
  					data = new String(buffer, "UTF-16LE");
  				else if (buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB
  						&& buffer.length > 3 && buffer[2] == (byte) 0xBF)
  					data = new String(buffer, 3, buffer.length - 3, "UTF-8");
  				else if (buffer[0] == (byte) 0xFF || buffer[0] == (byte) 0xFE)
  					data = new String(buffer, "UTF-16");
  				else {
  					data = new String(buffer);
  				}
      	} else {
      		// Older Code:
					// Remove 0 values from byte array, messing up any Unicode strings 
					byte[] text = new byte[buffer.length];
					int j = 0;
					for (int i = 0; i < buffer.length; i++) {
						if (buffer[i] != 0)
							text[j++] = buffer[i];
					}

	        data = new String(text, 0, j);
				}

        if (data == null)
        	return null;

        String[] split = data.split("[\r\n]+", 2);

        myData = new URLType();
       	myData.linkURL = (split.length > 0) ? split[0] : "";
     		myData.linkText = (split.length > 1) ? split[1] : "";
      } catch (Exception ex) {
      	ex.printStackTrace();
        return null;
      }
      return myData;
    }

    return null;
  }
  protected String[] getTypeNames() {
    return supportedTypes;
  }
  protected int[] getTypeIds() {
    return supportedTypeIds;
  }
  /**
	 * @param transferData
	 * @see org.eclipse.swt.dnd.Transfer#isSupportedType(org.eclipse.swt.dnd.TransferData)
	 * @return
	 */
  public boolean isSupportedType(TransferData transferData) {
  	if (bCheckingString)
  		return true;

  	if (transferData != null) {
			for (int i = 0; i < supportedTypeIds.length; i++) {
				if (transferData.type == supportedTypeIds[i]) {
					if (transferData.type == 13 || transferData.type == 1) {
						// TODO: Check if it's a string list of URLs
						
						// String -- Check if URL, skip to next if not
						URLType url = null;
						
						// nativeToJava will call isSupportedType, so we need to prevent
						// infinite recursion
						bCheckingString = true;
						try {
							url = (URLType)nativeToJava(transferData);
						} finally {
							bCheckingString = false;
						}
						
						if (url == null) {
							continue;
						}
						
						if (!isURL(url.linkURL)) {
							continue;
						}
					}
					return true;
				}
			}
		}
//  	System.out.println("Drop type not supported: " + transferData.type);
    return false;
  }

	private boolean isURL(String sURL) {
		String sLower = sURL.toLowerCase();
		return sLower.startsWith("http://") || sLower.startsWith("https://")
				|| sLower.startsWith("magnet:") || sLower.startsWith("ftp://");
	}
	
	/**
	 * Sometimes, CF_Text will be in currentDataType even though CF_UNICODETEXT 
	 * is present.  This is a workaround until its fixed properly.
	 * <p>
	 * Place it in <code>dropAccept</code>
	 * 
	 * <pre>
	 *if (event.data instanceof URLTransfer.URLType)
	 *	event.currentDataType = URLTransfer.pickBestType(event.dataTypes, event.currentDataType);
	 * </pre>
	 * 
	 * @param dataTypes
	 * @param def
	 * @return
	 */
	public static TransferData pickBestType(TransferData[] dataTypes, TransferData def) {
		for (int i = 0; i < supportedTypeIds.length; i++) {
			int supportedTypeID = supportedTypeIds[i];
			for (int j = 0; j < dataTypes.length; j++) {
				TransferData data = dataTypes[j];
				if (supportedTypeID == data.type)
					return data;
			}
		}
		return def;
	}

  public class URLType {
    public String linkURL;
    public String linkText;
    public String toString() {
      return linkURL + "\n" + linkText;
    }
  }

  /**
   * Test for varioud UTF Strings
   * BOM information from http://www.unicode.org/faq/utf_bom.html
   * @param args
   */
  public static void main(String[] args) {
  	
  	Map map = new LinkedHashMap();
  	map.put("UTF-8", 
  			new byte[] { (byte)0xEF, (byte)0xbb, (byte)0xbf, 'H', 'i' });
  	map.put("UTF-32 BE BOM", 
  			new byte[] { 0, 0, (byte)0xFE, (byte)0xFF, 'H', 0,0,0, 'i', 0,0,0 });
    map.put("UTF-16 LE BOM", 
    		new byte[] { (byte)0xFF, (byte)0xFE, 'H', 0, 'i', 0 });
    map.put("UTF-16 BE BOM", 
    		new byte[] { (byte) 0xFE, (byte) 0xFF, 0, 'H', 0, 'i' });
    map.put("UTF-16 LE", 
    		new byte[] { 'H', 0, 'i', 0 });
    map.put("UTF-16 BE", 
    		new byte[] { 0, 'H', 0, 'i' });
    
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String element = (String) iterator.next();
			System.out.println(element + ":");
			byte[] buffer = (byte[]) map.get(element);

			boolean bFirst0 = buffer[0] == 0;
			boolean bSecond0 = buffer[1] == 0;
			String data = "";
			try {
				if (bFirst0 && bSecond0)
					// This is probably UTF-32 Big Endian.  
					// Let's hope default constructor can handle it (It can't)
					data = new String(buffer);
				else if (bFirst0)
					data = new String(buffer, "UTF-16BE");
				else if (bSecond0)
					data = new String(buffer, "UTF-16LE");
				else if (buffer[0] == (byte) 0xEF && buffer[1] == (byte) 0xBB
						&& buffer.length > 3 && buffer[2] == (byte) 0xBF)
					data = new String(buffer, 3, buffer.length - 3, "UTF-8");
				else if (buffer[0] == (byte) 0xFF || buffer[0] == (byte) 0xFE)
					data = new String(buffer, "UTF-16");
				else {
					data = new String(buffer);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println(data);
		}
	}
}
