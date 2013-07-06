package shop.server.domain;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import shop.common.exceptions.ArtikelBestandIstKeineVielfacheDerPackungsgroesseException;
import shop.common.exceptions.ArtikelBestandIstZuKleinException;
import shop.common.exceptions.ArtikelExistiertBereitsException;
import shop.common.exceptions.ArtikelExistiertNichtException;
import shop.common.exceptions.KundeExistiertBereitsException;
import shop.common.exceptions.KundeExistiertNichtException;
import shop.common.exceptions.MitarbeiterExistiertBereitsException;
import shop.common.exceptions.MitarbeiterExistiertNichtException;
import shop.common.exceptions.UsernameExistiertBereitsException;
import shop.common.exceptions.WarenkorbIstLeerException;
import shop.common.interfaces.ShopInterface;
import shop.common.valueobjects.Artikel;
import shop.common.valueobjects.Ereignis;
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.Massengutartikel;
import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.MitarbeiterFunktion;
import shop.common.valueobjects.Person;
import shop.common.valueobjects.Rechnung;
import shop.common.valueobjects.WarenkorbArtikel;

/**
 * Klasse zur Verwaltung vom eShop.
 * 
 * @author Christof Ferreira Torres, Angelo Migliosi & Oliver Thummerer
 * @version 1.0.0
 */
public class ShopVerwaltung implements ShopInterface{
	
	private String artikelDateiname = "eShop Server/SHOP_A.ser";
	private String mitarbeiterDateiname = "eShop Server/SHOP_M.ser";
	private String kundenDateiname = "eShop Server/SHOP_K.ser";
	private String logDateiname = "eShop Server/EinAuslagerung.log";

	private ArtikelVerwaltung meineArtikel;
	private MitarbeiterVerwaltung meineMitarbeiter;
	private KundenVerwaltung meineKunden;
	private EreignisVerwaltung meineEreignisse;
	
	private int mitarbeiterNextId;
	private int kundenNextId;
	
	/**
	 * Konstruktor, der die Basisdaten (Artikel, Mitarbeiter, Kunden) aus Dateien einliest
	 * (Initialisierung des Shops).
	 * 
	 * Namensmuster fŸr Dateien:
	 *   "SHOP_A.ser" ist die Datei der Artikel
	 *   "SHOP_M.ser" ist die Datei der Mitarbeiter
	 *   "SHOP_K.ser" ist die Datei der Kunden
	 *   
	 * @throws IOException, z.B. wenn eine der Dateien nicht existiert.
	 * @throws ArtikelExistiertBereitsException 
	 * @throws ClassNotFoundException 
	 */
	public ShopVerwaltung() throws IOException {
		meineArtikel = new ArtikelVerwaltung();
		//meineArtikel.liesDaten(artikelDateiname);
		try {
			meineArtikel.einfuegen(new Artikel(10010, "Apfel", 1.50, 35));
			meineArtikel.einfuegen(new Artikel(10011, "Banane", 1.60, 8));
			meineArtikel.einfuegen(new Massengutartikel(10012, "Dosenbier", 2.00, 6, 48));
			meineArtikel.einfuegen(new Artikel(10013, "Klavier", 7000, 7));
			meineArtikel.einfuegen(new Massengutartikel(10014, "Joghurt", 1.20, 4, 24));
		} catch (ArtikelExistiertBereitsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		meineArtikel.schreibeDaten(artikelDateiname);
		
		meineMitarbeiter = new MitarbeiterVerwaltung();
		meineMitarbeiter.liesDaten(mitarbeiterDateiname);
		mitarbeiterNextId = meineMitarbeiter.getMitarbeiterListe().get(meineMitarbeiter.getMitarbeiterListe().size()-1).getId() + 1;
		
		meineKunden = new KundenVerwaltung();
		meineKunden.liesDaten(kundenDateiname);
		kundenNextId = meineKunden.getKundenListe().get(meineKunden.getKundenListe().size()-1).getId() + 1;
		
		meineEreignisse = new EreignisVerwaltung();
	}
	
	// Artikel Methoden
	
	public void fuegeArtikelEin(Mitarbeiter mitarbeiter, int artikelnummer, String bezeichnung, double preis, int bestand) throws ArtikelExistiertBereitsException {
		Artikel artikel = new Artikel(artikelnummer, bezeichnung, preis, bestand);
		meineArtikel.einfuegen(artikel);
		
		meineEreignisse.hinzufuegen(new Ereignis(new Date(), artikel, bestand, mitarbeiter));
	}
	
	public void fuegeMassengutartikelEin(Mitarbeiter mitarbeiter, int artikelnummer, String bezeichnung, double preis, int packungsgroesse, int bestand) throws ArtikelExistiertBereitsException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		Massengutartikel artikel = new Massengutartikel(artikelnummer, bezeichnung, preis, packungsgroesse, bestand);
		meineArtikel.einfuegen(artikel);
		
		meineEreignisse.hinzufuegen(new Ereignis(new Date(), artikel, bestand, mitarbeiter));
	}
	
