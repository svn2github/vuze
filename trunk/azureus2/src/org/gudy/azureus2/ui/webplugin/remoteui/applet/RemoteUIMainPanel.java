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
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*;


import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.view.*;

public class 
RemoteUIMainPanel
	extends JPanel
{
	protected DownloadManager			download_manager;
	
	protected MDDownloadModel			download_model;
	
	protected ArrayList					listeners = new ArrayList();
	
	protected JTextArea		log_area;
	
	protected int			next_refresh;
	protected int			refresh_period;
	
	protected boolean		destroyed;
	
	public
	RemoteUIMainPanel(
		PluginInterface	_pi,
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
			
			download_model 	= new MDDownloadModel( download_manager );
			
			final VWDownloadView 	download_view 	= new VWDownloadView(download_model);
			
			MDConfigModel			config_model	= new MDConfigModel( _pi );
			
			VWConfigView			config_view 	= new VWConfigView( config_model );
			
			refresh_period = config_model.getRefreshPeriod();
			
			config_model.addListener(
					new MDConfigModelListener()
					{
						public void
						propertyChanged(
							MDConfigModelPropertyChangeEvent	ev )
						{
							if ( ev.getPropertyType() == MDConfigModelPropertyChangeEvent.PT_REFRESH_PERIOD ){
							
								refresh_period = ((Integer)ev.getPropertyValue()).intValue();								
							}
						}
					});
			
			JTabbedPane	tabs = new JTabbedPane();
			
			tabs.addTab( "Downloads", 	download_view.getComponent());
			tabs.addTab( "Config", 		config_view.getComponent());
			
			add( tabs, BorderLayout.CENTER );
			
			JPanel	bottom_panel = new JPanel( new BorderLayout());
			
			log_area = new JTextArea();
			
			log_area.setEditable(false);
			log_area.setBorder(BorderFactory.createEtchedBorder());
			
			log_area.setRows(3);

			bottom_panel.add( new JScrollPane(log_area), BorderLayout.CENTER );
			
			add( bottom_panel, BorderLayout.SOUTH );
			
			refresh.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							refresh();
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
						
								download_model.start(download_view.getSelectedRows());
								
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
								
								download_model.stop(download_view.getSelectedRows());
								
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
								
								download_model.remove(download_view.getSelectedRows());
								
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
							
								download_model.refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			new Thread()
			{
				public void
				run()
				{
					periodicRefresh();
				}
			}.start();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			reportError( e );
		}
	}
	
	protected void
	periodicRefresh()
	{
		int	ticks = 0;
		
		while( !destroyed ){
		
			if ( ticks >= refresh_period ){
				
				refresh();
				
				ticks = 0;
			}
			
			try{
				Thread.sleep(1000);
				
			}catch( InterruptedException e ){
				
			}
			
			ticks++;
		}
	}
	
	protected void
	refresh()
	{
		try{
			for (int i=0;i<listeners.size();i++){
				
				((RemoteUIMainPanelListener)listeners.get(i)).refresh();
			}
			
			download_model.refresh();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			reportError( e );
		}
	}
	
	public void
	logMessage(
		final String	str )
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void
				run()
				{
					String ts = new SimpleDateFormat("hh:mm:ss - ").format( new Date());
					log_area.setText(log_area.getText()+ "\r\n" + ts + str );
				}
			});
	}
	
	public void
	reportError(
		final Throwable 	e )
	{
		logMessage( e.getMessage());
			
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
	
	public void
	destroy()
	{
		destroyed	= true;
	}
}
