package com.aelitis.azureus.ui.swt.shells.friends;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.shells.AbstractWizardPage;
import org.gudy.azureus2.ui.swt.shells.MultipageWizard;

public class SharePage
	extends AbstractWizardPage
{
	private static final String ID = "share.wizard.page";

	private Composite content;

	public SharePage(MultipageWizard wizard) {
		super(wizard);
	}

	public void createControls(Composite parent) {
		content = new Composite(parent, SWT.NONE);

		Label label = new Label(content, SWT.BORDER);
		label.setBounds(10, 10, 100, 20);
		label.setText("This is a label!!!!!!");
	}

	public Control getControl() {
		return content;
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

	public void createCustomButtons() {
		getWizard().createButton("test.button", null);
	}
}
