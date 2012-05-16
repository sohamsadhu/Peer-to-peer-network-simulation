// File: Node.java
// Referred from : http://www.cs.rit.edu/~ark/730/module02/notes.shtml
// 				 : http://www.cs.rit.edu/~ark/730/mrs02/mrs02.shtml
//               : http://download.oracle.com/javase/tutorial/essential/io/
//               : http://download.oracle.com/javase/tutorial/rmi/overview.html
// From Prof. Alan Kaminsky class notes and Oracle tutorials.

import java.rmi.RemoteException ;
import java.rmi.server.UnicastRemoteObject ;
import java.rmi.NoSuchObjectException ;
import edu.rit.ds.registry.RegistryProxy ;
import edu.rit.ds.registry.NotBoundException ;
import edu.rit.ds.registry.AlreadyBoundException ;
import java.io.FileNotFoundException ;
import java.io.BufferedReader ;
import java.io.FileReader ;
import java.io.IOException ;
import java.util.HashMap ;
import java.util.List ;
import java.util.ArrayList ;
import edu.rit.ds.Lease ;
import edu.rit.ds.RemoteEventGenerator ;
import edu.rit.ds.RemoteEventListener ;

/**
 * Class  Node that will implement the Remote Interface NodeInterface.
 * Binds itself and other instances of it created to the RMI registry and then functions as a node.
 * The node is supposed to be searchable through RMI registry.
 * Usage: java Start Node <host> <port> <myid> <connid1> <connid2> <file>
 */
public class Node implements NodeInterface {
	
	// The data required for the command line arguments.
	private String host ;	// This can be static since connecting to same Registry server.
	private int port ;		// This should be int. Any other argument will throw number format exception.
	private String myid ;	// id referring to this class.
	private String connid1 ;// One of the other node instances of this class referred by this instance.
	private String connid2 ;// Another node instance referred by this instance.
	private String file ;	// Since this is a file name it can be a string.
	private int timestamp ; // Used to increment the query number that came.
	
	// The data structure for storing the article titles and the content taken from the file.
	private HashMap< String, String > encyclopedia = new HashMap< String, String >() ;
	
	private RegistryProxy myProxy ;		// The Registry server proxy variable that was required.
	
	// myIDList contains ID of the nodes that are created till now. This is to check that no duplicate
	// nodes are created. Since each instance has to look up into this list; it has to be static.
	private List<String> myIdList = new ArrayList<String>() ;
	
	/**
	 * A hash map of node and time stamp. Whenever a node gets a query from client if it does not find
	 * the file locally then it forwards the query along with its' own id and a time stamp. If the 
	 * node id and the time stamp are same then the node that gets that forwarded query does not 
	 * forward the same. Since the nodes are not running parallel and at any one time there could be
	 * only one query forwarded from one node. Hence you just need to keep track of the present time
	 * stamp associated with that node. To see if you got the same one which you had already processed.
	 */
	private HashMap< String, Integer > nodets = new HashMap< String, Integer >() ;
	
	private QueryEvent myQueryEvent ;
	private RemoteEventGenerator< QueryEvent > generator = 
		new RemoteEventGenerator< QueryEvent >() ;
	
