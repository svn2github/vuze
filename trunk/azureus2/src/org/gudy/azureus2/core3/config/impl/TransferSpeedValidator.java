package org.gudy.azureus2.core3.config.impl;

import org.gudy.azureus2.core3.config.COConfigurationManager;

//todo extract if more validators are needed

/**
 * Provides validation for transfer speed settings
 * @version 1.0
 * @since 1.4
 * @author James Yeh
 */
public final class TransferSpeedValidator
{
    private static final String UPLOAD_CONFIGKEY =  "Max Upload Speed KBs";
    private static final String DOWNLOAD_CONFIGKEY =  "Max Download Speed KBs";

    /**
     * Validates the given configuration key/value pair and returns the validated value
     * @param configKey Configuration key
     * @param value Configuration value
     * @return Modified configuration value that conforms to validation
     */
    public static Object validate(final String configKey, final Object value)
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
     * Validates the given configuration key/value pair and saves the user configuration
     * @param configKey Configuration key
     * @param value Configuration value
     */
    public static void validateAndSave(final String configKey, final Object value)
    {
        assert value instanceof Number;

        COConfigurationManager.setParameter(configKey, ((Number)validate(configKey, value)).intValue());
        COConfigurationManager.save();
    }
}
