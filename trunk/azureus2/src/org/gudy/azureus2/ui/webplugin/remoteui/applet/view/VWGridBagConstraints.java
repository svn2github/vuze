/*
 * File    : VWGridBagConstraints.java
 * Created : 19-Feb-2004
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

public class 
VWGridBagConstraints 
	extends GridBagConstraints
{
		public 
		VWGridBagConstraints(
				int gridx, int gridy,
	            int gridwidth, int gridheight,
	            double weightx, double weighty,
	            int anchor, int fill,
	            Insets insets, int ipadx, int ipady)
		{
			super();
			
			this.gridx = gridx;
			this.gridy = gridy;
			this.gridwidth = gridwidth;
			this.gridheight = gridheight;
			this.fill = fill;
			this.ipadx = ipadx;
			this.ipady = ipady;
			this.insets = insets;
			this.anchor  = anchor;
			this.weightx = weightx;
			this.weighty = weighty;
		}
}