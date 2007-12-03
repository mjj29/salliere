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
import static cx.ath.matthew.salliere.Gettext._;

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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
            super(GSalliereMainFrame.this, _("Edit Board ")+b.getNumber(), true);
            this.board = b;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setContentPane(new JPanel(new BorderLayout()));

            JTable data = new JTable(new HandTableDataModel(b));
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
         public BoardTableDataModel(List boards)
         {
            if (null == boards) return;
            Collections.sort(boards, new BoardNumberComparer());
            this.boards = (Board[]) boards.toArray(new Board[0]);
         }
         public int getRowCount()
         { return null == boards ? 1 : boards.length+1; }
         public int getColumnCount()
         { return  3; }
         public String getColumnName(int columnIndex)
         {
            switch (columnIndex) {
               case 0: return _("Number");
               case 1: return _("Played");
               case 2: return _("Pairs");
               default: return _("ERR0R");
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
            return columnIndex==0;
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
            if (null == boards) {
               GSalliere.boards = new Vector();
               boards = new Board[1];
               boards[0] = new Board();
               rowIndex = 0;
               GSalliere.boards.add(boards[0]);
            }
            else if (rowIndex >= boards.length) {
               Board[] t = new Board[boards.length+1];
               System.arraycopy(boards, 0, t, 0, boards.length);
               boards = t;
               rowIndex = boards.length-1;
               boards[rowIndex] = new Board();
               GSalliere.boards.add(boards[rowIndex]);
            }
            switch (columnIndex) {
               case 0: 
                  boards[rowIndex].setNumber((String) aValue);
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
         private Board b;
         public HandTableDataModel(Board b)
         {
            this.b = b;
            List handv = b.getHands();
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
               case 0: return _("Number");
               case 1: return _("NS");
               case 2: return _("EW");
               case 3: return _("Contract");
               case 4: return _("By");
               case 5: return _("Tricks");
               case 6: return _("Score NS");
               case 7: return _("Score EW");
               case 8: return _("MP NS");
               case 9: return _("MP EW");
               default: return _("ERR0R");
            }
         }
         public Class getColumnClass(int columnIndex)
         {
            switch (columnIndex) {
               case 0: 
               case 1: 
               case 2: 
               case 3: 
               case 4: 
               case 6:
               case 7: return String.class;
               case 5: return Integer.class;
               case 8: 
               case 9: return Double.class;
               default: return null;
            }
         }
         public boolean isCellEditable(int rowIndex, int columnIndex)
         {
            return columnIndex > 0;
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
                  case 6: switch (hands[rowIndex].getNSAverage()) {
                             case Hand.AVERAGE: return "av=";
                             case Hand.AVERAGE_PLUS: return "av+";
                             case Hand.AVERAGE_MINUS: return "av-";
                             default: return hands[rowIndex].getNSScore();
                          }
                  case 7: switch (hands[rowIndex].getEWAverage()) {
                             case Hand.AVERAGE: return "av=";
                             case Hand.AVERAGE_PLUS: return "av+";
                             case Hand.AVERAGE_MINUS: return "av-";
                             default: return hands[rowIndex].getEWScore();
                          }
                  case 8: return hands[rowIndex].getNSMP();
                  case 9: return hands[rowIndex].getEWMP();
                  default: return null;
               }
         }
         public void setValueAt(Object aValue, int rowIndex, int columnIndex)
         {
            try {
               if (null == hands) {
                  hands = new Hand[1];
                  hands[0] = new Hand();
                  rowIndex = 0;
                  b.addHand(hands[0]);
               }
               else if (rowIndex >= hands.length) {
                  Hand[] t = new Hand[hands.length+1];
                  System.arraycopy(hands, 0, t, 0, hands.length);
                  hands = t;
                  rowIndex = hands.length-1;
                  hands[rowIndex] = new Hand();
                  b.addHand(hands[rowIndex]);
               }
               switch (columnIndex) {
                  case 1: hands[rowIndex].setNS((String) aValue);
                          break;
                  case 2: hands[rowIndex].setEW((String) aValue);
                          break;
                  case 3: hands[rowIndex].setContract((String) aValue);
                          break;
                  case 4: 
                          for (int i = 0; i < ((String) aValue).length(); i++)
                             if (((String) aValue).charAt(i) != ' ')
                                hands[rowIndex].setDeclarer(((String) aValue).charAt(i));
                          break;
                  case 5: hands[rowIndex].setTricks((Integer) aValue);
                          break;
                  case 6: hands[rowIndex].setNSScore((String) aValue);
                          hands[rowIndex].setForcedNSScore(true);
                          break;
                  case 7: hands[rowIndex].setEWScore((String) aValue);
                          hands[rowIndex].setForcedEWScore(true);
                          break;
                  case 8: hands[rowIndex].setNSMP((Double) aValue);
                          hands[rowIndex].setForcedNSMP(true);
                          break;
                  case 9: hands[rowIndex].setEWMP((Double) aValue);
                          hands[rowIndex].setForcedEWMP(true);
                          break;
                  default:
                          break;
               }
            } catch (HandParseException HPe) {
               if (Debug.debug) Debug.print(HPe);
               showerror(_("Problem while entering score: ")+HPe);
            } catch (BoardValidationException BVe) {
               if (Debug.debug) Debug.print(BVe);
               showerror(_("Problem while exporting: ")+BVe);
            }
         }
         public void addTableModelListener(TableModelListener l) {}
         public void removeTableModelListener(TableModelListener l) {}
      }
      class PairTableDataModel implements TableModel
      {
         private Pair[] pairs;
         public PairTableDataModel(List pairs)
         {
            if (null == pairs) return;
            Collections.sort(pairs, new PairNumberComparer());
            this.pairs = (Pair[]) pairs.toArray(new Pair[0]);
         }
         public int getRowCount()
         { return null == pairs ? 1 : pairs.length+1; }
         public int getColumnCount()
         { return 6; }
         public String getColumnName(int columnIndex)
         {
            switch (columnIndex) {
               case 0: return _("Number");
               case 1: return _("Names");
               case 2: return "";
               case 3: return _("Match Points");
               case 4: return _("Percentage");
               case 5: return _("Local Points");
               default: return _("ERR0R");
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
            return true;
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
            if (null == pairs) {
               GSalliere.pairs = new Vector();
               pairs = new Pair[1];
               pairs[0] = new Pair();
               rowIndex = 0;
               GSalliere.pairs.add(pairs[0]);
            }
            else if (rowIndex >= pairs.length) {
               Pair[] t = new Pair[pairs.length+1];
               System.arraycopy(pairs, 0, t, 0, pairs.length);
               pairs = t;
               rowIndex = pairs.length-1;
               pairs[rowIndex] = new Pair();
               GSalliere.pairs.add(pairs[rowIndex]);
            }
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

                status.setText(_("Loading Boards..."));
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
                      status.setText(_("Loading Boards from ")+boardfile);
                      boards = readBoards(new FileInputStream(f));
                      boardtable.setModel(new BoardTableDataModel(boards));
                      status.setText(_("Loaded Boards from ")+boardfile);
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror(_("Problem loading boards file: ")+IOe);
                      boards = null;
                   } catch (BoardValidationException BVe) {
                      if (Debug.debug) Debug.print(BVe);
                      showerror(_("Problem loading boards file: ")+BVe);
                      boards = null;
                   }
                } else {
                   if (Debug.debug) Debug.print("chooser returned "+rv);
                }

             } else if ("loadnames".equals(command)) {

                status.setText(_("Loading Names..."));
                JFileChooser fc;
                if (null != namesfile)
                   fc = new JFileChooser(new File(namesfile).getParent());
                else if (null != boardfile)
                   fc = new JFileChooser(new File(boardfile).getParent());
                else
                   fc = new JFileChooser();

                int rv = fc.showOpenDialog(GSalliereMainFrame.this);
                if (rv == JFileChooser.APPROVE_OPTION) {
                   File f = fc.getSelectedFile();

                   try {
                      namesfile = f.getCanonicalPath();
                      status.setText(_("Loading Names from ")+namesfile);
                      pairs = readPairs(new FileInputStream(f), false);
                      nametable.setModel(new PairTableDataModel(pairs));
                      status.setText(_("Loaded Names from ")+namesfile);
                   } catch (IOException IOe) {
                      if (Debug.debug) Debug.print(IOe);
                      showerror(_("Problem loading names file: ")+IOe);
                      pairs = null;
                   }
                }

             } else if ("loadtricks".equals(command)) {

                status.setText(_("Loading Tricks..."));
                if (null == boards) showerror(_("Must Load Boards before loading trick data"));
                else {
                   JFileChooser fc;
                   if (null == boardfile)
                      fc = new JFileChooser();
                   else
                      fc = new JFileChooser(new File(boardfile).getParent());

                   int rv = fc.showOpenDialog(GSalliereMainFrame.this);
                   if (rv == JFileChooser.APPROVE_OPTION) {
                      File f = fc.getSelectedFile();

                      try {
                         namesfile = f.getCanonicalPath();
                         status.setText(_("Loading Tricks from ")+namesfile);
                         readTrickData(boards, new FileInputStream(f));
                         nametable.setModel(new PairTableDataModel(pairs));
                         status.setText(_("Loaded Tricks from ")+namesfile);
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror(_("Problem loading names file: ")+IOe);
                         pairs = null;
                      }
                   }
                }

             } else if ("savescores".equals(command)) {
                if (null == boards) showerror(_("Must Load Boards before saving them"));
                else {

                   status.setText(_("Saving Boards"));
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
                         status.setText(_("Saving Boards to ")+boardfile);
                         writeBoards(boards, new FileOutputStream(f));
                         status.setText(_("Saved Boards to ")+boardfile);
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror(_("Problem saving boards file: ")+IOe);
                      }
                   }

                }
             } else if ("savenames".equals(command)) {
                if (null == pairs) showerror(_("Must Load Pairs before saving them"));
                else {

                   status.setText(_("Saving Names"));
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
                         status.setText(_("Saving Names to ")+namesfile);
                         writePairs(pairs, new FileOutputStream(f));
                         status.setText(_("Saved Names to ")+namesfile);
                      } catch (IOException IOe) {
                         if (Debug.debug) Debug.print(IOe);
                         showerror(_("Problem saving names file: ")+IOe);
                      }
                   }
                }
             } else if ("export".equals(command)) {
                status.setText(_("Exporting results"));
                if (null == pairs || null == boards) showerror(_("Must Load Pairs and boards before exporting"));
                else 
                  export(GSalliereMainFrame.this);
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
                   status.setText(_("Scoring boards"));
                   if (null == boards) showerror(_("Must Load Boards before Scoring"));
                   else score(boards);
                } else if ("matchpoint".equals(command)) {
                   status.setText(_("Matchpointing boards"));
                   if (null == boards) showerror(_("Must Load Boards before Matchpointing"));
                   else matchpoint(boards);
                } else if ("ximp".equals(command)) {
                   status.setText(_("XIMPing boards"));
                   if (null == boards) showerror(_("Must Load Boards before XIMPing"));
                   else ximp(boards);
                } else if ("parimp".equals(command)) {
                   status.setText(_("IMPing boards against par"));
                   if (null == boards) showerror(_("Must Load Boards before IMPing"));
                   else parimp(boards);
                } else if ("total".equals(command)) {
                   status.setText(_("Calculating total matchpoints and percentages"));
                   if (null == boards || null == pairs) showerror(_("Must Load Boards and Pairs before Totalling"));
                   else total(pairs, boards);
                   nametable.repaint();
                } else if ("localpoint".equals(command)) {
                   status.setText(_("Allocating local points"));
                   if (null == pairs) showerror(_("Must Load Pairs before allocating local points"));
                   else localpoint(pairs, false);
                   nametable.repaint();
                } else if ("results".equals(command)) {
                   status.setText(_("Exporting results"));
                   if (null == boards || null == pairs) showerror(_("Must Load Boards and Pairs before exporting results"));
                   else export(GSalliereMainFrame.this);
                } else if ("validate".equals(command)) {
                   status.setText(_("Verifying Movement"));
                   if (null == boards) showerror(_("Must Load Boards before Verifying movement"));
                   else verify(boards, setsize.getText());
                }
             } catch (ScoreException Se) {
               if (Debug.debug) Debug.print(Se);
               showerror(_("Problem while performing action: ")+Se);
             } catch (ContractParseException CPe) {
               if (Debug.debug) Debug.print(CPe);
               showerror(_("Problem while performing action: ")+CPe);
             } catch (BoardValidationException BVe) {
                if (Debug.debug) Debug.print(BVe);
                showerror(_("Problem validating boards: ")+BVe);
             } catch (MovementVerificationException MVe) {
               if (Debug.debug) Debug.print(MVe);
               showerror(_("Problem while performing action: ")+MVe);
             } catch (HandParseException HPe) {
               if (Debug.debug) Debug.print(HPe);
               showerror(_("Problem while performing action: ")+HPe);
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
         super(_("GSalliere - Duplicate Bridge Scoring"));
         setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

         /* MENU */
         JMenuBar menuBar = new JMenuBar();
         menuBar.setVisible(true);
         setJMenuBar(menuBar);
         setContentPane(new JPanel(new BorderLayout()));

         // file menu
         JMenu file = new JMenu(_("File"));
         file.setMnemonic(KeyEvent.VK_F);
         file.getAccessibleContext().setAccessibleDescription(
                       _("Has options to load and save score files and export results"));
         menuBar.add(file);

         JMenuItem item;
         ActionListener mal = new MenuActionListener();

         // load score file
         item = new JMenuItem(_("Load Score File"), KeyEvent.VK_L);
         item.setActionCommand("loadscores");
         item.addActionListener(mal);
         file.add(item);

         // load names file
         item = new JMenuItem(_("Load Names File"), KeyEvent.VK_N);
         item.setActionCommand("loadnames");
         item.addActionListener(mal);
         file.add(item);

         // load tricks file
         item = new JMenuItem(_("Load Tricks File"), KeyEvent.VK_T);
         item.setActionCommand("loadtricks");
         item.addActionListener(mal);
         file.add(item);

         // save score file
         item = new JMenuItem(_("Save Score File"), KeyEvent.VK_S);
         item.setActionCommand("savescores");
         item.addActionListener(mal);
         file.add(item);

         // save names file
         item = new JMenuItem(_("Save Names File"), KeyEvent.VK_A);
         item.setActionCommand("savenames");
         item.addActionListener(mal);
         file.add(item);

         // export results
         item = new JMenuItem(_("Export Results"), KeyEvent.VK_E);
         item.setActionCommand("export");
         item.addActionListener(mal);
         file.add(item);

         // quit
         item = new JMenuItem(_("Quit"), KeyEvent.VK_Q);
         item.setActionCommand("quit");
         item.addActionListener(mal);
         file.add(item);

         /* MAIN BODY */
         body = new JPanel();
         body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
         body.setVisible(true);
         add(body, BorderLayout.CENTER);

         body.add(new JLabel(_("Pairs")));

         MouseListener moal = new MouseActionListener();

         nametable = new JTable(new PairTableDataModel(null));
         nametable.setVisible(true);
         body.add(new JScrollPane(nametable));

         if (null != pairs)
            nametable.setModel(new PairTableDataModel(pairs));

         body.add(new JLabel(_("Boards")));

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
         button = new JButton(_("Score"));
         button.setToolTipText(_("Score the contracts"));
         button.setActionCommand("score");
         button.setMnemonic(KeyEvent.VK_S);
         button.addActionListener(bal);
         buttonbar.add(button);

         // matchpoint
         button = new JButton(_("Match Point"));
         button.setToolTipText(_("Matchpoint the boards"));
         button.setActionCommand("matchpoint");
         button.setMnemonic(KeyEvent.VK_M);
         button.addActionListener(bal);
         buttonbar.add(button);

         // ximp
         button = new JButton(_("XIMP"));
         button.setToolTipText(_("Cross-IMP the boards"));
         button.setActionCommand("ximp");
         button.setMnemonic(KeyEvent.VK_X);
         button.addActionListener(bal);
         buttonbar.add(button);

         // parimp
         button = new JButton(_("IMP-Par"));
         button.setToolTipText(_("IMP the boards against par"));
         button.setActionCommand("parimp");
         button.setMnemonic(KeyEvent.VK_P);
         button.addActionListener(bal);
         buttonbar.add(button);

         // total
         button = new JButton(_("Total"));
         button.setToolTipText(_("Total the match points for the pairs"));
         button.setActionCommand("total");
         button.setMnemonic(KeyEvent.VK_T);
         button.addActionListener(bal);
         buttonbar.add(button);

         // localpoint
         button = new JButton(_("Local Point"));
         button.setToolTipText(_("Calculate local points for the pairs"));
         button.setActionCommand("localpoint");
         button.setMnemonic(KeyEvent.VK_M);
         button.addActionListener(bal);
         buttonbar.add(button);

         // results
         button = new JButton(_("Results"));
         button.setToolTipText(_("Export the Results"));
         button.setActionCommand("results");
         button.setMnemonic(KeyEvent.VK_R);
         button.addActionListener(bal);
         buttonbar.add(button);

         /* BUTTON BAR 2 */
         buttonbar = new JPanel(new FlowLayout());
         buttonbar.setVisible(true);
         buttonbarholder.add(buttonbar);
         
         // validate
         buttonbar.add(new JLabel(_("Set Size:")));
         buttonbar.add(setsize);
         button = new JButton(_("Validate"));
         button.setToolTipText(_("Validate the movement"));
         button.setActionCommand("validate");
         button.setMnemonic(KeyEvent.VK_V);
         button.addActionListener(bal);
         buttonbar.add(button);

         // save
         button = new JButton(_("Save"));
         button.setToolTipText(_("Save both files"));
         button.setActionCommand("save");
         button.setMnemonic(KeyEvent.VK_A);
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
         status.setText(_("Error: ")+text);
         JOptionPane.showMessageDialog(this, text, _("Error"), JOptionPane.ERROR_MESSAGE);
      }
   }
   public static void export(GSalliereMainFrame root)
   {
      JFileChooser fc = new JFileChooser();
      JPanel accessories = new JPanel();
      accessories.setLayout(new BoxLayout(accessories, BoxLayout.Y_AXIS));
      fc.setAccessory(accessories);

      JTextField title = new JTextField();
      ButtonGroup format = new ButtonGroup();
      JRadioButton textformat = new JRadioButton(_("Text"), true);
      textformat.setActionCommand("Text");
      format.add(textformat);
      JRadioButton htmlformat = new JRadioButton(_("HTML"), true);
      htmlformat.setActionCommand("HTML");
      format.add(htmlformat);
      JRadioButton pdfformat = new JRadioButton(_("PDF"), true);
      pdfformat.setActionCommand("PDF");
      format.add(pdfformat);
      JCheckBox results = new JCheckBox(_("Results"), true);
      JCheckBox matrix = new JCheckBox(_("MP Matrix"));
      JCheckBox boardby = new JCheckBox(_("Board-by-boards"));
      JCheckBox orange = new JCheckBox(_("Orange Points"));
      JCheckBox ximp = new JCheckBox("IMP");
      JCheckBox par = new JCheckBox(_("Show Par"));
      accessories.add(new JLabel(_("Title: ")));
      accessories.add(title);
      accessories.add(textformat);
      accessories.add(htmlformat);
      accessories.add(pdfformat);
      accessories.add(results);
      accessories.add(matrix);
      accessories.add(boardby);
      accessories.add(orange);
      accessories.add(ximp);
      accessories.add(par);


      int rv = fc.showDialog(root, _("Export"));
      if (rv == JFileChooser.APPROVE_OPTION) {
         File exportfile = fc.getSelectedFile();
         String titlestr = title.getText();
         boolean orangebool = orange.isSelected();
         boolean ximpbool = ximp.isSelected();
         boolean parbool = par.isSelected();

         String command = format.getSelection().getActionCommand();

         if (Debug.debug) Debug.print("Exporting to "+exportfile+" command="+command);
         try {
            FileOutputStream out = new FileOutputStream(exportfile);
            TablePrinter tabular = null;
            if ("Text".equals(command))
               tabular = new AsciiTablePrinter(new PrintStream(out));
            else if ("HTML".equals(command))
               tabular = new HTMLTablePrinter(titlestr, new PrintStream(out));
            else if ("PDF".equals(command))
               tabular = new PDFTablePrinter(titlestr, out);

            tabular.init();
            tabular.header(titlestr);

            if (results.isSelected()) 
               results(pairs, tabular, orangebool, ximpbool, false);
            if (matrix.isSelected()) matrix(pairs, boards, tabular);
            if (boardby.isSelected()) boardbyboard(boards, tabular, ximpbool, parbool);

            tabular.close();
            out.close();
         } catch (IOException IOe) {
            if (Debug.debug) Debug.print(IOe);
            root.showerror(_("Problem while exporting: ")+IOe);
         }
      }
   }

   private static List boards;
   private static String boardfile;
   private static String namesfile;
   private static String outputfile;
   private static List pairs;

   public static void main(String[] args)
   {
      try {
         if (Debug.debug) Debug.setThrowableTraces(true);

         if (args.length == 2) {
            boardfile = new File(args[0]).getCanonicalPath();
            namesfile = new File(args[1]).getCanonicalPath();

            try {
               boards = readBoards(new FileInputStream(boardfile));
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               System.out.println(_("Problem loading boards file: ")+IOe);
               boards = null;
            }

            try {
               pairs = readPairs(new FileInputStream(namesfile), false);
            } catch (IOException IOe) {
               if (Debug.debug) Debug.print(IOe);
               System.out.println(_("Problem loading names file: ")+IOe);
               pairs = null;
            }
         }
        

         GSalliereMainFrame main = new GSalliereMainFrame();
         main.setVisible(true);

      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
         System.out.println(_("There was a problem during the execution of GSalliere: ")+e.getMessage());
         System.exit(1);
      }
   }
}
