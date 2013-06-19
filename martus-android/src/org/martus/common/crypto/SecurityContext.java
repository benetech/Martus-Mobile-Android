package org.martus.common.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author roms
 *         Date: 6/17/13
 */
public interface SecurityContext
{
	public String getSecurityProviderName();
	public Provider  createSecurityProvider();
	public X509Certificate createCertificate(RSAPublicKey publicKey, RSAPrivateCrtKey privateKey, SecureRandom secureRandom)
				throws SecurityException, SignatureException, InvalidKeyException,
				CertificateEncodingException, IllegalStateException, NoSuchAlgorithmException;
}
