package org.gudy.azureus2.ui.swt.components;

/*
 * Created on 19-Feb-2005
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.gudy.azureus2.core3.util.Debug;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * <p>
 * Provides a generic flat button that looks and feels like flat bordered buttons found on Mac OS X
 * </p>
 * <p>
 * Button usage entails supplying a PNG image or a resource URL that contains the PNG image. If an URL
 * is supplied, disabled states and pressed states are <i>filename</i>.png.disabled.png and <i>filename</i>.png.pressed.png,
 * respectively.
 * <p>
 * Traversal behaviour is currently not implemented.
 * </p>
 * @author James Yeh
 * @version 1.0
 * @since 1.4
 */
public class FlatImageButton extends Canvas
{
    public static final URL PLUS_BUTTON;
    public static final URL MINUS_BUTTON;

    private static final String RES_PREFIX = "org/gudy/azureus2/ui/icons/imagebutton/";

    private Image[] imageStates = new Image[3];
    private static final int NORMAL = 0;
    private static final int PRESSED = 1;
    private static final int DISABLED = 2;

    private int currentState = NORMAL;

    private LinkedList selectionListeners = new LinkedList();

    /**
     * Initializes the url to special buttons - plus and minus
     */
    static
    {
        PLUS_BUTTON = FlatImageButton.class.getClassLoader().getResource(RES_PREFIX + "plus.png");
        MINUS_BUTTON = FlatImageButton.class.getClassLoader().getResource(RES_PREFIX + "minus.png");
    }

    /**
     * Creates a FlatImageButton
     * @param parent
     * @param style SWT style; the supplied parameter currently unused
     */
    public FlatImageButton(Composite parent, int style)
    {
        super(parent, SWT.NO_MERGE_PAINTS);

        setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        ButtonEventHandler handler = new ButtonEventHandler();
        addPaintListener(handler);
        addDisposeListener(handler);
        addMouseListener(handler);

        super.setSize(16, 16);
    }

    /**
     * Creates a FlatImageButton with an external path to the image
     * @param parent
     * @param filename External path to the image; do not use this for resources
     */
    public FlatImageButton(Composite parent, String filename)
    {
        this(parent, SWT.NONE);
        if(new File(filename).exists())
            setImage(new Image(parent.getDisplay(), filename));
        else
            Debug.out(MessageFormat.format("Image file {0} for FlatImageButton not found. ", new String[]{filename}));
    }

    /**
     * Creates a FlatImageButton with the resource URL
     * @param parent
     * @param resource URL to the resource
     */
    public FlatImageButton(Composite parent, URL resource)
    {
        this(parent, SWT.NONE);
        if(resource != null) {
            setImage(parent.getDisplay(), resource);
        }
        else {
            Debug.out(MessageFormat.format("Image resource for FlatImageButton {0} not found. ", new String[]{this.toString()}));
        }
    }

    /**
     * @see org.eclipse.swt.widgets.Button#addSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    public void addSelectionListener(SelectionListener listener)
    {
        selectionListeners.add(listener);
    }

    /**
     * @see org.eclipse.swt.widgets.Button#removeSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    public void removeSelectionListener(SelectionListener listener)
    {
        selectionListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enable)
    {
        if(currentState == DISABLED && enable)
        {
            currentState = NORMAL;
            redraw();
        }
        else if(!(currentState == PRESSED && enable))
        {
            currentState = DISABLED;
            redraw();
        }

        super.setEnabled(enable);
    }

    /**
     * Sets the image of the FlatImageButton instance. The pressed and disabled states are parsed using SWT's gray and disabled flags,
     * respectively.
     * @param img
     */
    public void setImage(Image img)
    {
        disposePreviousImages();

        imageStates[NORMAL] = img;
        imageStates[PRESSED] = new Image(Display.getCurrent(), img, SWT.IMAGE_GRAY);
        imageStates[DISABLED] = new Image(Display.getCurrent(), img, SWT.IMAGE_DISABLE);

        super.setSize(img.getBounds().width,  img.getBounds().height);
    }

