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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
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
  
  Image workingImage;
  Image background;
  Font mainFont;
  Animator animator;    
  
  public DonationWindow2(Display display) {
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
    shell = new Shell(SWT.BORDER | SWT.APPLICATION_MODAL);
    FormLayout layout = new FormLayout();
    shell.setLayout(layout);
    
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("DonationWindow.title"));
    
    background = ImageRepository.getImage("donation");
    workingImage = new Image(display,background,SWT.IMAGE_COPY);
    
    Font tempFont = shell.getFont();
    FontData fontDataMain[] = tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {
      fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.4));
      fontDataMain[i].setStyle(SWT.BOLD);     
    }
    mainFont = new Font(display,fontDataMain);
    
    shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				if(shell == null || shell.isDisposed())
          return;
        paint();
			}
    });
    
    shell.addMouseListener(new MouseAdapter() {
  		public void mouseUp(MouseEvent arg0) {
  			close();
  		} 
    });
    
    ImageData data = background.getImageData();
    shell.setSize(data.width,data.height);
    Utils.centreWindow(shell);
    shell.open();
    
    animator = new Animator();
    animator.start();
  }
  
  private class Animator extends Thread {
   
    boolean ended = false;
    int nbchars = 0;
    
    public Animator() {
     super("Donation animator"); 
    }
    
    public void run() {
    	while(!ended) {
        if(display == null || display.isDisposed())
          return;
        display.asyncExec(new Runnable() {
          public void run() {
           Image tempImage = new Image(display,background,SWT.IMAGE_COPY);
          
           nbchars++;
           if(nbchars <= mainText.length()) {
            String textToSet = mainText.substring(0,nbchars);
            GC tempGC = new GC(tempImage);
            //tempGC.setForeground(MainWindow.white);
            if(mainFont == null || mainFont.isDisposed()) return;
            tempGC.setFont(mainFont);
            tempGC.drawText(textToSet,10,60,true);
            tempGC.dispose();
            Image oldImage = workingImage;
            workingImage = tempImage;
            if(oldImage != null && ! oldImage.isDisposed()) oldImage.dispose();
            paint();                        
           } else {
            ended = true;
            System.out.println("ended");
           }           
          }
        });
    		try {
    			Thread.sleep(30);          
        } catch (InterruptedException e) {
         ended = true; 
        }
      }
   }
    
    public void dispose() {
     ended = true; 
    }
  }
  
  private void close() {
   animator.dispose();
   mainFont.dispose();
   workingImage.dispose();
   shell.dispose();
  }
  
  private void paint() {
    if(shell == null || shell.isDisposed()) return;
    GC gcShell = new GC(shell);
    gcShell.drawImage(workingImage,0,0);
    gcShell.dispose();
  }
  
}
