package matchdog;

import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;


/**
 * PlayerStats implements a serializable holder for
 * PlayerStat instances. Each PlayerStat represents
 * one opponenent and keeps a history of recorded
 * matches each represented by a MatchLog instance
 *
 * */
public class PlayerStats implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	HashMap<String, PlayerStat> pstats;
	Date since;
	
	PlayerStats () {
		pstats = new HashMap<String, PlayerStat>();
		since = new Date();
	}
	
	PlayerStat getByName(String name) {
		return pstats.get(name);
	}

	String getPlayerNameByMatchLog(MatchLog log) {
		for (String p : pstats.keySet()) {
            for(Date d : pstats.get(p).getHistory().keySet()) {
                if(pstats.get(p).getHistory().get(d).equals(log)) {
                    return p;
                }
            }
        }
        return null;
	}
	
	boolean hasPlayer(String name) { 
			
		return pstats.containsKey(name);
	}
	
	void cratePlayer(String name) {
		pstats.put(name, new PlayerStat(name));
	}
	
	class PlayerStat implements Serializable {
		
		String playername;
		int matchcount;
		HashMap<Date, MatchLog> history;

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		PlayerStat(String name) {
			
			playername = name;
			matchcount = 0;
			history = new HashMap<Date, MatchLog>();

		}
		
		MatchLog createLog(Match match) {

			HashMap<Integer, String> sh = match.scorehistory;
			HashMap<Integer, Long> th = match.timehistory;
			HashMap<Integer, Integer> rpg = match.roundspergames;

			MatchLog log = new MatchLog( 
					match.getMl(),
					match.getFinalscore(),
					match.getTotalTime(),
					match.getGameno(),
					match.getOwnRating(),
					match.getOppRating(),
					match.getOwnRatingChange(),
					match.getOppRatingChange(),
					sh, th, rpg, match.isDropped());
			
			return log;
		}
		
		void putMatch(Match match) {

			MatchLog log = createLog(match);
			matchcount++;
			history.put(match.start, log);
		}
		
		void putMatch(Date start, MatchLog log) {

			matchcount++;
			history.put(start, log);
		}

		void appendMatch(Match match) {
			
			MatchLog saved = getSaved();
			
			MatchLog log = createLog(match);
			
			if(saved == null || !match.wasResumed()) 
				return;
			

			// update ratings
			saved.ownRating = log.ownRating;
			saved.ownChange = log.ownChange;
			saved.oppRating = log.oppRating;
			saved.oppChange = log.oppChange;
			
			// update final score
			saved.finalscore = log.finalscore;
			
			// update histories
			for(int i = 1; i <= log.scorehistory.size(); i++) {
				saved.scorehistory.put(saved.scorehistory.size() + 1, log.scorehistory.get(i));
			}

			for(int i = 1; i <= log.roundspergames.size(); i++) {
				saved.roundspergames.put(saved.roundspergames.size() + 1, log.roundspergames.get(i));
			}
			
			for(int i = 1; i <= log.timehistory.size(); i++) {
				saved.timehistory.put(saved.timehistory.size() + 1, log.timehistory.get(i));
			}
			
			for(int i = 1; i <= log.drophistory.size(); i++) {
				saved.drophistory.put(saved.drophistory.size() + 1, log.drophistory.get(i));
			}			
			
			// update time
			saved.totaltime += log.totaltime;
			saved.totaltime2 += log.totaltime2;
			
			// update gamecount to show the real number of games
			// (excluding the dropped ones)
			saved.gamecount += log.gamecount - 1;
			
			saved.totalrounds += log.totalrounds;
			
			if(!match.isDropped() && saved.saved) {
				saved.saved = false;
			}
			
			if(match.wasResumed()) {
				saved.dropresumedates.put(saved.getDropresumekey(), match.start.getTime());
			}	
			
			if(match.isDropped()) {
				long tmp = System.currentTimeMillis();
				saved.dropresumedates.put(tmp, null);
				saved.dropresumekey = tmp;
			}
		}

		public Date getFirstMatchDate() {
			TreeMap<Date, MatchLog> t = new TreeMap<Date, MatchLog>(history);
			return t.entrySet().iterator().next().getKey();
		}

		private MatchLog getSaved() {
			for(Date d : history.keySet()) {
				MatchLog l = history.get(d);
				if(l.isSaved()) 
					return l;
			}
			return null;
		}

		public HashMap<Date, MatchLog> getHistory() {
			return history;
		}

		public int getMatchcount() {
			return matchcount;
		}
		
		public String getPlayername() {
			return playername;
		}	
		
	} //// END INNER CLASS: PlayerStat

	public String getSince(int mode) {
		if(mode < 0 || mode > 1)
			mode = 0;
		SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy");
		switch(mode) {
		case 0:
			return since.toString();
			
		case 1:
			return df.format(since);
			
		default:
			return since.toString();
		}
		
	}
	
}
