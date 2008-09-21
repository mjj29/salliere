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

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.MessageFormat;

public class Salliere
{
   // data gathered from looking at ecats-ftp sessions
   public static final String ECATS_SERVER = "sims.ecats.co.uk";
   public static final String ECATS_USERNAME = "simsuser";
   public static final String ECATS_PASSWORD = "simsuser32";
   public static final String ECATS_UPLOAD_DIR = "\\sims";
   public static final String ECATS_PROGRAM_VERSION = "5.6.28";

   static class HandNSComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         String n1 = ((Hand) ob1).getNS();
         String n2 = ((Hand) ob2).getNS();
         return n1.compareTo(n2);
      }
   }

   static class BoardNumberComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         String n1 = ((Board) ob1).getNumber();
         String n2 = ((Board) ob2).getNumber();
         String[] n = n1.split(":");
         String[] v = n[n.length-1].split(";");
         int num1 = Integer.parseInt(v[0]);
         n = n2.split(":");
         v = n[n.length-1].split(";");
         int num2 = Integer.parseInt(v[0]);

         return num1-num2;
      }
   }

   static class PairNumberComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         return ((Pair) ob1).getNumber().compareTo(((Pair) ob2).getNumber());
      }
   }

   static class PairPercentageComparer implements Comparator
   {
      public int compare(Object ob1, Object ob2)
      {
         return (int) ((((Pair) ob2).getPercentage()-((Pair) ob1).getPercentage())*100.0);
      }
   }

   static void readTrickData(List boards, InputStream trickdatafile) throws IOException
   {
      Collections.sort(boards, new BoardNumberComparer());

      BufferedReader br = new BufferedReader(new InputStreamReader(trickdatafile));

      for (Board b: (Board[]) boards.toArray(new Board[0])) {
         String s;
         if (null != (s = br.readLine()))
            b.importTricks(s);
      }

   }

   static List readBoards(InputStream is) throws IOException, BoardValidationException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      Map boards = new HashMap();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) {
               Debug.print(in.getCurrentRecord()+": "+Arrays.asList(values));
            }
            Board b = (Board) boards.get(values[0]);
            if (null == b) {
               b = new Board(values[0]);
               boards.put(values[0], b);
            }
            b.addHand(new Hand(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return new ArrayList(boards.values());
   }

   static List readPairs(InputStream is) throws IOException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      List pairs = new Vector();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) Debug.print(Arrays.asList(values));
            pairs.add(new Pair(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return pairs;
   }

   static void writePairs(List pairs, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
         out.writeRecord(p.export());
      out.close();
   }

   static void syntax()
   {
      String version = Package.getPackage("cx.ath.matthew.salliere")
                              .getImplementationVersion();
      System.out.println("Salliere Duplicate Bridge Scorer - version "+version);
      System.out.println("Usage: salliere [options] [commands] -- <boards.csv> <names.csv>");
      System.out.println("   Commands: verify score matchpoint ximp parimp total handicap localpoint results matrix boards ecats-upload");
      System.out.println("   Options: --help --output=[<format>:]file --title=title --orange --setsize=N --ximp --with-par --trickdata=<tricks.txt> --handicapdata=<handicap.csv> --with-handicaps --handicap-normalizer=<num> --ecats-options=<key:val,key2:val2,...>");
      System.out.println("   Formats: txt html htmlfrag pdf");
      System.out.println("   ECATS options: ");
      System.out.println("      clubName = name of club (required)");
      System.out.println("      session = ECATS session number (required)");
      System.out.println("      phone = contact phone number (required)");
      System.out.println("      country = club country (required)");
      System.out.println("      name = contact name");
      System.out.println("      fax = contact fax number");
      System.out.println("      email = contact email");
      System.out.println("      town = club town");
      System.out.println("      county = club county");
      System.out.println("      date = event date");
      System.out.println("      event = event name");
   }


   static void writeBoards(List boards, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            out.writeRecord(h.export());
      out.close();
   }

   private static void doC(PrintStream out, List boards, Map options)
   {
      // write C.txt
      out.print("\t");
      out.print("1"); //spare = 1
      out.print("\t");
      out.print("\"false\""); // true iff 2 winner movement
      out.print("\t");
      out.print(""+boards.size()); // # boards played (not important)
      out.print("\t");
      out.print("0"); // #boards/round (not important)
      out.print("\t");
      out.print("\"true\""); // true iff contracts were recorded
      out.print("\t");
      out.print('"'+(String) options.get("clubName")+'"'); //club name, compulsory (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("town")+'"'); //town (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("county")+'"'); //county (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("country")+'"'); //country, compulsory, spelling agreed in advance (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("name")+'"'); //name (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("phone")+'"'); //phone, compulsory (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("fax")+'"'); //fax (max 50 chars)
      out.print("\t");
      out.print('"'+(String) options.get("email")+'"'); //email (max 50 chars)
      out.print("\t");
      out.print("\"false\""); //spare = false
      out.print("\t");
      out.print((String) options.get("session")); //session #, compulsory, numeric
      out.print("\t");
      out.print('"'+ECATS_PROGRAM_VERSION+'"'); // program version. Important. My program? their program? this number seems to work...
      out.print("\t");
      out.print('"'+(String) options.get("date")+'"'); // "dd/mm/yyyy" 
      out.print("\t");
      out.print('"'+(String) options.get("event")+'"'); // event name, text (max 50 chars)
      out.print("\r\n");
   }

   private static void doP(PrintStream out, List pairs)
   {
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
         out.print("\t");
         out.print("0"); // spare = 0
         out.print("\t");
         out.print("0"); // spare = 0
         out.print("\t");
         out.print(p.getNumber()); // pair number (int)
         out.print("\t");
         String[] names = p.getNames();
         out.print('"'+names[0]+" & "+names[1]+'"');
         out.print("\t");
         out.print('"'+names[0]+'"');
         out.print("\t");
         out.print('"'+names[1]+'"');
         out.print("\t");
         out.print("\"NS\""); // for a 2 winner movement, set this to the direction they were sitting. 
         out.print("\r\n");
      }
   }
   private static void doR(PrintStream out, List boards, String session)
   {
      for (Board bd: (Board[]) boards.toArray(new Board[0])) {
         for (Hand b: (Hand[]) bd.getHands().toArray(new Hand[0])) {
            out.print("\t");
            out.print("0"); // spare == 0
            out.print("\t");
            out.print("\"\""); // spare == ""
            out.print("\t");
            out.print("\"\""); // section, not used == ""
            out.print("\t");
            out.print(b.getNumber()); // int, non zero
            out.print("\t");
            out.print(b.getNS());
            out.print("\t");
            out.print(b.getEW());
            out.print("\t");
            out.print(""+(int) b.getNSScore());
            out.print("\t");
            out.print(""+(int) b.getEWScore());
            out.print("\t");
            out.print("0"); // spare = 0
            out.print("\t");
            out.print('"'+b.getContract()+'"'); // text, not used
            out.print("\t");
            out.print("\""+b.getTricks()+'"'); // text, not used
            out.print("\t");
            out.print("\""+b.getDeclarer()+'"'); // text, not used
            out.print("\t");
            out.print(""+b.getNSMP()); // double, recalculated
            out.print("\t");
            out.print(""+b.getEWMP()); // double, recalculated
            out.print("\t");
            out.print('"');
            if (b.isAveraged()) {
               out.print("A");
               switch (b.getNSAverage()) {
                  case Hand.AVERAGE_PLUS:
                     out.print("60");
                     break;
                  case Hand.AVERAGE_MINUS:
                     out.print("40");
                     break;
                  default:
                     out.print("50");
               }
               switch (b.getEWAverage()) {
                  case Hand.AVERAGE_PLUS:
                     out.print("60");
                     break;
                  case Hand.AVERAGE_MINUS:
                     out.print("40");
                     break;
                  default:
                     out.print("50");
               }
            }
            out.print('"');
            out.print("\t");
            out.print(session); // session number again
            out.print("\t");
            out.print("0"); //spare = 0
            out.print("\t");
            out.print("0"); //spare = 0
            out.print("\r\n");
         }
      }
   }

   public static void exportToECATS(List boards, List pairs, Map options, String exportdir) throws ScoreException
   {
      try {
         if (null == exportdir)
            exportdir = new File (".").getCanonicalPath();
         System.err.println(_("Exporting scores in ECATS format to ")+exportdir);

         String session = (String) options.get("session");
   
         // write club file
         PrintStream out = new PrintStream(new FileOutputStream(exportdir+"/C.txt"));
         doC(out, boards, options);
         out.close();

         // write pairs file
         out = new PrintStream(new FileOutputStream(exportdir+"/P.txt"));
         doP(out, pairs);
         out.close();

         // write boards file
         out = new PrintStream(new FileOutputStream(exportdir+"/R.txt"));
         doR(out, boards, session);
         out.close();

         // write end file
         out = new PrintStream(new FileOutputStream(exportdir+"/E.txt"));
         out.print("End\r\n");
         out.close();

      } catch (IOException IOe) {
         if (Debug.debug) Debug.print(IOe);
         throw new ScoreException(_("Exception occurred while trying to upload to ECATS: ")+IOe.getMessage());
      }

      System.err.println(_("ECATS files have been written. Email the files C.txt, P.txt, R.txt and E.txt to results@simpairs.com"));
   }


   public static void uploadToECATS(List boards, List pairs, Map options) throws ScoreException
   {
      System.err.println(_("Uploading scores to ECATS"));

      // connect
      FTPClient ftp = new FTPClient();
      try {
         ftp.connect(ECATS_SERVER);
         ftp.login(ECATS_USERNAME, ECATS_PASSWORD);
         ftp.changeWorkingDirectory(ECATS_UPLOAD_DIR);
         ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
         ftp.enterLocalPassiveMode();

         // get options
         String session = (String) options.get("session");
         String clubName = (String) options.get("clubName");
         String phone = (String) options.get("phone");

         // calculate file prefix
         StringBuffer prefixb = new StringBuffer();
         for (int i = session.length(); i < 6; i++)
            prefixb.append('0');
         prefixb.append(session);
         prefixb.append(clubName.replaceAll(" ", ""));
         prefixb.append(phone.replaceAll(" ", ""));
         String prefix = prefixb.toString();

         // write club file
         PrintStream out = new PrintStream(ftp.storeFileStream(prefix+"C.txt"));
         doC(out, boards, options);
         out.close();
         if (!ftp.completePendingCommand()) throw new IOException(_("Uploading club details failed"));

         // write pairs file
         System.err.print(_("Uploading pair details"));
         out = new PrintStream(ftp.storeFileStream(prefix+"P.txt"));
         doP(out, pairs);
         out.close();
         if (!ftp.completePendingCommand()) throw new IOException(_("Uploading pairs failed"));
         System.err.println();

         // write boards file
         System.err.print(_("Uploading results"));
         out = new PrintStream(ftp.storeFileStream(prefix+"R.txt"));
         doR(out, boards, session);
         out.close();
         System.err.println();
         if (!ftp.completePendingCommand()) throw new IOException(_("Uploading results failed"));

         // write end file
         out = new PrintStream(ftp.storeFileStream(prefix+"E.txt"));
         out.print("End\r\n");
         out.close();
         if (!ftp.completePendingCommand()) throw new IOException(_("Uploading end file failed"));

         // log out
         System.err.println(_("Upload complete"));
         ftp.logout();
         ftp.disconnect();
      } catch (IOException IOe) {
         if (Debug.debug) Debug.print(IOe);
         try {
            ftp.logout();
            ftp.disconnect();
         } catch (IOException e) {}
         throw new ScoreException(_("Exception occurred while trying to upload to ECATS: ")+IOe.getMessage());
      }
   }

   public static void score(List boards) throws ScoreException, ContractParseException, HandParseException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         for (Hand h: (Hand[]) b.getHands().toArray(new Hand[0])) 
            if (!h.isAveraged())
               h.score();
      modifiedboards = true;
   }

   public static void verify(List boardv, String setsize) throws MovementVerificationException, BoardValidationException, HandParseException
   {
      for (Board b: (Board[]) boardv.toArray(new Board[0])) 
         b.validate();

      if (null == setsize || setsize.length() == 0) return;

      int size = 0;
      try {
         size = Integer.parseInt(setsize);
      } catch (NumberFormatException NFe) {
         if (Debug.debug) Debug.print(NFe);
         throw new MovementVerificationException(setsize+_(" isn't a number!"));
      }
      
      Collections.sort(boardv, new BoardNumberComparer());
      Board[] boards = (Board[]) boardv.toArray(new Board[0]);
      Set pairs = new TreeSet();
      for (int i = 0; i < boards.length; i++) {
         if (0 == (i % size)) {
            // reset the pairs
            pairs.clear();
            for (Hand h: (Hand[]) boards[i].getHands().toArray(new Hand[0])) 
               pairs.add(h.getNS()+" "+h.getEW());
         } else {
            // check the pairs
            for (Hand h: (Hand[]) boards[i].getHands().toArray(new Hand[0])) 
               if (!pairs.contains(h.getNS()+" "+h.getEW()))
                  throw new MovementVerificationException(
                        MessageFormat.format(_("Board {0} was played by {1} and {2}, which I was not expecting."), 
                           new Object[] { boards[i].getNumber(), h.getNS(), h.getEW() }));
         }
      }
   }

   public static void matchpoint(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.matchPoint();
      modifiedboards = true;
   }

   public static void ximp(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.ximp();
      modifiedboards = true;
   }

   public static void parimp(List boards) throws ScoreException
   {
      for (Board b: (Board[]) boards.toArray(new Board[0])) 
         b.parimp();
      modifiedboards = true;
   }

   public static void total(List pairs, List boards)
   {
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
         p.total(boards);
      modifiedpairs = true;
   }

   public static void matrix(List pairv, List boardv, TablePrinter tabular) 
   {
      Collections.sort(boardv, new BoardNumberComparer());
      Collections.sort(pairv, new PairNumberComparer());
      Board[] boards = (Board[]) boardv.toArray(new Board[0]);
      Pair[] pairs = (Pair[]) pairv.toArray(new Pair[0]);

      DecimalFormat format = new DecimalFormat("0.##");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
      
      String[][] matrix = new String[boards.length][pairs.length+1]; 
      for (int i = 0; i < boards.length; i++) {
         matrix[i][0] = boards[i].getNumber();
         for (int j = 0; j < pairs.length; j++) {
            StringBuffer tmp = new StringBuffer();
            if (boards[i].played(pairs[j].getNumber()))
               matrix[i][j+1] = format.format(boards[i].getMPs(pairs[j].getNumber()), tmp, field).toString();
            else
               matrix[i][j+1] = " ";
         }
      }

      String[] headers = new String[pairs.length+1];
      headers[0] = _("Board");
      for (int j = 0; j < pairs.length; j++) 
         headers[j+1] = pairs[j].getNumber()+"  ";

      tabular.print(headers, matrix);
      tabular.gap();
   }
   public static void boardbyboard(List boards, TablePrinter tabular, boolean ximp, boolean withpar) 
   {
      String[] headers = new String[] {
            _("NS"),
            _("EW"),
            _("Contract"), 
            _("By"), 
            _("Tricks"),
            _("Score:"),
            "",
            ximp ? _("IMPs") 
                 : _("MPs:"), 
            "" };
      Collections.sort(boards, new BoardNumberComparer());

      for (Board b: (Board[]) boards.toArray(new Board[0])) {
         Vector lines = new Vector();
         List hands = b.getHands();
         Collections.sort(hands, new HandNSComparer());
         for (Hand h: (Hand[]) hands.toArray(new Hand[0])) {
            String[] ex = h.export();
            String[] line = new String[ex.length-1];
            System.arraycopy(ex, 1, line, 0, line.length);
            lines.add(line);
         }
         if (withpar)
            tabular.header(_("Board: ")+b.getNumber()+" [ "+b.getParContract()+" by "+b.getPar().getDeclarer()+" ]");
         else
            tabular.header(_("Board: ")+b.getNumber());
         tabular.print(headers, (String[][]) lines.toArray(new String[0][]));
         tabular.gap();
      }
   }

   public static Map readHandicapData(List pairs, InputStream handicapfile) throws ScoreException
   {
      Map handicaps = new HashMap();
      Map pairhandicaps = new HashMap();
      try {
         CsvReader in = new CsvReader(new InputStreamReader(handicapfile));
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (values.length != 2) throw new ScoreException(_("Malformed handicap line: ")+Arrays.deepToString(values));
            handicaps.put(values[0], new Double(values[1]));
         }
         in.close();
      } catch (IOException IOe) {
         if (Debug.debug) Debug.print(IOe);
         throw new ScoreException(_("Failure in reading handicap file: ")+IOe.getMessage());
      }
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
         double handicap = 0.0;
         String[] names = p.getNames();
         for (String n: names) {
            Double v = (Double) handicaps.get(n);
            if (null == v) v = 50.0;
            handicap += v;
         }
         handicap /= (double) names.length;
         pairhandicaps.put(p, handicap);
      }
      return pairhandicaps;
   }
   public static void handicap(List pairs, Map handicaps,  double normalize) throws ScoreException
   {
      if (null == handicaps) throw new ScoreException(_("Must supply a handicap file before calculating handicapped scores"));
      for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
         double handicap = (Double) handicaps.get(p);
         p.setPercentage(p.getPercentage()-handicap+normalize);
      }
      modifiedpairs = true;
   }
   public static void localpoint(List pairs) throws ScoreException
   {
      int rate = (1 == Pair.getMaxNames()) ? 3 : 6;
      // sort pairs in order
      Collections.sort(pairs, new PairPercentageComparer());
      Pair[] ps = (Pair[]) pairs.toArray(new Pair[0]);

      // check we have enough to give LPs
      if (ps.length < ((1 == Pair.getMaxNames())?8:6)) throw new ScoreException(_("Must have at least 3 full tables at pairs or 2 full tables at individuals to award local points"));

      // calculate LP scale based on number of pairs
      int[] LPs = new int[ps.length];
      int awarded = (int) Math.ceil(LPs.length/3.0);
      int top;
      if (1 == Pair.getMaxNames()) {
         int tables = (int) Math.floor(ps.length/4.0);
         top = (3 * (int) Math.ceil(tables/3.0)) + (3 * tables);
      } else
         top = rate*awarded;
      if (top > 100) top = 100;
      if (Debug.debug) Debug.print(Debug.DEBUG, "Awarding LPs to "+ps.length+" pairs, top is "+top+" rate is "+rate+" number receiving LPs: "+awarded);
      for (int i = 0, lp=top; i < awarded; i++, lp -= rate)
         LPs[i] = lp > 6 ? lp : 6;

      // award LPs, splitting on draws
      for (int i = 0; i < ps.length && i < awarded; i++) {
         int a = i;
         int total = LPs[i];
         while (i+1 < ps.length && ps[i].getPercentage()==ps[i+1].getPercentage())
            total += LPs[++i];
         double award = Math.ceil(total / (1.0+i-a));
         if (award < 6 && award > 0) award = 6;
         for (int j = a; j <= i; j++) {
            if (ps[j].getLPs() != 0 && ps[j].getLPs() != award) 
               throw new ScoreException(
                     MessageFormat.format(_("Calculated {0} LPs for pair {1} but data says {2}."),
                        new Object[] { award, ps[j].getNumber(), ps[j].getLPs() }));
            ps[j].setLPs(award);
         }
      }
      modifiedpairs = true;
   }

   public static void results(List pairs, TablePrinter tabulate, boolean orange, boolean ximp, Map handicapdata, boolean handicaps)
   {
      Vector results = new Vector();
      Collections.sort(pairs, new PairPercentageComparer());

      Vector headerv = new Vector();
      if (Pair.getMaxNames() == 1) {
         headerv.add(_("Num"));
         headerv.add(_("Name"));
      } else {
         headerv.add(_("Pair"));
         headerv.add(_("Names"));
         for (int i = 1; i < Pair.getMaxNames(); i++)
            headerv.add("");
      }
      if (ximp) 
         headerv.add(_("IMPs"));
      else {
         headerv.add(_("MPs"));
         if (handicaps)
            headerv.add(_("h'cap"));
         headerv.add(_("%age"));
      }
      if (orange)
         headerv.add(_("OPs"));
      else
         headerv.add(_("LPs"));

      String[] header = (String[]) headerv.toArray(new String[0]);

      if (ximp)
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) {
            String[] a = p.export();
            String[] b = new String[a.length-1];
            System.arraycopy(a, 0, b, 0, 4);
            System.arraycopy(a, 5, b, 4, a.length-5);
            results.add(b);
         }
      else
         for (Pair p: (Pair[]) pairs.toArray(new Pair[0])) 
            results.add(p.export());

      if (handicaps) 
         for (int i = 0; i < pairs.size(); i++) {
            Pair p = (Pair) pairs.get(i);
            String[] r = (String[]) results.get(i);
            String[] n = new String[r.length+1];
            System.arraycopy(r, 0, n, 0, r.length-2);
            DecimalFormat format = new DecimalFormat("0.#");
            FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
            StringBuffer tmp = new StringBuffer();

            n[r.length-2] = format.format(handicapdata.get(p), tmp, field).toString();
            if (Debug.debug) Debug.print("Printing handicap "+n[r.length-2]+" for "+p);
            System.arraycopy(r, r.length-2, n, r.length-1, 2);
            results.set(i, n);
         }

      tabulate.print(header, 
            (String[][]) results.toArray(new String[0][]));
      tabulate.gap();
   }

   public static boolean modifiedpairs = false;
   public static boolean modifiedboards = false;

   public static void main(String[] args)
   {
      try {
         if (Debug.debug) {
            File f = new File("debug.conf");
            if (f.exists())
               Debug.loadConfig(f);
            Debug.setThrowableTraces(true);
         }
         Vector commands = new Vector();
         HashMap options = new HashMap();
         options.put("--output", "-");
         options.put("--help", null);
         options.put("--orange", null);
         options.put("--ximp", null);
         options.put("--title", _("Salliere Duplicate Bridge Scorer: Results"));
         options.put("--setsize", null);
         options.put("--with-par", null);
         options.put("--with-handicaps", null);
         options.put("--trickdata", null);
         options.put("--handicapdata", null);
         options.put("--handicap-normalizer", "0.0");
         options.put("--ecats-options", null);
         options.put("--ecats-export-dir", null);
         int i;
         for (i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) break;
            else if (args[i].startsWith("--")) {
               String[] opt = args[i].split("=");
               if (Debug.debug) Debug.print(Arrays.asList(opt));
               if (options.containsKey(opt[0])) {
                  if (opt.length == 1)
                     options.put(opt[0], "true");
                  else
                     options.put(opt[0], opt[1]);
               } else {
                  System.out.println(_("Error: unknown option ")+opt[0]);
                  syntax();
                  System.exit(1);
               }
            } else
               commands.add(args[i]);
         }

         if (args.length < (i+3)) {
            System.out.println(_("You must specify boards.csv and names.csv"));
            syntax();
            System.exit(1);
         }

         if (null != options.get("--help")) {
            syntax();
            System.exit(1);
         }

         List boards;
         List pairs;

         boards = readBoards(new FileInputStream(args[i+1]));
         pairs = readPairs(new FileInputStream(args[i+2]));

         if (null != options.get("--trickdata")) {
            readTrickData(boards, new FileInputStream((String) options.get("--trickdata")));
         }

         Map handicapdata = null;

         if (null != options.get("--handicapdata")) {
            handicapdata = readHandicapData(pairs, new FileInputStream((String) options.get("--handicapdata")));
         }

         Map ecatsoptions = new ECatsOptionsMap((String) options.get("--ecats-options"));

         TablePrinter tabular = null;

         String[] format = (String[]) ((String) options.get("--output")).split(":");
         PrintStream out;
         if ("-".equals(format[format.length-1]))
            out = System.out;
         else 
            out = new PrintStream(new FileOutputStream(format[format.length-1]));

         if (format.length == 1)
            tabular = new AsciiTablePrinter(out);
         else if ("txt".equals(format[0].toLowerCase()))
            tabular = new AsciiTablePrinter(out);
         else if ("html".equals(format[0].toLowerCase()))
            tabular = new HTMLTablePrinter((String) options.get("--title"), out);
         else if ("htmlfrag".equals(format[0].toLowerCase()))
            tabular = new HTMLFragTablePrinter((String) options.get("--title"), out);
         else if ("pdf".equals(format[0].toLowerCase()))
            tabular = new PDFTablePrinter((String) options.get("--title"), out);
         else {
            System.out.println(_("Unknown format: ")+format[0]);
            syntax();
            System.exit(1);
         }
         
         tabular.init();
         tabular.header((String) options.get("--title"));

         for (String command: (String[]) commands.toArray(new String[0])) {
            if ("score".equals(command)) score(boards);
            else if ("verify".equals(command)) verify(boards, (String) options.get("--setsize"));
            else if ("matchpoint".equals(command)) matchpoint(boards);
            else if ("total".equals(command)) total(pairs, boards);
            else if ("results".equals(command)) results(pairs, tabular, null != options.get("--orange"), null != options.get("--ximp"), handicapdata, null != options.get("--with-handicaps"));
            else if ("matrix".equals(command)) matrix(pairs, boards, tabular);
            else if ("boards".equals(command)) boardbyboard(boards, tabular, null != options.get("--ximp"), null != options.get("--with-par"));
            else if ("localpoint".equals(command)) localpoint(pairs);
            else if ("handicap".equals(command)) handicap(pairs, handicapdata, Double.parseDouble((String) options.get("--handicap-normalizer")));
            else if ("ximp".equals(command)) ximp(boards);
            else if ("parimp".equals(command)) parimp(boards);
            else if ("ecats-upload".equals(command)) uploadToECATS(boards, pairs, ecatsoptions);
            else if ("ecats-export".equals(command)) exportToECATS(boards, pairs, ecatsoptions, (String) options.get("--ecats-export-dir"));
            else {
               System.out.println(_("Bad Command: ")+command);
               syntax();
               System.exit(1);
            }
         }

         tabular.close();
         out.close();
     
         if (modifiedboards) 
            writeBoards(boards, new FileOutputStream(args[i+1]));
         if (modifiedpairs) 
            writePairs(pairs, new FileOutputStream(args[i+2]));
      } catch (Exception e) {
         if (Debug.debug) {
            Debug.setThrowableTraces(true);
            Debug.print(e);
         }
         System.out.println(_("Salliere failed to compute results: ")+e.getMessage());
         System.exit(1);
      }
   }
}
