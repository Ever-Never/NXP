package com.nxp.nxpwalletconndev.listeners;


public interface OnTransmitApduListener {
    public abstract void sendApduToSE(byte[] apdu, int timeout);
}
