/*
 * MonitorImpl.java
 *
 * Created on July 4, 2002, 11:09 PM
 */

package net.sf.repbot;

import java.util.*;
import java.util.regex.*;

/** Implementation of the fibs monitor.
 *
 * @author  avi
 */
public class MonitorImpl implements Monitor, LineListener {

	private Collection<Listener> listeners = new ArrayList<Listener>();
	private Listener dist = new Distributor();

	private final static Pattern shouts = Pattern.compile("(\\S+) shouts: (.*)");
	private final static Pattern logsin = Pattern.compile("(\\S+) logs in( again)?.");
	private final static Pattern logsout = Pattern.compile("(\\S+) logs out.");
	private final static Pattern drops = Pattern.compile("(\\S+) drops connection.");
	private final static Pattern closeold = Pattern.compile("Closed old connection with user (\\S+).");
	private final static Pattern timedout = Pattern.compile("Connection with (\\S+) timed out.");
	private final static Pattern admin = Pattern.compile("(\\S+) closes connection with (\\S),");
	private final static Pattern neterr = Pattern.compile("Network error with (\\S+).");
	private final static Pattern start = Pattern.compile("(\\S+) and (\\S+) start a (\\d+) point match.");
	private final static Pattern resume = Pattern
			.compile("(\\S+) and (\\S+) are resuming their (\\d+)-point match.");
	private static Pattern end = Pattern
			.compile("(\\S+) wins a (\\d+) point match against (\\S+)  (\\d+)-(\\d+) .");
	private static Pattern newuser = Pattern.compile("(\\S+) just registered and logs in\\.");

	/** Creates a new instance of MonitorImpl */
	public MonitorImpl(FibsListener connection) {
		connection.addLineListener(this);
	}

	/** Registers a new event listener.  */
	public void addEventListener(Listener listener) {
		listeners.add(listener);
	}

	/** Deregisters an event listener.  */
	public void removeEventListener(Listener listener) {
		listeners.remove(listener);
	}

	/** Called on a received line.  */
	public void onLine(String line, FibsListener connection) {
		Matcher m;
		if ((m = shouts.matcher(line)).matches())
			dist.onShout(m.group(1), m.group(2));
		else if ((m = newuser.matcher(line)).matches()) {
			dist.onNewUser(m.group(1));
			dist.onLogin(m.group(1));
		} else if ((m = logsin.matcher(line)).matches())
			dist.onLogin(m.group(1));
		else if ((m = logsout.matcher(line)).matches())
			dist.onLogout(m.group(1));
		else if ((m = drops.matcher(line)).matches())
			dist.onDrop(m.group(1));
		else if ((m = admin.matcher(line)).matches())
			dist.onAdminClose(m.group(2), m.group(1));
		else if ((m = neterr.matcher(line)).matches())
			dist.onNetworkError(m.group(1));
		else if ((m = closeold.matcher(line)).matches())
			dist.onClosedOldConnection(m.group(1));
		else if ((m = timedout.matcher(line)).matches())
			dist.onConnectionTimedOut(m.group(1));
		else if ((m = start.matcher(line)).matches())
			dist.onStartMatch(m.group(1), m.group(2), number(m.group(3)));
		else if ((m = resume.matcher(line)).matches())
			dist.onResumeMatch(m.group(1), m.group(2), number(m.group(3)));
		else if ((m = end.matcher(line)).matches())
			dist.onEndMatch(m.group(1), m.group(3), number(m.group(2)), number(m.group(4)),
					number(m.group(5)));
	}

	private final static int number(String s) {
		return Integer.parseInt(s);
	}

	private class Distributor implements Listener {

		/** Called when a user drops connection.  */
		public void onDrop(String user) {
			for (Listener l : listeners)
				l.onDrop(user);
		}

		/** Called when a game is finished.  */
		public void onEndMatch(String user1, String user2, int length, int points1, int points2) {
			for (Listener l : listeners)
				l.onEndMatch(user1, user2, length, points1, points2);
		}

		/** Called when a user logs in.  */
		public void onNewUser(String user) {
			for (Listener l : listeners)
				l.onNewUser(user);
		}

		/** Called when a user logs in.  */
		public void onLogin(String user) {
			for (Listener l : listeners)
				l.onLogin(user);
		}

		/** Called when a user logs out.  */
		public void onLogout(String user) {
			for (Listener l : listeners)
				l.onLogout(user);
		}

		/** Called when a game is resumed.  */
		public void onResumeMatch(String user1, String user2, int length) {
			for (Listener l : listeners)
				l.onResumeMatch(user1, user2, length);
		}

		/** Called when a user shouts something.  */
		public void onShout(String user, String message) {
			for (Listener l : listeners)
				l.onShout(user, message);
		}

		/** Called when a game is started.  */
		public void onStartMatch(String user1, String user2, int length) {
			for (Listener l : listeners)
				l.onStartMatch(user1, user2, length);
		}

		/** Called when a network error occurs with a user.  */
		public void onNetworkError(String user) {
			for (Listener l : listeners)
				l.onNetworkError(user);
		}

		/** Called when a user's connection is closed by an administrator.  */
		public void onAdminClose(String user, String admin) {
			for (Listener l : listeners)
				l.onAdminClose(user, admin);
		}

		/** Called when an old connection is closed.  */
		public void onClosedOldConnection(String user) {
			for (Listener l : listeners)
				l.onClosedOldConnection(user);
		}

		/** Called when a connection timeout occurs with a user.  */
		public void onConnectionTimedOut(String user) {
			for (Listener l : listeners)
				l.onConnectionTimedOut(user);
		}

	}

}
