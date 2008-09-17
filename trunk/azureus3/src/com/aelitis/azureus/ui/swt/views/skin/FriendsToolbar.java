package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.DockPosition;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;
import org.gudy.azureus2.ui.swt.shells.Offset;
import org.gudy.azureus2.ui.swt.shells.ShellDocker;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyListener;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.friends.AddFriendsPage;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectSash;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

/**
 * A simple toolbar for the Friends section in the left navigation
 * @author khai
 *
 */
public class FriendsToolbar
	extends SkinView
{
	private ToolBar toolbar;

	private Label friendsLabel;

	private Label showHideButton;

	private Composite parent;

	private Composite content;

	private Composite shareWithAllPanel;

	private ToolItem edit;

	private ToolItem addFriends;

	private Label image;

	private Label text;

	private int toolbarHeight = 30;

	static {
		ImageRepository.addPath("com/aelitis/azureus/ui/images/torrent_down.png",
				"button_collapse");
		ImageRepository.addPath("com/aelitis/azureus/ui/images/torrent_up.png",
				"button_expand");
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/buddy_add_to_share.png", "add_to_share");
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/buddy_add_to_share_selected.png",
				"add_to_share_selected");
	}

	public FriendsToolbar() {
	}

	public FriendsToolbar(Composite parent) {
		this.parent = parent;
		init();
	}

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		parent = (Composite) skinObject.getControl();

		init();
		return null;
	}

	private void init() {
		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException("Parent cannot be null or disposed");
		}

		Layout parentLayout = parent.getLayout();
		if (null == parentLayout) {
			parentLayout = new FormLayout();
			parent.setLayout(parentLayout);

		} else if (false == (parentLayout instanceof FormLayout)) {
			throw new IllegalArgumentException(
					"Oops! We can not handle any layout other than FormLayout at the moment!!!");
		}

		content = new Composite(parent, SWT.NONE);

		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		content.setLayoutData(fd);

		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = 3;
		layout.marginWidth = 3;
		content.setLayout(layout);

		createControls();

		/*
		 * KN: Not sure why this must be done at the 'parent' level but without it
		 * the toolbar is not visible at startup
		 */
		parent.getParent().layout(true, true);

		/*
		 * This height is used to dynamically calculate the percentage of the sash
		 * the toolbar will occupy
		 */
		toolbarHeight = parent.getSize().y;

		/*
		 * When the sidebar is resized we need to recalculate the percentage for the toolbar again
		 */
		SWTSkinObject soSidebar = skin.getSkinObject(SkinConstants.VIEWID_SIDEBAR);
		if (null != soSidebar) {
			soSidebar.getControl().addListener(SWT.Resize, new Listener() {
				public void handleEvent(Event event) {
					/*
					 * This is only applicable when the Friends are not visible because when the 
					 * Friends are visible the percentage for the toolbar is not in use
					 */
					if (false == COConfigurationManager.getBooleanParameter("Friends.visible")) {
						collapseToToolbar();
					}
				}
			});
		}
	}

	private void createControls() {
		friendsLabel = new Label(content, SWT.NONE);
		friendsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false));

		hookTuxGoodies(friendsLabel);

		/*
		 * Initial Friends count
		 */
		updateFriendsLabel();

		/*
		 * Update when the number of Friends changes
		 */
		VuzeBuddyManager.addListener(new VuzeBuddyListener() {

			public void buddyRemoved(VuzeBuddy buddy) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});

			}

			public void buddyAdded(VuzeBuddy buddy, int position) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});
			}

			public void buddyOrderChanged() {
			}

			public void buddyChanged(VuzeBuddy buddy) {
			}

		}, false);

		createSharePanel();

		toolbar = new ToolBar(content, SWT.HORIZONTAL);
		toolbar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		createToolItems();

		showHideButton = new Label(content, SWT.NONE);
		showHideButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

		/*
		 * Initial state from configuration
		 */
		boolean isVisible = COConfigurationManager.getBooleanParameter("Friends.visible");
		SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash-bottom");
		if (null != soSash) {
			/*
			 * If it's visible then it may have a user-defined height so remember it;
			 * we will use it later when the user expands the Friends area after a collapse
			 */
			if (true == isVisible) {

				COConfigurationManager.setParameter("Friends.sash.percent",
						(float) soSash.getPercent());

			} else {

				collapseToToolbar();

			}
		}
		showFooterToggleButton(isVisible);

		/*
		 * Change button display if the property changes
		 */
		COConfigurationManager.addParameterListener("Friends.visible",
				new ParameterListener() {

					public void parameterChanged(String parameterName) {
						boolean isVisible = COConfigurationManager.getBooleanParameter("Friends.visible");
						showFooterToggleButton(isVisible);
						showFriends(isVisible);
					}
				});

		showHideButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				boolean wasExpanded = COConfigurationManager.getBooleanParameter("Friends.visible");
				COConfigurationManager.setParameter("Friends.visible", !wasExpanded);
			}
		});

	}

	private void createToolItems() {

		edit = new ToolItem(toolbar, SWT.CHECK);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				if (true == edit.getSelection()) {
					setEditMode();
				} else {
					reset();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				if (null == info.userName) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							edit.setText(MessageText.getString("Button.bar.edit"));
							edit.setSelection(false);
							reset();
							content.layout(true);
						}
					});

				}
			}
			
			// @see com.aelitis.azureus.util.ILoginInfoListener#avatarURLUpdated()
			public void avatarURLUpdated(String newAvatarURL) {
			}
		});

		addFriends = new ToolItem(toolbar, SWT.PUSH);
		addFriends.setText("Add");
		addFriends.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				addBuddy();

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void createSharePanel() {
		/*
		 * This panel is initially not visible; it will be made visible as needed
		 */
		shareWithAllPanel = new Composite(content, SWT.NONE);
		shareWithAllPanel.setVisible(false);

		GridData gData = new GridData(SWT.END, SWT.CENTER, false, false);
		gData.widthHint = 100;
		gData.exclude = true;
		shareWithAllPanel.setLayoutData(gData);
		shareWithAllPanel.setLayout(new GridLayout(2, false));

		image = new Label(shareWithAllPanel, SWT.NONE);
		image.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		image.setImage(ImageRepository.getImage("add_to_share"));
		text = new Label(shareWithAllPanel, SWT.NONE);
		text.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		text.setText(MessageText.getString("v3.Share.add.buddy.all"));

		hookShareListener();
	}

	private void hookShareListener() {
		MouseAdapter listener = new MouseAdapter() {
			boolean shareWithAll = false;

			public void mouseUp(MouseEvent e) {
				shareWithAll = !shareWithAll;
				shareAllBuddies(shareWithAll);
			}

		};

		shareWithAllPanel.addMouseListener(listener);
		image.addMouseListener(listener);
		text.addMouseListener(listener);
	}

	public void enableShareButton(boolean value) {
		shareWithAllPanel.setEnabled(value);
		image.setEnabled(value);
		text.setEnabled(value);

		if (false == value) {
			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
			if (null != viewer) {
				viewer.setMode(BuddiesViewer.disabled_mode);
			}
		} else {
			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
			if (null != viewer) {
				viewer.setMode(BuddiesViewer.share_mode);
			}
		}
	}

	protected void shareAllBuddies(boolean value) {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null != viewer) {
			if (true == value) {
				image.setImage(ImageRepository.getImage("add_to_share_selected"));
				viewer.addAllToShare();

			} else {
				image.setImage(ImageRepository.getImage("add_to_share"));
				viewer.removeAllFromShare();
			}
		}
	}

	private void updateFriendsLabel() {
		friendsLabel.setText(MessageText.getString("v3.buddies.count",
				new String[] {
					VuzeBuddyManager.getAllVuzeBuddies().size() + ""
				}));

		/*
		 * The text could be a different length now so do a relayout just in case
		 */
		content.layout(true);
	}
	
	public void addBuddy() {
		if (!VuzeBuddyManager.isEnabled()) {
			VuzeBuddyManager.showDisabledDialog();
			return;
		}

		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				_addBuddy();
			}

		});
	}

	protected void _addBuddy() {
		addFriends.setEnabled(false);
		friendsLabel.setEnabled(false);
		showHideButton.setEnabled(false);
		edit.setEnabled(false);

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.add_buddy_mode);

