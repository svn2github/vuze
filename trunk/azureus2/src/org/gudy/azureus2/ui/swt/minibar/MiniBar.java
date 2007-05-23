/*
 * Created on 12 May 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt.minibar;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * @author Allan Crooks
 *
 */
public abstract class MiniBar implements MenuBuildUtils.MenuBuilder {
	
	//
	// These are components which are related to the bar itself.
	//
	protected Label lDrag;
	protected MiniBar stucked;
	protected Rectangle screen;
	protected int xPressed, yPressed;
	protected boolean moving;
	protected int hSize;
	protected Shell splash;
	protected MiniBarManager manager;
	
	private Font bold_font = null;

	//
	// These are components used during the construction of the bar.
	//
	private boolean constructing = false;
	private boolean constructed = false;
	private Menu menu;
	private MouseMoveListener mMoveListener;
	private MouseListener mListener;
	private int xSize;
	private boolean separateDataProt;
	
	protected MiniBar(MiniBarManager manager) {
		this.manager = manager;
		this.constructing = false;
		this.constructed = false;
		this.xSize = 0;
		this.hSize = -1;
		
		this.separateDataProt = DisplayFormatters.isDataProtSeparate();
	}
	
	//
	// These methods provide a construction API for subclasses.
	//
	private void assertConstructing() {
		if (!this.constructing)
			throw new RuntimeException("not constructing!");
	}
	
	private Font createBoldFont(Font original) {
		FontData[] font_data = original.getFontData();
		for (int i=0; i<font_data.length; i++) {
			font_data[i].setStyle(font_data[i].getStyle() | SWT.BOLD);
		}
		return new Font(original.getDevice(), font_data);
	}
	
	protected final void createGap(int width) {
		// We create a label just so we can attach the menu to it.
		assertConstructing();
		Label result = new Label(splash, SWT.NONE);
	    result.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
	    result.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
	    result.setText("");
	    result.addMouseListener(this.mListener);
	    result.addMouseMoveListener(this.mMoveListener);
	    result.setLocation(this.xSize, 0);
	    result.setSize(width, hSize);
	    result.setMenu(this.menu);
	    this.xSize += width;
	}
	
	protected final void createFixedTextLabel(String msg_key, boolean add_colon, boolean bold) {
		assertConstructing();
	    Label result = new Label(splash, SWT.NONE);
	    result.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
	    result.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
	    result.setText(MessageText.getString(msg_key) + ((add_colon) ? ":" : ""));
	    if (bold) {
	    	if (this.bold_font == null) {
	    		this.bold_font = createBoldFont(result.getFont());
	    	}
	    	result.setFont(this.bold_font);
	    }
	    result.addMouseListener(this.mListener);
	    result.addMouseMoveListener(this.mMoveListener);
	    result.pack();
	    result.setLocation(this.xSize, 0);
	    result.setMenu(this.menu);
	    if (this.hSize == -1) {
	        int hSizeText = result.getSize().y;
	        int hSizeImage = this.lDrag.getSize().y;
	        this.hSize = hSizeText > hSizeImage ? hSizeText : hSizeImage;
	    }
	    this.xSize += result.getSize().x + 3;
	}
	
	protected final Label createDataLabel(int width) {
		assertConstructing();
	    Label result = new Label(splash, SWT.CENTER);
	    result.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
	    result.setText("");
	    result.addMouseListener(this.mListener);
	    result.addMouseMoveListener(this.mMoveListener);
	    if (this.hSize == -1) {
	    	throw new RuntimeException("must add fixed text label first!");
	    }
	    result.setSize(width, hSize);
	    result.setLocation(this.xSize, 0);
	    result.setMenu(this.menu);
	    this.xSize += width + 3;
	    return result;
	}
	
	protected final ProgressBar createProgressBar(int min, int max, int width, final ProgressBarText pbt) {
		final ProgressBar result = new ProgressBar(splash, SWT.SMOOTH);
		result.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
		result.setForeground(Colors.blues[Colors.BLUES_MIDLIGHT]);
		result.setMinimum(min);
		result.setMaximum(max);
		result.addMouseListener(this.mListener);
		result.addMouseMoveListener(this.mMoveListener);
	    if (this.hSize == -1) {
	    	throw new RuntimeException("must add fixed text label first!");
	    }
		result.setSize(width, hSize);
		result.setLocation(this.xSize, 0);
		result.setMenu(this.menu);
		this.xSize += width + 3;
		
		// Add a listener to display text on the progress bar.
		if (pbt != null) {
			result.addListener(SWT.Paint, new Listener() {
	    		public void handleEvent(Event event) {
	    			Color old = event.gc.getForeground(); 
	    			event.gc.setForeground(Colors.black);
	    			int	char_width = event.gc.getFontMetrics().getAverageCharWidth();
	    			String pb_text = pbt.convert(result.getSelection());
	    			event.gc.drawText(pb_text, (result.getSize().x - pb_text.length() * char_width )/2, -1, true);
	    			event.gc.setForeground(old);
	    		}
			});
		}

		return result;
	}
	
