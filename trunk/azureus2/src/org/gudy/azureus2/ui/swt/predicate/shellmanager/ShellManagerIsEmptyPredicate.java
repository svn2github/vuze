package org.gudy.azureus2.ui.swt.predicate.shellmanager;

import org.gudy.azureus2.core3.predicate.Predicable;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;

/**
 * @version 1.0
 */
public final class ShellManagerIsEmptyPredicate implements Predicable
{
    public boolean evaluate(Object obj)
    {
        return ((ShellManager)obj).isEmpty();
    }
}
