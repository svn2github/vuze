package com.aelitis.azureus.ui.swt.mdi;

import java.lang.reflect.Constructor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.IViewExtension;

import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;

import org.gudy.azureus2.plugins.download.Download;

public class TabbedEntry
	extends BaseMdiEntry implements DisposeListener
{
	private CTabItem swtItem;

	private SWTSkin skin;

	private boolean showonSWTItemSet;

	private static long uniqueNumber = 0;

	public TabbedEntry(TabbedMDI mdi, SWTSkin skin, String id) {
		super(mdi, id);
		this.skin = skin;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry#show()
	 * 
	 * @note SideBarEntrySWT is neary identical to this one.  Please keep them
	 *       in sync until commonalities are placed in BaseMdiEntry
	 */
	public void show() {
		if (swtItem == null) {
			showonSWTItemSet = true;
			return;
		}
		showonSWTItemSet = false;
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
							soParent, getSkinRefParams());
					
					control = skinObject.getControl();
					control.setLayoutData(Utils.getFilledFormData());
					control.getParent().layout(true);
					// swtItem.setControl will set the control's visibility based on
					// whether the control is selected.  To ensure it doesn't set
					// our control invisible, set selection now
					swtItem.getParent().setSelection(swtItem);
					swtItem.setControl(control);
					setSkinObject(skinObject);
				} finally {
					shell.setCursor(cursor);
				}
			} else if (iview != null) {
				try {
					SWTSkinObjectContainer soContents = (SWTSkinObjectContainer) skin.createSkinObject(
							"MdiIView." + uniqueNumber++, "mdi.content.item",
							soParent);
					skin.addSkinObject(soContents);

					parent.setBackgroundMode(SWT.INHERIT_NONE);

					Composite viewComposite = soContents.getComposite();
					viewComposite.setBackground(parent.getDisplay().getSystemColor(
							SWT.COLOR_WIDGET_BACKGROUND));
					viewComposite.setForeground(parent.getDisplay().getSystemColor(
							SWT.COLOR_WIDGET_FOREGROUND));
					GridLayout gridLayout = new GridLayout();
					gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
					viewComposite.setLayout(gridLayout);
					viewComposite.setLayoutData(Utils.getFilledFormData());

					iview.initialize(viewComposite);
					setTitle(iview.getFullTitle());

					Composite iviewComposite = iview.getComposite();
					control = iviewComposite;
					Object existingLayout = iviewComposite.getLayoutData();
					if (existingLayout == null || (existingLayout instanceof GridData)) {
						GridData gridData = new GridData(GridData.FILL_BOTH);
						iviewComposite.setLayoutData(gridData);
					}

					if (iviewComposite.isVisible()) {
						parent.layout(true, true);
					}

					swtItem.getParent().setSelection(swtItem);
					swtItem.setControl(soContents.getControl());
					setSkinObject(soContents);
				} catch (Exception e) {
					Debug.out("Error creating sidebar content area for " + id, e);
					close(true);
				}

			} else if (iviewClass != null) {
				try {
					IView view = null;
					if (iviewClassArgs == null) {
						view = (IView) iviewClass.newInstance();
					} else {
						Constructor<?> constructor = iviewClass.getConstructor(iviewClassArgs);
						view = (IView) constructor.newInstance(iviewClassVals);
					}

					if (view != null) {
						setIView(view);
						// now that we have an IView, go through show one more time
						show();
					} else {
						close(true);
					}
					return;
				} catch (Exception e) {
					Debug.out(e);
					close(true);
				}
			}

			if (control != null) {
				control.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						close(true);
					}
				});
			}
		}

		if (swtItem.getParent().getSelection() != swtItem) {
			swtItem.getParent().setSelection(swtItem);
		}

		if (control != null) {
			if (iview instanceof ToolBarEnabler) {
				ISelectedContent[] sels = new ISelectedContent[1];
				sels[0] = new ToolBarEnablerSelectedContent((ToolBarEnabler) iview);
				TableView<?> tv = null;
				if (iview instanceof TableView<?>) {
					tv = (TableView<?>) iview;
				}
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, tv);

			} else {

				SelectedContentManager.clearCurrentlySelectedContent();

			}

			disableViewModes();
			
			UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uif != null) {
				//uif.refreshIconBar(); // needed?
				uif.refreshTorrentMenu();
			}

			// bit of hackery to change currently selected content when
			// moving to an iview that has Download(Manager) as a datasource
			// Unsure if needed as view activation should take care of this..
			if (iview instanceof UISWTViewImpl) {
				Object ds = ((UISWTViewImpl) iview).getDataSource();
				DownloadManager dm = null;
				if (ds instanceof DownloadManager) {
					dm = (DownloadManager) ds;
				} else if (ds instanceof Download) {
					dm = PluginCoreUtils.unwrap((Download) ds);
				}
				if (dm != null) {
					try {
						TableView<?> tv = null;
						if (iview instanceof TableView<?>) {
							tv = (TableView<?>) iview;
						}
						SelectedContentManager.changeCurrentlySelectedContent(id,
								new ISelectedContent[] {
									new SelectedContentV3(dm)
								}, tv);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			SWTSkinObject skinObject = getSkinObject();
			if (skinObject instanceof SWTSkinObjectContainer) {
				SWTSkinObjectContainer container = (SWTSkinObjectContainer) skinObject;
				//container.setVisible(true);
				Composite composite = container.getComposite();
				if (composite != null && !composite.isDisposed()) {
					composite.setVisible(true);
					composite.moveAbove(null);
					//composite.setFocus();
					//container.getParent().relayout();
					composite.getParent().layout();
				}
				// This causes double show because createSkinObject already calls show
				//container.triggerListeners(SWTSkinObjectListener.EVENT_SHOW);
			}
			if (iview != null) {
				Composite c = iview.getComposite();
				if (c != null && !c.isDisposed()) {
					c.setVisible(true);
					c.getParent().layout();
				}
			}

			if (iview instanceof IViewExtension) {
				try {
					((IViewExtension) iview).viewActivated();
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
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
		} else if (iviewClass != null) {
			swtItem.setText(iviewClass.getSimpleName());
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
				} else if (iview != null) {
					iview.delete();
					iview = null;

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

		IView iview = getIView();
		if (iview != null) {
			setIView(null);
			iview.delete();
		}
		SWTSkinObject so = getSkinObject();
		if (so != null) {
			setSkinObject(null);
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
				mdi.setEntryAutoOpen(id, false);
			}
		});
	}

	private String escapeAccelerators(String str) {
		if (str == null) {
			return (str);
		}

		return str.replaceAll("&", "&&");
	}
}
