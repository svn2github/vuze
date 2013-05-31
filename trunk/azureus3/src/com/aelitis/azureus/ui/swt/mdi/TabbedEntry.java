package com.aelitis.azureus.ui.swt.mdi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;

public class TabbedEntry
	extends BaseMdiEntry implements DisposeListener
{
	private CTabItem swtItem;

	private SWTSkin skin;

	private boolean showonSWTItemSet;

	private boolean buildonSWTItemSet;

	private static long uniqueNumber = 0;

	public TabbedEntry(TabbedMDI mdi, SWTSkin skin, String id) {
		super(mdi, id);
		this.skin = skin;
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
		if (control == null) {
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
					setSkinObject(skinObject, skinObject);
				} finally {
					shell.setCursor(cursor);
				}
			} else if (view != null) {
				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, "mdi.content.item",
							soParent);

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					Composite viewComposite = soContents.getComposite();
					//viewComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
					//viewComposite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

					boolean doGridLayout = true;
					UISWTView swtView = view;
					if (swtView.getControlType() == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
						doGridLayout = false;
					}
					if (doGridLayout) {
  					GridLayout gridLayout = new GridLayout();
  					gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
  					viewComposite.setLayout(gridLayout);
  					viewComposite.setLayoutData(Utils.getFilledFormData());
					}

					UISWTViewCore uiViewCore = view;
					uiViewCore.setSkinObject(soContents, soContents.getComposite());

					view.initialize(viewComposite);
					setTitle(view.getFullTitle());

					Composite iviewComposite = view.getComposite();
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

					//soContents is invisible, so of course iviwComposite is invisible
					//We should do the one time layout on the first show..
					//if (iviewComposite.isVisible()) {
					//	parent.layout(true, true);
					//}

					CTabItem oldSelection = swtItem.getParent().getSelection();
					swtItem.getParent().setSelection(swtItem);
					swtItem.setControl(soContents.getControl());
					if (oldSelection != null) {
						swtItem.getParent().setSelection(oldSelection);
					}
					setSkinObject(soContents, soContents);
				} catch (Exception e) {
					Debug.out("Error creating sidebar content area for " + id, e);
					close(true);
				}

			} else if (viewClass != null) {
				try {
					UISWTViewCore view = (UISWTViewCore) viewClass.newInstance();

					if (view != null) {
						setCoreView(view);
						// now that we have an view, go through show one more time
						return swt_build();
					}
					close(true);
					return false;
				} catch (Exception e) {
					Debug.out(e);
					close(true);
				}
			}

			if (control != null && !control.isDisposed()) {
				control.setData("BaseMDIEntry", this);
				control.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						close(true);
					}
				});
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

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#isCloseable()
	 */
	public boolean isCloseable() {
		// override.. we don't support non-closeable
		return true;
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
		} else if (viewClass != null) {
			swtItem.setText(viewClass.getSimpleName());
		}
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
			swtItem.setText(escapeAccelerators(title));
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
					swtItem.dispose();
					swtItem = null;
				} else if (view != null) {
					setCoreView(null);

					triggerCloseListeners(!SWTThread.getInstance().isTerminated());
				}
			}
		});
		return true;
	}

	public void redraw() {
	}

	public void widgetDisposed(DisposeEvent e) {
		setSwtItem(null);

		triggerCloseListeners(!SWTThread.getInstance().isTerminated());

		UISWTViewCore view = getCoreView();
		if (view != null) {
			setCoreView(null);
		}
		SWTSkinObject so = getSkinObject();
		if (so != null) {
			setSkinObject(null, null);
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

				try {
					COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
				} catch (Exception e2) {
					Debug.out(e2);
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

		String textIndicator = null;
		try {
			textIndicator = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
		} catch (Exception e) {
			Debug.out(e);
		}
		if (textIndicator != null) {
			setPullTitleFromView(false);
		}
		
		String newText = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
		if (newText != null) {
			if (textIndicator != null) {
				newText += " (" + textIndicator + ")";
			}
			setPullTitleFromView(false);
			setTitle(newText);
		} else if (view != null) {
			newText = view.getFullTitle();
			if (textIndicator != null) {
				newText += " (" + textIndicator + ")";
			}
			setTitle(newText);
		}
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
}
