/*
 * File    : ViewUtils.java
 * Created : 24-Oct-2003
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
 
package org.gudy.azureus2.ui.swt.views;

/**
 * @author parg
 */

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;

public class 
ViewUtils 
{

	public static void
	setText(
		Label		lab,
		String		value )
	{
		if ( lab.isDisposed()){
			return;
		}
		
		String old_value = lab.getText();
		
		if ( old_value == value ){
			
			return;
		}
		
		if (	old_value != null && 
				value != null &&
				old_value.equals( value )){
					
			return;
		}
		
		lab.setText( value==null?"":value );
	}
	
	public static void
	setText(
		TableItem	item,
		int			index,
		String		value )
	{
		if ( item.isDisposed()){
			return;
		}
		
		String old_value = item.getText( index );
		
		if ( old_value == value ){
			
			return;
		}
		
		if (	old_value != null && 
				value != null &&
				old_value.equals( value )){
					
			return;
		}
		
		item.setText( index, value==null?"":value );
	}
}
