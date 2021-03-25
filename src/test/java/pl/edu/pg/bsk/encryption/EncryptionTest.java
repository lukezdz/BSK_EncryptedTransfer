package pl.edu.pg.bsk.encryption;


import org.junit.Test;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import java.nio.charset.StandardCharsets;

public class EncryptionTest {
	@Test
	public void decryptingEncryptedDataReturnsOriginal() throws EncryptionFailedException {
		String text = "Hello world!";
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

		Encryption encryption = new Encryption(Encryption.getRandomSecureKey());
		byte[] encrypted = encryption.encrypt(bytes, EncryptionMode.AES_ECB);
		byte[] decrypted = encryption.decrypt(encrypted, EncryptionMode.AES_ECB);
		String decryptedString = new String(decrypted);

		assert decryptedString.equals(text);
	}
}
