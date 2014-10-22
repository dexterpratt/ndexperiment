package top;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.NdexStatus;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.Term;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class CommandProcessor {

	private NdexRestClient client;
	private NdexRestClientModelAccessLayer ndex;

	private String queryString = "HRAS";
	private Network queryNetwork = null;
	private Map<Node, String> nodeLabelMap = new HashMap<Node, String>();
	private Map<Term, String> termLabelMap = new HashMap<Term, String>();
	private Map<Citation, List<Support>> citationMap = new HashMap<Citation, List<Support>>();
	private Map<Support, List<Edge>> supportMap = new HashMap<Support, List<Edge>>();
	private File outputFile;
	private PrintStream output;

	public CommandProcessor() {

	}

	public static void main(String[] args) {
		// For the moment, as a proof of concept,
		// we will ignore the arguments and just run
		// a pre-packaged set of queries to the local NDEx,
		// assuming that it is running with the default database.

		CommandProcessor cp = new CommandProcessor();
		//String route = "http://169.228.38.220/rest";
		String route = "http://www.ndexbio.org/rest";
		//String route = "http://test.ndexbio.org/rest";
		if (args.length > 0 && null != args[0]) {
			route = args[0];

		}
		//cp.setClient(new NdexRestClient("ndexadministrator",
		//		"CCB20140829*!ndexSupport", route));
		
		//cp.setClient(new NdexRestClient("ndexopenbeladmin",
		//		"obenbel-321", route));
		
		cp.setClient(new NdexRestClient("dexterpratt",
				"carbluegreen", route));
		
		cp.setNdex(new NdexRestClientModelAccessLayer(cp.getClient()));
		
		
		cp.run(args);

	}

	private void run(String[] args) {
		try {
			printServerStatus();
			outputFile = new File("expOutput.txt");
			output = new PrintStream(outputFile);
			output.println("Results for Query : " + queryString);
			if (ndex.checkCredential()) {
				// Find Selventa full kb
				List<NetworkSummary> searchResults = ndex.findNetworks(
						"Large", null, 0, 10);
				NetworkSummary summary = searchResults.get(0);

				// Query large corpus
				queryNetwork = ndex.getNeighborhood(summary
						.getExternalId().toString(), queryString, 1);
				
				System.out.println("Query Network " + queryNetwork.getEdgeCount() + " edges");

				// Populate Support Map
				for (Edge edge : queryNetwork.getEdges().values()) {
					for (Long supportId : edge.getSupports()) {
						Support support = queryNetwork.getSupports().get(
								supportId);
						List<Edge> edgeList = supportMap.get(support);
						if (null == edgeList) {
							edgeList = new ArrayList<Edge>();
							supportMap.put(support, edgeList);
						}
						edgeList.add(edge);
					}

				}

				// Populate Citation Map
				for (Support support : supportMap.keySet()) {
					Long citationId = support.getCitation();
					Citation citation = queryNetwork.getCitations().get(
							citationId);
					List<Support> supportList = citationMap.get(citation);
					if (null == supportList) {
						supportList = new ArrayList<Support>();
						citationMap.put(citation, supportList);
					}
					supportList.add(support);
				}

				// Iterate over Citation Map
				for (Entry<Citation, List<Support>> citationEntry : citationMap
						.entrySet()) {
					Citation citation = citationEntry.getKey();
					output.println("");
					output.println("=========================================================================");
					output.println("        Citation: " + citation.getIdentifier());
					output.println("=========================================================================");
					output.println("");
					
					// Iterate over supports within citation
					for (Entry<Support, List<Edge>> supportEntry : supportMap
							.entrySet()) {
						Support support = supportEntry.getKey();
						output.println("____________________");
						output.println("Evidence: " + support.getText());
						output.println("");

						// Iterate over edges from the support map
						for (Edge edge : supportEntry.getValue()) {
							
							output.println("      " + getEdgeLabel(edge));
							printEdgeProperties(edge);

						}

					}
				}
			} else {
				System.out.println("Failed to authenticate user");
			}

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private void printEdgeProperties(Edge edge) {
		List<NdexPropertyValuePair> pvs = edge.getProperties();
		for (NdexPropertyValuePair pv : pvs){
			output.println("            " + pv.getPredicateString() + " : " + pv.getValue());		
		}	
	}

	private String getEdgeLabel(Edge edge) {
		Node subject = queryNetwork.getNodes().get(edge.getSubjectId());
		Node object = queryNetwork.getNodes().get(edge.getObjectId());
		BaseTerm predicate = (BaseTerm) getTermById(edge.getPredicateId(),
				"BaseTerm");
		return getNodeLabel(subject) + " " + stripBEL(getTermLabel(predicate)) + " "
				+ getNodeLabel(object);
	}

	// factory.updateNodeLabels = function (network) {
	// network.nodeLabelMap = [];
	// $.each(network.nodes, function (id, node) {
	// network.nodeLabelMap[id] = factory.getNodeLabel(node, network);
	// });
	// };

	private void populateNodeLabelMap() {
		for (Node node : queryNetwork.getNodes().values()) {
			nodeLabelMap.put(node, getNodeLabel(node));
		}
	}

	//
	// factory.getNodeLabel = function (node, network) {
	// //if (!network) network = factory.getNodeNetwork(node);
	// if ("name" in node && node.name && node.name != "") {
	// //console.log(node.name);
	// return node.name;
	// }
	// else if ("represents" in node && node.represents &&
	// network.terms[node.represents])
	// return factory.getTermLabel(network.terms[node.represents], network);
	// else
	// return "unknown"
	// };

	private String getNodeLabel(Node node) {
		if (null != node.getName())
			return node.getName();
		if (null != node.getRepresents()) {
			String termType = node.getRepresentsTermType();
			Term representedTerm = getTermById(node.getRepresents(), termType);
			if (null == representedTerm){
				output.println("Failed to find represented term id " + node.getRepresents() + " for type " + termType);
			} else {
				return getTermLabel(representedTerm);
			}
		}
		return "node:" + node.getId();
	}

	//
	// factory.getTermBase = function (term, network) {
	// if (term.namespace) {
	// var namespace = network.namespaces[term.namespace];
	//
	// if (!namespace || namespace.prefix === "LOCAL")
	// return {prefix: 'none', name: term.name};
	// else if (!namespace.prefix)
	// return {prefix: '', name: term.name};
	// else
	// return {prefix: namespace.prefix, name: term.name};
	// }
	// else {
	// return term.name;
	// }
	//
	// };
	//
	// /*-----------------------------------------------------------------------*
	// * Builds a term label based on the term type; labels rely on Base Terms,
	// * which have names and namespaces. Function Terms can refer to other
	// * Function Terms or Base Terms, and as such must be traversed until a
	// Base
	// * Term is reached.
	// *-----------------------------------------------------------------------*/
	private String getTermLabel(Term term) {
		String label = termLabelMap.get(term);
		if (null != label)
			return label;
		String termType = term.getTermType();
		if (termType.equalsIgnoreCase("BaseTerm")) {
			BaseTerm bt = (BaseTerm) term;
			Long namespaceId = bt.getNamespace();
			if (null == namespaceId) {
				label = bt.getName();
			} else {
				Namespace ns = queryNetwork.getNamespaces().get(namespaceId);
				if (null == ns) {
					label = bt.getName();
				} else if (null != ns.getPrefix()) {
					label = ns.getPrefix() + ":" + bt.getName();
				} else if (null != ns.getUri()) {
					label = ns.getUri() + bt.getName();
				} else {
					label = bt.getName();
				}
			}
		} else if (termType.equalsIgnoreCase("FunctionTerm")) {
			FunctionTerm ft = (FunctionTerm) term;
			BaseTerm function = (BaseTerm) getTermById(ft.getFunctionTermId(),
					"BaseTerm");
			String functionLabel = getTermLabel(function);
			functionLabel = getFunctionAbbreviation(functionLabel);

			// Process parameters
			List<String> parameterLabels = new ArrayList<String>();
			for (Long paramId : ft.getParameters()) {
				Term param = getTermById(paramId);
				parameterLabels.add(stripBEL(getTermLabel(param)));
			}

			// Compose label
			String parameterString = stringsToCsv(parameterLabels);
			label = functionLabel + "(" + parameterString + ")";

		} else if (termType.equalsIgnoreCase("ReifiedEdgeTerm")) {
			ReifiedEdgeTerm rt = (ReifiedEdgeTerm) term;
			Edge reifiedEdge = queryNetwork.getEdges().get(rt.getEdgeId());
			if (null == reifiedEdge){
				label = "(reifiedEdge:" + rt.getEdgeId() + ")";
			} else {
				label = "(" + getEdgeLabel(reifiedEdge) + ")";
			}

		} else {
			label = "term:" + term.getId();
		}
		termLabelMap.put(term, label);
		return label;
	}

	private String stringsToCsv(Collection<String> strings) {
		String resultString = "";
		for (final String string : strings) {
			resultString +=  string + ",";
		}
		resultString = resultString.substring(0, resultString.length() - 1);
		return resultString;

	}
	
	private String stripBEL(String string){
		if (string.startsWith("bel:") || string.startsWith("BEL:")){
			return string.substring(4);
		}
		if (string.startsWith("HGNC:"))
			return string.substring(5);
		
		return string;
	}

	private String getFunctionAbbreviation(String string) {
		String fl = stripBEL(string).toLowerCase();

		switch (fl) {
		case "abundance":
			return "a";
		case "biological_process":
			return "bp";
		case "catalytic_activity":
			return "cat";
		case "complex_abundance":
			return "complex";
		case "pathology":
			return "path";
		case "peptidase_activity":
			return "pep";
		case "protein_abundance":
			return "p";
		case "rna_abundance":
			return "r";
		case "protein_modification":
			return "pmod";
		case "transcriptional_activity":
			return "tscript";
		case "molecular_activity":
			return "act";
		case "degradation":
			return "deg";
		case "kinase_activity":
			return "kin";
		case "substitution":
			return "sub";
		default:
			return fl;
		}

	}

	// factory.getTermLabel = function (term, network) {
	// //if (!network) network = factory.getTermNetwork(term);
	// if (term.termType === "BaseTerm") {
	// if (term.namespace) {
	// var namespace = network.namespaces[term.namespace];
	//
	// if (!namespace || namespace.prefix === "LOCAL")
	// return term.name;
	// else if (!namespace.prefix)
	// return namespace.uri + term.name;
	// else
	// return namespace.prefix + ":" + term.name;
	// }
	// else
	// return term.name;
	// }
	// else if (term.termType === "FunctionTerm") {
	// var functionTerm = network.terms[term.functionTermId];
	// if (!functionTerm) {
	// console.log("no functionTerm by id " + term.functionTermId);
	// return;
	// }
	//
	// var functionLabel = factory.getTermLabel(functionTerm, network);
	// functionLabel = factory.lookupFunctionAbbreviation(functionLabel);
	//
	// var sortedParameters = factory.getDictionaryKeysSorted(term.parameters);
	// var parameterList = [];
	//
	// for (var parameterIndex = 0; parameterIndex < sortedParameters.length;
	// parameterIndex++) {
	// var parameterId = term.parameters[sortedParameters[parameterIndex]];
	// var parameterTerm = network.terms[parameterId];
	//
	// if (parameterTerm)
	// var parameterLabel = factory.getTermLabel(parameterTerm, network);
	// else
	// console.log("no parameterTerm by id " + parameterId);
	//
	// parameterList.push(parameterLabel);
	// }
	//
	// return functionLabel + "(" + parameterList.join(", ") + ")";
	// }
	// else
	// return "Unknown Term Type";
	// };
	//
	// factory.getTermNetwork = function (term) {
	// //TODO
	// return {};
	// }

	private Term getTermById(Long termId) {
		Term term = queryNetwork.getBaseTerms().get(termId);
		if (null != term)
			return term;
		term = queryNetwork.getFunctionTerms().get(termId);
		if (null != term)
			return term;
		term = queryNetwork.getReifiedEdgeTerms().get(termId);
		if (null != term)
			return term;
		return null;
	}

	private Term getTermById(Long termId, String termType) {
		if ("BaseTerm".equalsIgnoreCase(termType))
			return queryNetwork.getBaseTerms().get(termId);
		if ("FunctionTerm".equalsIgnoreCase(termType))
			return queryNetwork.getFunctionTerms().get(termId);
		if ("ReifiedEdgeTerm".equalsIgnoreCase(termType))
			return queryNetwork.getReifiedEdgeTerms().get(termId);
		return null;
	}

	private void printServerStatus() throws Exception {
		try {
			NdexStatus ndexStatus = ndex.getServerStatus();
			System.out.println("NDEx status:\\N------------------");
			System.out.println("Networks: " + ndexStatus.getNetworkCount());
			System.out.println("Groups: " + ndexStatus.getGroupCount());
			System.out.println("Users: " + ndexStatus.getUserCount());
		} catch (IOException e) {
			System.out.println("Error getting server status");
			e.printStackTrace();
			throw new Exception("Error getting server status");
		}

	}

	public void printTermsInNetwork(String searchString, int networkCount)
			throws Exception {
		client = new NdexRestClient("dexterpratt", "insecure");

		System.out.println("Finding up to " + networkCount
				+ " networks by string ' " + searchString + " '");
		List<NetworkSummary> networks = ndex.findNetworks(searchString, null,
				0, networkCount);
		for (NetworkSummary network : networks) {
			System.out.println("\n______\n" + network.getName() + "  id = "
					+ network.getExternalId());

			List<BaseTerm> baseTerms = ndex.getNetworkBaseTerms(network
					.getExternalId().toString(), 0, 50);
			System.out.println(baseTerms.size() + " terms in namespace :");
			int count = 0;
			for (BaseTerm baseTerm : baseTerms) {
				if (count > 10) {
					System.out.print(" ...");
					break;
				}
				System.out.println(" " + baseTerm.getName());
				count++;
			}
		}
	}

	public NdexRestClient getClient() {
		return client;
	}

	public void setClient(NdexRestClient client) {
		this.client = client;
	}

	public NdexRestClientModelAccessLayer getNdex() {
		return ndex;
	}

	public void setNdex(NdexRestClientModelAccessLayer ndex) {
		this.ndex = ndex;
	}

}
