package pl.edu.pg.bsk.encryption;

public enum EncryptionMode {
	AES_ECB("AES/ECB"),
	AES_CBC("AES/CBC"),
	AES_CFB("AES/CFB"),
	AES_OFB("AES/OFB"),
	AES_CTR("AES/CTR");

	private final String mode;

	EncryptionMode(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return this.mode;
	}
}
