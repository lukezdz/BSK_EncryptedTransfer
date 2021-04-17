package exceptions;

public class EncryptionFailedException extends Exception {
	private String message;

	public EncryptionFailedException(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return message;
	}
}
