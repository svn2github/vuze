package org.gudy.azureus2.core3.config.impl;

/*
 * Created on 13-Feb-2005
 * Created by James Yeh
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * Provides validation for transfer speed settings
 * @version 1.0
 * @since 1.4
 * @author James Yeh
 */
public final class TransferSpeedValidator
{
    public static final String UPLOAD_CONFIGKEY =  "Max Upload Speed KBs";
    public static final String DOWNLOAD_CONFIGKEY =  "Max Download Speed KBs";

    private final String configKey;
    private final Object configValue;

    /**
     * Creates a TransferSpeedValidator with the given configuration key and value
     * @param configKey Configuration key; must be "Max Upload Speed KBs" or "Max Download Speed KBs"
     * @param value Configuration value to be validated
     */
    public TransferSpeedValidator(final String configKey, final Object value)
    {
        this.configKey = configKey;
        configValue = value;
    }

    /**
     * Gets the transformed value as an Integer
     */
    private static Object validate(final String configKey, final Object value)
    {
        assert value instanceof Number;

        int newValue = ((Number)value).intValue();

        if(newValue < 0)
        {
            newValue = 0;
        }

        if(configKey == UPLOAD_CONFIGKEY)
        {
            final int downValue = COConfigurationManager.getIntParameter(DOWNLOAD_CONFIGKEY);

            if(
                    newValue != 0 &&
                    newValue < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED &&
                    (downValue == 0 || downValue > newValue*2)
            )
            {
                newValue = (downValue + 1)/2;
                //COConfigurationManager.setParameter(DOWNLOAD_CONFIGKEY, newValue * 2);
            }
        }
        else if(configKey == DOWNLOAD_CONFIGKEY)
        {
            final int upValue = COConfigurationManager.getIntParameter(UPLOAD_CONFIGKEY);

            if(
                    upValue != 0 &&
                    upValue < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED
            )
            {
                if(newValue > upValue*2)
                {
                    newValue = upValue*2;
                    //COConfigurationManager.setParameter(UPLOAD_CONFIGKEY, (newValue+1)/2);
                }
                else if(newValue == 0)
                {
                    newValue = upValue*2;
                    //COConfigurationManager.setParameter(UPLOAD_CONFIGKEY, 0);
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid Configuation Key; use key for max upload and max download");
        }

        return new Integer(newValue);
        //return value;
    }

    /**
     * Validates the given configuration key/value pair and returns the validated value
     * @return Modified configuration value that conforms to validation as an Integer
     */
    public Object getValue()
    {
        return validate(configKey, configValue);
    }
}
