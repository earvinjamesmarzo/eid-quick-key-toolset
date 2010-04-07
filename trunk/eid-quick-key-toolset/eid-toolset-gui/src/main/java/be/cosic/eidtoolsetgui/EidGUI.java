package be.cosic.eidtoolsetgui;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class EidGUI extends javax.swing.JFrame {
	private JMenuBar jMenuBar1;
	private JMenuItem jMenu2Write;
	private JLabel jLabel1;
	private JInternalFrame jInternalFrame1;
	private JScrollPane jScrollPaneIdentityExtra;
	private JScrollPane jScrollPaneCardPin;
	private JScrollPane jScrollPaneCertificates;
	private JScrollPane jScrollPaneIdentity;
	private JTabbedPane jTabbedPane1;
	private JMenuItem jMenu2Read;
	private JMenuItem jMenu1Exit;
	private JMenuItem jMenu1Save;
	private JMenuItem jMenu1Load;
	private JMenu jMenu2;
	private JMenu jMenu1;

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	/**
	* Auto-generated main method to display this JFrame
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				EidGUI inst = new EidGUI();
				inst.setLocationRelativeTo(null);
				inst.setVisible(true);
			}
		});
	}
	
	public EidGUI() {
		super();
		initGUI();
	}
	
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			
			
			{
				jTabbedPane1 = new JTabbedPane();
				getContentPane().add(jTabbedPane1, BorderLayout.CENTER);
				jTabbedPane1.setPreferredSize(new java.awt.Dimension(628, 422));
				{
					
				    /*JTextArea textArea = new JTextArea() {
				      final ImageIcon imageIcon = new ImageIcon("emptyeid.jpg");
				      Image image = imageIcon.getImage();
				      Image grayImage = GrayFilter.createDisabledImage(image);
				      {setOpaque(false);}  // instance initializer
				      public void paintComponent (Graphics g) {
				        g.drawImage(grayImage, 0, 0, this);
				        super.paintComponent(g);
				      }
				    };*/
					jScrollPaneIdentity = new JScrollPane();
					jTabbedPane1.addTab("Identity", null, jScrollPaneIdentity, null);
					{
						jLabel1 = new JLabel();
						
						jScrollPaneIdentity.setViewportView(jLabel1);
						jLabel1.setIcon(new ImageIcon(getClass().getClassLoader().getResource("emptyeid.JPG")));

					}
				}
				{
					jScrollPaneIdentityExtra = new JScrollPane();
					jTabbedPane1.addTab("Identity Extra", null, jScrollPaneIdentityExtra, null);
				}
				{
					jScrollPaneCertificates = new JScrollPane();
					jTabbedPane1.addTab("Certificates", null, jScrollPaneCertificates, null);
				}
				{
					jScrollPaneCardPin = new JScrollPane();
					jTabbedPane1.addTab("Card and PIN", null, jScrollPaneCardPin, null);
				}
			}
			{
				jMenuBar1 = new JMenuBar();
				setJMenuBar(jMenuBar1);
				{
					jMenu1 = new JMenu();
					jMenuBar1.add(jMenu1);
					jMenu1.setText("File");
					{
						jMenu1Load = new JMenuItem();
						jMenu1.add(jMenu1Load);
						jMenu1Load.setText("Load");
					}
					{
						jMenu1Save = new JMenuItem();
						jMenu1.add(jMenu1Save);
						jMenu1Save.setText("Save");
					}
					{
						jMenu1Exit = new JMenuItem();
						jMenu1.add(jMenu1Exit);
						jMenu1Exit.setText("Exit");
					}
				}
				{
					jMenu2 = new JMenu();
					jMenuBar1.add(jMenu2);
					jMenu2.setText("Actions");
					{
						jMenu2Read = new JMenuItem();
						jMenu2.add(jMenu2Read);
						jMenu2Read.setText("Read");
					}
					{
						jMenu2Write = new JMenuItem();
						jMenu2.add(jMenu2Write);
						jMenu2Write.setText("Write");
					}
				}
			}
			pack();
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public JScrollPane getjScrollPaneIdentityExtra() {
		return jScrollPaneIdentityExtra;
	}

	public JScrollPane getjScrollPaneCardPin() {
		return jScrollPaneCardPin;
	}

	public JScrollPane getjScrollPaneCertificates() {
		return jScrollPaneCertificates;
	}

	public JScrollPane getjScrollPaneIdentity() {
		return jScrollPaneIdentity;
	}
	
	
}
