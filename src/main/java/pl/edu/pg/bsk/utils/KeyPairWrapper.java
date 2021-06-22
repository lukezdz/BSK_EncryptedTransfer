package pl.edu.pg.bsk.utils;

import lombok.Getter;
import lombok.Setter;

import java.security.KeyPair;

public final class KeyPairWrapper {
	@Getter
	@Setter
	KeyPair pair;

	private final static KeyPairWrapper INSTANCE = new KeyPairWrapper();

	private KeyPairWrapper() {
		pair = null;
	}

	public static KeyPairWrapper getInstance() {
		return INSTANCE;
	}
}
