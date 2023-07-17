package it.betacom.main;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import it.betacom.model.Animale;
import it.betacom.model.Cliente;

public class Metodi {

	private Configuration config = new Configuration();
	private SessionFactory sf;
	private Session session;

	
	//------------------------INIZIALIZZAZIONE E CHIUSURA DELLA CONNESSIONE------------------------
	public void inizializza() {
		config.configure("hibernate.cfg.xml");
		sf = config.buildSessionFactory();
		session = sf.openSession();
	}

	public void chiusura() {
		session.close();
		sf.close();
	}

	//------------------------SCRITTURA NEL DATABASE CON L'AIUTO DI DUE METODI GET------------------------
	@SuppressWarnings("unused")
	public void importaCSV(String filePath) throws CsvValidationException {
		Map<String, Cliente> existClienti = new HashMap<>();
		Map<String, Animale> existAnimali = new HashMap<>();

		try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
			String[] header = csvReader.readNext();
			String[] header2 = csvReader.readNext();

			String[] nextLine;
			while ((nextLine = csvReader.readNext()) != null) {
				String nomeCliente = nextLine[0];
				String cognomeCliente = nextLine[1];
				String keyCliente = nomeCliente + " " + cognomeCliente;

				Cliente cliente;
				if (existClienti.containsKey(keyCliente)) {
					cliente = existClienti.get(keyCliente);
				} else {
					cliente = getClienteByNomeCognome(nomeCliente, cognomeCliente);
					if (cliente == null) {
						String cittaCliente = nextLine[2];
						String telefonoCliente = nextLine[3];
						String indirizzoCliente = nextLine[4];

						cliente = new Cliente();
						cliente.setNome(nomeCliente);
						cliente.setCognome(cognomeCliente);
						cliente.setCitta(cittaCliente);
						cliente.setTelefono(telefonoCliente);
						cliente.setIndirizzo(indirizzoCliente);

						session.beginTransaction();
						session.persist(cliente);
						session.getTransaction().commit();
					}
					existClienti.put(keyCliente, cliente);
				}

				String matricola = nextLine[7];
				if (!existAnimali.containsKey(matricola)) {
					Animale animale = getAnimaleByMatricola(matricola);
					if (animale == null) {
						String tipoAnimale = nextLine[5];
						String nomeAnimale = nextLine[6];
						String dataAcquisto = nextLine[8];
						double prezzo = Double.parseDouble(nextLine[9]);

						animale = new Animale();
						animale.setTipoAnimale(tipoAnimale);
						animale.setNomeAnimale(nomeAnimale);
						animale.setMatricola(matricola);
						animale.setDataAcquisto(dataAcquisto);
						animale.setPrezzo(prezzo);
						animale.setCliente(cliente);

						session.beginTransaction();
						session.persist(animale);
						session.getTransaction().commit();
					}
					existAnimali.put(matricola, animale);
				}
			}

			System.out.println("Dati caricati correttamente");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Cliente getClienteByNomeCognome(String nome, String cognome) {
		String query = "FROM Cliente WHERE nome = :nome AND cognome = :cognome";
		return session.createQuery(query, Cliente.class).setParameter("nome", nome).setParameter("cognome", cognome)
				.setMaxResults(1).uniqueResult();
	}

	private Animale getAnimaleByMatricola(String matricola) {
		String query = "FROM Animale WHERE matricola = :matricola";
		return session.createQuery(query, Animale.class).setParameter("matricola", matricola).setMaxResults(1)
				.uniqueResult();
	}

	//------------------------INSERIMENTO NEL CSV------------------------
	
	public void inserisci() {
		String filePath = "./documenti/PetShop_dati.csv";
		String nuoviDati = "Gennaro,Esposito,Napoli,12345678,Via Napoli 25,Cane,Cupcake,586346,31/12/2023,175";

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
			writer.append(nuoviDati);
			writer.newLine();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Scrittura Completata");
	}

	//------------------------GENERAZIONE REPORT------------------------
	
	public void generaReport1(String nome, String cognome) {
	    String filePath = "./report/" + nome + "_" + cognome + ".txt";

	    try {
	        PrintWriter writer = new PrintWriter(new FileWriter(filePath));

	        List<Cliente> clienti = session
	                .createQuery("SELECT c FROM Cliente c WHERE c.nome LIKE :nome AND c.cognome LIKE :cognome", Cliente.class)
	                .setParameter("nome", nome + "%")
	                .setParameter("cognome", cognome + "%")
	                .getResultList();
	        for (Cliente cliente : clienti) {
	            writer.println("Cliente: " + cliente.getNome() + " " + cliente.getCognome());

	            List<Animale> animali = session
	                    .createQuery("SELECT a FROM Animale a WHERE a.cliente = :cliente", Animale.class)
	                    .setParameter("cliente", cliente)
	                    .getResultList();

	            writer.println(StringUtils.rightPad("Data_Acquisto", 15) + StringUtils.rightPad("Matricola", 15)
	                    + StringUtils.rightPad("Nome", 15) + StringUtils.rightPad("Prezzo di Vendita", 15));

	            for (Animale animale : animali) {
	                String prezzo = String.valueOf(animale.getPrezzo());
	                writer.println(StringUtils.rightPad(animale.getDataAcquisto(), 15)
	                        + StringUtils.rightPad(animale.getMatricola(), 15)
	                        + StringUtils.rightPad(animale.getNomeAnimale(), 15) + StringUtils.rightPad(prezzo, 15));
	            }

	            writer.println();
	        }

	        writer.close();
	        System.out.println("Report 1 generato con successo!");
	    } catch (IOException e) {
	        System.out.println("Si è verificato un errore durante la generazione del report: " + e.getMessage());
	    }
	}

	public void generaReport2() {
		String filePath = "./report/Lista_Vendite.txt";

		try {
			PrintWriter writer = new PrintWriter(new FileWriter(filePath));

			List<Animale> animali = session
					.createQuery("SELECT a FROM Animale a ORDER BY a.dataAcquisto", Animale.class).getResultList();

			writer.println(StringUtils.rightPad("Data_Acquisto", 15) + StringUtils.rightPad("Animale", 17)
					+ StringUtils.rightPad("Matricola", 17) + StringUtils.rightPad("Nome_Cliente", 20)
					+ StringUtils.rightPad("Cellulare", 17));
			for (Animale animale : animali) {
				Cliente cliente = animale.getCliente();
				writer.println(StringUtils.rightPad(animale.getDataAcquisto(), 15)
						+ StringUtils.rightPad(animale.getNomeAnimale(), 17)
						+ StringUtils.rightPad(animale.getMatricola(), 17)
						+ StringUtils.rightPad(cliente.getNome() + " " + cliente.getCognome(), 21)
						+ StringUtils.rightPad(cliente.getTelefono(), 17));
			}

			writer.close();
			System.out.println("Report 2 generato con successo!");
		} catch (IOException e) {
			System.out.println("Si è verificato un errore durante la generazione del report: " + e.getMessage());
		}
	}

	public void generaReportTutti() {
		String filePath = "./report/Elenco_Clienti.txt";

		try {
			PrintWriter writer = new PrintWriter(new FileWriter(filePath));

			List<Cliente> clienti = session.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
			for (Cliente cliente : clienti) {
				writer.println("Cliente: " + cliente.getNome() + " " + cliente.getCognome());

				List<Animale> animali = session
						.createQuery("SELECT a FROM Animale a WHERE a.cliente = :cliente", Animale.class)
						.setParameter("cliente", cliente).getResultList();

				writer.println(StringUtils.rightPad("Data_Acquisto", 15) + StringUtils.rightPad("Matricola", 15)
						+ StringUtils.rightPad("Nome", 15) + StringUtils.rightPad("Prezzo di Vendita", 15));

				for (Animale animale : animali) {
					String prezzo = String.valueOf(animale.getPrezzo());
					writer.println(StringUtils.rightPad(animale.getDataAcquisto(), 15)
							+ StringUtils.rightPad(animale.getMatricola(), 15)
							+ StringUtils.rightPad(animale.getNomeAnimale(), 15) + StringUtils.rightPad(prezzo, 15));
				}

				writer.println();
			}

			writer.close();
			System.out.println("Report Totale generato con successo!");
		} catch (IOException e) {
			System.out.println("Si è verificato un errore durante la generazione del report: " + e.getMessage());
		}
	}
}
