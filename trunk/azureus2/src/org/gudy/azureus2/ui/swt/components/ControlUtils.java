package org.gudy.azureus2.ui.swt.components;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * General utility methods for SWT controls and components
 * @version 1.0
 * @author James Yeh
 * @deprecated JFace has higher-level classes and fields to cover this
 */
public final class ControlUtils
{
    private static boolean smallOSXControl = COConfigurationManager.getBooleanParameter("enable_small_osx_fonts");

    /**
     * <p>Gets the margin between buttons</p>
     * <p>The margin may vary between platforms, as specified by their guidelines</p>
     * @return Margin
     */
    public static int getButtonMargin()
    {
        if(Constants.isOSX)
            return (smallOSXControl) ? 10 : 12;
        else if(Constants.isWindows)
            return 6;
        else
            return 6; // this is gnome's
    }

    /**
     * <p>Gets the minimum width of a button in a dialog (usually for alerts)</p>
     * <p>The size may vary between platforms, as specified by their guidelines</p>
     * @return Width
     */
    public static int getDialogButtonMinWidth()
    {
        if(Constants.isOSX)
            return 90;
        else
            return 70;
    }

    /**
     * <p>Gets the minimum height of a button in a dialog (usually for alerts)</p>
     * <p>The size may vary between platforms, as specified by their guidelines</p>
     * @return Height
     */
    public static int getDialogButtonMinHeight()
    {
        return 20;
    }
}
