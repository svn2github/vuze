/*
 * File    : DonationWindow2.java
 * Created : 5 mars 2004
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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
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
public class DonationWindow2 {

  private Display display;
  private Shell shell;
  private Button ok;
  
  private String headerText,mainText,footerText;
  int timeToWait;
  OverallStats stats;
  
  Image workingImage;
  Image background;
  
  Font mainFont;
  Font smallFont;
  Font mediumFont;
  Animator animator;    
  PaintListener listener;
  
  private static final String donationUrl = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&no_note=1&tax=0&currency_code=EUR";
  private static final String donationUrlShort = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&currency_code=EUR";
  
  public DonationWindow2(Display display) {
      this.display = display;   
      stats = StatsFactory.getStats();
      
      mainText = MessageText.getString("DonationWindow.text");
      footerText = MessageText.getString("DonationWindow.text.footer");
      
      timeToWait = mainText.length() / 29;
  }  

  public void show() {
    shell = new Shell(SWT.BORDER | SWT.APPLICATION_MODAL);
    FormLayout layout = new FormLayout();
    shell.setLayout(layout);
    
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("DonationWindow.title"));
    shell.setBackground(MainWindow.white);
    
    background = ImageRepository.getImage("donation");    
    
    Font tempFont;
    FontData fontDataMain[];
    
    tempFont = shell.getFont();
    fontDataMain = tempFont.getFontData();
    
    boolean isMacLinux = (System.getProperty("os.name").equals("Mac OS X") ||
                          System.getProperty("os.name").equals("Linux"));
    
    
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(!isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.4));
      fontDataMain[i].setStyle(SWT.BOLD);     
    }
    mainFont = new Font(display,fontDataMain);
    
    tempFont = shell.getFont();
    fontDataMain= tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(!isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.2));
      //fontDataMain[i].setStyle(SWT.BOLD);     
    }
    mediumFont = new Font(display,fontDataMain);
    
    tempFont = shell.getFont();
    fontDataMain = tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      if(isMacLinux)
      	fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 0.70));
      else
        fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 0.90));
    }
    smallFont = new Font(display,fontDataMain);
    
    listener = new PaintListener() {
      public void paintControl(PaintEvent event) {
        if(shell == null || shell.isDisposed())
          return;
        paint();
      }};
      
    shell.addPaintListener(listener);
    
    /*shell.addMouseListener(new MouseAdapter() {
  		public void mouseUp(MouseEvent arg0) {
  			close();
  		} 
    });*/
    
    ImageData data = background.getImageData();
    shell.setSize(data.width,data.height);        
    Utils.centreWindow(shell);
    
    addControls();
    
    shell.open();
    
    animator = new Animator();
    animator.start();
  }
  
  private class Animator extends Thread {
   
    boolean ended = false;
    boolean drawingDone;
    int nbchars = 0;
    
    public Animator() {
     super("Donation animator"); 
    }
    
    public void run() {
    	while(!ended) {
        if(display == null || display.isDisposed())
          return;
        drawingDone = false;
        display.asyncExec(new Runnable() {
          public void run() {
           Image tempImage = new Image(display,background,SWT.IMAGE_COPY);
          
           nbchars++;
           if(nbchars <= mainText.length()) {
            String textToSet = mainText.substring(0,nbchars);
            GC tempGC = new GC(tempImage);
            if(mainFont == null || mainFont.isDisposed()) return;
            tempGC.setFont(mainFont);
            tempGC.drawText(DisplayFormatters.formatByteCountToKiBEtc(stats.getDownloadedBytes()),80,14,true);
            tempGC.drawText(DisplayFormatters.formatByteCountToKiBEtc(stats.getUploadedBytes()),235,14,true);
            tempGC.drawText(stats.getUpTime() / (60*60) + " " + MessageText.getString("DonationWindow.text.hours"),465,14,true);
            tempGC.drawText(textToSet,10,60,true);
            tempGC.setFont(null);
            tempGC.drawText(MessageText.getString("DonationWindow.text.downloaded"),70,32,true);
            tempGC.drawText(MessageText.getString("DonationWindow.text.uploaded"),235,32,true);
            tempGC.dispose();
            Image oldImage = workingImage;
            workingImage = tempImage;

            if(oldImage != null && ! oldImage.isDisposed()) oldImage.dispose();
            paint();                        
           } else {
            ended = true;            
           }  
           drawingDone = true;
          }
        });
    		try {
          Thread.sleep(30);
          while(!drawingDone)
            Thread.sleep(20);     
        } catch (InterruptedException e) {
         ended = true; 
        }            
      } 
      enableOk();
   }
    
    public void dispose() {
     ended = true; 
    }
  }
  
  private void close() {
   animator.dispose();
   mainFont.dispose();
   mediumFont.dispose();
   smallFont.dispose();
   workingImage.dispose();
   shell.dispose();
  }
  
  private void paint() {
    if(shell == null || shell.isDisposed()) return;
    if(workingImage == null || workingImage.isDisposed()) return;
    GC gcShell = new GC(shell);
    gcShell.drawImage(workingImage,0,0);
    gcShell.dispose();
  }
  
  private void enableOk() {
    if(display == null || display.isDisposed()) return;
     display.asyncExec(new Runnable() {
       public void run() {
         if(shell == null || shell.isDisposed())
           return;
         ok.setEnabled(true);
       }
    });
  }
  
  private void addControls() {
   /*if(display == null || display.isDisposed()) return;
   display.asyncExec(new Runnable() {
		public void run() {
      if(shell == null || shell.isDisposed())
        return;     */       
                  
      FormData formData;
      
      
      final Button radioDonate = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioDonate,"DonationWindow.options.donate");
      radioDonate.setFont(mainFont);
      radioDonate.setBackground(MainWindow.white);
      formData = new FormData();
      formData.top = new FormAttachment(65);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioDonate.setLayoutData(formData);        
      
      
      final Label textFooter = new Label(shell,SWT.NULL);    
      textFooter.setFont(smallFont);
      textFooter.setText(footerText);
      textFooter.setForeground(MainWindow.black);
      textFooter.setBackground(MainWindow.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioDonate);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      textFooter.setLayoutData(formData);
      
      
      final Button radioNoDonate = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioNoDonate,"DonationWindow.options.nodonate");
      radioNoDonate.setFont(mediumFont);
      radioNoDonate.setBackground(MainWindow.white);
      formData = new FormData();
      formData.top = new FormAttachment(textFooter);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioNoDonate.setLayoutData(formData);
      
      final Button radioLater = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioLater,"DonationWindow.options.later");
      radioLater.setFont(mediumFont);
      radioLater.setBackground(MainWindow.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioNoDonate);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioLater.setLayoutData(formData);
      
      final Button radioAlready = new Button(shell,SWT.RADIO);
      Messages.setLanguageText(radioAlready,"DonationWindow.options.already");      
      radioAlready.setFont(mediumFont);
      radioAlready.setBackground(MainWindow.white);
      formData = new FormData();
      formData.top = new FormAttachment(radioLater);
      formData.left = new FormAttachment(0,140);
      formData.right = new FormAttachment(100,-5);
      radioAlready.setLayoutData(formData);
      
      
      final Text textForCopy = new Text(shell,SWT.BORDER);
      textForCopy.setText(donationUrlShort);      
      textForCopy.setFont(smallFont);
      formData = new FormData();
      formData.bottom = new FormAttachment(100,-7);
      formData.left = new FormAttachment(0,5);
      textForCopy.setLayoutData(formData);
      
      
      //By default, donate is selected (of course)
      radioDonate.setSelection(true);    
      
      ok = new Button(shell,SWT.PUSH);     
      ok.setEnabled(false);
      Messages.setLanguageText(ok,"DonationWindow.ok");
      
      formData = new FormData();
      formData.bottom = new FormAttachment(100,-5);
      formData.right = new FormAttachment(100,-5);
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
            close();
          }       
        }
      });
      
      shell.layout();
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
