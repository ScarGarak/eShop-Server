package shop.server.domain;

/**
 * Diese Klasse dient zur Verwaltung der Ereignisse. Ein Ereignis entsteht bei einer
 * Aus- oder Einlagerung eines Artikels. Diese Verwaltung stellt mehrere Methoden zur
 * Verwaltung von der Logdatei zur Verfügung.
 * 
 *  @author Migliosi Angelo
 */

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import shop.common.valueobjects.Artikel;
import shop.common.valueobjects.Ereignis;
import shop.common.valueobjects.Mitarbeiter;
import shop.server.persistence.log.FileLogPersistenceManager;
import shop.server.persistence.log.LogPersistenceManager;

public class EreignisVerwaltung {
	
	private Vector<Ereignis> ereignisListe = new Vector<Ereignis>();
	private LogPersistenceManager lpm = new FileLogPersistenceManager();
	
	private Hashtable<Integer, Vector<String[]>> bestandsHistorieListe;	// Wenn man eine Artikel ID angibt, bekommt man dessen Bestandshistorie
	
	/**
	 * Diese Methode schreibt die Ereignisse in der Ereignisliste in die Logdatei
	 * 
	 * @see LogPersistenceManager
	 * @see FileLogPersistenceManager
	 * @param dateiname Name der Logdatei
	 * @throws IOException
	 */
	public void schreibeDaten(String dateiname) throws IOException{
		this.cleanLogdatei(dateiname);
		
		lpm.openForWriting(dateiname);

		Iterator<Ereignis> it = ereignisListe.iterator();

		while(it.hasNext()){
			Ereignis e = it.next();
			if(e.getAnzahl() < 0){
				lpm.speichereAuslagerung(e.getPerson(), Math.abs(e.getAnzahl()), e.getArtikel().getArtikelnummer(), e.getDatum());
			}else if (e.getAnzahl() > 0){
				lpm.speichereEinlagerung((Mitarbeiter)e.getPerson(), e.getAnzahl(), e.getArtikel().getArtikelnummer(), e.getDatum());
			}
		}

		lpm.close();
	}
	
	
	/**
	 * Diese Methode gibt die Bestandshistorie des angegebenen Artikels als ein einziger String zurueck.
	 * 
	 * Bevor sie dies tut, wird kontrolliert, ob die Bestandshistorie des angegebenen Artikels bereits
	 * gebildet wurde. Wenn nicht, ruft sie die Methode zum erstellen der Bestandshistorie auf.
	 * Anschließend iteriert sie durch die Bestandshistorie und fügt alles in einen String.
	 * 
	 * @see EreignisVerwaltung#erstelleBestandsHistorie(Artikel, String)
	 * @param artikel
	 * @param dateiname
	 * @return Bestandshistorie des angegebenen Artikels
	 * @throws IOException
	 */
	public String gibBestandsHistorie(Artikel artikel, String dateiname) throws IOException{
		int artikelID = artikel.getArtikelnummer();
		if(bestandsHistorieListe == null){
			bestandsHistorieListe = new Hashtable<Integer, Vector<String[]>>();
		}
		
		
		if(!bestandsHistorieListe.containsKey(artikelID)){
			erstelleBestandsHistorie(artikel, dateiname);
		}
		
		String result = "";
		
		// Die Bestandshistorie für den Artikel mit artikelID wurde bereits berechnet
		Iterator<String[]> it = bestandsHistorieListe.get(artikelID).iterator();
		while(it.hasNext()){
			String[] bestandHistorie = it.next();
			result += bestandHistorie[0]+"\t"+bestandHistorie[1]+"\n";
		}
		return result;
	}
	
