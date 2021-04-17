package pl.edu.pg.bsk.transfer;

import pl.edu.pg.bsk.encryption.AsymmetricEncryption;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.KeySize;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.EncryptionInstanceCreationException;
import pl.edu.pg.bsk.exceptions.TransferException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

public class TransferHandler {
	private static final int MAX_PART_SIZE = 128;
	private static final int SERVER_PORT = 8800;
	private static final int CLIENT_PORt = 8801;

	private final SymmetricEncryption symmetricEncryption;
	private final AsymmetricEncryption asymmetricEncryption;
	private final Map<InetAddress, PublicKey> hostsPublicKeys;

	private EncryptionMode prevEncryptionMode = null;

	TransferHandler() throws EncryptionInstanceCreationException {
		asymmetricEncryption = new AsymmetricEncryption();
		symmetricEncryption = new SymmetricEncryption(SymmetricEncryption.getRandomSecureKey(KeySize.K_128));
		hostsPublicKeys = new HashMap<>();
	}

	TransferHandler(KeyPair keyPair) {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
		symmetricEncryption = new SymmetricEncryption(SymmetricEncryption.getRandomSecureKey(KeySize.K_128));
		hostsPublicKeys = new HashMap<>();
	}

	public void performHandshake() {

	}

	/**
	 * Encrypts and sends data to provided IPv4 address. Takes care of session and key exchange.
	 * @param data Data to be encrypted and sent
	 * @param encryptionMode User selected encryption mode
	 * @param address IPv4 address to which data should be sent
	 */
	public void sendEncryptedData(byte[] data, EncryptionMode encryptionMode,
								  InetAddress address) throws EncryptionFailedException, TransferException
	{
		if (encryptionMode != prevEncryptionMode) {
			prevEncryptionMode = encryptionMode;
			SecretKey newKey = SymmetricEncryption.getRandomSecureKey(KeySize.K_128);
			symmetricEncryption.setKey(newKey);
			performHandshake();
		}
		Optional<IvParameterSpec> iv = encryptionMode.needsInitializationVector() ?
				Optional.of(SymmetricEncryption.generateInitializationVector()) : Optional.empty();
		byte[] encrypted = symmetricEncryption.encrypt(data, encryptionMode, iv);
		List<TransferPart> parts = new ArrayList<TransferPart>();

		int start = 0;

		for(int i = 0; i*MAX_PART_SIZE < data.length; i++) {
			byte[] nextPart = new byte[MAX_PART_SIZE];
			if(start + MAX_PART_SIZE > data.length) {
				System.arraycopy(data, start, nextPart, 0, data.length - start);
			} else {
				System.arraycopy(data, start, nextPart, 0, MAX_PART_SIZE);
			}
			start += MAX_PART_SIZE;
			parts.add(new TransferPart(data, i));
		}

		for (TransferPart part: parts) {
			try {
				sendPartOfEncryptedData(part, address);
			} catch (IOException e) {
				throw new TransferException();
			}
		}
	}

	private void sendPartOfEncryptedData(TransferPart part, InetAddress address) throws IOException {
		Socket socket = new Socket(address, SERVER_PORT);
		OutputStream output = socket.getOutputStream();
		output.write(part.getData());
	}
}
