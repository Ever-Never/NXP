package com.nxp.nxpwalletconndev.adapters;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nxp.nxpwalletconndev.R;
import com.nxp.nxpwalletconndev.classes.Card;
import com.nxp.nxpwalletconndev.listeners.OnFavClickListener;

public class MyCardsAdapter extends BaseAdapter {
	private OnFavClickListener mClickListener = null;
	
    private LayoutInflater inflater=null; 
    private List<Card> cards;
 
    public MyCardsAdapter(Context c, List<Card> cards, OnFavClickListener listener) {
    	this.cards = cards;
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mClickListener = listener;
    }
 
    public int getCount() {
        return cards.size();
    }
 
    public Object getItem(int position) {
        return position;
    }
 
    public long getItemId(int position) {
        return position;
    }
 
    public View getView(final int position, View convertView, ViewGroup parent) {
    	View vi = convertView;
        
        if(vi == null)
            vi = inflater.inflate(R.layout.list_card_entry, null);
        
        TextView name = (TextView) vi.findViewById(R.id.card_name);
        TextView number = (TextView) vi.findViewById(R.id.card_number);
        TextView exp = (TextView) vi.findViewById(R.id.card_exp);
        
        ImageView cardFav = (ImageView) vi.findViewById(R.id.card_fav);
        ImageView cardIcon = (ImageView) vi.findViewById(R.id.card_icon);
        ImageView cardLocked = (ImageView) vi.findViewById(R.id.card_locked); 
        
        Log.d("MyCardsAdapter", "My Cards. Script ID: " + cards.get(position).getIdScript() + " VC Entry: " + cards.get(position).getIdVc() +
        		" Status: " + cards.get(position).getStatus() + " Fav: " + cards.get(position).isFav());
        
        switch(cards.get(position).getStatus()) {
        	case Card.STATUS_PERSONALIZED:
        		if(cards.get(position).getType() == Card.TYPE_PAYMENTS) {
	        		name.setText(cards.get(position).getCardName());
			        number.setText("XXXX XXXX XXXX " + cards.get(position).getCardNumber().substring(cards.get(position).getCardNumber().length() - 4));
			        exp.setText(cards.get(position).getCardExpMonth() + " / " + cards.get(position).getCardExpYear());
			        
			        cardFav.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if(mClickListener != null)
					            mClickListener.onFavClick(position); 
						}
					});
			
			        if(cards.get(position).isFav() == true) {
			        	cardFav.setImageResource(R.drawable.fav);
			        } else {
			        	cardFav.setImageResource(R.drawable.no_fav);
			        }
			        
			        if(cards.get(position).isLocked() == true) {
			        	cardLocked.setVisibility(View.VISIBLE);
			        } else {
			        	cardLocked.setVisibility(View.GONE);
			        }
        		} else if(cards.get(position).getType() == Card.TYPE_MIFARE_CLASSIC
        				|| cards.get(position).getType() == Card.TYPE_MIFARE_DESFIRE) {
        			name.setText(cards.get(position).getCardName());
        			cardFav.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if(mClickListener != null)
					            mClickListener.onFavClick(position); 
						}
					});
			
			        if(cards.get(position).isFav() == true) {
			        	cardFav.setImageResource(R.drawable.fav);
			        } else {
			        	cardFav.setImageResource(R.drawable.no_fav);
			        }
			        
			        if(cards.get(position).getMifareType() == Card.MIFARE_HOSPITALITY) {
			        	if(cards.get(position).getCardNumber().equals("999") == true)
			        		number.setText("To complete check in please tap your card against a check-in post!");
			        	else
			        		number.setText("Room: " + cards.get(position).getCardNumber());
			        } else if(cards.get(position).getMifareType() == Card.MIFARE_LOYALTY) {
		        		number.setText("Points: " + cards.get(position).getCardNumber());
			        } else if(cards.get(position).getMifareType() == Card.MIFARE_TICKETING) {
		        		number.setText("Trips: " + cards.get(position).getCardNumber());
			        } else if(cards.get(position).getMifareType() == Card.MIFARE_MYMIFAREAPP) {

			        }
        		}
		        	        
		        // Show the card icon for this particular credit card
		        cardIcon.setImageResource(cards.get(position).getIconRsc());
		        
		        break;
		        
        	case Card.STATUS_ACTIVATING:
        		if(cards.get(position).getType() == Card.TYPE_PAYMENTS) {
	        		name.setText(cards.get(position).getCardName());
			        number.setText("XXXX XXXX XXXX " + cards.get(position).getCardNumber().substring(cards.get(position).getCardNumber().length() - 4));
			        exp.setText(cards.get(position).getCardExpMonth() + " / " + cards.get(position).getCardExpYear());
			        		
			        cardFav.setImageResource(R.drawable.activating);
			        			        
			        if(cards.get(position).isLocked() == true) {
			        	cardLocked.setVisibility(View.VISIBLE);
			        } else {
			        	cardLocked.setVisibility(View.GONE);
			        }
        		} else if(cards.get(position).getType() == Card.TYPE_MIFARE_CLASSIC
        				|| cards.get(position).getType() == Card.TYPE_MIFARE_DESFIRE) {
        			name.setText(cards.get(position).getCardName());
        			number.setText(cards.get(position).getCardNumber());
        					
        			cardFav.setImageResource(R.drawable.activating);
        		}
		        	        
		        // Show the card icon for this particular credit card
		        cardIcon.setImageResource(cards.get(position).getIconRsc());
		        
		        break;

        	case Card.STATUS_CREATING:
        		name.setText(vi.getResources().getString(R.string.my_cards_creating));
        		
        		// Is not personalized, thus cannot be fav 
    	        cardFav.setImageResource(R.drawable.no_fav);
    	        cardIcon.setImageResource(R.drawable.card_blank);
        		
        		break;
        	case Card.STATUS_PERSONALIZING:
        		name.setText(vi.getResources().getString(R.string.my_cards_personalizing));
        		
        		// Is not personalized, thus cannot be fav 
    	        cardFav.setImageResource(R.drawable.no_fav);
    	        cardIcon.setImageResource(R.drawable.card_blank);
        		
        		break;
        	case Card.STATUS_DELETING:
        		name.setText(vi.getResources().getString(R.string.my_cards_deleting));
        		
        		// Is not personalized, thus cannot be fav 
    	        cardFav.setImageResource(R.drawable.no_fav);
    	        cardIcon.setImageResource(R.drawable.card_blank);
        		
        		break;
        		
        	case Card.STATUS_FAILED:
        		name.setText(vi.getResources().getString(R.string.my_cards_failed));
        		
        		// Is not personalized, thus cannot be fav 
    	        cardFav.setImageResource(R.drawable.no_fav);
    	        cardIcon.setImageResource(R.drawable.card_blank);
        		
        		break;
        }
                       
        return vi;
    }
    
    /**
     * Method used to refresh the listView
     */
    public void updateCards(List<Card> cards) {
    	this.cards = cards;
    }
    
    public List<Card> getList(){
		return this.cards;
	}
}
