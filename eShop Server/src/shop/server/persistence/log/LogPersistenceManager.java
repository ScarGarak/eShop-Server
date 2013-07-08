package shop.server.persistence.log;

import java.io.IOException;
import java.util.Date;

import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.Person;

/**
 * 
 * @author Christof Ferreira Torres
 * 
 * Interface für den Zugriff auf ein Speichermedium
 * zum Ablegen von Ein- und Auslagerungsdaten.
 * 
 * Das Interface muss von Klassen implementiert werden wenn eine
 * Persistez-Schnittstelle realisiert werden soll 
 */
public interface LogPersistenceManager {

	
	/**
	 * Methoden zum öffnen und schließen einer externen Datenquelle
	 * 
	 * @param datenquelle
	 * @throws IOException
	 */
	public void openForReading(String datenquelle) throws IOException;
	
	public void openForWriting(String datenquelle) throws IOException;
	
	public boolean close();
	
	
	public String ladeEinAuslagerung() throws IOException;

	public void speichereEinlagerung(Mitarbeiter m, int anzahl, int artikelnummer, Date datum) throws IOException;
	
	public void speichereAuslagerung(Person p, int anzahl, int artikelnummer, Date datum) throws IOException;
	
	/**
	 * Diese Methode loescht alles in der angegebenen Datei was sich vor der angegebenen Zeile befindet.
	 * @param zeile Die Zeile ab welcher alles behalten wird. Die Zeile selbst wird auch behalten.
	 * @param dateiname Der Name der Datei.
	 * @return true, wenn kein Fehler auftrat
	 * @throws IOException
	 */
	public boolean cleanLogdatei(String zeile, String dateiname) throws IOException;
	
	/**
	 * Diese Methode entfernt alle Eintraege, die die angegebene id enthalten.
	 * @param id Die id, dessen Eintraege zu entfernen sind
	 * @param dateiname Der Name der Datei
	 * @return true, wenn kein Fehler auftrat
	 * @throws IOException
	 */
	public boolean entferneArtikelAusLog(String id, String dateiname) throws IOException;
	
}
