package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

public class TransferData {
	public static final String METADATA = "Metadata";
	public static final String BODY = "Body";
	public static final String BODY_SECRET_KEY = "SecretKey";
	public static final String BODY_IV = "InitializationVector";
	public static final String BODY_ENCRYPTION_MODE = "EncryptionMode";



	public static class ReadTransferData {
		@Getter
		final Metadata metadata;

		@Getter
		@Setter
		byte[] data;

		private ReadTransferData(Metadata metadata) {
			this.metadata = metadata;
		}
	}

	public static byte[] getPartOneHandshakeData(PublicKey publicKey, Metadata.TransferType transferType) {
		JSONObject formatted = new JSONObject();
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(1);
		metadata.setTransferType(transferType);
		formatted.put(METADATA, metadata);
		formatted.put(BODY, publicKey.getEncoded());

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] getPartTwoHandshakeBody(SecretKey secretKey, IvParameterSpec iv, EncryptionMode mode) {
		JSONObject body = new JSONObject();
		body.put(BODY_SECRET_KEY, secretKey);
		body.put(BODY_IV, iv);
		body.put(BODY_ENCRYPTION_MODE, mode);

		return body.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] getPartTwoHandshakeData(byte[] encrypted, Metadata.TransferType transferType) {
		JSONObject formatted = new JSONObject();
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(2);
		metadata.setTransferType(transferType);
		formatted.put(METADATA, metadata);
		formatted.put(BODY, encrypted);

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static SessionInfo parsePartTwoHandshakeData(byte[] decrypted) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject parsed = (JSONObject) parser.parse(new String(decrypted));

		SecretKey secretKey = (SecretKey) parsed.get(BODY_SECRET_KEY);
		IvParameterSpec iv = (IvParameterSpec) parsed.get(BODY_IV);
		EncryptionMode mode = (EncryptionMode) parsed.get(BODY_ENCRYPTION_MODE);

		return new SessionInfo(secretKey, mode, iv);
	}

	public static byte[] getTransferData(byte[] data, Metadata metadata) {
		JSONObject formatted = new JSONObject();
		formatted.put(METADATA, metadata);
		formatted.put(BODY, data);

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static ReadTransferData readTransferData(byte[] transferData) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject parsed = (JSONObject) parser.parse(new String(transferData));

		Metadata metadata = (Metadata) parsed.get(METADATA);
		byte[] data = (byte[]) parsed.get(BODY);

		ReadTransferData read = new ReadTransferData(metadata);
		read.setData(data);
		return read;
	}
}
