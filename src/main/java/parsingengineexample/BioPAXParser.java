package parsingengineexample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.common.util.TermStringType;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.ProvenanceHelpers;
import org.ndexbio.task.parsingengines.IParsingEngine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;


public class BioPAXParser implements IParsingEngine {
	private final File bioPAXFile;
	private final String bioPAXURI;
	private List<String> msgBuffer;

	private static Logger logger = Logger.getLogger("BioPAXParser");

	private NdexPersistenceService persistenceService;

	public BioPAXParser(String fn, String ownerName, NdexDatabase db) throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
				"A network owner name is required");
		this.msgBuffer = Lists.newArrayList();
		if ( fn.startsWith("/") || fn.matches("^[a-zA-Z]:.*")) 
			this.bioPAXFile = new File(fn);
		else
		    this.bioPAXFile = new File(getClass().getClassLoader().getResource(fn).toURI());
		this.bioPAXURI = bioPAXFile.toURI().toString();
		this.persistenceService = new NdexPersistenceService(db);
		
		String title =  Files.getNameWithoutExtension(this.bioPAXFile.getName());

		persistenceService.createNewNetwork(ownerName, title, null);
		
		this.addBioPAXNamespaces();

	}

	public List<String> getMsgBuffer() {
		return this.msgBuffer;
	}

	public String getBioPAXURI() {
		return bioPAXURI;
	}

	public File getBioPAXFile() {
		return bioPAXFile;
	}

	@Override
	public void parseFile() throws  NdexException {
		BufferedReader bufferedReader = null;
		try {

			this.getMsgBuffer().add("Parsing lines from " + this.getBioPAXURI());

			this.processBioPAX(this.getBioPAXFile());

			//add provenance to network
			NetworkSummary currentNetwork = this.persistenceService.getCurrentNetwork();
			
			String uri = NdexDatabase.getURIPrefix();

			ProvenanceEntity provEntity = ProvenanceHelpers.createProvenanceHistory(currentNetwork,
					uri, "FILE_UPLOAD", currentNetwork.getCreationTime(), (ProvenanceEntity)null);
			provEntity.getCreationEvent().setEndedAtTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
			
			List<SimplePropertyValuePair> l = provEntity.getCreationEvent().getProperties();
			l.add(	new SimplePropertyValuePair ( "filename",this.bioPAXFile.getName()) );
			
			this.persistenceService.setNetworkProvenance(provEntity);
			
			// close database connection
			this.persistenceService.persistNetwork();
			
		} catch (Exception e) {
			// delete network and close the database connection
			e.printStackTrace();
			this.persistenceService.abortTransaction();
			throw new NdexException("Error occurred when loading file " +
					this.bioPAXFile.getName() + ". " + e.getMessage() );
		} finally {
			if ( bufferedReader != null )
				try {
					bufferedReader.close();
				} catch (IOException e) {}
		}
	}
	
	private void processBioPAX(File f) throws Exception{
		FileInputStream fin = new FileInputStream(f);
		BioPAXIOHandler handler = new SimpleIOHandler();
		Model model = handler.convertFromOWL(fin);
		this.loadBioPAXModel(model);
	}
	
	private void loadBioPAXModel(Model model) throws Exception{
		// Create Network
		// NdexPersistenceService persistenceService = new NdexPersistenceService(db);
		//persistenceService.createNewNetwork(ownerName, title, null);
		Set<BioPAXElement> elementSet = model.getObjects();
		for (BioPAXElement bpe : elementSet) {
			if (bpe instanceof Xref){
				// Process Xrefs
				this.processXREFElement(bpe);
			} else {
				// Process all Other Elements to create Node objects
				this.processElementToNode(bpe);
			}	
		}
		// Process all Properties in each Element to create NDExPropertyValuePair and Edge objects
	}
	
	private void processElementToNode(BioPAXElement bpe){
		String rdfId = bpe.getRDFId();
		String className = bpe.getClass().getName();
		String simpleName = bpe.getModelInterface().getSimpleName();
		System.out.println("Element: " + rdfId + ": " + simpleName);
		
	}
	
	private void processXREFElement(BioPAXElement xref){
		String rdfId = xref.getRDFId();
		String name = xref.getClass().getName();
		if (xref instanceof PublicationXref){
			System.out.println("Citation: " + rdfId + ": " + name );
			PublicationXref pubXref = (PublicationXref) xref;
			String title = pubXref.getTitle();
			
			// Create the citation
			Long citationId = this.persistenceService.getCitationId(
						"", 
						NdexPersistenceService.defaultCitationType,
						NdexPersistenceService.pmidPrefix + pubmedIdTokens[1], 
						null
						);
			
			// need to map the ndex element id to the bioPAX element id
				//	this.pubmedIdSet.add(pubmedIdTokens[1]);
					
		
			
			
		} else if (xref instanceof UnificationXref){
			System.out.println("BaseTerm (u): " + rdfId + ": " + name );
			
		} else if (xref instanceof RelationshipXref){
			System.out.println("BaseTerm (r): " + rdfId + ": " + name );
		} else {
			System.out.println("Unexpected xref of type: " + name);
		}
	}
	
	private handleXrefProperty(long elementId, long ){
		
		Long elementId = null
		this.persistenceService.addCitationToElement(edgeId, citationId, NdexClasses.Edge);
	}

	private Long addNode(String name) throws ExecutionException, NdexException {
		TermStringType stype = TermUtilities.getTermType(name);
		if ( stype == TermStringType.NAME) {
			return persistenceService.getNodeIdByName(name);
		} 
		return persistenceService.getNodeIdByBaseTerm(name);
		
	}

	private Long addEdge(String subject, String predicate, String object)
			throws ExecutionException, NdexException {
		Long subjectNodeId = addNode(subject);
		Long objectNodeId = addNode(object);
		Long predicateTermId = persistenceService.getBaseTermId(predicate);
		return persistenceService.getEdge(subjectNodeId, objectNodeId,
				predicateTermId, null,null,null);

	}
	
	private void addBioPAXNamespaces(){
		
	}

	// fragments
	/*
	 
	this.getMsgBuffer().add(e.getMessage()); 
	
	---------
	
						counter ++;
					if ( counter % 2000 == 0 ) {
						logger.info("processed " + counter + " lines so far. commit this batch.");
						this.persistenceService.commit();
					}
	 
	 -----------
	 
	 Long citationId = this.persistenceService.getCitationId(
										"", NdexPersistenceService.defaultCitationType,
										NdexPersistenceService.pmidPrefix + pubmedIdTokens[1], null);
								//	this.pubmedIdSet.add(pubmedIdTokens[1]);
									this.persistenceService.addCitationToElement(edgeId, citationId, NdexClasses.Edge);
									
	----------
	
	Long participantNodeId = this.persistenceService.getNodeIdByBaseTerm(participantIdentifier);
	
	----------
	
	if (tokens.length > 3) {
						String[] unificationAliases = tokens[3].split(";");
						this.persistenceService.addAliasToNode(participantNodeId,unificationAliases);
						if (tokens.length > 4) {
							String[] relationshipAliases = tokens[4].split(";");
							this.persistenceService.addRelatedTermToNode(participantNodeId, relationshipAliases);
						}
					}
					
	-------------
	
	                NdexPropertyValuePair p = new NdexPropertyValuePair ("URI", values[2]);
                props.add(p);
                
    -------------
    
    
    this.persistenceService.setNetworkProperties(props, null);
    
    
    -------------
    
    this.persistenceService.createNamespace2("UniProt", 	"http://identifiers.org/uniprot/");
                
	 */


}
