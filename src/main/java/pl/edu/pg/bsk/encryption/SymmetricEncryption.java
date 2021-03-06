package pl.edu.pg.bsk.encryption;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
			if (algorithm.needsInitializationVector()) {
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
