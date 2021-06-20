package pl.edu.pg.bsk.controllers;

import lombok.Getter;
import pl.edu.pg.bsk.transfer.Metadata;

import java.io.File;
import java.net.InetAddress;

@Getter
public class ControllerNotification {
	private final Metadata metadata;
	private final InetAddress from;
	private String message;
	private File file;

	public ControllerNotification(Metadata metadata, InetAddress inetAddress, String message) {
		this.metadata = metadata;
		this.from = inetAddress;
		this.message = message;
	}

	public ControllerNotification(Metadata metadata, InetAddress inetAddress, File file) {
		this.metadata = metadata;
		this.from = inetAddress;
		this.file = file;
	}
}