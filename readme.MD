***REQUIREMENTS***

**OS:**
However MatchDogServer is written completely in Java and is a console program (without GUI),
it has only been tested in a bash shell of a Debain Linux box. It will probably run in other
Linux distributions and it will most likekly NOT run in MS Windows (cmd.exe).

**Java:**
I used the 1.6 update 3 release of the JDK to write and test the program.
It will probably also work with any 1.5 release of JRE but has not been tested with any earlier release.

**GNU Backgammon:**
In order to successfully run the program you should be able to run gnubg.
MatchDogServer will run the gnubg binary with `-q -t` options (silent, no gui). Give
full path to the binary in the PROGRAM PARAMETERES section.

MatchDogServer should be able to open and also to connect
to a socket on the localhost on the port number specified
in the PROGRAM PARAMETERS section of the file 'MatchDogServer.java'.

**FIBS:**
MatchDog's FIBS player must use the following 'toggle' settings
to work correctly:
```allowpip        YES
autoboard       YES
autodouble      NO
automove        NO
bell            NO
crawford        YES
double          NO
greedy          NO
moreboards      YES
moves           NO
notify          YES
ratings         YES
ready           YES
report          YES
silent          NO
telnet          YES
wrap            NO
```



***SETUP***

NOTE! 
First, some program parameters and at least one user must be set up
in the file `MatchDogServer.java` before compilation and run. 

See comments for the first five variables at the very beginning of the file.

See comment for user preferences setup in the same file.

Also, for the 'show log' function to work, the first five
variables in the included `ftpupload.sh` file need to be set.

***COMPILE***

One way to compile the sources on a Linux a system is to
 change directory to `[PROJECT_ROOT]/bin` 
 (or create it if necessary) and issue the following
 three commands in a bash shell (assuming that the java
 binaries are on the path):

`javac -d .  ../src/jcons/src/com/meyling/console/*.java`

`javac -d .  ../src/etc/TorExitNodeChecker.java`

`javac -d .  ../src/matchdog/*.java`

Make sure files compile without errors. Ignore 'Notes' from jcons.


***USAGE***

On a Linux a system, change directory to change directory to `[PROJECT_ROOT]/bin`  where the compiled java .class
files are located and issue the following command in a bash shell
(assuming that the java binaries are on the path):

`java matchdog/MatchDogServer [n]`

where `[n]` is the index number of the player preferences object
in the player map.

There must be at least one user preferences object instantiated
and put in the players map at index 0.

If no command line parameter is given or not a number is given
or there is no user preferences object at that index of the
map, the default user at index 0 is used.


***FEATURES***

* several preconfigured users can be stored
* run MatchDog in multiple instances
* select user with command line argument
* autologin option
* keep track of saved games
* resume saved matches
* a list of 5 preferred players and match lengths
* RepBot integration: check players' reputation before accepting invitation
* crawford rule handling
* separate user settings (true or false) for:
  - automatically invite preferred players
  - automatically invite players when they log in if there is a saved match
  - automatically join an invitation for a saved match
  - automatically join an invitation 
    (if this is true, then opponent experience
    and reputation is considered before joining)
* opponent experience divider:
    the following must be true to accept an invitation:
      `[match length] < [opponent experience] / [opponent experience divider]`
* opponent reputation limit:
    the opponent's reputation reported by RepBot must be
    greater than this limit
* primitive chatbot: replies something silly when talked to
* manual (runtime) switching of a number of settings currently including:
  - autojoin, autoinvite, crawford
* option to send commands to fibs/gnubg/gnubg socket from command line
* timeout for inactivity
* player statistics - keeps logs of all matches (including dropped ones)
* statistics writer - write to stdout or file
* statistics uploader - upload stat file to ftp server and send url to requester
* player statistics files automatically uploaded to a ftp server on user request

-----
* preconfigurable user preferences include settings for:
  - fibs user name and password
  - a list of 5 preferred players (opponents on fibs)
  - autologin to fibs
  - maximum match length (when accepting invitations)
  - autoinvite preferred players
  - autoinvite for saved matches
  - autojoin for saved mathces
  - autojoin
  - gnubg checkquer play ply
  - gnubg cube decision ply
  - gnubg noise
  - gnubg movefilters




**Example initialisation command sequence**

`set threads 8` # number of cpu cores this instance will use

`set eval sameasanalysis off`

`set evaluation chequer eval plies 3`

`set evaluation cubedecision eval plies 3`

`set evaluation movefilter 1 0 0 16 0.320`

`set evaluation movefilter 2 0 0 16 0.320`

`set evaluation movefilter 3 0 0 16 0.320`

`set evaluation movefilter 3 2 0 4 0.080`

`set evaluation movefilter 4 0 0 16 0.320`

`set evaluation movefilter 4 2 0 4 0.080`

`external localhost:xxxx`



