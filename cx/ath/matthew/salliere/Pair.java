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

import java.util.Arrays;
import java.util.List;
import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Pair
{
   static int maxnames = 0;
   String number = "";
   String[] names = new String[] { "", "" };
   double mps;
   double percentage;
   double lps;
   public static synchronized int getMaxNames() { return maxnames; }
   public static synchronized void resetNames() { maxnames = 0; }
   public Pair() {}
   public Pair(String[] data)
   {
      int names = 0;
      for (int i = 1; i <= data.length; i++)
         if (i == data.length || (data[i].length() > 0 && 
             (data[i].charAt(0) >= '0' && data[i].charAt(0) <= '9'))) {
            names = i-1;
            break;
         }

      this.number = data[0];
      this.names = new String[names];
      System.arraycopy(data, 1, this.names, 0, this.names.length);
      if (Debug.debug) Debug.print(Debug.DEBUG, "Parsing "+names+" names: "+Arrays.deepToString(this.names));

      if (data.length >= names+4)
         if (0 < data[names+3].length())
            lps = Double.parseDouble(data[names+3]);
      
      if (data.length >= names+3)
         if (0 < data[names+2].length())
            percentage = Double.parseDouble(data[names+2]);
      
      if (data.length >= names+2)
         if (0 < data[names+1].length())
            mps = Double.parseDouble(data[names+1]);

      synchronized (Pair.class) {
         if (maxnames < names) maxnames = names;
      }
   }
   public void total(List boards)
   {
      double top = 0;
      int bds = 0;
      mps = 0;
      for (Board b: (Board[]) boards.toArray(new Board[0])) {
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
   public void setNames(String[] names) { 
      this.names = names; 
      synchronized (Pair.class) {
         if (maxnames < names.length) maxnames = names.length;
      }
   }
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
      int maxnames;
      synchronized (Pair.class) {
         maxnames = Pair.maxnames;
      }
      if (Debug.debug) Debug.print(Debug.DEBUG, "Exporting pair, "+(maxnames+4)+" fields");
      String[] rv = new String[maxnames+4];
      rv[0] = number;
      System.arraycopy(names, 0, rv, 1, names.length);

      for (int i = names.length+1; i <= maxnames; i++)
         rv[i] = "";

      DecimalFormat format = new DecimalFormat("0.#");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      StringBuffer tmp = new StringBuffer();
      rv[maxnames+1] = format.format(mps, tmp, field).toString();

      tmp = new StringBuffer();
      rv[maxnames+3] = format.format(lps, tmp, field).toString();

      format = new DecimalFormat("0.#");
      tmp = new StringBuffer();
      rv[maxnames+2] = format.format(percentage, tmp, field).toString();

      return rv;
   }
}

