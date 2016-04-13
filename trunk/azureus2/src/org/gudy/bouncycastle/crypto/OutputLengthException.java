package org.gudy.bouncycastle.crypto;

import org.gudy.bouncycastle.crypto.DataLengthException;

public class OutputLengthException
    extends DataLengthException
{
    public OutputLengthException(String msg)
    {
        super(msg);
    }
}
