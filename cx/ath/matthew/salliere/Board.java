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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.Vector;

import java.text.MessageFormat;

public class Board
{
   public static final int NORTH = 0;
   public static final int SOUTH = 1;
   public static final int EAST = 2;
   public static final int WEST = 3;
   public static final int CLUBS = 0;
   public static final int DIAMONDS = 1;
   public static final int HEARTS = 2;
   public static final int SPADES = 3;
   public static final int NOTRUMPS = 4;
   class HandScoreNSComparer implements Comparator<Hand> {
      public int compare(Hand obj1, Hand obj2)
      {
         double score1 = ( 0 == obj1.getNSScore() ) ? - obj1.getEWScore() : obj1.getNSScore();
         double score2 = ( 0 == obj2.getNSScore() ) ? - obj2.getEWScore() : obj2.getNSScore();
         return (int) (score1 - score2);
      }
   }
   class HandScoreEWComparer implements Comparator<Hand> {
      public int compare(Hand obj1, Hand obj2)
      {
         double score1 = ( 0 == obj1.getEWScore() ) ? - obj1.getNSScore() : obj1.getEWScore();
         double score2 = ( 0 == obj2.getEWScore() ) ? - obj2.getNSScore() : obj2.getEWScore();
         return (int) (score1 - score2);
      }
   }
   Vector<Hand> hands;
   byte[] tricks;
   String number = "";
   Contract par;
   public Board() 
   {
      this.hands = new Vector<Hand>();
   }
   public Board(String number)
   {
      this.number = number;
      this.hands = new Vector<Hand>();
   }
   public void matchPoint() throws ScoreException
   {
      // order by (NS score) and (EW score)
      Collections.sort(hands, new HandScoreNSComparer());
      Hand[] nshs = hands.toArray(new Hand[0]);
      Collections.sort(hands, new HandScoreEWComparer());
      Hand[] ewhs = hands.toArray(new Hand[0]);

      // create score-frequency table
      Map<Double, Double> ewfrequencies = new HashMap<Double, Double>();
      Map<Double, Double> nsfrequencies = new HashMap<Double, Double>();
      int avcount = 0;
      for (int i = 0; i < nshs.length; i++) {
         if (Debug.debug) Debug.print(nshs[i]);
         double score = (0 == nshs[i].getNSScore()) ? - nshs[i].getEWScore() : nshs[i].getNSScore();
         if (nshs[i].isAveraged()) {
            if (Debug.debug) Debug.print("Average on "+nshs[i]);
            avcount++;
         } else {
            Double freq = nsfrequencies.get(score);
            if (null != freq)
               nsfrequencies.put(score,freq+1.0);
            else
               nsfrequencies.put(score,1.0);
         }
      }

      for (int i = 0; i < ewhs.length; i++) {
         if (Debug.debug) Debug.print(ewhs[i]);
         double score = (0 == ewhs[i].getEWScore()) ? - ewhs[i].getNSScore() : ewhs[i].getEWScore();
         if (ewhs[i].isAveraged())
            ;
         else {
            Double freq = ewfrequencies.get(score);
            if (null != freq)
               ewfrequencies.put(score,freq+1.0);
            else
               ewfrequencies.put(score,1.0);
         }
      }

      // increase frequencies for averages
      double increment = 1.0 + ( (double) avcount / (double) ( nshs.length - avcount ) );
      if (Debug.debug) Debug.print("Got "+avcount+" averages, increment = "+increment);
      for (Double f: ewfrequencies.keySet())
         ewfrequencies.put(f, ewfrequencies.get(f)*increment);
      for (Double f: nsfrequencies.keySet())
         nsfrequencies.put(f, nsfrequencies.get(f)*increment);

      double avp = 0.60 * getTop();
      double ave = 0.50 * getTop();
      double avm = 0.40 * getTop();

      for (int i = 0; i < nshs.length; i++) {
         double nsmps = 0.0;
         double score = (0 == nshs[i].getNSScore()) ? - nshs[i].getEWScore() : nshs[i].getNSScore();
			if (Debug.debug) Debug.print("N/S Average: "+nshs[i].getNSAverage());
            switch (nshs[i].getNSAverage()) {
               case Hand.AVERAGE: nsmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: nsmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: nsmps = avm;
                                        break;
               default:
            nsmps = nsfrequencies.get(score) - 1;
            for (Double f: nsfrequencies.keySet())
               if (f < score) nsmps += ( 2 * nsfrequencies.get(f) );

         }

         if (!nshs[i].hasForcedNSMP()) {
            if (nshs[i].getNSMP() == 0 || (
                     nshs[i].getNSMP() - nsmps <= 0.005 &&
                     nshs[i].getNSMP() - nsmps > -0.005))
               nshs[i].setNSMP(nsmps);
            else
               throw new ScoreException(MessageFormat.format(_("Calculated {0} MPs for NS, but hand says: {1}"), new Object[] { nsmps, nshs[i]}));
            if (Debug.debug) Debug.print("Setting NSMP on board "+number+" to "+nsmps);
         }
      }

      for (int i = 0; i < ewhs.length; i++) {
         double ewmps = 0.0;
         double score = (0 == ewhs[i].getEWScore()) ? - ewhs[i].getNSScore() : ewhs[i].getEWScore();
			if (Debug.debug) Debug.print("E/W Average: "+ewhs[i].getNSAverage());
            switch (ewhs[i].getEWAverage()) {
               case Hand.AVERAGE: ewmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: ewmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: ewmps = avm;
                                        break;
               default:
            ewmps = ewfrequencies.get(score) - 1;
            for (Double f: ewfrequencies.keySet())
               if (f < score) ewmps += ( 2 * ewfrequencies.get(f) );

         }

         if (!ewhs[i].hasForcedEWMP()) {
            if (ewhs[i].getEWMP() == 0 || (
                ewhs[i].getEWMP() - ewmps <= 0.005 &&
                ewhs[i].getEWMP() - ewmps > -0.005))
               ewhs[i].setEWMP(ewmps);
            else
               throw new ScoreException(MessageFormat.format(_("Calculated {0} MPs for EW, but hand says: {1}"), new Object[] { ewmps, ewhs[i]}));
            if (Debug.debug) Debug.print("Setting EWMP on board "+number+" to "+ewmps);
         }
      }
   }
   public double imp(double score)
   {
      double abs = Math.abs(score);
      boolean neg = score < 0;
      if (abs <= 10) return 0;
      else if (abs <= 40) return neg ? -1 : 1;
      else if (abs <= 80) return neg ? -2 : 2;
      else if (abs <= 120) return neg ? -3 : 3;
      else if (abs <= 160) return neg ? -4 : 4;
      else if (abs <= 210) return neg ? -5 : 5;
      else if (abs <= 260) return neg ? -6 : 6;
      else if (abs <= 310) return neg ? -7 : 7;
      else if (abs <= 360) return neg ? -8 : 8;
      else if (abs <= 420) return neg ? -9 : 9;
      else if (abs <= 490) return neg ? -10 : 10;
      else if (abs <= 590) return neg ? -11 : 11;
      else if (abs <= 740) return neg ? -12 : 12;
      else if (abs <= 890) return neg ? -13 : 13;
      else if (abs <= 1090) return neg ? -14 : 14;
      else if (abs <= 1290) return neg ? -15 : 15;
      else if (abs <= 1490) return neg ? -16 : 16;
      else if (abs <= 1740) return neg ? -17 : 17;
      else if (abs <= 1990) return neg ? -18 : 18;
      else if (abs <= 2240) return neg ? -19 : 19;
      else if (abs <= 2490) return neg ? -20 : 20;
      else if (abs <= 2990) return neg ? -21 : 21;
      else if (abs <= 3490) return neg ? -22 : 22;
      else if (abs <= 3990) return neg ? -23 : 23;
      else return neg ? -24 : 24;
   }
   public void parimp() throws ScoreException
   {
      double avs = 0;
      for (Hand h1: hands) {
         double nsimps = 0;
         double ewimps = 0;
         if (h1.isAveraged()) {
            // +-2 or 0
            switch (h1.getNSAverage()) {
               case Hand.AVERAGE:
                  nsimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  nsimps = 2;
                  break;
               case Hand.AVERAGE_MINUS:
                  nsimps = -2;
                  break;
            }
            switch (h1.getEWAverage()) {
               case Hand.AVERAGE:
                  ewimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  ewimps = 2;
                  break;
               case Hand.AVERAGE_MINUS:
                  ewimps = -2;
                  break;
            }
            avs++;
         } else {
            // for each score, IMP against the par score
            double s1 = (0 == h1.getNSScore()) ? - h1.getEWScore() : h1.getNSScore();
            double s2 = getParScore();
            double diff = s1-s2;
            nsimps = imp(diff);
            ewimps = imp(-diff);
            if (Debug.debug) Debug.print(Debug.VERBOSE, "score="+s1+", par="+s2+", diff="+diff+", nsimps="+nsimps+", ewimps="+ewimps);
         }
         h1.setNSMP(nsimps);
         h1.setEWMP(ewimps);
         if (Debug.debug) Debug.print(Debug.DEBUG, h1.getNumber()+" NSMP="+nsimps+" EWMP="+ewimps);
      }
   }
   public double[] sumimp(String prefix, int teams) throws ScoreException
   {
      double usimps = 0;
      double themimps = 0;
      double usscore = 0;
      double themscore = 0;
      int AVNUM = 4 == teams ? 3 : 5;
      for (Hand h1: hands) {
         boolean usns = false;
         if (h1.getNS().startsWith(prefix)) usns = true;
         if (h1.isAveraged()) {
            // +-2 or 0
            switch (usns ? h1.getNSAverage() : h1.getEWAverage()) {
               case Hand.AVERAGE:
                  usimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  usimps = AVNUM;
                  break;
               case Hand.AVERAGE_MINUS:
                  usimps = -AVNUM;
                  break;
            }
            switch (usns ? h1.getEWAverage() : h1.getNSAverage()) {
               case Hand.AVERAGE:
                  themimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  themimps = AVNUM;
                  break;
               case Hand.AVERAGE_MINUS:
                  themimps = -AVNUM;
                  break;
            }
            return new double[] { usimps, themimps };
         } else {
            usscore += usns ? h1.getNSScore() : h1.getEWScore();
            themscore += usns ? h1.getEWScore() : h1.getNSScore();
         }
         double diff = usscore-themscore;
         usimps = imp(diff);
         themimps = imp(-diff);
         if (Debug.debug) Debug.print(Debug.DEBUG, h1.getNumber()+" NSMP="+usimps+" EWMP="+themimps);
      }
      return new double[] { usimps, themimps };
   }
   public void ximp() throws ScoreException
   {
      double avs = 0;
      for (Hand h1: hands) {
         double nsimps = 0;
         double ewimps = 0;
         if (h1.isAveraged()) {
            // +-2 or 0
            switch (h1.getNSAverage()) {
               case Hand.AVERAGE:
                  nsimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  nsimps = 2;
                  break;
               case Hand.AVERAGE_MINUS:
                  nsimps = -2;
                  break;
            }
            switch (h1.getEWAverage()) {
               case Hand.AVERAGE:
                  ewimps = 0;
                  break;
               case Hand.AVERAGE_PLUS:
                  ewimps = 2;
                  break;
               case Hand.AVERAGE_MINUS:
                  ewimps = -2;
                  break;
            }
            avs++;
         } else {
            // for each score, IMP against the other scores, sum, 
            // then divide by the number of scores. 
            for (Hand h2: hands) {
               if (Debug.debug) Debug.print(Debug.DEBUG, h1+" imp "+h2);
               if (!h2.isAveraged()) {
                   double s1 = (0 == h1.getNSScore()) ? - h1.getEWScore() : h1.getNSScore();
                   double s2 = (0 == h2.getNSScore()) ? - h2.getEWScore() : h2.getNSScore();
                   double diff = s1-s2;
                   nsimps += imp(diff);
                   ewimps += imp(-diff);
               if (Debug.debug) Debug.print(Debug.VERBOSE, "s1="+s1+", s2="+s2+", diff="+diff+", nsimps="+nsimps+", ewimps="+ewimps);
               }
            }
            double s = hands.size();
            nsimps = nsimps * (s / ((s-avs) * (s - 1.0)));
            ewimps = ewimps * (s / ((s-avs) * (s - 1.0)));
         }
         h1.setNSMP(nsimps);
         h1.setEWMP(ewimps);
         if (Debug.debug) Debug.print(Debug.DEBUG, h1.getNumber()+" NSMP="+nsimps+" EWMP="+ewimps);
      }
   }
   public void importTricks(String s)
   {
      String[] ss = s.split(",");
      tricks = new byte[ss.length];
      for (int i = 0; i < ss.length; i++)
         tricks[i] = Byte.parseByte(ss[i]);
      if (Debug.debug) {
         Vector<Byte> v = new Vector<Byte>();
         for (int i = 0; i < tricks.length; i++) 
            v.add(tricks[i]);
         Debug.print(Debug.INFO, "Read avaiable tricks for board "+number+": "+v);
      }
   }
   private Contract getScore(int v, int d, int p, int vuln) throws ContractParseException, NoContractException
   {
      if (null == tricks) return null;
      char decl = ' ';
      switch (p) {
         case NORTH: decl = 'n'; break;
         case SOUTH: decl = 's'; break;
         case EAST: decl = 'e'; break;
         case WEST: decl = 'w'; break;
      }
      String contr = ""+v;
      switch (d) {
         case CLUBS: contr += "c"; break;
         case DIAMONDS: contr += "d"; break;
         case HEARTS: contr += "h"; break;
         case SPADES: contr += "s"; break;
         case NOTRUMPS: contr += "n"; break;
      }
      int tr = tricks[(4-d)+5*p];
      if (tr < 6+v) contr += 'x';
      if (Debug.debug) Debug.print(Debug.VERBOSE, "getScore("+v+", "+d+", "+p+", "+vuln+") tricks = "+tr+" decl = "+decl+" contr = "+contr);
      return new Contract(contr, decl, vuln, tr);
   }

