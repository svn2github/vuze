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
import org.eclipse.swt.custom.CCombo;
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
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.Constants;
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
  private static final String donationUrlShort = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&currency_code=EUR";
  
  private String headerText;
  private String mainText;
  private String footerText;
  
  public DonationWindow(Display display) {
   this.display = display;   
   OverallStats stats = StatsFactory.getStats();
   headerText = MessageText.getString("DonationWindow.text.time") + " " +(stats.getUpTime() / (60*60))
              + " " + MessageText.getString("DonationWindow.text.hours_downloaded") + " " + DisplayFormatters.formatByteCountToKiBEtc(stats.getDownloadedBytes())
              + " " + MessageText.getString("DonationWindow.text.uploaded") + " " + DisplayFormatters.formatByteCountToKiBEtc(stats.getUploadedBytes()) + "\n";
              
   mainText = MessageText.getString("DonationWindow.text");
   footerText = MessageText.getString("DonationWindow.text.footer");
   
   timeToWait = mainText.length() / 29;
  }
  
  public void show() {
    shell = new Shell(display,SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
    
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("DonationWindow.title"));
    //shell.setBackground(MainWindow.white);
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 5;
    layout.marginWidth = 5;
    layout.spacing = 5;
    FormData formData;
    shell.setLayout(layout);    
    
    final Label textHeader = new Label(shell,SWT.NULL);    
    formData = new FormData();
    formData.right = new FormAttachment(100);
    formData.left = new FormAttachment(0);
    textHeader.setLayoutData(formData);
    
    textHeader.setText(headerText);        
    Font fontHeader = textHeader.getFont();
    FontData fontDataHeader[] = fontHeader.getFontData();
    for(int i=0 ; i < fontDataHeader.length ; i++) {
      fontDataHeader[i].setHeight((int) (fontDataHeader[i].getHeight() * 1.2));
      fontDataHeader[i].setStyle(SWT.BOLD);     
    }
    textHeader.setFont(new Font(display,fontDataHeader));
    textHeader.setBackground(MainWindow.white);   
        
    final Label textMain = new Label(shell,SWT.NULL);    
    textMain.setText(mainText);
    Font fontMain = textMain.getFont();
    FontData fontDataMain[] = fontMain.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.4));
      fontDataMain[i].setStyle(SWT.BOLD);     
    }
    textMain.setFont(new Font(display,fontDataMain));
    textMain.setForeground(MainWindow.blues[4]);
    //textMain.setBackground(MainWindow.blues[0]);
    formData = new FormData();
    formData.top = new FormAttachment(textHeader);
    formData.left = new FormAttachment(0);
    formData.right = new FormAttachment(100);    
    textMain.setLayoutData(formData);
    
    final Button radioDonate = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioDonate,"DonationWindow.options.donate");
    Font fontDonate = radioDonate.getFont();
    FontData fontDataDonate[] = fontDonate.getFontData();
    for(int i=0 ; i < fontDataDonate.length ; i++) {
      fontDataDonate[i].setHeight((int) (fontDataDonate[i].getHeight() * 1.4));
      fontDataDonate[i].setStyle(SWT.BOLD); 
    }
    radioDonate.setFont(new Font(display,fontDataDonate));
    formData = new FormData();
    formData.top = new FormAttachment(textMain);
    radioDonate.setLayoutData(formData);        
    
    
    final Label textFooter = new Label(shell,SWT.NULL);    
    textFooter.setText(footerText);
    textFooter.setForeground(MainWindow.black);
    formData = new FormData();
    formData.top = new FormAttachment(radioDonate);
    formData.left = new FormAttachment(4,0);
    textFooter.setLayoutData(formData);
   
    
    final Button radioNoDonate = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioNoDonate,"DonationWindow.options.nodonate");
    Font fontNoDonate = radioNoDonate.getFont();
    FontData fontDataNoDonate[] = fontNoDonate.getFontData();
    for(int i=0 ; i < fontDataNoDonate.length ; i++) {
      fontDataNoDonate[i].setHeight((int) (fontDataNoDonate[i].getHeight() * 1.3));
      fontDataNoDonate[i].setStyle(SWT.BOLD); 
    }
    radioNoDonate.setFont(new Font(display,fontDataNoDonate));
    formData = new FormData();
    formData.top = new FormAttachment(textFooter);
    radioNoDonate.setLayoutData(formData);
    
    final Button radioLater = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioLater,"DonationWindow.options.later");
    Font fontLater = radioLater.getFont();
    FontData fontDataLater[] = fontLater.getFontData();
    for(int i=0 ; i < fontDataLater.length ; i++) {
      fontDataLater[i].setHeight((int) (fontDataLater[i].getHeight() * 1.3));
      fontDataLater[i].setStyle(SWT.BOLD); 
    }
    radioLater.setFont(new Font(display,fontDataLater));
    formData = new FormData();
    formData.top = new FormAttachment(radioNoDonate);
    radioLater.setLayoutData(formData);
    
    final Button radioAlready = new Button(shell,SWT.RADIO);
    Messages.setLanguageText(radioAlready,"DonationWindow.options.already");
    Font fontAlready = radioAlready.getFont();
    FontData fontDataAlready[] = fontAlready.getFontData();
    for(int i=0 ; i < fontDataAlready.length ; i++) {
      fontDataAlready[i].setHeight((int) (fontDataAlready[i].getHeight() * 1.3));
      fontDataAlready[i].setStyle(SWT.BOLD); 
    }
    radioAlready.setFont(new Font(display,fontDataAlready));
    formData = new FormData();
    formData.top = new FormAttachment(radioLater);
    radioAlready.setLayoutData(formData);
    
    
    final Text textForCopy = new Text(shell,SWT.BORDER);
    textForCopy.setText(donationUrlShort);
    Font fontCopy = textForCopy.getFont();
    FontData fontDataCopy[] = fontCopy.getFontData();
    for(int i=0 ; i < fontDataCopy.length ; i++) {
      fontDataCopy[i].setHeight((int) (fontDataCopy[i].getHeight() * 0.9));
    }
    textForCopy.setFont(new Font(display,fontDataCopy));
    formData = new FormData();
    formData.top = new FormAttachment(radioAlready);
    textForCopy.setLayoutData(formData);
    
    
    //By default, donate is selected (of course)
    radioDonate.setSelection(true);    
    
    ok = new Button(shell,SWT.PUSH);
    ok.setEnabled(false);
    
    formData = new FormData();
    formData.top = new FormAttachment(radioAlready);
    formData.right = new FormAttachment(95,5);
    formData.width = 100;
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
        if(!radioDonate.getSelection()) {
          shell.dispose();
        }       
			}
    });
    shell.pack();    
    shell.open();
    textMain.setText("");
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
                    if(cutAt <= mainText.length()) {
                      textMain.setText(mainText.substring(0,cutAt));
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
              textMain.setText(mainText);
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
    COConfigurationManager.setParameter("donations.donated",true);    
    COConfigurationManager.save();
  }
  
  private void stopAsking() {
   COConfigurationManager.setParameter("donations.nextAskTime",-1);
   COConfigurationManager.setParameter("donations.lastVersion",Constants.AZUREUS_VERSION);
   COConfigurationManager.save();
  }
}
