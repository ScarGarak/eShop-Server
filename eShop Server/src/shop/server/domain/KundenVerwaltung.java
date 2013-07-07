package shop.server.domain;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import shop.common.exceptions.ArtikelBestandIstKeineVielfacheDerPackungsgroesseException;
import shop.common.exceptions.ArtikelBestandIstZuKleinException;
import shop.common.exceptions.ArtikelExistiertNichtException;
import shop.common.exceptions.KundeExistiertBereitsException;
import shop.common.exceptions.KundeExistiertNichtException;
import shop.common.exceptions.WarenkorbIstLeerException;
import shop.common.valueobjects.Artikel;
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.Rechnung;
import shop.common.valueobjects.WarenkorbArtikel;
import shop.server.persistence.data.DataPersistenceManager;
import shop.server.persistence.data.ObjectDataPersistenceManager;

/** 
 * This class manages a costumer list and provides the methods to add,
 * delete and search for costumers
 * 
 * @author Thummerer, Oliver
 * @version 1.0.0
 *  
 * last edited 23.04.12
 */
public class KundenVerwaltung {
	
	private Vector<Kunde> kundenListe = new Vector<Kunde>();
	
	private DataPersistenceManager pm = new ObjectDataPersistenceManager();
	
	private WarenkorbVerwaltung warenkorbVerwaltung = new WarenkorbVerwaltung();
	
	public void liesDaten(String dateiName) throws IOException{
		pm.openForReading(dateiName);
		
		Kunde k;
		
		do{
			k = pm.ladeKunden();
			if(k != null){
				try{
					einfuegen(k);
				}catch(KundeExistiertBereitsException e){
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}while(k != null);
		
		pm.close();
	}
	
	public synchronized void schreibeDaten(String dateiName) throws IOException{
		pm.openForWriting(dateiName);
		
		if(kundenListe != null){
			Iterator<Kunde> it = kundenListe.iterator();

			while(it.hasNext()){
				pm.speichereKunden(it.next());
			}
		}
		pm.close();
	}
	
	/**
	 * Method to insert a new costumer
	 */
	public void einfuegen(Kunde k) throws KundeExistiertBereitsException {
		if(!kundenListe.contains(k)){
			kundenListe.add(k);
		}else{
			throw new KundeExistiertBereitsException(k, "Fehler beim einfuegen!");
		}
	}
	
	public synchronized void bearbeiten(int id, String passwort, String name, String strasse, int plz, String wohnort, boolean blockiert) throws KundeExistiertNichtException {
		Kunde k = sucheKunde(id);
		k.setPasswort(passwort);
		k.setName(name);
		k.setStrasse(strasse);
		k.setPlz(plz);
		k.setWohnort(wohnort);
		k.setBlockiert(blockiert);
	}
	
	/**
	 * Method deletes the costumer instance.
	 * @param k Kunden instance to delete
	 */
	public void loeschen(Kunde k){
		this.kundenListe.remove(k);
	}
	
	/**
	 * searching for costumer by ID number
	 * the search stops if the instance was given back
	 * @param id search for the costumer ID number.
	 * @return the costumer instance with specified ID number, or "null" if no instance Number was found.
	 */
	public Kunde sucheKunde(int id) throws KundeExistiertNichtException{
		Kunde k = null;
		
		Iterator<Kunde> it = kundenListe.iterator();
		
		while(it.hasNext()){
			Kunde tmp = it.next();
			if(tmp.getId() == id){
				k = tmp;
				break;
			}
		}
		
		if(k == null){
			throw new KundeExistiertNichtException(id, "");
		}
		
		return k;

	}
	
	/**
	 * this method returns a copy of the current costumer list
	 * @return copy of costumer list.
	 */
	public Vector<Kunde> getKundenListe(){
		
		//creates a copy
		Vector<Kunde> kopie = new Vector<Kunde>();
		
		Iterator<Kunde> it = kundenListe.iterator();
		
		while(it.hasNext()){
			kopie.add(it.next());
		}
		
		return kopie;
		
	}	
	
	public List<WarenkorbArtikel> gibWarenkorb(Kunde kunde) {
		return kunde.getWarenkorb();
	}
	
	public WarenkorbArtikel gibWarenkorbArtikel(Kunde kunde, Artikel artikel) throws ArtikelExistiertNichtException {
		return warenkorbVerwaltung.getWarenkorbArtikel(kunde, artikel);
	}
	
	public void inDenWarenkorbLegen(Kunde kunde, WarenkorbArtikel warenkorbArtikel) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		warenkorbVerwaltung.hinzufuegen(kunde, warenkorbArtikel);
	}

	public void ausDemWarenkorbHerausnehmen(Kunde kunde, Artikel artikel) throws ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		warenkorbVerwaltung.entfernen(kunde, warenkorbVerwaltung.getWarenkorbArtikel(kunde, artikel));
	}
	
	public void stueckzahlAendern(Kunde kunde, WarenkorbArtikel warenkorbArtikel, int neueStueckzahl) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		warenkorbVerwaltung.stueckzahlAendern(kunde, warenkorbArtikel, neueStueckzahl);
	}
	
	public Rechnung kaufen(Kunde kunde) throws WarenkorbIstLeerException {
		return new Rechnung(kunde, new Date(), warenkorbVerwaltung.kaufen(kunde));
	}
	
	public void leeren(Kunde kunde) throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		warenkorbVerwaltung.leeren(kunde);
	}
	
}
