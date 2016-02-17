package matchdog;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by marekful on 16/02/2016.
 */
public class ProgramPrefs extends Prefs {

    // Fibs host
    public String 	fibshost;
    // Fibs port
    public int 	fibsport;
    // Array of full paths to gnubg executable and params (-t for no gui, -q for silent)
    // Each will be tried, firsst succeeds used
    public String[] gnubgCmdArr = new String[] {"", ""};
    // A small shell script that uploads match log files
    // comes with the package and should be installed
    // so it's executable by the server. Give its path here.
    public String 	scpscriptpath;

    public String platform;
    public String version;

    ProgramPrefs() {
        initPrefs();
    }

    private void initPrefs() {
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream("../config/matchdog.prefs");
            props.load(in);
            in.close();

            initProps(props);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFibshost() {
        return fibshost;
    }

    public int getFibsport() {
        return fibsport;
    }

    public String[] getGnubgCmdArr() {
        return gnubgCmdArr;
    }

    public String getScpscriptpath() {
        return scpscriptpath;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVersion() {
        return version;
    }
}
