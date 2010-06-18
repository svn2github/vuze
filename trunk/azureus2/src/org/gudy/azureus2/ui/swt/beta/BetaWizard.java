/*
 * Created on May 24, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.ui.swt.beta;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

public class 
BetaWizard
	extends Wizard
{	
	private boolean beta_enabled = COConfigurationManager.getBooleanParameter( "Beta Programme Enabled" );
	
	private boolean	finished;
	
	public
	BetaWizard()
	{
		super( "beta.wizard.title", false );
			
		BetaWizardStart panel = new BetaWizardStart( this );
		
		setFirstPanel( panel );
	}
	
	public void 
	onClose()
	{
		super.onClose();
		
		if ( finished ){
			
			COConfigurationManager.setParameter( "Beta Programme Enabled",beta_enabled );
		}
	}
	
	protected boolean
	getBetaEnabled()
	{
		return( beta_enabled );
	}
	
	protected void
	setBetaEnabled(
		boolean b )
	{
		beta_enabled = b;
	}
	
	public void
	finish()
	{
		finished = true;
		
		close();
	}
}
