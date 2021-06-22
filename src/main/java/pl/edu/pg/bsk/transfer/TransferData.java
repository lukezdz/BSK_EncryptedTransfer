package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import lombok.Setter;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PublicKey;

@Getter
@Setter
public class TransferData implements Serializable {
	private final Metadata metadata;
	private byte[] payload;
	private HandshakeComplexBody handshakeComplexBody;

	public TransferData(Metadata metadata, byte[] data) {
		this.metadata = metadata;
		this.payload = data;
	}

	public static TransferData getPartOneHandshakeData(PublicKey publicKey, Metadata.TransferType transferType) {
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(1);
		metadata.setTransferType(transferType);

		return new TransferData(metadata, publicKey.getEncoded());
	}

	public static byte[] getPartTwoHandshakeBody(byte[] secretKey, byte[] iv, EncryptionMode mode) {
		HandshakeComplexBody body = new HandshakeComplexBody(secretKey, iv, mode);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		byte[] result = null;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(body);
			out.flush();
			result = bos.toByteArray();
		} catch (IOException ex) {

		}
		finally {
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return result;
	}

	public static TransferData getPartTwoHandshakeData(byte[] encrypted, Metadata.TransferType transferType) {
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(2);
		metadata.setTransferType(transferType);

		return new TransferData(metadata, encrypted);
	}

	public static HandshakeComplexBody parsePartTwoHandshakeData(byte[] decrypted) {
		ByteArrayInputStream bis = new ByteArrayInputStream(decrypted);
		ObjectInput in = null;
		HandshakeComplexBody body = null;
		try {
			in = new ObjectInputStream(bis);
			 body = (HandshakeComplexBody) in.readObject();
		} catch (IOException | ClassNotFoundException ex) {

		}
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return body;
	}
}
