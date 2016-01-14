/* $Id: ConsoleForegroundColor.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
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

import java.util.ArrayList;
import java.util.List;

/**
 * Common foreground colors.
 * 
 * @author  Michael Meyling
 */
public class ConsoleForegroundColor {

    /** Black foreground. */
    public static final ConsoleForegroundColor BLACK
        = new ConsoleForegroundColor("black");

    /** Dark red foreground. */
    public static final ConsoleForegroundColor DARK_RED
        = new ConsoleForegroundColor("dark red");

    /** Dark green foreground. */
    public static final ConsoleForegroundColor DARK_GREEN
        = new ConsoleForegroundColor("dark green");

    /** Dark yellow foreground. */
    public static final ConsoleForegroundColor DARK_YELLOW
        = new ConsoleForegroundColor("dark yellow");

    /** Dark blue foreground. */
    public static final ConsoleForegroundColor DARK_BLUE
        = new ConsoleForegroundColor("dark blue");

    /** Dark magenta foreground. */
    public static final ConsoleForegroundColor DARK_MAGENTA
        = new ConsoleForegroundColor("dark magenta");

    /** Dark cyan foreground. */
    public static final ConsoleForegroundColor DARK_CYAN
    = new ConsoleForegroundColor("dark cyan");

    /** Grey foreground ("dark white"). */
    public static final ConsoleForegroundColor GREY
        = new ConsoleForegroundColor("grey");

    /** Bright red foreground. */
    public static final ConsoleForegroundColor LIGHT_RED
        = new ConsoleForegroundColor("light red");

    /** Bright green foreground. */
    public static final ConsoleForegroundColor LIGHT_GREEN
        = new ConsoleForegroundColor("light green");

    /** Bright yellow foreground. */
    public static final ConsoleForegroundColor LIGHT_YELLOW
        = new ConsoleForegroundColor("light yellow");

    /** Bright blue foreground. */
    public static final ConsoleForegroundColor LIGHT_BLUE
        = new ConsoleForegroundColor("light blue");

    /** Bright magenta foreground. */
    public static final ConsoleForegroundColor LIGHT_MAGENTA
        = new ConsoleForegroundColor("light magenta");

    /** Bright cyan foreground. */
    public static final ConsoleForegroundColor LIGHT_CYAN
        = new ConsoleForegroundColor("light cyan");

    /** White foreground. */
    public static final ConsoleForegroundColor WHITE
        = new ConsoleForegroundColor("white");

    /** List of foreground colors. */
    private static List list = new ArrayList();

    /** Name of color. */
    private final String name;

    static {
        list.add(BLACK);
        list.add(DARK_RED);
        list.add(DARK_GREEN);
        list.add(DARK_YELLOW);
        list.add(DARK_BLUE);
        list.add(DARK_MAGENTA);
        list.add(DARK_CYAN);
        list.add(GREY);
        list.add(LIGHT_RED);
        list.add(LIGHT_GREEN);
        list.add(LIGHT_YELLOW);
        list.add(LIGHT_BLUE);
        list.add(LIGHT_MAGENTA);
        list.add(LIGHT_CYAN);
        list.add(WHITE);
    }

    /**
     * Constructor.
     * 
     * @param   name    Color name.
     */
    private ConsoleForegroundColor(final String name) {
        this.name = name;
    }

    /**
     * Get <code>i</code>-th color.
     * <code>i</code> must be between 0 and 
     * {@link #size()}<code> - 1</code>.. 
     * 
     * @param   i  Get this color.
     * @return  Color.
     */
    public static final ConsoleForegroundColor get(int i) {
        return (ConsoleForegroundColor) list.get(i);
    }

    /**
     * Get number of colors.
     * 
     * @return  Color number.
     */
    public static final int size() {
        return list.size();
    }

    /**
     * Get color name.
     * 
     * @return  Color name.
     */
    public final String getName() {
        return name;
    }

}
