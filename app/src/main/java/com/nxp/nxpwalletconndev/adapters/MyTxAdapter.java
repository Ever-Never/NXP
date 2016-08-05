package com.nxp.nxpwalletconndev.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Transaction;

public class MyTxAdapter extends BaseAdapter {
	private LayoutInflater inflater=null; 
    private List<Transaction> txs;
 
    public MyTxAdapter(Context c, List<Transaction> txs) {
    	this.txs = txs;
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
 
    public int getCount() {
        return txs.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(final int position, View convertView, ViewGroup parent) {
        View vi=convertView;
        
        if(convertView==null)
            vi = inflater.inflate(R.layout.list_transaction_entry, null);
        
        ImageView icon = (ImageView) vi.findViewById(R.id.card_icon);
        TextView name = (TextView) vi.findViewById(R.id.card_name);
        TextView date = (TextView) vi.findViewById(R.id.tx_date);
        TextView amount = (TextView) vi.findViewById(R.id.tx_amount);

        icon.setImageResource(txs.get(position).getCard_icon());
        name.setText(txs.get(position).getCard_name());
        date.setText(txs.get(position).getDate());
        amount.setText(txs.get(position).getAmount() + " " + txs.get(position).getCurrency());
        
        return vi;
    }
    
    /**
     * Method used to refres the listView
     */
    public void updateTransactions(List<Transaction> txs) {
    	this.txs = txs;
    }
}
