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

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import cx.ath.matthew.debug.Debug;
import static cx.ath.matthew.salliere.Gettext._;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
  Generate handicap file from many results
*/
public class Handicaps extends Salliere
{

   static void syntax()
   {
      String version = Package.getPackage("cx.ath.matthew.salliere")
                              .getImplementationVersion();
      System.out.println("Salliere Duplicate Bridge Scorer (Handicap generator) - version "+version);
      System.out.println("Usage: salliere-handicap [options] <names.csv> ...");
      System.out.println("   Options: --help --top=<n>");
   }

	static Map<String, Vector<Double> > readScores(Vector<String> files) throws IOException
	{
		Map<String, Vector<Double> > scores = new HashMap<String, Vector<Double> >();

		for (String file: files) {
			List<Pair> pairs;

			pairs = readPairs(new FileInputStream(file), null);
			for (Pair p: pairs) {
				for (String name: p.getNames()) {
					if (scores.containsKey(name)) {
						scores.get(name).add(p.getPercentage());
					} else {
						Vector<Double> t = new Vector<Double>();
						t.add(p.getPercentage());
						scores.put(name, t);
					}
				}
			}
		}
		return scores;
	}

	static Map<String, Double> calculateHandicaps(Map<String, Vector<Double> > scores, Integer topn)
	{
		Map<String, Double> handicaps = new HashMap<String, Double>();

		for (String name: scores.keySet()) {
			Vector<Double> v = scores.get(name);
			Collections.sort(v);
			int n = null != topn ? topn : v.size();
			if (n > v.size()) n = v.size();
			double total = 0;
			if (Debug.debug) Debug.print("Calculating handicap for "+name+", input values="+v.toString()+", using the top "+n+" values");
			for (int i = v.size()-1; i >= v.size()-n; i--) {
				total += (double) v.elementAt(i);
			}
			handicaps.put(name, total / n);
		}
		return handicaps;
	}

	static void printHandicaps(OutputStream os, Map<String, Double> handicaps) throws IOException
	{
		CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
		for (String name: handicaps.keySet()) {
			String[] s = new String[2];
			s[0] = name;
			s[1] = handicaps.get(name).toString();
			out.writeRecord(s);
		}
		out.close();
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
         Vector<String> files = new Vector<String>();
         HashMap<String, String> options = new HashMap<String, String>();
         options.put("--help", null);
         options.put("--top", null);
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

         if (null != options.get("--help")) {
            syntax();
            System.exit(1);
         }

         if (files.size() == 0) {
            System.out.println(_("You must specify some names.csv files"));
            syntax();
            System.exit(1);
         }

			Map<String, Vector<Double> > scores = readScores(files);
			Map<String, Double> handicaps = calculateHandicaps(scores, null == options.get("--top") ? null : new Integer(options.get("--top")));
			printHandicaps(System.out, handicaps);

      } catch (Exception e) {
         if (Debug.debug) {
            Debug.setThrowableTraces(true);
            Debug.print(Debug.ERR, e);
         }
         System.out.println(_("Salliere failed to compute results: ")+e.getMessage());
         System.exit(1);
      }
	}
}
