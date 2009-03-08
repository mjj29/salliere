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

public class CSVTablePrinter implements TablePrinter
{
   private PrintStream output;
   public CSVTablePrinter(PrintStream output)
   {
      this.output = output;
   }
   public void print(String[] headers, String[][] rows)
   {
      for (int i = 0; i < headers.length; i++) {
         output.print(headers[i]);
         output.print(',');
      }
      output.println();

      for (int i = 0; i < rows.length; i++) {
         for (int j = 0; j < rows[i].length; j++) {
            output.print(rows[i][j]);
            output.print(',');
         }
         output.println();
      }

      output.flush();
   }
   public void header(String header)
   {
   }
   public void gap()
   {
   }
   public void init() {}
   public void close() {}
}
