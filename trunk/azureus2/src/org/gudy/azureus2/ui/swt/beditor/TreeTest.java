/*
 * File    : TreeTest.java
 * Created : 1 oct. 2003
 * By      : Paul Duran 
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
package org.gudy.azureus2.ui.swt.beditor;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * @author Paul Duran
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TreeTest {
	
	private Tree theTree;
	private Composite treeComposite;
	private boolean showArrayIndices;
	private boolean isEditable;
		
	public TreeTest(Decorations parent) throws FileNotFoundException
	{
		//		FormLayout layout = new FormLayout();

//		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
//		layout.pack = false;
//		layout.justify = true;
//		layout.marginLeft = 3;
//		layout.marginRight = 3;
//		layout.marginTop = 3;
//		layout.marginBottom = 3;
		FillLayout layout = new FillLayout();
		treeComposite = new Composite(parent, SWT.NORMAL);
		treeComposite.setLayout(new FillLayout());
		treeComposite.setSize(400,500);	
		theTree = new Tree(treeComposite, SWT.MULTI);
		setupContextMenu(parent, theTree);

	}
	public Composite getPanel()
	{
		return treeComposite;
	}
	/**
	 * attaches some context menu items to the specified tree item.
	 * @param item
	 */
	private void setupContextMenu(Decorations theWindow, final Tree theTree)
	{
		System.out.println("adding menu");
		final Menu menu = new Menu((Decorations)theWindow, SWT.POP_UP);
		theTree.setMenu(menu);
		final MenuItem addItem;
		final MenuItem editItem;
		final MenuItem removeItem;		

		addItem = new MenuItem(menu, SWT.PUSH);
		addItem.setText("New");
		addItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("allowing them to add another item"); 		
			}			
		});
		
		editItem = new MenuItem(menu, SWT.PUSH);
		editItem.setText("Edit");
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("allowing them to edit an item"); 		
			}			
		});

		removeItem = new MenuItem(menu, SWT.PUSH);
		removeItem.setText("Remove");
		removeItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("removing item");
			}
		});	

		menu.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				System.out.println("Show event received");
				if( theTree.getSelectionCount() == 1 ) {
					TreeItem selectedItem = theTree.getSelection()[0];
					if( selectedItem.getItemCount() == 0 ) {
						addItem.setEnabled(false);
					} else {
						addItem.setEnabled(true);
					}
					editItem.setEnabled(true);
					removeItem.setEnabled(true);
				} else {
					addItem.setEnabled(false);
					editItem.setEnabled(false);
					removeItem.setEnabled(false);
				}
				TreeItem []items = theTree.getSelection();
				for (int i = 0; i < items.length; i++) {
					System.out.println(items[i] + " is selected");
				}
			}
		});
		
	}
	/**
	 * returns the selected tree items as a list rather than an array
	 * @return
	 */
	private List getSelectedItems()
	{
		List list = new ArrayList();
		TreeItem items[] = theTree.getSelection();
		for( int i = 0 ; i < items.length ; i++ )
		{
			list.add(items[i]);
		}
		return list;
	}
	/**
	 * adds each of the items out of a map to the specified TreeItem parent
	 * @param parent
	 * @param theData
	 */
	private void addDictionary( TreeItem parent, Map theData )
	{		
//		setupBranchNode(parent);
		List sortedKeys = new ArrayList(theData.keySet());		
		Collections.sort(sortedKeys);
		for (Iterator iter = sortedKeys.iterator(); iter.hasNext();) {
			String key = (String)iter.next();
			Object val = theData.get(key);
//			System.out.println("key: " + entry.getKey() + " . value: " + entry.getValue() + " of class: " + entry.getValue().getClass());
			TreeItem item = new TreeItem(parent, SWT.NORMAL);
			fillItem(item, key, val);
		}
	}
	private void fillItem( TreeItem item, String key, Object val)
	{
		String prefix = (key != null ? key + ": " : "");
		if( key == null)
			key = "";
		if( val instanceof Number ) {
//			System.out.println("its a number: " + val);
			item.setText(prefix + val);			
			item.setImage(ImageRepository.getImage("int"));
			item.setData("type", Number.class);
		} else if( val instanceof byte[] ) {
//			System.out.println("its a byte array!");
			byte []data = (byte [])val;
			if( data.length > 1000 ) {
				item.setText(prefix + "(large byte array)");
			} else {
				try {
					item.setImage(ImageRepository.getImage("string"));
					item.setText(prefix + new String(data, "ISO-8859-1"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			item.setData("type", String.class);
		} else if( val instanceof Map ) {
			item.setText(key);
			item.setImage(ImageRepository.getImage("dict"));
			item.setData("type", Map.class);
			addDictionary( item, (Map)val);
			item.setExpanded(true);
		} else if( val instanceof List ) {
			item.setText(key);
			item.setImage(ImageRepository.getImage("list"));
			item.setData("type", List.class);
			int index = 0;
			for (Iterator iterator = ((List)val).iterator(); iterator.hasNext(); ) 
			{
//				item.setImage(new Image(display, "d:/down.gif")); 
				Object iterVal = iterator.next();
				
				TreeItem child = new TreeItem(item, SWT.NORMAL);
				if( showArrayIndices )
					fillItem(child, "" + index, iterVal );
				else
					fillItem(child, null, iterVal);
					
				item.setExpanded(true);
				index++;
			}
		}
	}
	public void addMetaData(String fileName) throws FileNotFoundException
	{
		addMetaData(fileName, "/");
	}
	public void addMetaData(String fileName, String title) throws FileNotFoundException
	{
		BufferedInputStream bis = new BufferedInputStream( new FileInputStream(fileName));
		Map data = BDecoder.decode(bis);
		addMetaData(data, title);
	}
	public void addMetaData(byte[] data)
	{
		addMetaData(data, "/");
	}
	public void addMetaData(byte[] data,String title)
	{
		Map dataMap = BDecoder.decode(data);
		addMetaData(dataMap,title);
	}
	public void addMetaData(Map root)
	{
		addMetaData(root, "/");
	}
	public void addMetaData(Map root,String title)
	{
		TreeItem item = new TreeItem(theTree, SWT.NORMAL);
		item.setText(title);
		item.setImage(ImageRepository.getImage("root"));
		addDictionary( item, root );
		item.setExpanded(true);
		treeComposite.pack();
	}
	
	public static void waitForClose(Shell mainWindow, Display display)
	{
		while (!mainWindow.isDisposed()) {
		  try {
			if (!display.readAndDispatch())
			  display.sleep();
		  }
		  catch (Exception e) {
			e.printStackTrace();
		  }
		}
		display.dispose();
	}	
	public void setShowArrayIndices(boolean newValue)
	{
		this.showArrayIndices = newValue;
	}
	public void setEditable(boolean editable)
	{
		this.isEditable = editable;
	}
	public static void main(String args[]) throws FileNotFoundException
	{
		Shell mainWindow;
		Display display;
		
		System.out.println("starting");
		display = new Display();
		ImageRepository.loadImages(display);
		mainWindow = new Shell(display);
		Layout layout = new FillLayout();
		mainWindow.setLayout(layout);
		mainWindow.setText("SWT Test");

		String fileName;
		if( args.length > 0 )
			fileName = args[0];
		else 
			fileName = "d:/test2.torrent";
		TreeTest test = new TreeTest(mainWindow);
		test.addMetaData(fileName);
		test.addMetaData(fileName, "Another One");
		mainWindow.pack();
		mainWindow.open();		

		waitForClose(mainWindow,display);
	}

}