	public int[] gibBestandsHistorieDaten(Artikel artikel, String dateiname) throws IOException{
		int artikelID = artikel.getArtikelnummer();
		if(bestandsHistorieListe == null){
			bestandsHistorieListe = new Hashtable<Integer, Vector<String[]>>();
		}
		
		
		if(!bestandsHistorieListe.containsKey(artikelID)){
			erstelleBestandsHistorie(artikel, dateiname);
		}
		
		int[] result = new int[30];
		
		// Die Bestandshistorie für den Artikel mit artikelID wurde bereits berechnet
		try{
			Iterator<String[]> it = bestandsHistorieListe.get(artikelID).iterator();
			for(int i = 0; i < result.length && it.hasNext(); i++){
				String[] bestandHistorie = it.next();
				result[i] = Integer.parseInt(bestandHistorie[1]);
			}
		}catch (NumberFormatException nfe){
			result = null;
		}
		
		return result;
	}

	
	/**
	 * Diese Methode erstellt die Bestandshistorie des angegebenen Artikels. 
	 * Sie realisiert dies indem sie durch die Logdatei geht und die Einträge des Artikels,
	 * die jünger als 30 Tage sind, entnimmt und mit Hilfe der Anzahl der Ein-/Auslagerungen
	 * und des Datums die Bestandsveraenderungen eines ganzen Tages rechnet.
	 * 
	 * Anschließend ruft sie noch die Methode zur Berechnung der Bestände mit Hilfe des
	 * aktuellen Artikelbestandes.
	 * 
	 * @see EreignisVerwaltung#rechneBestand(int, int)
	 * @see EreignisVerwaltung#istDatumGueltig(String)
	 * @param artikel
	 * @param dateiname
	 * @throws IOException
	 */
	private void erstelleBestandsHistorie(Artikel artikel, String dateiname) throws IOException {
		int artikelID = artikel.getArtikelnummer();
		lpm.openForReading(dateiname);
		
		// Bilde den Eintrag in der Hashtable
		Vector<String[]> bestandsHistorie = new Vector<String[]>();
		bestandsHistorieListe.put(artikelID, bestandsHistorie);
		
		String datum = "";
		String zeile = "";
		String[] tokens = null;

		// Gehe von Linie zur Linie, und kontrolliere, ob das Datum nicht älter als 30 Tage ist.
		// Wenn die Linie nicht älter als 30 Tage ist, gehe weiter.
		do{
			zeile = lpm.ladeEinAuslagerung();
			tokens = zeile.split(" ");
			if(tokens != null && tokens.length > 1){
				datum = tokens[0]+" "+tokens[1];
			}else{
				// Es sind keine Einträge in den letzten 30 Tagen vorhanden
				break;
			}
		}while(!istDatumGueltig(datum));

		String[] eintrag = new String[2];
		int bestandsVeraenderung = 0;
		boolean ersterDurchlauf = true;
		// Gehe von Zeile zur zeile, bis zum Schluss der Datei
		do{
			// Solange wie die eingelesene Linie nicht das Pattern 'Artikel artikelID'
			// beinhaltet, gehen wir zur nächsten Linie.
			while(!zeile.contains("Artikel "+artikelID) && !zeile.equals("")){
				zeile = lpm.ladeEinAuslagerung();
			}
			
			if(!zeile.equals("")){
				///////////////////////// Eintrag gefunden /////////////////////////

				// Speichere in newDatum das Datum (YYYY-MM-DD) der eingelesenen Zeile
				tokens = zeile.split(" ");
				String newDatum = tokens[0];


				// Neuer Tag
				if(!newDatum.equals(datum)){	
					// Das eingelesene Datum ist nicht das gleiche Datum wie die Zeile vorher
					// d.h. jetzt kommt eine Zeile mit Ein- oder Auslagerungen von einem
					// anderen Tag
					if(!ersterDurchlauf){ // Beim ersten Mal wird nicht abgespeichert
						// Die Summe der Bestands-Veraenderungen an diesem Tag und datum abspeichern
						eintrag[0] = datum;
						eintrag[1] = bestandsVeraenderung+"";
						bestandsHistorie.add(eintrag);
						
						// Die fehlenden Tage zwischen Datum und NewDatum hinzufügen
						fehlendeTageHinzufuegen(bestandsHistorie, datum, newDatum);
					}else{
						ersterDurchlauf = false;
						
					}
					
					// Das neue Datum wird in 'datum' gespeichert
					datum = newDatum;

					// Re-intialisierung der Variablen
					eintrag = new String[2];
					bestandsVeraenderung = 0;
					
				}


				// Entnehmen der Bestands-Veraenderung in der Zeile
				try{
					if(tokens[8].equals("eingelagert")){
						// Da wir den Bestand vom aktuellem Bestand zurueck rechnen,
						// müssen wir die anzahl der Einlagerung wieder subtrahieren
						bestandsVeraenderung -= Integer.parseInt(tokens[4]);
					}else{
						bestandsVeraenderung += Integer.parseInt(tokens[4]);
					}
				}catch (NumberFormatException e){
					System.err.println();
				}

				zeile = lpm.ladeEinAuslagerung();
			}
		}while(!zeile.equals(""));
		
		
		int bestand = artikel.getBestand();
		String heute = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		
		// Wenn das Datum der letzten Zeile das heutige Datum ist, wird es nicht abgespeichert
		if(!heute.equals(datum)){
			// Abspeicheren der zwischengelagerten Daten:
			eintrag[0] = datum;
			eintrag[1] = bestandsVeraenderung+"";
			bestandsHistorie.add(eintrag);
			
			// Fuege noch die Tage bis heute hinzu
			fehlendeTageHinzufuegen(bestandsHistorie, datum, heute);
		}else{
			// Um die Bestandshistorie zurueck rechnen zu können
			// müssen die Ein- und Auslagerungen von Heute jedoch
			// beachtet werden
			bestand += bestandsVeraenderung;
		}
		
		// Jetzt müssen nur noch die Ereignisse in der Ereignissliste
		// überprüft werden, da diese noch nich in der Log Datei
		// zu finden sind
		
		if(!ereignisListe.isEmpty()){
			Iterator<Ereignis> iter = ereignisListe.iterator();
			while(iter.hasNext()){
				Ereignis e = iter.next();
				if(e.getArtikel().equals(artikel)){
					bestand += -e.getAnzahl();
				}
			}
		}
		
		lpm.close();
		
		// Jetzt wird noch die 'Vorgeschichte' des Artikels hinzugefuegt! Dies ist nötig, um 
		// 30 Eintraege in der Bestandshistorie zu haben, auch wenn das Artikel noch nicht
		// solange im Bestand ist, oder es Bestandsveraenderungen gab, die aelter als 30 Tage sind.
		if(bestandsHistorie.size() < 30)
			fuegeVorgeschichteHinzu(bestandsHistorie);
		
		// Jetzt wird das Feld mit der Bestandsveraenderung, durch
		// den Bestand, der am Ende des jeweiligen Tages vorliegte,
		// ersetzt
		rechneBestand(artikel.getArtikelnummer(), bestand);
	}
	
