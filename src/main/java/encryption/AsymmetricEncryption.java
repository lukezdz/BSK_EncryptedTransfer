package encryption;

import lombok.Getter;
import exceptions.EncryptionFailedException;
import exceptions.EncryptionInstanceCreationException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

public class AsymmetricEncryption {
	private static final String RSA = "RSA";
	private static final String ENCRYPTION = "Encryption";
	private static final String DECRYPTION = "Decryption";

	@Getter
	private final PublicKey publicKey;
	private final PrivateKey privateKey;

	AsymmetricEncryption() throws EncryptionInstanceCreationException {
		KeyPairGenerator factory;
		try {
			factory = KeyPairGenerator.getInstance(RSA);
		}
		catch (NoSuchAlgorithmException e) {
			throw new EncryptionInstanceCreationException("Cannot create KeyPairGenerator for provided algorithm");
		}

		KeyPair pair = factory.generateKeyPair();
		publicKey = pair.getPublic();
		privateKey = pair.getPrivate();
	}

	public byte[] encryptWithPublic(byte[] data, PublicKey key) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(Cipher.ENCRYPT_MODE, publicKey);
		return performOperation(cipher, data, ENCRYPTION);
	}

	public byte[] decryptWithPublic(byte[] data, PublicKey key) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(Cipher.DECRYPT_MODE, publicKey);
		return performOperation(cipher, data, DECRYPTION);
	}

	public byte[] encryptWithPrivate(byte[] data) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(Cipher.ENCRYPT_MODE, privateKey);
		return performOperation(cipher, data, ENCRYPTION);
	}

	public byte[] decryptWithPrivate(byte[] data) throws EncryptionFailedException {
		Cipher cipher = getCipherInstance(Cipher.DECRYPT_MODE, privateKey);
		return performOperation(cipher, data, DECRYPTION);
	}

	private Cipher getCipherInstance(int cipherMode, Key key) throws EncryptionFailedException {
		Cipher cipher;
		String operation = cipherMode == Cipher.ENCRYPT_MODE ? ENCRYPTION : DECRYPTION;

		try {
			cipher = Cipher.getInstance(RSA);
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
