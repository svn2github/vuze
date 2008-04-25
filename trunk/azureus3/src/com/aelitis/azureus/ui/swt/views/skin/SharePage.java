package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.util.Constants;

public class SharePage
	implements IDetailPage
{

	public static final String PAGE_ID = "share.page";

	private Composite content;

	private StackLayout stackLayout;

	private Composite firstPanel = null;

	private Composite browserPanel = null;

	static Label shareMessage;

	private Label buddyListDescription;

	private Label addBuddyLabel;

	private Composite buddyList;

	private Composite contentDetail;

	private Button addBuddyButton;

	private Button sendNowButton;

	private Button cancelButton;

	private Label buddyImage;

	private Label commentLabel;

	private Text commentText;

	public SharePage() {
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);

		stackLayout = new StackLayout();
		stackLayout.marginHeight = 0;
		stackLayout.marginWidth = 0;
		content.setLayout(stackLayout);

		createFirstPanel();
		createBrowserPanel();
	}

	private void createFirstPanel() {
		firstPanel = new Composite(content, SWT.NONE);
		//		firstPanel.setBackground(ColorCache.getColor(parent.getDisplay(), 12, 30, 67));
		firstPanel.setLayout(new FormLayout());
		//		firstPanel.setBackground(Colors.black);

		shareMessage = new Label(firstPanel, SWT.NONE);
		shareMessage.setText("Share this content...");
		shareMessage.setForeground(Colors.white);

		buddyListDescription = new Label(firstPanel, SWT.NONE);
		buddyListDescription.setText("Selected buddies");
		buddyListDescription.setForeground(Colors.white);

		addBuddyLabel = new Label(firstPanel, SWT.NONE | SWT.WRAP | SWT.RIGHT);
		addBuddyLabel.setText("Invite more buddies to share with");
		addBuddyLabel.setForeground(Colors.white);

		buddyList = new Composite(firstPanel, SWT.BORDER);
		contentDetail = new Composite(firstPanel, SWT.BORDER);

		addBuddyButton = new Button(firstPanel, SWT.PUSH);
		addBuddyButton.setText("Add Buddy");

		sendNowButton = new Button(firstPanel, SWT.PUSH);
		sendNowButton.setText("Send Now");

		cancelButton = new Button(firstPanel, SWT.PUSH);
		cancelButton.setText("&Cancel");

		FormData shareMessageData = new FormData();
		shareMessageData.top = new FormAttachment(0, 8);
		shareMessageData.left = new FormAttachment(0, 8);
		shareMessageData.right = new FormAttachment(100, -8);
		shareMessage.setLayoutData(shareMessageData);

		FormData buddyListDescriptionData = new FormData();
		buddyListDescriptionData.top = new FormAttachment(shareMessage, 8);
		buddyListDescriptionData.left = new FormAttachment(buddyList, 0, SWT.LEFT);
		buddyListDescription.setLayoutData(buddyListDescriptionData);

		FormData buddyListData = new FormData();
		buddyListData.top = new FormAttachment(buddyListDescription, 0);
		buddyListData.left = new FormAttachment(0, 30);
		buddyListData.width = 200;
		buddyListData.height = 250;
		buddyList.setLayoutData(buddyListData);

		FormData contentDetailData = new FormData();
		contentDetailData.top = new FormAttachment(buddyList, 0, SWT.TOP);
		contentDetailData.left = new FormAttachment(buddyList, 30);
		contentDetailData.right = new FormAttachment(100, -8);
		contentDetailData.bottom = new FormAttachment(buddyList, 0, SWT.BOTTOM);
		contentDetail.setLayoutData(contentDetailData);

		FormData addBuddyButtonData = new FormData();
		addBuddyButtonData.top = new FormAttachment(buddyList, 8);
		addBuddyButtonData.right = new FormAttachment(buddyList, 0, SWT.RIGHT);
		addBuddyButton.setLayoutData(addBuddyButtonData);

		FormData addBuddyLabelData = new FormData();
		addBuddyLabelData.top = new FormAttachment(buddyList, 8);
		addBuddyLabelData.right = new FormAttachment(addBuddyButton, -8);
		addBuddyLabelData.left = new FormAttachment(buddyList, 0, SWT.LEFT);
		addBuddyLabel.setLayoutData(addBuddyLabelData);

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

		buddyImage = new Label(contentDetail, SWT.BORDER);
		FormData buddyImageData = new FormData();
		buddyImageData.top = new FormAttachment(0, 8);
		buddyImageData.left = new FormAttachment(0, 8);
		buddyImageData.width = 100;
		buddyImageData.height = 100;
		buddyImage.setLayoutData(buddyImageData);

		commentLabel = new Label(contentDetail, SWT.NONE);
		commentLabel.setText("Optional message:");
		commentLabel.setForeground(Colors.white);
		FormData commentLabelData = new FormData();
		commentLabelData.top = new FormAttachment(buddyImage, 16);
		commentLabelData.left = new FormAttachment(0, 8);
		commentLabel.setLayoutData(commentLabelData);

		commentText = new Text(contentDetail, SWT.BORDER);
		FormData commentTextData = new FormData();
		commentTextData.top = new FormAttachment(commentLabel, 16);
		commentTextData.left = new FormAttachment(0, 8);
		commentTextData.right = new FormAttachment(100, -8);
		commentTextData.bottom = new FormAttachment(100, -8);
		commentText.setLayoutData(commentTextData);

		stackLayout.topControl = firstPanel;
		content.layout();

		hookListeners();

	}

	private void createBrowserPanel() {
		browserPanel = new Composite(content, SWT.NONE);
		FillLayout fLayout = new FillLayout();
		browserPanel.setLayout(fLayout);
		Browser browser = new Browser(browserPanel, SWT.NONE);
		browser.setUrl(Constants.URL_FAQ);
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
	}

	public Control getControl() {
		return content;
	}

	public String getPageID() {
		return PAGE_ID;
	}
}
