/*
 * File    : HealthHelpWindow.java
 * Created : 18 déc. 2003}
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
package org.gudy.azureus2.ui.swt.help;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * @author Olivier
 *
 */
public class HealthHelpWindow {
  
  static Image grey,red,blue,yellow,green;
  
  public static void show(Display display) {    
    final Shell window = new Shell(display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    window.setImage(ImageRepository.getImage("azureus"));
    window.setText(MessageText.getString("MyTorrentsView.menu.health"));
    
    disposeImages();
    grey = new Image(display,ImageRepository.getImage("st_stopped_selected"),SWT.IMAGE_COPY);
    grey.setBackground(window.getBackground());
    
    red = new Image(display,ImageRepository.getImage("st_ko_selected"),SWT.IMAGE_COPY);
    red.setBackground(window.getBackground());
    
    blue = new Image(display,ImageRepository.getImage("st_no_tracker_selected"),SWT.IMAGE_COPY);
    blue.setBackground(window.getBackground());
    
    yellow = new Image(display,ImageRepository.getImage("st_no_remote_selected"),SWT.IMAGE_COPY);
    yellow.setBackground(window.getBackground());
    
    green = new Image(display,ImageRepository.getImage("st_ok_selected"),SWT.IMAGE_COPY);
    green.setBackground(window.getBackground());
    
    FormLayout layout = new FormLayout();
    layout.marginHeight = 3;
    layout.marginWidth = 3;
    try {
      layout.spacing = 3;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    window.setLayout(layout);
    FormData formData;
    
    Label lblGreyImage = new Label(window,SWT.NULL);
    lblGreyImage.setImage(grey);
    
    Label lblGreyExplain = new Label(window,SWT.NULL);
    lblGreyExplain.setText(MessageText.getString("health.explain.grey"));
    formData = new FormData();    
    formData.left = new FormAttachment(lblGreyImage,5);
    lblGreyExplain.setLayoutData(formData);
    
    Label lblRedImage = new Label(window,SWT.NULL);
    lblRedImage.setImage(red);
    formData = new FormData();
    formData.top = new FormAttachment(lblGreyExplain,5);
    lblRedImage.setLayoutData(formData);
    
    Label lblRedExplain = new Label(window,SWT.NULL);
    lblRedExplain.setText(MessageText.getString("health.explain.red"));
    formData = new FormData();
    formData.top = new FormAttachment(lblGreyExplain,5);
    formData.left = new FormAttachment(lblRedImage,5);
    lblRedExplain.setLayoutData(formData);
    
    
    Label lblBlueImage = new Label(window,SWT.NULL);
    lblBlueImage.setImage(blue);
    formData = new FormData();
    formData.top = new FormAttachment(lblRedExplain,5);
    lblBlueImage.setLayoutData(formData);
    
    Label lblBlueExplain = new Label(window,SWT.NULL);
    lblBlueExplain.setText(MessageText.getString("health.explain.blue"));
    formData = new FormData();
    formData.top = new FormAttachment(lblRedExplain,5);
    formData.left = new FormAttachment(lblBlueImage,5);
    lblBlueExplain.setLayoutData(formData);
    
    
    Label lblYellowImage = new Label(window,SWT.NULL);
    lblYellowImage.setImage(yellow);
    formData = new FormData();
    formData.top = new FormAttachment(lblBlueExplain,5);
    lblYellowImage.setLayoutData(formData);
    
    Label lblYellowExplain = new Label(window,SWT.NULL);
    lblYellowExplain.setText(MessageText.getString("health.explain.yellow"));
    formData = new FormData();
    formData.top = new FormAttachment(lblBlueExplain,5);
    formData.left = new FormAttachment(lblYellowImage,5);
    lblYellowExplain.setLayoutData(formData);
            
    Label lblGreenImage = new Label(window,SWT.NULL);
    lblGreenImage.setImage(green);
    formData = new FormData();
    formData.top = new FormAttachment(lblYellowExplain,5);
    lblGreenImage.setLayoutData(formData);
    
    Label lblGreenExplain = new Label(window,SWT.NULL);
    lblGreenExplain.setText(MessageText.getString("health.explain.green"));
    formData = new FormData();
    formData.top = new FormAttachment(lblYellowExplain,5);
    formData.left = new FormAttachment(lblGreenImage,5);
    lblGreenExplain.setLayoutData(formData);
    
    
    
    Button btnOk = new Button(window,SWT.PUSH);
    btnOk.setText(MessageText.getString("Button.ok"));
    formData = new FormData();
    formData.top = new FormAttachment(lblGreenExplain,20);
    formData.right = new FormAttachment(100,0);
    formData.width = 70;
    btnOk.setLayoutData(formData);
    
    btnOk.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        window.dispose();
      }
    });
    
    window.addDisposeListener(new DisposeListener() {
	    public void widgetDisposed(DisposeEvent arg0) {
	      disposeImages();
	    }
    });
    
    window.pack();
    window.open();
    
  }
  
  private static void disposeImages() {
    if(grey != null && ! grey.isDisposed()) {
      grey.dispose();
    }
    if(red != null && ! red.isDisposed()) {
      red.dispose();
    }
    if(blue != null && ! blue.isDisposed()) {
      blue.dispose();
    }
    if(yellow != null && ! yellow.isDisposed()) {
      yellow.dispose();
    }
    if(green != null && ! green.isDisposed()) {
      green.dispose();
    }
  }
}
