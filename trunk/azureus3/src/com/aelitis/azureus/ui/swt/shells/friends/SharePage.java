package com.aelitis.azureus.ui.swt.shells.friends;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.progress.ProgressReportMessage;
import org.gudy.azureus2.ui.swt.shells.AbstractWizardPage;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractStatusListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.shells.StyledMessageWindow;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.ui.swt.views.skin.BuddiesViewer;
import com.aelitis.azureus.ui.swt.views.skin.FriendsToolbar;

import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.widgets.FriendsList;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.ImageDownloader;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.ImageDownloader.ImageDownloaderListener;

public class SharePage
	extends AbstractWizardPage
{
	public static final String ID = "share.wizard.page";

	private Composite content;

	private FriendsList buddyList;

	private Composite inviteePanel;

	private FriendsList inviteeList;

	private Button addBuddyButton;

	private Text messageText;

	private BuddiesViewer buddiesViewer;

	private FriendsToolbar friendsToolbar;

	private BrowserContext context;

	private AbstractBuddyPageListener buddyPageListener;

	private SelectedContentV3 shareItem;

	private String referer;

	private DownloadManager dm;

	private Label contentThumbnail;

	private Browser browser;

	private Composite contentDetail;

	private StyledText contentStats;

	private Font contentTitleFont = null;

	static {
		ImageRepository.addPath(
				"com/aelitis/azureus/ui/images/buddy_prompt_image.png",
				"buddy_prompt_image");
	}

	public SharePage(MultipageWizard wizard) {
		super(wizard);
	}

	public Composite createControls(Composite parent) {
		content = super.createControls(parent);

		buddiesViewer = (BuddiesViewer) SkinViewManager.getByClass(BuddiesViewer.class);
		friendsToolbar = (FriendsToolbar) SkinViewManager.getByClass(FriendsToolbar.class);

//		content.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		content.setBackgroundMode(SWT.INHERIT_FORCE);
		
		content.setLayout(new GridLayout(2, false));
		createContentDetail();
		createFriendsPanel();
		createOptionalMessage();

		return content;
	}

	private void createContentDetail() {
		contentDetail = new Composite(content, SWT.NONE);
		GridData gData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gData.horizontalSpan = 2;
		contentDetail.setLayoutData(gData);

		contentDetail.setLayout(new GridLayout(2, false));

		contentThumbnail = new Label(contentDetail, SWT.NONE);
		gData = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
		gData.widthHint = 142;
		gData.heightHint = 82;
		contentThumbnail.setLayoutData(gData);

		contentStats = new StyledText(contentDetail, SWT.WRAP);
		contentStats.setBackground(contentDetail.getBackground());

		contentStats.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		contentStats.getCaret().setVisible(false);
		contentStats.setEditable(false);
	}

	private void createFriendsPanel() {
		Composite friendsPanel = new Composite(content, SWT.NONE);
		friendsPanel.setLayout(new FillLayout(SWT.HORIZONTAL));
		friendsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createExistingFriendsList(friendsPanel);
		createNewFriendsList(friendsPanel);
	}

	private void createExistingFriendsList(Composite parent) {
		buddyList = new FriendsList(parent);
		buddyList.setBuddiesViewer(buddiesViewer);

		buddyList.setDefault_prompt_text(MessageText.getString("message.prompt.add.friends"));
		buddyList.setDefault_prompt_image(ImageRepository.getImage("buddy_prompt_image"));

	}

	private void createNewFriendsList(Composite parent) {
		inviteePanel = new Composite(parent, SWT.NONE);
		GridLayout gLayout = new GridLayout(2, false);
		gLayout.marginWidth = 0;
		gLayout.marginHeight = 0;
		gLayout.marginBottom = 5;
		inviteePanel.setLayout(gLayout);

		inviteeList = new FriendsList(inviteePanel);

		inviteeList.setEmailDisplayOnly(true);

		GridData gData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gData.horizontalSpan = 2;
		inviteeList.getControl().setLayoutData(gData);

		Label addBuddyPromptLabel = new Label(inviteePanel, SWT.NONE | SWT.WRAP
				| SWT.RIGHT);
		addBuddyPromptLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true,
				false));
		addBuddyPromptLabel.setText(MessageText.getString("v3.Share.invite.buddies.prompt"));

		addBuddyButton = new Button(inviteePanel, SWT.PUSH);
		addBuddyButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		addBuddyButton.setText(MessageText.getString("v3.Share.add.buddy"));
		addBuddyButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				getWizard().showPage(AddFriendsPage.ID);
				friendsToolbar.enableShareButton(false);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

	}

	private void createOptionalMessage() {
		Composite messagePanel = new Composite(content, SWT.NONE);
		GridData gData = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		gData.horizontalSpan = 2;
		messagePanel.setLayoutData(gData);

		messagePanel.setLayout(new GridLayout());

		Label messageLabel = new Label(messagePanel, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		messageLabel.setText(MessageText.getString("v3.Share.optional.message"));

		messageText = new Text(messagePanel, SWT.WRAP | SWT.BORDER);
		gData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gData.heightHint = messageText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y * 2;
		messageText.setLayoutData(gData);
		messageText.setTextLimit(140);

		Label messageDisclaimerLabel = new Label(messagePanel, SWT.WRAP);
		messageDisclaimerLabel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM,
				true, false));
		messageDisclaimerLabel.setText(MessageText.getString("v3.Share.disclaimer"));
	}

	/**
	 * Overriding default button
	 */
	protected void createButtons(Composite buttonPanel) {

		createButton(BUTTON_CANCEL, MessageText.getString("Button.cancel"),
				defaultButtonListener);

		createButton(BUTTON_OK, MessageText.getString("v3.Share.send.now"),
				new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {
						getMessageContext().executeInBrowser(
								"sendSharingBuddies('" + getCommitJSONMessage() + "')");

						getMessageContext().executeInBrowser(
								"setShareReferer('" + referer + "')");

						getMessageContext().executeInBrowser("shareSubmit()");
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				});
	}

	private String getCommitJSONMessage() {
		if (null == shareItem || null == shareItem.getHash()) {
			return null;
		}
		List buddieloginIDsAndContentHash = new ArrayList();
		List loginIDs = new ArrayList();
		for (Iterator iterator = buddyList.getFriends().iterator(); iterator.hasNext();) {
			VuzeBuddySWT vuzeBuddy = (VuzeBuddySWT) iterator.next();
			loginIDs.add(vuzeBuddy.getLoginID());
		}
		buddieloginIDsAndContentHash.add(loginIDs);
		buddieloginIDsAndContentHash.add(shareItem.getHash());

		return JSONUtils.encodeToJSON(buddieloginIDsAndContentHash);
	}

	public void setShareItem(SelectedContentV3 content, String referer) {
		this.shareItem = content;
		this.referer = referer;
		this.dm = shareItem.getDM();

		if (SystemTime.getCurrentTime() - PlatformBuddyMessenger.getLastSyncCheck() > PlatformConfigMessenger.getBuddySyncOnShareMinTimeSecs() * 1000) {
			try {
				PlatformBuddyMessenger.sync(null);
			} catch (NotLoggedInException e) {
			}
		}

		if (content != null && content.getThumbURL() != null) {
			ImageDownloader.loadImage(content.getThumbURL(),
					new ImageDownloaderListener() {
						public void imageDownloaded(final byte[] image) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									if (contentThumbnail != null
											&& !contentThumbnail.isDisposed()) {
										ByteArrayInputStream bis = new ByteArrayInputStream(image);
										final Image img = new Image(Display.getDefault(), bis);
										if (img != null) {
											contentThumbnail.addDisposeListener(new DisposeListener() {
												public void widgetDisposed(DisposeEvent e) {
													if (img != null && !img.isDisposed()) {
														img.dispose();
													}
												}
											});
											contentThumbnail.setImage(img);
										}
									}
								}
							});
						}
					});
		}

		if (null != shareItem) {
			if (null != friendsToolbar) {
				friendsToolbar.setShareMode();
			}

			//			getDetailPanel().show(true, PAGE_ID);
			//
			//// KN: Work in progress for new Share wizard			
			//			ShareWizard shell = new ShareWizard(
			//					UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell(),
			//					SWT.DIALOG_TRIM | SWT.RESIZE);
			//			shell.setText("Vuze - Wizard");
			//			shell.setSize(500, 550);
			//			
			//			/*
			//			 * Opens a centered free-floating shell
			//			 */
			//
			//			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			//			if (null == uiFunctions) {
			//				/*
			//				 * Centers on the active monitor
			//				 */
			//				Utils.centreWindow(shell.getShell());
			//			} else {
			//				/*
			//				 * Centers on the main application window
			//				 */
			//				Utils.centerWindowRelativeTo(shell.getShell(),
			//						uiFunctions.getMainShell());
			//			}
			//
			//			shell.open();
		}
	}

	public SelectedContentV3 getShareItem() {
		return shareItem;
	}

	public void performFinish() {
		System.out.println("TODO: send the share message now!!!!!");
	}

	public void performCancel() {
		super.performCancel();
	}

	public String getPageID() {
		return ID;
	}

	public String getDesciption() {
		return MessageText.getString("v3.Share.header.message");
	}

	public String getTitle() {
		return MessageText.getString("v3.Share.header");
	}

	public String getWindowTitle() {
		return MessageText.getString("v3.Share.wizard.title");
	}

	public void performAboutToBeShown() {
		super.performAboutToBeShown();
		friendsToolbar.enableShareButton(true);

		/*
		 * Init the browser if it was not done already
		 */
		if (null == browser) {
			getBrowser();
		}
		browser.refresh();

		if (null != friendsToolbar) {
			friendsToolbar.setShareMode();
		}

		if (null != buddiesViewer) {
			setBuddies(buddiesViewer.getSelection());
			buddiesViewer.addSelectionToShare();
		}

		byte[] imageBytes = shareItem.getImageBytes();
		if (imageBytes == null && null != dm && null != dm.getTorrent()) {
			imageBytes = PlatformTorrentUtils.getContentThumbnail(dm.getTorrent());
		}

		Image img = null;
		if (null != imageBytes && imageBytes.length > 0) {
			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			img = new Image(Display.getDefault(), bis);

			/*
			 * Dispose this image when the canvas is disposed
			 */
			final Image img_final = img;
			contentDetail.addDisposeListener(new DisposeListener() {

				public void widgetDisposed(DisposeEvent e) {
					if (null != img_final && false == img_final.isDisposed()) {
						img_final.dispose();
					}
				}
			});

		} else if (dm != null) {
			String path = dm.getDownloadState().getPrimaryFile();
			if (path != null) {
				img = ImageRepository.getPathIcon(path, true, dm.getTorrent() != null
						&& !dm.getTorrent().isSimpleTorrent());
				/*
				 * DO NOT dispose the image from .getPathIcon()!!!!
				 */
			}
		}

		contentThumbnail.setImage(img);

		updateContentStats();

	}

	public void setBuddies(List buddies) {
		buddyList.clear();
		for (Iterator iterator = buddies.iterator(); iterator.hasNext();) {
			Object vuzeBuddy = iterator.next();
			if (vuzeBuddy instanceof VuzeBuddy) {
				buddyList.addFriend((VuzeBuddy) vuzeBuddy);
			}
		}
	}

	private void updateContentStats() {
		contentStats.setText("");

		if (shareItem == null) {
			return;
		}

		if (null == contentTitleFont) {
			FontData[] fDatas = contentStats.getFont().getFontData();
			for (int i = 0; i < fDatas.length; i++) {
				fDatas[i].height += 2;
			}
			contentTitleFont = new Font(contentStats.getDisplay(), fDatas);
			contentStats.addDisposeListener(new DisposeListener() {

				public void widgetDisposed(DisposeEvent e) {
					if (null != contentTitleFont
							&& false == contentTitleFont.isDisposed()) {
						contentTitleFont.dispose();
					}
				}
			});

		}

		int charCount = contentStats.getCharCount();
		contentStats.append(shareItem.getDisplayName() + "\n");
		StyleRange style2 = new StyleRange();
		style2.start = charCount;
		style2.length = shareItem.getDisplayName().length();
		style2.font = contentTitleFont;
		contentStats.setStyleRange(style2);

		if (null == dm) {
			return;
		}

		String publisher = PlatformTorrentUtils.getContentPublisher(dm.getTorrent());

		if (null != publisher && publisher.length() > 0) {
			if (publisher.startsWith("az")) {
				publisher = publisher.substring(2);
			}
			contentStats.append("From: " + publisher + "\n");
		}

		contentStats.append("File size: " + dm.getSize() / 1000000 + " MB");
	}

	public ClientMessageContext getMessageContext() {
		if (null == context) {
			context = new BrowserContext("buddy-page-listener-share" + Math.random(),
					getBrowser(), null, true);

			context.addMessageListener(new DisplayListener(getBrowser()));

			/*
			 * Add listener to call the 'inviteFromShare' script; this listener is only called
			 * once whenever a web page is loaded the first time or when it's refreshed
			 */
			context.addMessageListener(new AbstractStatusListener("status") {
				public void handlePageLoadCompleted() {
					/*
					 * Setting inviteFromShare to true in the browser
					 */
					context.executeInBrowser("inviteFromShare(" + true + ")");

					//					SharePage.this.notifyRefreshListeners();

				}
			});

			/*
			 * Add the appropriate messaging listeners
			 */

			buddyPageListener = new AbstractBuddyPageListener(getBrowser()) {

				private Map confirmationResponse;

				public void handleCancel() {
//					Utils.execSWTThread(new AERunnable() {
//						public void runSupport() {
//							getWizard().showPage(ID);
//						}
//					});
				}

				public void handleClose() {
//					Utils.execSWTThread(new AERunnable() {
//						public void runSupport() {
//							getWizard().showPage(ID);
//						}
//					});

				}

				public void handleBuddyInvites() {

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							inviteeList.clear();
							for (Iterator iterator = getInvitedBuddies().iterator(); iterator.hasNext();) {
								VuzeBuddy buddy = (VuzeBuddy) iterator.next();
								inviteeList.addFriend(buddy);
							}
						}
					});

				}

				public void handleEmailInvites() {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							for (Iterator iterator = getInvitedEmails().iterator(); iterator.hasNext();) {
								VuzeBuddy buddy = VuzeBuddyManager.createPotentialBuddy(null);
								buddy.setLoginID((iterator.next()).toString());
								inviteeList.addFriend(buddy);
							}
						}
					});

				}

				public void handleInviteConfirm() {
					confirmationResponse = getConfirmationResponse();

					if (null != confirmationResponse) {
						final List buddiesToShareWith = buddyList.getFriends();
						final VuzeBuddy[] buddies = (VuzeBuddy[]) buddiesToShareWith.toArray(new VuzeBuddy[buddiesToShareWith.size()]);
						SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
							public void loginComplete() {
								try {
									VuzeBuddyManager.inviteWithShare(confirmationResponse,
											getShareItem(), messageText.getText(), buddies);
									getWizard().close();
									showConfirmationDialog(buddiesToShareWith);

								} catch (NotLoggedInException e) {
									//Do nothing if login failed; leaves the Share page open... the user can then click cancel to dismiss or 
									// try again
								}
							}
						});
					}
				}

				public void handleResize() {
				}

			};
			context.addMessageListener(buddyPageListener);
		}
		return context;
	}

	private void showConfirmationDialog(List buddiesToShareWith) {

		if (null != buddyPageListener) {

			final String[] message = new String[1];
			final List messages = new ArrayList();

			if (null == buddiesToShareWith) {
				buddiesToShareWith = Collections.EMPTY_LIST;
			}

			/*
			 * Share only
			 */
			if (buddyPageListener.getInvitationsSent() == 0) {
				/*
				 * The main message to display
				 */
				if (buddiesToShareWith.size() > 1) {
					message[0] = MessageText.getString("message.confirm.share.plural");
				} else {
					message[0] = MessageText.getString("message.confirm.share.singular");
				}
			}

			/*
			 * Share with invitations
			 */
			else {

				boolean hasError = false;
				List inviteMessages = buddyPageListener.getConfirmationMessages();
				for (Iterator iterator = inviteMessages.iterator(); iterator.hasNext();) {
					ProgressReportMessage cMessage = (ProgressReportMessage) iterator.next();
					if (true == cMessage.isError()) {
						hasError = true;
						break;
					}
				}

				if (true == hasError) {
					message[0] = MessageText.getString("message.confirm.invite.error");
					messages.addAll(buddyPageListener.getConfirmationMessages());
				} else {
					/*
					 * The main message to display
					 */
					if (buddiesToShareWith.size()
							+ buddyPageListener.getInvitationsSent() == 1) {
						message[0] = MessageText.getString("message.confirm.share.invite.singular");
					} else {
						message[0] = MessageText.getString("message.confirm.share.invite.plural");
					}
				}
			}

			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					final LightBoxShell lightBoxShell = new LightBoxShell(false);
					StyledMessageWindow messageWindow = new StyledMessageWindow(
							lightBoxShell.getShell(), 6, true);

					messageWindow.setDetailMessages(messages);
					messageWindow.setMessage(message[0]);
					messageWindow.setTitle("Share confirmation");
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

	private Browser getBrowser() {
		if (null == browser) {

			AddFriendsPage page = (AddFriendsPage) getWizard().getPage(
					AddFriendsPage.ID);
			if (null != page) {
				browser = page.getBrowser();
			}

			/*
			 * Calling to initialize the listeners
			 */
			getMessageContext();

		}

		return browser;
	}
}
