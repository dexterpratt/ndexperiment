package parsingenginetests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.utility.BulkFileUploadUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import parsingengineexample.BioPAXParser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class BioPAXParserTest {

	private static ODatabaseDocumentTx db;
	private static String user = "Support";
	
	private static final Logger logger = LoggerFactory.getLogger(BulkFileUploadUtility.class);


	@Test
	public void test() throws Exception {
    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd());
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		
		String userAccount = "reactomeadmin";
		BioPAXParser parser = new BioPAXParser(
				"ca-calmodulin-dependent_protein_kinase_activation.SIF", 
				userAccount,
				db);
		parser.parseFile();

/*		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
				Paths.get("/home/chenjing/working/ndex/networks/reactome46_human"))) 
		{
            for (Path path : directoryStream) {
              logger.info("Processing file " +path.toString());
              BioPAXParser parser2 = new BioPAXParser(path.toString(),userAccount,db);
         		parser2.parseFile();
      		
  			 logger.info("file upload for  " + path.toString() +" finished.");
            }
        } catch (IOException | IllegalArgumentException e) {
        	logger.error(e.getMessage());
        	throw e;
        }
*/		
		db.close();
		
		NdexAOrientDBConnectionPool.close();
	} 

}
