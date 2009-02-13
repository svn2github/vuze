package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.*;

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
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
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

	private Label friendsLabel;
	private Label friendsCountLabel;
	
	private Label onlineFriendsLabel;

	private Label showHideButton;

	private Composite parent;

	private Composite content;

	private Composite shareWithAllPanel;

	private Label edit;

	private Label addFriends;

	private Label image;

	private Label text;

	private int toolbarHeight = 45;
	
	private Color friendsTextColor;
	private Color numberOfFriendsTextColor;
	private Color secondaryTextColor;
	private Color hoverTextColor;
	private Font boldFont;
	private Font friendsFont;

	private Listener hoverListener;
	private ImageLoader imageLoader;
	
	
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
		imageLoader = ImageLoader.getInstance();

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
		
		hoverListener = new Listener() {
			public void handleEvent(Event event) {
				Widget widget = event.widget;
				if(! (widget instanceof Control)) return;
				Control control = (Control) widget;
				
				switch (event.type) {
				case SWT.MouseEnter:
						control.setForeground(hoverTextColor);
					break;

				case SWT.MouseExit:
						control.setForeground(secondaryTextColor);
					break;
				}
			}
		};

		content = new Composite(parent, SWT.NONE);
		content.setBackgroundMode(SWT.INHERIT_DEFAULT);
		imageLoader.setBackgroundImage(content, "friends_bg");
		
		
		FontData[] datas = content.getFont().getFontData();
		
		for(int i = 0 ; i < datas.length ; i++) {
			datas[i].setStyle(SWT.BOLD);
			if(Constants.isOSX) {
				datas[i].setHeight(11);
			} else {
				datas[i].setHeight(8);
			}
		}
		
		boldFont = new Font(content.getDisplay(),datas);
		
		for(int i = 0 ; i < datas.length ; i++) {
			if(Constants.isOSX) {
				datas[i].setHeight(13);
			} else {
				datas[i].setHeight(10);
			}
		}
		
		friendsFont = new Font(content.getDisplay(),datas);
		
		friendsTextColor = new Color(content.getDisplay(),49,52,60);
		numberOfFriendsTextColor = new Color(content.getDisplay(),77,77,77);
		secondaryTextColor = new Color(content.getDisplay(),51,63,79);
		hoverTextColor = new Color(content.getDisplay(),42,63,113);
		
		FormData fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.height = 46;
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		content.setLayoutData(fd);

		content.setLayout(new FormLayout());

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
		toolbarHeight = parent.getSize().y - 1;

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
		FormData data;
		
		friendsLabel = new Label(content, SWT.NONE);
		friendsLabel.setFont(friendsFont);
		friendsLabel.setForeground(friendsTextColor);
		friendsLabel.setText(MessageText.getString("v3.buddies.friends"));
		
		friendsCountLabel = new Label(content, SWT.NONE);
		friendsCountLabel.setFont(friendsFont);
		friendsCountLabel.setForeground(numberOfFriendsTextColor);
		
		onlineFriendsLabel = new Label(content, SWT.NONE);
		onlineFriendsLabel.setFont(boldFont);
		onlineFriendsLabel.setForeground(secondaryTextColor);
		
		showHideButton = new Label(content, SWT.NONE);
		showHideButton.setData("over",new Boolean(false));
		Listener hoverBtnListener = new Listener() {
		public void handleEvent(Event event) {
			Boolean expandedB = (Boolean) showHideButton.getData("expanded");
			boolean isExpanded = expandedB != null ? expandedB.booleanValue() : true;
			
			switch (event.type) {
				case SWT.MouseEnter:
					showHideButton.setData("over",new Boolean(true));
					imageLoader.setLabelImage(showHideButton, isExpanded
								? "btn_collapse_over" : "btn_expand_over");
					break;
	
				case SWT.MouseExit:
					showHideButton.setData("over",new Boolean(false));
					imageLoader.setLabelImage(showHideButton, isExpanded
							? "btn_collapse" : "btn_expand");
					break;
			}
			}	
		};
		
		
		showHideButton.addListener(SWT.MouseEnter, hoverBtnListener);
		showHideButton.addListener(SWT.MouseExit, hoverBtnListener);
		
		
		data = new FormData();
		data.left = new FormAttachment(0,8);
		data.top = new FormAttachment(0,4);
		friendsLabel.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(friendsLabel,2);
		data.right = new FormAttachment(showHideButton,-5);
		data.top = new FormAttachment(0,4);
		friendsCountLabel.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(100,-8);
		data.top = new FormAttachment(0,10);
		showHideButton.setLayoutData(data);

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
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});
			}

			public void buddyChanged(VuzeBuddy buddy) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateFriendsLabel();
					}
				});
			}

		}, false);

		createToolItems();

		createSharePanel();

		data = new FormData();
		data.left = new FormAttachment(0,8);
		data.right = new FormAttachment(edit,-5);
		data.top = new FormAttachment(0,28);
		onlineFriendsLabel.setLayoutData(data);	
		
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

		edit = new Label(content, SWT.PUSH);
		edit.setFont(boldFont);
		edit.setForeground(secondaryTextColor);
		edit.setCursor(content.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		edit.addListener(SWT.MouseEnter, hoverListener);
		edit.addListener(SWT.MouseExit, hoverListener);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				if (new Boolean(false).equals(edit.getData("edit_mode"))) {
					setEditMode();
				} else {
					reset();
				}
			}
		});
		
		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info, boolean isNewLoginID) {
				if (null == info.userName) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							edit.setText(MessageText.getString("Button.bar.edit"));
							edit.setData("selection", new Boolean(false));
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

		addFriends = new Label(content, SWT.NONE);
		addFriends.setFont(boldFont);
		addFriends.setForeground(secondaryTextColor);
		addFriends.setCursor(content.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		addFriends.addListener(SWT.MouseEnter, hoverListener);
		addFriends.addListener(SWT.MouseExit, hoverListener);
		addFriends.setText(MessageText.getString("Button.bar.add"));
		addFriends.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				addBuddy();
			}
		});
		
		Label separator = new Label(content,SWT.NONE);
		separator.setFont(boldFont);
		separator.setForeground(secondaryTextColor);
		separator.setText("/");
		
		FormData data;
		
		data = new FormData();
		data.right = new FormAttachment(100,-8);
		data.top = new FormAttachment(0,28);
		addFriends.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(addFriends,-1);
		data.top = new FormAttachment(0,28);
		separator.setLayoutData(data);
		
		
		data = new FormData();
		data.right = new FormAttachment(separator,-1);
		data.top = new FormAttachment(0,28);
		edit.setLayoutData(data);
		
		
	}

	private void createSharePanel() {
		/*
		 * This panel is initially not visible; it will be made visible as needed
		 */
		shareWithAllPanel = new Composite(content, SWT.NONE);
		shareWithAllPanel.setVisible(false);
		
		FormData data ;
		data = new FormData();
		data.left = new FormAttachment(0,8);
		data.top = new FormAttachment(0,29);
		data.right = new FormAttachment(edit,-5);
		//data.bottom = new FormAttachment(100,-1);
		shareWithAllPanel.setLayoutData(data);
		
		shareWithAllPanel.setLayout(new FormLayout());

		image = new Label(shareWithAllPanel, SWT.NONE);
		imageLoader.setLabelImage(image, "add_to_share");
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.top = new FormAttachment(0,0);
		image.setLayoutData(data);

		text = new Label(shareWithAllPanel, SWT.NONE);
		text.setFont(boldFont);
		text.setForeground(secondaryTextColor);
		text.setText(MessageText.getString("v3.Share.add.buddy.all"));
		
		
		data = new FormData();
		data.left = new FormAttachment(image,5);
		data.top = new FormAttachment(0,0);
		text.setLayoutData(data);
		

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
				imageLoader.setLabelImage(image, "add_to_share_selected");
				viewer.addAllToShare();

			} else {
				imageLoader.setLabelImage(image, "add_to_share");
				viewer.removeAllFromShare();
			}
		}
	}

	private void updateFriendsLabel() {
		
		if (friendsCountLabel.isDisposed()) {
			return;
		}
		
		List buddies = VuzeBuddyManager.getAllVuzeBuddies();
		
		int onlineBuddies = 0;
		for(int i = 0 ; i < buddies.size() ; i++) {
			VuzeBuddy buddy = (VuzeBuddy) buddies.get(i);
			if(buddy.isOnline(true)) onlineBuddies++;
		}
		
		friendsCountLabel.setText("(" + buddies.size() + ")");
		
		onlineFriendsLabel.setText(MessageText.getString("v3.buddies.online",
				
				new String[] {
					onlineBuddies + ""
				}));

		/*
		 * The text could be a different length now so do a relayout just in case
		 */
		content.layout(true);
	}
	
	public void addBuddy(final String message) {
		if (!VuzeBuddyManager.isEnabled()) {
			VuzeBuddyManager.showDisabledDialog();
			return;
		}

		SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
			public void loginComplete() {
				_addBuddy(message);
			}

		});
	}
	
	public void addBuddy() {
		addBuddy(null);
	}

	protected void _addBuddy(final String message) {
		addFriends.setEnabled(false);
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
					AddFriendsPage add = new AddFriendsPage(this);
					if(message != null) {
						add.setMessage(message);
					}
					addPage(add);
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
		onlineFriendsLabel.setVisible(true);
		addFriends.setEnabled(true);
		friendsLabel.setEnabled(true);
		showHideButton.setEnabled(true);
		edit.setEnabled(true);
		edit.setText(MessageText.getString("Button.bar.edit"));
		edit.setData("edit_mode",new Boolean(false));

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		viewer.setMode(BuddiesViewer.none_active_mode);

		shareWithAllPanel.setVisible(false);
		content.layout(true);

	}

	public void setShareMode() {
		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		if (null == viewer) {
			return;
		}
		
		viewer.setMode(BuddiesViewer.share_mode);
		addFriends.setEnabled(false);
		onlineFriendsLabel.setVisible(false);
		showHideButton.setEnabled(false);
		edit.setEnabled(false);

		shareWithAllPanel.setVisible(true);
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
				showHideButton.setEnabled(false);
				edit.setEnabled(false);

				shareWithAllPanel.setVisible(true);
			}

			public long getCancelDelay() {
				return 0;
			}

			public void loginCanceled() {
				reset();
			}

		});

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

				
				edit.setData("edit_mode",new Boolean(true));
				
				edit.setText(MessageText.getString("Button.bar.edit.cancel"));
				edit.setEnabled(true);
				addFriends.setEnabled(false);
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
		showHideButton.setData("expanded",new Boolean(isExpanded));
		
		Boolean overB = (Boolean) showHideButton.getData("over");
		boolean isOver = overB != null ? overB.booleanValue() : false;
		
		if(isOver) {
			imageLoader.setLabelImage(showHideButton, isExpanded
					? "btn_collapse_over" : "btn_expand_over");
		} else {
			imageLoader.setLabelImage(showHideButton, isExpanded
					? "btn_collapse" : "btn_expand");
		}

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

					soSash.setVisible(true);
					soSash.setPercent((double) COConfigurationManager.getFloatParameter("Friends.sash.percent"));

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
			/*
			 * Hide the sash if the Friends viewer is not visible
			 */
			soSash.setVisible(false);
			soSash.setBelowPX(toolbarHeight);
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
