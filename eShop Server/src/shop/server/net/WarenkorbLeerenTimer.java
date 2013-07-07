package shop.server.net;

import java.util.TimerTask;

import shop.common.exceptions.ArtikelBestandIstKeineVielfacheDerPackungsgroesseException;
import shop.common.interfaces.ShopInterface;
import shop.common.valueobjects.Kunde;

public class WarenkorbLeerenTimer extends TimerTask {

	private ShopInterface shop;
	private Kunde k;
	
	public WarenkorbLeerenTimer(ShopInterface shop, Kunde k){
		this.shop = shop;
		this.k = k;
	}
	
	@Override
	public void run() {
		try {
			shop.leeren(k);
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			System.err.println("Fehler beim Timeout Leeren des Warenkorbes!");
			System.err.println(e.getMessage());
		}
	}

}
