/* $Id: Test.java,v 1.1 2006/03/17 04:58:55 m31 Exp $
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
 * Test JCons.
 * 
 * @author  Michael Meyling
 */
public class Test {

    /**
     * Main.
     * 
     * @param   args    Ignored.
     */
    public static void main(final String[] args) {
        System.out.println();
        System.out.println("Testing JCons - Colorful Java Console");
        System.out.println();
        final Console console = ConsoleFactory.getConsole();
        console.resetColors();
        for (int i = 0; i < ConsoleBackgroundColor.size(); i++) {
            console.setBackgroundColor(ConsoleBackgroundColor.get(i));
            for (int j = 0; j < ConsoleForegroundColor.size(); j++) {
                System.out.print(" ");
                console.setForegroundColor(ConsoleForegroundColor.get(j));
                System.out.print(ConsoleForegroundColor.get(j).getName());
                if (0 == (j + 1) % 8) {
                    System.out.println();
                }
            }
            System.out.println();
        }
        console.resetColors();
        System.out.println();
        System.out.println();
        for (int i = 9; i > 0; i--) {
            System.out.print("\rwaiting ");
            console.setForegroundColor(ConsoleForegroundColor.LIGHT_RED);
            System.out.print(i);
            console.resetColors();
            System.out.print(" seconds");
            sleep();
        }
        console.setForegroundColor(ConsoleForegroundColor.LIGHT_GREEN);
        System.out.println("\rready            ");
        console.resetColors();
    }

    /**
     * Sleep for one second.
     */
    private static final void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
