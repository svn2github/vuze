/*
 * Created on 9 sept. 2003
 *
 */
package org.gudy.azureus2.applet;

import java.applet.Applet;
import java.awt.GridLayout;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.gudy.azureus2.core.DownloadManager;

/**
 * @author Olivier
 * 
 */
public class Main extends Applet {
  
  JPanel appletPanel;
  JLabel lFileName;
  JLabel lDownSpeed;
  JLabel lPercentDone;
  
  String url;
  String savePath;
  
  DownloadManager dm;    
  
  public Main() {
    appletPanel = new JPanel();
    GridLayout layout = new GridLayout(3,2);
    appletPanel.setLayout(layout);
    
    appletPanel.add(new JLabel("File :"));
    lFileName = new JLabel();
    appletPanel.add(lFileName);
    
    appletPanel.add(new JLabel("Down Speed :"));
    lDownSpeed = new JLabel();
    appletPanel.add(lDownSpeed);
        
    appletPanel.add(new JLabel("% done :"));
    lPercentDone = new JLabel();
    appletPanel.add(lPercentDone);  
    
    
    //Initialize parameters
    url = this.getParameter("torrentUrl");
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showSaveDialog(this);
    if(result == JFileChooser.APPROVE_OPTION)
      savePath =  fileChooser.getSelectedFile().getName();
      
    System.out.println(url + "\n" + savePath);   
      
    
    this.add(appletPanel);
    this.setSize(200,100);
  }
  
  

}
