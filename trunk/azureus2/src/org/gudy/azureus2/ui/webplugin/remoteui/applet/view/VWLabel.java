/*
 * File    : VWLabel.java
 * Created : 14-Mar-2004
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


import java.awt.*;
import javax.swing.*;

public class 
VWLabel
	extends JLabel
{
	protected Dimension	fixed_size;
	
	protected int		minimum_width		= -1;
	protected int		minimum_height		= -1;
		

	public
	VWLabel(
		String		str )
	{
		super( str );
	}
	
	public void
	setFixedSize(
		Dimension	s )
	{
		fixed_size	= s;
		
		setSize(fixed_size);
	}
	
	public void
	setMinimumWidth(
		int		_min )
	{
		minimum_width = _min;
		
		setSize( getSize());
	}
	
	public void
	setMinimumHeight(
		int		_min )
	{
		minimum_height = _min;
		
		setSize( getSize());
	}
					
	public Dimension
	getPreferredSize()
	{
		return( fix( super.getPreferredSize()));
	}
	
	public Dimension
	getMinimumSize()
	{
		return(fix( super.getMinimumSize()));
	}
	
	protected Dimension
	fix(
		Dimension	dim )
	{
		int	width	= fixed_size==null||fixed_size.width==-1?dim.width:fixed_size.width;
		int	height	= fixed_size==null||fixed_size.height==-1?dim.height:fixed_size.height;
		
		if ( minimum_width != -1 && width < minimum_width ){
			
			width = minimum_width;
		}
		
		if ( minimum_height != -1 && height < minimum_height ){
			
			height = minimum_height;
		}
		
		return( new Dimension( width, height ));
	}
	
	public void
	setBounds(
		int		_x,
		int		_y,
		int		_width,
		int		_height )
	{		
		Dimension temp = fix( new Dimension( _width, _height ));
				
		super.setBounds( _x, _y, temp.width, temp.height );
	}
}

