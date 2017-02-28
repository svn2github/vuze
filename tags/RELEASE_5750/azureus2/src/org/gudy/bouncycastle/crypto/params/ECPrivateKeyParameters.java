package org.gudy.bouncycastle.crypto.params;

import java.math.BigInteger;

import org.gudy.bouncycastle.crypto.params.ECDomainParameters;
import org.gudy.bouncycastle.crypto.params.ECKeyParameters;

public class ECPrivateKeyParameters
    extends ECKeyParameters
{
    BigInteger d;

    public ECPrivateKeyParameters(
        BigInteger          d,
        ECDomainParameters  params)
    {
        super(true, params);
        this.d = d;
    }

    public BigInteger getD()
    {
        return d;
    }
}
