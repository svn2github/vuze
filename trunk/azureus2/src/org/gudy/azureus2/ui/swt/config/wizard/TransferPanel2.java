/*
 * File    : TransferPanel.java
 * Created : 12 oct. 2003 19:41:14
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.config.wizard;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * @author Olivier
 * 
 */
public class 
TransferPanel2
	extends AbstractWizardPanel<ConfigureWizard> 
{
	private volatile boolean test_in_progress;

	private static final int kbit = 1000;
	private static final int mbit = 1000*1000;
	
	private static final int[] connection_rates = { 
		0,
		28800,
		56 * kbit,
		64 * kbit,
		96 * kbit,
		128 * kbit,
		192 * kbit,
		256 * kbit,
		384 * kbit,
		512 * kbit,
		640 * kbit,
		768 * kbit,
		1 * mbit,
		2 * mbit,
		5 * mbit,
		10 * mbit,
		20 * mbit,
		50 * mbit,
		100 * mbit,
	};
		
  Label nbMaxActive;
  Label nbMaxDownloads;

  
  private static final int upRates[] =
    {
      0,
      5,
      6,
      7,
      8,
      9,
      10,
      11,
      12,
      13,
      14,
      15,
      20,
      25,
      30,
      35,
      40,
      45,
      50,
      55,
      60,
      70,
      80,
      85,
      90,
      100,
      110,
      150,
      200,
      250,
      300,
      350,
      400,
      450,
      500,
      600,
      700,
      800,
      900,
      1000 };

  public TransferPanel2(ConfigureWizard wizard, IWizardPanel previous) {
    super(wizard, previous);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.wizard.IWizardPanel#show()
   */
  public void show() {
    wizard.setTitle(MessageText.getString("configureWizard.transfer.title"));
    wizard.setCurrentInfo(MessageText.getString("configureWizard.transfer2.hint"));
    Composite rootPanel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    rootPanel.setLayout(layout);

    Composite panel = new Composite(rootPanel, SWT.NULL);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    panel.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 2;
    panel.setLayout(layout);

    Label label = new Label(panel, SWT.WRAP);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "configureWizard.transfer2.message");

    final Group gRadio = new Group(panel, SWT.WRAP);
    Messages.setLanguageText(gRadio, "configureWizard.transfer2.group");
    gRadio.setLayoutData(gridData);
    gRadio.setLayout(new RowLayout(SWT.VERTICAL));
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 1;
    gRadio.setLayoutData(gridData);


    Button auto_button = new Button (gRadio, SWT.RADIO);
    Messages.setLanguageText(auto_button, "wizard.maketorrent.auto");
    auto_button.setSelection( true );
    
    final Button speed_test = new Button( gRadio, SWT.NULL );
    
    speed_test.setText( "Speed Test!" );
    
    speed_test.addSelectionListener(
    	new SelectionAdapter()
    	{
    		public void 
    		widgetSelected(
    			SelectionEvent arg0 ) 
    		{
    			speed_test.setEnabled( false );
    			
    			test_in_progress = true;
    			
				wizard.setNextEnabled( false );
				wizard.setPreviousEnabled( false );

				
    			UIFunctionsManager.getUIFunctions().installPlugin(
    				"mlab",
    				"dlg.install.mlab",
    				new UIFunctions.actionListener()
    				{
    					public void 
    					actionComplete(
    						Object result )
    					{
    						if ( result instanceof Boolean ){
    							
    							PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "mlab" );

    							IPCInterface callback = 
    								new IPCInterface()
	    							{
    									public Object 
    									invoke(
    										String 		methodName, 
    										Object[]	params )
    									{
    										try{
	    										if ( methodName.equals( "results" )){
	    											   											
		    										Map<String,Object> 	results = (Map<String,Object>)params[0];
		    										
		    										Long	up_rate = (Long)results.get( "up" );
		    										
		    										if ( up_rate != null ){
		    											
		    											final int u = up_rate.intValue();
		    											
		    											if ( u > 0 ){
		    												
		    												Utils.execSWTThread(
		    													new Runnable()
		    													{
		    														public void
		    														run()
		    														{
		    														}								
		    													});
		    											}
		    										}
	    										}
	    										
	    										return( null );
	    										
    										}finally{
    											
    											enableTest();
    										}
    									}
    								
    									public boolean 
    									canInvoke( 
    										String methodName, 
    										Object[] params )
    									{
    										return( true );
    									}
	    							};
    							
	    						try{
	    							pi.getIPC().invoke(
	    								"runTest",
	    								new Object[]{ new HashMap<String,Object>(), callback });
	    							
	    						}catch( Throwable e ){
	    							
	    							Debug.out( e );
	    							
	    							enableTest();
	    						}
    						}else{
    						
    							try{
    								Throwable error = (Throwable)result;
    							
    								Debug.out( error );
    								
    							}finally{
    								
    								enableTest();
    							}
    						}
    					}
    					
    					protected void
    					enableTest()
    					{
							Utils.execSWTThread(
									new Runnable()
									{
										public void 
										run() 
										{
											speed_test.setEnabled( true );
											
											test_in_progress = false;
											
											wizard.setNextEnabled( true );
											wizard.setPreviousEnabled( true );
											
										};
									});
    					}
    				});	
    		}
    	});
    
    final Button manual_button = new Button( gRadio, SWT.RADIO );
    Messages.setLanguageText(manual_button, "manual.mode");

    
    String connection_labels[] = new String[connection_rates.length];

    connection_labels[0] = "";
 
    String dial_up = MessageText.getString( "dial.up" );
    
    for (int i = 1; i < connection_rates.length; i++) {
    	
    	connection_labels[i] = (i<3?(dial_up+ " "):"xxx/") + DisplayFormatters.formatByteCountToBitsPerSec( connection_rates[i]/8);
    }
    
    final Combo connection_speed = new Combo(gRadio, SWT.SINGLE | SWT.READ_ONLY);
    connection_speed.setEnabled( false );
    
    for ( int i=0; i<connection_rates.length; i++ ){
    	
    	connection_speed.add(connection_labels[i]);
    }
    
    manual_button.addListener(
    	SWT.Selection,
    	new Listener()
    	{
    		public void 
    		handleEvent(
    			Event arg0 ) 
    		{
    			boolean is_manual = manual_button.getSelection();
    				
    			speed_test.setEnabled( !is_manual );
    			
    			connection_speed.setEnabled( is_manual );
    		}
    	});
 
  }


  
  public boolean 
  isNextEnabled() 
  {
    return( !test_in_progress );
  }
  
  public boolean 
  isPreviousEnabled() 
  {
    return( !test_in_progress );
  }
  
  public IWizardPanel 
  getNextPanel() 
  {
    return new NatPanel(((ConfigureWizard)wizard),this);
  }

}