	// Make the constructor as per the specifications of the Start program.
	// The constructor acts as starting point and hence all the check code goes there.
	/**
	 * Constructor of this class Node that will take the arguments and get the execution going.
	 * @param String args : the string args that came through command line.
	 */
	public Node( String args [] ) throws RemoteException {		
		super() ; // Will be called implicitly. Mentioned for clarity.
		
		// Start with the checks to arguments and throw exceptions.
		
		// Case 1: Required arguments are missing.
		if( args.length < 6 ) {
			throw new IllegalArgumentException("Not all required arguments provided \n" +
					"Usage: java Start Node <host> <port> <myid> <connid1> <connid2> <file>") ;
		}
		
		// Case 2: There are extra arguments.
		if( args.length > 6 ) {
			throw new IllegalArgumentException("More than 6 arguments are not required.\n" +
			"Usage: java Start Node <host> <port> <myid> <connid1> <connid2> <file>") ;
		}
		this.host = args[ 0 ] ; 	// Get the host.
		
		// Case 3: The port argument cannot be parsed as an integer.
		try {
			this.port 	 = Integer.parseInt( args[ 1 ] ) ;
		} catch( NumberFormatException n ) {
			System.err.println("Usage: java Start Node <host> <port> <myid> <connid1> <connid2> <file>" 
				 + "\n" + "The arguments for port is not a integer.") ;
			n.getMessage() ;
		}
		
		this.myid    = args[ 2 ] ; 	// Initialise the values for node and 
		this.connid1 = args[ 3 ] ;	// connection ids.
		this.connid2 = args[ 4 ] ;
		
		// Case 4: connid1 argument is the same as the myid argument.
		if( this.myid.equals( this.connid1 ) ) {
			throw new IllegalArgumentException("Given node id and first connecting node id are same.") ;
		}
		
		// Case 5: connid2 argument is the same as the myid argument.
		if( this.myid.equals( this.connid2 ) ) {
			throw new IllegalArgumentException("Given node id and second connecting node id are same.") ;
		}
		
		// Case 6: connid2 argument is the same as the connid1 argument.
		if( this.connid1.equals( this.connid2 ) ) {
			throw new IllegalArgumentException("Please provide two different connecting nodes.") ;
		}
		
		// Case 7: article file's contents cannot be read or are invalid.
		this.file = args[ 5 ] ;
		try {
			BufferedReader r = new BufferedReader( new FileReader( this.file ) ) ;
			String title = new String() ;			// One for the key.
			String contents = new String() ;		// Next for the contents to key in hashmap.
			while( ( title = r.readLine() ) != null ) {	// Keep reading file one line at a time.
				contents = r.readLine() ;		// First was title the next is contents.
				this.encyclopedia.put( title, contents ) ;
			}			
			r.close() ;		// Close the reader.
		} catch( FileNotFoundException f ) {
			System.err.println("File not found \n" + f.getMessage() ) ;
		} catch( SecurityException se ) {
			System.err.println("Please change file security settings \n" + se.getMessage() ) ;
		} catch( IOException ioex ) {
			System.err.println("Could not read the file \n" + ioex.getMessage() ) ;
		}
		
		// Case 8: There is no Registry Server running at the given host and port.
		try {
			this.myProxy = new RegistryProxy( this.host, this.port ) ;
		} catch( RemoteException r ) {
			System.err.println( "Seems no Registry server running at given "+ this.host +" "+
					            this.port + "\n" + r.getMessage() ) ;
		}
		
		// Case 9: Another Node object with the same ID is already in existence.
		this.myIdList = this.myProxy.list() ;	// Get the list of objects bound to registry.
		if( myIdList.contains( this.myid ) ) { 		// So I can use it in this line with the list.
			throw new IllegalArgumentException
			          ("The node id you are trying to assign is already present.") ;
		} else {
			myIdList.add( this.myid ) ; // Make sure it does not happen the next time
		}
		
		// End all check arguments and throw exceptions.
		
		// Get a proxy of the Registry Server there somewhere.
		this.myProxy = new RegistryProxy( this.host, this.port ) ;
		
		// Now time to export my reference for availablity.
		// Since I am not remotely available but my interface is so have to typecast myself to that.
		NodeInterface remoteRef = (NodeInterface)UnicastRemoteObject.exportObject( this, 0 ) ;
		// Now attach my exported self to the registry server.
		try {
			// Now I must bind myself to the registry server by what myid is given to me and I will be 
			// searched by. args[2] is myid which is already in string format. Since I do not want to 
			// handle the exception if I have already bound by accident I over write,
			// using rebind method.
			this.myProxy.bind( args[ 2 ], this ) ;
		} catch( AlreadyBoundException abe ) {
			System.err.println( "The node id you are trying to assign is already present." ) ;
			System.err.print( abe.getMessage() ) ;
		} catch ( RemoteException re ) {
			// If there is any error in attaching myself to registry, then I remove myself.
			try {
				UnicastRemoteObject.unexportObject( this, true ) ;
			} catch ( NoSuchObjectException noex ) {}
			throw re ;
		}
		this.timestamp = 0 ; // The time stamp initialisation.
	}
	
	// Add the listener of the query event to my query event generators.
	// There could be many node event listeners instances but they all hail from the log class
	// that is keeping tabs from the node listeners about what is coming here.
	/**
	 * Adds the parameter listener to the remote event generator of this class.
	 * @param listener the remote event listener which has the Query Event in it that will have the info.
	 * @throws RemoteExcpetion
	 */
	public Lease addListener( RemoteEventListener< QueryEvent > listener ) 
							throws RemoteException {
		return this.generator.addListener( listener ) ;	
	}
	
	/**
	 * Fired when this class object gets a query. The generator reports the event with the 
	 * updated QueryEvent class which is extension of remote event. 
	 * @param myid The id of this class at present which will be sent to the remote listener.
	 * @param title The query string that came to node that is to be reported to listener.
	 */
	private void reportStatus( String myid, String title ) {
		this.myQueryEvent = new QueryEvent( myid, title ) ;
		this.generator.reportEvent( this.myQueryEvent ) ;
	}
	
