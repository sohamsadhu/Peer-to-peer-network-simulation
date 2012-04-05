// File: Query.java
// Referred from : http://www.cs.rit.edu/~ark/730/module02/notes.shtml
// 				 : http://www.cs.rit.edu/~ark/730/mrs02/mrs02.shtml
//               : http://download.oracle.com/javase/tutorial/rmi/overview.html
// From Prof. Alan Kaminsky class notes and Oracle tutorials.

import edu.rit.ds.registry.RegistryProxy ;
import edu.rit.ds.registry.NotBoundException ;
import java.rmi.RemoteException ;

/**
 * Class Query that is the client program that checks in the RMI registry for Node provided
 * and forwards its' query to that node.
 * Usage: java Query <host> <port> <id> <title>
 */
public class Query {
	
	/**
	 * Method usage() just says if the said input were correctly entered or not.
	 * @param port: Needs to only know that port is a valid integer.
	 * @param args_length: Needs to know that no extra or any valid number of arguments missing.
	 */
	public void usage( String port, int args_length ) {
		if( args_length > 4 ) {	// Check if extra arguments have been sent.
			System.out.println("Usage: java Query <host> <port> <id> \"<title>\"") ;
			System.out.println("The number of arguments are more than required.") ;
			System.exit( 0 ) ; // There is a exception give message and then exit.
		}
		try {	// Check if the port number is a valid argument.
			Integer.parseInt( port ) ;
		} catch( NumberFormatException nfe ) {
			System.out.println("Usage: java Query <host> <port> <id> \"<title>\"") ;
			System.err.println("The argument for the port is not a integer. " + nfe.getMessage() ) ;
			System.exit( 0 ) ; // There is a exception give message and then exit.
		}
	}
	
	/**
	 * Method query() that forwards the query to the given node.
	 * @param host: the host where the RMI server is running.
	 * @param port: the port on the host where the RMI server can be found.
	 * @param id: The id of the node to which to forward query
	 * @param title: The title that is required or looking for.
	 * @return string contents for the article with that title.
	 */
	public String query( String host, String port, String id, String title ) {
		String contents = new String( "" ) ;	// Make the string to return.
		int s_port = Integer.parseInt( port ) ;	// Convert the registry server port number to integer.
		try {
			RegistryProxy proxy = new RegistryProxy( host, s_port ) ;	// Get a registry proxy.
			NodeInterface node = ( NodeInterface )proxy.lookup( id ) ;	// Look up the registry for 
												// node interface implementation node with the id.
			contents = node.makeQuery( title ) ;	// Make the query get contents. 
			return contents ;
		} catch( RemoteException rex ) {
			System.err.println("There is no registry server at the mentioned host/port. \n"
					           + rex.getMessage() ) ;
			System.exit( 0 ) ; // Not a exact abnormal termination.
		} catch( NotBoundException nbex ) {
			System.err.println( "There does not exist a node with id " + id + ".\n" 
					            + nbex.getMessage() ) ;
			System.exit( 0 ) ; // Not a exact abnormal termination.
		}
		return contents ;	// Fail safe. If exception succeeds still a empty string sent back. 
	}
	
	/**
	 * Method print() just prints the title and article associated with it if any.
	 * @param contents: contents of the article for the respective title.
	 * @param title: The request forwarded with.
	 */
	public void print( String contents, String title ) {
		if( contents.equals( "" ) ) {	       // If contents string empty either a exception
			System.out.println( title ) ;	   // article not found.
			System.out.println("Not found") ;
		} else {							   // Else print whatever you found.	
			System.out.println( title ) ;
			System.out.println( contents ) ;
		}
	}
	
	/**
	 * The main() method or the starting point for all work of the class.
	 * @param args: The arguments provided via command line.
	 */
	public static void main( String [] args ) {
		Query q = new Query() ;
		if( args.length < 4 ) {	 // Check if the arguments are missing.
			System.out.println("Usage: java Query <host> <port> <id> \"<title>\"") ;
			System.out.println("The required number of arguments are missing.") ;
			System.exit( 0 ) ; // There is a exception give message and then exit.
		}
		q.usage( args[ 1 ], args.length ) ;	// Usage only needs to know if port is int and #args.
		String contents = q.query( args[ 0 ], args[ 1 ], args[ 2 ], args[ 3 ] ) ; // Do the query.
		q.print( contents, args[ 3 ] ) ;	// Once got the contents print contents.
	}
}
