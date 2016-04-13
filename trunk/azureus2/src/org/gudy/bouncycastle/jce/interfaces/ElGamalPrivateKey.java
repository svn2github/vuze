package org.gudy.bouncycastle.jce.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

import org.gudy.bouncycastle.jce.interfaces.ElGamalKey;

public interface ElGamalPrivateKey
    extends ElGamalKey, PrivateKey
{
    public BigInteger getX();
}
