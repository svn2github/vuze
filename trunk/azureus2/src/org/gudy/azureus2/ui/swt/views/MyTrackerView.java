/*
 * File    : MyTrackerView.java
 * Created : 30-Oct-2003
 * By      : parg
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

package org.gudy.azureus2.ui.swt.views;

/**
 * @author parg
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.TrackerChangerWindow;
import org.gudy.azureus2.ui.swt.views.tableitems.ManagerItem;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.*;

public class 
MyTrackerView 
	extends AbstractIView
{
	private Composite composite;
	private Composite panel;
	private Table table;
	private HashMap managerItems;
	private HashMap managers;
	private Menu menu;

	private HashMap downloadBars;

	public MyTrackerView(GlobalManager globalManager) {
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite composite0) {
	  if(panel != null) {      
		return;
	  }
	  composite = new Composite(composite0, SWT.NULL);
	  GridLayout layout = new GridLayout();
	  layout.numColumns = 1;
	  layout.horizontalSpacing = 0;
	  layout.verticalSpacing = 0;
	  layout.marginHeight = 0;
	  layout.marginWidth = 0;
	  composite.setLayout(layout);
	  GridData gridData = new GridData(GridData.FILL_BOTH);
          
	  panel = new Composite(composite, SWT.NULL);
	  panel.setLayoutData(gridData);
    
	  layout = new GridLayout(1, false);
	  layout.marginHeight = 0;
	  layout.marginWidth = 0;
	  layout.verticalSpacing = 0;
	  layout.horizontalSpacing = 0;
	  panel.setLayout(layout);
	}
	
	public Composite getComposite() {
	   return composite;
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#refresh()
	  */
	 public void refresh() {
	   if (getComposite() == null || getComposite().isDisposed())
		 return;
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#delete()
	  */
	 public void delete() {
	   MainWindow.getWindow().setMyTracker(null);
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
	  */
	 public String getShortTitle() {
	   return MessageText.getString("MyTrackerView.mytracker");
	 }

	 /* (non-Javadoc)
	  * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
	  */
	 public String getFullTitle() {
	   return MessageText.getString("MyTrackerView.mytracker");
	 }
}
