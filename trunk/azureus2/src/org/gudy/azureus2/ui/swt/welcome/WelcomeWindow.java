/*
 * Created on 15 avr. 2005
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.welcome;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

public class WelcomeWindow {
  
	public static final int		WELCOME_VERSION		 	=  2306;
	public static final String	WELCOME_VERSION_STRING	= "2.3.0.6";
	
  private static final String lineSeparator = System.getProperty ("line.separator");
  
  Display display;
  Shell shell;
  Color black,white,light,grey,green,blue,fg,bg;
  
  public WelcomeWindow() {
    shell = ShellFactory.createShell(SWT.BORDER | SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    if(! Constants.isOSX) {
      shell.setImage(ImageRepository.getImage("azureus"));
    }
	
    int version = WELCOME_VERSION;

    shell.setText(MessageText.getString("window.welcome.title", new String[]{ WELCOME_VERSION_STRING }));
    
    display = shell.getDisplay();
    
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);
    
    GridData data;
    
    StyledText helpPanel = new StyledText(shell, SWT.VERTICAL | SWT.BORDER);
    data = new GridData(GridData.FILL_BOTH);
    helpPanel.setLayoutData(data);

    helpPanel.setEditable(false);
    try {
      String helpText = "";
      String helpFile = MessageText.getString("window.welcome.file");
      InputStream stream = getClass().getResourceAsStream("/org/gudy/azureus2/internat/whatsnew/" + helpFile);
      if (stream == null) {
        System.err.println("Welcome Window: Error loading resource: /org/gudy/azureus2/internat/whatsnew/" + helpFile);
      }
      else {
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));

        helpPanel.setRedraw(false);
        helpPanel.setWordWrap(true);

        black = new Color((Device)display, 0,0,0);
        white = new Color((Device)display, 255,255,255);  
        light = new Color((Device)display, 200,200,200);  
        grey = new Color((Device)display, 50,50,50);
        green = new Color((Device)display, 30,80,30);
        blue = new Color((Device)display, 20,20,80);
        int style;
        boolean setStyle;

        helpPanel.setForeground(grey);

        String line;
        while ((line = in.readLine()) != null) {

          setStyle = false;
          fg = grey;
          bg = white;
          style = SWT.NORMAL;

          char styleChar;
          String text;

          if (line.length() < 2) {
            styleChar = ' ';
            text = " " + lineSeparator;
          }
          else {
            styleChar = line.charAt(0);
            text = line.substring(1) + lineSeparator;
          }

          switch (styleChar) {
            case '*':
              text = "  * " + text;
              fg = green;
              setStyle = true;
            break;
            case '+':
              text = "     " + text;
              fg = black;
              bg = light;
              style = SWT.BOLD;
              setStyle = true;
            break;
            case '!':
              style = SWT.BOLD;
              setStyle = true;
            break;
            case '@':
              fg = blue;
              setStyle = true;
            break;
            case '$':
              bg = blue;
              fg = white;
              style = SWT.BOLD;
              setStyle = true;
            break;
            case ' ':
              text = "  " + text;
            break;
          }

          helpPanel.append(text);

          if (setStyle) {
            int lineCount = helpPanel.getLineCount()-1;
            int charCount = helpPanel.getCharCount();
  //          System.out.println("Got Linecount " + lineCount + ", Charcount " + charCount);

            int lineOfs = helpPanel.getOffsetAtLine(lineCount - 1);
            int lineLen = charCount-lineOfs;
  //          System.out.println("Setting Style : " + lineOfs + ", " + lineLen);
            helpPanel.setStyleRange(new StyleRange(lineOfs, lineLen, fg, bg, style));
            helpPanel.setLineBackground(lineCount-1, 1, bg);
          }
        }
      }
        helpPanel.setRedraw(true);
      }
      catch (Exception e) {
        System.out.println("Unable to load help contents because:" + e);
        //e.printStackTrace();
      }
    
    Button bClose = new Button(shell,SWT.PUSH);
    bClose.setText(MessageText.getString("Button.close"));
    data = new GridData();
    data.widthHint = 70;
    data.horizontalAlignment = SWT.RIGHT;
    bClose.setLayoutData(data);
    
    Listener closeListener = new Listener() {
      public void handleEvent(Event event) {
        close();
      }
    };
    
    bClose.addListener(SWT.Selection, closeListener);
    shell.addListener(SWT.Close,closeListener);
    
	shell.setDefaultButton( bClose );
	
	shell.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
				close();
			}
		}
	});
	
    shell.setSize(500,400);
    Utils.centreWindow(shell);
    shell.layout();
    shell.open();    
  }
  
  private void close() {
    if(black != null && !black.isDisposed())  black.dispose();
    if(white != null && !white.isDisposed())  white.dispose();
    if(light != null && !light.isDisposed())  light.dispose();
    if(grey  != null && !grey.isDisposed() )  grey.dispose();
    if(green != null && !green.isDisposed())  green.dispose();
    if(blue  != null && !blue.isDisposed() )  blue.dispose();
    shell.dispose();
  }
}