//		SkinView detailPanelView = SkinViewManager.getByClass(DetailPanel.class);
//		if (detailPanelView instanceof DetailPanel) {
//			DetailPanel detailPanel = ((DetailPanel) detailPanelView);
//			detailPanel.show(true, InvitePage.PAGE_ID);
		//KN: Work in progress for new SHare Wizard			
		SWTSkinObject soSidebar = skin.getSkinObject("sidebar-sash");//SkinConstants.VIEWID_SIDEBAR);
		if (null != soSidebar) {
			MultipageWizard shell = new MultipageWizard(
					UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell(),
					SWT.DIALOG_TRIM | SWT.RESIZE) {
				public void createPages() {
					addPage(new AddFriendsPage(this,true));
				}
			};
			shell.setText("Vuze - Wizard");
			shell.setSize(500, 550);

			/*
			 * TODO: below is the 2 possible ways to open this shell; must pick one before the product is release
			 */
			boolean useDocker = false;

			if (true == useDocker) {

				/*
				 * Use a shelldocker to 'dock' the shell; this is currently configured to dock on the right
				 * side of the main vertical sash.  Notice that if you move of resize the main application
				 * the docking behavior is adjusted accordingly
				 */
				ShellDocker docker = new ShellDocker(soSidebar.getControl(),
						shell.getShell());
				docker.setAnchorControlPosition(new DockPosition(
						DockPosition.BOTTOM_RIGHT, new Offset(1, -shell.getSize().y)));
				docker.openShell(true, false);

			} else {

				/*
				 * Opens a centered free-floating shell
				 */

				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (null == uiFunctions) {
					/*
					 * Centers on the active monitor
					 */
					Utils.centreWindow(shell.getShell());
				} else {
					/*
					 * Centers on the main application window
					 */
					Utils.centerWindowRelativeTo(shell.getShell(),
							uiFunctions.getMainShell());
				}

				shell.open();
			}
		}
	}

	public void reset() {
		addFriends.setEnabled(true);
		friendsLabel.setEnabled(true);
		showHideButton.setEnabled(true);
		edit.setEnabled(true);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.setSelection(false);

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.none_active_mode);

		showAddWithAll(false);
		content.layout(true);

	}

	public void setShareMode() {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.share_mode);
		addFriends.setEnabled(false);
		friendsLabel.setEnabled(false);
		showHideButton.setEnabled(false);
		edit.setEnabled(false);

		showAddWithAll(true);
		content.layout(true);

		COConfigurationManager.setParameter("Friends.visible", true);

	}

	public void setAddFriendsMode() {

		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {

			public void loginComplete() {
				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null == viewer) {
					return;
				}
				viewer.setMode(BuddiesViewer.share_mode);
				addFriends.setEnabled(false);
				friendsLabel.setEnabled(false);
				showHideButton.setEnabled(false);
				edit.setEnabled(false);

				showAddWithAll(true);
			}

			public long getCancelDelay() {
				return 0;
			}

			public void loginCanceled() {
				reset();
			}

		});

	}

	private void showAddWithAll(boolean value) {
		GridData gData = (GridData) shareWithAllPanel.getLayoutData();
		gData.exclude = !value;
		shareWithAllPanel.setVisible(value);
	}

	public void setEditMode() {
		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {

			public void loginComplete() {
				COConfigurationManager.setParameter("Friends.visible", true);
				BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
				if (null == viewer) {
					return;
				}
				viewer.setMode(BuddiesViewer.edit_mode);

				edit.setText(MessageText.getString("Button.bar.edit.cancel"));
				edit.setEnabled(true);
				addFriends.setEnabled(false);
				friendsLabel.setEnabled(false);
				showHideButton.setEnabled(false);
				content.layout(true);
			}

			public long getCancelDelay() {
				return 0;
			}

			public void loginCanceled() {
				reset();
			}

		});
	}

	/**
	 * Toggles between showing the 'collapse' or 'expand' buttons
	 * @param value
	 */
	private void showFooterToggleButton(boolean isExpanded) {
		showHideButton.setImage(isExpanded
				? ImageRepository.getImage("button_collapse")
				: ImageRepository.getImage("button_expand"));

	}

	/**
	 * Here we collapse or expand the bottom sash of the sidebar
	 * @param isExpanded
	 */
	public void showFriends(final boolean isExpanded) {

		/*
		 * To hide a sash we typically set it's percentage to 0 but since this bottom
		 * sash has a toolbar that we want to always be visible we use the toolbar's height
		 * then calculate that as a percentage of the total height of the side bar; we use this 
		 * as the percentage when 'collapsed'
		 * 
		 * Additionally when collapsed we make the sash invisible and turn it back to visible when
		 * it's expanded.
		 */
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash-bottom");

				if (true == isExpanded) {

					soSash.setPercent((double) COConfigurationManager.getFloatParameter("Friends.sash.percent"));
					soSash.setVisible(true);

				} else {

					COConfigurationManager.setParameter("Friends.sash.percent",
							(float) soSash.getPercent());

					collapseToToolbar();

				}
			}
		});
	}

	private void collapseToToolbar() {

		SWTSkinObject soSidebar = skin.getSkinObject(SkinConstants.VIEWID_SIDEBAR);
		if (null != soSidebar) {
			SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash-bottom");
			double minPercent = (double) toolbarHeight
					/ (double) soSidebar.getControl().getSize().y;
			soSash.setPercent(minPercent);
			/*
			 * Hide the sash if the Friends viewer is not visible
			 */
			soSash.setVisible(false);
		}
	}

	private void hookTuxGoodies(Control control) {
		if (!org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			return;
		}
		Menu menu = new Menu(control);
		MenuItem menuItem;
		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText("buddy sync up");
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!LoginInfoManager.getInstance().isLoggedIn()) {
					Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
							"not logged in. no can do");
					return;
				}
				try {
					PlatformRelayMessenger.fetch(0);
					PlatformBuddyMessenger.sync(null);
					PlatformBuddyMessenger.getInvites();
				} catch (NotLoggedInException e1) {
				}
			}
		});

		menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText("send msg to all buddies");
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!LoginInfoManager.getInstance().isLoggedIn()) {
					Utils.openMessageBox(null, SWT.ICON_ERROR, "No",
							"not logged in. no can do");
					return;
				}
				InputShell is = new InputShell("Moo", "Message:");
				String txt = is.open();
				if (txt != null) {
					VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
							SystemTime.getCurrentTime(), txt, "Test");
					List buddies = VuzeBuddyManager.getAllVuzeBuddies();
					for (Iterator iter = buddies.iterator(); iter.hasNext();) {
						VuzeBuddy buddy = (VuzeBuddy) iter.next();
						System.out.println("sending to " + buddy.getDisplayName());
						try {
							buddy.sendActivity(entry);
						} catch (NotLoggedInException e1) {
							Debug.out("Shouldn't Happen", e1);
						}
					}
				}
			}
		});

		control.setMenu(menu);
	}

}
