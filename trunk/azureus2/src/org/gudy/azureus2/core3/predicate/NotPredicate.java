package org.gudy.azureus2.core3.predicate;

/**
 * A NotPredicate decorates an existing Predicable to return the negative result of the evaluation
 * @version 1.0
 */
public final class NotPredicate implements Predicable
{
    private Predicable ref;

    /**
     * Creates a NotPredicate
     * @param toEvaluate A predicable to evaluate the "Not" from
     */
    public NotPredicate(Predicable toEvaluate)
    {
        this.ref = toEvaluate;
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(Object obj)
    {
        return !ref.evaluate(obj);
    }
}
