package be.cosic.eidtoolset.engine;


import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;



import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.smartcardio.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;


import be.cosic.eidtoolset.gui.*;

import be.cosic.eidtoolset.exceptions.AIDNotFound;
import be.cosic.eidtoolset.exceptions.GeneralSecurityException;
import be.cosic.eidtoolset.exceptions.InvalidResponse;
import be.cosic.eidtoolset.exceptions.NoCardConnected;
import be.cosic.eidtoolset.exceptions.NoReadersAvailable;
import be.cosic.eidtoolset.exceptions.NoSuchFeature;
import be.cosic.eidtoolset.exceptions.SmartCardReaderException;
import be.cosic.eidtoolset.exceptions.UnknownCardException;
import be.cosic.util.TextUtils;


import be.cosic.eidtoolset.eidlibrary.*;


/**
 * 
 * Code written by Gauthier Van Damme for COSIC
 *  
 */
@SuppressWarnings("restriction")
public class Engine extends Component{

	 BufferedImage img = null;
	
	public Engine(){
		
	        try {
	        	
	        	BelpicCard belpicCard = null;
	    		
	    			belpicCard = new BelpicCard("eidTool");
	    			
	    			byte[] id = belpicCard.readCitizenIdentityDataBytes();
	    			
	    			
	    		
	    			
	    		//byte[] atr = belpicCard.getATR();
	    		
	    		//System.out.println("card ATR: " + TextUtils.hexDump(atr));
	            
	    		
	    		//Test the eid functions: all implemented?
	    			
	    		//Junit for testing them???
	    			
	    			
	    			
	    		//Test the write functions
	    		/*
	    		byte[] id = belpicCard.readCitizenIdentityDataBytes();
	    			
	    		Hashtable table = new Hashtable();
	            
	            IdentityDataParser.ParseIdentityData(id,table);
	            
	            System.out.println("data of cardholder in array: " + TextUtils.hexDump(id));
	            System.out.println("datalength: " + id.length);
	            System.out.println("parsed data of the cardholder: " + table.toString());
	    		
	            String str = "Van Damme";
	            
	            table.put("Name",str);
	            
	            id = IdentityDataParser.ParseHashTableToIdentityData(table);
	            
	            System.out.println("new data of cardholder in array: " + TextUtils.hexDump(id));
	            System.out.println("datalength: " + id.length);   
	            
	            IdentityDataParser.ParseIdentityData(id,table);
	            
	            System.out.println("newly parsed data of the cardholder: " + table.toString());
	    		
	            //testen writen!!
	            belpicCard.writeCitizenIdentityDataBytes(id);
	            
	            /*
    			byte[] ad = belpicCard.readCitizenAddressBytes();
    			
	    		Hashtable table = new Hashtable();
	            
	            IdentityDataParser.ParseIdentityAddressData(ad,table);
	            
	            System.out.println("data of cardholder in array: " + TextUtils.hexDump(ad));
	            System.out.println("datalength: " + ad.length);
	            System.out.println("parsed data of the cardholder: " + table.toString());
	    		
	            String str = "8000";
	            
	            table.put("Zip code",str);
	            
	            ad = IdentityDataParser.ParseHashTableToAddressData(table);
	            
	            System.out.println("data of cardholder in array: " + TextUtils.hexDump(ad));
	            System.out.println("datalength: " + ad.length);   
	            
	            IdentityDataParser.ParseIdentityAddressData(ad,table);
	            
	            System.out.println("newly parsed data of the cardholder: " + table.toString());
	    		*/
	            
	            
	           
	            
	    		
	    			
	    			
	    		/*
	    		byte[] id = belpicCard.readCitizenIdentityDataBytes();
	    		
	    		
	            Hashtable table = new Hashtable();
	            
	            IdentityDataParser.ParseIdentityData(id,table);
	            
	    		//System.out.println("data of the cardholder: " + TextUtils.hexDump(id)); 
	            
	            System.out.println("parsed data of the cardholder: " + table.toString());
	    		
	            
	            
	            byte[] photo = belpicCard.readCitizenPhotoBytes();
	            
	            //System.out.println("picture of the cardholder: " + TextUtils.hexDump(photo));
	            
	            //ImageInputStream in = (ImageInputStream) new ByteArrayInputStream(photo);
	            //img = ImageIO.read(in);

	           
	            /*FileOutputStream fos = new FileOutputStream("sample.jpg");
	            fos.write(photo, 0, photo.length);
	            
	        	 
	        	 
	        	 img = (BufferedImage) Toolkit.getDefaultToolkit().createImage(photo);*/
	        
	        } catch (Exception e) {
				System.out.println("Exception"); 
				e.printStackTrace();
			
			}
			
//			} catch (IOException e) {
//				System.out.println("Can not read inputstream!");
//				e.printStackTrace();
			
			
	}
    
