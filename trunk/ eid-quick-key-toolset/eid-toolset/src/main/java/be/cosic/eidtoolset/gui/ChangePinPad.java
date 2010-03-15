package be.cosic.eidtoolset.gui;

import be.cosic.eidtoolset.engine.EidCard;

public class ChangePinPad extends Thread {
	private EidCard eidCard;

	public ChangePinPad(EidCard ec) {
		super();
		eidCard = ec;
	}

	public void run() {
		try {
			eidCard.changePin();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}