	/**
	 * Searches locally if data not found then forwards to processQuery() to deal with same.
	 * @param String title : that is the title to search in local file and if not found forward.
	 * @return String that has got the contents of the title, empty if not found.
	 * @throws RemoteException
	 */
	public synchronized String makeQuery( String title ) throws RemoteException {
		String contents = new String( "" ) ;
		// I got the query to myself. Now report this event. Basically log
		// wants to know what came to me so I fire a event only when I receive something.
		this.reportStatus( this.myid , title ) ;		
		try {
			Thread.sleep( 1000 ) ;
			if( this.encyclopedia.containsKey( title ) ) {	// Search locally.
				contents = this.encyclopedia.get( title ) ;
			} else {
				this.timestamp++ ;	// Increment the timestamp each time you forward a query.
				// Put your own id and the time stamp to make sure if query comes back to you you
				// do not forward the same. Explicitly type case you never know when ouch!
				this.nodets.put( this.myid, ( Integer )this.timestamp ) ;
				// If not forward for further processing.
				contents = processQuery( title, this.myid, this.timestamp ) ;	
			}
		} catch ( InterruptedException iex ) {
			//System.err.println("Execution was interrupted " + iex.getMessage() ) ;
		}
		return contents ;
	}
	
	/**
	 * Look up for the nodes connected and forward the query .
	 * @param title that comes the title to be searched for.
	 * @return string that has contents 
	 */
	public String processQuery( String title, String id, int ts  ) {
		String contents = new String ( "" ) ; 	// Initialise a empty string.
		// Need to look on other nodes which are present on the Registry server.
		NodeInterface connid1, connid2 ;	// You need the interface types of connected node.
		try {
			// Get the connected node stubs from the Registry server.
			connid1 = ( NodeInterface )this.myProxy.lookup( this.connid1 ) ;			
			// Forward the query.
			contents = connid1.forwardQuery( title, id, ts ) ;
		} catch( RemoteException rex ) {
			//System.err.println("The nodes connected to this node could not be found " 
					//+ rex.getMessage() ) ;
		} catch( NotBoundException nbex ) {
			//System.err.println("The nodes connected to this node could not be found " 
					//+ nbex.getMessage() ) ;
		}
		// If the previous guy already gave you the answer no need to go further.
		if( !contents.equals( "" ) ) {
			return contents ;
		}
		// Had to do this since if one of the connected node threw exception then 
		// it was not forwarding the query to other node which existed. It went 
		// straight to the exception.
		try {
			connid2 = ( NodeInterface )this.myProxy.lookup( this.connid2 ) ;
			contents = connid2.forwardQuery( title, id, ts ) ;
		} catch( RemoteException rex ) {
			//System.err.println("The nodes connected to this node could not be found " 
					//+ rex.getMessage() ) ;
		} catch( NotBoundException nbex ) {
			//System.err.println("The nodes connected to this node could not be found " 
					//+ nbex.getMessage() ) ;
		}
		return contents ;
	}
	
	/**
	 * Search for the data locally first and return if found. If not then check if same else forward.
	 * @param title string for the encyclopedia article related to be searched for
	 * @param id int the id of the node from where the query originated.
	 * @param ts int the time stamp that needs to be checked to see not the same query.
	 * @throws RemoteException
	 */
	public String forwardQuery( String title, String id, int ts ) throws RemoteException {
		String contents = new String( "" ) ;
		this.reportStatus( this.myid, title ) ;	// Firing this event here since also want to tell
						                        // log when other nodes forwarded me a query.
		try{		// Slow down the thing for processing.
			Thread.sleep( 1000 ) ;
		} catch( InterruptedException iex ) {}
		if( this.encyclopedia.containsKey( title ) ) {	// Search locally.
			contents = this.encyclopedia.get( title ) ;
		} else {	// Determine first if you want to forward.
			if( this.nodets.containsKey( id ) ) {	// You already received a query from this node.
				if( this.nodets.get( id ) != ts ) {	// You are not receiving it for the same thing.
					this.nodets.put( id, ts ) ;		// Put the query new query value in hash table.
					contents = processQuery( title, id, ts ) ; // Call for forwarding.
				} else {	// Stale query do not forward and since not found locally.
					return contents ;		// send back the empty string.
				}
			} else {	// Just received a query from a node that did not previously forward.
				this.nodets.put( id, ts ) ; 	// Put the new value in.
				contents = processQuery( title, id, ts ) ; // Call for forwarding.
			}
		}
		return contents ;
	}
	
}