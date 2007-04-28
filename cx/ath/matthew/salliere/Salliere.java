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

   private static Map/*<String,Board>*/ readBoards(InputStream is) throws IOException
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

   private static Map/*<String,Pair>*/ readPairs(InputStream is) throws IOException
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

   private static void writePairs(Map pairs, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Pair p: (Pair[]) pairs.values().toArray(new Pair[0])) 
         out.writeRecord(p.export());
      out.close();
   }

   private static void syntax()
   {
      System.out.println("Salliere Duplicate Bridge Scorer - version "+System.getProperty("Version"));
      System.out.println("Syntax: salliere [commands] -- <boards.csv> <names.csv>");
      System.out.println("   Commands: score matchpoint total localpoint results matrix boards");
   }


   private static void writeBoards(Map boards, OutputStream os) throws IOException
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

   public static void matrix(Map pairv, Map boardv) 
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

      TablePrinter tabular = new TablePrinter(System.out);
      tabular.print(headers, matrix);
      System.out.println();
   }
   public static void boardbyboard(Map boards) 
   {
      TablePrinter tabular = new TablePrinter(System.out);
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
         System.out.println("Board: "+b.getNumber());
         tabular.print(headers, (String[][]) lines.toArray(new String[0][]));
         System.out.println();
      }

   }

   public static void localpoint(Map pairs) {}

   public static void results(Map pairs)
   {
      Vector results = new Vector();
      List pairv = new ArrayList(pairs.values());
      Collections.sort(pairv, new PairPercentageComparer());
      for (Pair p: (Pair[]) pairv.toArray(new Pair[0])) 
         results.add(p.export());

      TablePrinter tabulate = new TablePrinter(System.out);
      tabulate.print(new String[] { "Pair#", "Names", "", "MPs", "%age", "OPs" }, 
               (String[][]) results.toArray(new String[0][]));
      System.out.println();
   }

   public static void main(String[] args)
   {
      try {
         if (Debug.debug) Debug.setThrowableTraces(true);
         Vector commands = new Vector();
         int i;
         for (i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) break;
            commands.add(args[i]);
         }

         if (args.length < (i+3)) {
            syntax();
            System.exit(1);
         }

         Map/*<String,Board>*/ boards;
         Map/*<String,Pairs>*/ pairs;

         boards = readBoards(new FileInputStream(args[i+1]));
         for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
            b.validate();

         pairs = readPairs(new FileInputStream(args[i+2]));

         for (String command: (String[]) commands.toArray(new String[0])) {
            if ("score".equals(command)) score(boards);
            else if ("matchpoint".equals(command)) matchpoint(boards);
            else if ("total".equals(command)) total(pairs, boards);
            else if ("results".equals(command)) results(pairs);
            else if ("matrix".equals(command)) matrix(pairs, boards);
            else if ("boards".equals(command)) boardbyboard(boards);
            else if ("localpoint".equals(command)) localpoint(pairs);
            else {
               syntax();
               System.exit(1);
            }
         }
     
         writeBoards(boards, new FileOutputStream(args[i+1]));
         writePairs(pairs, new FileOutputStream(args[i+2]));
      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
         System.out.println("Salliere failed to compute results: "+e.getMessage());
         System.exit(1);
      }
   }
}
