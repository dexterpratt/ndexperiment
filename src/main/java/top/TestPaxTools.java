package top;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;

public class TestPaxTools {

	public static void main(String[] args) throws Exception {
		loadBioPAX("/Users/dextergraphics/biopax/L3/testnfkb.owl", "doom", "none");



	}
	
	public static void loadBioPAX(String filepath, String ownerName, String db) throws Exception{
		// Load a sample test BioPAX File via Simple IO Handler
		File f = new File(filepath);
		String title = f.getName();
		FileInputStream fin = new FileInputStream(f);
		BioPAXIOHandler handler = new SimpleIOHandler();
		Model model = handler.convertFromOWL(fin);
		loadBioPAXModel(model, title, ownerName, db);
	}
	
	public static void loadBioPAXModel(Model model, String title, String ownerName, String db) throws Exception{
		// Create Network
		// NdexPersistenceService persistenceService = new NdexPersistenceService(db);
		//persistenceService.createNewNetwork(ownerName, title, null);
		Set<BioPAXElement> elementSet = model.getObjects();
		for (BioPAXElement bpe : elementSet) {
			if (bpe instanceof Xref){
				// Process Xrefs
				processXREF(bpe);
			} else {
				// Process all Other Elements to create Node objects
				processElementToNode(bpe);
			}	
		}
		
		
		
		// Process all Properties in each Element to create NDExPropertyValuePair and Edge objects
	}
	
	public static void processElementToNode(BioPAXElement bpe){
		String rdfId = bpe.getRDFId();
		String className = bpe.getClass().getName();
		String simpleName = bpe.getModelInterface().getSimpleName();
		System.out.println("Element: " + rdfId + ": " + simpleName);
		
	}
	
	public static void processXREF(BioPAXElement xref){
		String rdfId = xref.getRDFId();
		String name = xref.getClass().getName();
		if (xref instanceof PublicationXref){
			System.out.println("Citation: " + rdfId + ": " + name );
			
		} else if (xref instanceof UnificationXref){
			System.out.println("BaseTerm (u): " + rdfId + ": " + name );
			
		} else if (xref instanceof RelationshipXref){
			System.out.println("BaseTerm (r): " + rdfId + ": " + name );
		} else {
			System.out.println("Unexpected xref of type: " + name);
		}
	}

	// public static String[][] PrintProperties(BioPAXElement bpe)
	public static void printProperties(BioPAXElement bpe) {
		// In order to use properties we first need an EditorMap
		// EditorMap editorMap = SimpleEditorMap.L3;
		EditorMap editorMap = SimpleEditorMap.L3;
		// And then get all the editors for our biopax element
		Set<PropertyEditor> editors = editorMap.getEditorsOf(bpe);
		// Let's prepare a table to return values
		// String value[][] = new String[editors.size()][2];
		// int row = 0;
		// For each property
		for (PropertyEditor editor : editors) {
			// First column is the name of the property, e.g. "Name"
			// value[row][0] = editor.getProperty();
			for (Object val : editor.getValueFromBean(bpe)) {
				System.out.println("       " + editor.getProperty() + " : ("
						+ val.getClass().getName() + ") " + val.toString());
			}

			// For each property that has a value or values, we want to see if
			// each value is a literal or a resource
			// If its a resource, we make an edge, otherwise we make a property

			// Second column is the value e.g. "p53"
			// value[row][1] = editor.getValueFromBean(bpe).toString();
			// System.out.println(editor.getValueFromBean(bpe).toString());
			// increase the row index
			// row++;
		}
		// return value;
	}
	
	public static void handleElement(BioPAXElement bpe){
		
	}
	
	public static void handleElementProperty(BioPAXElement bpe, PropertyEditor editor){
		
	}

}
