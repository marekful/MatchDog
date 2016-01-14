package etc;
/*
 * Copyright (c) 2008 by Ingo Macherius, created 20080210
 * I hereby abandon any property rights to this source code, compiled code, and documentation
 * contained in this distribution into the Public Domain. This code comes with NO WARRANTY or
 * guarantee of fitness for any purpose. 
 *
 * $Id$
 */


import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://exitlist.torproject.org/

public class TorExitNodeChecker {

	static DirContext cachedInitialContext = null;
	static String cachedReverseFibsIP4Address = null;

	final static Pattern IP4PATTERN = Pattern
			.compile("([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})");

	final static String TOR_DNS_EL_DNS_SUFFIX = ".ip-port.exitlist.torproject.org";
	final static String TOR_DNS_EL_YES = "127.0.0.2";

	public static void main(String[] args) {
		System.out.println(isTorExitNode("fibs.com"));
		System.out.println(isTorExitNode("198.144.203.150"));
		System.out.println(isTorExitNode("tor-exit.8thdaytech.com"));
		System.out.println(isTorExitNode("yahoo.de"));
	}

	////A request for the A record 
	////"209.137.169.81.6667.4.3.2.1.ip-port.exitlist.torproject.org" 
	////would return 127.0.0.2 if
	////there's a Tor node that can 
	////exit through 81.169.137.209
	////to port 6667
	////at 1.2.3.4.
	////If there isn't such an exit node, the DNSEL returns NXDOMAIN.
	//

	public static boolean isTorExitNode(String host) {
		try {
			String reply = getDNSRecs(tordnselQueryString(host)).get(0);
			return TOR_DNS_EL_YES.equals(reply);
		} catch (NamingException e) {
			return false;
		}
	}

	private final static String tordnselQueryString(String host) throws NamingException {
		return reverseOrderedIP4Address(host) + ".4321." + getReversedFibsIP4Address() + TOR_DNS_EL_DNS_SUFFIX;
	}

	private final static String reverseOrderedIP4Address(String hostname) throws NamingException {
		hostname = hostname.trim();
		final Matcher m = IP4PATTERN.matcher(hostname);
		if (m.matches()) {
			return toReverseIP4String(m);
		} else {
			final String reverseMapping;
			List<String> hostnames = getDNSRecs(hostname);
			if (hostnames.size() == 0) {
				throw new NamingException("hostnames.size() == 0");
			} else if (hostnames.size() > 1) {
				reverseMapping = hostnames.get(0);
				System.out.println("Multiple addresses truncated");
			} else {
				reverseMapping = hostnames.get(0);
			}
			final Matcher m2 = IP4PATTERN.matcher(reverseMapping);
			if (m2.matches()) {
				return toReverseIP4String(m2);
			} else {
				throw new NamingException("Not a numerical IP4 address and reverse lookup failed for '"
						+ hostname + "'.");
			}
		}
	}

	private final static String toReverseIP4String(Matcher m) throws NamingException {
		try {
			return m.group(4) + '.' + m.group(3) + '.' + m.group(2) + '.' + m.group(1);
		} catch (IndexOutOfBoundsException iobe) {
			NamingException ne = new NamingException("Failed to construct reverse numerical IP String.");
			ne.setRootCause(iobe);
			throw ne;
		} catch (IllegalStateException iss) {
			NamingException ne = new NamingException("Failed to construct reverse numerical IP String.");
			ne.setRootCause(iss);
			throw ne;
		}
	}
	
	private final static String REVERSE_FIBS_IP = "60.199.95.74"; // hardcoded as per February 2008 
	
	private final static String getReversedFibsIP4Address() {
		if (cachedReverseFibsIP4Address != null) {
			return cachedReverseFibsIP4Address;
		}
		
		List<String> fibsAdresses;
		try {
			fibsAdresses = getDNSRecs("fibs.com");
		} catch (NamingException ne) {
			fibsAdresses = Collections.emptyList();
		}
		if (fibsAdresses.size() > 0) {
			final Matcher m = IP4PATTERN.matcher(fibsAdresses.get(0));
			m.matches();
			try {
				cachedReverseFibsIP4Address = toReverseIP4String(m);
			} catch (NamingException e) {
				cachedReverseFibsIP4Address = REVERSE_FIBS_IP;	
			}
		} else {
			cachedReverseFibsIP4Address = REVERSE_FIBS_IP;
		}
		return cachedReverseFibsIP4Address;
	}
	


	// getting DNS Records using JNDI

	/**
	 * Gets all matching dns records as an array of strings.
	 *
	 * @param hostName domain, e.g. oberon.ark.com or oberon.com which you want
	 *               the DNS records.
	 * @return List<String>
	 *
	 * @throws NamingException if DNS lookup fails.
	 */

	final private static String[] TYPE_A_DNS_QUERY = new String[] { "A" };

	private static List<String> getDNSRecs(String hostName) throws NamingException {
		ArrayList<String> results = new ArrayList<String>(2);
		Attributes attrs = getInitialContext().getAttributes(hostName, TYPE_A_DNS_QUERY);
		for (Enumeration<? extends Attribute> e = attrs.getAll(); e.hasMoreElements();) {
			Attribute a = (Attribute) e.nextElement();
			final int size = a.size();
			for (int i = 0; i < size; i++) {
				// "A" type response string is just IP
				results.add((String) a.get(i));
			}
		}
		return results;
	}

	private static DirContext getInitialContext() throws NamingException {
		if (cachedInitialContext == null) {
			final Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
			cachedInitialContext = new InitialDirContext(env);
		}
		return cachedInitialContext;
	}
}
