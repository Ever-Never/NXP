package com.nxp.nxpwalletconndev.listeners;

public interface OnOperationListener {
    public abstract void processOperationResult(byte[] mBufferDataCmd);
    public abstract void processOperationNotCompleted();
}
