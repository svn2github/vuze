/*
 * File    : DonationWindow.java
 * Created : 1 mars 2004
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
package org.gudy.azureus2.ui.swt.donations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * @author Olivier
 * 
 */
public class DonationWindow {
  
  private Display display;
  private Shell shell;
  private Button ok;
  private int timeToWait;
  private static final String donationUrl = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&no_note=1&tax=0&currency_code=EUR";
  private String fullText;
  
  public DonationWindow(Display display) {
   this.display = display;   
   OverallStats stats = StatsFactory.getStats();
   fullText = MessageText.getString("DonationWindow.text.time") + (stats.getUpTime() / (60*60))
              + MessageText.getString("DonationWindow.text.hours_downloaded") + DisplayFormatters.formatByteCountToKiBEtc(stats.getDownloadedBytes())
              + MessageText.getString("DonationWindow.text.uploaded") + DisplayFormatters.formatByteCountToKiBEtc(stats.getUploadedBytes()) + "\n"
              + MessageText.getString("DonationWindow.text");
   timeToWait = fullText.length() / 25 ;
  }
  
  public void show() {
    shell = new Shell(display,SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
    
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("DonationWindow.title"));
    
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 5;
    layout.marginWidth = 5;
    layout.spacing = 5;
    FormData formData;
    shell.setLayout(layout);
    
    
    final Label text = new Label(shell,SWT.NULL);    
    text.setText(fullText);
    Font font = text.getFont();
    FontData fontData[] = font.getFontData();
    for(int i=0 ; i < fontData.length ; i++) {
      fontData[i].setHeight((int) (fontData[i].getHeight() * 1.2));
      fontData[i].setStyle(SWT.BOLD);     
    }
    text.setFont(new Font(display,fontData));
    text.setForeground(MainWindow.blues[4]);
    text.setBackground(MainWindow.blues[0]);
    
    
    final Button radioDonate = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioDonate,"DonationWindow.options.donate");        
    formData = new FormData();
    formData.top = new FormAttachment(text);
    radioDonate.setLayoutData(formData);        
    
    final Button radioNoDonate = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioNoDonate,"DonationWindow.options.nodonate");
    formData = new FormData();
    formData.top = new FormAttachment(radioDonate);
    radioNoDonate.setLayoutData(formData);
    
    final Button radioLater = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioLater,"DonationWindow.options.later");
    formData = new FormData();
    formData.top = new FormAttachment(radioNoDonate);
    radioLater.setLayoutData(formData);
    
    final Button radioAlready = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioAlready,"DonationWindow.options.already");
    formData = new FormData();
    formData.top = new FormAttachment(radioLater);
    radioAlready.setLayoutData(formData);
    
    //By default, donate is selected (of course)
    radioDonate.setSelection(true);    
    
    ok = new Button(shell,SWT.PUSH);
    ok.setEnabled(false);
    
    formData = new FormData();
    formData.top = new FormAttachment(radioAlready);
    formData.right = new FormAttachment(100,0);
    formData.width = 70;
    ok.setLayoutData(formData);
    
    
    
    
    ok.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {				
        if(radioDonate.getSelection()) {
         Program.launch(donationUrl);
        }
        if(radioAlready.getSelection()) {
         thanks();
         stopAsking(); 
        }
        if(radioNoDonate.getSelection()){
         stopAsking(); 
        }
        shell.dispose();
			}
    });
    shell.pack();    
    shell.open();
    text.setText("");
    Utils.centreWindow(shell);
    
    new Thread() {
      public void run() {
        long initialTime = System.currentTimeMillis() / 1000;
        int lastTime = timeToWait;
        int nbChars = 0;
        while(timeToWait>0) {
          try {
            nbChars++;
            final int cutAt = nbChars;          
            timeToWait = lastTime - (int)(System.currentTimeMillis() / 1000 - initialTime);
            if(shell.isDisposed()) {
              timeToWait = 0;           
            } else {
              if(display != null && ! display.isDisposed()) {
                display.asyncExec(new Runnable() {
                  public void run() {
                    if(cutAt <= fullText.length()) {
                    	text.setText(fullText.substring(0,cutAt));
                    }
                    ok.setText(MessageText.getString("DonationWindow.ok.waiting") +  " " + timeToWait);
                  }
                });
              }
            }
            Thread.sleep(30);          
          } catch(Exception e) {
            //End the loop
            timeToWait = 0;
            e.printStackTrace();
          }
        }
        if(display != null && ! display.isDisposed()) {
          display.asyncExec(new Runnable() {
            public void run() {
              text.setText(fullText);
              Messages.setLanguageText(ok,"DonationWindow.ok");
              ok.setEnabled(true);
            }
          });
        }
      }
    }.start();
  }

  private void thanks() {
    MessageBox msgThanks = new MessageBox(shell,SWT.OK);
    msgThanks.setText(MessageText.getString("DonationWindow.thanks.title"));
    msgThanks.setMessage(MessageText.getString("DonationWindow.thanks.text"));
    msgThanks.open();
  }
  
  private void stopAsking() {
   COConfigurationManager.setParameter("donations.keepAsking",false); 
  }
}
