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

import java.util.Map;

public class Pair
{
   String number;
   String[] names;
   double mps;
   double percentage;
   double lps;
   public Pair(String[] data)
   {
      this.number = data[0];
      this.names = new String[2];
      System.arraycopy(data, 1, names, 0, names.length);
      switch (data.length) {
         case 6:
            if (0 > data[5].length())
               mps = Double.parseDouble(data[5]);
         case 5:
            if (0 > data[4].length())
               mps = Double.parseDouble(data[4]);
         case 4:
            if (0 > data[3].length())
               mps = Double.parseDouble(data[3]);
      }
   }
   public void total(Map/*<String,Board>*/ boards)
   {
      double top = 0;
      int bds = 0;
      mps = 0;
      for (Board b: (Board[]) boards.values().toArray(new Board[0])) {
         top = b.getTop();
         mps += b.getMPs(number);
         bds++;
      }
      percentage = (mps*100) / (top*bds);
   }
   public String getNumber() { return number; }
   public String[] getNames() { return names; }
   public double getMPs() { return mps; }
   public double getLPs() { return lps; }
   public double getPercentage() { return percentage; }
   public String toString() 
   { 
      StringBuilder sb = new StringBuilder();
      sb.append(number);
      sb.append(": ");
      for (String s: names) {
         sb.append(s);
         sb.append(' ');
      }
      return sb.toString();
   }
   public String[] export()
   {
      String[] rv = new String[names.length+1];
      rv[0] = number;
      System.arraycopy(names, 0, rv, 1, names.length);
      return rv;
   }
}

