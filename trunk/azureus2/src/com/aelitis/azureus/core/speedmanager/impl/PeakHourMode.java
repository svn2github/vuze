/**
 * Created on May 21, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.speedmanager.impl;

import org.gudy.azureus2.core3.util.SystemTime;

import java.util.Calendar;

public class PeakHourMode {
    public static final PeakHourMode PEAK_HOUR = new PeakHourMode("PEAK_HOUR");
    public static final PeakHourMode NOT_PEAK_HOUR = new PeakHourMode("NOT_PEAK_HOUR");
    public static final PeakHourMode USER_DEFINED_PEAK_HOUR = new PeakHourMode("USER_DEFINED_PEAK_HOUR");
    public static final PeakHourMode USER_DEFINED_NOT_PEAK_HOUR = new PeakHourMode("USER_DEFINED_NOT_PEAK_HOUR");

    private final String name;

    private PeakHourMode(String _name) {
        name = _name;
    }

    public PeakHourMode getPeakHourMode(){

        //The time of day and day of week determine peak hours.
        long currTime = SystemTime.getCurrentTime();

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(currTime);

        int dayOfWeek = c.get( Calendar.DAY_OF_WEEK );
        int hour = c.get( Calendar.HOUR_OF_DAY );

        //On M-F peak hours are 6 PM to 11 PM
        if( dayOfWeek!=Calendar.SATURDAY && dayOfWeek!=Calendar.SUNDAY ){
            if( hour > 18 ){
                return PEAK_HOUR;
            }else{
                return NOT_PEAK_HOUR;
            }
        }

        //On Sat-Sun peak hours are 10 AM to 10 PM
        if( hour>10 && hour<22 ){
            return PEAK_HOUR;
        }else{
            return NOT_PEAK_HOUR;
        }

    }

    public String toString() {
        return name;
    }
}
