package org.martus.common.crypto;

import java.security.Provider;

import org.spongycastle.jce.provider.BouncyCastleProvider;

/**
 * @author roms
 *         Date: 6/17/13
 */
public class DefaultSecurityProviderAccessor implements SecurityProviderAccessor
{
	@Override
	public String getSecurityProviderName()
	{
		return MartusCrypto.SECURITY_PROVIDER_BOUNCYCASTLE;
	}

	@Override
	public Provider createSecurityProvider()
	{
		return new BouncyCastleProvider();
	}
}
