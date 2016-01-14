/* $Id: UnixConsole.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
 * 
 * This file is part of the project "JCons"
 *    http://sourceforge.net/projects/jcons
 *
 * Copyright 2006,  Michael Meyling <mime@qedeq.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcons.src.com.meyling.console;

import java.util.HashMap;
import java.util.Map;
import jcons.src.com.meyling.console.Console;

/**
 * Unix console colors with ESC sequences.
 * 
 * @author  Michael Meyling
 */
public class UnixConsole implements Console {
    
    /** Resets the color to default. */
    public static final String RESET = "\u001B[0m";

    /** Black foreground. */
    public static final String BLACK = "\u001B[22;30m";

    /** Normal intensity red. */
    public static final String RED = "\u001B[22;31m";

    /** Normal intensity green. */
    public static final String GREEN = "\u001B[22;32m";

    /** Normal intesity yellow (brown). */
    public static final String YELLOW = "\u001B[22;33m";

    /** Normal intensity blue. */
    public static final String BLUE = "\u001B[22;34m";

    /** Normal intensity magenta (purple). */
    public static final String MAGENTA = "\u001B[22;35m";

    /** Normal intensity cyan. */
    public static final String CYAN = "\u001B[22;36m";

    /** Normal intesity white (grey). */
    public static final String WHITE = "\u001B[22;37m";

    /** Bright black (grey). */
    public static final String GREY = "\u001B[1;30m";

    /** Bright red. */
    public static final String LIGHT_RED = "\u001B[1;31m";

    /** Bright green. */
    public static final String LIGHT_GREEN = "\u001B[1;32m";

    /** Bright yellow. */
    public static final String LIGHT_YELLOW = "\u001B[1;33m";

    /** Bright blue. */
    public static final String LIGHT_BLUE = "\u001B[1;34m";

    /** Bright magenta. */
    public static final String LIGHT_MAGENTA = "\u001B[1;35m";

    /** Bright cyan. */
    public static final String LIGHT_CYAN = "\u001B[1;36m";

    /** Bright white (white). */
    public static final String LIGHT_WHITE = "\u001B[1;37m";

    /** Black background. */
    public static final String BACKGROUND_BLACK = "\u001B[40m";

    /** Red background. */
    public static final String BACKGROUND_RED = "\u001B[41m";

    /** Green background. */
    public static final String BACKGROUND_GREEN = "\u001B[42m";

    /** Yellow background. */
    public static final String BACKGROUND_YELLOW = "\u001B[43m";

    /** Blue background. */
    public static final String BACKGROUND_BLUE = "\u001B[44m";

    /** Magenta background. */
    public static final String BACKGROUND_MAGENTA = "\u001B[45m";

    /** Cyan background. */
    public static final String BACKGROUND_CYAN = "\u001B[46m";

    /** White background (grey). */
    public static final String BACKGROUND_WHITE = "\u001B[47m";

    /** Map of foreground colors. */
    private Map fcolors = new HashMap();

    /** Map of background colors. */
    private Map bcolors = new HashMap();

    /**
     * Create new unix console access.
     */
    protected UnixConsole() {
        fcolors.put(ConsoleForegroundColor.BLACK, BLACK);
        fcolors.put(ConsoleForegroundColor.DARK_RED, RED);
        fcolors.put(ConsoleForegroundColor.DARK_GREEN, GREEN);
        fcolors.put(ConsoleForegroundColor.DARK_YELLOW, YELLOW);
        fcolors.put(ConsoleForegroundColor.DARK_BLUE, BLUE);
        fcolors.put(ConsoleForegroundColor.DARK_MAGENTA, MAGENTA);
        fcolors.put(ConsoleForegroundColor.DARK_CYAN, CYAN);
        fcolors.put(ConsoleForegroundColor.GREY, WHITE);
        fcolors.put(ConsoleForegroundColor.LIGHT_RED, LIGHT_RED);
        fcolors.put(ConsoleForegroundColor.LIGHT_GREEN, LIGHT_GREEN);
        fcolors.put(ConsoleForegroundColor.LIGHT_YELLOW, LIGHT_YELLOW);
        fcolors.put(ConsoleForegroundColor.LIGHT_BLUE, LIGHT_BLUE);
        fcolors.put(ConsoleForegroundColor.LIGHT_MAGENTA, LIGHT_MAGENTA);
        fcolors.put(ConsoleForegroundColor.LIGHT_CYAN, LIGHT_CYAN);
        fcolors.put(ConsoleForegroundColor.WHITE, LIGHT_WHITE);

        bcolors.put(ConsoleBackgroundColor.BLACK, BACKGROUND_BLACK);
        bcolors.put(ConsoleBackgroundColor.DARK_RED, BACKGROUND_RED);
        bcolors.put(ConsoleBackgroundColor.DARK_GREEN, BACKGROUND_GREEN);
        bcolors.put(ConsoleBackgroundColor.DARK_YELLOW, BACKGROUND_YELLOW);
        bcolors.put(ConsoleBackgroundColor.DARK_BLUE, BACKGROUND_BLUE);
        bcolors.put(ConsoleBackgroundColor.DARK_MAGENTA, BACKGROUND_MAGENTA);
        bcolors.put(ConsoleBackgroundColor.DARK_CYAN, BACKGROUND_CYAN);
        bcolors.put(ConsoleBackgroundColor.GREY, BACKGROUND_WHITE);
    }

    public void setForegroundColor(ConsoleForegroundColor color) {
        System.out.print((String) fcolors.get(color));
    }

    public void setBackgroundColor(ConsoleBackgroundColor color) {
        System.out.print((String) bcolors.get(color));
    }

    public void resetColors() {
        System.out.print(RESET);
    }

}
