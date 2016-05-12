package org.gudy.bouncycastle.crypto.params;

import org.gudy.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.gudy.bouncycastle.crypto.params.ECDomainParameters;

public class ECKeyParameters
    extends AsymmetricKeyParameter
{
	ECDomainParameters params;

	protected ECKeyParameters(
        boolean             isPrivate,
        ECDomainParameters  params)
	{
        super(isPrivate);

		this.params = params;
	}

	public ECDomainParameters getParameters()
	{
		return params;
	}
}
