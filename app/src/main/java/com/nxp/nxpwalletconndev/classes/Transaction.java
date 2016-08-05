package com.nxp.nxpwalletconndev.classes;

public class Transaction {
	private int card_icon;
	private String card_name;
	
	private String date;
	private String amount;
	private String currency;
	
	public Transaction(int icon, String name, String date, String amount, String currency) {
		this.card_icon = icon;
		this.card_name = name;
		this.date = date;
		this.amount = amount;
		this.currency = currency;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getCard_icon() {
		return card_icon;
	}

	public void setCard_icon(int card_icon) {
		this.card_icon = card_icon;
	}

	public String getCard_name() {
		return card_name;
	}

	public void setCard_name(String card_name) {
		this.card_name = card_name;
	}
}