	/**
	 * @param args
	 * @throws IOException 
	 * @throws SmartCardReaderException 
	 * @throws UnknownCardException 
	 */
	public static void main(String[] args) throws IOException {//throws UnknownCardException, SmartCardReaderException {

		//For testing without GUI
		
		
        
		new Engine();
        
        /*JFrame f = new JFrame("Load Image Sample");
        
        f.addWindowListener(new WindowAdapter(){
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        
        f.add(new Engine());
        
        //System.out.println("number of components on the frame: " + f.getComponentCount());

        
        f.pack();
        
        f.setVisible(true);*/
        
		
		
		
		
		//System.out.println("0x9000 in int is: " + (Integer)0x9000);
		
		//gebruik 
		/*try{
		
		
		List readers = TerminalFactory.getInstance("PC/SC", null).terminals().list(); 
		
		if (readers.isEmpty()){
			System.out.println("no readers in list");
			throw new Exception();
		
		}
		
		CardTerminal reader;
		Card eid;
		
		int numberOfReaders = readers.size();
		System.out.println("number of readers found: " + numberOfReaders); 
		for(int i = 0; i<numberOfReaders; i++){
			reader = (CardTerminal)readers.get(i);
			System.out.println("reader number: " +i + " found. Name is: " + reader.getName());
		
			
		}*/
		
		
		//reader = (CardTerminal)readers.get(1);
		
		
		//eid = reader.connect("T=0");
		
		//System.err.println("Card ATR is <"
		//		+ eid.getATR().getBytes() + ">");
		
		//CardChannel conn = eid.getBasicChannel();//Or eid.getLogicalChannel()
		
		//System.out.println("channel name to nokia card: " + conn.getChannelNumber());
		
		//methode maken : selectEidApplet(byte[] Aid);
		
		//en dan in channel apdu's versturen!
		//CommandAPDU apdu = CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int dataOffset, int dataLength, int ne);
		//ResponseAPDU respons = conn.transmit(apdu);
		
		
		
		//op einde: disconnect
		//eid.disconnect(false);
		
//		}catch(Exception e){
//			System.out.println("exception during search cardreaders: " + e);
//		}
		
		
		
/////////////////////////////////////////////////////////////////////////////////////////////////////
		// for testing cardreaders, remove everything between this en next todo 
		/*
		byte[] ATR;
		int i =0;
		while(true){
			
			
				//Be carefull: this will always give back something different from null and no exceptions thrown)
			CadClientInterface cad	= CadDevice.getPCSCClientInstance(i);
			
			try{
			ATR = cad.powerUp();
			
			if(cad == null)//never reached for any i
			System.out.println("reader number: " +i + " found.");
			else throw new Exception();
			
			//cad.powerUp();
		
		
		
		
			i = i + 1;
			}catch(Exception e){
				System.out.println("exception during search cadclientineterface number: " + i);
				return;
			}
		}*/
		/////////////////////////////////////////////////////////////////////////////////////////
		
		

	}

	
}
