/*
 * Created on 15.11.2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.web2.stages.cache;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author tobi
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class Mime {
  //Filename extension -> MIME type
  private static Hashtable mimeTbl = new Hashtable();
  private static final String defaultMimeType = "text/plain";
  static {
    mimeTbl.put(".html", "text/html");
    mimeTbl.put(".gif", "image/gif");
    mimeTbl.put(".jpg", "image/jpeg");
    mimeTbl.put(".jpeg", "image/jpeg");
    mimeTbl.put(".png", "image/png");
    mimeTbl.put(".ico", "image/ico");
    mimeTbl.put(".css", "text/css");
    mimeTbl.put(".xml", "text/xml");
    mimeTbl.put(".ps", "application/postscript");
    mimeTbl.put(".eps", "application/postscript");
    mimeTbl.put(".pdf", "application/pdf");
  }
  public static String getMimeType(String url) {
    Enumeration e = mimeTbl.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (url.endsWith(key))
        return (String) mimeTbl.get(key);
    }
    return defaultMimeType;
  }

}
