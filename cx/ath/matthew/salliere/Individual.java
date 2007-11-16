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

import java.util.List;
import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Individual extends Pair
{
   String name = "";
   public Individual() {}
   public Individual(String[] data)
   {
      this.number = data[0];
      this.name = data[1];
      switch (data.length) {
         case 5:
            if (0 < data[4].length())
               lps = Double.parseDouble(data[4]);
         case 4:
            if (0 < data[3].length())
               percentage = Double.parseDouble(data[3]);
         case 3:
            if (0 < data[2].length())
               mps = Double.parseDouble(data[2]);
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
      if (Debug.debug) Debug.print("totalling for player "+number+": mps="+mps+", boards="+bds+", top="+top+", %age="+percentage);
   }
   public String[] getNames() { return new String[] { name, "" }; }
   public void setNames(String[] names) { this.name = names[0]; }
   public String getName() { return name; }
   public void setName(String name) { this.name = name; }
   public String toString() 
   { 
      StringBuilder sb = new StringBuilder();
      sb.append(number);
      sb.append(name);
      return sb.toString();
   }
   public String[] export()
   {
      String[] rv = new String[5];
      rv[0] = number;
      rv[1] = name;

      DecimalFormat format = new DecimalFormat("0.#");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      StringBuffer tmp = new StringBuffer();
      rv[2] = format.format(mps, tmp, field).toString();

      tmp = new StringBuffer();
      rv[4] = format.format(lps, tmp, field).toString();

      format = new DecimalFormat("0.#");
      tmp = new StringBuffer();
      rv[3] = format.format(percentage, tmp, field).toString();

      return rv;
   }
}

