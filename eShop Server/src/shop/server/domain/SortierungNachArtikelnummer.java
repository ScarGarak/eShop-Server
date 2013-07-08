package shop.server.domain;

import java.util.Comparator;

import shop.common.valueobjects.Artikel;

/**
 * Klasse zum sortieren von Artikeln nach Artikelnummer.
 * 
 * @author Christof Ferreira Torres
 */
public class SortierungNachArtikelnummer implements Comparator<Artikel> {

	/**
	 * Methode um zwei Artikel anhand ihrer Artikelnummer zu vergleichen.
	 */
    public int compare(Artikel artikel1, Artikel artikel2) {
		return artikel1.getArtikelnummer() - artikel2.getArtikelnummer();
	}

}
