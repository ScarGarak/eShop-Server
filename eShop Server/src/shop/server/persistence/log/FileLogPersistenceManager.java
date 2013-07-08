package shop.server.persistence.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.Person;
import shop.common.valueobjects.PersonTyp;
/**
 * @author Christof Ferreira Torres
 * 
 * Schnittstelle zur persistenten Speicherung von
 * Ein- und AuslagerungsDaten in .log Dateien
 * @see LogPersistenceManager
 */
public class FileLogPersistenceManager implements LogPersistenceManager {
	
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	
	@Override
	public void openForReading(String datei) throws FileNotFoundException {
		reader = new BufferedReader(new FileReader(datei));
	}
	
	@Override
	public void openForWriting(String datei) throws IOException {
		writer = new PrintWriter(new BufferedWriter(new FileWriter(datei, true)));
	}
	
	@Override
	public boolean close() {
		if (writer != null)
			writer.close();
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	@Override
	public String ladeEinAuslagerung() throws IOException {
		String zeile = liesZeile();
		if(zeile == null){
			zeile = "";
		}
		return zeile;
	}

	@Override
	public void speichereEinlagerung(Mitarbeiter m, int anzahl, int artikelnummer, Date datum) throws IOException {
		schreibeZeile(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(datum) + " Mitarbeiter " + m.getId() + " " + anzahl + " Stueck Artikel " + artikelnummer + " eingelagert");
	}
	
	@Override
	public void speichereAuslagerung(Person p, int anzahl, int artikelnummer, Date datum) throws IOException {
		String personTyp = p.getPersonTyp()+"";
		String auslagerungsTyp = "";
		if(p.getPersonTyp() == PersonTyp.Mitarbeiter){
			auslagerungsTyp = "ausgelagert";
		}else{
			auslagerungsTyp = "verkauft";
		}
		schreibeZeile(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(datum) + " " + personTyp+ " " + p.getId() + " " + anzahl + " Stueck Artikel " + artikelnummer + " "+auslagerungsTyp);
	}
	
	/**
	 * Diese Methode liest die aktuelle Logdatei ab der angegebenen Zeile und fuegt 
	 * dann jede Zeile in eine temporaer erstellte Datei.
	 * Zum Schluss wird die Logdatei geloescht und die temporaere Datei nimmt den Namen
	 * der Logdatei ein.
	 * Somit werden alle Eintraege vor der angegebenen Zeile geloescht.
	 */
	@Override
	public boolean cleanLogdatei(String zeile, String dateiname) throws IOException{
		try {

			File original = new File(dateiname);
			File tmp = new File(original.getAbsolutePath() + ".tmp");

			// eigenes Bilden von Reader und Writer, da hier File Objekte uebergeben werden,
			// die wir zum Loeschen, respkt. umbenennen der Dateien brauchen
			reader = new BufferedReader(new FileReader(original));
			writer = new PrintWriter(new FileWriter(tmp));

			String str = null;

			// Gehe von Zeile zur Zeile bis die eingelesene Zeile leer ist
			// oder der variablen "zeile" entspricht
			while ((str = reader.readLine()) != null && !str.equals(zeile)); 

			if(str != null){
				// Schreibe den String in die temporaere Datei
				do{
					writer.println(str);
					writer.flush();
				}while ((str = reader.readLine()) != null);
			}

			close();

			//Loesche das Original
			if (!original.delete()) {
				System.err.println("Loeschen des originals fehlgeschlagen!");
				return false;
			}

			//Gib der temporaeren Datei, den Namen des originals
			if (!tmp.renameTo(original))
				System.err.println("Umbenennen der temporaeren Datei fehlgeschlagen!");

		} catch (FileNotFoundException e) {
			System.err.println("Datei nicht gefunden!");
			System.err.println(e.getMessage());
		}
		
		return true;
	}
	
	/**
	 * Diese Methode liest die aktuelle Logdatei und fuegt jede Zeile in eine temporaer
	 * erstellte Datei. Jede Zeile die die angegebene ID enthaelt wird dabei ignoriert.
	 * Somit wird ein entferntes Artikel komplett aus der Logdatei entfernt.
	 * Dies ist notwendig, um die Bestandshistorie eines spaeter hinzugefuegten Artikels,
	 * mit der gleichen ID, koherent zu halten.
	 * Zum Schluss wird die Logdatei geloescht und die temporaere Datei nimmt den Namen
	 * der Logdatei ein.
	 */
	@Override
	public boolean entferneArtikelAusLog(String id, String dateiname) throws IOException {
		try {

			File original = new File(dateiname);
			File tmp = new File(original.getAbsolutePath() + ".tmp");

			// eigenes Bilden von Reader und Writer, da hier File Objekte uebergeben werden,
			// die wir zum Loeschen, respkt. umbenennen der Dateien brauchen
			reader = new BufferedReader(new FileReader(original));
			writer = new PrintWriter(new FileWriter(tmp));

			String str = null;

			// Gehe von Zeile zur Zeile bis die eingelesene Zeile leer ist
			// und schreibe nur die Zeilen, die nicht die ID beherbergen
			while ((str = reader.readLine()) != null){ 
				if(!str.contains(id)){
					// Schreibe den String in die temporaere Datei
					writer.println(str);
					writer.flush();
				}
			}
			close();

			//Loesche das Original
			if (!original.delete()) {
				System.err.println("Loeschen des originals fehlgeschlagen!");
				return false;
			}

			//Gib der temporaeren Datei, den Namen des originals
			if (!tmp.renameTo(original))
				System.err.println("Umbenennen der temporaeren Datei fehlgeschlagen!");

		} catch (FileNotFoundException e) {
			System.err.println("Datei nicht gefunden!");
			System.err.println(e.getMessage());
		}
		
		return true;
	}
	
	
	/*
	 * Hilfsmethoden zum lesen bzw. schreiben einer Zeile
	 */
	private String liesZeile() throws IOException {
		if (reader != null)
			return reader.readLine();
		else
			return "";
	}

	public void schreibeZeile(String daten) {
		if (writer != null)
			writer.println(daten);
	}
	
	
}
