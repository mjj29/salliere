/* 
 * Salliere Duplicate Bridge Scorer
 * 
 * Copyright (C) 2007 Matthew Johnson
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License Version 2 as published by
 * the Free Software Foundation.  This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.  You should have received a
 * copy of the GNU General Public License along with this program; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * To Contact me, please email src@matthew.ath.cx
 *
 */

package cx.ath.matthew.salliere;

import cx.ath.matthew.debug.Debug;
import static cx.ath.matthew.salliere.Gettext._;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class Leaders
{
   static void syntax()
   {
      String version = Package.getPackage("cx.ath.matthew.salliere")
                              .getImplementationVersion();
      System.out.println("Salliere Duplicate Bridge Scorer - version "+version);
      System.out.println("Usage: leaderboard <names.csv ...>");
   }
   public static void main(String[] args)
   {
      try {
         if (Debug.debug) {
            File f = new File("debug.conf");
            if (f.exists())
               Debug.loadConfig(f);
            Debug.setThrowableTraces(true);
         }
         HashMap options = new HashMap();
         options.put("--help", null);
         List files = new Vector();

         int i;
         for (i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
               String[] opt = args[i].split("=");
               if (Debug.debug) Debug.print(Arrays.asList(opt));
               if (options.containsKey(opt[0])) {
                  if (opt.length == 1)
                     options.put(opt[0], "true");
                  else
                     options.put(opt[0], opt[1]);
               } else {
                  System.out.println(_("Error: unknown option ")+opt[0]);
                  syntax();
                  System.exit(1);
               }
            } else
               files.add(args[i]);
         }

         if (0 == files.size()) {
            System.out.println(_("You must specify at least one names file"));
            syntax();
            System.exit(1);
         }

         if (null != options.get("--help")) {
            syntax();
            System.exit(1);
         }

         TablePrinter printer = new AsciiTablePrinter(System.out);
         LeaderBoard leaders = new LeaderBoard();

         for (Object f: files) {
            System.out.println(f);
            List pairs = Salliere.readPairs(new FileInputStream((String) f));
            if (Debug.debug) Debug.print(Debug.DEBUG, "Got results: "+pairs);
            Salliere.results(pairs, printer, false, false, null, false);
            for (Object p: pairs) {
               for (String s: ((Pair) p).getNames())
                  if (null != s && s.length() != 0)
                     leaders.update(s, ((Pair) p).getLPs());
            }
            printer.gap();
            Pair.resetNames();
         }

         printer.print(new String[] { _("Name"), _("Points") }, leaders.getLeaderBoard());

      } catch (Exception e) {
         if (Debug.debug) {
            Debug.setThrowableTraces(true);
            Debug.print(e);
         }
         System.out.println(_("LeaderBoard failed: ")+e.getMessage());
         System.exit(1);
      }
   }
}
