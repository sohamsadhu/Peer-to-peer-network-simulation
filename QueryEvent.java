// File QueryEvent.java
// Referred from : http://www.cs.rit.edu/~ark/730/module02/notes.shtml
// 				 : http://www.cs.rit.edu/~ark/730/mrs02/mrs02.shtml
//               : http://download.oracle.com/javase/tutorial/rmi/overview.html
// From Prof. Alan Kaminsky class notes.

import edu.rit.ds.RemoteEvent ;

/**
 * The class that is supposed to be fired as a query comes to a node.
 * This is the Remote Event class that is passed as a generic in the Event listeners.
 * And in which the data fields are changed when the generator calls on each class.
 */
public class QueryEvent extends RemoteEvent {
	
	private static final long serialVersionUID = 227L ;	// Asks for this since this class is serializable.
	private String nodeId ;
	private String qString ;
	
	/**
	 * Parameterized constructor called in creation of event and passed the 
	 * details of that class.
	 * @param nodeId : The id of the respective node.
	 * @param title : The query string to the node.
	 */
	public QueryEvent( String nodeId, String title ) {
		this.nodeId = nodeId ;
		this.qString = title ;
	}
	
	/**
	 * Returns the nodeID. A get method for string nodeID.
	 * @return the string that contains id of the node where the event was fired.
	 */
	public String getNodeId() {
		return this.nodeId ;
	}
	
	/**
	 * Returns the query string. A get method for string query.
	 * @return the string that contains the query that this node got.
	 */
	public String getQString() {
		return this.qString ;
	}
	
}
