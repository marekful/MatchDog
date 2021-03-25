/**
 * 
 */
package matchdog;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class PlayerPrefs extends Prefs {

	public final static int GNUBG_USE_EXTERNAL = 0;
	public final static int GNUBG_USE_HINT = 1;

	public Properties props;
	public String username, password, email;
	public boolean autologin, autoinvite, autojoin, autoinvitesaved, autojoinsaved;
	public int maxml, checkquerply, cubedecply, expdivider, replimit, replaytimeout;
    public int gnubgType, gnubgextport, listenerport;
	public double noise;
	public String [] movefilters = {"", "", "", "", "", "", "", "", "", ""};
	public Map<String, Integer> preferredOpps;
	public boolean analyseMatch;

	PlayerPrefs(Properties props) {
		this.props = props;
		preferredOpps = new HashMap<String, Integer>();

		initProps(props);
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
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isAutologin() {
		return autologin;
	}

	public void setAutologin(boolean autologin) {
		this.autologin = autologin;
	}

	public boolean isAutoinvite() {
		return autoinvite;
	}

	public void setAutoinvite(boolean autoinvite) {
		this.autoinvite = autoinvite;
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

	public int getGnubgType() {
		return gnubgType;
	}

	public void setGnubgType(int gnubgType) {
		this.gnubgType = gnubgType;
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
		return autojoin;
	}

	public void setAutojoin(boolean autojoin) {
		this.autojoin = autojoin;
	}

	public boolean isAutoresume() {
		return autoinvitesaved;
	}

	public void setAutoresume(boolean autoresume) {
		this.autoinvitesaved = autoresume;
	}

	public boolean isAutoinvitesaved() {
		return autoinvitesaved;
	}

	public void setAutoinvitesaved(boolean autoinvitesaved) {
		this.autoinvitesaved = autoinvitesaved;
	}

	public boolean isAutojoinsaved() {
		return autojoinsaved;
	}

	public void setAutojoinsaved(boolean autojoinsaved) {
		this.autojoinsaved = autojoinsaved;
	}

	public int getExpdivider() {
		return expdivider;
	}

	public void setExpdivider(int expdivider) {
		this.expdivider = expdivider;
	}

	public int getReplimit() {
		return replimit;
	}

	public void setReplimit(int replimit) {
		this.replimit = replimit;
	}
	
	public int getGnuBgPort() {
		return gnubgextport;
	}
	
	public int getListenerport() {
		return listenerport;
	}

    public int getReplayTimeout() {
        return replaytimeout;
    }

	public boolean analyseMatch() {
		return analyseMatch;
	}

	public void setAnalyseMatch(boolean analyseMatch) {
		this.analyseMatch = analyseMatch;
	}
}