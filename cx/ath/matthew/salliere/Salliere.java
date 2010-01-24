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

   static class HandNSComparer implements Comparator<Hand>
   {
      public int compare(Hand ob1, Hand ob2)
      {
         String n1 = ob1.getNS();
         String n2 = ob2.getNS();
         return n1.compareTo(n2);
      }
   }

   static class BoardNumberComparer implements Comparator<Board>
   {
      public int compare(Board ob1, Board ob2)
      {
         String n1 = ob1.getNumber();
         String n2 = ob2.getNumber();
         String[] n = n1.split(":");
         String[] v = n[n.length-1].split(";");
         int num1 = Integer.parseInt(v[0]);
         n = n2.split(":");
         v = n[n.length-1].split(";");
         int num2 = Integer.parseInt(v[0]);

         return num1-num2;
      }
   }

   static class PairNumberComparer implements Comparator<Pair>
   {
      public int compare(Pair ob1, Pair ob2)
      {
         return ob1.getNumber().compareTo(ob2.getNumber());
      }
   }

   static class PairPercentageComparer implements Comparator<Pair>
   {
      public int compare(Pair ob1, Pair ob2)
      {
			if (Debug.debug) Debug.print(Debug.VERBOSE, "Comparing "+ob1.getNumber()+"["+ob1.getPercentage()+"] and "+ob2.getNumber()+"["+ob2.getPercentage()+"]");
			
			// no need to split tie
			if (ob2.getPercentage() != ob1.getPercentage()) return (int) ((ob2.getPercentage()-ob1.getPercentage())*10000.0);

			List<Hand> hands1 = ob1.getHands();
			List<Hand> hands2 = ob2.getHands();

			if (hands1.size() == 0 || hands2.size() == 0) {
				if (Debug.debug) Debug.print(Debug.INFO, "Can't Split tie for "+ob1.getNumber()+" and "+ob2.getNumber()+", because there are no hands");
				return 0;
			}

			if (Debug.debug) Debug.print(Debug.INFO, "Splitting tie for "+ob1.getNumber()+" and "+ob2.getNumber());

			/* Only using this method here, so only works for all-play-all events without direct matches
				and only for two-way ties. Matchpoint the matchpoints for these two pairs */
			if (Debug.debug) Debug.print(Debug.DEBUG, "Point-a-board count-back:");
			int points1 = 0, points2 = 0;

			HashMap<String, Hand> hands2map = new HashMap<String, Hand>();
			for (Hand h: hands2) hands2map.put(h.getNumber(), h);
			
			for (Hand h: hands1) {
				if (Debug.debug) Debug.print(Debug.VERBOSE, "Board: "+h.getNumber());
				double mp1 = 0.0, mp2 = 0.0;
				if (h.getNS().equals(ob1.getNumber()) 
					|| h.getNS().startsWith(ob1.getNumber()+".") 
					|| h.getNS().endsWith("."+ob1.getNumber())) mp1 = h.getNSMP();
				else mp1 = h.getEWMP();
				if (hands2map.containsKey(h.getNumber())) {
					h = hands2map.remove(h.getNumber());
					if (h.getNS().equals(ob2.getNumber()) 
						|| h.getNS().startsWith(ob2.getNumber()+".") 
						|| h.getNS().endsWith("."+ob2.getNumber())) mp2 = h.getNSMP();
					else mp2 = h.getEWMP();
				} else {
					if (Debug.debug) Debug.print(Debug.VERBOSE, ob2.getNumber()+" not played, treating as average");
					mp2 = h.getBoard().getTop() / 2;
				}

				if (mp1 > mp2) {
					if (Debug.debug) Debug.print(Debug.VERBOSE, ob1.getNumber()+" win");
					points1 += 2;
				} else if (mp1 < mp2) {
					if (Debug.debug) Debug.print(Debug.VERBOSE, ob2.getNumber()+" win");
					points2 += 2;
				} else {
					if (Debug.debug) Debug.print(Debug.VERBOSE, "Draw");
					points1++;
					points2++;
				}
			}

			for (Hand h: hands2map.values()) {
				if (Debug.debug) Debug.print(Debug.VERBOSE, "Board: "+h.getNumber());
				if (Debug.debug) Debug.print(Debug.VERBOSE, ob1.getNumber()+" not played, treating as average");
				double mp1 = 0.0, mp2 = 0.0;
				mp1 = h.getBoard().getTop() / 2;
				if (h.getNS().equals(ob2.getNumber()) 
					|| h.getNS().startsWith(ob2.getNumber()+".") 
					|| h.getNS().endsWith("."+ob2.getNumber())) mp2 = h.getNSMP();
				else mp2 = h.getEWMP();
				if (mp1 > mp2) {
					if (Debug.debug) Debug.print(Debug.VERBOSE, ob1.getNumber()+" win");
					points1 += 2;
				} else if (mp1 < mp2) {
					if (Debug.debug) Debug.print(Debug.VERBOSE, ob2.getNumber()+" win");
					points2 += 2;
				} else {
					if (Debug.debug) Debug.print(Debug.VERBOSE, "Draw");
					points1++;
					points2++;
				}
			}

			if (Debug.debug) Debug.print(Debug.DEBUG, ob1.getNumber()+" have "+points1+" points and "+ob2.getNumber()+" have "+points2+" points.");
			return points2-points1;
      }
   }

   static void readTrickData(List<Board> boards, InputStream trickdatafile) throws IOException
   {
      Collections.sort(boards, new BoardNumberComparer());

      BufferedReader br = new BufferedReader(new InputStreamReader(trickdatafile));

      for (Board b: boards) {
         String s;
         if (null != (s = br.readLine()))
            b.importTricks(s);
      }

   }

   static List<Board> readBoards(InputStream is) throws IOException, BoardValidationException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      Map<String, Board> boards = new HashMap<String, Board>();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) {
               Debug.print(in.getCurrentRecord()+": "+Arrays.asList(values));
            }
            Board b = boards.get(values[0]);
            if (null == b) {
               b = new Board(values[0]);
               boards.put(values[0], b);
            }
            b.addHand(new Hand(values));
         }
      } catch (EOFException EOFe) {}
      in.close();
      return new ArrayList<Board>(boards.values());
   }

   static List<Pair> readPairs(InputStream is, List<Board> boards) throws IOException
   {
      CsvReader in = new CsvReader(new InputStreamReader(is));
      List<Pair> pairs = new Vector<Pair>();
		HashMap<String, Pair> pairmap = new HashMap<String, Pair>();
      try { 
         while (in.readRecord()) {
            String[] values = in.getValues();
            if (Debug.debug) Debug.print(Arrays.asList(values));
				Pair p = new Pair(values);
            pairs.add(p);
				pairmap.put(p.getNumber(), p);
         }
      } catch (EOFException EOFe) {}
      in.close();
	
		if (null != boards) {
			for (Board b: boards) {
				for (Hand h: b.getHands()) {
					if (pairmap.containsKey(h.getNS())) {
						pairmap.get(h.getNS()).addHand(h);
						pairmap.get(h.getEW()).addHand(h);
					} else {
						// probably individual
						String[] ss = h.getNS().split("\\.");
						for (String s: ss) if (pairmap.containsKey(s))
							pairmap.get(s).addHand(h);
						ss = h.getEW().split("\\.");
						for (String s: ss) if (pairmap.containsKey(s))
							pairmap.get(s).addHand(h);
					}
				}
			}
		}

      return pairs;
   }

   static void writePairs(List<Pair> pairs, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Pair p: pairs)
         out.writeRecord(p.export());
      out.close();
   }

   static void syntax()
   {
      String version = Package.getPackage("cx.ath.matthew.salliere")
                              .getImplementationVersion();
      System.out.println("Salliere Duplicate Bridge Scorer - version "+version);
      System.out.println("Usage: salliere [options] [commands] -- <boards.csv> <names.csv>");
      System.out.println("   Commands: verify score matchpoint ximp parimp total handicap localpoint results matrix boards ecats-upload ecats-export scoreteams scorecards");
      System.out.println("   Options: --help --output=[<format>:]file --title=title --orange --setsize=N --ximp --with-par --trickdata=<tricks.txt> --handicapdata=<handicap.csv> --with-handicaps --handicap-normalizer=<num> --ecats-options=<key:val,key2:val2,...> --print-ecats-options --ecats-export-dir=<dir> --mpscale=<scale> --print-mpscales --teamsize=N --teamprefix=<prefix> --original-entry=<#tables>");
      System.out.println("   Formats: txt html htmlfrag pdf csv");
   }


   static void writeBoards(List<Board> boards, OutputStream os) throws IOException
   {
      CsvWriter out = new CsvWriter(new OutputStreamWriter(os), ',');
      for (Board b: boards)
         for (Hand h: b.getHands())
            out.writeRecord(h.export());
      out.close();
   }

   private static void doC(PrintStream out, List<Board> boards, Map<String, String> options)
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
      out.print('"'+options.get("clubName")+'"'); //club name, compulsory (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("town")+'"'); //town (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("county")+'"'); //county (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("country")+'"'); //country, compulsory, spelling agreed in advance (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("name")+'"'); //name (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("phone")+'"'); //phone, compulsory (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("fax")+'"'); //fax (max 50 chars)
      out.print("\t");
      out.print('"'+options.get("email")+'"'); //email (max 50 chars)
      out.print("\t");
      out.print("\"false\""); //spare = false
      out.print("\t");
      out.print(options.get("session")); //session #, compulsory, numeric
      out.print("\t");
      out.print('"'+ECATS_PROGRAM_VERSION+'"'); // program version. Important. My program? their program? this number seems to work...
      out.print("\t");
      out.print('"'+options.get("date")+'"'); // "dd/mm/yyyy" 
      out.print("\t");
      out.print('"'+options.get("event")+'"'); // event name, text (max 50 chars)
      out.print("\r\n");
   }

   private static void doP(PrintStream out, List<Pair> pairs)
   {
      for (Pair p: pairs) {
         out.print("\t");
         out.print("0"); // spare = 0
         out.print("\t");
         out.print("0"); // spare = 0
         out.print("\t");
         out.print(p.getNumber()); // pair number (int)
         out.print("\t");
         String[] names = p.getNames();
			if (names.length > 1)
				out.print('"'+names[0]+" & "+names[1]+'"');
			else 
				out.print('"'+names[0]+'"');
         out.print("\t");
         out.print('"'+names[0]+'"');
         out.print("\t");
			if (names.length > 1)
				out.print('"'+names[1]+'"');
			else
				out.print('"'+'"');
         out.print("\t");
         out.print("\"NS\""); // for a 2 winner movement, set this to the direction they were sitting. 
         out.print("\r\n");
      }
   }
   private static void doR(PrintStream out, List<Board> boards, String session)
   {
      for (Board bd: boards) {
         for (Hand b: bd.getHands()) {
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

   public static void exportToECATS(List<Board> boards, List<Pair> pairs, Map<String,String> options, String exportdir) throws ScoreException
   {
      try {
         if (null == exportdir)
            exportdir = new File (".").getCanonicalPath();
         System.err.println(_("Exporting scores in ECATS format to ")+exportdir);

         String session = options.get("session");
   
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


   public static void uploadToECATS(List<Board> boards, List<Pair> pairs, Map<String, String> options) throws ScoreException
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
         String session = options.get("session");
         String clubName = options.get("clubName");
         String phone = options.get("phone");

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

   public static void score(List<Board> boards) throws ScoreException, ContractParseException, HandParseException
   {
      for (Board b: boards)
         for (Hand h: b.getHands())
            if (!h.isAveraged())
               h.score();
      modifiedboards = true;
   }

   public static void verify(List<Board> boardv, String setsize) throws MovementVerificationException, BoardValidationException, HandParseException
   {
      for (Board b: boardv)
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
      Board[] boards = boardv.toArray(new Board[0]);
      Set<String> pairs = new TreeSet<String>();
      for (int i = 0; i < boards.length; i++) {
         if (0 == (i % size)) {
            // reset the pairs
            pairs.clear();
            for (Hand h: boards[i].getHands())
               pairs.add(h.getNS()+" "+h.getEW());
         } else {
            // check the pairs
            for (Hand h: boards[i].getHands())
               if (!pairs.contains(h.getNS()+" "+h.getEW()))
                  throw new MovementVerificationException(
                        MessageFormat.format(_("Board {0} was played by {1} and {2}, which I was not expecting."), 
                           new Object[] { boards[i].getNumber(), h.getNS(), h.getEW() }));
         }
      }
   }

   public static void matchpoint(List<Board> boards) throws ScoreException
   {
		int played = 0;
      for (Board b: boards) {
			if (b.getHands().size() > played)
				played = b.getHands().size();
		}
      for (Board b: boards)
         b.matchPoint(played);
      modifiedboards = true;
   }

   public static void ximp(List<Board> boards) throws ScoreException
   {
      for (Board b: boards)
         b.ximp();
      modifiedboards = true;
   }

   public static void teams(List<Board> boards, TablePrinter tabular, String prefix, String teamsize) throws ScoreException
   {
      int teams = 4;
      if (null != teamsize && teamsize.length() >= 0)
         try {
            teams = Integer.parseInt(teamsize);
         } catch (NumberFormatException NFe) {
            if (Debug.debug) Debug.print(NFe);
            throw new ScoreException(teamsize+_(" isn't a number!"));
         }

      double usimps = 0;
      double themimps = 0;
      for (Board b: boards) {
         double[] res = b.sumimp(prefix, teams);
         if (res[0] > 0)
            usimps += res[0];
         else
            themimps += -res[0];
      }
      String[] headers = new String[3];
      headers[0] = _("Us IMPs");
      headers[1] = _("Them IMPs");
      headers[2] = _("Difference");
      DecimalFormat format = new DecimalFormat("0.##");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);
      StringBuffer tmp;
      String[][] matrix = new String[1][3]; 
      tmp = new StringBuffer();
      matrix[0][0] = format.format(usimps, tmp, field).toString();
      tmp = new StringBuffer();
      matrix[0][1] = format.format(themimps, tmp, field).toString();
      tmp = new StringBuffer();
      matrix[0][2] = format.format(usimps-themimps, tmp, field).toString();

      tabular.print(headers, matrix);
      tabular.gap();
   }

   public static void parimp(List<Board> boards) throws ScoreException
   {
      for (Board b: boards)
         b.parimp();
      modifiedboards = true;
   }

   public static void total(List<Pair> pairs, List<Board> boards)
   {
      for (Pair p: pairs)
         p.total(boards);
      modifiedpairs = true;
   }

   public static void matrix(List<Pair> pairv, List<Board> boardv, TablePrinter tabular, String setsize) throws MovementVerificationException
   {
      int grouping = 0;
      if (null != setsize && setsize.length() >= 0)
         try {
            grouping = Integer.parseInt(setsize);
         } catch (NumberFormatException NFe) {
            if (Debug.debug) Debug.print(NFe);
            throw new MovementVerificationException(setsize+_(" isn't a number!"));
         }

      Collections.sort(boardv, new BoardNumberComparer());
      Collections.sort(pairv, new PairNumberComparer());
      Board[] boards = boardv.toArray(new Board[0]);
      Pair[] pairs = pairv.toArray(new Pair[0]);

      DecimalFormat format = new DecimalFormat("0.##");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      String[] headers = new String[pairs.length+1];
      headers[0] = _("Board");
      for (int j = 0; j < pairs.length; j++) 
         headers[j+1] = pairs[j].getNumber()+"  ";

      if (0 == grouping) grouping = boards.length;
      String[][] matrix = new String[grouping+2][pairs.length+1]; 
      
      int start = 0;

      while (start < boards.length) {
         float[] sums = new float[pairs.length];
         for (int i = 0; i+start < boards.length && i < grouping; i++) {
            matrix[i][0] = boards[i+start].getNumber();
            for (int j = 0; j < pairs.length; j++) {
               StringBuffer tmp = new StringBuffer();
               if (boards[i+start].played(pairs[j].getNumber())) {
                  matrix[i][j+1] = format.format(boards[i+start].getMPs(pairs[j].getNumber()), tmp, field).toString();
                  sums[j] += boards[i+start].getMPs(pairs[j].getNumber());
               } else
                  matrix[i][j+1] = " ";
            }
         }
         matrix[matrix.length-2][0] = " ";
         matrix[matrix.length-1][0] = _("Total");
         for (int j = 0; j < sums.length; j++) {
            StringBuffer tmp = new StringBuffer();
            matrix[matrix.length-2][j+1] = " ";
            matrix[matrix.length-1][j+1] = format.format(sums[j], tmp, field).toString();
         }
         start += grouping;

         tabular.print(headers, matrix);
         tabular.gap();
      }
   }
   public static void boardbyboard(List<Board> boards, TablePrinter tabular, boolean ximp, boolean withpar) 
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

      for (Board b: boards) {
         Vector<String[]> lines = new Vector<String[]>();
         List<Hand> hands = b.getHands();
         Collections.sort(hands, new HandNSComparer());
         for (Hand h: hands) {
            String[] ex = h.export();
            String[] line = new String[ex.length-1];
            System.arraycopy(ex, 1, line, 0, line.length);
            lines.add(line);
         }
         if (withpar)
            tabular.header(_("Board: ")+b.getNumber()+" [ "+b.getParContract()+" by "+b.getPar().getDeclarer()+" ]");
         else
            tabular.header(_("Board: ")+b.getNumber());
         tabular.print(headers, lines.toArray(new String[0][]));
         tabular.gap();
      }
   }
   public static void scorecards(List<Pair> pairs, List<Board> boards, TablePrinter tabular, boolean ximp) 
   {
      String[] headers;
		if (0 == pairs.size()) return;
		if (pairs.get(0).getNames().length > 1) {
			headers = new String[] {
            _("Board"),
            _("Dir"),
            _("VS"),
            _("Contract"), 
            _("By"), 
            _("Tricks"),
            _("Score:"),
            "",
            ximp ? _("IMPs") 
                 : _("MPs:")}; 
			if (Debug.debug) Debug.print("Pairs");
		} else {
			headers = new String[] {
            _("Board"),
            _("Dir"),
            _("Contract"), 
            _("By"), 
            _("Tricks"),
            _("Score:"),
            "",
            ximp ? _("IMPs") 
                 : _("MPs:")}; 
			if (Debug.debug) Debug.print("Individual");
		}
      Collections.sort(pairs, new PairNumberComparer());
      Collections.sort(boards, new BoardNumberComparer());

		for (Pair p: pairs) {
			String header;
			if (p.getNames().length == 1) 
				header = _("Player ");
			else
				header = _("Pair ");
			header += p.getNumber();
			for (String n: p.getNames()) {
				header += ", "+n;
			}
			tabular.header(header);
			Vector<String[]> lines = new Vector<String[]>();
			for (Board b: boards) {
				List<Hand> hands = b.getHands();
				Collections.sort(hands, new HandNSComparer());
				for (Hand h: hands) {
					if (h.getNS().equals(p.getNumber())) {
						String[] ex = h.export();
						String[] line = new String[ex.length-1];
						line[0] = ex[0];
						line[1] = _("NS");
						line[2] = ex[2];
						line[3] = ex[3];
						line[4] = ex[4];
						line[5] = ex[5];
						line[6] = ex[6].equals("0") ? "" : ex[6];
						line[7] = ex[7].equals("0") ? "" : ex[7];
						line[8] = ex[8];
						lines.add(line);
					} else if (h.getEW().equals(p.getNumber())) {
						String[] ex = h.export();
						String[] line = new String[ex.length-1];
						line[0] = ex[0];
						line[1] = _("EW");
						line[2] = ex[1];
						line[3] = ex[3];
						line[4] = ex[4];
						line[5] = ex[5];
						line[6] = ex[7].equals("0") ? "" : ex[7];
						line[7] = ex[6].equals("0") ? "" : ex[6];
						line[8] = ex[9];
						lines.add(line);
					} else if (h.getNS().startsWith(p.getNumber()+".")) {
						String[] ex = h.export();
						String[] line = new String[ex.length-2];
						line[0] = ex[0];
						line[1] = _("N");
						line[2] = ex[3];
						line[3] = ex[4];
						line[4] = ex[5];
						line[5] = ex[7].equals("0") ? "" : ex[7];
						line[6] = ex[6].equals("0") ? "" : ex[6];
						line[7] = ex[9];
						lines.add(line);
					} else if (h.getNS().endsWith("."+p.getNumber())) {
						String[] ex = h.export();
						String[] line = new String[ex.length-2];
						line[0] = ex[0];
						line[1] = _("S");
						line[2] = ex[3];
						line[3] = ex[4];
						line[4] = ex[5];
						line[5] = ex[7].equals("0") ? "" : ex[7];
						line[6] = ex[6].equals("0") ? "" : ex[6];
						line[7] = ex[9];
						lines.add(line);
					} else if (h.getEW().startsWith(p.getNumber()+".")) {
						String[] ex = h.export();
						String[] line = new String[ex.length-2];
						line[0] = ex[0];
						line[1] = _("E");
						line[2] = ex[3];
						line[3] = ex[4];
						line[4] = ex[5];
						line[5] = ex[7].equals("0") ? "" : ex[7];
						line[6] = ex[6].equals("0") ? "" : ex[6];
						line[7] = ex[9];
						lines.add(line);
					} else if (h.getEW().endsWith("."+p.getNumber())) {
						String[] ex = h.export();
						String[] line = new String[ex.length-2];
						line[0] = ex[0];
						line[1] = _("W");
						line[2] = ex[3];
						line[3] = ex[4];
						line[4] = ex[5];
						line[5] = ex[7].equals("0") ? "" : ex[7];
						line[6] = ex[6].equals("0") ? "" : ex[6];
						line[7] = ex[9];
						lines.add(line);
					}
				}
			}
			tabular.print(headers, lines.toArray(new String[0][]));
			tabular.gap();
		}
   }


   public static Map<Pair, Double> readHandicapData(List<Pair> pairs, InputStream handicapfile) throws ScoreException
   {
      Map<String, Double> handicaps = new HashMap<String, Double>();
      Map<Pair, Double> pairhandicaps = new HashMap<Pair, Double>();
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
      for (Pair p: pairs) {
         double handicap = 0.0;
         String[] names = p.getNames();
         for (String n: names) {
            Double v = handicaps.get(n);
            if (null == v) v = 50.0;
            handicap += v;
         }
         handicap /= (double) names.length;
         pairhandicaps.put(p, handicap);
      }
      return pairhandicaps;
   }
   public static void handicap(List<Pair> pairs, Map<Pair, Double> handicaps,  double normalize) throws ScoreException
   {
      if (null == handicaps) throw new ScoreException(_("Must supply a handicap file before calculating handicapped scores"));
      for (Pair p: pairs) {
         double handicap = handicaps.get(p);
         p.setPercentage(p.getPercentage()-handicap+normalize);
      }
      modifiedpairs = true;
   }
   public static void localpoint(List<Pair> pairs, List<Board> boards, MasterPointScale scale, int originaltables) throws ScoreException
   {
      // sort pairs in order
      Collections.sort(pairs, new PairPercentageComparer());
      Pair[] ps = pairs.toArray(new Pair[0]);

		if (ps.length == 0) return;

		int npairs;
		int arity = ps[0].getNames().length;
		if (-1 == originaltables)
			npairs = ps.length;
		else
			npairs = originaltables * arity;

      // check we have enough to give LPs
		if (ps.length < scale.minPairs(arity)) throw new ScoreException(
                     MessageFormat.format(_("Must have at least {0} competitors to award local points in this event"),
                        new Object[] { new Integer(scale.minPairs(arity)) }));

      // calculate LP scale based on number of pairs
      int[] LPs = new int[ps.length];
      int awarded = scale.numberAwards(npairs, arity, boards.size());
      double top = scale.getTop(npairs, arity, boards.size());
      double rate = scale.getRate(arity);
      if (Debug.debug) Debug.print(Debug.DEBUG, "Awarding LPs to "+ps.length+" pairs, top is "+top+" rate is "+rate+" number receiving LPs: "+awarded);
		double lp = top;
      for (int i = 0; i < LPs.length && i < awarded; i++, lp -= rate)
			if (lp > scale.getMax(boards.size())) LPs[i] = scale.getMax(boards.size());
			else LPs[i] = (int) Math.ceil(lp);

      // award LPs, splitting on draws
      for (int i = 0; i < ps.length && i < awarded; i++) {
         int a = i;
         int total = LPs[i];
         while (i+1 < ps.length && ps[i].getPercentage()==ps[i+1].getPercentage())
            total += LPs[++i];
         double award = Math.ceil(total / (1.0+i-a));
         if (award < scale.getMin() && award > 0) award = scale.getMin();
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

   public static void results(List<Pair> pairs, TablePrinter tabulate, boolean orange, boolean ximp, Map<Pair, Double> handicapdata, boolean handicaps)
   {
      Vector<String[]> results = new Vector<String[]>();
      Collections.sort(pairs, new PairPercentageComparer());

      Vector<String> headerv = new Vector<String>();
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

      String[] header = headerv.toArray(new String[0]);

      if (ximp)
         for (Pair p: pairs) {
            String[] a = p.export();
            String[] b = new String[a.length-1];
            System.arraycopy(a, 0, b, 0, 4);
            System.arraycopy(a, 5, b, 4, a.length-5);
            results.add(b);
         }
      else
         for (Pair p: pairs)
            results.add(p.export());

      if (handicaps) 
         for (int i = 0; i < pairs.size(); i++) {
            Pair p = pairs.get(i);
            String[] r = results.get(i);
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

      tabulate.print(header, results.toArray(new String[0][]));
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
         Vector<String> commands = new Vector<String>();
         HashMap<String, String> options = new HashMap<String, String>();
         options.put("--output", "-");
         options.put("--help", null);
         options.put("--orange", null);
         options.put("--ximp", null);
         options.put("--title", _("Salliere Duplicate Bridge Scorer: Results"));
         options.put("--teamsize", null);
         options.put("--teamprefix", "");
         options.put("--setsize", null);
         options.put("--with-par", null);
         options.put("--with-handicaps", null);
         options.put("--trickdata", null);
         options.put("--handicapdata", null);
         options.put("--handicap-normalizer", "0.0");
         options.put("--ecats-options", null);
         options.put("--ecats-export-dir", null);
         options.put("--print-ecats-options", null);
         options.put("--print-mpscales", null);
         options.put("--mpscale", null);
         options.put("--original-entry", "-1");
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

         if (null != options.get("--help")) {
            syntax();
            System.exit(1);
         }
         if (null != options.get("--print-ecats-options")) {
            ECatsOptionsMap.printOptions();
            System.exit(1);
         }
         if (null != options.get("--print-mpscales")) {
            MasterPointScale.printScales();
            System.exit(1);
         }

         if (args.length < (i+3)) {
            System.out.println(_("You must specify boards.csv and names.csv"));
            syntax();
            System.exit(1);
         }

         List<Board> boards;
         List<Pair> pairs;

         boards = readBoards(new FileInputStream(args[i+1]));
         pairs = readPairs(new FileInputStream(args[i+2]), boards);

         if (null != options.get("--trickdata")) {
            readTrickData(boards, new FileInputStream(options.get("--trickdata")));
         }

         Map<Pair, Double> handicapdata = null;

         if (null != options.get("--handicapdata")) {
            handicapdata = readHandicapData(pairs, new FileInputStream(options.get("--handicapdata")));
         }

         Map<String, String> ecatsoptions = new ECatsOptionsMap(options.get("--ecats-options"));

         TablePrinter tabular = null;

         String[] format = options.get("--output").split(":");
         PrintStream out;
         if ("-".equals(format[format.length-1]))
            out = System.out;
         else 
            out = new PrintStream(new FileOutputStream(format[format.length-1]));

         if (format.length == 1)
            tabular = new AsciiTablePrinter(out);
         else if ("txt".equals(format[0].toLowerCase()))
            tabular = new AsciiTablePrinter(out);
         else if ("csv".equals(format[0].toLowerCase()))
            tabular = new CSVTablePrinter(out);
         else if ("html".equals(format[0].toLowerCase()))
            tabular = new HTMLTablePrinter(options.get("--title"), out);
         else if ("htmlfrag".equals(format[0].toLowerCase()))
            tabular = new HTMLFragTablePrinter(options.get("--title"), out);
         else if ("pdf".equals(format[0].toLowerCase()))
            tabular = new PDFTablePrinter(options.get("--title"), out);
         else {
            System.out.println(_("Unknown format: ")+format[0]);
            syntax();
            System.exit(1);
         }
         
         tabular.init();
         tabular.header(options.get("--title"));

         for (String command: commands) {
            if ("score".equals(command)) score(boards);
            else if ("verify".equals(command)) verify(boards, options.get("--setsize"));
            else if ("matchpoint".equals(command)) matchpoint(boards);
            else if ("total".equals(command)) total(pairs, boards);
            else if ("results".equals(command)) results(pairs, tabular, null != options.get("--orange"), null != options.get("--ximp"), handicapdata, null != options.get("--with-handicaps"));
            else if ("matrix".equals(command)) matrix(pairs, boards, tabular, options.get("--setsize"));
            else if ("boards".equals(command)) boardbyboard(boards, tabular, null != options.get("--ximp"), null != options.get("--with-par"));
            else if ("scorecards".equals(command)) scorecards(pairs, boards, tabular, null != options.get("--ximp"));
            else if ("localpoint".equals(command)) localpoint(pairs, boards, MasterPointScale.getScale(options.get("--mpscale")), Integer.parseInt(options.get("--original-entry")));
            else if ("handicap".equals(command)) handicap(pairs, handicapdata, Double.parseDouble(options.get("--handicap-normalizer")));
            else if ("ximp".equals(command)) ximp(boards);
            else if ("parimp".equals(command)) parimp(boards);
            else if ("ecats-upload".equals(command)) uploadToECATS(boards, pairs, ecatsoptions);
            else if ("ecats-export".equals(command)) exportToECATS(boards, pairs, ecatsoptions, options.get("--ecats-export-dir"));
            else if ("scoreteams".equals(command)) teams(boards, tabular, options.get("--teamprefix"), options.get("--teamsize"));
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
            Debug.print(Debug.ERR, e);
         }
         System.out.println(_("Salliere failed to compute results: ")+e.getMessage());
         System.exit(1);
      }
   }
}
