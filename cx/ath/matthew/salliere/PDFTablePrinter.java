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

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;

import cx.ath.matthew.debug.Debug;

import java.awt.Color;
import java.io.OutputStream;
import java.util.Arrays;

public class PDFTablePrinter implements TablePrinter
{
   private Document document;
   private OutputStream output;
   private String title;
   public PDFTablePrinter(String title, OutputStream output)
   {
      this.document = new Document();
      this.output = output;
      this.title = title;
   }
   public void print(String[] headers, String[][] rows)
   {
      try {
         Table table = new Table(headers.length);
         table.setBorderWidth(1);
         table.setBorderColor(new Color(0, 0, 0));
         table.setSpaceInsideCell(2);
         table.setDefaultVerticalAlignment(Table.ALIGN_MIDDLE);
         table.setDefaultCellBorder(Table.NO_BORDER);

         // print headers
         for (int i = 0; i < headers.length; i++) {
            Cell cell = new Cell(headers[i]);
            cell.setHeader(true);
            cell.setBorder(Table.BOTTOM);
            table.addCell(cell);
            table.endHeaders();
         }

         // print rows
         for (int i = 0; i < rows.length; i++) 
            for (int j = 0; j < rows[i].length; j++) 
               table.addCell(rows[i][j]);
         
         document.add(table);
      } catch (DocumentException De) {
         if (Debug.debug) Debug.print(De);
      }
   }
   public void header(String header)
   {
      try {
         document.add(new Paragraph(header, new Font(Font.HELVETICA, 16, Font.BOLD)));
      } catch (DocumentException De) {
         if (Debug.debug) Debug.print(De);
      }
   }
   public void gap()
   {
      try {
         document.add(new Paragraph(""));
      } catch (DocumentException De) {
         if (Debug.debug) Debug.print(De);
      }
   }
   public void init() 
   {
      try {
         PdfWriter.getInstance(document, output);
         document.addTitle(title);
         document.open();
      } catch (DocumentException De) {
         if (Debug.debug) Debug.print(De);
      }
   }
   public void close() 
   {
      document.close();
   }
}
