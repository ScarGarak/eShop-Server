package shop.server.domain;

import java.util.Comparator;

import shop.common.valueobjects.Artikel;

/**
 * Klasse zum sortieren von Artikeln nach Bezeichnung.
 * 
 * @author Christof Ferreira Torres
 */
public class SortierungNachBezeichnung implements Comparator<Artikel> {

	/**
	 * Methode um zwei Artikel anhand ihrer Bezeichnung zu vergleichen.
	 */
	public int compare(Artikel artikel1, Artikel artikel2) {
		return artikel1.getBezeichnung().compareTo(artikel2.getBezeichnung());
	}

}
