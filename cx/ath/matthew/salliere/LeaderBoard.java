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
import static cx.ath.matthew.salliere.Gettext._;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.util.Arrays;
import java.util.HashMap;

public class LeaderBoard
{
   class LeaderEntry implements Comparable
   {
      String name;
      double LPs;
      public LeaderEntry(String name, double LPs)
      {
         this.name = name;
         this.LPs = LPs;
      }
      public void update(double LPs)
      {
         this.LPs += LPs;
      }
      public int compareTo(Object o)
      {
         return (int) (((LeaderEntry) o).LPs - LPs);
      }
   }
   HashMap results = new HashMap();
   public void update(String name, double LPs)
   {
      if (LPs == 0) return; 

      LeaderEntry d = (LeaderEntry) results.get(name);
      if (null == d) {
         d = new LeaderEntry(name, LPs);
         results.put(name, d);
      } else
         d.update(LPs);
   }
   public String[][] getLeaderBoard()
   {
      if (Debug.debug) Debug.print("Serializing leaderboard: "+results);
      String[][] results = new String[this.results.size()][2];
      LeaderEntry[] es = (LeaderEntry[]) this.results.values().toArray(new LeaderEntry[0]);
      Arrays.sort(es);
      int i = 0;
      for (LeaderEntry e: es) {
         results[i][0] = e.name;
         StringBuffer sb = new StringBuffer();
         DecimalFormat format = new DecimalFormat("0.#");
         FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
         results[i][1] = format.format(e.LPs, sb, field).toString();
         i++;
      }
      return results;
   }
}
