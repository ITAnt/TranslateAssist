package com.sky.translate;

import java.awt.TextArea;

public enum PrintTools {
	INSTANCE;
	private StringBuilder mOutBuilder;
	private TextArea mOutLabel;
	
	public void init(StringBuilder builder, TextArea area) {
		this.mOutBuilder = builder;
		this.mOutLabel = area;
	}
	
	public void println(String message) {
		if (mOutLabel != null) {
			mOutLabel.append(message);
			mOutLabel.append("\n");
		}
		
		/*if (mOutBuilder != null) {
			mOutBuilder.append(message).append("\n");
		}*/
	}
	
	public void print(String message) {
		if (mOutLabel != null) {
			mOutLabel.append(message);
		}
		
		/*if (mOutBuilder != null) {
			mOutBuilder.append(message);
		}*/
	}
	
	public void println(String... messages) {
		for (String message : messages) {
		}
	}
	
	public void printlnNow(String message) {
		if (mOutLabel != null) {
			mOutLabel.append(message);
			mOutLabel.append("\n");
		}
		
		/*if (mOutBuilder != null) {
			mOutBuilder.append(message).append("\n");
		}*/
	}
	
	public void clearLog() {
		if (mOutLabel != null) {
			mOutLabel.setText("");
		}
		
		/*if (mOutBuilder != null) {
			mOutBuilder.delete(0, mOutBuilder.length());
		}*/
	}
}