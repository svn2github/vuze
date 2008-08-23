package org.gudy.azureus2.ui.swt.shells;

abstract public class AbstractWizardPage
	implements IWizardPage
{

	private MultipageWizard wizard;

	public AbstractWizardPage(MultipageWizard wizard) {
		this.wizard = wizard;
	}

	public MultipageWizard getWizard() {
		return wizard;
	}

	public String getDesciption() {
		return null;
	}

	public String getTitle() {
		return null;
	}

	public String getWindowTitle() {
		return null;
	}

	public boolean isComplete() {
		return false;
	}

	public boolean isLastPage() {
		return false;
	}

	public void performDispose() {
	}

	public void performFinish() {
	}

	public boolean setComplete() {
		return false;
	}

	public void performAboutToBeHidden() {
	}

	public void performAboutToBeShown() {
	}

	public void createCustomButtons(){
		
	}
}
