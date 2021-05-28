package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import java.security.PublicKey;

public class SessionInfo {
	@Getter
	private final PublicKey publicKey;
	@Getter
	private final SecretKey sessionKey;
	@Getter
	private final EncryptionMode encryptionMode;

	public SessionInfo(PublicKey publicKey, SecretKey secretKey, EncryptionMode encryptionMode) {
		this.publicKey = publicKey;
		this.sessionKey = secretKey;
		this.encryptionMode = encryptionMode;
	}
}
