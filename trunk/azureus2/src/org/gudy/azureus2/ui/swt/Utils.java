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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

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
    url.setText("http://");
    Clipboard cb = new Clipboard(shell.getDisplay());
    TextTransfer transfer = TextTransfer.getInstance();
    String data = (String) cb.getContents(transfer);
    if (data != null) {
      int begin = data.indexOf("http://");
      if (begin >= 0) {
        int end = data.indexOf("\n", begin + 7);
        String stringURL = end >= 0 ? data.substring(begin, end - 1) : data.substring(begin);
        try {
          URL parsedURL = new URL(stringURL);
          url.setText(parsedURL.toExternalForm());
          GC gc = new GC(url);
          FontMetrics fm = gc.getFontMetrics();
          int width = (url.getText().length() + 10) * fm.getAverageCharWidth();
          if (width > shell.getDisplay().getBounds().width) {
            gridData.widthHint = shell.getDisplay().getBounds().width - 20;
            shell.setLocation(0, 0);
          } else {
            gridData.widthHint = width;
          }
        } catch (MalformedURLException e1) {
        }
      }
    }
  }

}
