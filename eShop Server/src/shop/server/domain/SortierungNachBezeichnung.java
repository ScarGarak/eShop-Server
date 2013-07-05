package shop.server.domain;

import java.util.Comparator;

import shop.common.valueobjects.Artikel;

public class SortierungNachBezeichnung implements Comparator<Artikel> {

	public int compare(Artikel artikel1, Artikel artikel2) {
		return artikel1.getBezeichnung().compareTo(artikel2.getBezeichnung());
	}

}
