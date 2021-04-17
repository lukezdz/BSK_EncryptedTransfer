package pl.edu.pg.bsk.exceptions;

public class EncryptionInstanceCreationException extends Exception {
	private String message;

	public EncryptionInstanceCreationException(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return message;
	}
}
