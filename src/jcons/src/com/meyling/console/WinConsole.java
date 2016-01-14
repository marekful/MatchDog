/* $Id: WinConsole.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import jcons.src.com.meyling.console.Console;

/**
 * Windows console colors by JNI.
 * To load the JNI library you should provide information about
 * the library location by giving the JVM an parameter like:
 *      <code>-Djava.library.path=lib</code>
 * 
 * @author  Michael Meyling
 */
public class WinConsole implements Console {

    public static final int FOREGROUND_BLUE = 0x0001;
    public static final int FOREGROUND_GREEN = 0x0002;
    public static final int FOREGROUND_RED = 0x0004;
    public static final int FOREGROUND_INTENSITY = 0x0008;
    public static final int BACKGROUND_BLUE = 0x0010;
    public static final int BACKGROUND_GREEN = 0x0020;
    public static final int BACKGROUND_RED = 0x0040;
    public static final int BACKGROUND_INTENSITY = 0x0080;
    public static final int COMMON_LVB_LEADING_BYTE = 0x0100;
    public static final int COMMON_LVB_TRAILING_BYTE = 0x0200;
    public static final int COMMON_LVB_GRID_HORIZONTAL = 0x0400;
    public static final int COMMON_LVB_GRID_LVERTICAL = 0x0800;
    public static final int COMMON_LVB_GRID_RVERTICAL = 0x1000;
    public static final int COMMON_LVB_REVERSE_VIDEO = 0x4000;
    public static final int COMMON_LVB_UNDERSCORE = 0x8000;

    /** Initial value. */ 
    public static int initialColors = 0x0007;

    /**
     * Save current text attribute and enlist restore function. 
     */
    public static native void init();

    /**
     * Get current color attribute.
     * 
     * @return  Color attribute.
     */
    public static native int getColors();

    /**
     * Set color attribute.
     * 
     * @param   colors  Colors.
     */
    public static native void setColors(int colors);

    /** Map of foreground colors. */
    private Map fcolors = new HashMap();

    /** Map of background colors. */
    private Map bcolors = new HashMap();

    /* load library */
    static {
        try {
            System.loadLibrary("jcons");
        } catch (UnsatisfiedLinkError e1) {
            // try again on different locations
            try {
                System.load(new File("." ,"jcons.dll").getAbsolutePath());
            } catch (UnsatisfiedLinkError e2) {
                System.load(new File("lib" ,"jcons.dll").getAbsolutePath());
            }
        }
        init();
        initialColors = getColors();
    }

    /**
     * Create new windows console access.
     */
    protected WinConsole() {
        fcolors.put(ConsoleForegroundColor.BLACK, new Integer(0));
        fcolors.put(ConsoleForegroundColor.DARK_RED, new Integer(FOREGROUND_RED));
        fcolors.put(ConsoleForegroundColor.DARK_GREEN, new Integer(FOREGROUND_GREEN));
        fcolors.put(ConsoleForegroundColor.DARK_YELLOW, new Integer(FOREGROUND_RED | FOREGROUND_GREEN));
        fcolors.put(ConsoleForegroundColor.DARK_BLUE, new Integer(FOREGROUND_BLUE));
        fcolors.put(ConsoleForegroundColor.DARK_MAGENTA, new Integer(FOREGROUND_RED | FOREGROUND_BLUE));
        fcolors.put(ConsoleForegroundColor.DARK_CYAN, new Integer(FOREGROUND_GREEN | FOREGROUND_BLUE));
        fcolors.put(ConsoleForegroundColor.GREY, new Integer(FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE));
        fcolors.put(ConsoleForegroundColor.LIGHT_RED, new Integer(FOREGROUND_RED | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.LIGHT_GREEN, new Integer(FOREGROUND_GREEN | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.LIGHT_YELLOW, new Integer(FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.LIGHT_BLUE, new Integer(FOREGROUND_BLUE | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.LIGHT_MAGENTA, new Integer(FOREGROUND_RED | FOREGROUND_BLUE | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.LIGHT_CYAN, new Integer(FOREGROUND_GREEN | FOREGROUND_BLUE | FOREGROUND_INTENSITY));
        fcolors.put(ConsoleForegroundColor.WHITE, new Integer(FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE | FOREGROUND_INTENSITY));

        bcolors.put(ConsoleBackgroundColor.BLACK, new Integer(0));
        bcolors.put(ConsoleBackgroundColor.DARK_RED, new Integer(BACKGROUND_RED));
        bcolors.put(ConsoleBackgroundColor.DARK_GREEN, new Integer(BACKGROUND_GREEN));
        bcolors.put(ConsoleBackgroundColor.DARK_YELLOW, new Integer(BACKGROUND_RED | BACKGROUND_GREEN));
        bcolors.put(ConsoleBackgroundColor.DARK_BLUE, new Integer(BACKGROUND_BLUE));
        bcolors.put(ConsoleBackgroundColor.DARK_MAGENTA, new Integer(BACKGROUND_RED | BACKGROUND_BLUE));
        bcolors.put(ConsoleBackgroundColor.DARK_CYAN, new Integer(BACKGROUND_GREEN | BACKGROUND_BLUE));
        bcolors.put(ConsoleBackgroundColor.GREY, new Integer(BACKGROUND_RED | BACKGROUND_GREEN | BACKGROUND_BLUE));
    }

    public void setForegroundColor(ConsoleForegroundColor color) {
        setColors((getColors() & 0xFFF0) | ((Integer) fcolors.get(color)).intValue());
    }

    public void setBackgroundColor(ConsoleBackgroundColor color) {
        setColors((getColors() & 0xFF0F) | ((Integer) bcolors.get(color)).intValue());
    }

    public void resetColors() {
        setColors(initialColors);
    }
    
}
