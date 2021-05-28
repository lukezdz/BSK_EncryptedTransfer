package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TransferData {
	public static String METADATA = "Metadata";
	public static String ENCRYPTION_MODE = "EncryptionMode";
	public static String BODY = "Body";

	public static class FileMetadata {
		@Getter
		final String filename;
		@Getter
		final String fileExt;

		public FileMetadata(String filename, String fileExt) {
			this.filename = filename;
			this.fileExt = fileExt;
		}
	}

	public static class ReadTransferData {
		@Getter
		final Optional<FileMetadata> metadata;
		@Getter
		final EncryptionMode encryptionMode;
		@Getter
		final byte[] data;

		private ReadTransferData(Optional<FileMetadata> metadata, EncryptionMode encryptionMode, byte[] data) {
			this.metadata = metadata;
			this.encryptionMode = encryptionMode;
			this.data = data;
		}
	}

	public static byte[] getTransferData(EncryptionMode encryptionMode, byte[] data, Optional<FileMetadata> fileMetadata) {
		JSONObject formatted = new JSONObject();
		if (fileMetadata.isPresent()) {
			formatted.put(METADATA, fileMetadata.get());
		}
		formatted.put(ENCRYPTION_MODE, encryptionMode);
		formatted.put(BODY, data);

		return formatted.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	public static ReadTransferData readTransferData(byte[] transferData) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject parsed = (JSONObject) parser.parse(new String(transferData));

		Optional<FileMetadata> metadata = parsed.get(METADATA) != null ? Optional.of((FileMetadata) parsed.get(METADATA)) : Optional.empty();
		EncryptionMode encryptionMode = (EncryptionMode) parsed.get(ENCRYPTION_MODE);
		byte[] data = (byte[]) parsed.get(BODY);

		return new ReadTransferData(metadata, encryptionMode, data);
	}
}
