package shop.server.net;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

import shop.common.exceptions.KundeExistiertBereitsException;
import shop.common.exceptions.MitarbeiterExistiertBereitsException;
import shop.common.exceptions.MitarbeiterExistiertNichtException;
import shop.common.exceptions.UsernameExistiertBereitsException;
import shop.common.interfaces.ShopInterface;
import shop.common.valueobjects.Kunde;
import shop.common.valueobjects.Mitarbeiter;
import shop.common.valueobjects.MitarbeiterFunktion;
import shop.common.valueobjects.Person;

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

	// Shopverwaltungsobjekt, das die eigentliche Arbeit machen soll
	private ShopInterface shop; 

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
			else if (input.equals("ke")) {
				fuegeKundenHinzu();
			} 
			else if (input.equals("mf")){
				sucheMitarbeiter();
			}else if (input.equals("ma")){
				gibAlleMitarbeiter();
			}else if (input.equals("me")){
				fuegeMitarbeiterHinzu();
			}else if (input.equals("mb")){
				mitarbeiterBearbeiten();
			}else if (input.equals("ml")){
				mitarbeiterLoeschen();
			}
			/*else if (input.equals("a")) {
				// Aktion "Bücher _a_usgeben" gewählt
				ausgeben();
			} else if (input.equals("e")) {
				// Aktion "Buch _e_infügen" gewählt
				einfuegen();
			} else if (input.equals("f")) {
				// Aktion "Bücher _f_inden" (suchen) gewählt
				suchen();
			}
			else if (input.equals("s")) {
				// Aktion "_s_peichern" gewählt
				speichern();
			}*/
			// ---
			// weitere Server-Dienste ...
			// ---

		} while (!(input.equals("q")));

		// Verbindung wurde vom Client abgebrochen:
		disconnect();		
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
		}
	}
	
	private void sendePersonAnClient(Person p) {
		out.println(p.getPersonTyp());
		out.println(p.getId());
		out.println(p.getName());
		switch(p.getPersonTyp()) {
			case Kunde: 
				out.println(((Kunde) p).getStrasse());
				out.println(((Kunde) p).getPlz());
				out.println(((Kunde) p).getWohnort());
				break;
			case Mitarbeiter: 
				out.println(((Mitarbeiter) p).getFunktion());
				out.println(((Mitarbeiter) p).getGehalt());
				break;
			default: 
				break;
		}
	}
	
	/*
	private void speichern() {
		// Parameter sind in diesem Fall nicht einzulesen
		
		// die Arbeit macht wie immer Bibliotheksverwaltungsobjekt:
		try {
			bibV.schreibeBuecher();
			out.println("Erfolg");
		} catch (Exception e) {
			System.out.println("--->Fehler beim Sichern: ");
			System.out.println(e.getMessage());
			out.println("Fehler");
		}
	}

	private void suchen() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// hier ist nur der Titel der gesuchten Bücher erforderlich:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Titel): ");
			System.out.println(e.getMessage());
		}
		// Achtung: Objekte sind Referenzdatentypen:
		// Buch-Titel in neues String-Objekt kopieren, 
		// damit Titel nicht bei nächster Eingabe in input überschrieben wird
		String titel = new String(input);

		// die eigentliche Arbeit soll das Bibliotheksverwaltungsobjekt machen:
		List<Buch> buecher = null;
		if (titel.equals(""))
			buecher = bibV.gibAlleBuecher();
		else
			buecher = bibV.sucheNachTitel(titel);

		sendeBuecherAnClient(buecher);
	}

	private void ausgeben() {
		// Die Arbeit soll wieder das Bibliotheksverwaltungsobjekt machen:
		List<Buch> buecher = null;
		buecher = bibV.gibAlleBuecher();

		sendeBuecherAnClient(buecher);
	}

	private void sendeBuecherAnClient(List<Buch> buecher) {
		Iterator<Buch> iter = buecher.iterator();
		Buch buch = null;
		// Anzahl der gefundenen Bücher senden
		out.println(buecher.size());
		while (iter.hasNext()) {
			buch = iter.next();
			// Nummer des Buchs senden
			out.println(buch.getNummer());
			// Titel des Buchs senden
			out.println(buch.getTitel());
			// Verfügbarkeit des Buchs senden
			out.println(buch.isVerfuegbar());
		}
	}

	private void einfuegen() {
		String input = null;
		// lese die notwendigen Parameter, einzeln pro Zeile
		// zuerst die Nummer des einzufügenden Buchs:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (BuchNr): ");
			System.out.println(e.getMessage());
		}
		int buchNr = Integer.parseInt(input);

		// dann den Titel:
		try {
			input = in.readLine();
		} catch (Exception e) {
			System.out
					.println("--->Fehler beim Lesen vom Client (Titel): ");
			System.out.println(e.getMessage());
		}
		// Achtung: Objekte sind Referenzdatentypen:
		// Buch-Titel in neues String-Objekt kopieren, 
		// damit Titel nicht bei nächste Eingabe in input überschrieben wird
		String titel = new String(input);

		// die eigentliche Arbeit soll das Bibliotheksverwaltungsobjekt machen:
		boolean ok = bibV.fuegeBuchEin(titel, buchNr);

		// Rückmeldung an den Client: war die Aktion erfolgreich?
		if (ok)
			out.println("Erfolg");
		else
			out.println("Fehler");
	}*/
	
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
	
	////////Mitarbeiter ////////

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

	private void sendeMitarbeiter(Mitarbeiter m){
		out.println(m.getId());
		out.println(m.getUsername());
		out.println(m.getPasswort());
		out.println(m.getFunktion());
		out.println(m.getGehalt());
		out.println(m.getBlockiert());
	}
	
}
