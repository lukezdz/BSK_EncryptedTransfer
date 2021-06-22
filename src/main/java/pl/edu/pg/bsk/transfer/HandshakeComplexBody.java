package pl.edu.pg.bsk.transfer;

import lombok.Getter;
import pl.edu.pg.bsk.encryption.EncryptionMode;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.util.Base64;

@Getter
public class HandshakeComplexBody implements Serializable {
	private final byte[] encodedKey;
	private final byte[] encodedIv;
	private final EncryptionMode mode;

	public HandshakeComplexBody(byte[] key, byte[] iv, EncryptionMode mode) {
		encodedKey = key;
		encodedIv = iv;
		this.mode = mode;
	}

	public static byte[] serializeKey(SecretKey key) {
		return Base64.getEncoder().encodeToString(key.getEncoded()).getBytes();
	}

	public static byte[] serializeIv(IvParameterSpec iv) {
		return iv.getIV();
	}

	public static SecretKey deserializeKey(byte[] key) {
		byte[] decodedKey = Base64.getDecoder().decode(key);
		return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
	}

	public static IvParameterSpec deserializeIv(byte[] iv) {
		return new IvParameterSpec(iv);
	}
}
