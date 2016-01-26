package matchdog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import matchdog.PlayerStats.PlayerStat;

public class StatWriter {
	
	MatchDog server;
	PlayerStats playerstats;
	PlayerPrefs prefs;
	
	public StatWriter(MatchDog server) {
		this.server = server;
		playerstats = server.playerstats;
		prefs = server.prefs;
	}

	private void printDebug (String str) {
		server.printDebug(str);
	}

	public String getTotalTimeStr(long in) {
		
        long allseconds = in / 1000;

		int minutes = 0, hours = 0, seconds = 0, days = 0;
		if (allseconds > 59) {
			minutes = (int) allseconds / 60;
		}
		if (minutes > 59) {
			hours = minutes / 60;
			minutes = minutes % 60;
		}
		if (hours > 23) {
			days = hours / 24;
			hours = hours % 24;
		}
		seconds = (int) allseconds
				- (minutes * 60 + hours * 60 * 60 + days * 24 * 60 * 60);
		String hourszero = "";
		String minuteszero = "";
		String secondszero = "";
		String daysstring = "";
		if (hours < 10) {
			hourszero = "0";
		}
		if (minutes < 10) {
			minuteszero = "0";
		}
		if (seconds < 10) {
			secondszero = "0";
		}
		if (days > 0) {
			daysstring = days + " day(s) ";
		}
		String tmp = "--";
		tmp = daysstring + hourszero + hours + "h:" + minuteszero
				+ minutes + "m:" + secondszero + seconds + "s";		

		return tmp;
	}
	
	public void dumpPlayerStats(String player, int mode) {
	
		if(player.equals("")) {

			printDebug("DUMPING PLAYERSTATS:");
			for (PlayerStat p : playerstats.pstats.values()) {				
				printDebug("**** **** ****");
				dumpPlayerStat(p.getPlayername(), mode);
				printDebug("");
			}
		} else {
			dumpPlayerStat(player, mode);
		}
	}
	
	//// i
	// 
	
