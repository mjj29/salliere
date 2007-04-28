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

import cx.ath.matthew.debug.Debug;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Salliere
{

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

   private static Map/*<String,Board>*/ boards;
   private static Map/*<String,Pairs>*/ pairs;
   public static void main(String[] args) throws Exception
   {
      if (args.length < 2) {
         System.out.println("Syntax: salliere <boards.csv> <names.csv>");
         System.exit(1);
      }

      boards = readBoards(new FileInputStream(args[0]));
      if (Debug.debug)
         for (Board b: (Board[]) boards.values().toArray(new Board[0])) {
            for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) {
               h.score();
            }
            b.matchPoint();
            Debug.print(b);
         }

      pairs = readPairs(new FileInputStream(args[1]));

      List pairv = new ArrayList(pairs.values());
      for (Pair p: (Pair[]) pairv.toArray(new Pair[0])) 
         p.total(boards);

      Collections.sort(pairv, new PairPercentageComparer());
      for (Pair p: (Pair[]) pairv.toArray(new Pair[0])) 
         System.out.println(p+" "+p.getMPs());
   }
}
