package org.gudy.bouncycastle.jce.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

import org.gudy.bouncycastle.jce.interfaces.ElGamalKey;

public interface ElGamalPublicKey
    extends ElGamalKey, PublicKey
{
    public BigInteger getY();
}