	@SuppressWarnings("fallthrough")
   private int vuln()
   {
      String[] n = number.split(":");
      String[] v = n[n.length-1].split(";");
      int num = 0;
      try {
         num = Integer.parseInt(v[0]);
      } catch (NumberFormatException NFe) { 
         if (Debug.debug) Debug.print(NFe); 
      }
      int vul = Contract.NONE;
      if (v.length == 1) {
         switch (num%16) {
            // ns
            case 4:
            case 7:
            case 10:
            case 13:
               vul |= Contract.EAST;
            case 2:
            case 5:
            case 12:
            case 15:
               vul |= Contract.NORTH;
               break;
               // ew
            case 3:
            case 6:
            case 9:
            case 0:
               vul |= Contract.EAST;
               break;
         }
      } else {
         // split options
         String[] opts = v[1].split(",");
         for (String opt: opts) {
            String[] keyval = opt.split("=");

            // manual vulnerability
            if (keyval[0].toLowerCase().equals("vul") && 2 == keyval.length) {
               if (keyval[1].toLowerCase().equals("ew"))
                  vul = Contract.EAST;
               else if (keyval[1].toLowerCase().equals("ns"))
                  vul = Contract.NORTH;
               else if (keyval[1].toLowerCase().equals("all"))
                  vul = Contract.NORTH | Contract.EAST;
            }

         }
      }
      return vul;
   }
   /**
    * @return new int[] { value, denom, declarer, nsscore }
    */
   private Contract findPar() throws ContractParseException, NoContractException
   {
      if (null == tricks) return null;
      int denom = 0;
      int value = 1;
      int savedenom = -2;
      int savevalue = -2;
      int declarer = -1;
      int score = 0;
      int savescore = -1;
      Contract par = new Contract("P.O.", ' ', Contract.NONE, 0);

      int vuln = vuln();

      while (denom != savedenom || value != savevalue || score != savescore) {
         savevalue = value;
         savedenom = denom;
         savescore = score;
         for (int p = NORTH; p <= WEST; p++)
            PLAYER:
            for (int v = value; v <=7; v++)
               for (int d = (v == value) ? denom : CLUBS; d <= NOTRUMPS; d++) {
                  Contract c = getScore(v, d, p, vuln);
                  int newscore = (int) (c.getNSScore() - c.getEWScore());
                  if (Debug.debug) Debug.print(Debug.DEBUG, "Testing "+c.getContract()+" scores "+newscore+". Current score "+score);
                  if (((newscore > score) && 
                       (p == NORTH || p == SOUTH)) ||
                      ((newscore < score) && 
                       (p == WEST || p == EAST)))
                  {
                     par = c;
                     denom = d;
                     value = v;
                     declarer = p;
                     score = newscore;
                     break PLAYER;
                  }
               }
         if (Debug.debug) Debug.print(Debug.VERBOSE, "denom="+denom+" savedenom="+savedenom+" value="+value+" savevalue="+savevalue);
      }
      return par;
   }
   public String getParContract() 
   {
      try {
         if (null == par) par = findPar();
      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
      }
      return par.getContract();
   }
   public Contract getPar()
   {
      try {
         if (null == par) par = findPar();
      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
      }
      return par;
   }
   public int getParScore() 
   { 
      try { 
         if (null == par) par = findPar();
      } catch (Exception e) {
         if (Debug.debug) Debug.print(e);
      }
      return (int) (par.getNSScore()-par.getEWScore()); 
   }
   public byte[] getTricks() { return tricks; }
   public void setTricks(byte[] tricks) { this.tricks = tricks; }
   public String getNumber() { return number; }
   public void addHand(Hand h) throws BoardValidationException
   {
      if (Debug.debug)
         Debug.print("Adding hand to board: "+number+"/"+hands.size()+" "+h);
      if (h.getNumber().length() == 0)
         h.setNumber(number);
      else if (!h.getNumber().equals(number)) 
         throw new BoardValidationException(
                     MessageFormat.format(
                        _("This Hand is number {0} but this is board {1}."),
                        new Object[] { h.getNumber(), number }));
      hands.add(h); 
   }
   public List<Hand> getHands() { return hands; }
   public double getTop()
   {
      return 2*(hands.size()-1);
   }
   public double getMPs(String number)
   {
      for (Hand h: hands)
         if (h.getNS().equals(number)) 
            return h.getNSMP();
         else if (h.getEW().equals(number))
            return h.getEWMP();
         else if (h.getEW().startsWith(number+"."))
            return h.getEWMP();
         else if (h.getEW().endsWith("."+number))
            return h.getEWMP();
         else if (h.getNS().startsWith(number+"."))
            return h.getNSMP();
         else if (h.getNS().endsWith("."+number))
            return h.getNSMP();
      return 0;
   }
   public boolean containsAverage()
   {
      for (Hand h: hands)
         if (h.isAveraged()) return true;
      return false;
   }
   public boolean played(String number)
   {
      for (Hand h: hands)
         if (h.getNS().equals(number)) 
            return true;
         else if (h.getEW().equals(number))
            return true;
         else if (h.getEW().startsWith(number+"."))
            return true;
         else if (h.getEW().endsWith("."+number))
            return true;
         else if (h.getNS().startsWith(number+"."))
            return true;
         else if (h.getNS().endsWith("."+number))
            return true;
      return false;
   }
   public void validate() throws BoardValidationException, HandParseException
   {
      // conditions:
      // no pair plays the board twice
      Set<String> seen = new TreeSet<String>();
      for (Hand h: hands) {
         h.check();
         if (seen.contains(h.getNS()))
            throw new BoardValidationException(MessageFormat.format(_("Board {0} has been played by pair {1} twice."), new Object[] {number, h.getNS()}));
         else if (seen.contains(h.getEW()))
            throw new BoardValidationException(MessageFormat.format(_("Board {0} has been played by pair {1} twice."), new Object[] {number, h.getEW()}));
         else {
            seen.add(h.getNS());
            seen.add(h.getEW());
         }
      }
   }
   public void setNumber(String number) 
   { 
      this.number = number; 
      for (Hand h: hands)
         h.setNumber(number);
   }
   public String toString() 
   { 
      return "Board number "+number+", played "+hands.size()+" times.";
   }
}
