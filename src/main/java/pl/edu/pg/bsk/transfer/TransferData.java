package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

@Getter
@Setter
public class TransferData {
	private final Metadata metadata;
	private final byte[] payload;

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

	public static byte[] getPartTwoHandshakeBody(SecretKey secretKey, IvParameterSpec iv, EncryptionMode mode) {
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

	public static SessionInfo parsePartTwoHandshakeData(byte[] decrypted) throws ParseException {
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

		return new SessionInfo(body.getKey(), body.getMode(), body.getIv());
	}
}
