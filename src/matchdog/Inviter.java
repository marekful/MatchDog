package matchdog;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Inviter extends Thread {

	Timer t;
	String name, stateStr;
	MatchDog server;
	int length;
	boolean wait, dead;
	Date start;
	Inviter(MatchDog server, String name, int length) {
		
		this.name = name;
		this.server = server;
		this.length = length;
		setName("Inviter-" + name + "-" + length);
		
		this.start();
	}
	
	public void run() {
		stateStr = "";
		wait = true;
		dead = false;
		start = Calendar.getInstance().getTime();
		
		
		t = new Timer() {
		
				@Override
				public void schedule(TimerTask task, long delay, long period) {
					// TODO Auto-generated method stub
				super.schedule(task, delay, period);
			}
			
		};
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				if(Calendar.getInstance().getTime().getTime() - start.getTime() > 5000) {
					cancel();
				}
				checkPlayerReadyState();
			}
			

			public boolean cancel() {
				stopTimer();
				return true;
			}				


		}, 0000L, 2000L);
		
		server.printDebug("inviter started");
	}
	
	private void checkPlayerReadyState() {
		server.printDebug("inviter: checking user state");		
		
		server.fibsout.println("who " + name);
		
		while(wait) {
			try {
				Inviter.sleep(100);
			} catch(InterruptedException e) {
				e.printStackTrace();
				t.cancel();
				return;
			}
		}
		wait = true;
		if(stateStr.length() < 1)
			return;
		if(stateStr.split(" ")[2].equals("-") && stateStr.split(" ")[4].equals("1")) {
			String lengthstr = Integer.toString(length);
			if(length == 0) 
				lengthstr = "";
			server.fibsout.println("invite " + name + " " + lengthstr);
			stateStr = "";
			server.printDebug("inviter: INVITING user (invite " + name + " " + lengthstr + ")");			
		}
	}

	public String getStateStr() {
		return stateStr;
	}

	public void setStateStr(String stateStr) {
		this.stateStr = stateStr;
	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	public void stopTimer() {
		server.stopInviter();
	}	
	

}
