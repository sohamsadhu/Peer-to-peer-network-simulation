// File: Log.java
// Referred from : http://www.cs.rit.edu/~ark/730/module02/notes.shtml
// 				 : http://www.cs.rit.edu/~ark/730/mrs02/mrs02.shtml
//               : http://download.oracle.com/javase/tutorial/rmi/overview.html
// From Prof. Alan Kaminsky class notes and Oracle tutorials.

import edu.rit.ds.registry.RegistryEvent;
import edu.rit.ds.registry.RegistryProxy ;
import edu.rit.ds.RemoteEventListener ;
import java.rmi.RemoteException ;
import edu.rit.ds.registry.RegistryEventListener ;
import edu.rit.ds.registry.NotBoundException ;
import java.rmi.server.UnicastRemoteObject ;
import edu.rit.ds.registry.RegistryEventFilter ;

/**
 * Class Log extends RemoteEvent and implements RegistryEventListener.
 * It needs the event to know what event went in the Registry server.
 * It also at the same time listens for the event.
 * Usage: java Log <host> <port>
 */
public class Log {
	
	private RegistryProxy proxy ;
	private static final long serialVersionUID = 227L ;	// Asks for this since this class is serializable.
	
	/**
	 * The class nodeListener that will be fired every time a event is generated in the node.
	 * When the event generator fires this class is created and then the method is executed for
	 * printing the messages on the terminal or standard output.
	 */
	private RemoteEventListener< QueryEvent > nodeListener = 
		new RemoteEventListener< QueryEvent >() {
			public void report( long seqNum, QueryEvent theEvent ) {
				System.out.println("Node " + theEvent.getNodeId() + " -- query "
						+ theEvent.getQString() ) ;
			}
	} ;
	
	/**
	 * The method takes the nodes which are bound to the registry and then passes nodeListener 
	 * object to them so that they add that event to themselves.
	 * @param name: The name ny which the node object is bound to the Registry server.
	 * @throws RemoteException
	 */
	private void addNode( String name ) throws RemoteException {
		try {
			NodeInterface node = ( NodeInterface )this.proxy.lookup( name ) ;  // get the node.
			node.addListener( this.nodeListener ) ;	// Add your listener to that node.
		} catch( NotBoundException nbe ) {
			System.err.println( nbe.getMessage() ) ;
		}
	}
	
	/**
	 * This is the event listener at the Registry which listens for any node that got 
	 * bound to the registry. It then calls method addNode( string ) to add and make a list of
	 * all the node objects that you have on the registry.
	 */
	private RegistryEventListener registryListener = new RegistryEventListener() {
		public void report( long seqNum, RegistryEvent theEvent ) throws RemoteException {
			String name = theEvent.objectName() ;
			if( theEvent.objectIsAssignableTo("NodeInterface") ) {
				if( theEvent.objectWasBound() ) {
					addNode( name ) ;
				} // You do not need to do anything else since the only thing you need to print is
				  // which query came to which object.
			}
		} 
	} ;
	
	/**
	 * Method usage() only to check that class called correctly when executed.
	 * @param port: Only needs to know that port string argument can be parsed as integer.
	 * @param args_length: The number of arguments to make sure that no extra or less arguments provided.
	 */
	public void usage( String port, int args_length ) {
		if( args_length > 2 ) {
			System.out.println("Usage: java Log <host> <port>") ;
			System.out.println("Extra arguments were provided.") ;
			System.exit( 0 ) ;
		}
		try {
			Integer.parseInt( port ) ;
		} catch( NumberFormatException nex ) {
			System.err.println("Usage:  java Log <host> <port> \n" +
					"The argument for port is not a integer. \n" + nex.getMessage() ) ;
			System.exit( 0 ) ;
		}
	}
	
	/**
	 * Method register() that registers this class with RMI server so that it can listen 
	 * events out there.
	 * @param host: The machine on which the server is running.
	 * @param port: The port number on host on which the server will be available.
	 */
	public void register( String host, String port ) {
		try {
			// First export your remote event listeners. Doesn't matter where they go?
			// Atleast right now.
			UnicastRemoteObject.exportObject( this.nodeListener, 0 ) ;
			UnicastRemoteObject.exportObject( this.registryListener, 0 ) ;
			// Now get the registry proxy.
			this.proxy = new RegistryProxy( host, Integer.parseInt( port ) ) ;
			/* Once you get the proxy, next you addEventListener to the registry server,
			 * You are placing the filter on the events for the registry when a node object is  
			 * bound and unbound from the registry. The name is NodeInterface since you are looking
			 * for the remote node object that is bound to registry. However this may not be required
			 * this.proxy.addEventListener( this.registryListener ) should also do since we are not 
			 * interested in which nodes are bound or unbound to the registry. We are only interested
			 * in what query came to the registry for which node.
			 * Do not think you really need filter thing since since you do not need what node 
			 * attached to the registry.
			 * On second thoughts should keep the filter since the registry since I do not want the 
			 * query class that are going to registry. Have to check the output for confirmation.
			 */
			this.proxy.addEventListener( this.registryListener, 
				new RegistryEventFilter().reportBound().reportUnbound().reportType("NodeInterface") ) ;
			/* Now get the nodes presently in there, and then call addNode( name ) method
			 * to add the nodeListener event to the respective nodes.
			 */
			for( String name : this.proxy.list("NodeInterface") ) {
				addNode( name ) ;
			}
		} catch( RemoteException re ) {
			System.err.println("There may not be a server registry running at provided " +
					"host and port. " ) ;
			re.printStackTrace() ;
			System.exit( 0 ) ;	// Just exit on a exception.
		}
	}
	
	/**
	 * The main() method for doing all the work.
	 * @param args: Command line arguments that are provided.
	 */
	public static void main( String [] args ) {
		Log l = new Log() ;
		if( args.length < 2 ) {		// Check for low number of arguments
			System.out.println("Usage: java Log <host> <port>") ;
			System.out.println("The required number of arguments are missing.") ;
			System.exit( 0 ) ;
		}
		l.usage( args[ 1 ], args.length ) ;
		l.register( args[ 0 ], args[ 1 ] ) ;
	}

}