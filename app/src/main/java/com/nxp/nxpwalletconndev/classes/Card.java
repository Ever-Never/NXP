package com.nxp.nxpwalletconndev.classes;


public class Card {
	int id;
	int idScript;
	int idVc;
	
	public static final int STATUS_CREATING = 0x00;
	public static final int STATUS_PERSONALIZING = 0x01;
	public static final int STATUS_PERSONALIZED = 0x03;
	public static final int STATUS_DELETING = 0x02;	
	public static final int STATUS_FAILED = 0x04;
	public static final int STATUS_ACTIVATING = 0x05;
	public static final int STATUS_READING = 0x06;
	public static final int STATUS_SERVER_FAILED = 0x07;
	
	public static final int TYPE_PAYMENTS = 0x00;
	public static final int TYPE_MIFARE_CLASSIC = 0x01;
	public static final int TYPE_MIFARE_DESFIRE = 0x02;
	
	public static final int MIFARE_HOSPITALITY = 0x00;
	public static final int MIFARE_TICKETING = 0x01;
	public static final int MIFARE_LOYALTY = 0x02;
	public static final int MIFARE_PAYIN = 0x03;
	public static final int MIFARE_MYMIFAREAPP = 0x04;
	
	private String cardName;
	private String cardNumber;
	private String cardCVC;
	private String cardExpMonth;
	private String cardExpYear;

	private int status;
	
	private boolean isFav;
	private boolean isLocked;
	
	private int iconRsc;
	
	private int mifareType;
	private int type;
	private int order;
	
	public Card(int id, int idScript, int idVc, String cardName, String cardNumber, String cardCVC, String cardExpMonth, String cardExpYear, 
			int status, boolean isFav, boolean isLocked, int iconRsc, int mifareType, int type, int order) {
		this.id = id;
		this.idScript = idScript;
		this.idVc = idVc;
		
		this.cardName = cardName;
		this.cardNumber = cardNumber;
		this.cardCVC = cardCVC;
		this.cardExpMonth = cardExpMonth;
		this.cardExpYear = cardExpYear;
		
		this.status = status;
		this.isFav = isFav;
		this.isLocked = isLocked;
		this.iconRsc = iconRsc;
		
		this.mifareType = mifareType;
		this.type = type;
		this.order = order;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getMifareType() {
		return mifareType;
	}

	public void setMifareType(int mifareType) {
		this.mifareType = mifareType;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public void setLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	public int getIconRsc() {
		return iconRsc;
	}

	public void setIconRsc(int iconRsc) {
		this.iconRsc = iconRsc;
	}

	public int getIdScript() {
		return idScript;
	}

	public void setIdScript(int idScript) {
		this.idScript = idScript;
	}

	public int getIdVc() {
		return idVc;
	}

	public void setIdVc(int idVc) {
		this.idVc = idVc;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isFav() {
		return isFav;
	}

	public void setFav(boolean isFav) {
		this.isFav = isFav;
	}

	public String getCardName() {
		return cardName;
	}
	
	public void setCardName(String cardName) {
		this.cardName = cardName;
	}
	
	public String getCardNumber() {
		return cardNumber;
	}
	
	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}
	
	public String getCardCVC() {
		return cardCVC;
	}
	
	public void setCardCVC(String cardCVC) {
		this.cardCVC = cardCVC;
	}
	
	public String getCardExpMonth() {
		return cardExpMonth;
	}
	
	public void setCardExpMonth(String cardExpMonth) {
		this.cardExpMonth = cardExpMonth;
	}
	
	public String getCardExpYear() {
		return cardExpYear;
	}
	
	public void setCardExpYear(String cardExpYear) {
		this.cardExpYear = cardExpYear;
	}
}
