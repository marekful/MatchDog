package etc;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class MatchLog_v00 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6687773223849240121L;

	
	int length, gamecount, totalrounds;
	double ownRating, oppRating;
	double ownChange, oppChange;
	String finalscore;
	long totaltime, totaltime2;
	HashMap<Integer, String> scorehistory;
	HashMap<Integer, Long> timehistory;
	HashMap<Integer, Integer> roundspergames;
	HashMap<Integer, Boolean> drophistory;

	
	boolean saved;
	Date savedkey;
	
	MatchLog_v00(	
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
		//drophistory.put(0, false);
		for(int i = 1; i <= gamecount; i++) {
			if(i < gamecount ) {
				drophistory.put(i, false);
			} else if(i == gamecount) {
				if(isSaved)
					drophistory.put(i, true);
				else 
					drophistory.put(i, false); } } 
		}
	
	
	public String getTotalTimeStr(int mode) {
		String tmp = "--";
		if(mode == 0) {
			tmp = totaltime / 1000 / 60 + ":"
			+ (totaltime / 1000 - totaltime / 1000 / 60 * 60);
		
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
	
	
}
