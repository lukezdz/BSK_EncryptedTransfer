package pl.edu.pg.bsk.transfer;

import org.apache.commons.io.FileUtils;
import pl.edu.pg.bsk.encryption.AsymmetricEncryption;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.EncryptionUtils;
import pl.edu.pg.bsk.encryption.KeySize;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.EncryptionInstanceCreationException;
import pl.edu.pg.bsk.exceptions.TransferException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.simple.JSONObject;

public class TransferHandler {
	private static final int MAX_PART_SIZE = 128;
	private static final int SERVER_PORT = 8800;

	private final Map<InetAddress, SecretKey> sessionKeys = new HashMap<>();

	private final SymmetricEncryption symmetricEncryption =
			new SymmetricEncryption(EncryptionUtils.getRandomSecureKey(KeySize.K_128));
	private final AsymmetricEncryption asymmetricEncryption;
	private final Map<InetAddress, SessionInfo> sessionMap = new HashMap<>();

	private final ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

	TransferHandler() throws EncryptionInstanceCreationException, IOException {
		asymmetricEncryption = new AsymmetricEncryption();
	}

	TransferHandler(KeyPair keyPair) throws IOException {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
	}

	/**
	 * Encrypts and sends files to provided address
	 * @param files List containing files to be encrypted and sent
	 * @param encryptionMode Mode of AES encryption to use for encrypting files
	 * @param address Address of destination
	 * @throws EncryptionFailedException Thrown when encryption fails
	 * @throws TransferException Thrown when transfer fails
	 */
	public void sendEncryptedFiles(List<File> files, EncryptionMode encryptionMode, InetAddress address)
			throws EncryptionFailedException, TransferException, IOException {
		for (File file: files) {
			// TODO: change empty optional to actual data
			sendEncryptedData(FileUtils.readFileToByteArray(file), encryptionMode, address, Optional.empty());
		}
	}

	/**
	 * Encrypts and sends message to provided address
	 * @param message Message to be encrypted and sent
	 * @param encryptionMode Mode of AES encryption to use for encrypting message
	 * @param address Address of destination
	 * @throws EncryptionFailedException Thrown when encryption fails
	 * @throws TransferException Thrown when transfer fails
	 */
	public void sendEncryptedMessage(String message, EncryptionMode encryptionMode, InetAddress address)
			throws EncryptionFailedException, TransferException {
		sendEncryptedData(message.getBytes(StandardCharsets.UTF_8), encryptionMode, address, Optional.empty());
	}

	private PublicKey getDestinationPublicKey(InetAddress address) throws TransferException{
		// Send request to provided address with question about public key
		// Get response with public key
		// return response
		try {
			Socket socket = new Socket(address, SERVER_PORT);
			OutputStream stream = socket.getOutputStream();
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("Type", "Handshake");
			jsonRequest.put("Body", "RSAPublicKey");
			stream.write(jsonRequest.toJSONString().getBytes());

			return EncryptionUtils.generateKeyPair().getPublic();
		} catch (IOException e) {
			throw new TransferException("Could not obtain public key from provided address");
		} catch (NoSuchAlgorithmException e) {
			System.out.println(e);
		}

		return null;
	}

	private void sendEncryptedData(byte[] data, EncryptionMode encryptionMode,
								   InetAddress address, Optional<TransferData.FileMetadata> metadata) throws EncryptionFailedException, TransferException
	{
		SessionInfo info = sessionMap.get(address);
		if (info == null || !info.getEncryptionMode().equals(encryptionMode)) {
			SecretKey key = EncryptionUtils.getRandomSecureKey(KeySize.K_128);
			PublicKey publicKey = getDestinationPublicKey(address);
			info = new SessionInfo(publicKey, key, encryptionMode);
			sessionMap.put(address, info);
		}
		symmetricEncryption.setKey(info.getSessionKey());
		Optional<IvParameterSpec> iv = encryptionMode.needsInitializationVector() ?
				Optional.of(EncryptionUtils.generateInitializationVector()) : Optional.empty();
		byte[] encrypted = symmetricEncryption.encrypt(data, encryptionMode, iv);

		try {
			Socket socket = sendTransferHeader(address);
			OutputStream stream = socket.getOutputStream();
			stream.write(TransferData.getTransferData(encryptionMode, encrypted, metadata));
		} catch (IOException e) {
			throw new TransferException("Could not transfer part of data to destination");
		}
	}

	// Splits data into parts, but as we are using TCP sockets to send data, splitting is pointless
	// TCP expects a lot of data and works better with loads of data being sent at once https://gamedev.stackexchange.com/a/83550
	private List<TransferData> splitTransferData(byte[] data) {
		List<TransferData> parts = new ArrayList<TransferData>();

		int start = 0;

		for(int i = 0; i*MAX_PART_SIZE < data.length; i++) {
			byte[] nextPart = new byte[MAX_PART_SIZE];
			if(start + MAX_PART_SIZE > data.length) {
				System.arraycopy(data, start, nextPart, 0, data.length - start);
			} else {
				System.arraycopy(data, start, nextPart, 0, MAX_PART_SIZE);
			}
			start += MAX_PART_SIZE;
			//parts.add(new TransferData(data, i));
		}

		return parts;
	}

	private Socket sendTransferHeader(InetAddress address) throws IOException {
		Socket socket = new Socket(address, SERVER_PORT);
		// TODO: Send transfer header (filename, file extension, file size, number of parts etc.)
		return socket;
	}
}
