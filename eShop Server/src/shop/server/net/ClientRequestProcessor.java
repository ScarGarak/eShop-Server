package shop.server.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

import java.util.Timer;

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
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.Massengutartikel;
import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.MitarbeiterFunktion;
import shop.common.valueobjects.Person;
import shop.common.valueobjects.Rechnung;
import shop.common.valueobjects.WarenkorbArtikel;

/**
 * Klasse zur Verarbeitung der Kommunikation zwischen EINEM Client und dem
 * Server. Die Kommunikation folgt dabei dem "Protokoll" der Anwendung. Das
 * ClientRequestProcessor-Objekt fŸhrt folgende Schritte aus: 
 * 0. BegrŸ§ungszeile an den Client senden
 * Danach in einer Sschleife:
 *  1. Empfang einer Zeile vom Client (z.B. Aktionsauswahl, hier eingeschrŠnkt); 
 *     wenn der Client die Abbruchaktion sendet ('q'), wird die Schleife verlassen
 *  2. abhŠngig von ausgewŠhlter Aktion, Empfang weiterer Zeilen (Parameter fŸr ausgewŠhlte Aktion)
 *  3. Senden der Antwort an den Client; die Antwort besteht je nach Aktion aus einer oder mehr Zeilen
 * 
 * @author teschke, eirund
 */
class ClientRequestProcessor implements Runnable {
	
	private static final int WARENKORBLEERENTIMERDELAY = 1000*60*15;

	// Shopverwaltungsobjekt, das die eigentliche Arbeit machen soll
	private ShopInterface shop; 
	
	private Kunde kunde;
	private Timer warenkorbLeerenTimer;
	private TimerTask warenkorbLeerenTimerTask;

	// Datenstrukturen fŸr die Kommunikation
	private Socket clientSocket;
	private BufferedReader in;
	private PrintStream out;
	
	/**
	 * Konstruktor zur Erzeugung des Clientrequestprozessors.
	 * 
	 * @param socket
	 * @param shopVerwaltung
	 */
	public ClientRequestProcessor(Socket socket, ShopInterface shopVerwaltung) {

		shop = shopVerwaltung;
		clientSocket = socket;
		kunde = null;

		// I/O-Streams initialisieren und ClientRequestProcessor-Objekt als Thread starten:
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new PrintStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			try {
				clientSocket.close();
			} catch (IOException e2) {
				System.err.println("Ausnahme bei Schliessung des Streams: " + e2);
				return;
			}
			System.err.println("Ausnahme bei Bereitstellung des Streams: " + e);
			return;
		}
		
