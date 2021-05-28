package pl.edu.pg.bsk.encryption;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class EncryptionUtils {
	public static SecretKey getRandomSecureKey(KeySize keySize) {
		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		generator.init(keySize.getSize());
		return generator.generateKey();
	}

	public static IvParameterSpec generateInitializationVector() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}

	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator factory;
		factory = KeyPairGenerator.getInstance("RSA");

		return factory.generateKeyPair();
	}
}
