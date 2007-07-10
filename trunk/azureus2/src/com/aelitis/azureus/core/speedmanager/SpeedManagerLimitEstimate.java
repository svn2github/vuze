/*
 * Created on Jul 5, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.speedmanager;

public interface 
SpeedManagerLimitEstimate 
{
	public static final float RATING_UNKNOWN			= -0.1f;
	public static final float RATING_ESTIMATED			=  0.0f;
	public static final float RATING_MEASURED_MIN		= +0.8f;
	public static final float RATING_MEASURED			= +0.9f;
	public static final float RATING_MANUAL				= +0.1f;
	
	public int
	getBytesPerSec();
	
	public float
	getMetricRating();
	
	public int[][]
	getSegments();
	
	public String
	getString();
}
