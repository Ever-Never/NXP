package com.nxp.nxpwalletconndev.httpcomm;

import org.json.JSONObject;

import com.nxp.nxpwalletconndev.utils.Parsers;

public class HttpResponseJson {
	private String type;
	private String transactionId;
	private String data;
	private byte[] dataBytes;
	private boolean isValid;

	public HttpResponseJson(String jsonString) {
		isValid = true;
		try {
			JSONObject jsonObj = new JSONObject(jsonString);
			this.type = jsonObj.getString(HttpProtokollConstants.ParameterNameType);
			this.transactionId = jsonObj.getString(HttpProtokollConstants.ParameterNameTransactionId);
			this.data = jsonObj.getString(HttpProtokollConstants.ParameterCardData);
			if (this.type.equalsIgnoreCase(HttpProtokollConstants.TypeValueCommandApdu)){
				Parsers.hexToArray(data);
			}
		} catch (Exception e) {
			isValid = false;
		}

	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getData() {
		return data;
	}
	
	public byte[] getDataBytes() {
		return dataBytes;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String toJsonString() {
		StringBuffer retVal = new StringBuffer("{\n\t");
		retVal.append("\"type\": \"" + type + "\",");
		retVal.append("\"transactionId\": \"" + transactionId + "\",");
		retVal.append("\"data\": \"" + data + "\"\n}");
		return retVal.toString();
	}
	
	public boolean isValid(){
		return isValid;
	}
}
