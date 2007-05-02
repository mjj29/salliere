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

public class HTMLTablePrinter implements TablePrinter
{
   private PrintStream output;
   private String title;
   public HTMLTablePrinter(String title, PrintStream output)
   {
      this.output = output;
      this.title = title;
   }
   public void print(String[] headers, String[][] rows)
   {
      output.println("<table>");

      // print headers
      output.println("<thead>");
      output.println("<tr>");
      for (int i = 0; i < headers.length; i++) {
         output.print("<th>"+headers[i]+"</th>");
      }
      output.println("</tr>");
      output.println("</thead>");


      // print rows
      output.println("<tbody>");
      for (int i = 0; i < rows.length; i++) {
         output.println("<tr>");
         for (int j = 0; j < rows[i].length; j++) {
            output.print("<td>"+rows[i][j]+"</td>");
         }
         output.println("</tr>");
      }
      output.println("</tbody>");

      output.println("</table>");
      output.flush();
   }
   public void header(String header)
   {
      output.println("<h4>"+header+"</h4>");
   }
   public void gap()
   {
      output.println("<br>");
   }
   public void init() 
   {
      output.println("<html>");
      output.println("<head>");
      output.println("<title>"+title+"</title>");
      output.println("</head>");
      output.println("<body>");
   }
   public void close() 
   {
      output.println("</body>");
      output.println("</html>");
   }
}
