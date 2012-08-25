/*
 * File : SingleFilePanel.java Created : 30 sept. 2003 02:50:19 By : Olivier
 * 
 * Azureus - a Java Bittorrent client
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details ( see the LICENSE file ).
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.gudy.azureus2.ui.swt.maketorrent;

import java.io.File;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

/**
 * @author Olivier
 *  
 */
public class BYOPanel extends AbstractWizardPanel<NewTorrentWizard> {

	private Text txtInfo;
	private Text container;
	
	private List<String>		files = new ArrayList<String>();
	private String				container_txt	= "";
	
  public BYOPanel(NewTorrentWizard wizard, IWizardPanel<NewTorrentWizard> previous) {
    super(wizard, previous);
    
    wizard.byo_map	= null;
  }

  /*
	 * (non-Javadoc)
	 * 
	 * @see org.gudy.azureus2.ui.swt.maketorrent.IWizardPanel#show()
	 */
  public void show() {
    wizard.setTitle(MessageText.getString("wizard.newtorrent.byo"));
    wizard.setCurrentInfo(MessageText.getString("wizard.newtorrent.byo.info"));
    Composite panel = wizard.getPanel();
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    panel.setLayout(layout);
    
    Label l = new Label( panel, SWT.NULL );
    l.setText( MessageText.getString( "wizard.newtorrent.byo.container"));
    container = new Text(panel, SWT.BORDER);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 1;
    container.setLayoutData(gridData);
    container.setText(container_txt);

    container.addListener(SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
    	  container_txt =  container.getText().trim();
    	  wizard.setNextEnabled(container_txt.length() > 0 );
      }
    });
    
    
    
    txtInfo = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    gridData = new GridData(  GridData.FILL_BOTH );
    txtInfo.setEditable( false );
    
    gridData.horizontalSpan = 2;
    txtInfo.setLayoutData(gridData);
    txtInfo.setText("");
    
    container.setEnabled( false );
    
    for ( String file: files ){
    	
    	addFilename( file, true  );
    }
  }

  protected void
  addFilename(
	String		file )
  {
	  addFilename( file, false );
  }
  
  protected void
  addFilename(
	String		file,
	boolean		init )
  {
	  if ( !init && files.contains( file )){
		  
		  return;
	  }
	  
	  if ( !files.contains( file )){
		  
		  files.add( file );
	  }
	  
	  txtInfo.append( file + "\r\n" );
		  
	  container.setEnabled( files.size() > 1 );
	  
	  wizard.setNextEnabled( files.size() == 1 || ( files.size() > 1 && container.getText().trim().length() > 0 ));
  }
 
  public IWizardPanel<NewTorrentWizard> 
  getNextPanel() 
  {
	  Map	map = new HashMap();
	  
	  List<Map>	list = new ArrayList<Map>();
	  
	  map.put( "file_map", list );
	  
	  String 	cont_name = files.size() < 2?null:container.getText().trim();
	  
	  for ( String file: files ){
		  
		  Map m = new HashMap();
		  
		  list.add( m );
		  
		  List<String> path = new ArrayList<String>();
		  
		  if ( cont_name != null ){
			  
			  path.add( cont_name );
		  }
		  
		  path.add( new File( file ).getName());
		  
		  m.put( "logical_path", path );
		  m.put( "target", file );
	  }
	  
	  wizard.byo_map = map;
	  
	  try{
		  wizard.byo_desc_file= AETemporaryFileHandler.createTempFile();
	
		  FileUtil.writeBytesAsFile( wizard.byo_desc_file.getAbsolutePath(), BEncoder.encode( map ));
		  
	  }catch( Throwable e ){
		  
		  Debug.out( e );
	  }
	  
	  return new SavePathPanel( wizard, this);
  }
}
