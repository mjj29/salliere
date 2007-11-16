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
import java.io.File;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Salliere
{

   static class HandNSComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         String n1 = ((Hand) ob1).getNS();
         String n2 = ((Hand) ob2).getNS();
         return n1.compareTo(n2);
      }
   }

   static class BoardNumberComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         String n1 = ((Board) ob1).getNumber();
         String n2 = ((Board) ob2).getNumber();
         String[] n = n1.split(":");
         String[] v = n[n.length-1].split(";");
         int num1 = Integer.parseInt(v[0]);
         n = n2.split(":");
         v = n[n.length-1].split(";");
         int num2 = Integer.parseInt(v[0]);

         return num1-num2;
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

   static List readBoards(InputStream is) throws IOException, BoardValidationException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      Map boards = new HashMap();
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
      return new ArrayList(boards.values());
   }

   static List readPairs(InputStream is, boolean individual) throws IOException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      List pairs = new Vector();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) Debug.print(Arrays.asList(values));
            if (individual)
               pairs.add(new Individual(values));
            else
               pairs.add(new Pair(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return pairs;
   }

   static void writePairs(List pairs, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
         out.writeRecord(p.export());
      out.close();
   }

   static void syntax()
   {
      String version = Package.getPackage("cx.ath.matthew.salliere")
                              .getImplementationVersion();
      System.out.println("Salliere Duplicate Bridge Scorer - version "+version);
      System.out.println("Syntax: salliere [options] [commands] -- <boards.csv> <names.csv>");
      System.out.println("   Commands: verify score matchpoint ximp total localpoint results matrix boards");
      System.out.println("   Options: --help --output=[<format>:]file --title=title --orange --setsize=N --ximp --individual");
      System.out.println("   Formats: txt html pdf");
   }


   static void writeBoards(List boards, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            out.writeRecord(h.export());
      out.close();
   }

   public static void score(List boards) throws ScoreException, ContractParseException, HandParseException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            if (!h.isAveraged())
               h.score();
      modifiedboards = true;
   }

   public static void verify(List boardv, String setsize) throws MovementVerificationException, BoardValidationException, HandParseException
   {
      for (Board b: (Board[]) boardv.toArray(new Board[0])) 
         b.validate();

      if (null == setsize || setsize.length() == 0) return;

      int size = 0;
      try {
         size = Integer.parseInt(setsize);
      } catch (NumberFormatException NFe) {
         if (Debug.debug) Debug.print(NFe);
         throw new MovementVerificationException(setsize+" isn't a number!");
      }
      
      Collections.sort(boardv, new BoardNumberComparer());
      Board[] boards = (Board[]) boardv.toArray(new Board[0]);
      Set pairs = new TreeSet();
      for (int i = 0; i < boards.length; i++) {
         if (0 == (i % size)) {
            // reset the pairs
            pairs.clear();
            for (Hand h: (Hand[]) boards[i].getHands().toArray(new Hand[0])) 
               pairs.add(h.getNS()+" "+h.getEW());
         } else {
            // check the pairs
            for (Hand h: (Hand[]) boards[i].getHands().toArray(new Hand[0])) 
               if (!pairs.contains(h.getNS()+" "+h.getEW()))
                  throw new MovementVerificationException("Board "+boards[i].getNumber()+" was played by "+h.getNS()+" and "+h.getEW()+" which I was not expecting.");
         }
      }
   }

   public static void matchpoint(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.matchPoint();
      modifiedboards = true;
   }

   public static void ximp(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.ximp();
      modifiedboards = true;
   }

   public static void total(List pairs, List boards)
   {
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
         p.total(boards);
      modifiedpairs = true;
   }

   public static void matrix(List pairv, List boardv, TablePrinter tabular) 
   {
      Collections.sort(boardv, new BoardNumberComparer());
      Collections.sort(pairv, new PairNumberComparer());
      Board[] boards = (Board[]) boardv.toArray(new Board[0]);
      Pair[] pairs = (Pair[]) pairv.toArray(new Pair[0]);

      DecimalFormat format = new DecimalFormat("0.##");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
      
      String[][] matrix = new String[boards.length][pairs.length+1]; 
      for (int i = 0; i < boards.length; i++) {
         matrix[i][0] = boards[i].getNumber();
         for (int j = 0; j < pairs.length; j++) {
            StringBuffer tmp = new StringBuffer();
            if (boards[i].played(pairs[j].getNumber()))
               matrix[i][j+1] = format.format(boards[i].getMPs(pairs[j].getNumber()), tmp, field).toString();
            else
               matrix[i][j+1] = " ";
         }
      }

      String[] headers = new String[pairs.length+1];
      headers[0] = "Board";
      for (int j = 0; j < pairs.length; j++) 
         headers[j+1] = pairs[j].getNumber()+"  ";

      tabular.print(headers, matrix);
      tabular.gap();
   }
   public static void boardbyboard(List boards, TablePrinter tabular, boolean ximp) 
   {
      String[] headers = new String[] { "NS", "EW", "Contract", "By", "Tricks", "Score:", "", ximp ? "IMPs" : "MPs:", "" };
      Collections.sort(boards, new BoardNumberComparer());

      for (Board b: (Board[]) boards.toArray(new Board[0])) {
         Vector lines = new Vector();
         List hands = b.getHands();
         Collections.sort(hands, new HandNSComparer());
         for (Hand h: (Hand[]) hands.toArray(new Hand[0])) {
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

   public static void localpoint(List pairs, boolean individual) throws ScoreException
   {
      int rate = individual ? 3 : 6;
      // sort pairs in order
      Collections.sort(pairs, new PairPercentageComparer());
      Pair[] ps = (Pair[]) pairs.toArray(new Pair[0]);

      // check we have enough to give LPs
      if (ps.length < 6) throw new ScoreException("Must have at least 3 full table to award local points");

      // calculate LP scale based on number of pairs
      int[] LPs = new int[ps.length];
      int awarded = (int) Math.ceil(LPs.length/3.0);
      for (int i = 0, lp=rate*awarded; lp > 0; i++, lp -= rate)
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
      modifiedpairs = true;
   }

   public static void results(List pairs, TablePrinter tabulate, boolean orange, boolean ximp)
   {
      Vector results = new Vector();
      Collections.sort(pairs, new PairPercentageComparer());

      String points;
      if (orange) points = "OPs";
      else points = "LPs";

      if (ximp) {
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
            String[] a = p.export();
            String[] b = new String[a.length-1];
            System.arraycopy(a, 0, b, 0, 4);
            System.arraycopy(a, 5, b, 4, a.length-5);
            results.add(b);
         }
         tabulate.print(new String[] { "Pair", "Names", "", "IMPs", points }, 
               (String[][]) results.toArray(new String[0][]));
      } else {
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
            results.add(p.export());
         tabulate.print(new String[] { "Pair", "Names", "", "MPs", "%age", points }, 
               (String[][]) results.toArray(new String[0][]));
      }

      tabulate.gap();
   }

   public static boolean modifiedpairs = false;
   public static boolean modifiedboards = false;

   public static void main(String[] args)
   {
      try {
         if (Debug.debug) {
            File f = new File("debug.conf");
            if (f.exists())
               Debug.loadConfig(f);
            Debug.setThrowableTraces(true);
         }
         Vector commands = new Vector();
         HashMap options = new HashMap();
         options.put("--output", "-");
         options.put("--help", null);
         options.put("--orange", null);
         options.put("--ximp", null);
         options.put("--title", "Salliere Duplicate Bridge Scorer: Results");
         options.put("--setsize", null);
         options.put("--individual", null);
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

         List boards;
         List pairs;

         boards = readBoards(new FileInputStream(args[i+1]));
         pairs = readPairs(new FileInputStream(args[i+2]), null != options.get("--individual"));

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
            else if ("verify".equals(command)) verify(boards, (String) options.get("--setsize"));
            else if ("matchpoint".equals(command)) matchpoint(boards);
            else if ("total".equals(command)) total(pairs, boards);
            else if ("results".equals(command)) results(pairs, tabular, null != options.get("--orange"), null != options.get("--ximp"));
            else if ("matrix".equals(command)) matrix(pairs, boards, tabular);
            else if ("boards".equals(command)) boardbyboard(boards, tabular, null != options.get("--ximp"));
            else if ("localpoint".equals(command)) localpoint(pairs, null != options.get("--individual"));
            else if ("ximp".equals(command)) ximp(boards);
            else {
               System.out.println("Bad Command: "+command);
               syntax();
               System.exit(1);
            }
         }

         tabular.close();
         out.close();
     
         if (modifiedboards) 
            writeBoards(boards, new FileOutputStream(args[i+1]));
         if (modifiedpairs) 
            writePairs(pairs, new FileOutputStream(args[i+2]));
      } catch (Exception e) {
         if (Debug.debug) {
            Debug.setThrowableTraces(true);
            Debug.print(e);
         }
         System.out.println("Salliere failed to compute results: "+e.getMessage());
         System.exit(1);
      }
   }
}
