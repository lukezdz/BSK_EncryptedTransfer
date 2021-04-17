package pl.edu.pg.bsk.transfer;

import pl.edu.pg.bsk.encryption.AsymmetricEncryption;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.KeySize;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.EncryptionInstanceCreationException;

import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.Optional;

public class TransferHandler {
	private static final int MAX_PART_SIZE = 512;

	private final SymmetricEncryption symmetricEncryption;
	private final AsymmetricEncryption asymmetricEncryption;

	TransferHandler() throws EncryptionInstanceCreationException {
		asymmetricEncryption = new AsymmetricEncryption();
		symmetricEncryption = new SymmetricEncryption(SymmetricEncryption.getRandomSecureKey(KeySize.K_256));
	}

	TransferHandler(KeyPair keyPair) {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
		symmetricEncryption = new SymmetricEncryption(SymmetricEncryption.getRandomSecureKey(KeySize.K_256));
	}

	public void performHandshake() {

	}

	public void sendEncryptedData(InputStream dataStream, EncryptionMode encryptionMode) {

	}

	private void sendPartOfEncryptedData(byte[] data, EncryptionMode encryptionMode) {
		Optional<IvParameterSpec> iv = encryptionMode.needsInitializationVector() ?
				Optional.of(SymmetricEncryption.generateInitializationVector()) : Optional.empty();
		try {
			byte[] encrypted = symmetricEncryption.encrypt(data, encryptionMode, iv);
		} catch (EncryptionFailedException e) {
			System.out.println(e.getMessage());
		}
	}
}
