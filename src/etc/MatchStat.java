package etc;

import matchdog.Match;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class MatchStat implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5829481954628685476L;
	public class GameStat implements Serializable {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -2991042581628303929L;
		int rounds, cube, points;
		long gameTime, ownTime, oppTime, fibsTime; 
		int [] score = {0, 0};
		Map<Integer, Integer []> rolls;
		Map<Integer, String> boards;
		HashMap<Integer, String> log;
		
		
		GameStat(int initialRoll1, int initialRoll2) {
			rolls = new HashMap<Integer, Integer []>();
			boards = new HashMap<Integer, String>();
			log = new HashMap<Integer, String>();
			rolls.put(0, new Integer [] {initialRoll1, initialRoll2 });
		}
		
		void putBoard(String board) {
			boards.put(boards.size(), board);
		}
		void putLog(String str) {
			log.put(log.size(), str);
		}
		
		void putLastLog(String str) {
			log.put(log.size() -1, log.get(log.size() -1) + str);
		}
		
	}
	
	int length, games;
	int totalRounds;
	long startTime, totalTime, ownTime, oppTime, fibsTime;
	Map<Integer, GameStat> gamestats;
	Map<Integer, HashMap<Integer, String>> logs;
	GameStat gamestat;
	HashMap<Integer, String> log;
	//Match matchsave;
	
	
	MatchStat(Match match, int length, long start) {
		this.length = length;
		this.startTime = start;
		games = 0;
		totalRounds = 0;
		totalTime = 0;
		ownTime = 0;
		oppTime = 0;
		fibsTime = 0;
		gamestats = new HashMap<Integer, GameStat>();
		logs = new HashMap<Integer, HashMap<Integer, String>>();
		
		//matchsave = match;
	}
	
	protected void newGame(int roll1, int roll2) {
		
		gamestat = new GameStat(roll1, roll2);
		gamestats.put(++games, gamestat);
		gamestat.log = new HashMap<Integer, String>();
		logs.put(logs.size(), gamestat.log);
	}
	
	void putBoard(String board) {
		if(gamestat != null) {
			gamestat.putBoard(board);
		}
	}
	void putLog(String str) {
		if(gamestat != null) {
			gamestat.putLog(str);
		}
	}
	void putLastLog(String str) {
		if(gamestat != null) {
			gamestat.putLastLog(str);
		}
	}	
	
}
