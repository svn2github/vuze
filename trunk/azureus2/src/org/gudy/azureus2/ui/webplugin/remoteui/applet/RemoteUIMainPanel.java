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

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.gudy.azureus2.plugins.*;


import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.view.*;

public class 
RemoteUIMainPanel
	extends JPanel
{
	protected PluginInterface		pi;
	
	protected ArrayList					listeners = new ArrayList();
	
	public
	RemoteUIMainPanel(
		PluginInterface	_pi )
	{
		pi		= _pi;
	
		setLayout( new BorderLayout());
		
		JToolBar tb = new JToolBar();
		
		JButton	refresh = new JButton( "Refresh");
		
		tb.add( refresh );
		
		add( tb, BorderLayout.NORTH );
		
		final MDDownloadModel	model 	= new MDDownloadModel( pi.getDownloadManager());
		
		final VWDownloadView 	view 	= new VWDownloadView(model);
		
		add( view.getComponent(), BorderLayout.CENTER );
		
		refresh.addActionListener(
				new ActionListener()
				{
					public void
					actionPerformed(
							ActionEvent	ev )
					{
						for (int i=0;i<listeners.size();i++){
							
							((RemoteUIMainPanelListener)listeners.get(i)).refresh();
						}
						
						model.refresh();
						
						view.refresh();
					}
				});
	}
	
	public void
	addListener(
		RemoteUIMainPanelListener	l )
	{
		listeners.add(l);
	}
}
