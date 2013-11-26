package cuki.opc;

import javafish.clients.opc.JOpc;
import javafish.clients.opc.browser.JOpcBrowser;
import javafish.clients.opc.component.OpcGroup;
import javafish.clients.opc.component.OpcItem;
import javafish.clients.opc.exception.CoInitializeException;
import javafish.clients.opc.exception.CoUninitializeException;
import javafish.clients.opc.exception.ComponentNotFoundException;
import javafish.clients.opc.exception.ConnectivityException;
import javafish.clients.opc.exception.SynchReadException;
import javafish.clients.opc.exception.SynchWriteException;
import javafish.clients.opc.exception.UnableAddGroupException;
import javafish.clients.opc.exception.UnableAddItemException;
import javafish.clients.opc.exception.UnableBrowseBranchException;
import javafish.clients.opc.exception.UnableIBrowseException;
import javafish.clients.opc.exception.UnableRemoveGroupException;
import javafish.clients.opc.exception.VariantTypeException;
import javafish.clients.opc.variant.Variant;
import cuki.opc.ItensOPC;
import cuki.utils.BitField;

public class ServidorOPC {

	private String serverClientHandle;
	private String host;
	private String server;
	private JOpc jopc;
	private OpcGroup group;
	private String[] servidores;
	private ItensOPC[] itensPivo;

	public ServidorOPC() {
		this("localhost", "Atos.OPCConnect.1", "JOpcAtos1");
	}

	public ServidorOPC(String host, String server, String serverClientHandle) {

		this.host = host;
		this.server = server;
		this.serverClientHandle = serverClientHandle;

		jopc = new JOpc(this.host, this.server, this.serverClientHandle);
		group = new OpcGroup("group1", true, 500, 0.0f);
	}

	private String[] getServers() throws ConnectivityException,
			UnableBrowseBranchException, UnableIBrowseException,
			CoUninitializeException {

		String[] retorno = null;

		JOpcBrowser opcBrowser = new JOpcBrowser(this.host, this.server,
				this.serverClientHandle);

		try {
			JOpcBrowser.coInitialize();
		} catch (CoInitializeException e) {
			throw new CoInitializeException(
					"Falha ao buscar equipamentos de comunica��o");
		}

		try {
			opcBrowser.connect();
		} catch (ConnectivityException e) {
			throw new ConnectivityException(this.host + " " + this.server + " "
					+ this.serverClientHandle);
		}

		try {
			retorno = opcBrowser.getOpcBranch("");
		} catch (UnableBrowseBranchException e) {
			throw new UnableBrowseBranchException(
					"Falha ao buscar equipamentos de comunica��o (2)");
		} catch (UnableIBrowseException e) {
			throw new UnableIBrowseException(
					"Falha ao buscar equipamentos de comunica��o (3)");
		}

		try {
			JOpcBrowser.coUninitialize();
		} catch (CoUninitializeException e) {
			throw new CoUninitializeException("Falha ao encerrar OPC Browser");
		}

		return retorno;
	}

	public void connectAndRegister() throws ConnectivityException,
			UnableAddGroupException, UnableAddItemException,
			UnableBrowseBranchException, UnableIBrowseException,
			CoUninitializeException {

		try {
			this.servidores = getServers();
		} catch (UnableBrowseBranchException e) {
			throw e;
		} catch (UnableIBrowseException e) {
			throw e;
		} catch (CoUninitializeException e) {
			throw e;
		}

		itensPivo = new ItensOPC[servidores.length];

		int cont = 0;
		// for (String servidor : servidores) {
		// itensPivo[cont++] = new ItensOPC(servidor, group, jopc);
		// }

		for (String servidor : servidores) {

			itensPivo[cont] = new ItensOPC(servidor);

			for (OpcItem item : itensPivo[cont++].createItens(servidor))
				group.addItem(item);
		}

		jopc.addGroup(group);

		JOpc.coInitialize();

		try {
			jopc.connect();
			System.out.println("JOPC client is connected...");
		} catch (ConnectivityException e) {
			throw new ConnectivityException("Not Connected!");
		}

		try {
			jopc.registerGroups();
			System.out.println("OPCGroup are registered...");
		} catch (UnableAddGroupException e) {
			throw new UnableAddGroupException("No group added!");
		} catch (UnableAddItemException e) {
			throw new UnableAddItemException("No item added!");
		}
	}

	public String[] getServidores() {
		return this.servidores;
	}

	public void disconnect() throws ComponentNotFoundException,
			UnableRemoveGroupException {
		try {
			jopc.unregisterGroup(group);
		} catch (ComponentNotFoundException e) {
			throw new ComponentNotFoundException("Disconnect Error");
		} catch (UnableRemoveGroupException e) {
			throw new UnableRemoveGroupException("Remove group error");
		}

		JOpc.coUninitialize();

		jopc = null;

		System.out.println("Conex�o encerrada");
	}

