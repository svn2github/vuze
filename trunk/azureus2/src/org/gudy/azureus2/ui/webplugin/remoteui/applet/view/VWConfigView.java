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

	public
	VWConfigView(
		final RemoteUIMainPanel	_main,
		final MDConfigModel		_model )
	{
		component = new JPanel(new GridBagLayout());
		
		int	row = 0;
		
			// refresh rate
		
		component.add( 	new JLabel("Auto-refresh period (secs) - 0 = never"),
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
		
		tf.setValue( new Long( _model.getRefreshPeriod()));
		
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
							_model.setRefreshPeriod(val);
							
						}catch( Throwable e ){
							
							_main.reportError(e);
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
		
			// max upload
		
		component.add( 	new JLabel("Max global upload speed KB/s [0: unlimited]"),
						new VWGridBagConstraints(
								0, row, 1, 1, 0.0, 0.0,
								GridBagConstraints.WEST,
								GridBagConstraints.HORIZONTAL, 
								new Insets(0, 8, 0, 0), 0, 0 ));

		NumberFormat nf2 = NumberFormat.getNumberInstance();
		
		nf2.setParseIntegerOnly(true);
		
		final JFormattedTextField	tf2 = new JFormattedTextField(nf);
		
		tf2.setColumns(6);
		tf2.setHorizontalAlignment( JFormattedTextField.RIGHT );
		
		tf2.setValue( new Long( _model.getMaxUploadSpeed()));
		
		tf2.addPropertyChangeListener(
				"value",
				new PropertyChangeListener()
				{
					public void
					propertyChange(
						PropertyChangeEvent	ev )
					{
						Long l = (Long)tf2.getValue();
						
						int	val = l.intValue();
						
						if ( val < 0 ){
							 
							val = 0;
						}
						
						try{
							_model.setMaxUploadSpeed(val);
							
						}catch( Throwable e ){
							
							tf2.setValue(new Long(_model.getMaxUploadSpeed()));
							
							_main.reportError(e);
						}
					}
				});
		
		
		component.add( 	tf2,
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
		
		component.add( 	new JPanel(),
						new VWGridBagConstraints(
							0, row++, 3, 1, 1.0, 1.0,
							GridBagConstraints.WEST,
							GridBagConstraints.BOTH, 
							new Insets(0, 0, 0, 0), 0, 0 ));
	}
	
	public JComponent
	getComponent()
	{
		return( component );
	}
}
