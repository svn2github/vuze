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

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Olivier
 * 
 */
public class Utils {
  
  public static void disposeComposite(Composite composite,boolean disposeSelf) {
    if(composite == null || composite.isDisposed())
      return;
  Control[] controls = composite.getChildren();
  for(int i = 0 ; i < controls.length ; i++) {
    Control control = controls[i];                
    if(control != null && ! control.isDisposed()) {
      if(control instanceof Composite) {
        disposeComposite((Composite) control,true);
      }
      try {
        control.dispose();
      } catch (SWTException e) {
        Debug.printStackTrace( e );
      }
    }
  }
  // It's possible that the composite was destroyed by the child
  if (!composite.isDisposed() && disposeSelf)
    try {
      composite.dispose();
    } catch (SWTException e) {
      Debug.printStackTrace( e );
    }
  }
  
  public static void disposeComposite(Composite composite) {
    disposeComposite(composite,true);
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
  public static void 
  setTextLinkFromClipboard(
		  final Shell shell, final GridData gridData, final Text url, boolean accept_magnets ) {
    String link = getLinkFromClipboard(shell.getDisplay(),accept_magnets);
    setTextLink(shell, gridData, url, link);
  }

  /**
   * Sets a text into the URL dialog and adjusts the dialog size (and location).
   * @param shell
   * @param gridData
   * @param url the URL text control
   * @param link
   * @author Rene Leonhardt
   */
  public static void setTextLink(final Shell shell, final GridData gridData, final Text url, String link) {
    if(link == null)
      return;
    if(link.length() > 7) {
      GC gc = new GC(url);
      FontMetrics fm = gc.getFontMetrics();
      int width = (link.length() + 10) * fm.getAverageCharWidth();
      if(width > gridData.widthHint) {
        if (width > shell.getDisplay().getBounds().width) {
          gridData.widthHint = shell.getDisplay().getBounds().width - 20;
          shell.setLocation(0, 0);
        } else {
          gridData.widthHint = width;
        }
      }
    }
    url.setText(link);
  }

  /**
   * <p>Gets an URL from the clipboard if a valid URL for downloading has been copied.</p>
   * <p>The supported protocols currently are http, https, and magnet.</p>
   * @param display
   * @return first valid link from clipboard, else "http://"
   */
  public static String 
  getLinkFromClipboard(
	 Display 	display,
	 boolean	accept_magnets ) 
  {
    final String[] prefixes = new String[] {"http://", "https://", "magnet:?", "magnet://?" };
    
    final Clipboard cb = new Clipboard(display);
    final TextTransfer transfer = TextTransfer.getInstance();
    
    String data = (String)cb.getContents(transfer);
    
    if (data != null) {
      data	= data.trim();
      for(int i = 0; i < (accept_magnets?prefixes.length:2 ); i++) {
        final int begin = data.indexOf(prefixes[i]);
        if (begin >= 0) {
          final int end = data.indexOf("\n", begin + prefixes[i].length());
          final String stringURL = (end >= 0) ? data.substring(begin, end - 1) : data.substring(begin);
          try {
            final URL parsedURL = new URL(stringURL);
            return parsedURL.toExternalForm();
          } catch (MalformedURLException e1) {
          }
        }
      }
    
      if ( accept_magnets && data.length() == 40 ){
    	  
    	  for (int i=0;i<data.length();i++){
    		  
    		  if ( "0123456789abcdefABCDEF".indexOf( data.charAt(i)) == -1 ){
    			  
    			  return( prefixes[0] );
    		  }
    	  }
    	  
    	  	// accept raw hash of 40 hex chars
    	  
    	  return( data );
      }
    }
    
    return prefixes[0];
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

  public static void saveTableColumn(TableColumn[] t) {
    for (int i = 0; i < t.length; i++) {
      if(t[i] != null && t[i].getData("configName") != null) {
        COConfigurationManager.setParameter((String) t[i].getData("configName") + ".width", t[i].getWidth());
      }
    }
    COConfigurationManager.save();
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

  public static void
  centreWindow(
  	Shell	shell )
  {
    Rectangle displayRect;
    try {
    	displayRect = shell.getMonitor().getClientArea();
    } catch (NoSuchMethodError e) {
      displayRect = shell.getDisplay().getClientArea();
    }

    Rectangle shellRect = shell.getBounds();
	
    int x = (displayRect.width - shellRect.width) / 2;
    int y = (displayRect.height - shellRect.height) / 2;

    shell.setLocation(x, y);
  }

  /**
   * Centers a window relative to a control. That is to say, the window will be located at the center of the control.
   * @param window
   * @param control
   */
  public static void centerWindowRelativeTo(final Shell window, final Control control)
  {
      final Rectangle bounds = control.getBounds();
      final Point shellSize = window.getSize();
      window.setLocation(
              bounds.x + (bounds.width / 2) - shellSize.x / 2,
              bounds.y + (bounds.height / 2) - shellSize.y / 2
      );
  }

  /**
   * @param control the control (usually a Shell) to add the DropTarget
   * @param url the Text control where to set the link text
   *
   * @author Rene Leonhardt
   */
  public static void createURLDropTarget(final Control control, final Text url) {
    DropTarget dropTarget = new DropTarget(control, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
    dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance()});
    dropTarget.addDropListener(new DropTargetAdapter() {
      public void dragOver(DropTargetEvent event) {
        event.detail = DND.DROP_LINK;
      }
      public void drop(DropTargetEvent event) {
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.widthHint=300;
        setTextLink(control instanceof Shell ? (Shell) control : control.getShell(), gridData, url, ((URLTransfer.URLType)event.data).linkURL);
        url.setLayoutData(gridData);
        control.pack();
      }
    });
  }

  /**
   * Force label to use more vertical space if wrapped and in a GridLayout
   * Place this listener on the _parent_ of the label
   * See Eclipse SWT Bug #9866 (GridLayout does not handle wrapped Label properly)
   * This workaround only works for labels who:
   *   - horizontally span their whole parent 
   *     (ie. the parent has 3 columns, the label must span 3 columns)
   *   - GridData style has GridData.FILL_HORIZONTAL
   *   - Label style has SWT.WRAP
   *
   * @author TuxPaper
   * @note Bug 9866 fixed in 3105 and later
   */
  public static class LabelWrapControlListener extends ControlAdapter{
  	public void controlResized(ControlEvent e){
  		if (SWT.getVersion() >= 3105)
  			return;
  	  Composite parent = (Composite)e.widget;
  	  Control children[] = parent.getChildren();

  	  if (children.length > 0) {
        GridLayout parentLayout = (GridLayout)parent.getLayout();
        if (parentLayout != null) {
    	    Point size;
          int marginWidth = parentLayout.marginWidth;
          
      	  Composite grandParent = parent.getParent();
      	  if (grandParent instanceof ScrolledComposite) {
      	    Composite greatGP = grandParent.getParent();
      	    if (greatGP != null) {
              size = greatGP.getSize();
  
              if (greatGP.getLayout() instanceof GridLayout) {
                marginWidth += ((GridLayout)greatGP.getLayout()).marginWidth;
              }
            } else {
              // not tested
              size = grandParent.getSize();
            }

            if (grandParent.getLayout() instanceof GridLayout) {
              marginWidth += ((GridLayout)grandParent.getLayout()).marginWidth;
            }

            ScrollBar sb = grandParent.getVerticalBar();
            if (sb != null) {
              // I don't know why, but we have to remove one
              size.x -= sb.getSize().x + 1;
            }
          } else
            size = parent.getSize();
         
          boolean oneChanged = false;
      	  for (int i = 0; i < children.length; i++) {
      	    if ((children[i] instanceof Label) &&
      	        (children[i].getStyle() & SWT.WRAP) == SWT.WRAP) {
      	      GridData gd = (GridData)children[i].getLayoutData();
      	      if (gd != null && 
      	          gd.horizontalAlignment == GridData.FILL) {
      	        if (gd.horizontalSpan == parentLayout.numColumns) {
        		      gd.widthHint = size.x - 2 * marginWidth;
        		      oneChanged = true;
        		    } else {
        		      Point pt = children[i].getLocation();
        		      gd.widthHint = size.x - pt.x - (2 * marginWidth);
        		      oneChanged = true;
        		    }
      		    }
      		  }
      		}
      		if (oneChanged) {
      		  parent.layout(true);
        	  if (grandParent instanceof ScrolledComposite) {
        	    ((ScrolledComposite)grandParent).setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
        	  }
          }
      	}
    	} // size
  	} // controlResized
  } // class

  public static void alternateTableBackground(Table table) {
  	int iTopIndex = table.getTopIndex();
    int iBottomIndex = Math.min(iTopIndex
				+ (table.getClientArea().height / table.getItemHeight()), table
				.getItemCount() - 1);

  	Color[] colors = { table.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND),
        Colors.colorAltRow };
  	int iFixedIndex = iTopIndex;
    for (int i = iTopIndex; i <= iBottomIndex; i++) {
      TableItem row = table.getItem(i);
      // Rows can be disposed!
      if (!row.isDisposed()) {
      	Color newColor = colors[iFixedIndex % colors.length];
      	iFixedIndex++;
      	if (!row.getBackground().equals(newColor)) {
//        System.out.println("setting "+rows[i].getBackground() +" to " + newColor);
      		row.setBackground(newColor);
      	}
      }
    }
  }

  /**
   * <p>
   * Set a MenuItem's image with the given ImageRepository key. In compliance with platform
   * human interface guidelines, the images are not set under Mac OS X.
   * </p>
   * @param item SWT MenuItem
   * @param repoKey ImageRepository image key
   * @see <a href="http://developer.apple.com/documentation/UserExperience/Conceptual/OSXHIGuidelines/XHIGMenus/chapter_7_section_3.html#//apple_ref/doc/uid/TP30000356/TPXREF116">Apple HIG</a>
   */
  public static void setMenuItemImage(final MenuItem item, final String repoKey)
  {
      if(!Constants.isOSX)
          item.setImage(ImageRepository.getImage(repoKey));
  }

  public static void setMenuItemImage(final MenuItem item, final Image image)
  {
      if(!Constants.isOSX)
          item.setImage(image);
  }
  /**
   * Execute code in the Runnable object using SWT's thread.  If current
   * thread it already SWT's thread, the code will run immediately.  If the
   * current thread is not SWT's, code will be run either synchronously or 
   * asynchronously on SWT's thread at the next reasonable opportunity.
   * 
   * @param code code to run
   * @param async true if SWT asyncExec, false if SWT syncExec
   * @return success
   */
  public static boolean execSWTThread(Runnable code,
			boolean async) {
  	Display display = MainWindow.getWindow().getDisplay();

  	if (display == null || display.isDisposed())
			return false;

		if (display.getThread() == Thread.currentThread())
			code.run();
		else if (async)
			display.asyncExec(code);
		else
			display.syncExec(code);

		return true;
	}

  /**
   * Execute code in the Runnable object using SWT's thread.  If current
   * thread it already SWT's thread, the code will run immediately.  If the
   * current thread is not SWT's, code will be run asynchronously on SWT's 
   * thread at the next reasonable opportunity.
   * 
   * @param code code to run
   * @return success
   */
	public static boolean execSWTThread(Runnable code) {
		return execSWTThread(code, true);
	}
	
	public static boolean isThisThreadSWT() {
  	Display display = MainWindow.getWindow().getDisplay();
  	if (display == null || display.isDisposed()) {
  		System.out.println("display null");
			return false;
  	}
		return (display.getThread() == Thread.currentThread());
	}
	
	public static void
	openURL(
		String		url )
	{
		Program.launch( url );
	}
}

