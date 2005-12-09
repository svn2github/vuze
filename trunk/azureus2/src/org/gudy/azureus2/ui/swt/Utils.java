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
import java.util.ArrayList;

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
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

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
		  final Shell shell, final Text url, boolean accept_magnets ) {
    String link = getLinkFromClipboard(shell.getDisplay(),accept_magnets);
    if (link != null)
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
			public void dropAccept(DropTargetEvent event) {
				event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
						event.currentDataType);
			}
      public void dragOver(DropTargetEvent event) {
      	if ((event.operations & DND.DROP_LINK) > 0)
      		event.detail = DND.DROP_LINK;
      	else if ((event.operations & DND.DROP_COPY) > 0)
      		event.detail = DND.DROP_COPY;
      }
      public void drop(DropTargetEvent event) {
        if (((URLTransfer.URLType)event.data).linkURL != null)
        	url.setText(((URLTransfer.URLType)event.data).linkURL);
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

  public static void alternateRowBackground(TableItem item) {
  	if (item == null || item.isDisposed())
  		return;
  	Color[] colors = { item.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND),
        Colors.colorAltRow };
  	Color newColor = colors[ item.getParent().indexOf(item) % colors.length];
  	if (!item.getBackground().equals(newColor)) {
  		item.setBackground(newColor);
  	}
  }

  public static void alternateTableBackground(Table table) {
  	if (table == null || table.isDisposed())
  		return;

  	// On linux, table lines are actually anternating background colors
  	if (Constants.isLinux) {
  		if (!table.getLinesVisible())
  			table.setLinesVisible(true);
  		return;
  	}

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
   * Sets the shell's Icon(s) to the default Azureus icon.  OSX doesn't require
   * an icon, so they are skipped
   * 
   * @param shell
   */
  public static void setShellIcon(Shell shell) {
		final String[] sImageNames = { "azureus", "azureus32", "azureus64",
				"azureus128" };

		if (Constants.isOSX)
			return;

		try {
			ArrayList list = new ArrayList();
			Image[] images = new Image[] { ImageRepository.getImage("azureus"),
					ImageRepository.getImage("azureus32"),
					ImageRepository.getImage("azureus64"),
					ImageRepository.getImage("azureus128") };

			for (int i = 0; i < images.length; i++) {
				Image image = ImageRepository.getImage(sImageNames[i]);
				if (image != null)
					list.add(image);
			}

			if (list.size() == 0)
				return;

			shell.setImages((Image[]) list.toArray(new Image[0]));
		} catch (NoSuchMethodError e) {
			// SWT < 3.0
			Image image = ImageRepository.getImage(sImageNames[0]);
			if (image != null)
				shell.setImage(image);
		}
	}

  /**
   * Execute code in the Runnable object using SWT's thread.  If current
   * thread it already SWT's thread, the code will run immediately.  If the
   * current thread is not SWT's, code will be run either synchronously or 
   * asynchronously on SWT's thread at the next reasonable opportunity.
   * 
   * This method does not catch any exceptions.
   * 
   * @param code code to run
   * @param async true if SWT asyncExec, false if SWT syncExec
   * @return success
   */
  public static boolean execSWTThread(Runnable code,
			boolean async) {
    SWTThread swt = SWTThread.getInstance();
    
    if (swt == null) {
    	System.err.println("SWT Thread not started yet");
    	return false;
    }
    
    Display display = swt.getDisplay();

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
   * This method does not catch any exceptions.
   * 
   * @param code code to run
   * @return success
   */
	public static boolean execSWTThread(Runnable code) {
		return execSWTThread(code, true);
	}
	
	public static boolean isThisThreadSWT() {
    SWTThread swt = SWTThread.getInstance();
    
    if (swt == null) {
    	System.err.println("SWT Thread not started yet");
    	return false;
    }

    Display display = swt.getDisplay();

  	if (display == null || display.isDisposed())
			return false;

		return (display.getThread() == Thread.currentThread());
	}

	/** Open a messagebox using resource keys for title/text
	 * 
	 * @param parent Parent shell for messagebox
	 * @param style SWT styles for messagebox
	 * @param keyPrefix message bundle key prefix used to get title and text.  
	 *         Title will be keyPrefix + ".title", and text will be set to
	 *         keyPrefix + ".text"
	 * @param textParams any parameters for text
	 * 
	 * @return what the messagebox returns
	 */
	public static int openMessageBox(Shell parent, int style, String keyPrefix,
			String[] textParams) {
		MessageBox mb = new MessageBox(parent, style);
		mb.setMessage(MessageText.getString(keyPrefix + ".text", textParams));
		mb.setText(MessageText.getString(keyPrefix + ".title"));
		return mb.open();
	}

	/** Open a messagebox with actual title and text
	 * 
	 * @param parent
	 * @param style
	 * @param title
	 * @param text
	 * @return
	 */ 
	public static int openMessageBox(Shell parent, int style, String title,
			String text) {
		MessageBox mb = new MessageBox(parent, style);
		mb.setMessage(text);
		mb.setText(title);
		return mb.open();
	}
	
	/**
	 * Bottom Index may be negative
	 */ 
	public static int getTableBottomIndex(Table table, int iTopIndex) {
		return Math.min(iTopIndex
				+ ((table.getClientArea().height - table.getHeaderHeight() - 1) / table
						.getItemHeight()), table.getItemCount() - 1);
	}
	
	public static void
	openURL(
		String		url )
	{
		Program.launch( url );
	}
}

