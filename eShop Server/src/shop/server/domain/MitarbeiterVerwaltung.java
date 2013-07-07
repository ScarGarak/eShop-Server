package shop.server.domain;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import shop.common.exceptions.MitarbeiterExistiertBereitsException;
import shop.common.exceptions.MitarbeiterExistiertNichtException;
import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.MitarbeiterFunktion;
import shop.server.persistence.data.DataPersistenceManager;
import shop.server.persistence.data.ObjectDataPersistenceManager;

/**
 * Diese Klasse verwaltet eine Mitarbeiterliste. Sie ermöglicht
 * es Mitarbeiter zu suchen, hinzuzufügen und zu löschen.
 * 
 * @author Angelo
 * @version 1
 * 
 * Zuletzt editiert: 11.05.2013
 */

public class MitarbeiterVerwaltung {
	
	private Vector<Mitarbeiter> mitarbeiterListe = new Vector<Mitarbeiter>();
	
	private DataPersistenceManager pm = new ObjectDataPersistenceManager();
	
	/**
	 * Methode zum lesen der Mitarbeiterdaten aus einer externen Datenquelle
	 * @param dateiName Dateiname der externen Datenquelle
	 * @throws IOException
	 */
	public void liesDaten(String dateiName) throws IOException{
		pm.openForReading(dateiName);
		
		Mitarbeiter m;
		
		do{
			m = pm.ladeMitarbeiter();
			if(m != null){
				try{
					einfuegen(m);
				}catch(MitarbeiterExistiertBereitsException e){
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}while(m != null);
		
		pm.close();
	}
	
	/**
	 * Methode zum schreiben der Mitarbeiterdaten in eine externe Datenquelle
	 * @param dateiName	Dateiname der externen Datenquelle
	 * @throws IOException
	 */
	public synchronized void schreibeDaten(String dateiName) throws IOException{
		pm.openForWriting(dateiName);
		
		if(mitarbeiterListe != null){
			Iterator<Mitarbeiter> it = mitarbeiterListe.iterator();

			while(it.hasNext()){
				pm.speichereMitarbeiter(it.next());
			}
		}
		pm.close();
	}
	
	/**
	 * Diese Methode dient zum einfuegen von Mitarbeitern in die Mitarbeiter-Liste.
	 * @param m Die Mitarbeiter-Instanz die zur Liste hinzugefügt werden soll.
	 * @throws MitarbeiterExistiertBereitsException	Wenn die ID der hinzuzufügenden Mitarbeiter-Instanz schon einmal in der Liste existiert. 
	 */
	public void einfuegen(Mitarbeiter m) throws MitarbeiterExistiertBereitsException{
		if(!mitarbeiterListe.contains(m)){
			mitarbeiterListe.add(m);
		}else{
			throw new MitarbeiterExistiertBereitsException(m, "Fehler beim einfuegen!");
		}
	}
	
	/**
	 * Diese Methode löscht die angegebene Mitarbeiter Instanz aus der Liste.
	 * @param m Mitarbeiter Instanz zum löschen
	 */
	public void loeschen(Mitarbeiter m){
		this.mitarbeiterListe.remove(m);
	}
	
	/**
	 * Diese Methode sucht ein Mitarbeiter mittels einer angegebenen ID Nummer.
	 * @param id ID Nummer des zu suchenden Mitarbeiters.
	 * @return Die Mitarbeiter-Instanz mit der angegebenen ID Nummer, oder "null" wenn keine Instanz gefunden wurde.
	 */
	public Mitarbeiter sucheMitarbeiter(int id) throws MitarbeiterExistiertNichtException{
		Mitarbeiter m = null;
		
		Iterator<Mitarbeiter> it = mitarbeiterListe.iterator();
		
		while(it.hasNext()){
			Mitarbeiter tmp = it.next();
			if(tmp.getId() == id){
				m = tmp;
				break;
			}
		}
		
		if(m == null){
			throw new MitarbeiterExistiertNichtException(id, "");
		}
		return m;
	}
	
	/**
	 * Diese Methode gibt eine Kopie der aktuellen Mitarbeiterliste zurück.
	 * @return Kopie der Mitarbeiterliste.
	 */
	public Vector<Mitarbeiter> getMitarbeiterListe(){
		
		//Erschaffen einer kopie
		Vector<Mitarbeiter> kopie = new Vector<Mitarbeiter>();
		kopie.addAll(mitarbeiterListe);
		
		return kopie;
		
	}

	/**
	 * Diese Methode wird zum bearbeiten einer Mitarbeiterinstanz genutzt.
	 * @param id Die ID des Mitarbeiters
	 * @param passwort	Das Passwort des Mitarbeiters
	 * @param name Der Name des Mitarbeiters
	 * @param funktion Die Funktion des Mitarbeiters
	 * @param gehalt Der Gehalt des Mitarbeiters
	 * @param blockiert Ob der Mitarbeiter blockiert ist oder nicht
	 * @throws MitarbeiterExistiertNichtException 
	 */
	public synchronized void bearbeiten(int id, String passwort, String name, MitarbeiterFunktion funktion, double gehalt, boolean blockiert) throws MitarbeiterExistiertNichtException{
		Mitarbeiter m = sucheMitarbeiter(id);
		m.setPasswort(passwort);
		m.setFunktion(funktion);
		m.setGehalt(gehalt);
		m.setBlockiert(blockiert);
	}
	
}
