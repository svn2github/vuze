/*
 * Created on 13-Mar-2004
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

package org.gudy.azureus2.platform.macosx;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;


/**
 * Performs platform-specific operations with Mac OS X
 * @see PlatformManager
 * @author James Yeh
 * @version 1.0 Initial Version
 */
public class PlatformManagerImpl implements PlatformManager
{

    protected static PlatformManagerImpl singleton;
    protected static AEMonitor class_mon = new AEMonitor("PlatformManager");

    private static final String USERDATA_PATH = new File(System.getProperty("user.home") + "/Library/Application Support/").getPath();

    //T: PlatformManagerCapabilities
    private final HashSet capabilitySet = new HashSet();

    /**
     * Gets the platform manager singleton, which was already initialized
     */
    public static PlatformManagerImpl getSingleton()
    {
       return singleton;
    }

    /**
     * Instantiates the singleton
     */
    static
    {
        initializeSingleton();
    }

    /**
     * Instantiates the singleton
     */
    private static void initializeSingleton()
    {
        try
        {
            class_mon.enter();
            singleton = new PlatformManagerImpl();
        }
        catch (Throwable e)
        {
            LGLogger.log("Failed to initialize platform manager for Mac OS X", e);
        }
        finally
        {
            class_mon.exit();
        }
    }

    /**
     * Creates a new PlatformManager and initializes its capabilities
     */
    public PlatformManagerImpl()
    {
        capabilitySet.add(PlatformManagerCapabilities.RecoverableFileDelete);
        capabilitySet.add(PlatformManagerCapabilities.ShowFileInBrowser);
        capabilitySet.add(PlatformManagerCapabilities.ShowPathInCommandLine);
        capabilitySet.add(PlatformManagerCapabilities.CreateCommandLineProcess);
        capabilitySet.add(PlatformManagerCapabilities.GetUserDataDirectory);
        capabilitySet.add(PlatformManagerCapabilities.NativeScripting);
        capabilitySet.add(PlatformManagerCapabilities.PlaySystemAlert);
    }

    /**
     * {@inheritDoc}
     */
    public int getPlatformType()
    {
        return PT_MACOSX;
    }

    /**
     * {@inheritDoc}
     * @see org.gudy.azureus2.core3.util.SystemProperties#getUserPath()
     */
    public String getUserDataDirectory() throws PlatformManagerException
    {
        return USERDATA_PATH;
    }

    /**
     * Not implemented; returns True
     */
    public boolean isApplicationRegistered() throws PlatformManagerException
    {
        return true;
    }

    /**
     * Not implemented; does nothing
     */
    public void registerApplication() throws PlatformManagerException
    {
        // handled by LaunchServices and/0r user interaction
    }

