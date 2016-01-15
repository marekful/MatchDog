/**
 * 
 */
package matchdog;

import java.util.HashMap;
import java.util.Map;

class PlayerPrefs {

	Object [] prefsObj;
	String name, pw;
	boolean autoLogin, autoInvite, autoJoin, autoInviteSaved, autoJoinSaved;
	int maxml, checkquerply, cubedecply, expDivider, repLimit, gnubgPort, listenerPort;
	double noise;
	String [] movefilters = {"", "", "", "", "", "", "", "", "", ""};
	Map<String, Integer> preferredOpps;
	
	PlayerPrefs(Object [] prefsObj) {
		this.prefsObj = prefsObj;
		preferredOpps = new HashMap<String, Integer>();
		
		initPrefs();
		
	}
	
	void initPrefs () {
		autoInviteSaved = true;
		autoJoinSaved = true;
		int j = 0;
		try {
			for(int i = 0; i < prefsObj.length; i++) {
				if(i == 0) {
					name = prefsObj[i].toString();
				} else if(i == 1) {
					pw = prefsObj[i].toString();
				} else if(i == 2) {
					autoLogin = Boolean.parseBoolean(prefsObj[i].toString());
				} else if(i == 3) {
					autoInvite = Boolean.parseBoolean(prefsObj[i].toString());
				} else if(i == 4) {
					autoJoin = Boolean.parseBoolean(prefsObj[i].toString());				
				} else if(i == 5) {
					autoInviteSaved = Boolean.parseBoolean(prefsObj[i].toString());
				} else if(i == 6) {
					autoJoinSaved = Boolean.parseBoolean(prefsObj[i].toString());
				} else if(i == 7) {
					maxml = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 8) {
					expDivider = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 9) {
					repLimit = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 10) {
					checkquerply = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 11) {
					cubedecply = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 12) {
					noise = Double.parseDouble(prefsObj[i].toString());
				} else if(i > 12 && i < 63 && (i - 3) % 5 == 0) {
					movefilters[j] = prefsObj[i].toString() + " "
							+ prefsObj[i + 1].toString() + " "
							+ prefsObj[i + 2].toString() + " "
							+ prefsObj[i + 3].toString() + " "
							+ prefsObj[i + 4].toString() + " ";
					j++;				
					
				} else if(i > 62 && i < 83 && i % 2 == 1) {
					if(!prefsObj[i].toString().equals("") 
							&& !prefsObj[i].toString().equals("0")) {
						preferredOpps.put(prefsObj[i].toString(), 
								Integer.parseInt(prefsObj[i + 1].toString()));
					}
				} else if(i == 83) {
					gnubgPort = Integer.parseInt(prefsObj[i].toString());
				} else if(i == 84) {
					listenerPort = Integer.parseInt(prefsObj[i].toString());
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public String [] showPrefs() {
		//int maxml, checkquerply, cubedecply;
		//double noise;
		//String [] movefilters 
		String [] out = {"", "", "", "", "", "", "", "", ""};
		out[0] =  " max. match length  : " + maxml;
		out[1] =  "   gnubg";
		out[2] =  "     checquer play  : " + checkquerply + " ply";
		out[3] =  "     cube decision  : " + cubedecply + " ply";
		out[4] =  "     noise          : " + noise;

		if(checkquerply == 1 ) {
			out[5] =  "     movefilters (1 ply): ";			
			out[6] =  "       " + movefilters[0];
		} else if(checkquerply == 2) {
			out[5] =  "     movefilters (2 ply): ";	
			out[6] =  "       " + movefilters[1];
			out[7] =  "       " + movefilters[2];
		}  else if(checkquerply == 3) {
			out[5] =  "     movefilters (3 ply): ";	
			out[6] =  "       " + movefilters[3];
			out[7] =  "       " + movefilters[4];
			out[8] =  "       " + movefilters[5];
		} 

		return out;	
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public boolean isAutologin() {
		return autoLogin;
	}

	public void setAutologin(boolean autologin) {
		this.autoLogin = autologin;
	}

	public boolean isAutoinvite() {
		return autoInvite;
	}

	public void setAutoinvite(boolean autoinvite) {
		this.autoInvite = autoinvite;
	}

	public Map<String, Integer> getPreferredOpps() {
		return preferredOpps;
	}

	public int getMaxml() {
		return maxml;
	}

	public void setMaxml(int maxml) {
		this.maxml = maxml;
	}

	public int getCheckquerply() {
		return checkquerply;
	}

	public void setCheckquerply(int checkquerply) {
		this.checkquerply = checkquerply;
	}

	public int getCubedecply() {
		return cubedecply;
	}

	public void setCubedecply(int cubedecply) {
		this.cubedecply = cubedecply;
	}

	public double getNoise() {
		return noise;
	}

	public void setNoise(double noise) {
		this.noise = noise;
	}
	
	public String getMoveFilter(int i) {
		return movefilters[i];
	}

	public boolean isAutojoin() {
		return autoJoin;
	}

	public void setAutojoin(boolean autojoin) {
		this.autoJoin = autojoin;
	}

	public boolean isAutoresume() {
		return autoInviteSaved;
	}

	public void setAutoresume(boolean autoresume) {
		this.autoInviteSaved = autoresume;
	}

	public boolean isAutoInviteSaved() {
		return autoInviteSaved;
	}

	public void setAutoInviteSaved(boolean autoInviteSaved) {
		this.autoInviteSaved = autoInviteSaved;
	}

	public boolean isAutoJoinSaved() {
		return autoJoinSaved;
	}

	public void setAutoJoinSaved(boolean autoJoinSaved) {
		this.autoJoinSaved = autoJoinSaved;
	}

	public int getExpDivider() {
		return expDivider;
	}

	public void setExpDivider(int expDivider) {
		this.expDivider = expDivider;
	}

	public int getRepLimit() {
		return repLimit;
	}

	public void setRepLimit(int repLimit) {
		this.repLimit = repLimit;
	}
	
	public int getGnuBgPort() {
		return gnubgPort;
	}
	
	public int getListenerPort() {
		return listenerPort;
	}
}