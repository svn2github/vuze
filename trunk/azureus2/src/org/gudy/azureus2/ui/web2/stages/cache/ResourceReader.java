/*
 * Created on 14.11.2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.web2.stages.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.WebConst;
import org.gudy.azureus2.ui.web2.http.request.httpRequest;
import org.gudy.azureus2.ui.web2.http.response.httpNotFoundResponse;
import org.gudy.azureus2.ui.web2.http.response.httpOKResponse;
import org.gudy.azureus2.ui.web2.http.response.httpResponder;
import org.gudy.azureus2.ui.web2.http.response.httpResponse;

import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.EventHandlerException;
import seda.sandStorm.api.EventHandlerIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.core.BufferElement;

/**
 * @author tobi
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ResourceReader implements EventHandlerIF, WebConst {

  private static final Logger logger = Logger.getLogger("azureus2.ui.web.stages.ResourceReader");

  /*
   * (non-Javadoc)
   * 
   * @see seda.sandStorm.api.EventHandlerIF#handleEvent(seda.sandStorm.api.QueueElementIF)
   */
  public void handleEvent(QueueElementIF item) throws EventHandlerException {
    if (logger.isDebugEnabled())
      logger.debug("GOT QEL: " + item);

    if (item instanceof httpRequest) {
      httpRequest req = (httpRequest) item;
      httpResponse resp;
      String fileres = "org/gudy/azureus2/ui/web/template/" + req.getURL();
      fileres = fileres.replaceAll("//", "/");
      if (ClassLoader.getSystemResource(fileres) != null) {
        byte[] buf = new byte[1024];
        InputStream res = ClassLoader.getSystemResourceAsStream(fileres);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
          try {
            int i = res.read(buf);
            if (i > 0)
              out.write(buf, 0, i);
            else
              break;
          } catch (IOException e) {
            break;
          }
        }
        resp = new httpOKResponse(Mime.getMimeType(req.getURL()), new BufferElement(out.toByteArray()));
        req.getSink().enqueue_lossy(new httpResponder(resp, req, true));
        return;
      } else {
        resp = new httpNotFoundResponse(req, req.getURL() + " not found.");
        logger.info("Could not open resource " + fileres);
        UI.numErrors++;
      }
      req.getSink().enqueue_lossy(new httpResponder(resp, req, true));
      return;
    } else {
      logger.info("ResourceReader: Got unknown event type: " + item);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see seda.sandStorm.api.EventHandlerIF#handleEvents(seda.sandStorm.api.QueueElementIF[])
   */
  public void handleEvents(QueueElementIF[] items) throws EventHandlerException {
    for (int i = 0; i < items.length; i++) {
      handleEvent(items[i]);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see seda.sandStorm.api.EventHandlerIF#init(seda.sandStorm.api.ConfigDataIF)
   */
  public void init(ConfigDataIF arg0) throws Exception {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see seda.sandStorm.api.EventHandlerIF#destroy()
   */
  public void destroy() throws Exception {
    // TODO Auto-generated method stub

  }

}