	private void fuegeVorgeschichteHinzu(Vector<String[]> bestandsHistorie){
		try {
			Calendar datum = Calendar.getInstance();
			datum.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(bestandsHistorie.get(0)[0]));
			
			while(bestandsHistorie.size() < 30){
				datum.add(Calendar.DAY_OF_MONTH, -1);
				String[] eintrag = new String[2];
				eintrag[0] = new SimpleDateFormat("yyyy-MM-dd").format(datum.getTime());
				eintrag[1] = 0+"";
				bestandsHistorie.add(0, eintrag);
			}
		} catch (ParseException e) {
			System.err.println("Fehler beim parsen des Datums! - in fuegeVorgeschichteHinzu()");
			System.err.println(e.getMessage());
		}
	}
	
	private void fehlendeTageHinzufuegen(Vector<String[]> bestandsHistorie, String oldDatum, String newDatum){
		try {
			// Gib die Calendar Instanz
			Calendar altesDatum = Calendar.getInstance();
			Calendar neuesDatum = Calendar.getInstance();
			
			// Setze die Daten
			altesDatum.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(oldDatum));
			neuesDatum.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(newDatum));
			
			// Da es sich um das angegebene 'oldDatum' um das Datum handelt, was zuletzt zur Bestandshistorie
			// hinzugefügt wurde, muss es zuerst noch inkrementiert werden, bevor wir in die Schleife gehen
			altesDatum.add(Calendar.DAY_OF_MONTH, 1);
			
			// Wenn newDatum heute ist wird es dekrementiert, da Heute nicht in der Bestandshistorie angezeigt
			// werden soll.
			if(new SimpleDateFormat("yyyy-MM-dd").format(new Date()).equals(newDatum)){
				neuesDatum.add(Calendar.DAY_OF_MONTH, -1);
			}
			
			if(!neuesDatum.before(altesDatum)){
				while(!altesDatum.equals(neuesDatum)){
					String tmpDatum = new SimpleDateFormat("yyyy-MM-dd").format(altesDatum.getTime());
					String eintrag[] = new String[2];
					eintrag[0] = tmpDatum;
					eintrag[1] = 0+"";
					bestandsHistorie.add(eintrag);
					altesDatum.add(Calendar.DAY_OF_MONTH, 1);
				}
			}
			
		} catch (ParseException e) {
			System.err.println("Fehler beim parsen des Datums!");
			System.err.println("Fehler ausgelöst durch: '"+oldDatum+"' oder '"+newDatum+"'!");
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Diese Methode berechnet den Bestand am Ende des Tages, der letzten 30 Tage.
	 * Dies tut sie indem sie vom aktuellen Bestand des Artikels zurueck rechnet.
	 * 
	 * @param artikelID
	 * @param bestand Bestand wie er am Anfang des heutigen Tages, bzw am Ende des gestrigen Tages war.
	 */
	private void rechneBestand(int artikelID, int bestand){
		Vector<String[]> bestandsHistorie = bestandsHistorieListe.get(artikelID);
		ListIterator<String[]> lIter = bestandsHistorie.listIterator(bestandsHistorie.size());
		
		while(lIter.hasPrevious()){
			String[] eintrag = lIter.previous();
			int bestandsVeraenderung = Integer.parseInt(eintrag[1]);
			eintrag[1] = bestand+"";
			bestand += bestandsVeraenderung;
		}
	}
	
	/**
	 * Diese Methode kontrolliert, ob das angegebene Datum (im Format "yyyy-MM-dd HH:mm:ss") nicht
	 * älter als 30 Tage ist.
	 * 
	 * @see Calendar
	 * @param datum Datum im Format "yyyy-MM-dd HH:mm:ss".
	 * @return true wenn das angegebene Datum nicht älter als 30 Tage ist
	 */
	private boolean istDatumGueltig(String datum){
		boolean gueltig = false;
		try {
			// Gib die Calendar Instanz
			Calendar aktuellesDatum = Calendar.getInstance();
			Calendar eintragDatum = Calendar.getInstance();
			
			// Setze das aktuelle Datum
			aktuellesDatum.setTime(new Date());
			// Parse die Parametervariable zum Typ 'Date' und setze diese dann als Calendar Datum
			eintragDatum.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(datum));
			
			// Gehe vom aktuellem Datum 30 Tage zurueck
			aktuellesDatum.add(Calendar.DAY_OF_MONTH, -30);
			
			// Wenn das (verringerte) aktuelleDatum vor der Zeit des Eintrag-Datums liegt, ist der Eintrag gueltig
			if(aktuellesDatum.before(eintragDatum)){
				gueltig = true;
			}
		} catch (ParseException e) {
			System.err.println("Fehler beim parsen des Datums!");
			System.err.println("Fehler ausgelöst durch: '"+datum+"'");
			System.err.println(e.getMessage());
		}
		return gueltig;
	}
	
	/**
	 * Diese Methode löscht alle Zeilen aus der Logdatei, die älter als 30 Tage sind.
	 * Dies realisiert sie indem sie durch die Logdatei geht und die erste Zeile, die
	 * jünger als 30 Tage ist, der LogPersistenceManager-internen-Methode übergibt.
	 * 
	 * @see LogPersistenceManager#cleanLogdatei(String)
	 * @see FileLogPersistenceManager#cleanLogdatei(String)
	 * @param dateiname Name der Logdatei
	 * @throws IOException
	 */
	private void cleanLogdatei(String dateiname) throws IOException{
		lpm.openForReading(dateiname);
		String zeile = "";
		
		// Suche nachd er ersten Zeile die nicht älter als 30 Tage ist
		while(!(zeile = lpm.ladeEinAuslagerung()).equals("")){
			String[] tokens = zeile.split(" ");
			if(istDatumGueltig(tokens[0]+" "+tokens[1])){
				break;
			}
		}
		
		lpm.close();
		
		// Löschen aller Zeilen, die sich vor der gefundenen Zeile befinden
		lpm.cleanLogdatei(zeile, dateiname);
	}
	
	/**
	 * Dies Methode fuegt ein Ereignis zur Ereignisliste hinzu.
	 * 
	 * @param e
	 */
	public void hinzufuegen(Ereignis e){
		ereignisListe.add(e);
	}
	
	public String liesLogDatei(String dateiname) throws IOException{
		String log = "";		
		lpm.openForReading(dateiname);
		
		String zeile = lpm.ladeEinAuslagerung();
		while(!zeile.equals("")){
			log += zeile+"\n";
			zeile = lpm.ladeEinAuslagerung();
		}
		
		lpm.close();
		return log;
	}
	
	/**
	 * Diese Methode gibt die Ereignisliste zurueck.
	 * @return Ereignisliste
	 */
	public Vector<Ereignis> getEreignisListe(){
		return ereignisListe;
	}

}
