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

import java.awt.*;

import javax.swing.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.*; 

public class 
RemoteUIMainPanel
	extends JPanel
{
	protected PluginInterface		pi;
	
	public
	RemoteUIMainPanel(
		PluginInterface	_pi )
	{
		pi		= _pi;
	
		setLayout( new BorderLayout());
		
		add( new JLabel( "ding!"), BorderLayout.CENTER );
		
		DownloadManager	dm = pi.getDownloadManager();
		
		Download[]	downloads = dm.getDownloads();
		
		for (int i=0;i<downloads.length;i++){
			
			Download	download = downloads[i];
			
			System.out.println( "download:" + download.getTorrent().getName());
		}
	}
}