	////
	// 
	/**
	 * 
	 * 
	 * @param player The player to dump stats for. If empty
	 * (""), all players are considered.
	 * 
	 * @param mode Mode the output should be rendered.
	 * Including match logs:
	 * mode 0: to stdout incl. match logs
	 * mode 3: write to file incl. match logs
	 * 
	 * Excluding match logs:
	 * mode 1: to stdout excl. match logs
	 * mode 2: kibitz to fibsout excl. match logs  
	 *
	 * @return False if no stats for player or there's some incosistency
	 * in the player's stat object, true otherwise.
	 */
	public synchronized boolean dumpPlayerStat(String player, int mode) {
		PlayerStat p = playerstats.getByName(player);
		HashMap<Integer, String> out = new HashMap<Integer, String>();
		int k = 0;
		if(p == null) {
			return false;
		} else {
			
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:serverSocket");
			
			HashMap<Date, MatchLog> h = p.getHistory();
			// sorted h
			TreeMap<Date, MatchLog> sh = new TreeMap<Date, MatchLog>(h);
			int key1, key2, key3, key4, key5;
			if(mode != 2) out.put(k++, " ");
			out.put(k++, " Stats for " + p.getPlayername() + ": "
					+ p.getMatchcount() + " match(es)");
			out.put(k++, "");
			key1 = k;
			out.put(k++, "  Me/You won  : ");
			key2 = k;
			out.put(k++, "  My rating   : ");
			key3 = k;
			out.put(k++, "  Your rating : ");
			key4 = k;
			out.put(k++, "  Total time  : ");
			key5 = k;
			out.put(k++, "  Total drops : ");
			
			if(mode != 2) out.put(k++, " ");
			if(mode == 2) out.put(k++, "[since: " 
					+ server.playerstats.getSince(2) + "]");
			
			
			int a, b, won = 0, lost = 0, drops = 0;
			double owntotalr = 0.0, opptotalr = 0.0;
			long totaltime = 0L;

			
			HashMap<Integer, String> out2 = new HashMap<Integer, String>();
			int kk = 0;
			out2.put(kk++, "  Match history >>");
			
			for (Date d : sh.keySet()) {
				MatchLog l = h.get(d);
				if(l != null && l.finalscore != null) {
					String [] split = l.finalscore.replace("*", "").trim().split("-");
					try {
						a = Integer.parseInt(split[0]);
						b = Integer.parseInt(split[1]);
						
						if(a > b) {
							won++;
						} else {
							lost++;
						}
						
					} catch (Exception e) {	}
				}
				owntotalr += l.ownChange;
				opptotalr += l.oppChange;
				totaltime += l.totaltime;

				out2.put(kk++, " ");
				out2.put(kk++, "  -> " + d.toString() + " | Length: " + l.length);
				out2.put(kk++, "     Final score : " + l.finalscore);
				out2.put(kk++, "     Match time  : " + l.getTotalTimeStr(0));
				out2.put(kk++, "     My rating   : (" 
						+ l.ownRating + ") " + l.ownChange);
				out2.put(kk++, "     Your rating : (" 
						+ l.oppRating + ") " + l.oppChange);
				out2.put(kk++, "     Game(s) in match: " + l.gamecount 
							+ " (score: " + prefs.getName() + "-" + p.getPlayername() + ")");
				int gameno = 0;
				String gamestring = "", space = "", space2 = "";
				for (int i = 1; i <= l.scorehistory.size(); i++) {
					if(l == null || l.scorehistory.size() < 1/* || l.gamecount == 0*/) {
                        server.printDebug(" *** ERROR: matchlog not found");
						return false;
					}
					if(l.timehistory == null || l.timehistory.get(i) == null) {
                        server.printDebug(" *** ERROR: timehistory not found");
						return false;
					}
					String tmp = l.timehistory.get(i) / 1000 / 60 + ":"
					+ (l.timehistory.get(i) / 1000 - l.timehistory.get(i) / 1000 / 60 * 60);
					if(l.drophistory.get(i)) {
						gamestring = "      [dropped]";
						drops++;
					} else {
						gameno++;
						if(gameno < 10)
							space = " ";
						gamestring = "      [game " + gameno + "]" + space;
					}
					space = "";

					if(l.roundspergames.get(i) < 100 && l.roundspergames.get(i) > 9) {
						space += " ";
					} else if(l.roundspergames.get(i) < 10) {
						space += "  ";
					}
					int stmp = 6 - l.scorehistory.get(i).length();
					for(int j = 0; j < stmp; j++) {
						space2 += " ";
					}
					
					out2.put(kk++,  gamestring + " score: " 
							+ l.scorehistory.get(i) + space2
							+ " rounds: " + space
							+ l.roundspergames.get(i) + " time: "
							+ tmp);
					space2 = ""; space = "";
				}
				if(l.dropresumedates.size() > 0) {
					
					TreeMap<Long, Long> sdrd = new TreeMap<Long, Long>(l.dropresumedates);
					
					out2.put(kk++, " ");
					out2.put(kk++, "     * Dropped @ | Resumed @" );
					for(long drop : sdrd.keySet()) {
						Date d1 = new Date();
						Date d2 = new Date();
						String s1 = "", s2 = "not so far";
						d1.setTime(drop);
						s1 = df.format(d1);
						if(l.dropresumedates.get(drop) != null) {
							d2.setTime(l.dropresumedates.get(drop));
							s2 = df.format(d2);
						}
						out2.put(kk++, "       " + s1
								+ " | "+ s2);
					}
				}
			}
			String tmp1 = out.get(key1) + won + "/" + lost;
			String tmp2 = out.get(key2) + owntotalr;
			String tmp3 = out.get(key3) + opptotalr;
			String tmp4 = out.get(key4) + getTotalTimeStr(totaltime);
			String tmp5 = out.get(key5) + drops;

			out.put(key1, tmp1);
			out.put(key2, tmp2);
			out.put(key3, tmp3);
			out.put(key4, tmp4);
			out.put(key5, tmp5);
			//// including match logs
			// mode 0: to stdout incl. match logs
			
			//// excluding match logs
			// mode 1: to stdout excl. match logs
			// mode 2: kibitz to fibsout excl. match logs 
			// mode 3: 
			BufferedWriter logfile = null;
			String path = "../data/";
			String filename = p.hashCode() + ".txt";
			if(mode == 3 ) {		
				try {
					logfile = new BufferedWriter(new FileWriter(path + filename));
				} catch (IOException e) {
					printDebug("FILE CREATION FAILED :");
					e.printStackTrace();
				}
			}
			for(int i = 0; i < out.size(); i++) {
				if(mode == 3) {
					if(logfile != null) {
						 try {
							logfile.write(out.get(i));
							logfile.write("\r\n");
						} catch (IOException e) {}
					}
				} else if(mode == 2) {
					server.fibs.sleepFibs(500);
					server.fibsout.println("tell " + player + " " + out.get(i));
				} else if(mode < 2) {
					printDebug(out.get(i));
				}
			}
			if(mode == 0 || mode == 3) {
				for(int i = 0; i < out2.size(); i++) {
					if(mode == 0) {
						printDebug(out2.get(i));
					} else if(mode == 3) {
						try {
							logfile.write(out2.get(i));
							logfile.write("\r\n");
						} catch (IOException e) {}
						
					}
				}
			}
			if(mode == 3) {
				server.printDebug("STAT FILE WROTE: " + filename);
				
				server.statrequests.put(player, filename);
				try {
					logfile.write("\r\n\r\n[Log file created at: " + new Date());
					logfile.write(",\r\nbased on data since: " 
							+ server.playerstats.getSince(0) + "]");
					logfile.close();
				} catch (IOException e) {
					server.systemPrinter.printDebugln(e.getMessage());
				}
			}
			printDebug("");
			return true;
		}
	}
}
