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
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.download.*;


import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.view.*;

public class 
RemoteUIMainPanel
	extends JPanel
{
	protected Properties				properties;
	protected DownloadManager			download_manager;
	
	protected MDDownloadModel			download_model;
	protected VWDownloadView			download_view;
	
	protected VWStatusAreaView			status_area;
	
	protected ArrayList					listeners = new ArrayList();
	
	protected JTextArea		log_area;
	
	protected int			next_refresh;
	protected int			refresh_period;
	
	protected boolean		destroyed;
	
	public
	RemoteUIMainPanel(
		final PluginInterface	_pi,
		DownloadManager			_dm )
	{
		try{
			properties				= _pi.getPluginProperties();
			download_manager		= _dm;
		
			String	mode_str = (String)properties.get("mode");
						
			boolean view_mode = mode_str != null && mode_str.trim().equalsIgnoreCase("view");
			
			setLayout( new BorderLayout());
			
			JToolBar tb = new JToolBar();
			
			JButton	refresh = new JButton( "Refresh");
			
			tb.add( refresh );
			
			JButton	start = new JButton( "Start");
			start.setEnabled( !view_mode );
			tb.add( start );
			
			JButton	stop = new JButton( "Stop");
			stop.setEnabled( !view_mode );
			tb.add( stop );
			
			JButton	remove = new JButton( "Remove");
			remove.setEnabled( !view_mode );
			tb.add( remove );
			
			final JTextField	tf = new JTextField();
			
			tf.setColumns(20);
			
			tb.add( tf );
			
			JButton	open = new JButton( "Open");
			open.setEnabled( !view_mode );
			tb.add( open );
			
			add( tb, BorderLayout.NORTH );
			
			download_model 	= new MDDownloadModel( download_manager );
			
			download_view 	= new VWDownloadView(download_model);
			
			MDConfigModel			config_model	= new MDConfigModel( _pi );
			
			VWConfigView			config_view 	= new VWConfigView( this, config_model );
			
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
								
								logMessage( "Refresh period changed to " + refresh_period + " sec");
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
			
			
			status_area =  new VWStatusAreaView(new MDStatusAreaModel( _pi, download_model ));
			
			bottom_panel.add( status_area.getComponent(), BorderLayout.SOUTH );
			
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
								
								refresh();
								
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
								
								refresh();
								
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
								
								refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			
			AbstractAction open_action = 
				new AbstractAction()
				{
					public void
					actionPerformed(
							ActionEvent	ev )
					{
						try{
							String	url_str = tf.getText().trim();
							
							if ( url_str.length() == 0 ){
								
								throw( new Exception( "URL required" ));
							}
							
							if ( !url_str.toLowerCase().startsWith( "http" )){
								
								throw( new Exception( "Unsupported URL protocol" ));
							}
							
							URL	url = new URL( url_str );
						
							String protocol = url.getProtocol().toLowerCase();
							
							if ( !protocol.toLowerCase().startsWith( "http" )){
								
								throw( new Exception( "Unsupported URL protocol" ));
							}
							
							TorrentDownloader dl = _pi.getTorrentManager().getURLDownloader( url );
							
							Torrent torrent = dl.download();
							
							logMessage( "Downloaded torrent: " + torrent.getName());
							
							download_manager.addDownload( torrent );
						
							refresh();
							
						}catch( Throwable e ){
							
							reportError( e );
						}
					}
				};
				
			tf.registerKeyboardAction(
						open_action,
						KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ),
						JComponent.WHEN_FOCUSED );
			
			open.addActionListener( open_action );

			
			new Thread("RemoteUIMainPanel::refresh")
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
			
					// 0 -> don't refresh
				
				if ( refresh_period > 0 ){	
			
					SwingUtilities.invokeLater(
							new Runnable()
							{
								public void
								run()
								{
									refresh();
								}
							});
				}
				
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
			
			int[]	old_rows = download_view.getSelectedRows();
			
			download_model.refresh();
			
			download_view.setSelectedRows( old_rows );
			
			status_area.refresh();
			
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
					
					String	text = log_area.getText()+ "\r\n" + ts + str;
					
					while( text.length() > 65536 ){
						
						int	p = text.indexOf( "\r\n" );
						
						if ( p == -1 ){
							
							break;
						}
						
						text = text.substring( p+2 );
					}
					
					log_area.setText( text );
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
