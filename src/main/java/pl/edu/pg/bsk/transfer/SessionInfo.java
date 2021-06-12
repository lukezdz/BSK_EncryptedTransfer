package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class SessionInfo {
	@Getter
	private final SecretKey sessionKey;
	@Getter
	private final EncryptionMode encryptionMode;
	@Getter
	private final IvParameterSpec initializationVector;

	public SessionInfo(SecretKey secretKey, EncryptionMode encryptionMode, IvParameterSpec iv) {
		this.sessionKey = secretKey;
		this.encryptionMode = encryptionMode;
		this.initializationVector = iv;
	}
}
