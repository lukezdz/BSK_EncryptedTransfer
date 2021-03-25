package pl.edu.pg.bsk.encryption;

public enum EncryptionMode {
	AES_ECB("AES/ECB/PKCS5Padding"),
	AES_CBC("AES/CBC/PKCS5Padding"),
	AES_CFB("AES/CFB/PKCS5Padding"),
	AES_OFB("AES/OFB/PKCS5Padding"),
	AES_CTR("AES/CTR/PKCS5Padding");

	private final String mode;

	EncryptionMode(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return this.mode;
	}
}