	protected final ProgressBar createPercentProgressBar(int width) {
		return createProgressBar(0, 1000, width, new ProgressBarText() {
			public String convert(int value) {
				return DisplayFormatters.formatPercentFromThousands(value);
			}
		});
	}
	
	protected int
	getDataLabelWidth()
	{
		if ( separateDataProt ){
			
			return( 110 );
			
		}else{
			
			return( 65 );
		}
	}
	
	protected boolean
	isSeparateDataProt()
	{
		return( separateDataProt );
	}
	
	protected static interface ProgressBarText {
		public String convert(int value);
	}
	
	
	//
	// These methods define the main MiniBar behaviour.
	//

	public final void construct(Shell main) {
		if (this.constructed) {
			throw new RuntimeException("already constructed!");
		}
		
		this.constructing = true;
		
		this.stucked = null;
		this.splash = org.gudy.azureus2.ui.swt.components.shell.ShellFactory
				.createShell(SWT.ON_TOP);
		manager.register(this);
		main.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				close();
			}
		});
		
	    this.screen = main.getDisplay().getClientArea();
	    
	    lDrag = new Label(splash, SWT.NULL);
	    if(!Constants.isOSX) {
	      lDrag.setImage(ImageRepository.getImage("dragger"));
	    }
	    lDrag.pack();
	    
	    this.xSize = lDrag.getSize().x + 3;
	    lDrag.setLocation(0, 0);

	    this.mListener = new MouseAdapter() {
	      public void mouseDown(MouseEvent e) {
	        xPressed = e.x;
	        yPressed = e.y;
	        moving = true;
	        //System.out.println("Position : " + xPressed + " , " + yPressed);          
	      }

	      public void mouseUp(MouseEvent e) {
	        moving = false;
	      }

	    };
	    this.mMoveListener = new MouseMoveListener() {
	      public void mouseMove(MouseEvent e) {
	        if (moving) {
	          int dX = xPressed - e.x;
	          int dY = yPressed - e.y;
	          //System.out.println("dX,dY : " + dX + " , " + dY);
	          Point currentLoc = splash.getLocation();
	          currentLoc.x -= dX;
	          currentLoc.y -= dY;
	          setSnapLocation(currentLoc);
	          //System.out.println("Position : " + xPressed + " , " + yPressed);
	        }
	      }
	    };
	    
	    splash.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
	    splash.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
	    splash.addMouseListener(mListener);
	    splash.addMouseMoveListener(mMoveListener);
	    lDrag.addMouseListener(mListener);
	    lDrag.addMouseMoveListener(mMoveListener);
	    
	    this.menu = new Menu(splash, SWT.POP_UP);
	    MenuBuildUtils.addMaintenanceListenerForMenu(menu, this);
	    this.beginConstruction();
	    
	    splash.addListener(SWT.Deiconify, new Listener() {
	        public void handleEvent(Event e) {
	          splash.setVisible(true);
	          splash.setActive();
	        }
	      });
	      splash.setSize(xSize + 3, hSize + 2);
	    
	    // Tidy up construction variables.
	    this.mListener = null;
	    this.mMoveListener = null;
	    this.menu = null;

	    // Avoid doing a refresh on construction
	    this.refresh();
	    this.constructing = false;
	    this.constructed = true;
	    
	    splash.setVisible(true);
	    
	}
	
	public void buildMenu(Menu menu) {
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;
		Object plugin_context_obj = this.getPluginMenuContextObject();
		String plugin_menu_id = this.getPluginMenuIdentifier(plugin_context_obj);
		if (plugin_menu_id != null) {
			menu_items = MenuItemManager.getInstance().getAllAsArray(plugin_menu_id);
			if (menu_items.length > 0) {
				MenuBuildUtils.addPluginMenuItems(splash, menu_items, menu, true, true,
						// This will retrieve the plugin download object for associated menus.
						new MenuBuildUtils.MenuItemPluginMenuControllerImpl(plugin_context_obj)
				);
				new MenuItem(menu, SWT.SEPARATOR);
			}
		}
		
	    MenuItem itemClose = new MenuItem(menu,SWT.NULL);
	    itemClose.setText(MessageText.getString("wizard.close"));
	    itemClose.addListener(SWT.Selection,new Listener() {
	    	public void handleEvent(Event e) {
	    		close();
	    	}
	    });
	}
	
	protected final void setSnapLocation(Point currentLoc) {
		if (currentLoc.x < 10)
			currentLoc.x = 0;
		else if (currentLoc.x > screen.width - splash.getBounds().width - 10)
			currentLoc.x = screen.width - splash.getBounds().width;
		if (currentLoc.y < 10)
			currentLoc.y = 0;
		MiniBar mw = this;
		int height = 0;
		while (mw != null) {
			Shell s = mw.getShell();
			if (s.isDisposed())
				mw = null;
			else {
				height += s.getBounds().height - 1;
				mw = mw.getStucked();
				if (mw == this)
					mw = null;
			}
		}
		if (currentLoc.y > screen.height - height - 10)
			currentLoc.y = screen.height - height;

		MiniBarManager g_manager = MiniBarManager.getManager();
		try {
			g_manager.getMiniBarMonitor().enter();
			if (g_manager.countMiniBars() > 1) {
				Iterator itr = g_manager.getMiniBarIterator();
				while (itr.hasNext()) {
					MiniBar downloadBar = (MiniBar) itr.next();
					Point location = downloadBar.getShell().getLocation();
					// isn't the height always 10?
					// Gudy : No it depends on your system font.
					location.y += downloadBar.getShell().getBounds().height;
					//Stucking to someone else
					if (downloadBar != this && downloadBar.getStucked() == null
							|| downloadBar.getStucked() == this) {
						if (Math.abs(location.x - currentLoc.x) < 10
								&& location.y - currentLoc.y < 10
								&& location.y - currentLoc.y > 0) {
							downloadBar.setStucked(this);
							currentLoc.x = location.x;
							currentLoc.y = location.y - 1;
						}
					}
					//Un-stucking from someone
					if (downloadBar != this && downloadBar.getStucked() == this) {
						if (Math.abs(location.x - currentLoc.x) > 10
								|| Math.abs(location.y - currentLoc.y) > 10)
							downloadBar.setStucked(null);
					}
				}
			}
		}
		finally {
			g_manager.getMiniBarMonitor().exit();
		}

		splash.setLocation(currentLoc);
		MiniBar mwCurrent = this;
		while (mwCurrent != null) {
			currentLoc.y += mwCurrent.getShell().getBounds().height - 1;
			MiniBar mwChild = mwCurrent.getStucked();
			if (mwChild != null && mwChild != this) {
				Shell s = mwChild.getShell();
				if (s.isDisposed()) {
					mwCurrent.setStucked(null);
					mwCurrent = null;
				}
				else {
					mwCurrent = mwChild;
					mwCurrent.getShell().setLocation(currentLoc);
				}
			}
			else
				mwCurrent = null;
		}
	}


	
	//
	// These methods define the management of MiniBars.
	//
	public Shell getShell() {
		return this.splash;
	}

	public void setVisible(boolean visible) {
		splash.setVisible(visible);
	}

	public final boolean hasSameContext(MiniBar m) {
		return this.hasContext(m.getContextObject());
	}

	public final boolean hasContext(Object context) {
		Object my_context = this.getContextObject();
		if (my_context == null) {
			return context == null;
		}
		else {
			return my_context.equals(context);
		}
	}
	
	public MiniBar getStucked() {
		return this.stucked;
	}

	public void setStucked(MiniBar mw) {
		this.stucked = mw;
	}

	public void close() {
		if (!splash.isDisposed()) {
			Display display = splash.getDisplay();
			if (display != null && !display.isDisposed()) {
				display.asyncExec(new AERunnable() {
					public void runSupport() {
						if (!splash.isDisposed()) {
							splash.dispose();
						}
						if (bold_font != null && !bold_font.isDisposed()) {
							bold_font.dispose();
						}
					}
				});
			}
		}
		manager.unregister(this);
	}
	

	//
	// Subclass methods.
	//
	protected abstract void refresh();
	protected abstract void beginConstruction();
	protected abstract Object getContextObject();
	
	public String getPluginMenuIdentifier(Object context) {
		return null;
	}
	
	public Object getPluginMenuContextObject() {
		return null;
	}

}
