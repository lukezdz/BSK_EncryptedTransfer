package encryption;

import lombok.Getter;

public enum KeySize {
	K_128(128),
	K_192(192),
	K_256(256);

	@Getter
	private final int size;

	KeySize(int size) {
		this.size = size;
	}
}
