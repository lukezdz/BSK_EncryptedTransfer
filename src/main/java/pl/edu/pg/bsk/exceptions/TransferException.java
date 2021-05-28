package pl.edu.pg.bsk.exceptions;

public class TransferException extends Exception {
	private final String message;

	public TransferException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
