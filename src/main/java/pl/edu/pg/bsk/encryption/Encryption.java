package pl.edu.pg.bsk.encryption;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

public class Encryption {
	@Getter
	@Setter
	private SecretKey key;

	public Encryption(SecretKey key) {
		this.key = key;
	}

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

	public byte[] encrypt(byte[] data, EncryptionMode mode, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(mode, Cipher.ENCRYPT_MODE, "Encryption", iv);
		return performOperation(cipher, data, "Encryption");
	}

	public byte[] decrypt(byte[] data, EncryptionMode mode, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(mode, Cipher.DECRYPT_MODE, "Decryption", iv);
		return performOperation(cipher, data, "Decryption");
	}

	private Cipher getCipherInstance(EncryptionMode algorithm, int cipherMode,
									 String operation, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		Cipher cipher;

		try {
			cipher = Cipher.getInstance(algorithm.getMode());
			if (iv.isPresent()) {
				cipher.init(cipherMode, key, iv.get());
			} else {
				cipher.init(cipherMode, key);
			}
		} catch (NoSuchPaddingException e) {
			throw new EncryptionFailedException(operation + " failed. No such padding available");
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionFailedException(operation + " failed. No such algorithm available");
		} catch (InvalidKeyException e) {
			throw new EncryptionFailedException(operation + " failed. Provided key is invalid");
		} catch (InvalidAlgorithmParameterException e) {
			throw new EncryptionFailedException(operation + " failed. Invalid initialization vector was provided");
		}

		return cipher;
	}

	private byte[] performOperation(Cipher cipher, byte[] data, String operation) throws EncryptionFailedException {
		try {
			return cipher.doFinal(data);
		} catch (BadPaddingException e) {
			throw new EncryptionFailedException(operation + " failed. Bad padding");
		} catch (IllegalBlockSizeException e) {
			throw new EncryptionFailedException(operation + " failed. Illegal block size");
		}
	}
}
