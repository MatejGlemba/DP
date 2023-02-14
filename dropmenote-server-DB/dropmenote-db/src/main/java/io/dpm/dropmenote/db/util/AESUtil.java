package io.dpm.dropmenote.db.util;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author martinjurek
 *
 */
public class AESUtil {

	// Devel
	private static final String AES_PASSWORD = "blabla";
	// Prod nieje sucastou GITU !!!!
	
	private static SecretKeySpec secretKey;
	private static Cipher cipher;

	private static Logger LOG = LoggerFactory.getLogger(AESUtil.class);

	{
		LOG.debug("{} initialisation.", AESUtil.class.getName());
	}
	
	static {
		try {
			byte[] secretKeyBytes = Arrays.copyOf(MessageDigest.getInstance("SHA-1").digest(AES_PASSWORD.getBytes("UTF-8")), 16);
			secretKey = new SecretKeySpec(secretKeyBytes, "AES");
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * 
	 * @param strToEncrypt
	 * @param secret
	 * @return
	 */
	public static synchronized String encrypt(String strToEncrypt) {
		if (strToEncrypt == null) {
			return null;
		}
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			LOG.error("Error while encrypting: " + e.toString());
			return null;
		}
	}

	/**
	 * 
	 * @param strToDecrypt
	 * @param secret
	 * @return
	 */
	public static synchronized String decrypt(String strToDecrypt) {
		if (strToDecrypt == null) {
			return null;
		}
		try {
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			LOG.error("Error while decrypting: " + e.toString());
			return null;
		}
	}
}