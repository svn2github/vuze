package com.aelitis.azureus.ui.swt.views.skin;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.AbstractBuddyPageListener;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.JSONUtils;

public class SharePage
	extends AbstractDetailPage
{

	public static final String PAGE_ID = "share.page";

	private Composite content;

	private StackLayout stackLayout;

	private Composite firstPanel = null;

	private Composite browserPanel = null;

	private Label shareHeaderLabel;

	private Label shareHeaderMessageLabel;

	private Label buddyListDescription;

	private Label addBuddyPromptLabel;

	private StyledText buddyList;

	private Composite inviteePanel;

	private StyledText inviteeList;

	private Composite contentDetail;

	private StyledText contentStats;

	private Button addBuddyButton;

	private Button sendNowButton;

	private Button cancelButton;

	private Label contentThumbnail;

	private Label optionalMessageLabel;

	private Text commentText;

	private Browser browser = null;

	private ClientMessageContext context = null;

	private List selectedBuddies = new ArrayList();

	private Map confirmationResponse = null;

	private Color textColor = null;

	private SelectedContent shareItem = null;

	private DownloadManager dm = null;

	public SharePage(DetailPanel detailPanel) {
		super(detailPanel, PAGE_ID);
	}

	public void createControls(Composite parent) {

		textColor = SWTSkinFactory.getInstance().getSkinProperties().getColor(
				"color.text.fg");

		content = new Composite(parent, SWT.NONE);

		createFirstPanel();
		createBrowserPanel();
	}

	private void createFirstPanel() {
		createControls();
		formatControls();
		layoutControls();
		hookListeners();
	}

	private void createControls() {

		firstPanel = new Composite(content, SWT.NONE);

		shareHeaderLabel = new Label(firstPanel, SWT.NONE);
		shareHeaderMessageLabel = new Label(firstPanel, SWT.NONE);
		buddyListDescription = new Label(firstPanel, SWT.NONE);
		buddyList = new StyledText(firstPanel, SWT.NONE);
		inviteePanel = new Composite(firstPanel, SWT.NONE);
		inviteeList = new StyledText(inviteePanel, SWT.NONE);
		addBuddyPromptLabel = new Label(inviteePanel, SWT.NONE | SWT.WRAP
				| SWT.RIGHT);
		addBuddyButton = new Button(inviteePanel, SWT.PUSH);
		contentDetail = new Composite(firstPanel, SWT.NONE);
		sendNowButton = new Button(firstPanel, SWT.PUSH);
		cancelButton = new Button(firstPanel, SWT.PUSH);
		contentThumbnail = new Label(contentDetail, SWT.BORDER);
		contentStats = new StyledText(contentDetail, SWT.NONE);
		optionalMessageLabel = new Label(contentDetail, SWT.NONE);
		commentText = new Text(contentDetail, SWT.BORDER);

	}

	private void layoutControls() {

		stackLayout = new StackLayout();
		stackLayout.marginHeight = 0;
		stackLayout.marginWidth = 0;
		content.setLayout(stackLayout);
		stackLayout.topControl = firstPanel;

		firstPanel.setLayout(new FormLayout());

		buddyList.setIndent(3);

		//============		
		FormLayout fLayout = new FormLayout();
		fLayout.marginTop = 3;
		fLayout.marginBottom = 3;
		fLayout.marginLeft = 3;
		fLayout.marginRight = 3;
		inviteePanel.setLayout(fLayout);

		FormData inviteePanelData = new FormData();
		inviteePanelData.top = new FormAttachment(buddyList, 10);
		inviteePanelData.left = new FormAttachment(buddyList, 0, SWT.LEFT);
		inviteePanelData.right = new FormAttachment(buddyList, 0, SWT.RIGHT);
		inviteePanelData.height = 125;
		inviteePanel.setLayoutData(inviteePanelData);

		FormData inviteeListData = new FormData();
		inviteeListData.top = new FormAttachment(0, 0);
		inviteeListData.left = new FormAttachment(0, 0);
		inviteeListData.right = new FormAttachment(100, 0);
		inviteeListData.height = 75;
		inviteeList.setLayoutData(inviteeListData);

		FormData addBuddyButtonData = new FormData();
		addBuddyButtonData.top = new FormAttachment(inviteeList, 8);
		addBuddyButtonData.right = new FormAttachment(inviteeList, -8, SWT.RIGHT);
		addBuddyButton.setLayoutData(addBuddyButtonData);

		FormData addBuddyLabelData = new FormData();
		addBuddyLabelData.top = new FormAttachment(inviteeList, 8);
		addBuddyLabelData.right = new FormAttachment(addBuddyButton, -8);
		addBuddyLabelData.left = new FormAttachment(inviteeList, 0, SWT.LEFT);
		addBuddyPromptLabel.setLayoutData(addBuddyLabelData);

		//==============

		FormData shareHeaderData = new FormData();
		shareHeaderData.top = new FormAttachment(0, 8);
		shareHeaderData.left = new FormAttachment(0, 8);
		shareHeaderLabel.setLayoutData(shareHeaderData);

		FormData shareHeaderMessageData = new FormData();
		shareHeaderMessageData.top = new FormAttachment(shareHeaderLabel, 8);
		shareHeaderMessageData.left = new FormAttachment(0, 30);
		shareHeaderMessageData.right = new FormAttachment(100, -8);
		shareHeaderMessageLabel.setLayoutData(shareHeaderMessageData);

		FormData buddyListDescriptionData = new FormData();
		buddyListDescriptionData.top = new FormAttachment(shareHeaderMessageLabel,
				8);
		buddyListDescriptionData.left = new FormAttachment(buddyList, 0, SWT.LEFT);
		buddyListDescription.setLayoutData(buddyListDescriptionData);

		FormData buddyListData = new FormData();
		buddyListData.top = new FormAttachment(buddyListDescription, 0);
		buddyListData.left = new FormAttachment(0, 30);
		buddyListData.width = 200;
		buddyListData.height = 150;
		buddyList.setLayoutData(buddyListData);

		FormData contentDetailData = new FormData();
		contentDetailData.top = new FormAttachment(buddyList, 0, SWT.TOP);
		contentDetailData.left = new FormAttachment(buddyList, 30);
		contentDetailData.right = new FormAttachment(100, -8);
		contentDetailData.bottom = new FormAttachment(inviteePanel, 0, SWT.BOTTOM);
		contentDetail.setLayoutData(contentDetailData);

		FormData sendNowButtonData = new FormData();
		sendNowButtonData.top = new FormAttachment(contentDetail, 8);
		sendNowButtonData.right = new FormAttachment(contentDetail, 0, SWT.RIGHT);
		sendNowButton.setLayoutData(sendNowButtonData);

		FormData cancelButtonData = new FormData();
		cancelButtonData.right = new FormAttachment(sendNowButton, -8);
		cancelButtonData.top = new FormAttachment(contentDetail, 8);
		cancelButton.setLayoutData(cancelButtonData);

		FormLayout detailLayout = new FormLayout();
		detailLayout.marginWidth = 8;
		detailLayout.marginHeight = 8;
		contentDetail.setLayout(detailLayout);

		FormData buddyImageData = new FormData();
		buddyImageData.top = new FormAttachment(0, 8);
		buddyImageData.left = new FormAttachment(0, 8);
		buddyImageData.width = 142;
		buddyImageData.height = 82;
		contentThumbnail.setLayoutData(buddyImageData);

		FormData contentStatsData = new FormData();
		contentStatsData.top = new FormAttachment(0, 8);
		contentStatsData.left = new FormAttachment(contentThumbnail, 8);
		contentStatsData.right = new FormAttachment(100, -8);
		contentStatsData.bottom = new FormAttachment(contentThumbnail, 0,
				SWT.BOTTOM);
		contentStats.setLayoutData(contentStatsData);

		FormData commentLabelData = new FormData();
		commentLabelData.top = new FormAttachment(contentThumbnail, 16);
		commentLabelData.left = new FormAttachment(0, 8);
		optionalMessageLabel.setLayoutData(commentLabelData);

		FormData commentTextData = new FormData();
		commentTextData.top = new FormAttachment(optionalMessageLabel, 8);
		commentTextData.left = new FormAttachment(0, 8);
		commentTextData.right = new FormAttachment(100, -8);
		commentTextData.bottom = new FormAttachment(100, -8);
		commentText.setLayoutData(commentTextData);

		content.layout();
	}

	private void formatControls() {
		optionalMessageLabel.setForeground(textColor);
		addBuddyPromptLabel.setForeground(textColor);
		inviteeList.setForeground(textColor);
		buddyList.setForeground(textColor);
		buddyListDescription.setForeground(textColor);
		shareHeaderMessageLabel.setForeground(textColor);
		shareHeaderLabel.setForeground(textColor);

		contentStats.setForeground(textColor);

		applyRoundedBorder(contentDetail);
		applyRoundedBorder(inviteePanel);
		applyRoundedBorder(buddyList);

		sendNowButton.setEnabled(false);
		Messages.setLanguageText(sendNowButton, "v3.Share.send.now");

		Messages.setLanguageText(addBuddyPromptLabel,
				"v3.Share.invite.buddies.prompt");
		Messages.setLanguageText(addBuddyButton, "v3.Share.add.buddy");
		Messages.setLanguageText(optionalMessageLabel, "v3.Share.optional.message");
		Messages.setLanguageText(cancelButton, "v3.MainWindow.button.cancel");

		Messages.setLanguageText(shareHeaderLabel, "v3.Share.header");
		Messages.setLanguageText(shareHeaderMessageLabel, "v3.Share.header.message");
		Messages.setLanguageText(buddyListDescription, "v3.Share.add.buddy.all");

	}

	private void applyRoundedBorder(final Control control) {
		control.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				e.gc.setForeground(ColorCache.getColor(control.getDisplay(), 200, 200,
						200));
				e.gc.setAntialias(SWT.ON);
				Rectangle r = control.getBounds();
				e.gc.drawRoundRectangle(0, 0, r.width - 1, r.height - 1, 10, 10);

			}
		});
	}

	private void createBrowserPanel() {
		browserPanel = new Composite(content, SWT.NONE);
		FillLayout fLayout = new FillLayout();
		browserPanel.setLayout(fLayout);

	}

	private void activateFirstPanel() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				stackLayout.topControl = firstPanel;
				content.layout();
			}
		});

	}

	private void hookListeners() {

		addBuddyButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				stackLayout.topControl = browserPanel;
				content.layout();

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		sendNowButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				getMessageContext().executeInBrowser(
						"sendSharingBuddies('" + getCommitJSONMessage() + "')");

				getMessageContext().executeInBrowser("shareSubmit()");

				VuzeBuddy[] buddies = (VuzeBuddy[]) selectedBuddies.toArray(new VuzeBuddy[selectedBuddies.size()]);
				try {
					VuzeBuddyManager.inviteWithShare(confirmationResponse,
							getShareItem(), commentText.getText(), buddies);
				} catch (NotLoggedInException e1) {
					// XXX Handle me!
					e1.printStackTrace();
				}

				ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
				if (null != buttonBar) {
					buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
				}
				resetControls();
				getDetailPanel().show(false);

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		cancelButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				/*
				 * Tell the browser that we're canceling so it can re-initialize it's states
				 */
				getMessageContext().executeInBrowser("initialize()");

				ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
				if (null != buttonBar) {
					buttonBar.setActiveMode(BuddiesViewer.none_active_mode);
				}
				
				resetControls();
				getDetailPanel().show(false);

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	private void resetControls(){
		inviteeList.setText("");
		commentText.setText("");
	}
	private String getCommitJSONMessage() {
		if (null == shareItem || null == shareItem.hash) {
			return null;
		}
		List buddieloginIDsAndContentHash = new ArrayList();
		List loginIDs = new ArrayList();
		for (Iterator iterator = selectedBuddies.iterator(); iterator.hasNext();) {
			VuzeBuddySWT vuzeBuddy = (VuzeBuddySWT) iterator.next();
			loginIDs.add(vuzeBuddy.getLoginID());
		}
		buddieloginIDsAndContentHash.add(loginIDs);
		buddieloginIDsAndContentHash.add(shareItem.hash);

		return JSONUtils.encodeToJSON(buddieloginIDsAndContentHash);
	}

	public void setBuddies(List buddies) {
		selectedBuddies.clear();
		buddyList.setText("");
		for (Iterator iterator = buddies.iterator(); iterator.hasNext();) {
			Object vuzeBuddy = iterator.next();
			if (vuzeBuddy instanceof VuzeBuddySWT) {
				selectedBuddies.add(vuzeBuddy);
				buddyList.append(((VuzeBuddySWT) vuzeBuddy).getDisplayName() + "\n");
			} else {
				System.err.println("Bogus buddy: " + vuzeBuddy);//KN: sysout
			}
		}
	}

	public void addBuddies(List buddies) {
		for (Iterator iterator = buddies.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof VuzeBuddySWT) {
				addBuddy((VuzeBuddySWT) object);
			}
		}
	}

	public void addBuddy(VuzeBuddySWT vuzeBuddy) {
		if (false == selectedBuddies.contains(vuzeBuddy)) {
			selectedBuddies.add(vuzeBuddy);
			buddyList.append(vuzeBuddy.getDisplayName() + "\n");
			buddyList.layout();
		}
	}

	public Control getControl() {
		return content;
	}

	public ClientMessageContext getMessageContext() {
		if (null == context) {
			context = new BrowserContext("buddy-page-listener-share" + Math.random(),
					getBrowser(), null, true);
			/*
			 * Setting inviteFromShare to true in the browser
			 */
			context.executeInBrowser("inviteFromShare(" + true + ")");

			/*
			 * Add the appropriate messaging listeners
			 */
			context.addMessageListener(new AbstractBuddyPageListener(getBrowser()) {

				public void handleCancel() {
					System.out.println("'Cancel' called from share->invite buddy page");//KN: sysout

					activateFirstPanel();
				}

				public void handleClose() {
					System.out.println("'Close' called from share->invite buddy page");//KN: sysout
					activateFirstPanel();
				}

				public void handleBuddyInvites() {

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							inviteeList.setText("");
							for (Iterator iterator = getInvitedBuddies().iterator(); iterator.hasNext();) {
								VuzeBuddy buddy = (VuzeBuddy) iterator.next();
								inviteeList.append(buddy.getDisplayName() + "\n");
							}
							if (true == inviteeList.getCharCount() > 0) {
								Messages.setLanguageText(addBuddyButton,
										"v3.Share.add.or.remove.buddy");
							} else {
								Messages.setLanguageText(addBuddyButton, "v3.Share.add.buddy");
							}
							inviteePanel.layout();
						}
					});

				}

				public void handleEmailInvites() {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							for (Iterator iterator = getInvitedEmails().iterator(); iterator.hasNext();) {
								inviteeList.append(iterator.next() + "\n");//KN:
							}

							if (true == inviteeList.getCharCount() > 0) {
								Messages.setLanguageText(addBuddyButton,
										"v3.Share.add.or.remove.buddy");
							} else {
								Messages.setLanguageText(addBuddyButton, "v3.Share.add.buddy");
							}

							inviteePanel.layout();
						}
					});

				}

				public void handleInviteConfirm() {
					confirmationResponse = getConfirmationResponse();

					//Display pop-up here!!!
					System.err.println("\t'invite-confirm' called from share page: "
							+ getConfirmationMessage());//KN: sysout

					if (null != getConfirmationMessage()) {
						Utils.execSWTThread(new AERunnable() {

							public void runSupport() {
								MessageBox mb = new MessageBox(inviteePanel.getShell(),
										SWT.ICON_INFORMATION | SWT.OK);
								mb.setMessage(getConfirmationMessage());
								mb.open();
							}
						});
					}
				}
			});
		}
		return context;
	}

	private Browser getBrowser() {
		if (null == browser) {
			browser = new Browser(browserPanel, SWT.NONE);

			/*
			 * Adding a completion listener before we set the url; only when the page is fully loaded
			 * can we reliably call getMessageContext().
			 * Additionally this call only needs to be made once
			 */
			browser.addProgressListener(new ProgressListener() {
				private boolean runOnce = false;

				public void completed(ProgressEvent event) {
					/*
					 * Calling to initialize the context
					 */
					if (false == runOnce) {
						getMessageContext();
						runOnce = true;
					}
				}

				public void changed(ProgressEvent event) {
				}
			});

			String url = Constants.URL_PREFIX + "share.start";
			browser.setUrl(url);

		}

		return browser;
	}

	public void setShareItem(SelectedContent content) {
		this.shareItem = content;
		this.dm = shareItem.dm;

		if (null != shareItem) {
			BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
			if (null != viewer) {
				viewer.setShareMode(true);
			}
			getDetailPanel().show(true, PAGE_ID);
			sendNowButton.setEnabled(true);
		}
	}

	public SelectedContent getShareItem() {
		return shareItem;
	}

	public void refresh() {
		/*
		 * Init the browser if it was not done already
		 */
		if (null == browser) {
			getBrowser();
		}

		/*
		 * Setting the button bar to Share mode
		 */
		ButtonBar buttonBar = (ButtonBar) SkinViewManager.get(ButtonBar.class);
		if (null != buttonBar) {
			buttonBar.setActiveMode(BuddiesViewer.share_mode);
		}

		BuddiesViewer viewer = (BuddiesViewer) SkinViewManager.get(BuddiesViewer.class);
		if (null != viewer) {
			setBuddies(viewer.getSelection());
		}

		if (null != dm && null != dm.getTorrent()) {
			Image img = null;

			byte[] imageBytes = PlatformTorrentUtils.getContentThumbnail(dm.getTorrent());
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

			} else {
				String path = dm == null ? null
						: dm.getDownloadState().getPrimaryFile();
				if (path != null) {
					img = ImageRepository.getPathIcon(path, true, dm.getTorrent() != null
							&& !dm.getTorrent().isSimpleTorrent());
					/*
					 * DO NOT dispose the image from .getPathIcon()!!!!
					 */
				}
			}

			if (null != img) {
				contentThumbnail.setImage(img);
			} else {
				Debug.out("Problem getting image for torrent in SharePage.refresh()");
			}

		} else {
			contentThumbnail.setImage(null);
		}
		updateContentStats();
	}

	private void updateContentStats() {
		contentStats.setText("");

		if (shareItem == null) {
			return;
		}

		contentStats.append(shareItem.displayName + "\n");

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

		contentStats.append("Published: "
				+ DateFormat.getDateInstance().format(
						new Date(
								PlatformTorrentUtils.getContentLastUpdated(dm.getTorrent())))
				+ "\n");
		//		contentStats.append("Published: " + PlatformTorrentUtils.getContentLastUpdated(dm.getTorrent())   + "\n");
		contentStats.append("File size: " + dm.getSize() / 1000000 + " MB");
	}
}
