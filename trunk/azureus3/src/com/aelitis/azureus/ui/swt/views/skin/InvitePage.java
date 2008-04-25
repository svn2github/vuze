package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.aelitis.azureus.util.Constants;

public class InvitePage
	implements IDetailPage
{

	public static final String PAGE_ID = "invite.page";

	private Composite content;

	private Composite blinder;

	private Browser browser = null;

	private StackLayout stackLayout = null;

	public InvitePage() {
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		stackLayout = new StackLayout();
		content.setLayout(stackLayout);
		init();
	}

	private void init() {
		browser = new Browser(content, SWT.NONE);
		browser.setUrl(Constants.URL_FAQ);

		blinder = new Composite(content, SWT.NONE);
		stackLayout.topControl = browser;
		content.layout();
	}

	public String getPageID() {
		return PAGE_ID;
	}

	public Control getControl() {
		return content;
	}
}
