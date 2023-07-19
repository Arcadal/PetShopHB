package it.betacom.main;

import com.opencsv.exceptions.CsvValidationException;

public class Main {
	final static String fileCSV = ".\\documenti\\PetShop_dati.csv";

	public static void main(String[] args) throws CsvValidationException {
		Metodi metodo = new Metodi();
		metodo.log();
		metodo.inizializza();
		
		metodo.inserisci();
		metodo.importaCSV(fileCSV);
		metodo.generaReport1("Mario", "Rossi");
		metodo.generaReport2();
		metodo.generaReportTutti();

		metodo.chiusura();
	}
}
