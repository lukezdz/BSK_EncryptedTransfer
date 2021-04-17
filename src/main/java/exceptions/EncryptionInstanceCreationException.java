package exceptions;

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
