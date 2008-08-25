package com.aelitis.azureus.ui.swt.shells.friends;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.shells.AbstractWizardPage;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractStatusListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
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

	private boolean isStandalone;

	public AddFriendsPage(MultipageWizard wizard) {
		super(wizard);
	}

	public Composite createControls(Composite parent) {
		content = super.createControls(parent);
		content.setLayout(new FillLayout());

		friendsToolbar = (FriendsToolbar) SkinViewManager.getByClass(FriendsToolbar.class);

		browser = new Browser(content, SWT.NONE);
		String url = Constants.URL_PREFIX + "share.start?ts=" + Math.random();
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

		createButton(BUTTON_PREVIEW, MessageText.getString("Button.preview"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						context.executeInBrowser("preview()");
						showButton(BUTTON_BACK, true);
						showButton(BUTTON_CONTINUE, false);
						showButton(BUTTON_PREVIEW, false);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});

		createButton(BUTTON_CONTINUE, MessageText.getString("Button.continue"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						System.out.println("TODO: do continue");
						performBack();
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});

		createButton(BUTTON_BACK, MessageText.getString("wizard.previous"),
				new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						context.executeInBrowser("previewCancel()");
						showButton(BUTTON_CONTINUE, true);
						showButton(BUTTON_PREVIEW, true);
						showButton(BUTTON_BACK, false);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
		showButton(BUTTON_BACK, false);
	}

	public Browser getBrowser() {
		return browser;
	}

	public boolean isInitOnStartup() {
		return true;
	}

	public ClientMessageContext getMessageContext() {
		if (null == context) {
			context = new BrowserContext(
					"buddy-page-listener-invite" + Math.random(), getBrowser(), null,
					true);

			context.addMessageListener(new DisplayListener(getBrowser()));

			/*
			 * Add listener to call the 'inviteFromShare' script; this listener is only called
			 * once whenever a web page is loaded the first time or when it's refreshed
			 */
			if (true == isStandalone()) {
				context.addMessageListener(new AbstractStatusListener("status") {
					public void handlePageLoadCompleted() {
						/*
						 * Setting inviteFromShare to false in the browser
						 */
						context.executeInBrowser("inviteFromShare(" + false + ")");

					}
				});
			}
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
				}

				public void handleEmailInvites() {
				}

				public void handleInviteConfirm() {
					try {
						VuzeBuddyManager.inviteWithShare(getConfirmationResponse(), null,
								null, null);
					} catch (NotLoggedInException e) {
						// XXX Handle me!
						e.printStackTrace();
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

			};

			getMessageContext().addMessageListener(buddyPageListener);
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
					final LightBoxShell lightBoxShell = new LightBoxShell(false);
					StyledMessageWindow messageWindow = new StyledMessageWindow(
							lightBoxShell.getShell(), 6, true);

					messageWindow.setDetailMessages(buddyPageListener.getConfirmationMessages());
					messageWindow.setMessage(buddyPageListener.getFormattedInviteMessage());

					messageWindow.setTitle("Invite confirmation");
					messageWindow.setSize(400, 300);

					messageWindow.addListener(SWT.Dispose, new Listener() {
						public void handleEvent(Event event) {
							lightBoxShell.close();
						}
					});
					lightBoxShell.open(messageWindow);

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
