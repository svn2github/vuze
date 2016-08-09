/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.aelitis.azureus.ui.swt.mdi;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListenerEx;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventCancelledException;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.util.MapUtils;

/**
 * MDI Entry that is a {@link CTabItem} and belongs wo {@link TabbedMDI}
 * <p>
 * TODO: VitalityImages
 */
public class TabbedEntry
	extends BaseMdiEntry implements DisposeListener
{
	private static final String SO_ID_ENTRY_WRAPPER = "mdi.content.item";

	private CTabItem swtItem;

	private SWTSkin skin;

	private boolean showonSWTItemSet;

	private boolean buildonSWTItemSet;

	private static long uniqueNumber = 0;

	public TabbedEntry(TabbedMDI mdi, SWTSkin skin, String id, String parentViewID) {
		super(mdi, id, parentViewID);
		this.skin = skin;
	}

	public boolean
	canBuildStandAlone()
	{
		String skinRef = getSkinRef();

		if (skinRef != null){
			
			return( true );
			
		}else {
			
			UISWTViewEventListener event_listener = getEventListener();
			
			if ( event_listener instanceof UISWTViewCoreEventListenerEx && ((UISWTViewCoreEventListenerEx)event_listener).isCloneable()){

				return( true );
			}
		}
		
		return( false );
	}
	
	public SWTSkinObjectContainer 
	buildStandAlone(
		SWTSkinObjectContainer		soParent )
	{
		Control control = null;

		//SWTSkin skin = soParent.getSkin();
		
		Composite parent = soParent.getComposite();

		String skinRef = getSkinRef();
		
		if ( skinRef != null ){
			
			Shell shell = parent.getShell();
			Cursor cursor = shell.getCursor();
			try {
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

				// wrap skinRef with a container that we control visibility of
				// (invisible by default)
				SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
						"MdiContents." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
						soParent, null);
				
				SWTSkinObject skinObject = skin.createSkinObject(id, skinRef,
						soContents, getDatasourceCore());

				control = skinObject.getControl();
				control.setLayoutData(Utils.getFilledFormData());
				control.getParent().layout(true, true);
			
				soContents.setVisible( true );
							
				return( soContents );
				
			} finally {
				shell.setCursor(cursor);
			}
		} else {
			// XXX: This needs to be merged into BaseMDIEntry.initialize

			UISWTViewEventListener event_listener = getEventListener();
			
			if ( event_listener instanceof UISWTViewCoreEventListenerEx && ((UISWTViewCoreEventListenerEx)event_listener).isCloneable()){
				
				final UISWTViewImpl view = new UISWTViewImpl( getParentID(), id, true );
				
				try{
					view.setEventListener(((UISWTViewCoreEventListenerEx)event_listener).getClone(),false);
					
				}catch( Throwable e ){
					// shouldn't happen as we aren't asking for 'create' to occur which means it can't fail
					Debug.out( e );
				}
				
				view.setDatasource( datasource );

				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, SO_ID_ENTRY_WRAPPER,
							soParent );

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					final Composite viewComposite = soContents.getComposite();
					boolean doGridLayout = true;
					if (getControlType() == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
						doGridLayout = false;
					}
					//					viewComposite.setBackground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_BACKGROUND));
					//					viewComposite.setForeground(parent.getDisplay().getSystemColor(
					//							SWT.COLOR_WIDGET_FOREGROUND));
					if (doGridLayout) {
						GridLayout gridLayout = new GridLayout();
						gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
						viewComposite.setLayout(gridLayout);
						viewComposite.setLayoutData(Utils.getFilledFormData());
					}

					view.setPluginSkinObject(soContents);
					view.initialize(viewComposite);
					
					//swtItem.setText(view.getFullTitle());

					Composite iviewComposite = view.getComposite();
					control = iviewComposite;
					// force layout data of IView's composite to GridData, since we set
					// the parent to GridLayout (most plugins use grid, so we stick with
					// that instead of form)
					if (doGridLayout) {
						Object existingLayoutData = iviewComposite.getLayoutData();
						Object existingParentLayoutData = iviewComposite.getParent().getLayoutData();
						if (existingLayoutData == null
								|| !(existingLayoutData instanceof GridData)
								&& (existingParentLayoutData instanceof GridLayout)) {
							GridData gridData = new GridData(GridData.FILL_BOTH);
							iviewComposite.setLayoutData(gridData);
						}
					}

					parent.layout(true, true);
					
					final UIUpdater updater = UIFunctionsManager.getUIFunctions().getUIUpdater();

					updater.addUpdater(
						new UIUpdatable()  {
							
							public void updateUI() {
								if (viewComposite.isDisposed()){
									updater.removeUpdater( this );
								}else{
									view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
								}
							}
							
							public String getUpdateUIName() {
								return( "popout" );
							}
						});
					
					soContents.setVisible( true );
					
					view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
					
					return( soContents );
					
				} catch (Throwable e) {
					
					Debug.out(e);
				}
			}
		}
		
		return( null );
	}
	
	
	/* (non-Javadoc)
	 * @note SideBarEntrySWT is neary identical to this one.  Please keep them
	 *       in sync until commonalities are placed in BaseMdiEntry
	 */
	public void build() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_build();
				TabbedEntry.super.build();
			}
		});
	}
	
	public boolean swt_build() {
		if (swtItem == null) {
			buildonSWTItemSet = true;
			return true;
		}
		buildonSWTItemSet = false;

		Control control = swtItem.getControl();
		if (control == null || control.isDisposed()) {
			Composite parent = swtItem.getParent();
			SWTSkinObject soParent = (SWTSkinObject) parent.getData("SkinObject");

			String skinRef = getSkinRef();
			if (skinRef != null) {
				Shell shell = parent.getShell();
				Cursor cursor = shell.getCursor();
				try {
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

//					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
//							"MdiContents." + uniqueNumber++, "mdi.content.item",
//							soParent, getSkinRefParams());
//					skin.addSkinObject(soContents);

					
					SWTSkinObject skinObject = skin.createSkinObject(id, skinRef,
							soParent, getDatasourceCore());
					
					control = skinObject.getControl();
					control.setLayoutData(Utils.getFilledFormData());
					control.getParent().layout(true);
					// swtItem.setControl will set the control's visibility based on
					// whether the control is selected.  To ensure it doesn't set
					// our control invisible, set selection now
					CTabItem oldSelection = swtItem.getParent().getSelection();
					swtItem.getParent().setSelection(swtItem);
					swtItem.setControl(control);
					if (oldSelection != null) {
						swtItem.getParent().setSelection(oldSelection);
					}
					setPluginSkinObject(skinObject);
					setSkinObjectMaster(skinObject);


					initialize((Composite) control);
				} finally {
					shell.setCursor(cursor);
				}
			} else {
				// XXX: This needs to be merged into BaseMDIEntry.initialize
				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, "mdi.content.item",
							soParent);

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					Composite viewComposite = soContents.getComposite();
					//viewComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
					//viewComposite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

					boolean doGridLayout = true;
					if (getControlType() == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
						doGridLayout = false;
					}
					if (doGridLayout) {
  					GridLayout gridLayout = new GridLayout();
  					gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
  					viewComposite.setLayout(gridLayout);
  					viewComposite.setLayoutData(Utils.getFilledFormData());
					}

					setPluginSkinObject(soContents);

					initialize(viewComposite);

					Composite iviewComposite = getComposite();
					control = iviewComposite;
					if (doGridLayout) {
						Object existingLayoutData = iviewComposite.getLayoutData();
						Object existingParentLayoutData = iviewComposite.getParent().getLayoutData();
						if (existingLayoutData == null
								|| !(existingLayoutData instanceof GridData)
								&& (existingParentLayoutData instanceof GridLayout)) {
							GridData gridData = new GridData(GridData.FILL_BOTH);
							iviewComposite.setLayoutData(gridData);
						}
					}

					CTabItem oldSelection = swtItem.getParent().getSelection();
					swtItem.getParent().setSelection(swtItem);
					swtItem.setControl(viewComposite);
					if (oldSelection != null) {
						swtItem.getParent().setSelection(oldSelection);
					}
					setSkinObjectMaster(soContents);
				} catch (Exception e) {
					Debug.out("Error creating sidebar content area for " + id, e);
					try {
						setEventListener(null, false);
					} catch (UISWTViewEventCancelledException e1) {
					}
					close(true);
				}

			}

			if (control != null && !control.isDisposed()) {
				control.setData("BaseMDIEntry", this);
				/** XXX Removed this because we can dispose of the control and still
				 * want the tab (ie. destroy on focus lost, rebuild on focus gain)
				control.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						close(true);
					}
				});
				*/
			} else {
				return false;
			}
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#show()
	 */
	public void show() {
		// ensure show order by user execThreadLater
		// fixes case where two showEntries are called, the first from a non
		// SWT thread, and the 2nd from a SWT thread.  The first one will run last
		// showing itself
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				swt_show();
			}
		});
	}

	private void swt_show() {
		if (swtItem == null) {
			showonSWTItemSet = true;
			return;
		}
		showonSWTItemSet = false;
		if (!swt_build()) {
			return;
		}
		
		triggerOpenListeners();


		if (swtItem.getParent().getSelection() != swtItem) {
			swtItem.getParent().setSelection(swtItem);
		}

		super.show();
	}

	/**
	 * Tabs don't have Vitality Image support (yet)
	 */
	public MdiEntryVitalityImage addVitalityImage(String imageID) {
		return null; // new SideBarVitalityImageSWT(this, imageID);
	}

	public boolean isCloseable() {
		// override.. we don't support non-closeable
		return ((TabbedMDI) getMDI()).isMainMDI ? true : super.isCloseable();
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#setCloseable(boolean)
	 */
	@Override
	public void setCloseable(boolean closeable) {
		// override.. we don't support non-closeable for main
		if (((TabbedMDI) getMDI()).isMainMDI) {
			closeable = true;
		}
		super.setCloseable(closeable);
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (swtItem == null || swtItem.isDisposed()) {
					return;
				}
				swtItem.setShowClose(isCloseable());
			}
		});
	}

	public void setSwtItem(CTabItem swtItem) {
		this.swtItem = swtItem;
		if (swtItem == null) {
			setDisposed(true);
			return;
		}
		setDisposed(false);

		swtItem.addDisposeListener(this);
		String title = getTitle();
		if (title != null) {
			swtItem.setText(escapeAccelerators(title));
		}

		updateLeftImage();
		
		swtItem.setShowClose(isCloseable());

		if (buildonSWTItemSet) {
			build();
		}
		if (showonSWTItemSet) {
			show();
		}
	}

	public Item getSwtItem() {
		return swtItem;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		super.setTitle(title);

		if (swtItem != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (swtItem == null || swtItem.isDisposed()) {
						return;
					}
					swtItem.setText(escapeAccelerators(getTitle()));
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#getVitalityImages()
	 */
	public MdiEntryVitalityImage[] getVitalityImages() {
		return new MdiEntryVitalityImage[0];
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#close()
	 */
	public boolean close(boolean forceClose) {
    // triggerCloseListener
		if (!super.close(forceClose)) {
			return false;
		}

		Utils.execSWTThread(new Runnable() {
			public void run() {
				if (swtItem != null && !swtItem.isDisposed()) {
					// this will triggerCloseListeners
					swtItem.dispose();
					swtItem = null;
				}
			}
		});
		return true;
	}

	public void redraw() {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (swtItem == null || swtItem.isDisposed()) {
					return;
				}
				// recalculate the size of tab (in case indicator text changed)
				swtItem.getParent().notifyListeners(SWT.Resize, new Event());
				// redraw indicator text
				swtItem.getParent().redraw();
			}
		});
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#setImageLeftID(java.lang.String)
	public void setImageLeftID(String id) {
		super.setImageLeftID(id);
		updateLeftImage();
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#setImageLeft(org.eclipse.swt.graphics.Image)
	public void setImageLeft(Image imageLeft) {
		super.setImageLeft(imageLeft);
		updateLeftImage();
	}

	private void updateLeftImage() {
		if (swtItem == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (swtItem == null || swtItem.isDisposed()) {
					return;
				}
				Image image = getImageLeft(null);
				swtItem.setImage(image);
			}
		});
	}

	public void widgetDisposed(DisposeEvent e) {
		setSwtItem(null);

		triggerCloseListeners(!SWTThread.getInstance().isTerminated());
		
		try {
			setEventListener(null, false);
		} catch (UISWTViewEventCancelledException e1) {
		}

		SWTSkinObject so = getSkinObject();
		if (so != null) {
			setSkinObjectMaster(null);
			so.getSkin().removeSkinObject(so);
		}
		
		// delay saving of removing of auto-open flag.  If after the delay, we are 
		// still alive, it's assumed the user invoked the close, and we should
		// remove the auto-open flag
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				// even though execThreadLater will not run on close of app because
				// the display is disposed, do a double check of tree disposal just
				// in case.  We don't want to trigger close listeners or
				// remove autoopen parameters if the user is closing the app (as
				// opposed to closing  the sidebar)
				if (SWTThread.getInstance().isTerminated()) {
					return;
				}

				mdi.removeItem(TabbedEntry.this);
				mdi.removeEntryAutoOpen(id);
			}
		});
	}

	private String escapeAccelerators(String str) {
		if (str == null) {
			return (str);
		}

		return str.replaceAll("&", "&&");
	}
	
	public void expandTo() {
	}
	
	public void viewTitleInfoRefresh(ViewTitleInfo titleInfoToRefresh) {
		super.viewTitleInfoRefresh(titleInfoToRefresh);

		if (titleInfoToRefresh == null || this.viewTitleInfo != titleInfoToRefresh) {
			return;
		}
		if (isDisposed()) {
			return;
		}

		String newText = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
		if (newText != null) {
			setTitle(newText);
		} else {
			String titleID = getTitleID();
			if (titleID != null) {
				setTitleID(titleID);
			}
		}
		redraw();
	}
	
	// @see com.aelitis.azureus.ui.mdi.MdiEntry#isSelectable()
	public boolean isSelectable() {
		return true;
	}

	// @see com.aelitis.azureus.ui.mdi.MdiEntry#setSelectable(boolean)
	public void setSelectable(boolean selectable) {
	}

	// @see com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT#addListener(com.aelitis.azureus.ui.swt.mdi.MdiSWTMenuHackListener)
	public void addListener(MdiSWTMenuHackListener l) {
		// TODO Auto-generated method stub
	}

	// @see com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT#removeListener(com.aelitis.azureus.ui.swt.mdi.MdiSWTMenuHackListener)
	public void removeListener(MdiSWTMenuHackListener l) {
		// TODO Auto-generated method stub
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#setParentID(java.lang.String)
	public void setParentID(String id) {
		// Do not set
	}
	
	// @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#getParentID()
	public String getParentID() {
		return null;
	}
	
	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateImage#obfusticatedImage(org.eclipse.swt.graphics.Image)
	public Image obfusticatedImage(Image image) {
		Rectangle bounds = swtItem == null ? null : swtItem.getBounds();
		if ( bounds != null ){
			
			boolean isActive = swtItem.getParent().getSelection() == swtItem;
			boolean isHeaderVisible = swtItem.isShowing();

			Point location = Utils.getLocationRelativeToShell(swtItem.getParent());
	
			bounds.x += location.x;
			bounds.y += location.y;
			
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("image", image);
			map.put("obfuscateTitle", false);
			if (isActive) {
				triggerEvent(UISWTViewEvent.TYPE_OBFUSCATE, map);

				if (viewTitleInfo instanceof ObfusticateImage) {
					((ObfusticateImage) viewTitleInfo).obfusticatedImage(image);
				}
			}

			if (isHeaderVisible) {
  			if (viewTitleInfo instanceof ObfusticateTab) {
  				String header = ((ObfusticateTab) viewTitleInfo).getObfusticatedHeader();
  				if (header != null) {
  					UIDebugGenerator.obfusticateArea(image, bounds, header);
  				}
  			}
	
  			if (MapUtils.getMapBoolean(map, "obfuscateTitle", false)) {
  				UIDebugGenerator.obfusticateArea(image, bounds);
  			}
			}
		}

		return image;
	}
}
