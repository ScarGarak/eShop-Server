package shop.server.domain;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import shop.common.exceptions.ArtikelBestandIstKeineVielfacheDerPackungsgroesseException;
import shop.common.exceptions.ArtikelExistiertBereitsException;
import shop.common.exceptions.ArtikelExistiertNichtException;
import shop.common.valueobjects.Artikel;
import shop.server.persistence.data.DataPersistenceManager;
import shop.server.persistence.data.ObjectDataPersistenceManager;

/**
 * Klasse zur Verwaltung von Artikeln.
 * 
 * @author Christof Ferreira Torres
 */
public class ArtikelVerwaltung {

	// Verwaltung des Artikelbestands in einem Vector
	private List<Artikel> artikelBestand = new Vector<Artikel>();
	// Persistenz-Schnittstelle, die f�r die Details des Dateizugriffs verantwortlich ist
	private DataPersistenceManager pm = new ObjectDataPersistenceManager();
	
	/**
	 * Methode zum Einlesen von Artikeldaten aus einer Datei.
	 * 
	 * @param datei Datei die den einzulesenden Artikelbestand enth�lt.
	 * @throws IOException
	 * @throws ArtikelExistiertBereitsException 
	 * @throws ClassNotFoundException 
	 */
	public void liesDaten(String datei) throws IOException {
		// PersistenzManager f�r Lesevorg�nge �ffnen
		pm.openForReading(datei);

		Artikel einArtikel;
		do {
			// Artikel-Objekt einlesen
			einArtikel = pm.ladeArtikel();
			if (einArtikel != null) {
				// Artikel in die Liste einf�gen
				try {
					einfuegen(einArtikel);
				} catch (ArtikelExistiertBereitsException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		} while (einArtikel != null);

		// Persistenz-Schnittstelle wieder schlie�en
		pm.close();
	}
	
	/**
	 * Methode zum Schreiben der Artikeldaten in eine Datei.
	 * 
	 * @param datei Datei, in die der Artikelbestand geschrieben werden soll
	 * @throws IOException
	 */
	public synchronized void schreibeDaten(String datei) throws IOException  {
		// PersistenzManager f�r Schreibvorg�nge �ffnen
		pm.openForWriting(datei);

		if (!artikelBestand.isEmpty()) {
			Iterator<Artikel> iter = artikelBestand.iterator();
			while (iter.hasNext()) {
				pm.speichereArtikel(iter.next());				
			}
		}			
		
		// Persistenz-Schnittstelle wieder schlie�en
		pm.close();
	}
	
	/**
	 * Methode zum einf�gen eines Artikels in den Bestand.
	 * 
	 * @param artikel
	 * @throws ArtikelExistiertBereitsException
	 */
	public void einfuegen(Artikel artikel) throws ArtikelExistiertBereitsException {
		if (!artikelBestand.contains(artikel))
			artikelBestand.add(artikel);
		else
			throw new ArtikelExistiertBereitsException(artikel, " - in 'einfuegen()'");
	}
	
	/**
	 * Methode zum ver�ndern des Bestands eines Artikels.
	 * 
	 * @param artikelnummer
	 * @param anzahl
	 * @throws ArtikelExistiertNichtException
	 * @throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException
	 */
	public synchronized void bestandVeraendern(int artikelnummer, int anzahl) throws ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		int index = -1;

		Iterator<Artikel> iter = artikelBestand.iterator();
		while (iter.hasNext()) {
			Artikel artikel = iter.next();
			if (artikel.getArtikelnummer() == artikelnummer)
				index = artikelBestand.indexOf(artikel);
		}
		
		if (index != -1) {
			artikelBestand.get(index).setBestand(artikelBestand.get(index).getBestand() + anzahl);
		} else 
			throw new ArtikelExistiertNichtException(artikelnummer, " - in 'bestandErhoehen()'");
	}
	
	/**
	 * Methode die anhand der Artikelnummer einen Artikel zur�ckgibt.
	 * 
	 * @param artikelnummer
	 * @return Artikel
	 * @throws ArtikelExistiertNichtException
	 */
	public Artikel getArtikel(int artikelnummer) throws ArtikelExistiertNichtException {
		Iterator<Artikel> iter = artikelBestand.iterator();
		while (iter.hasNext()) {
			Artikel artikel = iter.next();
			if (artikel.getArtikelnummer() == artikelnummer) {
				return artikel;
			}
		}
		throw new ArtikelExistiertNichtException(artikelnummer, " - in 'getArtikel()'");
	}
	
	/**
	 * Methode um Artikel mit einer bestimmten Artikelnummer im Bestand zu suchen. Es wird
	 * eine Liste von Artikeln zur�ckgegeben.
	 * 
	 * @param artikelnummer
	 * @return List<Artikel>
	 */
	public List<Artikel> sucheArtikel(int artikelnummer) {
		List<Artikel> ergebnis = new Vector<Artikel>();
		
		Iterator<Artikel> iter = artikelBestand.iterator();
		while (iter.hasNext()) {
			Artikel artikel = iter.next();
			if (artikel.getArtikelnummer() == artikelnummer) {
				ergebnis.add(artikel);
			}
		}
		
		Collections.sort(ergebnis, new SortierungNachArtikelnummer());
		return ergebnis;
	}
	
	/**
	 * Methode um Artikel mit einer bestimmten Bezeichnug im Bestand zu suchen. Es wird
	 * eine Liste von Artikeln zur�ckgegeben.
	 * 
	 * @param bezeichnung
	 * @return List<Artikel>
	 */
	public List<Artikel> sucheArtikel(String bezeichnung) {
		List<Artikel> ergebnis = new Vector<Artikel>();
		
		Iterator<Artikel> iter = artikelBestand.iterator();
		while (iter.hasNext()) {
			Artikel artikel = iter.next();
			if (artikel.getBezeichnung().toLowerCase().contains(bezeichnung.toLowerCase())) {
				ergebnis.add(artikel);
			}
		}
		
		Collections.sort(ergebnis, new SortierungNachBezeichnung());
		return ergebnis;
	}
	
	/**
	 * Methode um einen Artikel aus dem Bestand zu entfernen.
	 * 
	 * @param artikelnummer
	 * @throws ArtikelExistiertNichtException
	 */
	public synchronized void entfernen(int artikelnummer) throws ArtikelExistiertNichtException {
		int index = -1;

		Iterator<Artikel> iter = artikelBestand.iterator();
		while (iter.hasNext()) {
			Artikel artikel = iter.next();
			if (artikel.getArtikelnummer() == artikelnummer)
				index = artikelBestand.indexOf(artikel);
		}
		
		if (index != -1)
			artikelBestand.remove(index);
		else
			throw new ArtikelExistiertNichtException(artikelnummer, " - in 'entfernen()'");
	}
	
	/**
	 * Methode die den Artikelbestand zur�ckgibt.
	 * 
	 * @return List<Artikel>
	 */
	public List<Artikel> getArtikelBestand() {
		List<Artikel> ergebnis = new Vector<Artikel>();
		ergebnis.addAll(artikelBestand);
		return ergebnis;
	}
	
	/**
	 * Methode die den Artikelbestand sortiert nach Artikelnummer zur�ck gibt.
	 * 
	 * @return List<Artikel>
	 */
	public List<Artikel> getArtikelBestandSortiertNachArtikelnummer() {
		List<Artikel> ergebnis = new Vector<Artikel>();
		ergebnis.addAll(artikelBestand);
		Collections.sort(ergebnis, new SortierungNachArtikelnummer());
		return ergebnis;
	}
	
	/**
	 * Methode die den Artikelbestand sortiert nach Bezeichnung zur�ck gibt.
	 * 
	 * @return
	 */
	public List<Artikel> getArtikelBestandSortiertNachBezeichnung() {
		List<Artikel> ergebnis = new Vector<Artikel>();
		ergebnis.addAll(artikelBestand);
		Collections.sort(ergebnis, new SortierungNachBezeichnung());
		return ergebnis;
	}
	
	/**
	 * Methode zum bearbeiten eines Artikels.
	 * 
	 * @param artikelnummer
	 * @param preis
	 * @param bezeichnung
	 * @throws ArtikelExistiertNichtException
	 */
	public synchronized void bearbeiten(int artikelnummer, double preis, String bezeichnung) throws ArtikelExistiertNichtException{
		Artikel artikel = getArtikel(artikelnummer);
		artikel.setPreis(preis);
		artikel.setBezeichnung(bezeichnung);
	}
	
}
