package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractStatusListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.shells.StyledMessageWindow;
import com.aelitis.azureus.util.Constants;

public class InvitePage
	extends AbstractDetailPage

{

	public static final String PAGE_ID = "invite.page";

	private Composite content;

	private Browser browser = null;

	private ClientMessageContext context = null;

	private RefreshListener refreshListener = null;

	private AbstractBuddyPageListener buddyPageListener;

	public InvitePage(DetailPanel detailPanel) {
		super(detailPanel, PAGE_ID);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		init();
	}

	private void init() {
		FillLayout fLayout = new FillLayout();
		fLayout.marginHeight = 0;
		fLayout.marginWidth = 0;
		content.setLayout(fLayout);

	}

	private Browser getBrowser() {
		if (null == browser) {
			browser = new Browser(content, SWT.NONE);
			String url = Constants.URL_PREFIX + "share.start?ts=" + Math.random();
			browser.setUrl(url);

			if (null != activationListener) {
				browser.addMouseListener(new MouseAdapter() {

					public void mouseDown(MouseEvent e) {
						activationListener.pageActivated();
					}
				});
			}

			/*
			 * Calling to initialize the listeners
			 */
			getMessageContext();

		}
		return browser;
	}

	public Control getControl() {
		return content;
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
			context.addMessageListener(new AbstractStatusListener("status") {
				public void handlePageLoadCompleted() {
					/*
					 * Setting inviteFromShare to false in the browser
					 */
					context.executeInBrowser("inviteFromShare(" + false + ")");

					if (null != refreshListener) {
						refreshListener.refreshCompleted();
					}
				}
			});

			/*
			 * Add the appropriate messaging listeners
			 */

			buddyPageListener = new AbstractBuddyPageListener(getBrowser()) {

				public void handleCancel() {
					ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
					if (null != buttonBar) {
						buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
					}

					getDetailPanel().show(false);

				}

				public void handleClose() {
					ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
					if (null != buttonBar) {
						buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
					}
					getDetailPanel().showBusy(true, 0);

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
					if (null != getWindowState()) {
						System.out.println("Resizing standalone Add Friends: "
								+ getWindowState());//KN: sysout
					}
				}

			};

			getMessageContext().addMessageListener(buddyPageListener);
		}
		return context;
	}

	public void refresh(RefreshListener refreshListener) {
		this.refreshListener = refreshListener;
		/*
		 * Calling to init the browser if it's not been done already
		 */
		if (null == browser) {
			getBrowser();
		}
		browser.refresh();
	}

	private void showConfirmationDialog() {
		getDetailPanel().showBusy(false, 0);
		getDetailPanel().show(false);

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
}
