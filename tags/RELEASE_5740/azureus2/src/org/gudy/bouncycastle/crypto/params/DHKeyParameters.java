package org.gudy.bouncycastle.crypto.params;

import org.gudy.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.gudy.bouncycastle.crypto.params.DHKeyParameters;
import org.gudy.bouncycastle.crypto.params.DHParameters;


public class DHKeyParameters
    extends AsymmetricKeyParameter
{
    private DHParameters    params;

    protected DHKeyParameters(
        boolean         isPrivate,
        DHParameters    params)
    {
        super(isPrivate);

        this.params = params;
    }   

    public DHParameters getParameters()
    {
        return params;
    }

    public boolean equals(
        Object  obj)
    {
        if (!(obj instanceof DHKeyParameters))
        {
            return false;
        }

        DHKeyParameters    dhKey = (DHKeyParameters)obj;

        return (params != null && !params.equals(dhKey.getParameters()));
    }
}
