// File: NodeInterface.java
// Referred from : http://www.cs.rit.edu/~ark/730/module02/notes.shtml
// 				 : http://www.cs.rit.edu/~ark/730/mrs02/mrs02.shtml
//               : http://download.oracle.com/javase/tutorial/rmi/overview.html 
// From Prof. Alan Kaminsky class notes and Oracle tutorials.

import java.rmi.Remote ;
import java.rmi.RemoteException ;
import edu.rit.ds.Lease ;
import edu.rit.ds.RemoteEventListener ;

/**
 * NodeInterface class that acts as the Remote interface stub or promise of methods to be delivered
 * to the client or the class Query. Please note that this class extends remote which means it can 
 * generate remote events to which any listener can listen to.
 * This interface is implemented by class Node.
 */
public interface NodeInterface extends Remote {

	// Node gets only the title string and returns a null string or the contents if they 
	// are found. This remote method to throw the remote exception.
	/**
	 * Method makeQuery( String ) that will accept the query from class Query.
	 * @param title: type String, the title of the article to be found.
	 * @ return string the resultant contents of query a empty string if query did not succeed.
	 * @throws RemoteException: a remote method  to throw remote exception.
	 */
	public String makeQuery( String title ) throws RemoteException ;
	
	/**
	 * Used to forward the query from one node to another if the said query was not found locally.
	 * @param title: the string on which the message needs to be searched.
	 * @param nodeid: the id of the node that got the message from client.
	 * @param ts: The sequence number of the query that is forwarded by originating node.
	 * @return the string contents of the query result, a empty string if contents not found.
	 * @throws RemoteException
	 */
	public String forwardQuery( String title, String nodeid, int ts ) throws RemoteException ;
	
	// Adding Listener to the Node Object. Node will report query events to listener.
	/**
	 * This is the method that will bind the remote listeners to the remote event generators present
	 * in this class, which will enable the event logging.
	 * @return Lease for the registration. Basically the time default of 60 seconds.
	 * @param listener the remote event listener that will be hearing events from this class.
	 * @throws RemoteException as any remote method in a remote class would.
	 */
	public Lease addListener( RemoteEventListener< QueryEvent > listener ) 
				 throws RemoteException ;
}
