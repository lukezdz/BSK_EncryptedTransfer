package encryption;

import lombok.Getter;
import lombok.Setter;
import exceptions.EncryptionFailedException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Optional;

public class SymmetricEncryption {
	private static final String ENCRYPTION = "Encryption";
	private static final String DECRYPTION = "Decryption";

	@Getter
	@Setter
	private SecretKey key;

	public SymmetricEncryption(SecretKey key) {
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
		Cipher cipher = getCipherInstance(mode, Cipher.ENCRYPT_MODE, iv);
		return performOperation(cipher, data, ENCRYPTION);
	}

	public byte[] decrypt(byte[] data, EncryptionMode mode, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(mode, Cipher.DECRYPT_MODE, iv);
		return performOperation(cipher, data, DECRYPTION);
	}

	private Cipher getCipherInstance(EncryptionMode algorithm, int cipherMode, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		Cipher cipher;
		String operation = cipherMode == Cipher.ENCRYPT_MODE ? ENCRYPTION : DECRYPTION;

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
