/*
 * File    : ErrorPopupShell.java
 * Created : 15 mars 2004
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
package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.animations.Animator;
import org.gudy.azureus2.ui.swt.animations.shell.AnimableShell;
import org.gudy.azureus2.ui.swt.animations.shell.LinearAnimator;


/**
 * @author Olivier Chalouhi
 *
 */
public class MessagePopupShell implements AnimableShell {
  
  private Shell shell;
  private Shell detailsShell;  
  Image shellImg;
  private Display display;  
  
  public static final String ICON_ERROR = "error";
  
  public MessagePopupShell(Display display,String icon,String title,String errorMessage,String details) {    
    this.display = display;    
    detailsShell = new Shell(display,SWT.BORDER | SWT.ON_TOP);
    detailsShell.setImage(ImageRepository.getImage("azureus"));
    
    detailsShell.setLayout(new FillLayout());
    StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);  
    textDetails.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    if(details != null)
      textDetails.setText(details);
    detailsShell.layout();    
    detailsShell.setSize(500,300);    
    
    
    shell = new Shell(display,SWT.ON_TOP | SWT.APPLICATION_MODAL);
    shell.setSize(250,150);
    shell.setImage(ImageRepository.getImage("azureus"));
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0; layout.marginWidth = 0 ; layout.spacing = 0;
    shell.setLayout(layout);
    
    
    shellImg = new Image(display,ImageRepository.getImage("popup"),SWT.IMAGE_COPY);
    GC gcImage = new GC(shellImg);
    
    Image imgIcon = ImageRepository.getImage(icon);
    
    gcImage.drawImage(imgIcon,5,5);
    
    Font tempFont = shell.getFont();
    FontData[] fontDataMain = tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {             
      fontDataMain[i].setStyle(SWT.BOLD);
      fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.2));
    }
    
    Font fontTitle = new Font(display,fontDataMain);
    gcImage.setFont(fontTitle);
    
    GCStringPrinter.printString(gcImage,title,new Rectangle(59,11,182,43));
    
    gcImage.setFont(tempFont);
    fontTitle.dispose();
    
    
    GCStringPrinter.printString(gcImage,errorMessage, new Rectangle(5,40,240,60));
    
    gcImage.dispose();            
    
    final Button btnDetails = new Button(shell,SWT.TOGGLE);
    Messages.setLanguageText(btnDetails,"popup.error.details");    
    btnDetails.setEnabled(details != null);
    
    Button btnHide = new Button(shell,SWT.PUSH);
    Messages.setLanguageText(btnHide,"popup.error.hide");    
    
    Label lblImage = new Label(shell,SWT.NULL);
    lblImage.setImage(shellImg);
    
    FormData formData;
    
    formData = new FormData();    
    formData.right = new FormAttachment(btnHide,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnDetails.setLayoutData(formData);
    
    formData = new FormData();
    formData.right = new FormAttachment(100,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnHide.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0);
    formData.top = new FormAttachment(0);
    lblImage.setLayoutData(formData);
    
    shell.layout();
    
    
    btnHide.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event arg0) {
        if(currentAnimator == null) {
          detailsShell.setVisible(false);
          detailsShell.forceActive();
          detailsShell.forceFocus();
          currentAnimator = new LinearAnimator(MessagePopupShell.this,new Point(x0,y1),new Point(x1,y1),20,30);
          currentAnimator.start();
          closeAfterAnimation = true;
        }
      }
    });
    
    btnDetails.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event arg0) {
       detailsShell.setVisible(btnDetails.getSelection());
      }
    });
    
    Rectangle bounds = display.getClientArea();
    x0 = bounds.width - 255;
    x1 = bounds.width;
    
    y0 = bounds.height;
    y1 = bounds.height - 155;
    
    shell.setLocation(x0,y0);
    detailsShell.setLocation(x1-detailsShell.getSize().x,y1-detailsShell.getSize().y);
    currentAnimator = new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),20,30);
    currentAnimator.start();
    shell.open();
  }
  
  
  
  private Animator currentAnimator;
  private boolean closeAfterAnimation;
  int x0,y0,x1,y1;
  
  public void animationEnded(Animator source) {
    if(source == currentAnimator) {
      currentAnimator = null;
    }
    if(closeAfterAnimation) {   
      if(display == null || display.isDisposed())
        return;
      display.asyncExec(new Runnable() {
        public void run() {
          shell.dispose();
          detailsShell.dispose();
          shellImg.dispose();          
        }
      });     
    }
  }

  public void animationStarted(Animator source) {   
  }

  
  public Shell getShell() {
    return shell;
  }
  
  public void reportPercent(int percent) {    
  }
}
