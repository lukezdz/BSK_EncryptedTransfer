package pl.edu.pg.bsk.transfer;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import pl.edu.pg.bsk.controllers.ControllerNotification;
import pl.edu.pg.bsk.controllers.NotifiableController;
import pl.edu.pg.bsk.encryption.AsymmetricEncryption;
import pl.edu.pg.bsk.encryption.EncryptionMode;
import pl.edu.pg.bsk.encryption.EncryptionUtils;
import pl.edu.pg.bsk.encryption.KeySize;
import pl.edu.pg.bsk.encryption.SymmetricEncryption;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;
import pl.edu.pg.bsk.exceptions.TransferException;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransferHandler extends Thread {
	public static final int SERVER_PORT = 8800;

	private static final int MAX_PART_SIZE = 128;
	private static final long TIMEOUT = 5;

	private final Map<InetAddress, SessionInfo> sessionInfos = new HashMap<>();
	private final Map<InetAddress, PublicKey> publicKeys = new HashMap<>();
	private final Map<InetAddress, ConnectionThread> connections = new HashMap<>();
	private final SymmetricEncryption symmetricEncryption =
			new SymmetricEncryption(EncryptionUtils.getRandomSecureKey(KeySize.K_128));
	private AsymmetricEncryption asymmetricEncryption;
	private final ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
	private final NotifiableController myController;

	@Setter
	private File downloadDir;

	public TransferHandler(NotifiableController controller, KeyPair keyPair) throws IOException {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
		myController = controller;
	}

	@SneakyThrows
	@Override
	public void run() {
		System.out.println("Transfer handler is started and listening on thread " + SERVER_PORT);

		while (!isInterrupted()) {
			Socket clientSocket = serverSocket.accept();
			InetAddress address = clientSocket.getInetAddress();
			System.out.println("Accepted client socket from " + address);
			ConnectionThread thread = new ConnectionThread(this, clientSocket);

			connections.put(address, thread);
			thread.start();
		}
	}

	public void setKeyPair(KeyPair keyPair) {
		asymmetricEncryption = new AsymmetricEncryption(keyPair);
	}

	/**
	 * Ends the server listening loop and closes all open connections.
	 */
	public void quitServer() throws IOException {
		interrupt();

		for (Map.Entry<InetAddress, ConnectionThread> entry : connections.entrySet()) {
			entry.getValue().close();
		}
	}

	/**
	 * Encrypts and sends files to provided address
	 * @param file File to be encrypted and sent
	 * @param encryptionMode Mode of AES encryption to use for encrypting files
	 * @param address Address of destination
	 * @throws TransferException Thrown when transfer fails
	 */
	public Task<Void> sendEncryptedFile(File file, EncryptionMode encryptionMode, InetAddress address)
			throws TransferException, IOException {
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
	 * @throws TransferException Thrown when transfer fails
	 */
	public Task<Void> sendEncryptedMessage(String message, EncryptionMode encryptionMode, InetAddress address)
			throws TransferException {
		Metadata metadata = new Metadata(Metadata.MetadataType.MESSAGE);
		return sendEncryptedData(message.getBytes(StandardCharsets.UTF_8), encryptionMode, address, metadata);
	}

	public void receiveData(TransferData data, InetAddress address)
			throws EncryptionFailedException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		Metadata metadata = data.getMetadata();
		System.out.println("Receive data - metadata: " + metadata.getType());

		switch (metadata.getType()) {
			case MESSAGE: {
				SessionInfo info = sessionInfos.get(address);
				Optional<IvParameterSpec> iv = info.getEncryptionMode().needsInitializationVector() ?
						Optional.of(info.getInitializationVector()) : Optional.empty();

				symmetricEncryption.setKey(info.getSessionKey());
				String message = new String(symmetricEncryption.decrypt(
						data.getPayload(), info.getEncryptionMode(), iv));

				ControllerNotification notification = new ControllerNotification(metadata, address, message);
				myController.notifyController(notification);

				break;
			}
			case FILE: {
				SessionInfo info = sessionInfos.get(address);
				Optional<IvParameterSpec> iv = info.getInitializationVector() == null ?
						Optional.empty() : Optional.of(info.getInitializationVector());

				File file = new File(downloadDir.getPath() + "/" + metadata.getFilename());
				symmetricEncryption.setKey(info.getSessionKey());
				byte[] decrypted = symmetricEncryption.decrypt(data.getPayload(), info.getEncryptionMode(), iv);
				FileUtils.writeByteArrayToFile(file, decrypted);

				ControllerNotification notification = new ControllerNotification(metadata, address, file);
				myController.notifyController(notification);

				break;
			}
			case HANDSHAKE: {
				if (data.getMetadata().getHandshakePart() == 1) {
					byte[] read = data.getPayload();
					PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
							new X509EncodedKeySpec(read));
					publicKeys.put(address, publicKey);
					System.out.println("Saved public key from " + address + ".\nPublic key: " + publicKey + "\nMy public key: " + asymmetricEncryption.getPublicKey());
					if (metadata.getTransferType() == Metadata.TransferType.REQUEST) {
						TransferData response = TransferData.getPartOneHandshakeData(
								asymmetricEncryption.getPublicKey(), Metadata.TransferType.ANSWER);
						connections.get(address).write(response);
					}
				}
				else if (metadata.getHandshakePart() == 2) {
					HandshakeComplexBody handshakeComplexBody = data.getHandshakeComplexBody();
					byte[] keyBytes = asymmetricEncryption.decryptWithPrivate(handshakeComplexBody.getEncodedKey());
					byte[] ivBytes = asymmetricEncryption.decryptWithPrivate(handshakeComplexBody.getEncodedIv());
					SecretKey key = HandshakeComplexBody.deserializeKey(keyBytes);
					IvParameterSpec iv = HandshakeComplexBody.deserializeIv(ivBytes);
					SessionInfo sessionInfo = new SessionInfo(key, handshakeComplexBody.getMode(), iv);
					sessionInfos.put(address, sessionInfo);
					if (metadata.getTransferType() == Metadata.TransferType.REQUEST) {
						byte[] keyBytesToEncode = HandshakeComplexBody.serializeKey(sessionInfo.getSessionKey());
						byte[] ivBytesToEncode = HandshakeComplexBody.serializeIv(sessionInfo.getInitializationVector());
						byte[] encodedKey = asymmetricEncryption.encryptWithPublic(keyBytesToEncode, publicKeys.get(address));
						byte[] encodedIv = asymmetricEncryption.encryptWithPublic(ivBytesToEncode, publicKeys.get(address));

						HandshakeComplexBody responseBody = new HandshakeComplexBody(encodedKey, encodedIv, sessionInfo.getEncryptionMode());
						TransferData response = TransferData.getPartTwoHandshakeData(
								null, Metadata.TransferType.ANSWER);
						response.setHandshakeComplexBody(responseBody);
						connections.get(address).write(response);
					}
				}
				break;
			}
		}
	}

	private Task<Void> sendEncryptedData(byte[] data, EncryptionMode encryptionMode, InetAddress address,
										 Metadata metadata) throws TransferException
	{
		SessionInfo info = sessionInfos.get(address);
		Task<Void> handshakeTask = null;
		if (info == null || !info.getEncryptionMode().equals(encryptionMode)) {
			handshakeTask = performHandshake(address, encryptionMode);
		}

		Task<Void> sendingTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				SessionInfo info = sessionInfos.get(address);

				symmetricEncryption.setKey(info.getSessionKey());
				Optional<IvParameterSpec> iv = encryptionMode.needsInitializationVector() ?
						Optional.of(info.getInitializationVector()) : Optional.empty();
				byte[] encrypted = symmetricEncryption.encrypt(data, encryptionMode, iv);

				try {
					ConnectionThread thread = connections.get(address);
					if (thread == null) {
						throw new TransferException("Socket is closed");
					}
					thread.write(new TransferData(metadata, encrypted));
				} catch (IOException e) {
					throw new TransferException("Could not transfer data to destination");
				}

				succeeded();
				return null;
			}
		};

		if (handshakeTask != null) {
			handshakeTask.setOnSucceeded(workerStateEvent -> {
				sendingTask.run();
			});
			handshakeTask.setOnFailed(workerStateEvent -> {
				Alert alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Handshake failed");
				alert.setContentText(workerStateEvent.getSource().getException().getMessage());
				alert.showAndWait();
			});
		}
		else {
			sendingTask.run();
		}

		return sendingTask;
	}

	private Task<Void> performHandshake(InetAddress address, EncryptionMode encryptionMode) throws TransferException {
		try {
			sessionInfos.remove(address);
			Socket socket = new Socket(address, SERVER_PORT);
			ConnectionThread connectionThread = new ConnectionThread(this, socket);
			connections.put(address, connectionThread);
			connectionThread.start();

			if (publicKeys.get(address) == null) {
				TransferData handshakeData = TransferData.getPartOneHandshakeData(asymmetricEncryption.getPublicKey(), Metadata.TransferType.REQUEST);
				connectionThread.write(handshakeData);
			}

			Task<Void> respondingTask = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					System.out.println("Started Handshake responding task");
					LocalTime start = LocalTime.now();

					while (Duration.between(start, LocalTime.now()).toSeconds() <= TIMEOUT) {

						PublicKey key = publicKeys.get(address);
						if (key != null) {
							System.out.println("Handshake responding task - key was not null, proceeding");
							updateMessage("Received public key.");

							byte[] keyBytesToEncode = HandshakeComplexBody.serializeKey(
									EncryptionUtils.getRandomSecureKey(KeySize.K_128));
							byte[] ivBytesToEncode = HandshakeComplexBody.serializeIv(
									EncryptionUtils.generateInitializationVector());
							byte[] encodedKey = asymmetricEncryption.encryptWithPublic(keyBytesToEncode, key);
							byte[] encodedIv = asymmetricEncryption.encryptWithPublic(ivBytesToEncode, key);
							System.out.println("Encoded key: " + Arrays.toString(encodedKey));
							System.out.println("Encoded key size: " + encodedKey.length);
							System.out.println("Encoded iv: " + Arrays.toString(encodedIv));
							System.out.println("Encoded iv size: " + encodedIv.length);

							HandshakeComplexBody handshakeBody = new HandshakeComplexBody(encodedKey, encodedIv, encryptionMode);

							System.out.println("Handshake responding task - getting data");
							TransferData data = TransferData.getPartTwoHandshakeData(null, Metadata.TransferType.REQUEST);
							data.setHandshakeComplexBody(handshakeBody);
							updateMessage("Responded with session info.");
							System.out.println("Handshake responding task - writing data");
							connectionThread.write(data);

							LocalTime responseWaitStart = LocalTime.now();
							System.out.println("Handshake responding task - waiting for SessionInfo response");
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
