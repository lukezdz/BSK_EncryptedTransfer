package pl.edu.pg.bsk.controllers;

import javafx.fxml.Initializable;

public abstract class NotifiableController implements Initializable {
	public abstract void notifyController(ControllerNotification notification);
}
