package com.aelitis.azureus.ui.swt.shells.friends;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;

public class ShareWizard
	extends MultipageWizard
{

	public ShareWizard(Display display, int shellStyle) {
		super(display, shellStyle);
	}

	public ShareWizard(Shell parent, int shellStyle) {
		super(parent, shellStyle);
	}

	public void createPages() {
		addPage(new SharePage(this));
		addPage(new AddFriendsPage(this));
	}

}
