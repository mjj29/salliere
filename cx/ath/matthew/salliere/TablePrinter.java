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

import java.io.PrintStream;
import java.util.Arrays;

public class TablePrinter
{
   private PrintStream output;
   public TablePrinter(PrintStream output)
   {
      this.output = output;
   }
   public void print(String[] headers, String[][] rows)
   {
      // calculate column width
      int[] widths = new int[headers.length];
      for (int i = 0; i < headers.length; i++)
         widths[i] = headers[i].length();
      for (int i = 0; i < rows.length; i++)
         for (int j = 0; j < rows[i].length; j++)
            if (widths[j] < rows[i][j].length())
               widths[j] = rows[i][j].length();

      // gap
      for (int i = 0; i < widths.length; i++) 
         widths[i]++;

      // print headers
      for (int i = 0; i < headers.length; i++) {
         output.print(headers[i]);
         for (int j = headers[i].length(); j < widths[i]; j++)
            output.print(' ');
      }
      output.println();

      // print line
      for (int i = 0; i < widths.length; i++) 
         for (int j = 0; j < widths[i]; j++)
            output.print('-');
      output.println();

      // print rows
      for (int i = 0; i < rows.length; i++) {
         for (int j = 0; j < rows[i].length; j++) {
            output.print(rows[i][j]);
            for (int k = rows[i][j].length(); k < widths[j]; k++)
               output.print(' ');
         }
         output.println();
      }

      output.flush();
   }
}
