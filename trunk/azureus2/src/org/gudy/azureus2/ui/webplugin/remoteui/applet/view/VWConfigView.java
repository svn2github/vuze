/*
 * File    : VWConfigView.java
 * Created : 17-Feb-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet.view;

/**
 * @author parg
 *
 */


import java.awt.*;
import java.beans.*;
import java.text.*;

import javax.swing.*;

import org.gudy.azureus2.ui.webplugin.remoteui.applet.model.*;
import org.gudy.azureus2.ui.webplugin.remoteui.applet.*;

public class 
VWConfigView 
{
	protected JComponent	component;

	protected RemoteUIMainPanel	main;
	protected int				row;
	
	public
	VWConfigView(
		final RemoteUIMainPanel	_main,
		final MDConfigModel		_model )
	{
		main		= _main;
		
		component = new JPanel(new GridBagLayout());
				
			// refresh rate
		
		addIntParameter( 
				"Auto-refresh period (secs) - 0 = never",
				new intValueAdapter()
				{
					public int
					getValue()
					{
						return( _model.getRefreshPeriod() );
					}
					
					public void
					setValue(
						int	value )
					{
						_model.setRefreshPeriod(value);
					}
				});	
		
			// max upload
		
		addIntParameter( 
				"Max global upload speed KB/s [0: unlimited]",
				new intValueAdapter()
				{
					public int
					getValue()
					{
						return( _model.getMaxUploadSpeed() );
					}
					
					public void
					setValue(
						int	value )
					{
						_model.setMaxUploadSpeed(value);
					}
				});
	
		addIntParameter( 
				"Max global download speed KB/s [0: unlimited]",
				new intValueAdapter()
				{
					public int
					getValue()
					{
						return( _model.getMaxDownloadSpeed() );
					}
					
					public void
					setValue(
						int	value )
					{
						_model.setMaxDownloadSpeed(value);
					}
				});
		
		addIntParameter( 
				"Max connections per torrent [0: unlimited]",
				new intValueAdapter()
				{
					public int
					getValue()
					{
						return( _model.getMaxConnectionsPerTorrent() );
					}
					
					public void
					setValue(
						int	value )
					{
						_model.setMaxConnectionsPerTorrent(value);
					}
				});
	
		addIntParameter( 
				"Max connections global [0: unlimited]",
				new intValueAdapter()
				{
					public int
					getValue()
					{
						return( _model.getMaxConnectionsGlobal() );
					}
					
					public void
					setValue(
						int	value )
					{
						_model.setMaxConnectionsGlobal(value);
					}
				});
	
		
		component.add( 	new JPanel(),
						new VWGridBagConstraints(
							0, row++, 3, 1, 1.0, 1.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH, 
							new Insets(0, 0, 0, 0), 0, 0 ));
	}
	
	protected void
	addIntParameter(
		String					label,
		final intValueAdapter	value_adapter )
	{
		component.add( 	new JLabel(label),
				new VWGridBagConstraints(
						0, row, 1, 1, 0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL, 
						new Insets(0, 8, 0, 0), 0, 0 ));

		NumberFormat nf = NumberFormat.getNumberInstance();
		
		nf.setParseIntegerOnly(true);
		
		final JFormattedTextField	tf = new JFormattedTextField(nf);
		
		tf.setColumns(6);
		tf.setHorizontalAlignment( JFormattedTextField.RIGHT );
		
		tf.setValue( new Long( value_adapter.getValue()));
		
		tf.addPropertyChangeListener(
				"value",
				new PropertyChangeListener()
				{
					public void
					propertyChange(
						PropertyChangeEvent	ev )
					{
						Long l = (Long)tf.getValue();
						
						int	val = l.intValue();
						
						if ( val < 0 ){
							 
							val = 0;
						}
						
						try{
							value_adapter.setValue( val );
							
						}catch( Throwable e ){
							
							tf.setValue(new Long(value_adapter.getValue()));
							
							main.reportError(e);
						}
					}
				});
		
		
		component.add( 	tf,
						new VWGridBagConstraints(
							1, row, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.HORIZONTAL, 
							new Insets(0, 8, 0, 0), 0, 0 ));
		
		component.add( 	new JPanel(),
						new VWGridBagConstraints(
							2, row, 1, 1, 1.0, 0.0,
							GridBagConstraints.WEST,
							GridBagConstraints.HORIZONTAL, 
							new Insets(0, 8, 0, 0), 0, 0 ));
		
		row++;
	}
	
	public JComponent
	getComponent()
	{
		return( component );
	}
	
	protected interface
	intValueAdapter
	{
		public int
		getValue();

		public void
		setValue(
			int		value );
	}
}
