package pl.edu.pg.bsk.encryption;

import org.junit.Test;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.EncryptionInstanceCreationException;

import java.nio.charset.StandardCharsets;

public class AsymmetricEncryptionTest {
	private final static String text = "Hello world!";

	@Test
	public void textEncryptedWithPublicKeyIsDifferentThanOriginal() throws EncryptionFailedException, EncryptionInstanceCreationException {
		AsymmetricEncryption encryption = new AsymmetricEncryption();
		String encrypted = new String(encryption.encryptWithPublic(text.getBytes(StandardCharsets.UTF_8), encryption.getPublicKey()));

		assert(!encrypted.equals(text));
	}

	@Test
	public void textEncryptedWithPrivateKeyIsDifferentThanOriginal() throws EncryptionFailedException, EncryptionInstanceCreationException {
		AsymmetricEncryption encryption = new AsymmetricEncryption();
		String encrypted = new String(encryption.encryptWithPrivate(text.getBytes(StandardCharsets.UTF_8)));

		assert(!encrypted.equals(text));
	}

	@Test
	public void encryptWithPublicDecryptWithPrivateReturnsOriginalValue() throws EncryptionFailedException, EncryptionInstanceCreationException {
		AsymmetricEncryption encryption = new AsymmetricEncryption();
		byte[] encrypted = encryption.encryptWithPublic(text.getBytes(StandardCharsets.UTF_8), encryption.getPublicKey());
		String decrypted = new String(encryption.decryptWithPrivate(encrypted));

		assert(decrypted.equals(text));
	}

	@Test
	public void encryptWithPrivateDecryptWithPublicReturnsOriginalValue() throws EncryptionFailedException, EncryptionInstanceCreationException {
		AsymmetricEncryption encryption = new AsymmetricEncryption();
		byte[] encrypted = encryption.encryptWithPrivate(text.getBytes(StandardCharsets.UTF_8));
		String decrypted = new String(encryption.decryptWithPublic(encrypted, encryption.getPublicKey()));

		assert(decrypted.equals(text));
	}
}
