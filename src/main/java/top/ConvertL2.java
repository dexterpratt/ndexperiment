package top;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.biopax.paxtools.converter.LevelUpgrader;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;

public class ConvertL2 {

	public static void main(String[] args) throws Exception {
		convertL2("/Users/dextergraphics/biopax/L2/testnfkb.owl", "/Users/dextergraphics/biopax/L3/testnfkb.owl");

	}
	
	public static void convertL2(String filepathL2, String filepathL3) throws Exception{
		// Load a sample test BioPAX File via Simple IO Handler
		File source = new File(filepathL2);
		File target = new File(filepathL3);
		FileInputStream fin = new FileInputStream(source);
		FileOutputStream fout = new FileOutputStream(target);
		BioPAXIOHandler handler = new SimpleIOHandler();
		Model model = handler.convertFromOWL(fin);
		LevelUpgrader up = new LevelUpgrader();
		model = up.filter(model);
		handler.convertToOWL(model, fout);
	}
	

}
