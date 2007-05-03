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
import com.csvreader.CsvWriter;

import cx.ath.matthew.debug.Debug;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class GSalliere extends Salliere
{

   static class GSalliereMainFrame extends JFrame
   {
      class MenuActionListener implements ActionListener
      {
         public void actionPerformed(ActionEvent e)
         {
             String command = e.getActionCommand();
             if ("loadboards".equals(command)) {

                JFileChooser fc;
                if (null == boardfile)
                   fc = new JFileChooser();
                else
                   fc = new JFileChooser(new File(boardfile).getParent());

                int rv = fc.showOpenDialog(GSalliereMainFrame.this);
                if (rv == JFileChooser.APPROVE_OPTION) {
                   File f = fc.getSelectedFile();
                   try {
                      boardfile = f.getCanonicalPath();
                      boards = readBoards(new FileInputStream(f));
                      for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
                         b.validate();
                   } catch (BoardValidationException BVe) {
                      if (Debug.debug) Debug.print(BVe);
                      showerror("Problem loading boards file: "+BVe);
                      boards = null;
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror("Problem loading boards file: "+IOe);
                      boards = null;
                   }
                }

             } else if ("loadpairs".equals(command)) {

                JFileChooser fc;
                if (null == namesfile)
                   fc = new JFileChooser();
                else
                   fc = new JFileChooser(new File(namesfile).getParent());

                int rv = fc.showOpenDialog(GSalliereMainFrame.this);
                if (rv == JFileChooser.APPROVE_OPTION) {
                   File f = fc.getSelectedFile();

                   try {
                      namesfile = f.getCanonicalPath();
                      pairs = readPairs(new FileInputStream(f));
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror("Problem loading names file: "+IOe);
                      pairs = null;
                   }
                }

             } else if ("saveboards".equals(command)) {
                if (null == boards) showerror("Must Load Boards before saving them");
                else {

                   JFileChooser fc;
                   if (null == boardfile)
                      fc = new JFileChooser();
                   else
                      fc = new JFileChooser(new File(boardfile).getParent());

                   int rv = fc.showSaveDialog(GSalliereMainFrame.this);
                   if (rv == JFileChooser.APPROVE_OPTION) {
                      File f = fc.getSelectedFile();
                      try {
                         boardfile = f.getCanonicalPath();
                         writeBoards(boards, new FileOutputStream(f));
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror("Problem saving boards file: "+IOe);
                      }
                   }

                }
             } else if ("savepairs".equals(command)) {
                if (null == pairs) showerror("Must Load Pairs before saving them");
                else {

                   JFileChooser fc;
                   if (null == namesfile)
                      fc = new JFileChooser();
                   else
                      fc = new JFileChooser(new File(namesfile).getParent());

                   int rv = fc.showSaveDialog(GSalliereMainFrame.this);
                   if (rv == JFileChooser.APPROVE_OPTION) {
                      File f = fc.getSelectedFile();
                      try {
                         namesfile = f.getCanonicalPath();
                         writePairs(pairs, new FileOutputStream(f));
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror("Problem saving names file: "+IOe);
                      }
                   }
                }
             } else if ("export".equals(command)) {
                if (null == pairs || null == boards) showerror("Must Load Pairs and boards before exporting");
                else 
                  export();
             }
         }
      }
      class ButtonActionListener implements ActionListener
      {
         public void actionPerformed(ActionEvent e)
         {
             String command = e.getActionCommand();
             try {
                if ("score".equals(command)) {
                   if (null == boards) showerror("Must Load Boards before Scoring");
                   else score(boards);
                } else if ("matchpoint".equals(command)) {
                   if (null == boards) showerror("Must Load Boards before Matchpointing");
                   else matchpoint(boards);
                } else if ("total".equals(command)) {
                   if (null == boards || null == pairs) showerror("Must Load Boards and Pairs before Totalling");
                   else total(pairs, boards);
                } else if ("localpoint".equals(command)) {
                   if (null == pairs) showerror("Must Load Pairs before Scoring");
                   else localpoint(pairs);
                } else if ("results".equals(command)) {
                   if (null == boards || null == pairs) showerror("Must Load Boards and Pairs before exporting results");
                   else export();
                }
             } catch (ScoreException Se) {
               if (Debug.debug) Debug.print(Se);
               showerror("Problem while performing action: "+Se);
             } catch (ContractParseException CPe) {
               if (Debug.debug) Debug.print(CPe);
               showerror("Problem while performing action: "+CPe);
             }
         }
      }
      private JPanel panel;
      public GSalliereMainFrame()
      {
         super("GSalliere - Duplicate Bridge Scoring");
         setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);

         /* MENU */
         JMenuBar menuBar = new JMenuBar();
         menuBar.setVisible(true);
         setJMenuBar(menuBar);

         // file menu
         JMenu file = new JMenu("File");
         file.setMnemonic(KeyEvent.VK_F);
         file.getAccessibleContext().setAccessibleDescription(
                       "Has options to load and save score files and export results");
         menuBar.add(file);

         JMenuItem item;
         ActionListener mal = new MenuActionListener();

         // load score file
         item = new JMenuItem("Load Score File", KeyEvent.VK_L);
         item.setActionCommand("loadscores");
         item.addActionListener(mal);
         file.add(item);

         // load names file
         item = new JMenuItem("Load Names File", KeyEvent.VK_N);
         item.setActionCommand("loadnames");
         item.addActionListener(mal);
         file.add(item);

         // save score file
         item = new JMenuItem("Save Score File", KeyEvent.VK_S);
         item.setActionCommand("savescores");
         item.addActionListener(mal);
         file.add(item);

         // save names file
         item = new JMenuItem("Save Names File", KeyEvent.VK_A);
         item.setActionCommand("savenames");
         item.addActionListener(mal);
         file.add(item);

         // export results
         item = new JMenuItem("Export Results", KeyEvent.VK_E);
         item.setActionCommand("export");
         item.addActionListener(mal);
         file.add(item);

         /* MAIN BODY */
         panel = new JPanel(new BorderLayout());
         panel.setVisible(true);
         setContentPane(panel);


         /* BUTTON BAR */
         JPanel buttonbar = new JPanel(new FlowLayout());
         buttonbar.setVisible(true);
         add(buttonbar, BorderLayout.NORTH);
         
         JButton button;
         ActionListener bal = new ButtonActionListener();

         // score
         button = new JButton("Score");
         button.setToolTipText("Score the contracts");
         button.setActionCommand("score");
         button.setMnemonic(KeyEvent.VK_S);
         button.addActionListener(bal);
         buttonbar.add(button);

         // matchpoint
         button = new JButton("Match Point");
         button.setToolTipText("Matchpoint the boards");
         button.setActionCommand("matchpoint");
         button.setMnemonic(KeyEvent.VK_M);
         button.addActionListener(bal);
         buttonbar.add(button);

         // total
         button = new JButton("Total");
         button.setToolTipText("Total the match points for the pairs");
         button.setActionCommand("total");
         button.setMnemonic(KeyEvent.VK_T);
         button.addActionListener(bal);
         buttonbar.add(button);

         // localpoint
         button = new JButton("Local Point");
         button.setToolTipText("Calculate local points for the pairs");
         button.setActionCommand("localpoint");
         button.setMnemonic(KeyEvent.VK_M);
         button.addActionListener(bal);
         buttonbar.add(button);

         // results
         button = new JButton("Results");
         button.setToolTipText("Export the Results");
         button.setActionCommand("results");
         button.setMnemonic(KeyEvent.VK_R);
         button.addActionListener(bal);
         buttonbar.add(button);

         pack();         
      }
      public void showerror(String text)
      {
         JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
      }
   }
   public static void export()
   {
      /* if (format.length == 1)
         tabular = new AsciiTablePrinter(out);
         else if ("txt".equals(format[0].toLowerCase()))
         tabular = new AsciiTablePrinter(out);
         else if ("html".equals(format[0].toLowerCase()))
         tabular = new HTMLTablePrinter((String) options.get("--title"), out);
         else if ("pdf".equals(format[0].toLowerCase()))
         tabular = new PDFTablePrinter((String) options.get("--title"), out);
         else {
         System.out.println("Unknown format: "+format[0]);
         syntax();
         System.exit(1);
         }

         tabular.init();
         tabular.header((String) options.get("--title"));

         for (String command: (String[]) commands.toArray(new String[0])) {
         if ("score".equals(command)) score(boards);
         else if ("matchpoint".equals(command)) matchpoint(boards);
         else if ("total".equals(command)) total(pairs, boards);
         else if ("results".equals(command)) results(pairs, tabular, null == options.get("--orange") ? "LPs" : "OPs");
         else if ("matrix".equals(command)) matrix(pairs, boards, tabular);
         else if ("boards".equals(command)) boardbyboard(boards, tabular);
         else if ("localpoint".equals(command)) localpoint(pairs);
         else {
         System.out.println("Bad Command: "+command);
         syntax();
         System.exit(1);
         }
         }

         tabular.close();
         out.close();
         */

   }

   private static Map boards;
   private static String boardfile;
   private static String namesfile;
   private static String outputfile;
   private static Map pairs;

   public static void main(String[] args)
   {
      try {
         GSalliereMainFrame main = new GSalliereMainFrame();
         main.setVisible(true);
         if (Debug.debug) Debug.setThrowableTraces(true);

         if (args.length == 2) {
            boardfile = args[0];
            namesfile = args[1];

            try {
               boards = readBoards(new FileInputStream(boardfile));
               for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
                  b.validate();
            } catch (BoardValidationException BVe) {
               if (Debug.debug) Debug.print(BVe);
               main.showerror("Problem loading boards file: "+BVe);
               boards = null;
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               main.showerror("Problem loading boards file: "+IOe);
               boards = null;
            }

            try {
               pairs = readPairs(new FileInputStream(namesfile));
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               main.showerror("Problem loading names file: "+IOe);
               pairs = null;
            }

         }

      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
         System.out.println("There was a problem during the execution of GSalliere: "+e.getMessage());
         System.exit(1);
      }
   }
}
