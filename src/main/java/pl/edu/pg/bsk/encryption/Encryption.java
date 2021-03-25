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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Encryption {
	@Getter
	@Setter
	private SecretKey key;

	public Encryption(SecretKey key) {
		this.key = key;
	}

	public static SecretKey getRandomSecureKey() {
		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		generator.init(128);
		return generator.generateKey();
	}

	public byte[] encrypt(byte[] data, EncryptionMode mode) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(mode, Cipher.ENCRYPT_MODE, "Encryption");
		return performOperation(cipher, data, "Encryption");
	}

	public byte[] decrypt(byte[] data, EncryptionMode mode) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(mode, Cipher.DECRYPT_MODE, "Decryption");
		return performOperation(cipher, data, "Decryption");
	}

	private Cipher getCipherInstance(EncryptionMode algorithm, int cipherMode, String operation) throws EncryptionFailedException {
		Cipher cipher;

		try {
			cipher = Cipher.getInstance(algorithm.getMode());
			cipher.init(cipherMode, key);
		} catch (NoSuchPaddingException e) {
			throw new EncryptionFailedException(operation + " failed. No such padding available");
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionFailedException(operation + " failed. No such algorithm available");
		} catch (InvalidKeyException e) {
			throw new EncryptionFailedException(operation + " failed. Provided key is invalid");
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
