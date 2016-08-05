package com.nxp.nxpwalletconndev.listeners;

import java.util.ArrayList;

import com.nxp.nxpwalletconndev.classes.Transaction;

public interface OnReadRecordResultListener {
    public abstract void onReadRecordResult(ArrayList<Transaction> txs, int id);
}
