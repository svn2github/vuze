/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
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

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.program.Program;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.ui.swt.ipchecker.IpCheckerWizard;
import org.gudy.azureus2.ui.swt.ipchecker.IpSetterCallBack;
import org.gudy.azureus2.ui.swt.MainWindow;

public class ConfigSectionTracker implements ConfigSectionSWT {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_ROOT;
  }

	public String configSectionGetName() {
		return "tracker";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    GridData gridData;
    GridLayout layout;
    Label label;

    // main tab set up
    Composite gMainTab = new Composite(parent, SWT.NULL);

    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gMainTab.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 6;
    gMainTab.setLayout(layout);
    
      // MAIN TAB DATA
    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollinterval");
    gridData = new GridData();
    label.setLayoutData( gridData );

    	// Poll Group
    
    Group gPollStuff = new Group(gMainTab, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 5;
    gPollStuff.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gPollStuff.setLayout(layout);

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmin");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMin = new IntParameter(gPollStuff, "Tracker Poll Interval Min", TRHost.DEFAULT_MIN_RETRY_DELAY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalMin.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalmax");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalMax = new IntParameter(gPollStuff, "Tracker Poll Interval Max", TRHost.DEFAULT_MAX_RETRY_DELAY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalMax.setLayoutData( gridData );

    // row

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincby");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncBy = new IntParameter(gPollStuff, "Tracker Poll Inc By", TRHost.DEFAULT_INC_BY );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalIncBy.setLayoutData( gridData );

    label = new Label(gPollStuff, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.pollintervalincper");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter pollIntervalIncPer = new IntParameter(gPollStuff, "Tracker Poll Inc Per", TRHost.DEFAULT_INC_PER );

    gridData = new GridData();
    gridData.widthHint = 30;
    pollIntervalIncPer.setLayoutData( gridData );

    
    // scrape + cache group
 
    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.scrapeandcache");
    gridData = new GridData();
    label.setLayoutData( gridData );
    
    Group gScrapeCache = new Group(gMainTab, SWT.NULL);
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 5;
    gScrapeCache.setLayoutData(gridData);
    layout = new GridLayout();
    layout.numColumns = 4;
    gScrapeCache.setLayout(layout);
    
    // row
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcescrapepercentage");
    gridData = new GridData();
    gridData.horizontalSpan	= 1;
    label.setLayoutData( gridData );

    IntParameter scrapeannouncepercentage = new IntParameter(gScrapeCache, "Tracker Scrape Retry Percentage", TRHost.DEFAULT_SCRAPE_RETRY_PERCENTAGE );

    gridData = new GridData();
    gridData.widthHint = 30;
    scrapeannouncepercentage.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.scrapecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter scrapeCachePeriod = new IntParameter(gScrapeCache, "Tracker Scrape Cache", TRHost.DEFAULT_SCRAPE_CACHE_PERIOD );

    gridData = new GridData();
    gridData.widthHint = 30;
    scrapeCachePeriod.setLayoutData( gridData );
    
 
    // row

    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcecacheminpeers");
    gridData = new GridData();
    gridData.horizontalSpan	= 1;
    label.setLayoutData( gridData );

    IntParameter announceCacheMinPeers = new IntParameter(gScrapeCache, "Tracker Announce Cache Min Peers", TRHost.DEFAULT_ANNOUNCE_CACHE_PEER_THRESHOLD );

    gridData = new GridData();
    gridData.widthHint = 30;
    announceCacheMinPeers.setLayoutData( gridData );
    
    label = new Label(gScrapeCache, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.announcecacheperiod");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter announceCachePeriod = new IntParameter(gScrapeCache, "Tracker Announce Cache", TRHost.DEFAULT_ANNOUNCE_CACHE_PERIOD );

    gridData = new GridData();
    gridData.widthHint = 30;
    announceCachePeriod.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.maxpeersreturned");
    gridData = new GridData();
    label.setLayoutData( gridData );

    IntParameter maxPeersReturned = new IntParameter(gMainTab, "Tracker Max Peers Returned", 100 );

    gridData = new GridData();
    gridData.widthHint = 50;
    maxPeersReturned.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.ip");

    final StringParameter tracker_ip = new StringParameter(gMainTab, "Tracker IP", "" );

    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalSpan = 2;
    tracker_ip.setLayoutData( gridData );

    Button check_button = new Button(gMainTab, SWT.PUSH);

    Messages.setLanguageText(check_button, "ConfigView.section.tracker.checkip"); //$NON-NLS-1$

    final Display display = gMainTab.getDisplay();

    check_button.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event event) {
          IpCheckerWizard wizard = new IpCheckerWizard(display);
          wizard.setIpSetterCallBack(new IpSetterCallBack() {
              public void setIp(final String ip) {
                if(display == null || display.isDisposed())
                  return;
                  display.asyncExec(new Runnable() {
                  public void run() {
                    if(tracker_ip != null)
                      tracker_ip.setValue(ip);
                  }
                });
              }
           }); // setIPSetterCallback
         }
    });

    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.port");

    IntParameter tracker_port = new IntParameter(gMainTab, "Tracker Port", TRHost.DEFAULT_PORT );

    gridData = new GridData();
    gridData.widthHint = 50;
    tracker_port.setLayoutData( gridData );

    final BooleanParameter nonsslEnable = new BooleanParameter(gMainTab, "Tracker Port Enable", true);

    Control[] non_ssl_controls = new Control[1];
    non_ssl_controls[0] = tracker_port.getControl();

    nonsslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( non_ssl_controls ));

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.forceport");

    BooleanParameter forcePortDetails = new BooleanParameter(gMainTab,  "Tracker Port Force External", false);
    label = new Label(gMainTab, SWT.NULL);


    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.sslport");

    IntParameter tracker_port_ssl = new IntParameter(gMainTab, "Tracker Port SSL", TRHost.DEFAULT_PORT_SSL );

    gridData = new GridData();
    gridData.widthHint = 50;
    tracker_port_ssl.setLayoutData( gridData );

    final BooleanParameter sslEnable = new BooleanParameter(gMainTab, "Tracker Port SSL Enable", false);

    Control[] ssl_controls = new Control[1];
    ssl_controls[0] = tracker_port_ssl.getControl();

    sslEnable.setAdditionalActionPerformer(new ChangeSelectionActionPerformer( ssl_controls ));

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.sslport.info");
    final String linkFAQ = "http://azureus.sourceforge.net/faq.php#19";
    label.setCursor(MainWindow.handCursor);
    label.setForeground(MainWindow.blue);
    label.addMouseListener(new MouseAdapter() {
       public void mouseDoubleClick(MouseEvent arg0) {
         Program.launch(linkFAQ);
       }
       public void mouseDown(MouseEvent arg0) {
         Program.launch(linkFAQ);
       }
    });
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    Control[] f_controls = new Control[1];
    f_controls[0] = forcePortDetails.getControl();

    IAdditionalActionPerformer f_enabler =
      new GenericActionPerformer(f_controls) {
        public void performAction()
        {
          boolean selected =  nonsslEnable.isSelected() ||
          sslEnable.isSelected();
    
          controls[0].setEnabled( selected );
        }
      };

    nonsslEnable.setAdditionalActionPerformer(f_enabler);
    sslEnable.setAdditionalActionPerformer(f_enabler);


    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.publicenable");

    BooleanParameter publicPublish = new BooleanParameter(gMainTab, "Tracker Public Enable", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    publicPublish.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenableweb");

    final BooleanParameter passwordEnableWeb = new BooleanParameter(gMainTab, "Tracker Password Enable Web", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    passwordEnableWeb.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent");

    final BooleanParameter passwordEnableTorrent = new BooleanParameter(gMainTab, "Tracker Password Enable Torrent", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    passwordEnableTorrent.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.passwordenabletorrent.info");

    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.username");

    final StringParameter tracker_username = new StringParameter(gMainTab, "Tracker Username", "" );

    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.widthHint = 100;
    tracker_username.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

     // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.password");

    final PasswordParameter tracker_password = new PasswordParameter(gMainTab, "Tracker Password" );

    gridData = new GridData();
    gridData.widthHint = 100;
    gridData.horizontalSpan = 2;
    tracker_password.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );


    Control[] x_controls = new Control[2];
    x_controls[0] = tracker_username.getControl();
    x_controls[1] = tracker_password.getControl();

    IAdditionalActionPerformer enabler =
        new GenericActionPerformer(x_controls)
            {
            public void performAction()
            {
              boolean selected =  passwordEnableWeb.isSelected() ||
                        passwordEnableTorrent.isSelected();

              for (int i=0;i<controls.length;i++){

                controls[i].setEnabled( selected );
              }
            }
            };

    passwordEnableWeb.setAdditionalActionPerformer(enabler);
    passwordEnableTorrent.setAdditionalActionPerformer(enabler);
    
    // row

    label = new Label(gMainTab, SWT.NULL);
    Messages.setLanguageText(label, "ConfigView.section.tracker.logenable");

    BooleanParameter logEnable = new BooleanParameter(gMainTab, "Tracker Log Enable", false);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    logEnable.setLayoutData( gridData );

    label = new Label(gMainTab, SWT.NULL);
    label = new Label(gMainTab, SWT.NULL);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData( gridData );

    return gMainTab;
  }
}
