/*
 * File    : Utils.java
 * Created : 25 sept. 2003 16:15:07
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
 
package org.gudy.azureus2.ui.swt;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * @author Olivier
 * 
 */
public class Utils {
  
  public static void disposeComposite(Composite composite) {
    if(composite == null || composite.isDisposed())
        return;
    Control[] controls = composite.getChildren();
    for(int i = 0 ; i < controls.length ; i++) {
      Control control = controls[i];                
      if(control != null && ! control.isDisposed()) {
        if(control instanceof Composite) {
          disposeComposite((Composite) control);
        }
        control.dispose();
      }
    }    
    composite.dispose();
  }
  
  public static void changeBackgroundComposite(Composite composite,Color color) {
    if(1==1)
      return;  
    if(composite == null || composite.isDisposed())
          return;
      Control[] controls = composite.getChildren();
      for(int i = 0 ; i < controls.length ; i++) {
        Control control = controls[i];                
        if(control != null && ! control.isDisposed()) {
          if(control instanceof Composite) {
            changeBackgroundComposite((Composite) control,color);
          }
          control.setBackground(color);
        }
      }    
      composite.setBackground(color);
    }

  /**
   * Initializes the URL dialog with http://
   * If a valid link is found in the clipboard, it will be inserted
   * and the size (and location) of the dialog is adjusted.
   * @param shell to set the dialog location if needed
   * @param gridData to adjust the dialog with
   * @param url the URL text control
   *
   * @author Rene Leonhardt
   */
  public static void setTextLinkFromClipboard(final Shell shell, final GridData gridData, final Text url) {
    String link = getLinkFromClipboard(shell.getDisplay());
    if(link.length() > 7) {
      GC gc = new GC(url);
      FontMetrics fm = gc.getFontMetrics();
      int width = (link.length() + 10) * fm.getAverageCharWidth();
      if (width > shell.getDisplay().getBounds().width) {
        gridData.widthHint = shell.getDisplay().getBounds().width - 20;
        shell.setLocation(0, 0);
      } else {
        gridData.widthHint = width;
      }
    }
    url.setText(link);
  }

  /**
   * @param shell
   * @return first valid link from clipboard, else "http://" 
   *
   * @author Rene Leonhardt
   */
  public static String getLinkFromClipboard(final Display display) {
    String link = "http://";
    Clipboard cb = new Clipboard(display);
    TextTransfer transfer = TextTransfer.getInstance();
    String data = (String) cb.getContents(transfer);
    if (data != null) {
      int begin = data.indexOf("http://");
      if (begin >= 0) {
        int end = data.indexOf("\n", begin + 7);
        String stringURL = end >= 0 ? data.substring(begin, end - 1) : data.substring(begin);
        try {
          URL parsedURL = new URL(stringURL);
          link = parsedURL.toExternalForm();
        } catch (MalformedURLException e1) {
        }
      }
    }
    return link;
  }

  /**
   * Saves the width from the given column in the configuration.
   * The data property "configName" must be set to determine the full table column name.
   * @param t the table column from which to save the width 
   *
   * @author Rene Leonhardt
   */
  public static void saveTableColumn(TableColumn t) {
    if(t != null && t.getData("configName") != null) {
      COConfigurationManager.setParameter((String) t.getData("configName") + ".width", t.getWidth());
      COConfigurationManager.save();
    }
  }

  /**
   * Saves the width from the given column in the configuration if the user has allowed it.
   * The data property "configName" must be set to determine the full table column name.
   * @param t the table column from which to save the width 
   *
   * @author Rene Leonhardt
   */
  public static void saveTableColumnIfAllowed(TableColumn t) {
    if(COConfigurationManager.getBooleanParameter("Save detail views column widths", false))
      saveTableColumn(t);
  }

}
