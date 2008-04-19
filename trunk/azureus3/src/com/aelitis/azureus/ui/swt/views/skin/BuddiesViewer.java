package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddyUtils;

public class BuddiesViewer
{
	private Composite avatarsPanel = null;

	private Composite parent = null;

	private int borderWidth = 3;

	private Point avatarImageSize = new Point(32 + borderWidth, 32 + borderWidth);

	private Point avatarSize = new Point(128, 52);

	public BuddiesViewer(Composite parent) {
		this.parent = parent;

		init();
	}

	private void init() {
		if (null == parent || true == parent.isDisposed()) {
			throw new NullPointerException(
					"The variable 'parent' can not be null or disposed");
		}

		avatarsPanel = new Composite(parent, SWT.NONE);

		RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
		rLayout.wrap = false;
		rLayout.spacing = 10;
		rLayout.marginTop = 0;
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		avatarsPanel.setLayout(rLayout);
		
		fillBuddies(avatarsPanel);

		parent.layout(true);

		
		Listener l = new Listener() {
			int startX, startY;

			public void handleEvent(Event e) {
				
				if (e.type == SWT.MouseDown && e.button == 1 && (e.stateMask & SWT.CONTROL) != 0) {
					startX = e.x;
					startY = e.y;
					avatarsPanel.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
					System.out.println("Mouse down");
				}
				if (e.type == SWT.MouseMove && (e.stateMask & SWT.BUTTON1) != 0 && (e.stateMask & SWT.CONTROL) != 0) {
					Point p = avatarsPanel.toDisplay(e.x, e.y);
					p.x -= startX;
					p.y -= startY;
//					content.setLocation(p);
					System.err.println("X:" + p.x);
					System.out.println("Mouse moving...");
				}
				if(e.type == SWT.MouseUp){
					avatarsPanel.setCursor(null);
					System.out.println("Mouse up");
				}
				
				if (e.type == SWT.KeyDown && (e.stateMask & SWT.BUTTON1) != 0 && (e.stateMask & SWT.CONTROL) != 0) {
					avatarsPanel.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				}
			}
		};
		avatarsPanel.addListener(SWT.MouseDown, l);
		avatarsPanel.addListener(SWT.MouseMove, l);
		avatarsPanel.addListener(SWT.MouseUp, l);
		avatarsPanel.addListener(SWT.KeyDown, l);
		
		
		
	}

	private void fillBuddies(Composite composite) {

		VuzeBuddySWT[] buddies = getBuddies();

		for (int i = 0; i < buddies.length; i++) {
			VuzeBuddySWT vuzeBuddy = buddies[i];
			createBuddyControl(composite, vuzeBuddy);
		}

	}

	private AvatarWidget createBuddyControl(Composite composite,
			final VuzeBuddySWT vuzeBuddy) {
		AvatarWidget avatarWidget = new AvatarWidget(composite, avatarSize,
				avatarImageSize, vuzeBuddy);
		avatarWidget.setBorderWidth(borderWidth);
		
		RowData rData = new RowData();
		rData.width = avatarSize.x;
		rData.height = avatarSize.y;
		avatarWidget.getControl().setLayoutData(rData);

		return avatarWidget;
	}

	private VuzeBuddySWT[] getBuddies() {
		VuzeBuddySWT[] buddies = new VuzeBuddySWT[10];
		for (int i = 0; i < buddies.length; i++) {
			buddies[i] = (VuzeBuddySWT) VuzeBuddyUtils.createRandomBuddy();
		}

		return buddies;
	}
}
