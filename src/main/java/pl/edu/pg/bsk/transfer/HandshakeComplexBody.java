package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.Serializable;

@Getter
public class HandshakeComplexBody implements Serializable {
	private final SecretKey key;
	private final IvParameterSpec iv;
	private final EncryptionMode mode;

	public HandshakeComplexBody(SecretKey key, IvParameterSpec iv, EncryptionMode mode) {
		this.key = key;
		this.iv = iv;
		this.mode = mode;
	}
}
