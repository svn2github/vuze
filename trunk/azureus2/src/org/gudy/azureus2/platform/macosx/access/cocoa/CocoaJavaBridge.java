package org.gudy.azureus2.platform.macosx.access.cocoa;

/*
 * Created on 26-Mar-2005
 * Created by James Yeh
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

import com.apple.cocoa.application.NSWorkspace;
import com.apple.cocoa.foundation.NSArray;
import com.apple.cocoa.foundation.NSDictionary;
import org.gudy.azureus2.platform.macosx.NativeInvocationBridge;

import java.io.File;
import java.util.Enumeration;

/**
 * Performs PlatformManager tasks using Cocoa-Java
 * @version 1.0
 */
public final class CocoaJavaBridge extends NativeInvocationBridge
{
    /**
     * The path the Cocoa-Java class files are located at
     */
    private static final String CLASS_PATH = "/system/library/java";

    public CocoaJavaBridge(){}

    /**
     * {@inheritDoc}
     */
    protected boolean performRecoverableFileDelete(File path)
    {
        if(!path.exists())
            return false;

        return NSWorkspace.sharedWorkspace().performFileOperation(
                NSWorkspace.RecycleOperation,
                path.getParent(),
                "",
                new NSArray(path.getName())
        ) != -1;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean showInFinder(File path)
    {
        return NSWorkspace.sharedWorkspace().selectFile(path.getAbsolutePath(), path.getAbsolutePath());
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isEnabled()
    {
        // simple check with classpath
        return System.getProperty("java.class.path").toLowerCase().indexOf(CLASS_PATH) != -1;
    }

    /**
     * Gets the application name with the specified identifier, <b>if it is running</b>
     * @param identifier The bundle identifier (e.g. com.apple.TextEdit)
     * @return The application name; or null if application is not found
     */
    protected String launchedApplicationNameWithIdentifier(String identifier)
    {
        NSArray launchedApps = NSWorkspace.sharedWorkspace().launchedApplications();
        Enumeration en = launchedApps.objectEnumerator();
        while (en.hasMoreElements())
        {
            NSDictionary dictionary = (NSDictionary) en.nextElement();
            if(identifier.equals(dictionary.objectForKey("NSApplicationBundleIdentifier")))
                return (String)dictionary.objectForKey("NSApplicationName");
        }

        return null;
    }
}
