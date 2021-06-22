package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import lombok.Setter;

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

	public static TransferData getPartTwoHandshakeData(byte[] encrypted, Metadata.TransferType transferType) {
		Metadata metadata = new Metadata(Metadata.MetadataType.HANDSHAKE);
		metadata.setHandshakePart(2);
		metadata.setTransferType(transferType);

		return new TransferData(metadata, encrypted);
	}
}
