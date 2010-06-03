package org.gudy.azureus2.ui.swt.speedtest;



import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

public class 
SpeedTestSelector
	extends AbstractWizardPanel<SpeedTestWizard> 
{
	private boolean	mlab_test = true;

	public SpeedTestSelector(SpeedTestWizard wizard, IWizardPanel previous) {
		super(wizard, previous);
	}

	public void show() {
		wizard.setTitle(MessageText.getString("speedtest.wizard.select.title"));
		wizard.setCurrentInfo( "" );
		final Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

		Composite panel = new Composite(rootPanel, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		final Group gRadio = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gRadio, "speedtest.wizard.select.group");
		gRadio.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		gRadio.setLayout( layout );
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gRadio.setLayoutData(gridData);


		// general test

		Button auto_button = new Button (gRadio, SWT.RADIO);
		Messages.setLanguageText(auto_button, "speedtest.wizard.select.general");
		auto_button.setSelection( true );

		// BT

		final Button manual_button = new Button( gRadio, SWT.RADIO );
		Messages.setLanguageText(manual_button, "speedtest.wizard.select.bt");   

		manual_button.addListener(
				SWT.Selection,
				new Listener()
				{
					public void 
					handleEvent(
							Event arg0 ) 
					{
						mlab_test = !manual_button.getSelection();
					}
				});
	}



	public boolean 
	isNextEnabled() 
	{
		return( true );
	}

	public boolean 
	isPreviousEnabled() 
	{
		return( false );
	}

	public IWizardPanel 
	getNextPanel() 
	{
		if ( mlab_test ){

			wizard.close();

			new ConfigureWizard( false, true );
			
			return( null );

		}else{

			return( new SpeedTestPanel( wizard, null ));
		}
	}
}