		warenkorbLeerenTimer = new Timer();
		warenkorbLeerenTimerTask = new WarenkorbLeerenTimer(shop, kunde);
		
		
		System.out.println("Verbunden mit " + clientSocket.getInetAddress()
				+ ":" + clientSocket.getPort());
	}

	/**
	 * Methode zur Abwicklung der Kommunikation mit dem Client gemŠ§ dem
	 * vorgebenen Kommunikationsprotokoll.
	 */
	public void run() {

		String input = "";

		// BegrŸ§ungsnachricht an den Client senden
		out.println("Server an Client: Bin bereit fŸr Deine Anfragen!");

		// Hauptschleife zur wiederholten Abwicklung der Kommunikation
		do {
			// Beginn der Benutzerinteraktion:
			// Aktion vom Client einlesen [dann ggf. weitere Daten einlesen ...]
			try {
				input = in.readLine();
				if(kunde != null){
					resetWarenkorbLeerenTimer();
					if(shop.gibWarenkorb(kunde) != null && shop.gibWarenkorb(kunde).size() != 0){
						warenkorbLeerenTimer.schedule(warenkorbLeerenTimerTask, WARENKORBLEERENTIMERDELAY);
					}
				}
			} catch (Exception e) {
				System.out.println("--->Fehler beim Lesen vom Client (Aktion): ");
				System.out.println(e.getMessage());
				continue;
			}

			// Eingabe bearbeiten:
			if (input == null) {
				// input wird von readLine() auf null gesetzt, wenn Client Verbindung abbricht
				// Einfach behandeln wie ein "quit"
				input = "q";
			}
			else if (input.equals("pl")) {
				pruefeLogin();
			}

			// Artikel-Methode 
			else if (input.equals("fae")) {
				fuegeArtikelEin();


			}
			else if (input.equals("fme")) {
				fuegeMassengutartikelEin();

			}

			else if (input.equals("abv")) {
				artikelBestandVeraendern();
			}
			else if (input.equals("gaasna")) {
				gibAlleArtikelSortiertNachArtikelnummer();
			}
			else if (input.equals("gaasnb")) {
				gibAlleArtikelSortiertNachBezeichnung();
			}
			else if (input.equals("saa")) {
				sucheArtikelNachArtikelnummer();
			}
			else if (input.equals("sab")) {
				sucheArtikelNachBezeichnung();
			}
			else if (input.equals("ab")) {
				artikelBearbeiten();
			}
			else if (input.equals("ea")) {
				entferneArtikel();
			}
			else if (input.equals("scha")) {
				schreibeArtikel();
			}
			// Kunden-Methoden
			else if (input.equals("ke")) {
				fuegeKundenHinzu();
			} 
			// Mitarbeiter-Methoden
			else if (input.equals("mf")) {
				sucheMitarbeiter();
			}
			else if (input.equals("ma")) {
				gibAlleMitarbeiter();
			}
			else if (input.equals("me")) {
				fuegeMitarbeiterHinzu();
			}
			else if (input.equals("mb")) {
				mitarbeiterBearbeiten();
			}
			else if (input.equals("ml")) {
				mitarbeiterLoeschen();
			}
			else if (input.equals("sm")) {
				schreibeMitarbeiter();
			}
			// Warenkorb
			else if (input.equals("gw")) {
				gibWarenkorb();
			}
			else if (input.equals("idwl")) {
				inDenWarenkorbLegen();
			}
			else if (input.equals("adwh")) {
				ausDemWarenkorbHerausnehmen();
			}
			else if (input.equals("sa")) {
				stueckzahlAendern();
			}
			else if (input.equals("k")) {
				kaufen();
			}
			else if (input.equals("l")) {
				leeren();
			}
			// Ereignis-Methoden
			else if (input.equals("se")) {
				schreibeEreignisse();
			}
			else if (input.equals("gbhd")) {
				gibBestandsHistorieDaten();
			}
			else if (input.equals("gl")) {
				gibLogDatei();
			}
			
			else if (input.equals("lv")) {
				loginVergessen();
			}
			else if (input.equals("kb")) {
				kundenBearbeiten();
			} else if (input.equals("sk")) {
				sucheKunde();
			}
			else if (input.equals("gak")) {
				gibAlleKunden();
			}else if (input.equals("kl")) {
				kundenLoeschen();
			}else if (input.equals("sck")) {
				schreibeKunden();
			} 
			// ---
			// weitere Server-Dienste ...
			// ---

		} while (!(input.equals("q")));

		// Verbindung wurde vom Client abgebrochen:
		disconnect();		
	}
	
	private void loginVergessen() {
		Kunde k = null;
		try {
		String name = in.readLine();
		String strasse = in.readLine();
		int zip = Integer.parseInt(in.readLine());
		String wohnort = in.readLine();
		k = shop.loginVergessen(name, strasse, zip, wohnort);
		System.out.println("login vergessen kunde k: " + k);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (k !=null) {
//			kunden suche erfolgreich
			out.println("kse");
			sendeKunde(k);
		} else {
			out.println("ken");
		}
	}
	private void fuegeKundenHinzu() {
		String input = null;
		String ergebnis = null;
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String username = new String(input);
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String passwort = new String(input);
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String name = new String(input);
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strasse = new String(input);
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strPlz = new String(input);
		int plz = Integer.parseInt(strPlz);
		
		try {
			input = in.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String wohnort = new String(input);
		
		try {
			shop.fuegeKundenHinzu(username, passwort, name, strasse, plz, wohnort);
			try {
				shop.schreibeKunden();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ergebnis = "kee";
		} catch (KundeExistiertBereitsException e) {
			ergebnis = "keb";
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsernameExistiertBereitsException e) {
			ergebnis = "ueb";
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.println(ergebnis);
	}
	
	private void pruefeLogin() {
		String input = null;
		
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Username): ");
			System.out.println(e.getMessage());
		}
		String username = new String(input);
		
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Password): ");
			System.out.println(e.getMessage());
		}
		String password = new String(input);

		Person p = shop.pruefeLogin(username, password);
		if (p != null) {
			sendePersonAnClient(p);
		} else {
			out.println("Fehler");
		}
	}
	
	private void fuegeArtikelEin() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Mitarbeiters:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Mitarbeiter): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// dann die Bezeichnung:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bezeichnung): ");
			System.out.println(e.getMessage());
		}
		String bezeichnung = new String(input);
		
		// dann den Preis:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Preis): ");
			System.out.println(e.getMessage());
		}
		double preis = Double.parseDouble(input);
		
		// dann den Bestand:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bestand): ");
			System.out.println(e.getMessage());
		}
		int bestand = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.fuegeArtikelEin(shop.sucheMitarbeiter(id), artikelnummer, bezeichnung, preis, bestand);
			out.println("Erfolg");
		} catch (ArtikelExistiertBereitsException e) {
			out.println("ArtikelExistiertBereitsException");
		} catch (MitarbeiterExistiertNichtException e) {
			out.println("MitarbeiterExistiertNichtException");
		}
	}
	
	private void fuegeMassengutartikelEin() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Mitarbeiters:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Mitarbeiter): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// dann die Bezeichnung:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bezeichnung): ");
			System.out.println(e.getMessage());
		}
		String bezeichnung = new String(input);
		
		// dann den Preis:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Preis): ");
			System.out.println(e.getMessage());
		}
		double preis = Double.parseDouble(input);
		
		// dann die Packungsgroesse:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Packungsgroesse): ");
			System.out.println(e.getMessage());
		}
		int packungsgroesse = Integer.parseInt(input);
		
		// dann den Bestand:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bestand): ");
			System.out.println(e.getMessage());
		}
		int bestand = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.fuegeMassengutartikelEin(shop.sucheMitarbeiter(id), artikelnummer, bezeichnung, preis, packungsgroesse, bestand);
			out.println("Erfolg");
		} catch (ArtikelExistiertBereitsException e) {
			out.println("ArtikelExistiertBereitsException");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (MitarbeiterExistiertNichtException e) {
			out.println("MitarbeiterExistiertNichtException");
		} 
	}
	
	private void artikelBestandVeraendern() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Mitarbeiters:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Mitarbeiter): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// dann die Anzahl:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Anzahl): ");
			System.out.println(e.getMessage());
		}
		int anzahl = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.artikelBestandVeraendern(shop.sucheMitarbeiter(id), artikelnummer, anzahl);
			out.println("Erfolg");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (MitarbeiterExistiertNichtException e) {
			out.println("MitarbeiterExistiertNichtException");
		} 
	}
	
	private void gibAlleArtikelSortiertNachArtikelnummer() {
		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		List<Artikel> artikel = null;
		artikel = shop.gibAlleArtikelSortiertNachArtikelnummer();
		
		sendeArtikelAnClient(artikel);
	}
	
	private void gibAlleArtikelSortiertNachBezeichnung() {
		
		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		List<Artikel> artikel = null;
		artikel = shop.gibAlleArtikelSortiertNachBezeichnung();
		
		sendeArtikelAnClient(artikel);
	}
	
	private void sucheArtikelNachArtikelnummer() {
		if(kunde != null){
			resetWarenkorbLeerenTimer();
			if(shop.gibWarenkorb(kunde) != null && shop.gibWarenkorb(kunde).size() != 0){
				warenkorbLeerenTimer.schedule(warenkorbLeerenTimerTask, WARENKORBLEERENTIMERDELAY);
			}
		}
		
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// hier ist nur die Artikelnummer der gesuchten Artikel erforderlich:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		List<Artikel> artikel = null;
		artikel = shop.sucheArtikel(artikelnummer);

		sendeArtikelAnClient(artikel);
	}
	
	private void sucheArtikelNachBezeichnung() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// hier ist nur der Bezeichnung der gesuchten Artikel erforderlich:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bezeichnung): ");
			System.out.println(e.getMessage());
		}
		String bezeichnung = new String(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		List<Artikel> artikel = null;
		artikel = shop.sucheArtikel(bezeichnung);

		sendeArtikelAnClient(artikel);
	}
	
	private void artikelBearbeiten() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);

		// dann den Preis:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Preis): ");
			System.out.println(e.getMessage());
		}
		double preis = Double.parseDouble(input);
		
		// dann die Bezeichnung:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Bezeichnung): ");
			System.out.println(e.getMessage());
		}
		String bezeichnung = new String(input);
		
		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.artikelBearbeiten(artikelnummer, preis, bezeichnung);
			out.println("Erfolg");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} 
	}
	
	private void entferneArtikel() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Mitarbeiters:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Mitarbeiter): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.entferneArtikel(shop.sucheMitarbeiter(id), artikelnummer);
			out.println("Erfolg");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} catch (IOException e) {
			out.println("IOException");
		} catch (MitarbeiterExistiertNichtException e) {
			out.println("MitarbeiterExistiertNichtException");
		}
	}
	
	private void schreibeArtikel() {
		// Parameter sind in diesem Fall nicht einzulesen
		
		// die Arbeit macht wie immer das Shopverwaltungsobjekt:
		try {
			shop.schreibeArtikel();
			out.println("Erfolg");
		} catch (IOException e) {
			out.println("IOException");
		} catch (Exception e) {
			System.out.println("--->Fehler beim Sichern: ");
			System.out.println(e.getMessage());
			out.println("Fehler");
		} 
	}
	
	private void gibWarenkorb() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Kunden:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);
		
		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		List<WarenkorbArtikel> warenkorbArtikel = null;
		try {
			warenkorbArtikel = shop.gibWarenkorb(shop.sucheKunde(id));
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");
		}
				
		sendeWarenkorbArtikelAnClient(warenkorbArtikel);
	}
	
	private void inDenWarenkorbLegen() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Kunden:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// dann die StŸckzahl:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (StŸckzahl): ");
			System.out.println(e.getMessage());
		}
		int stueckzahl = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.inDenWarenkorbLegen(shop.sucheKunde(id), artikelnummer, stueckzahl);
			out.println("Erfolg");
		} catch (ArtikelBestandIstZuKleinException e) {
			out.println("ArtikelBestandIstZuKleinException");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");
		}
		
		// Starte Timer:
		if(kunde != null){
			resetWarenkorbLeerenTimer();
			if(shop.gibWarenkorb(kunde) != null && shop.gibWarenkorb(kunde).size() != 0){
				warenkorbLeerenTimer.schedule(warenkorbLeerenTimerTask, WARENKORBLEERENTIMERDELAY);
			}
		}
	}
	
	private void ausDemWarenkorbHerausnehmen() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Kunden:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.ausDemWarenkorbHerausnehmen(shop.sucheKunde(id), artikelnummer);
			out.println("Erfolg");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");
		}
	}
	
	private void stueckzahlAendern() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die ID des Kunden:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// dann die Warenkorb Artikelnummer:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Artikelnummer): ");
			System.out.println(e.getMessage());
		}
		int artikelnummer = Integer.parseInt(input);
		
		// dann die neue StŸckzahl:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Neue StŸckzahl): ");
			System.out.println(e.getMessage());
		}
		int neueStueckzahl = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.stueckzahlAendern(shop.sucheKunde(id), artikelnummer, neueStueckzahl);
			out.println("Erfolg");
		} catch (ArtikelBestandIstZuKleinException e) {
			out.println("ArtikelBestandIstZuKleinException");
		} catch (ArtikelExistiertNichtException e) {
			out.println("ArtikelExistiertNichtException");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");
		}
	}
	
	private void kaufen() {
		//Stoppe Timer
		resetWarenkorbLeerenTimer();
		
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// hier ist nur die ID des Kunden erforderlich:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		Rechnung rechnung = null;
		try {
			rechnung = shop.kaufen(shop.sucheKunde(id));
			sendeRechnungAnClient(rechnung);
		} catch (IOException e) {
			out.println("IOException");
		} catch (WarenkorbIstLeerException e) {
			out.println("WarenkorbIstLeerException");
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");
		}
	}
	
	private void leeren() {
		resetWarenkorbLeerenTimer();

		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// hier ist nur die ID des Kunden erforderlich:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (ID Kunde): ");
			System.out.println(e.getMessage());
		}
		int id = Integer.parseInt(input);

		// die eigentliche Arbeit soll das Shopverwaltungsobjekt machen:
		try {
			shop.leeren(shop.sucheKunde(id));
			out.println("Erfolg");
		} catch (ArtikelBestandIstKeineVielfacheDerPackungsgroesseException e) {
			out.println("ArtikelBestandIstKeineVielfacheDerPackungsgroesseException");
		} catch (KundeExistiertNichtException e) {
			out.println("KundeExistiertNichtException");

		}
	}
	
	private void sendePersonAnClient(Person p) {
		// Typ der Person senden
		out.println(p.getPersonTyp());
		// ID der Person senden
		out.println(p.getId());
		// Name der Person senden
		out.println(p.getName());
		switch(p.getPersonTyp()) {
			case Kunde: 
				// Strasse des Kunden senden
				out.println(((Kunde) p).getStrasse());
				// Postleitzahl des Kunden senden
				out.println(((Kunde) p).getPlz());
				// Wohnort des Kunden senden
				out.println(((Kunde) p).getWohnort());
				kunde = ((Kunde)p);
				resetWarenkorbLeerenTimer();
				break;
			case Mitarbeiter: 
				// Funktion des Mitarbeiters senden
				out.println(((Mitarbeiter) p).getFunktion());
				// Gehalt des Mitarbeiters senden
				out.println(((Mitarbeiter) p).getGehalt());
				break;
			default: 
				break;
		}
		out.println(p.getBlockiert());
	}
	
	private void sendeArtikelAnClient(List<Artikel> artikel) {
		Iterator<Artikel> iter = artikel.iterator();
		Artikel a = null;
		// Anzahl der Artikel senden
		out.println(artikel.size());
		while (iter.hasNext()) {
			a = iter.next();
			// Artikeltyp des Artikels senden
			if (a instanceof Massengutartikel)
				out.println("Massengutartikel");
			else 
				out.println("Artikel");
			// Nummer des Artikels senden
			out.println(a.getArtikelnummer());
			// Bezeichnung des Artikels senden
			out.println(a.getBezeichnung());
			// Preis des Artikels senden
			out.println(a.getPreis());
			// Bestand des Artikels senden
			out.println(a.getBestand());
			if (a instanceof Massengutartikel)
				// Packungsgroesse des Massengutartikels senden
				out.println(((Massengutartikel) a).getPackungsgroesse());
		}
	}
	
	private void sendeWarenkorbArtikelAnClient(List<WarenkorbArtikel> warenkorbArtikel) {
		Iterator<WarenkorbArtikel> iter = warenkorbArtikel.iterator();
		WarenkorbArtikel wa = null;
		// Anzahl der Warenkorb Artikel senden
		out.println(warenkorbArtikel.size());
		while (iter.hasNext()) {
			wa = iter.next();
			// Artikel des Warenkorb Artikels senden
			Artikel a = wa.getArtikel();
			// Artikeltyp des Artikels senden
			if (a instanceof Massengutartikel)
				out.println("Massengutartikel");
			else 
				out.println("Artikel");
			// Nummer des Artikels senden
			out.println(a.getArtikelnummer());
			// Bezeichnung des Artikels senden
			out.println(a.getBezeichnung());
			// Preis des Artikels senden
			out.println(a.getPreis());
			// Bestand des Artikels senden
			out.println(a.getBestand());
			if (a instanceof Massengutartikel)
				// Packungsgroesse des Massengutartikels senden
				out.println(((Massengutartikel) a).getPackungsgroesse());
			// StŸckzahl des Warenkorb Artikels senden
			out.println(wa.getStueckzahl());
		}
	}
	
	private void sendeRechnungAnClient(Rechnung rechnung) {
		// Datum der Rechnung senden
		out.println(rechnung.getDatum());
		// Warenkorb der Rechnung senden
		sendeWarenkorbArtikelAnClient(rechnung.getWarenkorb());
	}
	
	private void disconnect() {
		try {
			out.println("Tschuess!");
			clientSocket.close();

			System.out.println("Verbindung zu " + clientSocket.getInetAddress()
					+ ":" + clientSocket.getPort() + " durch Client abgebrochen");
		} catch (Exception e) {
			System.out.println("--->Fehler beim Beenden der Verbindung: ");
			System.out.println(e.getMessage());
			out.println("Fehler");
		}
	}
	
	//////// Mitarbeiter ////////

	private void sucheMitarbeiter(){
		String input = null;
		Mitarbeiter m = null;

		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out.println("--->Fehler beim Lesen vom Client (Mitarbeiter ID): ");
			System.out.println(e.getMessage());
		}
		int id = new Integer(input);

		try {
			m = shop.sucheMitarbeiter(id);
		} catch (MitarbeiterExistiertNichtException e) {
			out.println("MitarbeiterExistiertNicht");
		}

		if(m != null){
			sendeMitarbeiter(m);
		}

	}

	private void gibAlleMitarbeiter(){
		Vector<Mitarbeiter> mitarbeiterListe = null;

		mitarbeiterListe = shop.gibAlleMitarbeiter();
		Iterator<Mitarbeiter> iter = mitarbeiterListe.iterator();

		out.println(mitarbeiterListe.size());
		while(iter.hasNext()){
			sendeMitarbeiter(iter.next());
		}
	}

	private void fuegeMitarbeiterHinzu(){
		try{
			String username = in.readLine();
			String passwort = in.readLine();
			String name = in.readLine();
			MitarbeiterFunktion funktion = MitarbeiterFunktion.valueOf(in.readLine());
			double gehalt = Double.parseDouble(in.readLine());
			shop.fuegeMitarbeiterHinzu(username, passwort, name, funktion, gehalt);
			out.println("OK");
		} catch (MitarbeiterExistiertBereitsException e) {
			out.println("MitarbeiterExistiertBereits");
		} catch (UsernameExistiertBereitsException e) {
			out.println("UsernameExistiertBereits");
		} catch (Exception e){
			System.out.println("--->Fehler beim Lesen vom Client (fuegeMitarbeiterHinzu): ");
			System.out.println(e.getMessage());
		} 

	}

	private void kundenBearbeiten() {
		try {
			int id = Integer.parseInt(in.readLine());
			String passwort = in.readLine();
			String name = in.readLine();
			String strasse = in.readLine();
			int plz = Integer.parseInt(in.readLine());
			String wohnort = in.readLine();
			boolean blockiert = Boolean.valueOf(in.readLine());
			try {
				shop.kundenBearbeiten(id, passwort, name, strasse, plz, wohnort, blockiert);
			} catch (KundeExistiertNichtException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (NumberFormatException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void mitarbeiterBearbeiten(){
		try{
			int id = Integer.parseInt(in.readLine());
			Mitarbeiter m = shop.sucheMitarbeiter(id);
			// Empfangen der Daten
			String passwort = in.readLine();
			String name = in.readLine();
			MitarbeiterFunktion funktion = MitarbeiterFunktion.valueOf(in.readLine());
			double gehalt = Double.parseDouble(in.readLine());
			boolean blockiert = Boolean.valueOf(in.readLine());
			// Speicheren der Daten
			m.setPasswort(passwort);
			m.setName(name);
			m.setFunktion(funktion);
			m.setGehalt(gehalt);
			m.setBlockiert(blockiert);

			out.println("OK");
		} catch (MitarbeiterExistiertNichtException e){ 
			out.println("MitarbeiterExistiertNicht");
		} catch (Exception e){
			System.out.println("--->Fehler beim Lesen vom Client (mitarbeiterBearbeiten): ");
			System.out.println(e.getMessage());
		} 
	}

	private void mitarbeiterLoeschen(){
		try {
			int id = Integer.parseInt(in.readLine());
			Mitarbeiter m = shop.sucheMitarbeiter(id);
			shop.mitarbeiterLoeschen(m);
		} catch (IOException e) {
			System.out.println("--->Fehler beim Lesen vom Client (mitarbeiterLoeschen): ");
			System.out.println(e.getMessage());
		} catch (Exception e){
			System.out.println("--->Fehler beim Loeschen von Mitarbeiter: ");
			System.out.println(e.getMessage());
		}
	}
	
	private void schreibeMitarbeiter(){
		try {
			shop.schreibeMitarbeiter();
		} catch (IOException e) {
			out.println("IOException");
			return;
		}
		out.println("OK");
	}

	public void sucheKunde() {
		Kunde k = null;
		int id = 0;
		try {
			id = Integer.parseInt(in.readLine());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			k = shop.sucheKunde(id);
		} catch (KundeExistiertNichtException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (k != null) {
			out.println("kse");
			out.print(k.getId());
			sendeKunde(k);
		} else {
			out.println("ksf");
		}
	}
	
	private void gibAlleKunden() {
		Vector<Kunde> kundenListe = null;

		kundenListe = shop.gibAlleKunden();
		Iterator<Kunde> iter = kundenListe.iterator();

		out.println(kundenListe.size());
		while(iter.hasNext()){
			Kunde k = iter.next();
			out.println(k.getId());
			sendeKunde(k);
			out.println(k.getBlockiert());
		}
	}
	
	public void kundenLoeschen() {
		try {
			int id = Integer.parseInt(in.readLine());
			Kunde k = shop.sucheKunde(id);
			shop.kundenLoeschen(k);
		} catch (IOException e) {
			System.out.println("--->Fehler beim Lesen vom Client (KundenLoeschen): ");
			System.out.println(e.getMessage());
		} catch (Exception e){
			System.out.println("--->Fehler beim Loeschen von Kunde: ");
			System.out.println(e.getMessage());
		}
	}
	
	private void schreibeKunden() {
		try {
			shop.schreibeKunden();
		} catch (IOException e) {
			System.out.println("--->Fehler beim schreiben von Kunde: ");
			System.out.println(e.getMessage());
		}
	}
	
	private void sendeKunde(Kunde k) {
		out.println(k.getUsername());
		out.println(k.getPasswort());
		out.println(k.getName());
		out.println(k.getStrasse());
		out.println(k.getPlz());
		out.println(k.getWohnort());
	}
	
	private void sendeMitarbeiter(Mitarbeiter m){
		out.println(m.getId());
		out.println(m.getUsername());
		out.println(m.getPasswort());
		out.println(m.getName());
		out.println(m.getFunktion());
		out.println(m.getGehalt());
		out.println(m.getBlockiert());
	}
	
	
	//////// Ereignisse ////////
	
	private void schreibeEreignisse(){
		try {
			shop.schreibeEreignisse();
		} catch (IOException e) {
			System.out.println("--->Fehler beim schreiben von Ereignissen: ");
			System.out.println(e.getMessage());
		}
	}
	
	private void gibBestandsHistorieDaten(){
		try {
			int artikelnummer = Integer.parseInt(in.readLine());
			int[] daten = shop.gibBestandsHistorieDaten(artikelnummer);
			out.println(daten.length);
			for (int i = 0; i < daten.length; i++){
				out.println(daten[i]);
			}
		} catch (IOException e) {
			System.out.println("--->Fehler beim Lesen vom Client (gibBestandsHistorieDaten): ");
			System.out.println(e.getMessage());
		} catch (ArtikelExistiertNichtException e){
			System.out.println("--->Fehler beim Senden der Bestandshistoriedaten: ");
			System.out.println(e.getMessage());
		}
	}
	
	private void gibLogDatei(){
		try {
			String logDatei = shop.gibLogDatei();
			String[] eintraege = logDatei.split("\n");
			out.println(eintraege.length);
			for(int i = 0; i < eintraege.length; i++){
				out.println(eintraege[i]);
			}
		} catch (IOException e) {
			out.println("IOException");
		}
	}

	private void resetWarenkorbLeerenTimer(){
		warenkorbLeerenTimerTask.cancel();
		warenkorbLeerenTimerTask = new WarenkorbLeerenTimer(shop, kunde);
	}
}
