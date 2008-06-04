/**
 * 
 */
package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.custom.CLabel;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;

/**
 * @author Allan Crooks
 *
 */
public class UISWTStatusEntryImpl implements UISWTStatusEntry, MainStatusBar.CLabelUpdater {
	
	private AEMonitor this_mon = new AEMonitor("UISWTStatusEntryImpl@" + Integer.toHexString(this.hashCode()));
	
	private UISWTStatusEntryListener listener = null;
	
	// Used by "update".
	private boolean needs_update = false;
	private String text = null;
	private String tooltip = null;
	private boolean image_enabled = false;
	private Image image = null;
	private boolean is_visible = false;
	private boolean needs_disposing = false;
	private boolean is_destroyed = false;
	
	private List	menu_items = new ArrayList();
	private Menu	menu;
	
	private void checkDestroyed() {
		if (is_destroyed) {throw new RuntimeException("object is destroyed, cannot be reused");}
	}
	
	public void update(CLabel label) {
		if (needs_disposing && !label.isDisposed()) {label.dispose(); return;}
		if (!needs_update) {return;}
		
		// This is where we do a big update.
		try {
			this_mon.enter();
			update0(label);
		}
		finally {
			this_mon.exit();
		}
	}
	
	private void update0(final CLabel label) {
		label.setText(text);
		label.setToolTipText(tooltip);
		label.setImage(image_enabled ? image : null);
		label.setVisible(this.is_visible);
		
		if ( menu_items.size() > 0 && menu == null ){
			
			menu = new Menu(label);
			
			label.setMenu( menu );
			
			MenuBuildUtils.addMaintenanceListenerForMenu(menu,
					new MenuBuildUtils.MenuBuilder() {
						public void 
						buildMenu(
							Menu menu) 
						{
							this_mon.enter();
							
							MenuItem[]	items = (MenuItem[])menu_items.toArray(new MenuItem[ menu_items.size()]);
							
							this_mon.exit();
							
							MenuBuildUtils.addPluginMenuItems(
									label, 
									items, 
									menu, 
									true,
									true, 
									MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER );
						}
			});

			
		}else if ( menu_items.size() == 0 && menu != null ){
			
			label.setMenu( null );
			
			menu = null;
		}
	}
	
	void onClick() {
		UISWTStatusEntryListener listener0 = listener; // Avoid race conditions.
		if (listener0 != null) {listener.entryClicked(this);}
	}

	public void destroy() {
		try {
			this_mon.enter();
			this.is_visible = false;
			this.listener = null;
			this.image = null;
			this.needs_disposing = true;
			this.is_destroyed = true;
		}
		finally {
			this_mon.exit();
		}
	}

	public void setImage(int image_id) {
		String img_name;
		switch (image_id) {
			case IMAGE_LED_GREEN:
				img_name = "greenled";
				break;
			case IMAGE_LED_RED:
				img_name = "redled";
				break;
			case IMAGE_LED_YELLOW:
				img_name = "yellowled";
				break;
			default:
				img_name = "grayled";
				break;
		}
		this.setImage(ImageRepository.getImage(img_name));
	}

	public void setImage(Image image) {
		checkDestroyed();
		this_mon.enter();
		this.image = image;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setImageEnabled(boolean enabled) {
		checkDestroyed();
		this_mon.enter();
		this.image_enabled = enabled;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setListener(UISWTStatusEntryListener listener) {
		checkDestroyed();
		this.listener = listener;
	}

	public void setText(String text) {
		checkDestroyed();
		this_mon.enter();
		this.text = text;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setTooltipText(String text) {
		checkDestroyed();
		this_mon.enter();
		this.tooltip = text;
		this.needs_update = true;
		this_mon.exit();
	}

	public void setVisible(boolean visible) {
		checkDestroyed();
		this_mon.enter();
		this.is_visible = visible;
		this.needs_update = true;
		this_mon.exit();
	}

	public MenuItem addMenuItem( String resource_key ){
		
		final MenuItemImpl item = new MenuItemImpl(MenuManager.MENU_STATUS_ENTRY, resource_key );
		
		this_mon.enter();
		
		menu_items.add( item );
		
		needs_update = menu == null;;
		
		this_mon.exit();
		
		item.setRemoveListener(
			new MenuItemImpl.removeListener()
			{
				public void 
				removed() 
				{
					this_mon.enter();
					
					menu_items.remove( item );
					
					needs_update = menu_items.size() == 0;
					
					this_mon.exit();
				}
			});
		
		return( item );
	}
}
