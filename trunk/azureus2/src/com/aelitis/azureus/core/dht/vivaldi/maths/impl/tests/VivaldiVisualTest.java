/*
 * Created on 22 juin 2005
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
package com.aelitis.azureus.core.dht.vivaldi.maths.impl.tests;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.ui.swt.views.stats.VivaldiPanel;
import com.aelitis.azureus.core.dht.vivaldi.maths.Coordinates;
import com.aelitis.azureus.core.dht.vivaldi.maths.VivaldiPosition;
import com.aelitis.azureus.core.dht.vivaldi.maths.impl.HeightCoordinatesImpl;
import com.aelitis.azureus.core.dht.vivaldi.maths.impl.VivaldiPositionImpl;

public class VivaldiVisualTest {
  
  private static final int MAX_HEIGHT = 50;
  private static final int ELEMENTS_X = 50;
  private static final int ELEMENTS_Y = 50;
  private static final int DISTANCE   = 10;
  private static final int MAX_ITERATIONS = 1000;
  private static final int NB_CONTACTS = 7;
  
  public VivaldiVisualTest() {
    final Display display = new Display();
    Shell shell = new Shell(display);
    final VivaldiPanel panel = new VivaldiPanel(shell);
    shell.setLayout(new FillLayout());
    shell.setSize(800,800);
    shell.setText("Vivaldi Simulator");
    shell.open();
    
    Thread runner = new Thread() {
      public void run() {
        VivaldiPosition positions[][] = new VivaldiPosition[ELEMENTS_X][ELEMENTS_Y];
        final List lPos = new ArrayList(ELEMENTS_X*ELEMENTS_Y);        
        Coordinates realCoordinates[][] = new Coordinates[ELEMENTS_X][ELEMENTS_Y];
        //Init all
        for(int i = 0 ; i < ELEMENTS_X ; i++) {
          for(int j = 0 ; j < ELEMENTS_Y ; j++) {
            realCoordinates[i][j] = new HeightCoordinatesImpl(i*DISTANCE,j*DISTANCE,MAX_HEIGHT);
            positions[i][j] = new VivaldiPositionImpl(new HeightCoordinatesImpl(0,0,0));
            lPos.add(positions[i][j]);
          }
        }
        
        //Main loop
        for(int iter = 0 ; iter < MAX_ITERATIONS ; iter++) {
          //For each node :
          for(int i = 0 ; i < ELEMENTS_X ; i++) {
            for(int j = 0 ; j < ELEMENTS_Y ; j++) {
              VivaldiPosition position = positions[i][j];
              //Pick N random nodes
              for(int k = 0 ; k < NB_CONTACTS ; k++) {
                int i1 = (int) (Math.random() * ELEMENTS_X);
                int j1 = (int) (Math.random() * ELEMENTS_Y);
                if(i1 == i && j1 ==j) continue;
                VivaldiPosition position1 = positions[i1][j1];
                float rtt = realCoordinates[i1][j1].distance(realCoordinates[i][j]);
                position.update(rtt,position1.getCoordinates(),position1.getErrorEstimate());
              }
              
            }
          }
          System.out.println(iter);
          display.syncExec( new Runnable() {
            public void run() {
              panel.refresh(lPos);
            }
          });
        }
      }
    };
    runner.setDaemon(true);
    runner.start();
    
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();
  }
  
  public static void main(String args[]) {
    new VivaldiVisualTest();
  }
}
