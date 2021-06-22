package pl.edu.pg.bsk.transfer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.lang.reflect.Type;
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
		Gson gson = new Gson();
		String metadataJson = gson.toJson(metadata);
		String dataJson = gson.toJson(publicKey.getEncoded());
		formatted.put(METADATA, metadataJson);
		formatted.put(BODY, dataJson);

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] getPartTwoHandshakeBody(SecretKey secretKey, IvParameterSpec iv, EncryptionMode mode) {
		JSONObject body = new JSONObject();
		Gson gson = new Gson();
		body.put(BODY_SECRET_KEY, gson.toJson(secretKey));
		body.put(BODY_IV, gson.toJson(iv));
		body.put(BODY_ENCRYPTION_MODE, gson.toJson(mode));

		return body.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] getPartTwoHandshakeData(byte[] encrypted, Metadata.TransferType transferType) {
		JSONObject formatted = new JSONObject();
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(2);
		metadata.setTransferType(transferType);
		Gson gson = new Gson();
		formatted.put(METADATA, gson.toJson(metadata));
		formatted.put(BODY, gson.toJson(encrypted));

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static SessionInfo parsePartTwoHandshakeData(byte[] decrypted) throws ParseException {
		JSONParser parser = new JSONParser();
		String str = new String(decrypted);
		JSONObject parsed = (JSONObject) parser.parse(str);

		Gson gson = new Gson();
		Type secretKeyType = new TypeToken<SecretKey>() {}.getType();
		Type ivType = new TypeToken<IvParameterSpec>() {}.getType();
		Type modeType = new TypeToken<EncryptionMode>() {}.getType();

		SecretKey secretKey = gson.fromJson((String) parsed.get(BODY_SECRET_KEY), secretKeyType);
		IvParameterSpec iv = gson.fromJson((String) parsed.get(BODY_IV), ivType);
		EncryptionMode mode = gson.fromJson((String) parsed.get(BODY_ENCRYPTION_MODE), modeType);

		return new SessionInfo(secretKey, mode, iv);
	}

	public static byte[] getTransferData(byte[] data, Metadata metadata) {
		JSONObject formatted = new JSONObject();
		Gson gson = new Gson();
		String metadataJson = gson.toJson(metadata);
		String dataJson = gson.toJson(data);
		System.out.println("Metadata json: " + metadataJson);
		System.out.println("Data json: " + dataJson);
		formatted.put(METADATA, metadataJson);
		formatted.put(BODY, dataJson);

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static ReadTransferData readTransferData(byte[] transferData) throws ParseException {
		JSONParser parser = new JSONParser();
		String str = new String(transferData);
		JSONObject parsed = (JSONObject) parser.parse(str);

		Gson gson = new Gson();
		Type metadataType = new TypeToken<Metadata>() {}.getType();
		Type dataType = new TypeToken<byte[]>() {}.getType();
		Metadata metadata = gson.fromJson((String) parsed.get(METADATA), metadataType);
		byte[] data = gson.fromJson((String) parsed.get(BODY), dataType);

		ReadTransferData read = new ReadTransferData(metadata);
		read.setData(data);
		return read;
	}
}
