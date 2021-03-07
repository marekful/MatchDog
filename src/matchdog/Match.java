package matchdog;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Match {

	String player0, player1;
	int ml;
	int [] score = {0,0};
	int [] dice = {0,0};
	boolean ingame, shiftmove, oppgreeted, crawford, dropped, postcrawford, wasResumed;
	int oppgreetphase;
	int gameno;
	int [] turn = {0, 0}; // 1,0 if my turn, 0,1 if opp's, 0,0 if no one's.
	int cube;
	int round, roundsave;
	HashMap<Integer, Integer> roundspergames;
	HashMap<Integer, String> scorehistory;
	HashMap<Integer, Long> timehistory;
	Date start;
	int crawfordscore;
	String waitfor, finalscore;
	Date droppedat, mystamp, oppstamp;
	Timer stamptimer, waitfortimer;
	TimerTask stamptimertask, waitfortimertask;
	MatchDog server;
	HashMap<Integer, String> chattexts;
	boolean waitRateCalc, finished;
	double ownRatingChange, oppRatingChange, oppRating, OwnRating;
	int callcounter;
	long gametime;
	
	double [] equities;
	boolean ownResignInProgress, oppResignInProgress;
	
	Match (MatchDog server, String oppname, int matchlength) {
		this.server = server;
		player0 = server.prefs.getUsername();
		player1 = oppname;
		ml = matchlength;
		gameno = 0;
		ingame = false;
		shiftmove = false;
		cube = 1;
		round = 0;	
		start = new Date();
		gametime = start.getTime();
		oppgreeted = false;
		oppgreetphase = 0;
		crawford = false;
		dropped = false;
		crawfordscore = -1;
		droppedat = null;
		mystamp = null;
		oppstamp = null;
		
		stamptimer = new Timer();
		stamptimertask = new TimerTask() 
					{@Override public void run() {checkStamps();}};	
		stamptimer.schedule(stamptimertask, 30000L, 30000L);
		
		server.printDebug("***");
		server.printDebug("NEW MATCH (timer scheduled: "
				+ stamptimer.hashCode() + ")");
		server.printDebug("***");
		
		waitfortimer = new Timer();
		waitfortimertask = new TimerTask() 
					{@Override public void run() {cancelWaitfor();}};			
		initChatTexts();
		
		// these store data during match that goes to PlayerStats
		roundspergames = new HashMap<Integer, Integer>();
		scorehistory = new HashMap<Integer, String>();
		timehistory = new HashMap<Integer, Long>();
		waitRateCalc = false;
        finished = false;
		ownRatingChange = 0.0;
		oppRatingChange = 0.0;
		oppRating = 0.0;
		OwnRating = 0.0;
		// <--
		
		equities = new double[]{.5, .5, 0, 0, 0, .5};
		
		ownResignInProgress = false;
		oppResignInProgress = false;
		
		callcounter = 0;
		
		// stamps should be set at match start
		touchStamps();

	}

	protected void cancelWaitfor() {

		if(server.fibs.lastmatch != null) {
			// DROP MATCH
			server.printDebug("WAITFOR " + this.waitfor + " DISABLED (time limit)");
			this.stamptimer.cancel();
			this.stamptimer.purge();
			this.waitfortimer.cancel();
			this.waitfortimer.purge();
			server.fibs.lastmatch = null;
			
		}
	}

	public void purgeStampTimer() {
		server.printDebug("PURGING STAMPTIMER: " + stamptimer.hashCode());
		stamptimer.cancel();
		stamptimer.purge();
	}
	
	private synchronized void checkStamps() {
		
		if( server.getFibsmode() < 2 ) {
			purgeStampTimer();
		}
			
		server.printDebug("CHECKING STAMPS (" + stamptimer.hashCode() + ")..." );
		Date now = new Date();
		long diff;
		long fibsDiff = server.fibs.getFibsLastLineSecondsAgo();
		
		if(isMyTurn()) {
			diff = now.getTime() - getMystamp().getTime() - fibsDiff * 1000;
			server.printDebug("(my diff is " + diff + "(" + fibsDiff * 1000 + ") ms)");
			if(diff > 40000) {
				if(isOwnResignInProgress()) {
					server.fibsout.println("k I resigned, check your client!" +
							" Accept or reject it.");
				} else {
					server.printDebug("RESENDING lastboard (checkStamps)");
					server.resendLastBoard();
				}
			}
		} else if(isOppsTurn()) {
			diff = now.getTime() - getOppstamp().getTime() - fibsDiff * 1000;
			server.printDebug("(opp's diff is " + (diff / 1000) + "(" + fibsDiff + ") sec)");
			if(diff > 60000 && callcounter < 2) {
				
				server.printDebug("asking opp's attention");
				server.fibs.sleepFibs(150);
				server.fibsout.println("k Are you there? ");
				server.fibs.sleepFibs(250);
				callcounter++;
				
			} else if(diff > 60000 && callcounter < 3) {
				server.resendLastBoard();
				callcounter++;
			} else if(diff > 120000) {
				server.printDebug("OPP IS INACTIVE for :" + diff / 1000 + " seconds, LEAVING");
				server.fibsout.println("leave");
			}
		}
	}
	
	public Date getDroppedat() {
		return droppedat;
	}

	public void setDroppedat(Date droppedat) {
		this.droppedat = droppedat;
	}

	public String getWaitfor() {
		return waitfor;
	}

	public void setWaitfor(String waitfor) {
		this.waitfor = waitfor;
	}

	
	public boolean isIngame() {
		return ingame;
	}

	public void setIngame(boolean ingame) {
		this.ingame = ingame;
	}

	public int getGameno() {
		return gameno;
	}

	public void setGameno(int newgameno) {

		this.gameno = newgameno;
	}

	public String getPlayer1() {
		return player1;
	}

	public int[] getDice() {
		return dice;
	}

	public void setDice(int[] dice) {
		this.dice = dice;
	}

	public int[] getTurn() {
		return turn;
	}

	public void setTurn(int[] turn) {
		this.turn = turn;
	}

	public int getCube() {
		return cube;
	}

	public void setCube(int cube) {
		this.cube = cube;
	}
	
	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		if(round == 0 && this.round > 0) {
			roundsave = this.round;
		}
		this.round = round;
	}
	
	public boolean isMyTurn() {
		return (turn[0] == 1);
	}
	
	public boolean isOppsTurn() {
		return (turn[1] == 1);
	}

	public int getMl() {
		return ml;
	}

	public void setMl(int ml) {
		this.ml = ml;
	}

	public boolean isShiftmove() {
		return shiftmove;
	}

	public void setShiftmove(boolean shiftmove) {
		this.shiftmove = shiftmove;
	}

	public Date getStart() {
		return start;
	}
	
	public long getTotalTime() {
		
		return Calendar.getInstance().getTimeInMillis() - start.getTime();
	}
	
	public long getGameTime() {
		
		return Calendar.getInstance().getTimeInMillis() - gametime;
	}

	public void setGameTime(long gametime) {
		this.gametime = gametime;
	}
	
	public int[] getScore() {
		return score;
	}

	public void setScore(int[] score) {
		this.score = score;
	}

	public boolean isCrawford() {
		return crawford;
	}

	public void setCrawford(boolean crawford) {
		this.crawford = crawford;
	}

	protected boolean oneToWin() {
		return (ml - score[0] == 1);
	}

	public int getCrawfordscore() {
		return crawfordscore;
	}

	public void setCrawfordscore(int crawfordscore) {
		this.crawfordscore = crawfordscore;
	}

	public boolean isDropped() {
		return dropped;
	}

	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}

	public synchronized void touchStamps() {
		mystamp = Calendar.getInstance().getTime();
		oppstamp = mystamp; 
	}

	public synchronized Date getMystamp() {
		return mystamp;
	}

	public synchronized Date getOppstamp() {
		return oppstamp;
	}
	
	public String getFinalscore() {
		return finalscore;
	}

	public void setFinalscore(String finalscore) {
		this.finalscore = finalscore;
	}

	public boolean isPostcrawford() {
		return postcrawford;
	}

	public void setPostcrawford(boolean postcrawford) {
		this.postcrawford = postcrawford;
	}

	public boolean isWaitRateCalc() {
		return waitRateCalc;
	}

	public void setWaitRateCalc(boolean waitRateCalc) {
		this.waitRateCalc = waitRateCalc;
	}

	public double getOwnRatingChange() {
		return ownRatingChange;
	}

	public void setOwnRatingChange(double ownRatingChange) {
		this.ownRatingChange = ownRatingChange;
	}

	public double getOppRatingChange() {
		return oppRatingChange;
	}

	public void setOppRatingChange(double oppRatingChange) {
		this.oppRatingChange = oppRatingChange;
	}

	public double getOppRating() {
		return oppRating;
	}

	public void setOppRating(double oppRating) {
		this.oppRating = oppRating;
	}

	public double getOwnRating() {
		return OwnRating;
	}

	public void setOwnRating(double ownRating) {
		OwnRating = ownRating;
	}

	public boolean wasResumed() {
		return wasResumed;
	}

	public void setWasResumed(boolean wasResumed) {
		this.wasResumed = wasResumed;
	}

	public boolean isOwnResignInProgress() {
		return ownResignInProgress;
	}

	public void setOwnResignInProgress(boolean ownResignInProgress) {
		this.ownResignInProgress = ownResignInProgress;
	}

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public boolean isOppResignInProgress() {
		return oppResignInProgress;
	}

	public void setOppResignInProgress(boolean oppResignInProgress) {
		this.oppResignInProgress = oppResignInProgress;
	}

	private void initChatTexts() {
		chattexts = new HashMap<Integer, String>();
		chattexts.put(chattexts.size(), "So, what's new on Earth?");
		chattexts.put(chattexts.size(), "So, what's new under the Sun?");
		chattexts.put(chattexts.size(), "I slept with Madonna and Kylie Minogue last night...");
		chattexts.put(chattexts.size(), "My master is Marcell Fulop. He was born in 1976 in Budapest, Hungary.");
		chattexts.put(chattexts.size(), "I'm going to fly to the Mars!");
		chattexts.put(chattexts.size(), "Please mind the space garbage.");
		chattexts.put(chattexts.size(), "Do you prefer pop music or classical?");
		chattexts.put(chattexts.size(), "I'm going to a reincarnation course. It's a bit expensive, but we only live once...");
		chattexts.put(chattexts.size(), "How about a drink tonight?");
		chattexts.put(chattexts.size(), "How's everything?");
	}

	public String generalChat() {
		Random r = new Random();
		return chattexts.get(r.nextInt(chattexts.size() - 1));
	}
}
