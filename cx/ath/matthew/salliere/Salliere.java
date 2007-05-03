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

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Salliere
{

   static class BoardNumberComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         String n1 = ((Board) ob1).getNumber();
         String n2 = ((Board) ob2).getNumber();
         int len = n1.length() > n2.length() ? n1.length() : n2.length();
         
         StringBuilder nb1 = new StringBuilder();
         for (int i = n1.length(); i < len; i++)
            nb1.append('0');
         nb1.append(n1);

         StringBuilder nb2 = new StringBuilder();
         for (int i = n2.length(); i < len; i++)
            nb2.append('0');
         nb2.append(n2);
         
         return nb1.toString().compareTo(nb2.toString());
      }
   }

   static class PairNumberComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         return ((Pair) ob1).getNumber().compareTo(((Pair) ob2).getNumber());
      }
   }

   static class PairPercentageComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         return (int) ((((Pair) ob2).getPercentage()-((Pair) ob1).getPercentage())*100.0);
      }
   }

   static Map/*<String,Board>*/ readBoards(InputStream is) throws IOException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      HashMap/*<String,Board>*/ boards = new HashMap/*<String,Board>*/();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) {
               Debug.print(in.getCurrentRecord()+": "+Arrays.asList(values));
            }
            Board b = (Board) boards.get(values[0]);
            if (null == b) {
               b = new Board(values[0]);
               boards.put(values[0], b);
            }
            b.addHand(new Hand(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return boards;
   }

   static Map/*<String,Pair>*/ readPairs(InputStream is) throws IOException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      HashMap/*<String,Pair>*/ pairs = new HashMap/*<String,Board>*/();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) Debug.print(Arrays.asList(values));
            pairs.put(values[0],new Pair(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return pairs;
   }

   static void writePairs(Map pairs, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Pair p: (Pair[]) pairs.values().toArray(new Pair[0])) 
         out.writeRecord(p.export());
      out.close();
   }

   static void syntax()
   {
      System.out.println("Salliere Duplicate Bridge Scorer - version "+System.getProperty("Version"));
      System.out.println("Syntax: salliere [options] [commands] -- <boards.csv> <names.csv>");
      System.out.println("   Commands: score matchpoint total localpoint results matrix boards");
      System.out.println("   Options: --help --output=[<format>:]file --title=title --orange");
      System.out.println("   Formats: txt html pdf");
   }


   static void writeBoards(Map boards, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            out.writeRecord(h.export());
      out.close();
   }

   public static void score(Map boards) throws ScoreException, ContractParseException
   {
      for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            h.score();
   }

   public static void matchpoint(Map boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
         b.matchPoint();
   }

   public static void total(Map pairs, Map boards)
   {
      for (Pair p: (Pair[]) pairs.values().toArray(new Pair[0])) 
         p.total(boards);
   }

   public static void matrix(Map pairv, Map boardv, TablePrinter tabular) 
   {
      List sortedboards = new ArrayList(boardv.values());
      List sortedpairs = new ArrayList(pairv.values());
      Collections.sort(sortedboards, new BoardNumberComparer());
      Collections.sort(sortedpairs, new PairNumberComparer());
      Board[] boards = (Board[]) sortedboards.toArray(new Board[0]);
      Pair[] pairs = (Pair[]) sortedpairs.toArray(new Pair[0]);

      DecimalFormat format = new DecimalFormat("0.#");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
      
      String[][] matrix = new String[boards.length][pairs.length+1]; 
      for (int i = 0; i < boards.length; i++) {
         matrix[i][0] = boards[i].getNumber();
         for (int j = 0; j < pairs.length; j++) {
            StringBuffer tmp = new StringBuffer();
            matrix[i][j+1] = format.format(boards[i].getMPs(pairs[j].getNumber()), tmp, field).toString();
         }
      }

      String[] headers = new String[pairs.length+1];
      headers[0] = "Board";
      for (int j = 0; j < pairs.length; j++) 
         headers[j+1] = pairs[j].getNumber()+"  ";

      tabular.print(headers, matrix);
      tabular.gap();
   }
   public static void boardbyboard(Map boards, TablePrinter tabular) 
   {
      String[] headers = new String[] { "NS", "EW", "Contract", "By", "Tricks", "Score:", "", "MPs:", "" };
      ArrayList boardv = new ArrayList(boards.values());
      Collections.sort(boardv, new BoardNumberComparer());

      for (Board b: (Board[]) boardv.toArray(new Board[0])) {
         Vector lines = new Vector();
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) {
            String[] ex = h.export();
            String[] line = new String[ex.length-1];
            System.arraycopy(ex, 1, line, 0, line.length);
            lines.add(line);
         }
         tabular.header("Board: "+b.getNumber());
         tabular.print(headers, (String[][]) lines.toArray(new String[0][]));
         tabular.gap();
      }
   }

   public static void localpoint(Map pairs) throws ScoreException
   {
      // sort pairs in order
      List pairv = new ArrayList(pairs.values());
      Collections.sort(pairv, new PairPercentageComparer());
      Pair[] ps = (Pair[]) pairv.toArray(new Pair[0]);

      // check we have enough to give LPs
      if (ps.length < 6) throw new ScoreException("Must have at least 3 full table to award local points");

      // calculate LP scale based on number of pairs
      int[] LPs = new int[ps.length];
      int awarded = (int) Math.ceil(LPs.length/3.0);
      for (int i = 0, lp=6*awarded; lp > 0; i++, lp -= 6)
         LPs[i] = lp;

      // award LPs, splitting on draws
      for (int i = 0; i < ps.length; i++) {
         int a = i;
         int total = LPs[i];
         while (i+1 < ps.length && ps[i].getPercentage()==ps[i+1].getPercentage())
            total += LPs[++i];
         double award = total / (1.0+i-a);
         for (int j = a; j <= i; j++) {
            if (ps[j].getLPs() != 0 && ps[j].getLPs() != award) 
               throw new ScoreException("Calculated "+award+" LPs for pair "+ps[j].getNumber()+", but data says "+ps[j].getLPs());
            ps[j].setLPs(award);
         }
      }
   }

   public static void results(Map pairs, TablePrinter tabulate, String points)
   {
      Vector results = new Vector();
      List pairv = new ArrayList(pairs.values());
      Collections.sort(pairv, new PairPercentageComparer());
      for (Pair p: (Pair[]) pairv.toArray(new Pair[0])) 
         results.add(p.export());

      tabulate.print(new String[] { "Pair#", "Names", "", "MPs", "%age", points }, 
               (String[][]) results.toArray(new String[0][]));
      tabulate.gap();
   }

   public static void main(String[] args)
   {
      try {
         if (Debug.debug) Debug.setThrowableTraces(true);
         Vector commands = new Vector();
         HashMap options = new HashMap();
         options.put("--output", "-");
         options.put("--help", null);
         options.put("--orange", null);
         options.put("--title", "Salliere Duplicate Bridge Scorer: Results");
         int i;
         for (i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) break;
            else if (args[i].startsWith("--")) {
               String[] opt = args[i].split("=");
               if (Debug.debug) Debug.print(Arrays.asList(opt));
               if (options.containsKey(opt[0])) {
                  if (opt.length == 1)
                     options.put(opt[0], "true");
                  else
                     options.put(opt[0], opt[1]);
               } else {
                  System.out.println("Error: unknown option "+opt[0]);
                  syntax();
                  System.exit(1);
               }
            } else
               commands.add(args[i]);
         }

         if (args.length < (i+3)) {
            System.out.println("You must specify boards.csv and names.csv");
            syntax();
            System.exit(1);
         }

         if (null != options.get("--help")) {
            syntax();
            System.exit(1);
         }

         Map/*<String,Board>*/ boards;
         Map/*<String,Pairs>*/ pairs;

         boards = readBoards(new FileInputStream(args[i+1]));
         for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
            b.validate();

         pairs = readPairs(new FileInputStream(args[i+2]));

         TablePrinter tabular = null;

         String[] format = (String[]) ((String) options.get("--output")).split(":");
         PrintStream out;
         if ("-".equals(format[format.length-1]))
            out = System.out;
         else 
            out = new PrintStream(new FileOutputStream(format[format.length-1]));

         if (format.length == 1)
            tabular = new AsciiTablePrinter(out);
         else if ("txt".equals(format[0].toLowerCase()))
            tabular = new AsciiTablePrinter(out);
         else if ("html".equals(format[0].toLowerCase()))
            tabular = new HTMLTablePrinter((String) options.get("--title"), out);
         else if ("pdf".equals(format[0].toLowerCase()))
            tabular = new PDFTablePrinter((String) options.get("--title"), out);
         else {
            System.out.println("Unknown format: "+format[0]);
            syntax();
            System.exit(1);
         }
         
         tabular.init();
         tabular.header((String) options.get("--title"));

         for (String command: (String[]) commands.toArray(new String[0])) {
            if ("score".equals(command)) score(boards);
            else if ("matchpoint".equals(command)) matchpoint(boards);
            else if ("total".equals(command)) total(pairs, boards);
            else if ("results".equals(command)) results(pairs, tabular, null == options.get("--orange") ? "LPs" : "OPs");
            else if ("matrix".equals(command)) matrix(pairs, boards, tabular);
            else if ("boards".equals(command)) boardbyboard(boards, tabular);
            else if ("localpoint".equals(command)) localpoint(pairs);
            else {
               System.out.println("Bad Command: "+command);
               syntax();
               System.exit(1);
            }
         }

         tabular.close();
         out.close();
     
         writeBoards(boards, new FileOutputStream(args[i+1]));
         writePairs(pairs, new FileOutputStream(args[i+2]));
      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
         System.out.println("Salliere failed to compute results: "+e.getMessage());
         System.exit(1);
      }
   }
}
