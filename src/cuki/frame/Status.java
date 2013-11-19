package cuki.frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.miginfocom.swing.MigLayout;

import java.awt.Toolkit;
import java.io.File;

@SuppressWarnings("serial")
public class Status extends JFrame {

	private Oval oval = null;
	private Mostrador most = null;
	private AnguloAtual ang = null;
	private IO plcIO = null;

	private boolean sentido = true;

	public Status() {

		setIconImage(Toolkit.getDefaultToolkit().getImage(
				Status.class.getResource("/cuki/frame/images/logo-K.png")));

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Teste Supervisorio");
		setResizable(false);

		MigLayout layout = new MigLayout("", "[][]", "[][][]");
		getContentPane().setLayout(layout);

		JMenuBar menuBar = constructBar();
		getContentPane().add(menuBar, "cell 0 0 2 1");

		oval = new Oval();
		getContentPane().add(oval, "cell 0 1,grow");

		most = new Mostrador();
		getContentPane().add(most, "cell 1 1 1 2,grow");

		ang = new AnguloAtual();
		getContentPane().add(ang, "cell 0 2,grow");
		ang.getBto().addActionListener(new MudarSentido());

		pack();

	}

	private JMenuBar constructBar() {
		JMenu menu;
		JMenuBar menuBar = new JMenuBar();
		JMenuItem menuItem;

		menu = new JMenu("Arquivo");
		menuBar.add(menu);

		menuItem = menu.add(new JMenuItem("Sair"));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		menuItem = menu.add(new JMenuItem("Carregar configuração OPC"));
		menuItem.addActionListener(new ActionListener() {

			JFileChooser fc = new JFileChooser();

			@Override
			public void actionPerformed(ActionEvent e) {
				int retorno = fc.showOpenDialog(getParent());

				if (retorno == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					System.out.println(file);
				}
			}
		});

		menu = new JMenu("Vizualizar");
		menuBar.add(menu);

		menuItem = menu.add(new JMenuItem("Entradas e Saidas"));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				plcIO = new IO();
				plcIO.setVisible(true);
			}
		});

		return menuBar;
	}

	public void setAngulo(int angulo) {
		oval.setAngulo(angulo);
		ang.setAngulo(angulo);
	}

	public Mostrador getMostrador() {
		return this.most;
	}

	public void setword(int word0, int word4, int word6) {
		if (plcIO != null)
			plcIO.setBytes(word0, word4, word6);
	}

	private class MudarSentido extends Object implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			sentido = !sentido;
			ang.setSentido(sentido);
		}
	}
}
