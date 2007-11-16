package org.gudy.azureus2.ui.swt.progress;

public class ProgressReportMessage
	implements IMessage, IProgressReportConstants
{

	private String value = "";

	private int type;

	public ProgressReportMessage(String value, int type) {
		this.value = value;

		switch (type) {
			case MSG_TYPE_ERROR:
			case MSG_TYPE_INFO:
				this.type = type;
				break;
			default:
				this.type = MSG_TYPE_LOG;
		}
	}

	public String getValue() {
		return value;
	}

	public int getType() {
		return type;
	}

}
