package org.gudy.bouncycastle.crypto.params;

import java.math.BigInteger;

import org.gudy.bouncycastle.crypto.params.DSAKeyParameters;
import org.gudy.bouncycastle.crypto.params.DSAParameters;

public class DSAPrivateKeyParameters
    extends DSAKeyParameters
{
    private BigInteger      x;

    public DSAPrivateKeyParameters(
        BigInteger      x,
        DSAParameters   params)
    {
        super(true, params);

        this.x = x;
    }   

    public BigInteger getX()
    {
        return x;
    }
}
