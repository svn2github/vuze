package com.aelitis.azureus.core.speedmanager.impl;

/**
 * Created on Jun 5, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class SpeedLimitConfidence
    implements Comparable
{
    public static final SpeedLimitConfidence NONE = new SpeedLimitConfidence("NONE",0);
    public static final SpeedLimitConfidence LOW = new SpeedLimitConfidence("LOW",1);
    public static final SpeedLimitConfidence MED = new SpeedLimitConfidence("MED",2);
    public static final SpeedLimitConfidence HIGH = new SpeedLimitConfidence("HIGH",3);
    public static final SpeedLimitConfidence ABSOLUTE = new SpeedLimitConfidence("ABSOLUTE",4);

    private final String name;
    private final int order;

    private SpeedLimitConfidence(String _name, int _order){
        name = _name;
        order = _order;
    }

    /**
     * Turns a string into a SpeedLimitConfidence class.
     * @param setting - String is expected to be one of: NONE, LOW, MED, HIGH, ABSOLUE.
     * @return - class corresponding to String. If it isn't one of the know values then NONE is returned.
     */
    public static SpeedLimitConfidence parseString(String setting){
        SpeedLimitConfidence retVal = SpeedLimitConfidence.NONE;

        if(setting==null){
            return retVal;
        }

        if( "NONE".equalsIgnoreCase(setting) ){
            return SpeedLimitConfidence.NONE;
        }else if("LOW".equalsIgnoreCase(setting)){
            return SpeedLimitConfidence.LOW;
        }else if("MED".equalsIgnoreCase(setting)){
            return SpeedLimitConfidence.MED;
        }else if("HIGH".equalsIgnoreCase(setting)){
            return SpeedLimitConfidence.HIGH;
        }else if("ABSOLUTE".equalsIgnoreCase(setting)){
            return SpeedLimitConfidence.ABSOLUTE;
        }

        return retVal;
    }

    public String getString(){
        return name;
    }

    /**
     * Comparable interface
     * @param limitConf - Item to compare with.
     * @return -
     */
    public int compareTo(SpeedLimitConfidence limitConf){
        return  (order - limitConf.order);
    }

    /**
     * Comparable interface.
     * @param obj -
     * @return -
     */
    public int compareTo(Object obj){
        if( !(obj instanceof SpeedLimitConfidence)  ){
            throw new ClassCastException("Only comparable to SpeedLimitConfidence class.");
        }
        SpeedLimitConfidence casted = (SpeedLimitConfidence) obj;
        return compareTo(casted);
    }

}
