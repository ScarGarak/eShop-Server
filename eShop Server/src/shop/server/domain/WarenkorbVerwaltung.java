package shop.server.domain;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import shop.common.exceptions.ArtikelBestandIstKeineVielfacheDerPackungsgroesseException;
import shop.common.exceptions.ArtikelBestandIstZuKleinException;
import shop.common.exceptions.ArtikelExistiertNichtException;
import shop.common.exceptions.WarenkorbIstLeerException;
import shop.common.valueobjects.Artikel;
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.WarenkorbArtikel;

/**
 * Klasse zur Verwaltung vom Warenkorb.
 * 
 * @author Christof Ferreira Torres
 * @version 1.0.0
 */
public class WarenkorbVerwaltung {
	
	/**
	 * Synchronisierte methode zum hinzufügen eines Warenkorb Artikels in den Warenkorb.
	 * 
	 * @param kunde Der Kunde der einen Warenkorb Artikel in seinen Warenkorb hinzufügen will.
	 * @param warenkorbArtikel Der Warenkorb Artikel der in den Warenkorb hinzugefügt werden soll.
	 * @throws ArtikelBestandIstZuKleinException 
	 * @throws ArtikelExistiertNichtException 
	 * @throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException 
	 */
	public synchronized void hinzufuegen(Kunde kunde, WarenkorbArtikel warenkorbArtikel) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {	
		if (kunde.getWarenkorb().contains(warenkorbArtikel)) {
			this.stueckzahlAendern(kunde, warenkorbArtikel, kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getStueckzahl() + warenkorbArtikel.getStueckzahl());
		} else {
			int alterBestand = warenkorbArtikel.getArtikel().getBestand();
			if (alterBestand - warenkorbArtikel.getStueckzahl() >= 0) {
				warenkorbArtikel.getArtikel().setBestand(alterBestand - warenkorbArtikel.getStueckzahl());
				kunde.getWarenkorb().add(warenkorbArtikel);
			} else {
				throw new ArtikelBestandIstZuKleinException(kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel(), " - in 'stueckzahlAendern()'");
			}
		}
	}
	
	/**
	 * Synchronisierte methode zum ändern der Stückzahl eines Artikels im Warenkorb.
	 * 
	 * @param kunde Der Kunde der die Stückzahl eines Warenkorb Artikels in seinem Warenkorb ändern will.
	 * @param warenkorbArtikel Der Warenkorb Artikel dessen Stückzahl verändert werden soll.
	 * @param neueStueckzahl Die neue Stückzahl des Warenkorb Artikels.
	 * @throws ArtikelBestandIstZuKleinException 
	 * @throws ArtikelExistiertNichtException 
	 * @throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException 
	 */
	public synchronized void stueckzahlAendern(Kunde kunde, WarenkorbArtikel warenkorbArtikel, int neueStueckzahl) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		if (kunde.getWarenkorb().contains(warenkorbArtikel)) {
			int alterBestand = kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel().getBestand();
			int alteStueckzahl = kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getStueckzahl();
			if (alteStueckzahl < neueStueckzahl) {
				if (alterBestand - (neueStueckzahl - alteStueckzahl) >= 0) {
					kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel().setBestand(alterBestand - (neueStueckzahl - alteStueckzahl));
					kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).setStueckzahl(neueStueckzahl);
				} else {
					throw new ArtikelBestandIstZuKleinException(kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel(), " - in 'stueckzahlAendern()'");
				}
			} else {
				kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel().setBestand(alterBestand + (alteStueckzahl - neueStueckzahl));
				kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).setStueckzahl(neueStueckzahl);
			}
		} else {
			throw new ArtikelExistiertNichtException(warenkorbArtikel.getArtikel(), " - in 'stueckzahlAendern()'");
		}
	}
	
	/**
	 * Synchronisierte methode zum entfernen eines Artikels im Warenkorb.
	 * 
	 * @param kunde Der Kunde der einen Warenkorb Artikel in seinem Warenkorb entfernen will.
	 * @param warenkorbArtikel Der Warenkorb Artikel der entfernt werden soll.
	 * @throws ArtikelExistiertNichtException 
	 * @throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException 
	 */
	public synchronized void entfernen(Kunde kunde, WarenkorbArtikel warenkorbArtikel) throws ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		if (kunde.getWarenkorb().contains(warenkorbArtikel)) {
			kunde.getWarenkorb().get(kunde.getWarenkorb().indexOf(warenkorbArtikel)).getArtikel().setBestand(warenkorbArtikel.getArtikel().getBestand() + warenkorbArtikel.getStueckzahl());
			kunde.getWarenkorb().remove(warenkorbArtikel);
		} else {
			throw new ArtikelExistiertNichtException(warenkorbArtikel.getArtikel(), " - in 'entfernen()'");
		}
	}
	
	/**
	 * Methode zum kaufen aller im Warenkorb enthaltenen Artikel.
	 * 
	 * @param kunde Der Kunde der alle Warenkorb Artikel in seinem Warenkorb kaufen will.
	 * @return List<WarenkorbArtikel> Eine liste von den gekauften Warenkorb Artikeln.
	 * @throws WarenkorbIstLeerException 
	 */
	public List<WarenkorbArtikel> kaufen(Kunde kunde) throws WarenkorbIstLeerException {
		if (kunde.getWarenkorb().isEmpty())
			throw new WarenkorbIstLeerException(" - in 'kaufen()'");
		List<WarenkorbArtikel> ergebnis = new Vector<WarenkorbArtikel>();
		Iterator<WarenkorbArtikel> iter = kunde.getWarenkorb().iterator();
		while (iter.hasNext()) {
			ergebnis.add(iter.next());
		}
		kunde.getWarenkorb().clear();
		return ergebnis;
	}
	
	/**
	 * Synchronisierte methode zum leeren des Warenkorbes.
	 * 
	 * @param kunde Der Kunde der seinen Warenkorb leeren will.
	 * @throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException 
	 */
	public synchronized void leeren(Kunde kunde) throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		Iterator<WarenkorbArtikel> iter = kunde.getWarenkorb().iterator();
		while (iter.hasNext()) {
			WarenkorbArtikel warenkorbArtikel = iter.next();
			warenkorbArtikel.getArtikel().setBestand(warenkorbArtikel.getArtikel().getBestand() + warenkorbArtikel.getStueckzahl());
		}
		kunde.getWarenkorb().clear();
	}

	/**
	 * Methode zum zurückgeben eines Warenkorb Artikels anhand eines Artikels.
	 * 
	 * @param kunde Der Kunde der einen Warenkorb Artikel zurück haben möchte.
	 * @param artikel Der Artikel von dem der Kunde einen Warenkorb Artikel möchte.
	 * @return WarenkorbArtikel Eine Referenz auf einen Warenkorb Artikel.
	 * @throws ArtikelExistiertNichtException 
	 */ 
	public WarenkorbArtikel getWarenkorbArtikel(Kunde kunde, Artikel artikel) throws ArtikelExistiertNichtException {
		int index = -1;

		Iterator<WarenkorbArtikel> iter = kunde.getWarenkorb().iterator();
		while (iter.hasNext()) {
			WarenkorbArtikel warenkorbArtikel = iter.next();
			if (warenkorbArtikel.getArtikel().equals(artikel))
				index = kunde.getWarenkorb().indexOf(warenkorbArtikel);
		}
		
		if (index != -1)
			return kunde.getWarenkorb().get(index);
		else
			throw new ArtikelExistiertNichtException(artikel, " - in 'getWarenkorbArtikel()'");
	}
	
}
