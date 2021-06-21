package pl.edu.pg.bsk.transfer;

import lombok.SneakyThrows;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ConnectionThread extends Thread {
	private final TransferHandler handler;
	private final Socket socket;
	private final DataOutputStream dos;
	private final DataInputStream dis;

	public ConnectionThread(TransferHandler handler, Socket socket) throws IOException {
		this.handler = handler;
		this.socket = socket;
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}

	@SneakyThrows
	@Override
	public void run() {
		System.out.println("Started connection thread for " + socket.getInetAddress());
		while (!socket.isClosed()) {
			try {
				int length = dis.readInt();

				if (length > 0) {
					byte[] read = new byte[length];
					dis.readFully(read, 0, read.length);
					handler.receiveData(read, socket.getInetAddress());
				}
			} catch (IOException | ParseException | EncryptionFailedException | NoSuchAlgorithmException | InvalidKeySpecException ignored) {
				socket.close();
			}
		}
	}

	public void write(byte[] data) throws IOException {
		System.out.println("Sending data to " + socket.getInetAddress() + ":" + socket.getInetAddress().getHostAddress());
		dos.writeInt(data.length);
		dos.write(data);
	}

	public void close() throws IOException {
		socket.close();
	}

	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
}