    /**
     * {@inheritDoc}
     */
    public void createProcess(String cmd, boolean inheritsHandles) throws PlatformManagerException
    {
        try
        {
            performRuntimeExec(cmd.split(" "));
        }
        catch (Throwable e)
        {
            throw new PlatformManagerException("Failed to create process", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void performRecoverableFileDelete(String path) throws PlatformManagerException
    {
        try
        {
            StringBuffer sb = new StringBuffer();
            sb.append("tell application \"");
            sb.append(getFileBrowserName());
            sb.append("\" to move (posix file \"");
            sb.append(path);
            sb.append("\" as alias) to the trash");

            performOSAScript(sb);
        }
        catch (Throwable e)
        {
            throw new PlatformManagerException("Failed to move file", e);
        }
    }

    public boolean hasCapability(PlatformManagerCapabilities capability)
    {
        return capabilitySet.contains(capability);
    }

    // Public utility methods not shared across the interface

    /**
     * Plays the system alert (the jingle is specified by the user in System Preferences)
     */
    public void playSystemAlert()
    {
        try
        {
            performRuntimeExec(new String[]{"beep"});
        }
        catch (IOException e)
        {
            LGLogger.log(LGLogger.AT_WARNING, "Cannot play system alert");
            LGLogger.log(e);
        }
    }

    /**
     * <p>Shows the given file or directory in Finder</p>
     * <p>If Path Finder is running, it is used instead</p>
     * @param path Absolute path to the file or directory
     */
    public void showInFinder(String path)
    {
        showInFinder(new File(path));
    }

    /**
     * <p>Shows the given file or directory in Finder</p>
     * <p>If Path Finder is running, it is used instead</p>
     * @param path Absolute path to the file or directory
     */
    public void showInFinder(File path)
    {
        if(path.exists())
        {
            StringBuffer sb = new StringBuffer();
            sb.append("tell application \"");
            sb.append(getFileBrowserName());
            sb.append("\" to reveal (posix file \"");
            sb.append(path);
            sb.append("\" as alias)");

            try
            {
                performOSAScript(sb);
            }
            catch (IOException e)
            {
                LGLogger.logUnrepeatableAlert(LGLogger.AT_ERROR, e.getMessage());
            }
        }
        else
        {
            LGLogger.log(LGLogger.AT_WARNING, "Cannot find " + path.getName());
        }
    }

    /**
     * <p>Shows the given file or directory in Terminal by executing cd /absolute/path/to</p>
     * @param path Absolute path to the file or directory
     */
    public void showInTerminal(String path)
    {
        showInTerminal(new File(path));
    }

    /**
     * <p>Shows the given file or directory in Terminal by executing cd /absolute/path/to</p>
     * @param path Absolute path to the file or directory
     */
    public void showInTerminal(File path)
    {
        if(path.isFile())
            path = path.getParentFile();

        if(path != null && path.isDirectory())
        {
            StringBuffer sb = new StringBuffer();
            sb.append("tell application \"");
            sb.append("Terminal");
            sb.append("\" to do script \"cd ");
            sb.append(path.getAbsolutePath().replaceAll(" ", "\\ "));
            sb.append("\"");

            try
            {
                performOSAScript(sb);
            }
            catch (IOException e)
            {
                LGLogger.logUnrepeatableAlert(LGLogger.AT_ERROR, e.getMessage());
            }
        }
        else
        {
            LGLogger.log(LGLogger.AT_WARNING, "Cannot find " + path.getName());
        }
    }

    // Internal utility methods

    /**
     * Compiles a new AppleScript instance and runs it
     * @param cmd AppleScript command to execute; do not surround command with extra quotation marks
     * @return Output of the script
     * @throws IOException If the script failed to execute
     */
    protected static String performOSAScript(CharSequence cmd) throws IOException
    {
        Process osaProcess = performRuntimeExec(new String[]{"osascript", "-e", String.valueOf(cmd)});
        BufferedReader reader = new BufferedReader(new InputStreamReader(osaProcess.getInputStream()));
        String line = reader.readLine();
        reader.close();

        reader = new BufferedReader(new InputStreamReader(osaProcess.getErrorStream()));
        String errorMsg = reader.readLine();
        reader.close();

        if(errorMsg != null)
            throw new IOException(errorMsg);

        return line;
    }

    /**
     * @see Runtime#exec(String[])
     */
    protected static Process performRuntimeExec(String[] cmdargs) throws IOException
    {
        try
        {
            return Runtime.getRuntime().exec(cmdargs);
        }
        catch (IOException e)
        {
            LGLogger.logUnrepeatableAlert(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * <p>Gets the preferred file browser name</p>
     * <p>Currently supported browsers are Path Finder and Finder. If Path Finder is currently running
     * (not just installed), then "Path Finder is returned; else, "Finder" is returned.</p>
     * @return "Path Finder" if it is currently running; else "Finder"
     */
    private static String getFileBrowserName()
    {
        try
        {
            if(performOSAScript("tell application \"System Events\" to exists process \"Path Finder\"").equalsIgnoreCase("true"))
            {
                //Debug.out("Path Finder is running");
                return "Path Finder";
            }
        }
        finally
        {
            return "Finder";
        }
    }
}
