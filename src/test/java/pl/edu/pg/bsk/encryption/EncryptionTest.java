package pl.edu.pg.bsk.encryption;


import org.junit.Test;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class EncryptionTest {
	static final String text = "Hello world!";

	public String encryptAndDecrypt(String text, EncryptionMode mode, Optional<IvParameterSpec> iv) throws EncryptionFailedException {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

		Encryption encryption = new Encryption(Encryption.getRandomSecureKey(KeySize.K_128));
		byte[] encrypted = encryption.encrypt(bytes, mode, iv);
		byte[] decrypted = encryption.decrypt(encrypted, mode, iv);
		return new String(decrypted);
	}

	@Test
	public void decryptingEncryptedDataWithECBReturnsOriginal() throws EncryptionFailedException {
		String decryptedString = encryptAndDecrypt(text, EncryptionMode.AES_ECB, Optional.empty());

		assert decryptedString.equals(text);
	}

	@Test
	public void decryptingEncryptedDataWithCBCReturnsOriginal() throws EncryptionFailedException {
		String decryptedString = encryptAndDecrypt(text, EncryptionMode.AES_CBC, Optional.of(Encryption.generateInitializationVector()));

		assert decryptedString.equals(text);
	}

	@Test
	public void decryptingEncryptedDataWithCFBReturnsOriginal() throws EncryptionFailedException {
		String decryptedString = encryptAndDecrypt(text, EncryptionMode.AES_CFB, Optional.of(Encryption.generateInitializationVector()));

		assert decryptedString.equals(text);
	}

	@Test
	public void decryptingEncryptedDataWithOFBReturnsOriginal() throws EncryptionFailedException {
		String decryptedString = encryptAndDecrypt(text, EncryptionMode.AES_OFB, Optional.of(Encryption.generateInitializationVector()));

		assert decryptedString.equals(text);
	}

	@Test
	public void decryptingEncryptedDataWithCTRReturnsOriginal() throws EncryptionFailedException {
		String decryptedString = encryptAndDecrypt(text, EncryptionMode.AES_CTR, Optional.of(Encryption.generateInitializationVector()));

		assert decryptedString.equals(text);
	}
}
