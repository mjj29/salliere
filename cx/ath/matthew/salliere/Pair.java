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

import java.util.Map;
import java.text.DecimalFormat;
import java.text.FieldPosition;

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
            if (0 < data[5].length())
               lps = Double.parseDouble(data[5]);
         case 5:
            if (0 < data[4].length())
               percentage = Double.parseDouble(data[4]);
         case 4:
            if (0 < data[3].length())
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
         if (b.played(number)) {
            mps += b.getMPs(number);
            bds++;
         }
      }
      percentage = (mps*100) / (top*bds);
      if (Debug.debug) Debug.print("totalling for pair "+number+": mps="+mps+", boards="+bds+", top="+top+", %age="+percentage);
   }
   public String getNumber() { return number; }
   public String[] getNames() { return names; }
   public double getMPs() { return mps; }
   public double getLPs() { return lps; }
   public double getPercentage() { return percentage; }
   public void setLPs(double lps) { this.lps = lps; }
   public void setMPs(double mps) { this.mps = mps; }
   public void setPercentage(double percentage) { this.percentage = percentage; }
   public void setNumber(String number) { this.number = number; }
   public void setNames(String[] names) { this.names = names; }
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
      String[] rv = new String[names.length+4];
      rv[0] = number;
      System.arraycopy(names, 0, rv, 1, names.length);

      DecimalFormat format = new DecimalFormat("0.#");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      StringBuffer tmp = new StringBuffer();
      rv[names.length+1] = format.format(mps, tmp, field).toString();

      tmp = new StringBuffer();
      rv[names.length+3] = format.format(lps, tmp, field).toString();

      format = new DecimalFormat("0.#");
      tmp = new StringBuffer();
      rv[names.length+2] = format.format(percentage, tmp, field).toString();

      return rv;
   }
}

