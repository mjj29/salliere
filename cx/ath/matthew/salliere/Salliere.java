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

import java.io.BufferedReader;
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

   static void readTrickData(List boards, InputStream trickdatafile) throws IOException
   {
      Collections.sort(boards, new BoardNumberComparer());

      BufferedReader br = new BufferedReader(new InputStreamReader(trickdatafile));

      for (Board b: (Board[]) boards.toArray(new Board[0])) {
         String s;
         if (null != (s = br.readLine()))
            b.importTricks(s);
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
      System.out.println("   Commands: verify score matchpoint ximp parimp total localpoint results matrix boards");
      System.out.println("   Options: --help --output=[<format>:]file --title=title --orange --setsize=N --ximp --individual --with-par --trickdata=<tricks.txt>");
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

   public static void parimp(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.parimp();
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
   public static void boardbyboard(List boards, TablePrinter tabular, boolean ximp, boolean withpar) 
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
         if (withpar)
            tabular.header("Board: "+b.getNumber()+" [ "+b.getParContract()+" by "+b.getPar().getDeclarer()+" ]");
         else
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
      if (ps.length < (individual?8:6)) throw new ScoreException("Must have at least 3 full tables at pairs or 2 full tables at individuals to award local points");

      // calculate LP scale based on number of pairs
      int[] LPs = new int[ps.length];
      int awarded = (int) Math.ceil(LPs.length/3.0);
      int top;
      if (individual) {
         int tables = (int) Math.floor(ps.length/4.0);
         top = (3 * (int) Math.ceil(tables/3.0)) + (3 * tables);
      } else
         top = rate*awarded;
      if (top > 100) top = 100;
      if (Debug.debug) Debug.print(Debug.DEBUG, "Awarding LPs to "+ps.length+" pairs, top is "+top+" rate is "+rate+" number receiving LPs: "+awarded);
      for (int i = 0, lp=top; i < awarded; i++, lp -= rate)
         LPs[i] = lp > 6 ? lp : 6;

      // award LPs, splitting on draws
      for (int i = 0; i < ps.length; i++) {
         int a = i;
         int total = LPs[i];
         while (i+1 < ps.length && ps[i].getPercentage()==ps[i+1].getPercentage())
            total += LPs[++i];
         double award = Math.ceil(total / (1.0+i-a));
         if (award < 6 && award > 0) award = 6;
         for (int j = a; j <= i; j++) {
            if (ps[j].getLPs() != 0 && ps[j].getLPs() != award) 
               throw new ScoreException("Calculated "+award+" LPs for pair "+ps[j].getNumber()+", but data says "+ps[j].getLPs());
            ps[j].setLPs(award);
         }
      }
      modifiedpairs = true;
   }

   public static void results(List pairs, TablePrinter tabulate, boolean orange, boolean ximp, boolean individual)
   {
      Vector results = new Vector();
      Collections.sort(pairs, new PairPercentageComparer());

      String points;
      if (orange) points = "OPs";
      else points = "LPs";

      String[] header;
      if (ximp && individual)
         header = new String[] { "Num", "Name", "IMPs", points };
      else if (ximp && !individual)
         header = new String[] { "Pair", "Names", "", "IMPs", points };
      else if (!ximp && individual)
         header = new String[] { "Num", "Name", "MPs", "%age", points };
      else
         header = new String[] { "Pair", "Names", "", "MPs", "%age", points };

      if (ximp)
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
            String[] a = p.export();
            String[] b = new String[a.length-1];
            System.arraycopy(a, 0, b, 0, 4);
            System.arraycopy(a, 5, b, 4, a.length-5);
            results.add(b);
         }
      else
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
            results.add(p.export());
      tabulate.print(header, 
            (String[][]) results.toArray(new String[0][]));
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
         options.put("--with-par", null);
         options.put("--trickdata", null);
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

         if (null != options.get("--trickdata")) {
            readTrickData(boards, new FileInputStream((String) options.get("--trickdata")));
         }

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
            else if ("results".equals(command)) results(pairs, tabular, null != options.get("--orange"), null != options.get("--ximp"), null != options.get("--individual"));
            else if ("matrix".equals(command)) matrix(pairs, boards, tabular);
            else if ("boards".equals(command)) boardbyboard(boards, tabular, null != options.get("--ximp"), null != options.get("--with-par"));
            else if ("localpoint".equals(command)) localpoint(pairs, null != options.get("--individual"));
            else if ("ximp".equals(command)) ximp(boards);
            else if ("parimp".equals(command)) parimp(boards);
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
