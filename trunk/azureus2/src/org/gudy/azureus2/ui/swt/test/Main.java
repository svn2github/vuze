/*
 * File    : Main.java
 * Created : 14 oct. 2003 01:21:23
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
 
package org.gudy.azureus2.ui.swt.test;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class Main {


public static void main(String[] args) {
  final Display display = new Display();
  final Shell shell = new Shell(display);
  GridLayout layout = new GridLayout();
  layout.numColumns = 2;
  shell.setLayout(layout);
  Label label = new Label(shell,SWT.NULL);
  label.setText("A single-line Label");
  Combo combo = new Combo(shell,SWT.READ_ONLY);
  for(int i = 0 ; i < 5 ; i++) {
    combo.add("item " +1);
  }
  label = new Label(shell,SWT.NULL);
  label.setText("A multi-line\nLabel");
  combo = new Combo(shell,SWT.READ_ONLY);
  for(int i = 0 ; i < 5 ; i++) {
    combo.add("item " +1);
  }  
  shell.open();
  while (!shell.isDisposed()) {
    if (!display.readAndDispatch())
      display.sleep();
  }
  display.dispose();
}
}