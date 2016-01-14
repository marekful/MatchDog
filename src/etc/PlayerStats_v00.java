package etc;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class PlayerStats_v00 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -690201754545424470L;

	HashMap<String, PlayerStat> pstats;
	Date since;
	
	PlayerStats_v00 () {
		pstats = new HashMap<String, PlayerStat>();
		since = new Date();
	}
	
	PlayerStat getByName(String name) {
		return pstats.get(name); 
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
		HashMap<Date, MatchLog_v00> history;

		/**
		 * 
		 */
		private static final long serialVersionUID = 7261329021476323764L;
		
		PlayerStat(String name) {
			
			playername = name;
			matchcount = 0;
			history = new HashMap<Date, MatchLog_v00>();

		}
		
		MatchLog_v00 createLog(Match match) {
			
			//String timestr = match.getTime() / 1000	/ 60 + ":" 
			//+ (match.getTime() / 1000 - match.getTime() / 1000 / 60 * 60);

			HashMap<Integer, String> sh = match.scorehistory;
			HashMap<Integer, Long> th = match.timehistory;
			HashMap<Integer, Integer> rpg = match.roundspergames;

			MatchLog_v00 log = new MatchLog_v00( 
					match.getMl(),
					match.getFinalscore(),
					match.getTotalTime(),
					match.getGameno(),
					match.getOwnRating(),
					match.getOppRating(),
					match.getOwnRatingChange(),
					match.getOppRatingChange(),
					sh, th, rpg, match.isDropped());
			
			//if(match.isDropping()) 
			//	log.savedkey = match.start;
			
			return log;
		}
		
		void putMatch(Match match) {

			MatchLog_v00 log = createLog(match);

			matchcount++;
			
			history.put(match.start, log);
		}
		
		void appendMatch(Match match) {
			
			MatchLog_v00 saved = getSaved();
			
			MatchLog_v00 log = createLog(match);
			
			if(saved == null || !match.wasResumed()) 
				return;
			

			// update ratings
			saved.ownRating = log.ownRating;
			saved.ownChange = log.ownChange;
			// don't update opp rating in appendMatch cause that's 0.0 then
			//saved.oppRating = log.oppRating;
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
			
			if(!match.isDropped()) {
				saved.saved = false;
			}
			
			//removeSaved(saved.savedkey);
			
			//history.put(match.start, log);
			
		}

		private MatchLog_v00 getSaved() {
			for(Date d : history.keySet()) {
				MatchLog_v00 l = history.get(d);
				if(l.isSaved()) 
					return l;
			}
			return null;
		}

		public HashMap<Date, MatchLog_v00> getHistory() {
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
