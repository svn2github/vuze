/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.azureus.core.util.loopcontrol.impl;

import com.aelitis.azureus.core.util.loopcontrol.LoopControler;

public class PIDLoopControler implements LoopControler {

	final double pGain;
	final double iGain;
	final double dGain;
	
	double iState;
	static final double iMin = -5000;
	static final double iMax =  5000;
	
	double dState;
	
	public PIDLoopControler(double pGain,double iGain, double dGain) {
		this.pGain = pGain;
		this.iGain = iGain;
		this.dGain = dGain;
	}
	
	public double updateControler(double error, double position) {
		
		//Proportional
		double pTerm = pGain * error;
		
		
		iState += error;
		if(iState > iMax) iState = iMax;
		if(iState < iMin) iState = iMin;
		
		double iTerm = iGain * iState;
		
		double d = dState - position;
		
		double dTerm = dGain * d;
		dState = position;

		double result = pTerm + iTerm - dTerm;
		
		System.out.println("PID p,i,d (" + pGain + "," + iGain + "," + dGain +") : is,ds (" + iState + "," + d + ") p,i,d (" + pTerm + "," + iTerm + "," + dTerm + ") => " + result);
		
		return result;
	}
	
	public void reset() {
		dState = 0;
		iState = 0;
	}

}
