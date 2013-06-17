package org.martus.common.crypto;

import java.security.Provider;

/**
 * @author roms
 *         Date: 6/17/13
 */
public interface SecurityProviderAccessor
{
	public String getSecurityProviderName();
	public Provider  createSecurityProvider();

}
