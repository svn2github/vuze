package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class SharePage
{
	
	public static final String PAGE_ID ="share.page";
	
	private Composite parent;

	private StackLayout stackLayout;
	
	private Composite firstPanel = null;
	
	static Label shareMessage;

	static Label buddyListDescription;

	static Label addBuddyLabel;

	static Composite buddyList;

	static Composite contentDetail;

	static Button addBuddyButton;

	static Button sendNowButton;

	static Button cancelButton;

	static Label buddyImage;

	static Label commentLabel;

	static Text commentText;
	
	public SharePage(Composite parent) {
		this.parent = parent;

		stackLayout = new StackLayout();
		stackLayout.marginHeight = 0;
		stackLayout.marginWidth = 0;
		parent.setLayout(stackLayout);
		
		init();
	}

	private void init() {
		firstPanel = new Composite(parent, SWT.NONE);
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
		parent.layout();
		
		
	}
}
