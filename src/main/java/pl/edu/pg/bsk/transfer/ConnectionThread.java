package pl.edu.pg.bsk.transfer;

import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.exceptions.EncryptionFailedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class ConnectionThread extends Thread {
	private final TransferHandler handler;
	private final Socket socket;

	public ConnectionThread(TransferHandler handler, Socket socket) {
		this.handler = handler;
		this.socket = socket;
	}

	@Override
	public void run() {
		while (!socket.isClosed()) {
			try {
				byte[] read = socket.getInputStream().readAllBytes();

				if (read.length == 0) {
					continue;
				}

				handler.receiveData(read, socket.getInetAddress());
			} catch (IOException | ParseException | EncryptionFailedException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
				exception.printStackTrace();
			}
		}
	}

	public void write(byte[] data) throws IOException {
		socket.getOutputStream().write(data);
	}

	public void close() throws IOException {
		socket.close();
	}

	public InetAddress getInetAddress() {
		return socket.getInetAddress();
	}
}