package org.gudy.azureus2.ui.swt.predicate.shellmanager;

import org.gudy.azureus2.core3.predicate.Predicable;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.eclipse.swt.widgets.Shell;

import java.util.Iterator;

/**
 * @version 1.0
 */
public final class AllManagedShellsAreMinimizedPredicate implements Predicable
{
    public boolean evaluate(Object obj)
    {
        Iterator iter = ((ShellManager)obj).getWindows();
        while (iter.hasNext())
        {
            Shell shell = (Shell) iter.next();
            if(!shell.getMinimized())
                return false;
        }

        return true;
    }
}
