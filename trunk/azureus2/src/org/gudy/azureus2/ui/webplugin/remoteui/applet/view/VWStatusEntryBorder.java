/*
 * File    : VWStatusEntryBorder.java
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

/**
 * @author parg
 *
 */

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import javax.swing.border.*;

public class 
VWStatusEntryBorder
	extends AbstractBorder
{
	public
	VWStatusEntryBorder()
	{
	}
		
    public void 
	paintBorder(
		Component	c, 
		Graphics	g, 
		int			x, 
		int			y, 
		int			width, 
		int			height ) 
	{
		Color oldColor = g.getColor();
			
		int	rhs = x+width-1;
		int	bot = y+height-1;
		
		g.setColor(Color.white);
		
		g.drawLine(x+0, y+1,   x+rhs-0,y+1);
		g.drawLine(x+1, y+bot, x+rhs  ,y+bot);
		g.drawLine(x+rhs, y+1, x+rhs  ,y+bot);
		
		g.setColor( Color.gray );
		
		g.drawLine( x+1, y+2, x+rhs-1, y+2 );
		g.drawLine( x+1, y+2, x+1, y+bot-1 );
				
        g.setColor(oldColor);
    }
	
    public Insets 
	getBorderInsets(
		Component c)       
	{
        return( new Insets(5, 5, 1, 1));
    }

    public Insets 
	getBorderInsets(
		Component c, Insets insets) 
	{
        insets.left 	= 5; 
        insets.top 		= 5;
        insets.right = insets.bottom 	= 1;
		
        return insets;
    }

    public boolean 
	isBorderOpaque() 
	{
		return( false );
	}
}