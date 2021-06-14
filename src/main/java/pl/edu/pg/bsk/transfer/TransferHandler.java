package pl.edu.pg.bsk.transfer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.util.Pair;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransferHandler extends Thread {
	public static final int SERVER_PORT = 8800;

	private static final int MAX_PART_SIZE = 128;
	private static final long TIMEOUT = 5;

	@Getter
	private final ObjectProperty<Task<Pair<Metadata, byte[]>>> receiveTaskProperty = new SimpleObjectProperty<>();
	@Getter
	private final ObjectProperty<Task<Metadata>> receiveRequestTaskProperty = new SimpleObjectProperty<>();

	private final Map<InetAddress, SessionInfo> sessionInfos = new HashMap<>();
	private final Map<InetAddress, PublicKey> publicKeysMap = new HashMap<>();
	private final SymmetricEncryption symmetricEncryption =
			new SymmetricEncryption(EncryptionUtils.getRandomSecureKey(KeySize.K_128));
	private final AsymmetricEncryption asymmetricEncryption;
	private final ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

	private boolean quit = false;

	public TransferHandler() throws EncryptionInstanceCreationException, IOException {
		asymmetricEncryption = new AsymmetricEncryption();
	}

	public TransferHandler(KeyPair keyPair) throws IOException {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
	}

	@SneakyThrows
	@Override
	public void run() {
		while (!quit) {
			Socket clientSocket = serverSocket.accept();
			InputStream inputStream = clientSocket.getInputStream();

			TransferData.ReadTransferData readTransferData = TransferData.readTransferData(inputStream.readAllBytes());
			Metadata metadata = readTransferData.getMetadata();
			InetAddress address = clientSocket.getInetAddress();
			switch (metadata.getType()) {
				case MESSAGE: {
					Metadata.TransferType transferType = readTransferData.getMetadata().getTransferType();
					SessionInfo info = sessionInfos.get(address);
					if (transferType == Metadata.TransferType.REQUEST) {

					}
					else if (transferType == Metadata.TransferType.ANSWER) {
						Optional<IvParameterSpec> iv = info.getInitializationVector() == null ? Optional.empty() : Optional.of(info.getInitializationVector());
						this.receiveTaskProperty.set(getReceiveAndDecryptTask(clientSocket, info.getEncryptionMode(), info.getSessionKey(), iv, metadata));
					}
					break;
				}
				case FILE: {
					break;
				}
				case HANDSHAKE: {
					if (readTransferData.getMetadata().getHandshakePart() == 1) {
						byte[] data = readTransferData.getData();
						PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
								new X509EncodedKeySpec(data));
						publicKeysMap.put(address, publicKey);
						if (metadata.getTransferType() == Metadata.TransferType.REQUEST) {
							byte[] response = TransferData.getPartOneHandshakeData(
									asymmetricEncryption.getPublicKey(), Metadata.TransferType.ANSWER);
							clientSocket.getOutputStream().write(response);
						}
					}
					else if (readTransferData.getMetadata().getHandshakePart() == 2) {
						byte[] data = readTransferData.getData();
						byte[] decrypted = asymmetricEncryption.decryptWithPublic(data, publicKeysMap.get(address));
						SessionInfo sessionInfo = TransferData.parsePartTwoHandshakeData(decrypted);
						sessionInfos.put(address, sessionInfo);
						if (metadata.getTransferType() == Metadata.TransferType.REQUEST) {
							byte[] responseBody = TransferData.getPartTwoHandshakeBody(sessionInfo.getSessionKey(),
									sessionInfo.getInitializationVector(), sessionInfo.getEncryptionMode());
							byte[] encrypted = asymmetricEncryption.encryptWithPublic(
									responseBody, publicKeysMap.get(address));
							byte[] response = TransferData.getPartTwoHandshakeData(
									encrypted, Metadata.TransferType.ANSWER);
							clientSocket.getOutputStream().write(response);
						}
					}
					break;
				}
			}
		}
	}

	public void quitServer() {
		this.quit = true;
	}

	/**
	 * Encrypts and sends files to provided address
	 * @param file File to be encrypted and sent
	 * @param encryptionMode Mode of AES encryption to use for encrypting files
	 * @param address Address of destination
	 * @throws EncryptionFailedException Thrown when encryption fails
	 * @throws TransferException Thrown when transfer fails
	 */
	public Task<Void> sendEncryptedFile(File file, EncryptionMode encryptionMode, InetAddress address)
			throws EncryptionFailedException, TransferException, IOException {
		Metadata metadata = new Metadata(Metadata.MetadataType.FILE);
		metadata.setFilename(FilenameUtils.getName(file.getName()));
		metadata.setFileExt(FilenameUtils.getExtension(file.getName()));
		metadata.setFileSize(file.getTotalSpace());

		return sendEncryptedData(FileUtils.readFileToByteArray(file), encryptionMode, address, metadata);
	}

	/**
	 * Encrypts and sends message to provided address
	 * @param message Message to be encrypted and sent
	 * @param encryptionMode Mode of AES encryption to use for encrypting message
	 * @param address Address of destination
	 * @throws EncryptionFailedException Thrown when encryption fails
	 * @throws TransferException Thrown when transfer fails
	 */
	public Task<Void> sendEncryptedMessage(String message, EncryptionMode encryptionMode, InetAddress address)
			throws EncryptionFailedException, TransferException {
		Metadata metadata = new Metadata(Metadata.MetadataType.MESSAGE);
		metadata.setTransferType(Metadata.TransferType.REQUEST);
		return sendEncryptedData(message.getBytes(StandardCharsets.UTF_8), encryptionMode, address, metadata);
	}

	private Task<Void> sendEncryptedData(byte[] data, EncryptionMode encryptionMode, InetAddress address,
										 Metadata metadata) throws EncryptionFailedException, TransferException
	{
		SessionInfo info = sessionInfos.get(address);
		Task<Void> handshakeTask = null;
		if (info == null || !info.getEncryptionMode().equals(encryptionMode)) {
			handshakeTask = performHandshake(address, encryptionMode);
		}

		Task<Void> finalHandshakeTask = handshakeTask;
		Task<Void> sendingTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				SessionInfo info = sessionInfos.get(address);
				symmetricEncryption.setKey(info.getSessionKey());
				while (finalHandshakeTask == null || (info == null && finalHandshakeTask.isRunning())) {
					info = sessionInfos.get(address);
				}

				if (finalHandshakeTask.isCancelled()) {
					cancel();
					throw new TransferException("Transfer failed!");
				}

				symmetricEncryption.setKey(info.getSessionKey());
				Optional<IvParameterSpec> iv = encryptionMode.needsInitializationVector() ?
						Optional.of(info.getInitializationVector()) : Optional.empty();
				byte[] encrypted = symmetricEncryption.encrypt(data, encryptionMode, iv);

				try {
					Socket socket = getSocket(address);
					OutputStream stream = socket.getOutputStream();
					stream.write(TransferData.getTransferData(encrypted, metadata));
				} catch (IOException e) {
					throw new TransferException("Could not transfer data to destination");
				}

				succeeded();
				return null;
			}
		};

		sendingTask.run();
		return sendingTask;
	}

	private Task<Void> performHandshake(InetAddress address, EncryptionMode encryptionMode) throws TransferException {
		try {
			sessionInfos.remove(address);
			Socket socket = new Socket(address, SERVER_PORT);
			OutputStream stream = socket.getOutputStream();

			byte[] handshakeData = TransferData.getPartOneHandshakeData(asymmetricEncryption.getPublicKey(), Metadata.TransferType.REQUEST);
			stream.write(handshakeData);

			Task<Void> respondingTask = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					LocalTime start = LocalTime.now();

					while (Duration.between(start, LocalTime.now()).toSeconds() <= TIMEOUT) {
						PublicKey key = publicKeysMap.get(address);
						if (key != null) {
							updateMessage("Received public key.");
							byte[] handshakeBody = TransferData.getPartTwoHandshakeBody(
									EncryptionUtils.getRandomSecureKey(KeySize.K_128),
									EncryptionUtils.generateInitializationVector(),
									encryptionMode
							);
							byte[] encrypted = asymmetricEncryption.encryptWithPublic(handshakeBody, key);
							byte[] data = TransferData.getPartTwoHandshakeData(encrypted, Metadata.TransferType.REQUEST);
							updateMessage("Responded with session info.");
							stream.write(data);

							LocalTime responseWaitStart = LocalTime.now();
							while (Duration.between(responseWaitStart, LocalTime.now()).toSeconds() <= TIMEOUT) {
								SessionInfo info = sessionInfos.get(address);
								if (info != null) {
									succeeded();
									return null;
								}
							}

							cancel();
							throw new TransferException("RSA Handshake timed out.");
						}
					}

					cancel();
					throw new TransferException("RSA Handshake timed out.");
				}
			};

			respondingTask.run();
			return respondingTask;
		} catch (IOException e) {
			throw new TransferException("Could not obtain public key from provided address");
		}
	}

	private Task<Pair<Metadata, byte[]>> getReceiveAndDecryptTask(Socket socket, EncryptionMode mode, SecretKey key,
											 Optional<IvParameterSpec> iv, Metadata metadata) {
		Task<Pair<Metadata, byte[]>> receiveAndDecryptTask = new Task<Pair<Metadata, byte[]>>() {
			@Override
			protected Pair<Metadata, byte[]> call() throws Exception
			{
				symmetricEncryption.setKey(key);
				byte[] received = socket.getInputStream().readAllBytes();
				byte[] decrypted = symmetricEncryption.decrypt(received, mode, iv);

				return new Pair<Metadata, byte[]>(metadata, decrypted);
			}
		};
		receiveAndDecryptTask.run();

		return receiveAndDecryptTask;
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

	private Socket getSocket(InetAddress address) throws IOException {
		return new Socket(address, SERVER_PORT);
	}
}
