package org.gudy.bouncycastle.jce.spec;

import java.math.BigInteger;

import org.gudy.bouncycastle.jce.spec.ElGamalKeySpec;
import org.gudy.bouncycastle.jce.spec.ElGamalParameterSpec;
import org.gudy.bouncycastle.jce.spec.ElGamalPublicKeySpec;

/**
 * This class specifies an ElGamal private key with its associated parameters.
 *
 * @see ElGamalPublicKeySpec
 */
public class ElGamalPrivateKeySpec
    extends ElGamalKeySpec
{
    private BigInteger  x;

    public ElGamalPrivateKeySpec(
        BigInteger              x,
        ElGamalParameterSpec    spec)
    {
        super(spec);

        this.x = x;
    }

    /**
     * Returns the private value <code>x</code>.
     *
     * @return the private value <code>x</code>
     */
    public BigInteger getX()
    {
        return x;
    }
}
