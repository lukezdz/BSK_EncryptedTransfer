package pl.edu.pg.bsk.transfer;

import lombok.Getter;

public class TransferPart {
	@Getter
	private final byte[] data;
	@Getter
	private final int partNum;

	public TransferPart(byte[] data, int number) {
		this.data = data;
		this.partNum = number;
	}
}
