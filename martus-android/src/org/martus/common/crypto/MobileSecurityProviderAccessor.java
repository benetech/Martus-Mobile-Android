package org.martus.common.crypto;

import java.security.Provider;

import org.spongycastle.jce.provider.BouncyCastleProvider;

/**
 * @author roms
 *         Date: 6/17/13
 */
public class MobileSecurityProviderAccessor implements SecurityProviderAccessor
{
	@Override
	public String getSecurityProviderName()
	{
		return MartusKeyPairDataConstants.SECURITY_PROVIDER_SPONGYCASTLE;
	}

	@Override
	public Provider createSecurityProvider()
	{
		return new BouncyCastleProvider();
	}
}