	public void syncItens() throws ComponentNotFoundException,
			SynchReadException {
		jopc.synchReadGroup(group);
	}

	private int parseItem(OpcItem item) throws NumberFormatException {

		int retorno = 0;
		for (String aux : item.toString().split(";")) {
			if (aux.contains("itemValue")) {
				for (String aux2 : aux.split(" = ")) {
					if (!aux2.contains("itemValue")) {
						try {
							if (item.getItemName().contains("ASYNC"))
								retorno = Integer.parseInt(aux2, 16);
							else
								retorno = Integer.parseInt(aux2);
						} catch (NumberFormatException e) {
							throw new NumberFormatException("Formato errado");
						}
					}
				}
			}
		}
		return retorno;
	}

	@SuppressWarnings("unused")
	private int getResponse2(OpcItem item) throws ComponentNotFoundException,
			SynchReadException {
		int retorno = 0;

		try {
			OpcItem response = jopc.synchReadItem(group, item);
			retorno = parseItem(response);
		} catch (ComponentNotFoundException e) {
			throw new ComponentNotFoundException(item.getItemName()
					+ ": No component found");
		} catch (SynchReadException e) {
			throw new SynchReadException(item.getItemName()
					+ " Synch read error");
		}

		return retorno;
	}

	private int getResponse(OpcItem item) {

		int retorno = 0;
		for (OpcItem aux : group.getItemsAsArray())
			if (aux.getItemName().equals(item.getItemName()))
				retorno = parseItem(aux);

		return retorno;
	}

	private int indice(String pivo) {
		int cont = 0;
		for (ItensOPC item : itensPivo) {
			if (pivo.equals(item.getPivo()))
				break;
			else
				cont++;
		}
		return cont;
	}

	public int getTempoRestanteMinutos(String pivo)
			throws ComponentNotFoundException, SynchReadException {
		return getResponse(itensPivo[indice(pivo)].getTempoRestanteMinutos());
	}

	public int getanguloAtual(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getAnguloAtual());
	}

	public int getcicloAtual(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getCicloAtual());
	}

	public int getnrSetores(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getNrSetores());
	}

	public int getcontaFase(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getContaFase());
	}

	public int getcontaSetor(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getContaSetor());
	}

	public int getnrFases(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getNrFases());
	}

	public int getlaminaGet(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getLaminaGet());
	}

	public int gettempoRestanteHoras(String pivo)
			throws ComponentNotFoundException, SynchReadException,
			VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getTempoRestanteHoras());
	}

	public int getword0(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getWord0());
	}

	public int getword4(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getWord4());
	}

	public int getword6(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getWord6());
	}

	public int getstatusPivo(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getStatusPivo());
	}

	public int getsetorIndice(String pivo) throws ComponentNotFoundException,
			SynchReadException, VariantTypeException {
		return getResponse(itensPivo[indice(pivo)].getSetorIndice());
	}

	public OpcGroup getGroup() {
		return this.group;
	}

	public JOpc getJOpc() {
		return this.jopc;
	}

	public ItensOPC getItens(String Pivo) {
		for (ItensOPC item : itensPivo) {
			if (item.getPivo().equals(Pivo))
				return item;
		}
		return null;
	}

	private void writeItem(OpcItem item, int value) throws SynchWriteException,
			ComponentNotFoundException {

		Variant varIn = new Variant(value);
		item.setValue(varIn);

		try {
			jopc.synchWriteItem(group, item);
		} catch (ComponentNotFoundException e) {
			throw new ComponentNotFoundException("N�o Encontrado: "
					+ item.getItemName());
		} catch (SynchWriteException e) {
			throw new SynchWriteException("Erro de escrita "
					+ item.getItemName());
		}
	}

	public void writeSetorIndice(String pivo, int value)
			throws SynchWriteException, ComponentNotFoundException {
		try {
			writeItem(itensPivo[indice(pivo)].getSetorIndice(), value);
		} catch (ComponentNotFoundException e) {
			throw e;
		} catch (SynchWriteException e) {
			throw e;
		}
	}

	public void iniciaIrrigacao(String pivo) throws SynchWriteException,
			ComponentNotFoundException, SynchReadException {

		BitField word4;

		try {
			word4 = new BitField(getword4(pivo));
		} catch (SynchReadException | VariantTypeException e) {
			throw new SynchReadException(pivo + " word4");
		}

		word4.setBit(BitField.inicioIrriga);
		writeItem(itensPivo[indice(pivo)].getWord4(), word4.getByte());
	}
}
