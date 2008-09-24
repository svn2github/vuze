package com.aelitis.azureus.ui.swt.shells.friends;

import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.shells.AbstractWizardPage;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractStatusListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.shells.MessageWindow;
import com.aelitis.azureus.ui.swt.shells.StyledMessageWindow;
import com.aelitis.azureus.ui.swt.views.skin.FriendsToolbar;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.util.Constants;

public class AddFriendsPage
	extends AbstractWizardPage
{
	public static final String ID = "add.friends.wizard.page";

	public static final String BUTTON_PREVIEW = "button.preview";

	public static final String BUTTON_CONTINUE = "button.continue";
	
	

	private Composite content;

	private Browser browser;

	private BrowserContext context;

	private AbstractBuddyPageListener buddyPageListener;

	private FriendsToolbar friendsToolbar;

	private boolean previewMode = false;
	
	private boolean isStandalone;
	
	private SharePage sharePage;

	public AddFriendsPage(MultipageWizard wizard) {
		this(wizard,null);
	}
	
	public AddFriendsPage(MultipageWizard wizard,SharePage sharePage) {
		super(wizard);
		this.sharePage = sharePage;
		this.isStandalone = sharePage == null;
	}

	public Composite createControls(Composite parent) {
		if(isStandalone) {
			getWizard().getShell().addListener(SWT.Dispose, new Listener() {
				public void handleEvent(Event arg0) {
					friendsToolbar.reset();
				}
			});
		}
		
		content = super.createControls(parent);
		content.setLayout(new FillLayout());

		friendsToolbar = (FriendsToolbar) SkinViewManager.getByClass(FriendsToolbar.class);
		Color bg = parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		int red = bg.getRed();
		int blue = bg.getBlue();
		int green = bg.getGreen();
		
		System.out.println(bg);
		browser = new Browser(content, SWT.NONE);
		String url = Constants.URL_PREFIX + "/user/AddFriend.html?ts=" + Math.random() + "&bg=";
		browser.setUrl(url);

		getMessageContext();

		return content;
	}

	public String getPageID() {
		return ID;
	}

	public String getDesciption() {
		return MessageText.getString("v3.AddFriends.header.message");
	}

	public String getTitle() {
		return MessageText.getString("v3.AddFriends.header");
	}

	public String getWindowTitle() {
		return MessageText.getString("v3.AddFriends.wizard.title");
	}

	protected void createButtons(Composite buttonPanel) {
		createButton(BUTTON_CANCEL, isStandalone ? MessageText.getString("Button.cancel") : MessageText.getString("wizard.previous"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						if(previewMode && !isStandalone) {
							previewMode = false;
							context.executeInBrowser("previewCancel()");
							showButton(BUTTON_PREVIEW, true);
						} else {
							if(isStandalone) {
								getWizard().close();
							} else {
								getWizard().showPage(SharePage.ID);
							}
						}
						
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
		
		
		createButton(BUTTON_PREVIEW, MessageText.getString("Button.preview"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						previewMode = true;
						context.executeInBrowser("preview()");
						showButton(BUTTON_PREVIEW, false);
						if(isStandalone) {
							showButton(BUTTON_BACK, true);
							enableButton(BUTTON_BACK, true);
						}
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
		enableButton(BUTTON_PREVIEW, false);
		
		//Back for canceling the preview : only used in standalone mode
		if(isStandalone) {
			createButton(BUTTON_BACK, MessageText.getString("Button.back"),
					new SelectionListener() {
						public void widgetSelected(SelectionEvent e) {
							context.executeInBrowser("previewCancel()");
							showButton(BUTTON_BACK, false);
							showButton(BUTTON_PREVIEW, true);
						}
	
						public void widgetDefaultSelected(SelectionEvent e) {
						}
					});
			showButton(BUTTON_BACK, false);
		}
		
		
		createButton(BUTTON_OK, isStandalone ? MessageText.getString("Button.send") : MessageText.getString("Button.continue"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						context.executeInBrowser("inviteSubmit()");
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
		enableButton(BUTTON_OK, !isStandalone);
		
	}

	public Browser getBrowser() {
		return browser;
	}

	public boolean isInitOnStartup() {
		return true;
	}

	public synchronized ClientMessageContext getMessageContext() {
		if (null == context) {
			context = new BrowserContext(
					"buddy-page-listener-invite" + Math.random(), getBrowser(), null,
					true);

			context.addMessageListener(new DisplayListener(getBrowser()));

			/*
			 * Add listener to call the 'inviteFromShare' script; this listener is only called
			 * once whenever a web page is loaded the first time or when it's refreshed
			 */
			context.addMessageListener(new AbstractStatusListener("status") {
				public void handlePageLoadCompleted() {
					/*
					 * Setting inviteFromShare to false in the browser
					 */
					context.executeInBrowser("inviteFromShare(" + !isStandalone + ")");
					context.removeMessageListener(this);
				}
			});
			/*
			 * Add the appropriate messaging listeners
			 */

			buddyPageListener = new AbstractBuddyPageListener(getBrowser()) {

				public void handleCancel() {
					if (null != friendsToolbar) {
						friendsToolbar.reset();
					}
					if (true == isStandalone()) {
						getWizard().close();
					} else {
						getWizard().performBack();
					}

				}

				public void handleClose() {

					if (null != friendsToolbar) {
						friendsToolbar.reset();
					}
					if (true == isStandalone()) {
						getWizard().close();
					} else {
						getWizard().performBack();
					}

				}

				public void handleBuddyInvites() {

					if(sharePage != null) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								sharePage.inviteeList.clear();
								for (Iterator iterator = getInvitedBuddies().iterator(); iterator.hasNext();) {
									VuzeBuddy buddy = (VuzeBuddy) iterator.next();
									sharePage.inviteeList.addFriend(buddy);
								}
							}
						});
					}

				}

				public void handleEmailInvites() {
					if(sharePage != null) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								for (Iterator iterator = getInvitedEmails().iterator(); iterator.hasNext();) {
									VuzeBuddy buddy = VuzeBuddyManager.createPotentialBuddy(null);
									buddy.setLoginID((iterator.next()).toString());
									sharePage.inviteeList.addFriend(buddy);
								}
								getWizard().performBack();
							}
						});
					}

				}

				public void handleInviteConfirm() {
					try {
						VuzeBuddyManager.inviteWithShare(getConfirmationResponse(), null,
								null, null);
					} catch (NotLoggedInException e) {
						// XXX Handle me!
						e.printStackTrace();
					}
					
					if(isStandalone) {
						handleClose();
					}
					
					showConfirmationDialog();
				}

				public void handleResize() {
//					if (true == "maximize".equals(getWindowState())) {
//						fullScreen(true);
//					} else if (true == "restore".equals(getWindowState())) {
//						fullScreen(false);
//					}
				}
				
				public void handleNbBuddiesUpdated(int nbInvites) {
					enableButton(BUTTON_OK,nbInvites > 0 || !isStandalone);
					enableButton(BUTTON_PREVIEW,nbInvites > 0 );
				}

			};

			context.addMessageListener(buddyPageListener);
		}
		return context;
	}

	public void refresh() {
		/*
		 * Calling to init the browser if it's not been done already
		 */
		if (null == browser) {
			getBrowser();
		}
		browser.refresh();
	}

	private void showConfirmationDialog() {
		if (null != buddyPageListener) {
			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
					MessageWindow messageWindow = new MessageWindow(
							mainShell, 6);

					messageWindow.setDetailMessages(buddyPageListener.getConfirmationMessages());
					messageWindow.setMessage(buddyPageListener.getFormattedInviteMessage());

					messageWindow.setTitle("Invite confirmation");
					messageWindow.setSize(400, 300);

					Utils.centerWindowRelativeTo(messageWindow.getShell(),mainShell);
					
					messageWindow.open();


				}
			});
		}
	}

	/**
	 * Opens the invite-friend page.
	 * Pre-select a friend or Friends by calling the preSelect method in the browser
	 * and passing on the specially formatted message
	 * @param message
	 */
	public void inviteWithMessage(final String message) {

		if (null != friendsToolbar) {
			friendsToolbar.setAddFriendsMode();
		}
		//		addRefreshListener(new IDetailPage.RefreshListener() {
		//			public boolean runOnlyOnce() {
		//				return true;
		//			}
		//
		//			public void refreshCompleted() {
		//				Utils.execSWTThreadLater(0, new AERunnable() {
		//					public void runSupport() {
		//						getMessageContext().executeInBrowser(
		//								"preSelect(\"" + message + "\")");
		//					}
		//				});
		//
		//			}
		//		});

	}

	public void setStandalone(boolean isStandalone) {
		this.isStandalone = isStandalone;
	}

	public boolean isStandalone() {
		return isStandalone;
	}
}
