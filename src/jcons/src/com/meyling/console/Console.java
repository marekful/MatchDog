/* $Id: Console.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
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

/**
 * Provides console color access.
 * 
 * @author  Michael Meyling
 */
public interface Console {

    /**
     * Set console foreground color.
     * 
     * @param   color   Set this color.
     */
    public void setForegroundColor(ConsoleForegroundColor color);

    /**
     * Set console background color.
     * 
     * @param   color   Set this color.
     */
    public void setBackgroundColor(ConsoleBackgroundColor color);

    /**
     * Reset console backgound and foreground colors to initial values.
     */
    public void resetColors();

}
