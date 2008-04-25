package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class InvitePage
{
	
	public static final String PAGE_ID ="invite.page";
	
	private Composite parent;
	private StackLayout stackLayout;
	private Composite firstPanel = null;
	
	public InvitePage(Composite parent) {
		this.parent = parent;
		this.parent = parent;

		stackLayout = new StackLayout();
		stackLayout.marginHeight = 0;
		stackLayout.marginWidth = 0;
		parent.setLayout(stackLayout);
		
		init();
	}

	private void init() {
		firstPanel = new Composite(parent, SWT.NONE);
		firstPanel.setBackground(ColorCache.getColor(parent.getDisplay(), 75, 2, 10));
		stackLayout.topControl = firstPanel;
		parent.layout();
	}
}
