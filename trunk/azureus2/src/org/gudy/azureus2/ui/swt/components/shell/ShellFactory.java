package org.gudy.azureus2.ui.swt.components.shell;

/*
 * Created on 17-Mar-2005
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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainMenu;
import org.gudy.azureus2.core3.util.Constants;

/**
 * Facilitates the creation of SWT Shells with platform-specific additions.
 * All shells normal to the user should be created from ShellFactory
 * @version 1.0
 * @author James Yeh
 */
public final class ShellFactory
{
    /**
     * <p>Creates a shell</p>
     * <p>For platforms that use a unified menu bar, the shell's menu bar is set to the main window's menu bar</p>
     * @see org.eclipse.swt.widgets.Shell
     */
    public static Shell createShell(final Display disp, final int styles)
    {
        return getRegisteredShell(new AEShell(disp, styles));
    }

    /**
     * <p>Creates a shell</p>
     * <p>For platforms that use a unified menu bar, the shell's menu bar is set to the main window's menu bar</p>
     * @see org.eclipse.swt.widgets.Shell
     */
    public static Shell createShell(final Display disp)
    {
        return getRegisteredShell(new AEShell(disp));
    }

    /**
     * <p>Creates a shell</p>
     * <p>For platforms that use a unified menu bar, the shell's menu bar is set to the main window's menu bar</p>
     * @see org.eclipse.swt.widgets.Shell
     */
    public static Shell createShell(final Shell parent, final int styles)
    {
        return getRegisteredShell(new AEShell(parent, styles));
    }

    /**
     * <p>Creates a shell</p>
     * <p>For platforms that use a unified menu bar, the shell's menu bar is set to the main window's menu bar</p>
     * @see org.eclipse.swt.widgets.Shell
     */
    public static Shell createShell(final Shell parent)
    {
        return getRegisteredShell(new AEShell(parent));
    }

    /**
     * <p>Creates a shell</p>
     * <p>For platforms that use a unified menu bar, the shell's menu bar is set to the main window's menu bar</p>
     * @see org.eclipse.swt.widgets.Shell
     */
    public static Shell createShell(final int styles)
    {
        return getRegisteredShell(new AEShell(styles));
    }

    /**
     * <p>Gets the registered shell</p>
     * <p>Registration entails setting its menu bar if platform uses a unified menu bar.
     * Also, the shell is added to the shared ShellManager</p>
     * @param toRegister A SWT Shell
     * @return The SWT Shell
     */
    private static Shell getRegisteredShell(final Shell toRegister)
    {
        // register main menu
        if(Constants.isOSX)
        {
            if(MainWindow.getWindow() == null)
                throw new IllegalStateException("Main window is not initialized yet");

            new MainMenu(toRegister);
        }

        ShellManager.sharedManager().addWindow(toRegister);

        return toRegister;
    }

    /**
     * A shell that provides platform-specific behaviour in some methods in order to better suit the user experience
     */
    private static class AEShell extends Shell
    {
        /**
         * {@inheritDoc}
         */
        private AEShell(int styles)
        {
            super(styles);
        }

        /**
         * {@inheritDoc}
         */
        private AEShell(Display display)
        {
            super(display);
        }

        /**
         * {@inheritDoc}
         */
        private AEShell(Display display, int styles)
        {
            super(display, styles);
        }

        /**
         * {@inheritDoc}
         */
        private AEShell(Shell parent)
        {
            super(parent);
        }

        /**
         * {@inheritDoc}
         */
        private AEShell(Shell parent, int styles)
        {
            super(parent, styles);
        }

        /**
         * Does nothing
         */
        protected void checkSubclass() {}

        /**
         * <p>Sets the iconic representation of a SWT window</p>
         * <p>The icon is often located at the top-left corner of the title bar. This is different from Mac OS X's
          * document proxy icon.</p>
         * <p> For Mac OS X, this method does nothing (because the dock's image would be set instead).</p>
         * @param shell The SWT window
         * @param imgKey ImageRepository key for the image
         */
        public void setImage(final Image image)
        {
            if(!Constants.isOSX)
                super.setImage(image);
        }


        /**
         * <p>Sets the iconic representation of a SWT window</p>
         * <p>The icon is often located at the top-left corner of the title bar. This is different from Mac OS X's
         * document proxy icon.</p>
         * <p> For Mac OS X, this method does nothing (because the dock's image would be set instead).</p>
         * @param shell The SWT window
         * @param images Images
         */
        public void setImages(final Image[] images)
        {
            if(!Constants.isOSX)
                super.setImages(images);
        }
    }
}
