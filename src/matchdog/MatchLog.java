package matchdog;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class MatchLog implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	int length, gamecount, totalrounds;
	double ownRating, oppRating;
	double ownChange, oppChange;
	String finalscore;
	long totaltime, totaltime2;
	HashMap<Integer, String> scorehistory;
	HashMap<Integer, Long> timehistory;
	HashMap<Integer, Integer> roundspergames;
	HashMap<Integer, Boolean> drophistory;
	HashMap<Long, Long> dropresumedates;
	long dropresumekey;

	
	boolean saved;
	Date savedkey;
	
	MatchLog(	
				int length, 
				String finalscore,
				long totaltime,
				int gamecount,
				double ownRating,
				double oppRating,
				double ownChange,
				double oppChange,
				HashMap<Integer, String> scores,
				HashMap<Integer, Long> times,
				HashMap<Integer, Integer> rounds,
				boolean isSaved
				
	) {
		this.length = length;
		this.finalscore = finalscore;
		this.totaltime = totaltime;
		this.gamecount = gamecount;
		this.ownRating = ownRating;
		this.oppRating = oppRating;
		this.ownChange = ownChange;
		this.oppChange = oppChange;
		this.scorehistory = scores;
		this.timehistory = times;
		this.roundspergames = rounds;
		this.saved = isSaved;
		this.totaltime2 = 0;
		for(long gt : timehistory.values()) {
			this.totaltime2 += gt; }
		
		drophistory = new HashMap<Integer, Boolean>();
		dropresumedates = new HashMap<Long, Long>();
		for(int i = 1; i <= gamecount; i++) {
			if(i < gamecount ) {
				drophistory.put(i, false);
			} else if(i == gamecount) {
				if(isSaved) {
					drophistory.put(i, true);
					// if dropped and NOT resumed match,
					// put drop date here;
					// if dropped and resumed put in 'appendMatch'
					long tmp = System.currentTimeMillis();
					dropresumedates.put(tmp, null);
					dropresumekey = tmp;
				} else { 
					drophistory.put(i, false);
					dropresumekey = 0L;
				}
			} 
		} 
					
		
		}

	
	
	public String getTotalTimeStr(int mode) {
		String tmp = "--";
		long t1, t2;
		if(mode == 0) {
            t1 = totaltime / 1000 / 60;
            t2 = (totaltime / 1000 - totaltime / 1000 / 60 * 60);
			tmp = (t1 < 10 ? "0" : "" ) + t1 + ":"
			    + (t2 < 10 ? "0" : "" ) + t2;
		
		} else if(mode == 1) {
			tmp = totaltime2 / 1000 / 60 + ":"
			+ (totaltime2 / 1000 - totaltime2 / 1000 / 60 * 60);
		}
		return tmp;
	}
	
	public boolean isSaved() {
		return saved;
	}

	public void setSaved(boolean saved) {
		this.saved = saved;
	}



	public long getDropresumekey() {
		return dropresumekey;
	}
	
	
}
