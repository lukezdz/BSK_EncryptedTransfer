package pl.edu.pg.bsk.transfer;

import lombok.SneakyThrows;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ConnectionThread extends Thread {
	private final TransferHandler handler;
	private final Socket socket;
	private final DataOutputStream dos;
	private final DataInputStream dis;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;

	public ConnectionThread(TransferHandler handler, Socket socket) throws IOException {
		this.handler = handler;
		this.socket = socket;
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
	}

	@SneakyThrows
	@Override
	public void run() {
		System.out.println("Started connection thread for " + socket.getInetAddress());
		while (!socket.isClosed()) {
			try {
				TransferData data = (TransferData) ois.readObject();

				if (data != null) {
					System.out.println("Read int other than 0 from dis!");
					handler.receiveData(data, socket.getInetAddress());
				}
			} catch (IOException | ParseException | EncryptionFailedException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
				System.out.println("Closing socket:");
				exception.printStackTrace();
				socket.close();
			}
		}
	}

	public void write(TransferData data) throws IOException {
		System.out.println("Sending data to " + socket.getInetAddress() + ":" + socket.getInetAddress().getHostAddress());
		oos.writeObject(data);
	}

	public void close() throws IOException {
		socket.close();
	}

	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
}
