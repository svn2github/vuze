/*
 * File    : RemoteUIMainPanel.java
 * Created : 28-Jan-2004
 * By      : parg
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet;

/**
 * @author parg
 *
 */

import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.gudy.azureus2.plugins.download.*;


import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.view.*;

public class 
RemoteUIMainPanel
	extends JPanel
{
	protected DownloadManager		download_manager;
	
	protected ArrayList					listeners = new ArrayList();
	
	public
	RemoteUIMainPanel(
			DownloadManager	_dm )
	{
		try{
			download_manager		= _dm;
		
			setLayout( new BorderLayout());
			
			JToolBar tb = new JToolBar();
			
			JButton	refresh = new JButton( "Refresh");
			
			tb.add( refresh );
			
			JButton	start = new JButton( "Start");
			
			tb.add( start );
			
			JButton	stop = new JButton( "Stop");
			
			tb.add( stop );
			
			JButton	remove = new JButton( "Remove");
			
			tb.add( remove );
			
			final JTextField	tf = new JTextField();
			
			tf.setColumns(20);
			
			tb.add( tf );
			
			JButton	open = new JButton( "Open");
			
			tb.add( open );
			
			add( tb, BorderLayout.NORTH );
			
			final MDDownloadModel	model 	= new MDDownloadModel( download_manager );
			
			final VWDownloadView 	view 	= new VWDownloadView(model);
			
			add( view.getComponent(), BorderLayout.CENTER );
			
			refresh.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								for (int i=0;i<listeners.size();i++){
									
									((RemoteUIMainPanelListener)listeners.get(i)).refresh();
								}
								
								model.refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
	
			start.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
						
								model.start(view.getSelectedRows());
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			stop.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								
								model.stop(view.getSelectedRows());
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			remove.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								
								model.remove(view.getSelectedRows());
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			open.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								URL	url = new URL( tf.getText());
							
								download_manager.addDownload( url );
							
								model.refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			reportError( e );
		}
	}
	
	public void
	reportError(
		final Throwable 	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			((RemoteUIMainPanelListener)listeners.get(i)).error( e );
		}
	}
	
	public void
	addListener(
		RemoteUIMainPanelListener	l )
	{
		listeners.add(l);
	}
}
