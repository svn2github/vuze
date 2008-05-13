package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.util.Constants;

public class InvitePage
	extends AbstractDetailPage

{

	public static final String PAGE_ID = "invite.page";

	private Composite content;

	private Composite blinder;

	private Browser browser = null;

	private StackLayout stackLayout = null;

	private ClientMessageContext context = null;

	public InvitePage(DetailPanel detailPanel) {
		super(detailPanel, PAGE_ID);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		stackLayout = new StackLayout();
		content.setLayout(stackLayout);
		init();
	}

	private void init() {
		blinder = new Composite(content, SWT.NONE);
	}

	private Browser getBrowser() {
		if (null == browser) {
			browser = new Browser(content, SWT.NONE);
			String url = Constants.URL_PREFIX + "share.start";
			browser.setUrl(url);

			stackLayout.topControl = browser;
			content.layout();

			/*
			 * Calling to initialize the context
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
			context = new BrowserContext("buddy-page-listener-invite" + Math.random(),
					getBrowser(), null, true);
			
			/*
			 * Setting inviteFromShare to false in the browser
			 */
			context.executeInBrowser("inviteFromShare(" + false + ")");
			
			/*
			 * Add the appropriate messaging listeners
			 */
			getMessageContext().addMessageListener(
					new AbstractBuddyPageListener(getBrowser()) {

						public void handleCancel() {
							System.out.println("'Cancel' called from invite buddy page");//KN: sysout

							ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
							if (null != buttonBar) {
								buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
							}

							getDetailPanel().show(false);

						}

						public void handleClose() {
							System.out.println("'Close' called from invite buddy page");//KN: sysout

							ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
							if (null != buttonBar) {
								buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
							}

							getDetailPanel().show(false);

						}

						public void handleBuddyInvites() {
							System.out.println("'buddy-invites' called from invite buddy page");//KN: sysout
						}

						public void handleEmailInvites() {
							System.out.println("'email-invites' called from invite buddy page");//KN: sysout

						}

						public void handleInviteConfirm() {
							try {
								VuzeBuddyManager.inviteWithShare(getConfirmationResponse(), null,
										null, null);
							} catch (NotLoggedInException e) {
								// XXX Handle me!
								e.printStackTrace();
							}

							System.err.println("\t'invite-confirm' called from invite buddy page: "
									+ getConfirmationMessage());//KN: sysout

						}
					}

			);
		}
		return context;
	}

	public void refresh() {
		/*
		 * Calling to init the browser if it's not been done already
		 */
		if(null == browser){
			getBrowser();
		}

	}

}
