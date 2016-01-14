/* $Id: ConsoleBackgroundColor.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
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
 * All common background colors.
 * 
 * @author  Michael Meyling
 */
public class ConsoleBackgroundColor {

    /** Black background. */
    public static final ConsoleBackgroundColor BLACK
        = new ConsoleBackgroundColor("black");

    /** Red background. */
    public static final ConsoleBackgroundColor DARK_RED
        = new ConsoleBackgroundColor("dark red");

    /** Green background. */
    public static final ConsoleBackgroundColor DARK_GREEN
        = new ConsoleBackgroundColor("dark green");

    /** Yellow background. */
    public static final ConsoleBackgroundColor DARK_YELLOW 
        = new ConsoleBackgroundColor("dark yellow");

    /** Blue background. */
    public static final ConsoleBackgroundColor DARK_BLUE 
        = new ConsoleBackgroundColor("dark blue");

    /** Magenta background. */
    public static final ConsoleBackgroundColor DARK_MAGENTA 
        = new ConsoleBackgroundColor("dark magenta");

    /** Cyan background. */
    public static final ConsoleBackgroundColor DARK_CYAN 
        = new ConsoleBackgroundColor("dark cyan");

    /** Grey background. */
    public static final ConsoleBackgroundColor GREY 
        = new ConsoleBackgroundColor("grey");

    /** List of all background colors. */
    private static List list = new ArrayList();

    /** Color name. */
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
    }

    /**
     * Constructor.
     * 
     * @param   name    Color name.
     */
    private ConsoleBackgroundColor(final String name) {
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
    public static final ConsoleBackgroundColor get(int i) {
        return (ConsoleBackgroundColor) list.get(i);
    }

    /**
     * Get number of background colors.
     *
     * @return  Color numbers.
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
