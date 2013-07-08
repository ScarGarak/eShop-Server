package shop.server.persistence.data;

import java.io.IOException;

import shop.common.valueobjects.Artikel;
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.Mitarbeiter;

/**
 * 
 * @author Oliver Thummerer
 * 
 * Interface fuer den Zugriff auf ein Speichermedium
 * zum Ablegen von Kunden-, Artikel- und Mitarbeiterdaten.
 * 
 * Das Interface muss von Klassen implementiert werden wenn eine
 * Persistez-Schnittstelle realisiert werden soll 
 */
public interface DataPersistenceManager {
	
	/**
	 * Methoden zum oeffnen und schlieszen einer externen Datenquelle
	 * 
	 * @param datenquelle
	 * @throws IOException
	 */
	public void openForReading(String datenquelle) throws IOException;
	
	public void openForWriting(String datenquelle) throws IOException;
	
	public boolean close();
	
	/**
	 * Methode zum Einlesen der Artikeldaten aus einer externen Datenquelle.
	 * 
	 * @return Artikel-Objekt, wenn Einlesen erfolgreich, false null
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public Artikel ladeArtikel() throws IOException;

	/**
	 * Methode zum Schreiben der Buchdaten in eine externe Datenquelle.
	 * 
	 * @param a Artikel-Objekt, das gespeichert werden soll
	 * @throws IOException
	 */
	public void speichereArtikel(Artikel a) throws IOException;
	
	/**
	 * Methode zum laden Mitarbeiter Daten aus einer Datei
	 * @return Ein Mitarbeiter-Objekt
	 * @throws IOException
	 */
	public Mitarbeiter ladeMitarbeiter() throws IOException;
	
	/**
	 * Methode zum schreiben der Mitarbeiterdaten in eine externe Datei
	 * @param m Ein Mitarbeiter-Objekt zu speichern
	 * @throws IOException
	 */
	public void speichereMitarbeiter(Mitarbeiter m) throws IOException;
	
	/**
	 * Methode zum  Einlesen der Kundendaten aus einer externen Datenquelle.
	 * 
	 * @return Kunde-Objekt, wenn einlesen erfolgreich, false null
	 * @throws IOException
	 */
	public Kunde ladeKunden() throws IOException;
	
	/**
	 * Methode zum schreiben der Kundendaten in eine externe Datenquelle.
	 * 
	 * @param k Kunde-Objekt, das gespeichert werden soll
	 * @throws IOException
	 */
	public void speichereKunden(Kunde k) throws IOException;
	
}