    /**
     * Sets the image of the FlatImageButton instance. Disabled states and pressed states are <i>filename</i>.png.disabled.png
     * and <i>filename</i>.png.pressed.png, respectively.
     * @param context The SWT display to create the SWT image with
     * @param resource URL to the resource
     */
    private void setImage(Display context, URL resource)
    {
        disposePreviousImages();

        try
        {
            imageStates[NORMAL] = new Image(context, resource.openStream());
            imageStates[PRESSED] = new Image(context, new URL(String.valueOf(resource) + ".pressed.png").openStream());
            imageStates[DISABLED] = new Image(context, new URL(String.valueOf(resource) + ".disabled.png").openStream());

            super.setSize(imageStates[NORMAL].getBounds().width,  imageStates[NORMAL].getBounds().height);
        }
        catch (IOException e)
        {
            Debug.printStackTrace(e);
        }
    }

    /**
     * Disposes the already-instantiated button images, if any
     */
    private void disposePreviousImages()
    {
        for(int i = 0; i < imageStates.length; i++)
        {
            Image imageState = imageStates[i];
            if(imageState != null)
                imageState.dispose();
        }
    }

    /**
     * Executes the callbacks registered with the selection listeners
     */
    private void executeCallback()
    {
        final Event ev = new Event();
        ev.type = SWT.Selection;
        ev.item = this;
        ev.widget = this;
        SelectionEvent selev = new SelectionEvent(ev);

        Iterator it = selectionListeners.iterator();
        while(it.hasNext())
        {
            SelectionListener listener = (SelectionListener)it.next();
            listener.widgetSelected(selev);
        }
    }

    /**
     * Provides event handling for FlatImageButton. Currently handled events are dispose, paint, mouse (left button), and keyboard (return)
     */
    private class ButtonEventHandler extends MouseAdapter implements DisposeListener, PaintListener, KeyListener
    {
        /**
         * {@inheritDoc}
         */
        public void widgetDisposed(DisposeEvent disposeEvent)
        {
            FlatImageButton.this.disposePreviousImages();
        }

        /**
         * {@inheritDoc}
         */
        public void paintControl(PaintEvent e)
        {
            Image img = imageStates[currentState];
            if(img == null)
            {
                e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_CYAN));
                e.gc.fillRectangle(getBounds());
                return;
            }

            // simple image
            int x = Math.max(0, 0 + (getSize().x - imageStates[currentState].getBounds().width) / 2);
            int y = Math.max(0, 0 + (getSize().y - imageStates[currentState].getBounds().height) / 2);

            e.gc.drawImage(img, x, y);

            if(isFocusControl())
            {
                e.gc.drawFocus(
                        x,
                        y,
                        imageStates[currentState].getBounds().width,
                        imageStates[currentState].getBounds().height
                );
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseDown(MouseEvent mouseEvent)
        {
            currentState = (isEnabled() ? PRESSED : DISABLED);
            redraw();

            if(isEnabled() && mouseEvent.button == 1)
            {
                executeCallback();
                mouseUp(mouseEvent); // for after modal action
                redraw();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void mouseUp(MouseEvent mouseEvent)
        {
            currentState = (isEnabled() ? NORMAL : DISABLED);
            redraw();
        }

        /**
         * {@inheritDoc}
         */
        public void keyPressed(KeyEvent event)
        {
            if(event.character != SWT.CR) {return;}

            currentState = (isEnabled() ? PRESSED : DISABLED);
            redraw();

            if(isEnabled())
            {
                executeCallback();
                keyReleased(event);
                redraw();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void keyReleased(KeyEvent event)
        {
            if(event.character != SWT.CR) {return;}

            currentState = (isEnabled() ? NORMAL : DISABLED);
            redraw();
        }
    }
}