	public Artikel gibArtikel(int artikelnummer) throws ArtikelExistiertNichtException {
		return meineArtikel.getArtikel(artikelnummer);
	}
	
	public void artikelBestandVeraendern(Mitarbeiter mitarbeiter, int artikelnummer, int anzahl) throws ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		meineArtikel.bestandVeraendern(artikelnummer, anzahl);
		meineEreignisse.hinzufuegen(new Ereignis(new Date(), gibArtikel(artikelnummer), anzahl, mitarbeiter));
	}
	
	public List<Artikel> gibAlleArtikelSortiertNachArtikelnummer() {
		return meineArtikel.getArtikelBestandSortiertNachArtikelnummer();
	}
	
	public List<Artikel> gibAlleArtikelSortiertNachBezeichnung() {
		return meineArtikel.getArtikelBestandSortiertNachBezeichnung();
	}
	
	public List<Artikel> sucheArtikel(int artikelnummer) {
		return meineArtikel.sucheArtikel(artikelnummer); 
	}
	
	public List<Artikel> sucheArtikel(String bezeichnung) {
		return meineArtikel.sucheArtikel(bezeichnung); 
	}

	public void artikelBearbeiten(int artikelnummer, double preis, String bezeichnung) throws ArtikelExistiertNichtException {
		Artikel artikel = gibArtikel(artikelnummer);
		artikel.setPreis(preis);
		artikel.setBezeichnung(bezeichnung);
	}
	
	public void entferneArtikel(Mitarbeiter mitarbeiter, int artikelnummer) throws ArtikelExistiertNichtException, IOException {
		meineEreignisse.entferneArtikelAusLog(artikelnummer, logDateiname);
		meineArtikel.entfernen(artikelnummer);
	}
	
	/**
	 * Methode zum Speichern des Artikelbestands in einer Datei.
	 * 
	 * @throws IOException
	 */
	public void schreibeArtikel() throws IOException {
		meineArtikel.schreibeDaten(artikelDateiname);
	}
	
	// Mitarbeiter Methoden
	
	/**
	 * Diese Methode ermšglicht es einen Mitarbeiter nach seiner ID
	 * zu suchen.
	 * @param id ID der Mitarbeiter Instanz, die man suchen mšchte
	 * @return Die Mitarbeiter Instanz mit der gegebenen ID.
	 */
	public Mitarbeiter sucheMitarbeiter(int id) throws MitarbeiterExistiertNichtException{
		return meineMitarbeiter.sucheMitarbeiter(id);
	}

	/**
	 * Diese Methode ermšglicht es um die Mitarbeiterliste einzusehen.
	 * @return Vector mit allen aktuellen Mitarbeiter Instanzen.
	 */
	public Vector<Mitarbeiter> gibAlleMitarbeiter(){
		return meineMitarbeiter.getMitarbeiterListe();
	}

	/**
	 * Diese Methode ermšglicht es eine Mitarbeiter Instanz zu lšschen.
	 * @param m Mitarbeiter Instanz zum lšschen.
	 */
	public void mitarbeiterLoeschen(Mitarbeiter m){
		meineMitarbeiter.loeschen(m);
	}

	/**
	 * Diese Methode bildet eine neue Mitarbeiter Instanz und fŸgt sie
	 * zur Mitarbeiterverwaltung hinzu.
	 * @param name Name des neuen Mitarbeiters
	 * @throws MitarbeiterExistiertBereitsException
	 * @throws UsernameExistiertBereitsException 
	 */
	public void fuegeMitarbeiterHinzu(String username, String passwort, String name, MitarbeiterFunktion funktion, double gehalt) throws MitarbeiterExistiertBereitsException, UsernameExistiertBereitsException{
		this.existiertUsernameSchon(username, " - in fuegeMitarbeiterHinzu() !");
		
		Mitarbeiter m = new Mitarbeiter(mitarbeiterNextId, username, passwort, name, funktion, gehalt);
		meineMitarbeiter.einfuegen(m);
		mitarbeiterNextId++;
	}

	/**
	 * Diese Methode schreibt alle Mitarbeiterdaten in die Datenquelle.
	 * @throws IOException
	 */
	public void schreibeMitarbeiter() throws IOException{
		meineMitarbeiter.schreibeDaten(mitarbeiterDateiname);
	}
	
	/**
	 * Diese Methode ist zum bearbeiten von Mitarbeitern.
	 * @param id
	 * @param passwort
	 * @param name
	 * @param funktion
	 * @param gehalt
	 * @param blockiert
	 * @throws MitarbeiterExistiertNichtException
	 */
	public void mitarbeiterBearbeiten(int id, String passwort, String name, MitarbeiterFunktion funktion, double gehalt, boolean blockiert) throws MitarbeiterExistiertNichtException{
		Mitarbeiter m = sucheMitarbeiter(id);
		m.setPasswort(passwort);
		m.setFunktion(funktion);
		m.setGehalt(gehalt);
		m.setBlockiert(blockiert);
	}
	
	/**
	 * Diese Methode iteriert zuerst durch die Mitarbeiterliste und dann durch die Kundenliste und 
	 * vergleicht die Usernamen.
	 * Beim ersten Treffer wird eine UsernameExistiertBereitsException geworfen.
	 * @param username Username fŸr die neue Person.
	 * @param zusatzMsg ZusŠtliche Informationen.
	 * @throws UsernameExistiertBereitsException
	 */
	public void existiertUsernameSchon(String username, String zusatzMsg) throws UsernameExistiertBereitsException{
		
		Iterator<Mitarbeiter> itM = meineMitarbeiter.getMitarbeiterListe().iterator();
		while(itM.hasNext()){
			if(itM.next().getUsername().equals(username)){
				throw new UsernameExistiertBereitsException(username, zusatzMsg);
			}
		}
		
		Iterator<Kunde> itK = meineKunden.getKundenListe().iterator();
		while(itK.hasNext()){
			if(itK.next().getUsername().equals(username)){
				throw new UsernameExistiertBereitsException(username, zusatzMsg);
			}
		}
	}
	
	// Kunden Methoden
	
	/**
	* Diese Methode ermoeglicht es einen Kunden nach seiner ID
	* zu suchen.
	* @param id der Kunden Instanz, die man suchen moechte
	* @return Die Kunden Instanz mit der gegebenen ID.
	*/
	public Kunde sucheKunde(int id) throws KundeExistiertNichtException{
		return meineKunden.sucheKunde(id);
	}

	/**
	* Diese Methode ermoeglicht es um die Kundenliste einzusehen.
	* @return Vector mit alle aktuellen Kunden Instanzen.
	*/
	public Vector<Kunde> gibAlleKunden(){
		return meineKunden.getKundenListe();
	}

	/**
	* Diese Methode ermöglicht es eine Kunden Instanz zu loeschen.
	* @param k Kunde Instanz zum loeschen.
	*/
	public void kundenLoeschen(Kunde k){
		meineKunden.loeschen(k);
	}

	/**
	 * Diese Methode bidet eine neue Kunden Instanz und fuegt sie
	 * zur Kundenverwaltung hinzu.
	 * @param name Name des neuen Kunden
	 * @throws KundeExistiertBereitsException
	 * @throws UsernameExistiertBereitsException 
	 */
	public void fuegeKundenHinzu(String username, String passwort, String name, String strasse, int plz, String wohnort) throws KundeExistiertBereitsException, UsernameExistiertBereitsException{
		this.existiertUsernameSchon(username, " - in fuegeKundenHinzu() !");
		
		Kunde k = new Kunde(kundenNextId, username, passwort, name, strasse, plz, wohnort);
		meineKunden.einfuegen(k);
		kundenNextId++;
	}
	
	/**
	 * Diese Methode ist zum bearbeiten einer Kunden Instanz zustaendig.
	 * @param id
	 * @param passwort
	 * @param name
	 * @param strasse
	 * @param plz
	 * @param wohnort
	 * @param blockiert
	 * @throws KundeExistiertNichtException
	 */
	public void kundenBearbeiten(int id, String passwort, String name, String strasse, int plz, String wohnort, boolean blockiert) throws KundeExistiertNichtException{
		Kunde k = sucheKunde(id);
		k.setPasswort(passwort);
		k.setName(name);
		k.setStrasse(strasse);
		k.setPlz(plz);
		k.setWohnort(wohnort);
		k.setBlockiert(blockiert);
	}

	/**
 	* Diese Methode ermoeglicht es den "schreibe" befehl der KundenVerwaltung zu triggern.
 	* @throws IOException
 	*/
	public void schreibeKunden() throws IOException{
		meineKunden.schreibeDaten(kundenDateiname);
	}
	
	public List<WarenkorbArtikel> gibWarenkorb(Kunde kunde) {
		return meineKunden.gibWarenkorb(kunde);
	}
	
	private WarenkorbArtikel gibWarenkorbArtikel(Kunde kunde, Artikel artikel) throws ArtikelExistiertNichtException {
		return meineKunden.gibWarenkorbArtikel(kunde, artikel);
	}
	
	public void inDenWarenkorbLegen(Kunde kunde, int artikelnummer, int stueckzahl) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		meineKunden.inDenWarenkorbLegen(kunde, new WarenkorbArtikel(this.gibArtikel(artikelnummer), stueckzahl));
	}
	
	public void ausDemWarenkorbHerausnehmen(Kunde kunde, int artikelnummer) throws ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		meineKunden.ausDemWarenkorbHerausnehmen(kunde, this.gibArtikel(artikelnummer));
	}
	
	public void stueckzahlAendern(Kunde kunde, int warenkorbArtikelnummer, int neueStueckzahl) throws ArtikelBestandIstZuKleinException, ArtikelExistiertNichtException, ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		meineKunden.stueckzahlAendern(kunde, this.gibWarenkorbArtikel(kunde, this.gibArtikel(warenkorbArtikelnummer)), neueStueckzahl);
	}
	
	public Rechnung kaufen(Kunde kunde) throws IOException, WarenkorbIstLeerException {
		Rechnung rechnung = meineKunden.kaufen(kunde);
		schreibeArtikel();
		Iterator<WarenkorbArtikel> iter = rechnung.getWarenkorb().iterator();
		while(iter.hasNext()){
			WarenkorbArtikel wa = iter.next();
			try {
				meineEreignisse.hinzufuegen(new Ereignis(new Date(), wa.getArtikel(), -wa.getStueckzahl(), kunde));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rechnung;
	}
	
	public void leeren(Kunde k) throws ArtikelBestandIstKeineVielfacheDerPackungsgroesseException {
		meineKunden.leeren(k);
	}
	
	public Kunde loginVergessen(String name, String strasse, int zip, String wohnort){
		Kunde result = null;
		Iterator<Kunde> itK = meineKunden.getKundenListe().iterator();
		while(itK.hasNext()){
			Kunde k = itK.next();
			if(k.getName().equals(name) && k.getStrasse().equals(strasse) && k.getPlz() == zip && k.getWohnort().equals(wohnort)){
				result = k;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Methode zur überprüfung des Logins auf basis des Usernamens und des Passwortes
	 * @param username
	 * @param password
	 * @return Person p
	 */
	public Person pruefeLogin(String username, String password) {
		Iterator<Mitarbeiter> itM = meineMitarbeiter.getMitarbeiterListe().iterator();
		Person p = null;
		while (itM.hasNext()) {
			p = itM.next();
			if (((String) p.getUsername()).equals(username) && ((String) p.getPasswort()).equals(password)) {
				return p;
			}
		}
		Iterator<Kunde> itK = meineKunden.getKundenListe().iterator();
		while (itK.hasNext()) {
			p = itK.next();
			if (((String) p.getUsername()).equals(username) && ((String) p.getPasswort()).equals(password)) {
				return p;
			}
		}
		System.out.println("Zugriff verweigert!!!");
		System.out.println("Bitte überprüfen Sie ihren Usernamen und Ihr Passwort.");
		return null;
	}
	
	// Ereignis Methoden
	
	public void schreibeEreignisse() throws IOException{
		meineEreignisse.schreibeDaten(logDateiname);
	}
	
	public String gibBestandsHistorie(Artikel artikel) throws IOException{
		return meineEreignisse.gibBestandsHistorie(artikel, logDateiname);
	}
	
	public int[] gibBestandsHistorieDaten(Artikel artikel) throws IOException{
		return meineEreignisse.gibBestandsHistorieDaten(artikel, logDateiname);
	}
	
	public String gibLogDatei() throws IOException{
		return meineEreignisse.liesLogDatei(logDateiname);
	}

	@Override
	public void disconnect() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}
