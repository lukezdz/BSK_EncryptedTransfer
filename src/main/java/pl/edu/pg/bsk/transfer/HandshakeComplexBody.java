package pl.edu.pg.bsk.transfer;

import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.util.Base64;

public class HandshakeComplexBody implements Serializable {
	private final String keyString;
	private final byte[] ivBytes;
	private final EncryptionMode mode;

	public HandshakeComplexBody(SecretKey key, IvParameterSpec iv, EncryptionMode mode) {
		keyString = serializeKey(key);
		ivBytes = serializeIv(iv);
		this.mode = mode;
	}

	private String serializeKey(SecretKey key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	private byte[] serializeIv(IvParameterSpec iv) {
		return iv.getIV();
	}

	public SecretKey getKey() {
		byte[] decodedKey = Base64.getDecoder().decode(keyString);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
	}

	public IvParameterSpec getIv() {
		return new IvParameterSpec(ivBytes);
	}

	public EncryptionMode getMode() {
		return mode;
	}
}
