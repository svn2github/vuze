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
import javax.swing.event.*;

import org.gudy.azureus2.ui.swing.*;

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
	protected RemoteUIMainPanelAdaptor	adapter;
		
	protected MDDownloadModel	download_full_model;
	protected MDDownloadModel	downloading_model;
	protected MDDownloadModel	seeding_model;
							
	protected VWDownloadView downloading_view;
	protected VWDownloadView seeding_view;
	
	protected MDDownloadModel	current_download_model;
	protected VWDownloadView	current_download_view;

	protected VWStatusAreaView			status_area_view;
	protected MDStatusAreaModel			status_area_model;
		
	protected JTextArea		log_area;
	
	protected int			next_refresh;
	protected int			refresh_period;
	
	protected boolean		destroyed;
	
	public
	RemoteUIMainPanel(
		final PluginInterface		_pi,
		DownloadManager				_dm,
		RemoteUIMainPanelAdaptor	_adapter )
	{
		try{
			properties				= _pi.getPluginProperties();
			download_manager		= _dm;
			adapter					= _adapter;

			String	mode_str = (String)properties.get("mode");
						
			final boolean view_mode = mode_str != null && mode_str.trim().equalsIgnoreCase("view");
			
			setLayout( new BorderLayout());
			
			JToolBar tb = new JToolBar();
			
			JButton	refresh =				
				getButton(	"Refresh",
							getImageIcon( 
									UISwingImageRepository.getImage(
									adapter.getResource("org/gudy/azureus2/ui/icons/recheck.gif"))));

			refresh.setToolTipText("Refresh");
			
			tb.add( refresh );
			
			JButton	start = 
				getButton(	"Start",
							getImageIcon(UISwingImageRepository.getImage(
									adapter.getResource("org/gudy/azureus2/ui/icons/start.gif"))));
			
			start.setToolTipText("Start");

			start.setEnabled( !view_mode );
			tb.add( start );
			
			JButton	force_start = 
				getButton(	"FStart",
							getImageIcon(UISwingImageRepository.getImage(
									adapter.getResource("org/gudy/azureus2/ui/icons/forcestart.gif"))));
			
			force_start.setToolTipText("Toggle Force Start");

			force_start.setEnabled( !view_mode );
			tb.add( force_start );

			
			JButton	stop = 
				getButton( 	"Stop",
							getImageIcon(UISwingImageRepository.getImage(
									adapter.getResource("org/gudy/azureus2/ui/icons/stop.gif"))));

			stop.setToolTipText("Stop");
			
			stop.setEnabled( !view_mode );
			tb.add( stop );
			
			JButton	remove = 
				getButton( 	"Remove",
							getImageIcon(UISwingImageRepository.getImage(
									adapter.getResource("org/gudy/azureus2/ui/icons/delete.gif"))));
			
			remove.setToolTipText("Remove");

			remove.setEnabled( !view_mode );
			tb.add( remove );
			
			tb.addSeparator();
			
				// move up
			
			final JButton	move_up = 
				getButton( 	"Up",
							getImageIcon(UISwingImageRepository.getImage(
										adapter.getResource("org/gudy/azureus2/ui/icons/up.gif"))));

			move_up.setToolTipText("Move Up");

			move_up.setEnabled( false );
			tb.add( move_up );

				// move down
			
			final JButton	move_down = 
				getButton( 	"Down",
							getImageIcon(UISwingImageRepository.getImage(
										adapter.getResource("org/gudy/azureus2/ui/icons/down.gif"))));

			move_down.setToolTipText("Move Down");

			move_down.setEnabled( false );
			tb.add( move_down );
			
				// open 
			
			final JTextField	tf = new JTextField();
			
			tf.setColumns(20);
			
			tb.add( tf );
			
			JButton	open = 
				getButton( 	"Open",
							getImageIcon(UISwingImageRepository.getImage(
										adapter.getResource("org/gudy/azureus2/ui/icons/openFolder16x12.gif"))));

			open.setToolTipText("Open Torrent URL");

			open.setEnabled( !view_mode );
			tb.add( open );
			

			add( tb, BorderLayout.NORTH );
			
			
			MDDownloadSplitModel	split_model	= new MDDownloadSplitModel( download_manager );
			
			downloading_model	= split_model.getDownloadingModel();
			seeding_model		= split_model.getSeedingModel();
									
			download_full_model	= split_model.getFullModel();

			downloading_view 	= new VWDownloadView(downloading_model);
			
			downloading_view.addListener(
					new VWDownloadViewListener()
					{
						public void
						selectionChanged(
							int[]		rows )
						{
							if ( current_download_view == downloading_view && !view_mode ){
								
								if ( rows.length == 0 ){
									
									move_up.setEnabled(false);
									
									move_down.setEnabled( false );
									
								}else{
									
									Download	dl = downloading_model.getDownload( rows[0]);
									
									move_up.setEnabled(dl.getPosition() > 1 );
									
									move_down.setEnabled(dl.getPosition() < downloading_model.getDownloads().length );
								}
							}
						}
					});
			
			seeding_view 	= new VWDownloadView(seeding_model);
			
			seeding_view.addListener(
					new VWDownloadViewListener()
					{
						public void
						selectionChanged(
							int[]		rows )
						{
							if ( current_download_view == seeding_view && !view_mode ){
								
								if ( rows.length == 0 ){
									
									move_up.setEnabled(false);
									
									move_down.setEnabled( false );
									
								}else{
									
									Download	dl = seeding_model.getDownload( rows[0]);
									
									move_up.setEnabled(dl.getPosition() > 1 );
									
									move_down.setEnabled(dl.getPosition() < seeding_model.getDownloads().length );
								}
							}
						}
					});
			
			current_download_model	=	downloading_model;
			current_download_view	=	downloading_view;
			
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
			
			tabs.addTab( "Downloads", 	downloading_view.getComponent());
			tabs.addTab( "Seeds", 		seeding_view.getComponent());
			tabs.addTab( "Config", 		config_view.getComponent());
			
			tabs.addChangeListener(
					new ChangeListener()
					{
				        public void 
						stateChanged(
							ChangeEvent 	evt ) 
						{
				            JTabbedPane pane = (JTabbedPane)evt.getSource();
				    
				            int sel = pane.getSelectedIndex();
				            
				            if ( sel == 0 ){
				            	
				            	current_download_model	= downloading_model;
				            	
								current_download_view	= downloading_view;
								
				            }else if ( sel == 1 ){
				            	
				            	current_download_model	= seeding_model;
				            	
								current_download_view	= seeding_view;
				            }
				            
				            current_download_view.refresh();
				        }
				    });
			
			add( tabs, BorderLayout.CENTER );
			
			JPanel	bottom_panel = new JPanel( new BorderLayout());
			
			log_area = new JTextArea();
			
			log_area.setEditable(false);
			log_area.setBorder(BorderFactory.createEtchedBorder());
			
			log_area.setRows(3);

			bottom_panel.add( new JScrollPane(log_area), BorderLayout.CENTER );
			
			
			status_area_model =  new MDStatusAreaModel( _pi, download_full_model );
			
			status_area_view =  new VWStatusAreaView(status_area_model);
			
			bottom_panel.add( status_area_view.getComponent(), BorderLayout.SOUTH );
			
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
						
								current_download_model.start(current_download_view.getSelectedRows());
								
								refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			force_start.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
						
								current_download_model.forceStart(current_download_view.getSelectedRows());
								
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
								
								current_download_model.stop(current_download_view.getSelectedRows());
								
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
								
								current_download_model.remove(current_download_view.getSelectedRows());
								
								refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			move_up.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								
								current_download_model.moveUp(current_download_view.getSelectedRows());
								
								refresh();
								
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								reportError( e );
							}
						}
					});
			
			move_down.addActionListener(
					new ActionListener()
					{
						public void
						actionPerformed(
								ActionEvent	ev )
						{
							try{
								
								current_download_model.moveDown(current_download_view.getSelectedRows());
								
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
	
		// on Opera 7.5 images don't load properly so lash up 
	
	protected JButton
	getButton(
		String		text,
		ImageIcon	icon )
	{
		if ( icon == null ){
			
			return( new JButton( text ));
		
		}else{
			
			return( new JButton( icon ));
		}
	}
	protected ImageIcon
	getImageIcon(
		Image		image )
	{
		if ( image == null ){
			
			return( null );
			
		}else{
			
			return( new ImageIcon( image ));
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
			adapter.refresh();
			
			int[]	old_dl_rows = downloading_view.getSelectedRows();
			int[]	old_se_rows = seeding_view.getSelectedRows();
			
			download_full_model.refresh();
			
			downloading_view.setSelectedRows( old_dl_rows );
			seeding_view.setSelectedRows( old_se_rows );
			
			status_area_model.refresh();
			
			status_area_view.refresh();
			
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
			
		adapter.error( e );
	}
	
	public void
	destroy()
	{
		destroyed	= true;
	}
}
