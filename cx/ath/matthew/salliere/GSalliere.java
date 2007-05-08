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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;

public class GSalliere extends Salliere
{
   static class GSalliereMainFrame extends JFrame
   {
      class BoardEditDialog extends JDialog implements ActionListener
      {
         private Board board;
         public BoardEditDialog(Board b)
         {
            super(GSalliereMainFrame.this, "Edit Board "+b.getNumber(), true);
            this.board = b;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setContentPane(new JPanel(new BorderLayout()));

            JTable data = new JTable(new HandTableDataModel(b.getHands()));
            setSize(600, data.getRowHeight()*(b.getHands().size()+8));
            add(new JScrollPane(data), BorderLayout.CENTER);

            JButton button = new JButton("OK");
            button.addActionListener(this);
            add(button, BorderLayout.SOUTH);

            doLayout();
         }
         public void actionPerformed(ActionEvent e)
         {
            dispose();
         }
      }
      class BoardTableDataModel implements TableModel
      {
         private Board[] boards;
         public BoardTableDataModel(Map boardv)
         {
            if (null == boardv) return;
            List sortedboards = new ArrayList(boardv.values());
            Collections.sort(sortedboards, new BoardNumberComparer());
            this.boards = (Board[]) sortedboards.toArray(new Board[0]);
         }
         public int getRowCount()
         { return null == boards ? 1 : boards.length+1; }
         public int getColumnCount()
         { return  3; }
         public String getColumnName(int columnIndex)
         {
            switch (columnIndex) {
               case 0: return "Number";
               case 1: return "Played";
               case 2: return "Pairs";
               default: return "ERR0R";
            }
         }
         public Class getColumnClass(int columnIndex)
         {
            switch (columnIndex) {
               case 0: 
               case 2: return String.class;
               case 1: return Integer.class; 
               default: return null;
            }
         }
         public boolean isCellEditable(int rowIndex, int columnIndex)
         { 
            if (null == boards) return false;
            else return columnIndex==0 && rowIndex < boards.length; 
         }
         public Object getValueAt(int rowIndex, int columnIndex)
         {
            if (null == boards) return "";
            else if (rowIndex >= boards.length) return "";
            else 
               switch (columnIndex) {
                  case 0: return boards[rowIndex].getNumber();
                  case 1: return boards[rowIndex].getHands().size();
                  case 2: 
                     Vector s = new Vector();
                     for (Hand h: (Hand[]) boards[rowIndex].getHands().toArray(new Hand[0]))  {
                        s.add(h.getNS());
                        s.add(h.getEW());
                     }
                     return s.toString();
                  default: return "";
               }
         }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex)
         {
            if (null == boards) return;
            else if (rowIndex >= boards.length) return;
            else 
               switch (columnIndex) {
                  case 0: 
                     boards[rowIndex].setNumber((String) aValue);
                     for (Hand h: (Hand[]) boards[rowIndex].getHands().toArray(new Hand[0]))  
                        h.setNumber((String) aValue);
                     break;
                  default:
                     break;
               }
         }
         public void addTableModelListener(TableModelListener l) {}
         public void removeTableModelListener(TableModelListener l) {}
         public Board getBoardAt(int row) 
         {
            if (null == boards) return null;
            else if (row >= boards.length) return null;
            else return boards[row];
         }
      }
      class HandTableDataModel implements TableModel
      {
         private Hand[] hands;
         public HandTableDataModel(List handv)
         {
            if (null == handv) return;
            Collections.sort(handv, new HandNSComparer());
            this.hands = (Hand[]) handv.toArray(new Hand[0]);
         }
         public int getRowCount()
         { return null == hands ? 1 : hands.length+1; }
         public int getColumnCount()
         { return 10; }
         public String getColumnName(int columnIndex)
         {
            switch (columnIndex) {
               case 0: return "Number";
               case 1: return "NS";
               case 2: return "EW";
               case 3: return "Contract";
               case 4: return "By";
               case 5: return "Tricks";
               case 6: return "Score NS";
               case 7: return "Score EW";
               case 8: return "MP NS";
               case 9: return "MP EW";
               default: return "ERR0R";
            }
         }
         public Class getColumnClass(int columnIndex)
         {
            switch (columnIndex) {
               case 0: 
               case 1: 
               case 2: 
               case 3: return String.class;
               case 4: return Character.class;
               case 5: return Integer.class;
               case 6: 
               case 7: 
               case 8: 
               case 9: return Double.class;
               default: return null;
            }
         }
         public boolean isCellEditable(int rowIndex, int columnIndex)
         {
            if (null == hands) return false;
            else return rowIndex < hands.length && columnIndex > 0;
         }
         public Object getValueAt(int rowIndex, int columnIndex)
         {
            if (null == hands || rowIndex >= hands.length) switch (columnIndex) {
               case 0: 
               case 1: 
               case 2: 
               case 3: return "";
               case 4: return ' ';
               case 5: return 0;
               case 6: 
               case 7: 
               case 8: 
               case 9: return 0.0;
               default: return null;
            }
            else 
               switch (columnIndex) {
                  case 0: return hands[rowIndex].getNumber();
                  case 1: return hands[rowIndex].getNS();
                  case 2: return hands[rowIndex].getEW();
                  case 3: return hands[rowIndex].getContract();
                  case 4: return hands[rowIndex].getDeclarer();
                  case 5: return hands[rowIndex].getTricks();
                  case 6: return hands[rowIndex].getNSScore();
                  case 7: return hands[rowIndex].getEWScore();
                  case 8: return hands[rowIndex].getNSMP();
                  case 9: return hands[rowIndex].getEWMP();
                  default: return null;
               }
         }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex)
         {
            if (null == hands) return;
            else if (rowIndex >= hands.length) return;
            else 
               switch (columnIndex) {
                  case 1: hands[rowIndex].setNS((String) aValue);
                          break;
                  case 2: hands[rowIndex].setEW((String) aValue);
                          break;
                  case 3: hands[rowIndex].setContract((String) aValue);
                          break;
                  case 4: hands[rowIndex].setDeclarer((Character) aValue);
                          break;
                  case 5: hands[rowIndex].setTricks((Integer) aValue);
                          break;
                  case 6: hands[rowIndex].setNSScore((Double) aValue);
                          break;
                  case 7: hands[rowIndex].setEWScore((Double) aValue);
                          break;
                  case 8: hands[rowIndex].setNSMP((Double) aValue);
                          break;
                  case 9: hands[rowIndex].setEWMP((Double) aValue);
                          break;
                  default:
                          break;
               }
         }
         public void addTableModelListener(TableModelListener l) {}
         public void removeTableModelListener(TableModelListener l) {}
      }
      class PairTableDataModel implements TableModel
      {
         private Pair[] pairs;
         public PairTableDataModel(Map pairv)
         {
            if (null == pairv) return;
            List sortedpairs = new ArrayList(pairv.values());
            Collections.sort(sortedpairs, new PairNumberComparer());
            this.pairs = (Pair[]) sortedpairs.toArray(new Pair[0]);
         }
         public int getRowCount()
         { return null == pairs ? 1 : pairs.length+1; }
         public int getColumnCount()
         { return 6; }
         public String getColumnName(int columnIndex)
         {
            switch (columnIndex) {
               case 0: return "Number";
               case 1: return "Names";
               case 2: return "";
               case 3: return "Match Points";
               case 4: return "Percentage";
               case 5: return "Local Points";
               default: return "ERR0R";
            }
         }
         public Class getColumnClass(int columnIndex)
         {
            switch (columnIndex) {
               case 0: 
               case 1: 
               case 2: return String.class;
               case 3: 
               case 4: 
               case 5: return Double.class;
               default: return null;
            }
         }
         public boolean isCellEditable(int rowIndex, int columnIndex)
         {
            if (null == pairs) return false;
            else return rowIndex < pairs.length;
         }
         public Object getValueAt(int rowIndex, int columnIndex)
         {
            if (null == pairs) return columnIndex > 2 ? 0.0 : "";
            else if (rowIndex >= pairs.length) 
               return columnIndex > 2 ? 0.0 : "";
            else 
               switch (columnIndex) {
                  case 0: return pairs[rowIndex].getNumber();
                  case 1: return pairs[rowIndex].getNames()[0];
                  case 2: return pairs[rowIndex].getNames()[1];
                  case 3: return pairs[rowIndex].getMPs();
                  case 4: return pairs[rowIndex].getPercentage();
                  case 5: return pairs[rowIndex].getLPs();
                  default: return null;
               }
         }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex)
         {
            if (null == pairs) return;
            else if (rowIndex >= pairs.length) return;
            else 
               switch (columnIndex) {
                  case 0: pairs[rowIndex].setNumber((String) aValue);
                          break;
                  case 1: 
                          String[] names = pairs[rowIndex].getNames();
                          names[0] = (String) aValue;
                          pairs[rowIndex].setNames(names);
                          break;
                  case 2: 
                          names = pairs[rowIndex].getNames();
                          names[1] = (String) aValue;
                          pairs[rowIndex].setNames(names);
                          break;
                  case 3: pairs[rowIndex].setMPs((Double) aValue);
                          break;
                  case 4: pairs[rowIndex].setPercentage((Double) aValue);
                          break;
                  case 5: pairs[rowIndex].setLPs((Double) aValue);
                          break;
                  default:
                          break;
               }
         }
         public void addTableModelListener(TableModelListener l) {}
         public void removeTableModelListener(TableModelListener l) {}
      }
      class MenuActionListener implements ActionListener
      {
         public void actionPerformed(ActionEvent e)
         {
             String command = e.getActionCommand();
             if (Debug.debug) Debug.print("Menu Action: "+command);
             if ("loadscores".equals(command)) {

                status.setText("Loading Boards...");
                JFileChooser fc;
                if (null == boardfile)
                   fc = new JFileChooser();
                else
                   fc = new JFileChooser(new File(boardfile).getParent());

                if (Debug.debug) Debug.print("Showing File Chooser");
                int rv = fc.showOpenDialog(GSalliereMainFrame.this);
                if (rv == JFileChooser.APPROVE_OPTION) {
                   if (Debug.debug) Debug.print("Approved, file="+fc.getSelectedFile());
                   File f = fc.getSelectedFile();
                   try {
                      boardfile = f.getCanonicalPath();
                      status.setText("Loading Boards from "+boardfile);
                      boards = readBoards(new FileInputStream(f));
                      for (Board b: (Board[]) boards.values().toArray(new Board[0])) 
                         b.validate();
                      boardtable.setModel(new BoardTableDataModel(boards));
                      status.setText("Loaded Boards from "+boardfile);
                   } catch (BoardValidationException BVe) {
                      if (Debug.debug) Debug.print(BVe);
                      showerror("Problem loading boards file: "+BVe);
                      boards = null;
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror("Problem loading boards file: "+IOe);
                      boards = null;
                   }
                } else {
                   if (Debug.debug) Debug.print("chooser returned "+rv);
                }

             } else if ("loadnames".equals(command)) {

                status.setText("Loading Names...");
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
                      status.setText("Loading Names from "+namesfile);
                      pairs = readPairs(new FileInputStream(f));
                      nametable.setModel(new PairTableDataModel(pairs));
                      status.setText("Loaded Names from "+namesfile);
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror("Problem loading names file: "+IOe);
                      pairs = null;
                   }
                }

             } else if ("savescores".equals(command)) {
                if (null == boards) showerror("Must Load Boards before saving them");
                else {

                   status.setText("Saving Boards");
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
                         status.setText("Saving Boards to "+boardfile);
                         writeBoards(boards, new FileOutputStream(f));
                         status.setText("Saved Boards to "+boardfile);
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror("Problem saving boards file: "+IOe);
                      }
                   }

                }
             } else if ("savenames".equals(command)) {
                if (null == pairs) showerror("Must Load Pairs before saving them");
                else {

                   status.setText("Saving Names");
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
                         status.setText("Saving Names to "+namesfile);
                         writePairs(pairs, new FileOutputStream(f));
                         status.setText("Saved Names to "+namesfile);
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror("Problem saving names file: "+IOe);
                      }
                   }
                }
             } else if ("export".equals(command)) {
                status.setText("Exporting results");
                if (null == pairs || null == boards) showerror("Must Load Pairs and boards before exporting");
                else 
                  export();
             } else if ("quit".equals(command)) {
                setVisible(false);
                System.exit(0);
             }
         }
      }
      class ButtonActionListener implements ActionListener
      {
         private JTextField setsize;
         public ButtonActionListener(JTextField setsize)
         {
            this.setsize = setsize;
         }
         public void actionPerformed(ActionEvent e)
         {
             String command = e.getActionCommand();
             try {
                if ("score".equals(command)) {
                   status.setText("Scoring boards");
                   if (null == boards) showerror("Must Load Boards before Scoring");
                   else score(boards);
                } else if ("matchpoint".equals(command)) {
                   status.setText("Matchpointing boards");
                   if (null == boards) showerror("Must Load Boards before Matchpointing");
                   else matchpoint(boards);
                } else if ("total".equals(command)) {
                   status.setText("Calculating total matchpoints and percentages");
                   if (null == boards || null == pairs) showerror("Must Load Boards and Pairs before Totalling");
                   else total(pairs, boards);
                   nametable.repaint();
                } else if ("localpoint".equals(command)) {
                   status.setText("Allocating local points");
                   if (null == pairs) showerror("Must Load Pairs before Scoring");
                   else localpoint(pairs);
                   nametable.repaint();
                } else if ("results".equals(command)) {
                   status.setText("Exporting results");
                   if (null == boards || null == pairs) showerror("Must Load Boards and Pairs before exporting results");
                   else export();
                } else if ("validate".equals(command)) {
                   status.setText("Verifying Movement");
                   if (null == boards) showerror("Must Load Boards before Verifying movement");
                   else verify(boards, setsize.getText());
                }
             } catch (ScoreException Se) {
               if (Debug.debug) Debug.print(Se);
               showerror("Problem while performing action: "+Se);
             } catch (ContractParseException CPe) {
               if (Debug.debug) Debug.print(CPe);
               showerror("Problem while performing action: "+CPe);
             } catch (MovementVerificationException MVe) {
               if (Debug.debug) Debug.print(MVe);
               showerror("Problem while performing action: "+MVe);
             }
         }
      }
      class MouseActionListener implements MouseListener
      {
         public void mouseClicked(MouseEvent e) 
         {
            if (Debug.debug) Debug.print(e);
            int y = e.getY();
            JTable table = (JTable) e.getSource();
            int row = y / table.getRowHeight();
            if (Debug.debug) Debug.print("Clicked on row "+row);
            Board b = ((BoardTableDataModel) table.getModel()).getBoardAt(row);
            if (null != b) {
               BoardEditDialog bed = new BoardEditDialog(b);
               bed.show();
               table.repaint();
            }
         }
         public void mousePressed(MouseEvent e) {}
         public void mouseReleased(MouseEvent e) {}
         public void mouseEntered(MouseEvent e) {}
         public void mouseExited(MouseEvent e) {}
      }
      private JPanel body;
      private JLabel status;
      private JTable boardtable;
      private JTable nametable;
      public GSalliereMainFrame()
      {
         super("GSalliere - Duplicate Bridge Scoring");
         setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

         /* MENU */
         JMenuBar menuBar = new JMenuBar();
         menuBar.setVisible(true);
         setJMenuBar(menuBar);
         setContentPane(new JPanel(new BorderLayout()));

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

         // quit
         item = new JMenuItem("Quit", KeyEvent.VK_Q);
         item.setActionCommand("quit");
         item.addActionListener(mal);
         file.add(item);

         /* MAIN BODY */
         body = new JPanel();
         body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
         body.setVisible(true);
         add(body, BorderLayout.CENTER);

         body.add(new JLabel("Pairs"));

         MouseListener moal = new MouseActionListener();

         nametable = new JTable(new PairTableDataModel(null));
         nametable.setVisible(true);
         body.add(new JScrollPane(nametable));

         if (null != pairs)
            nametable.setModel(new PairTableDataModel(pairs));

         body.add(new JLabel("Boards"));

         boardtable = new JTable(new BoardTableDataModel(null));
         boardtable.addMouseListener(moal);
         boardtable.setVisible(true);
         body.add(new JScrollPane(boardtable));

         if (null != boards)
            boardtable.setModel(new BoardTableDataModel(boards));

         /* BUTTON BAR 1 */
         JPanel buttonbarholder = new JPanel();
         buttonbarholder.setLayout(new BoxLayout(buttonbarholder, BoxLayout.Y_AXIS));
         add(buttonbarholder, BorderLayout.NORTH);

         JPanel buttonbar = new JPanel(new FlowLayout());
         buttonbar.setVisible(true);
         buttonbarholder.add(buttonbar);
         
         JButton button;
         JTextField setsize = new JTextField(2);
         ActionListener bal = new ButtonActionListener(setsize);

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

         /* BUTTON BAR 2 */
         buttonbar = new JPanel(new FlowLayout());
         buttonbar.setVisible(true);
         buttonbarholder.add(buttonbar);
         
         // validate
         buttonbar.add(new JLabel("Set Size:"));
         buttonbar.add(setsize);
         button = new JButton("Validate");
         button.setToolTipText("Validate the movement");
         button.setActionCommand("validate");
         button.setMnemonic(KeyEvent.VK_V);
         button.addActionListener(bal);
         buttonbar.add(button);

         /* STATUS BAR */
         status = new JLabel();
         status.setVisible(true);
         add(status, BorderLayout.SOUTH);

         pack();         
      }
      public void showerror(String text)
      {
         status.setText("Error: "+text);
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
               System.out.println("Problem loading boards file: "+BVe);
               boards = null;
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               System.out.println("Problem loading boards file: "+IOe);
               boards = null;
            }

            try {
               pairs = readPairs(new FileInputStream(namesfile));
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               System.out.println("Problem loading names file: "+IOe);
               pairs = null;
            }

         }

         GSalliereMainFrame main = new GSalliereMainFrame();
         main.setVisible(true);

      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
         System.out.println("There was a problem during the execution of GSalliere: "+e.getMessage());
         System.exit(1);
      }
   }
}
