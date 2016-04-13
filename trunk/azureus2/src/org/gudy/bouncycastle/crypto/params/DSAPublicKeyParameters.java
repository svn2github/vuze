package org.gudy.bouncycastle.crypto.params;

import java.math.BigInteger;

import org.gudy.bouncycastle.crypto.params.DSAKeyParameters;
import org.gudy.bouncycastle.crypto.params.DSAParameters;

public class DSAPublicKeyParameters
    extends DSAKeyParameters
{
    private BigInteger      y;

    public DSAPublicKeyParameters(
        BigInteger      y,
        DSAParameters   params)
    {
        super(false, params);

        this.y = y;
    }   

    public BigInteger getY()
    {
        return y;
    }
}
