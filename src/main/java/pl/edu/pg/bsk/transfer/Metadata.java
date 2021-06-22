package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class Metadata implements Serializable {
	private MetadataType type;
	private TransferType transferType;
	private long handshakePart;
	private String filename;
	private String fileExt;
	private long fileSize;
	private LocalDateTime sendTime;

	public enum MetadataType {
		MESSAGE("Message"),
		FILE("File"),
		HANDSHAKE("Handshake");

		private final String typeName;

		MetadataType(String typeName) {
			this.typeName = typeName;
		}
	}

	public enum TransferType {
		REQUEST("Request"),
		ANSWER("Answer"),
		TRANSFER("Transfer");

		private final String type;

		TransferType(String type) {
			this.type = type;
		}
	}

	public Metadata(MetadataType type) {
		this.type = type;
		this.sendTime = LocalDateTime.now();
	}
}
