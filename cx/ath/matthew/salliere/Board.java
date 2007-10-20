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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.Vector;

public class Board
{
   class HandScoreNSComparer implements Comparator {
      public int compare(Object obj1, Object obj2)
      {
         double score1 = ( 0 == ((Hand) obj1).getNSScore() ) ? - ((Hand) obj1).getEWScore() : ((Hand) obj1).getNSScore();
         double score2 = ( 0 == ((Hand) obj2).getNSScore() ) ? - ((Hand) obj2).getEWScore() : ((Hand) obj2).getNSScore();
         return (int) (score1 - score2);
      }
   }
   class HandScoreEWComparer implements Comparator {
      public int compare(Object obj1, Object obj2)
      {
         double score1 = ( 0 == ((Hand) obj1).getEWScore() ) ? - ((Hand) obj1).getNSScore() : ((Hand) obj1).getEWScore();
         double score2 = ( 0 == ((Hand) obj2).getEWScore() ) ? - ((Hand) obj2).getNSScore() : ((Hand) obj2).getEWScore();
         return (int) (score1 - score2);
      }
   }
   Vector/*<Hand>*/ hands;
   String number = "";
   public Board() 
   {
      this.hands = new Vector();
   }
   public Board(String number)
   {
      this.number = number;
      this.hands = new Vector/*<Hand>*/();
   }
   public void matchPoint() throws ScoreException
   {
      // order by (NS score) and (EW score)
      Collections.sort(hands, new HandScoreNSComparer());
      Hand[] nshs = (Hand[]) hands.toArray(new Hand[0]);
      Collections.sort(hands, new HandScoreEWComparer());
      Hand[] ewhs = (Hand[]) hands.toArray(new Hand[0]);

      // create score-frequency table
      Map ewfrequencies = new HashMap();
      Map nsfrequencies = new HashMap();
      int avcount = 0;
      for (int i = 0; i < nshs.length; i++) {
         if (Debug.debug) Debug.print(nshs[i]);
         double score = (0 == nshs[i].getNSScore()) ? - nshs[i].getEWScore() : nshs[i].getNSScore();
         if (nshs[i].isAveraged()) {
            if (Debug.debug) Debug.print("Average on "+nshs[i]);
            avcount++;
         } else {
            Double freq = (Double) nsfrequencies.get(score);
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
            Double freq = (Double) ewfrequencies.get(score);
            if (null != freq)
               ewfrequencies.put(score,freq+1.0);
            else
               ewfrequencies.put(score,1.0);
         }
      }

      // increase frequencies for averages
      double increment = 1.0 + ( (double) avcount / (double) ( nshs.length - avcount ) );
      if (Debug.debug) Debug.print("Got "+avcount+" averages, increment = "+increment);
      for (Double f: (Double[]) ewfrequencies.keySet().toArray(new Double[0]))
         ewfrequencies.put(f, ((Double) ewfrequencies.get(f))*increment);
      for (Double f: (Double[]) nsfrequencies.keySet().toArray(new Double[0]))
         nsfrequencies.put(f, ((Double) nsfrequencies.get(f))*increment);

      double avp = 0.60 * getTop();
      double ave = 0.50 * getTop();
      double avm = 0.40 * getTop();

      for (int i = 0; i < nshs.length; i++) {
         double nsmps = 0.0;
         double score = (0 == nshs[i].getNSScore()) ? - nshs[i].getEWScore() : nshs[i].getNSScore();
            switch (nshs[i].getNSAverage()) {
               case Hand.AVERAGE: nsmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: nsmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: nsmps = avm;
                                        break;
               default:
            nsmps = ((Double) nsfrequencies.get(score)) - 1;
            for (Double f: (Double[]) nsfrequencies.keySet().toArray(new Double[0]))
               if (f < score) nsmps += ( 2 * ((Double) nsfrequencies.get(f)) );

         }

         if (!nshs[i].hasForcedNSMP()) {
            if (nshs[i].getNSMP() == 0 || (
                     nshs[i].getNSMP() - nsmps <= 0.005 &&
                     nshs[i].getNSMP() - nsmps > -0.005))
               nshs[i].setNSMP(nsmps);
            else
               throw new ScoreException("Calculated "+nsmps+" MPs for NS, but hand says: "+nshs[i]);
            if (Debug.debug) Debug.print("Setting NSMP on board "+number+" to "+nsmps);
         }
      }

      for (int i = 0; i < ewhs.length; i++) {
         double ewmps = 0.0;
         double score = (0 == ewhs[i].getEWScore()) ? - ewhs[i].getNSScore() : ewhs[i].getEWScore();
            switch (ewhs[i].getEWAverage()) {
               case Hand.AVERAGE: ewmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: ewmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: ewmps = avm;
                                        break;
               default:
            ewmps = ((Double) ewfrequencies.get(score)) - 1;
            for (Double f: (Double[]) ewfrequencies.keySet().toArray(new Double[0]))
               if (f < score) ewmps += ( 2 * ((Double) ewfrequencies.get(f)) );

         }

         if (!ewhs[i].hasForcedEWMP()) {
            if (ewhs[i].getEWMP() == 0 || (
                ewhs[i].getEWMP() - ewmps <= 0.005 &&
                ewhs[i].getEWMP() - ewmps > -0.005))
               ewhs[i].setEWMP(ewmps);
            else
               throw new ScoreException("Calculated "+ewmps+" MPs for EW, but hand says: "+ewhs[i]);
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
   public void ximp() throws ScoreException
   {
      double avs = 0;
      for (Hand h1: (Hand[]) hands.toArray(new Hand[0])) {
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
            for (Hand h2: (Hand[]) hands.toArray(new Hand[0])) {
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
   public String getNumber() { return number; }
   public void addHand(Hand h) throws BoardValidationException
   {
      if (Debug.debug)
         Debug.print("Adding hand to board: "+number+"/"+hands.size()+" "+h);
      if (h.getNumber().length() == 0)
         h.setNumber(number);
      else if (!h.getNumber().equals(number)) throw new BoardValidationException("This Hand is number "+h.getNumber()+" but this is board "+number);
      hands.add(h); 
   }
   public List/*<Hand>*/ getHands() { return hands; }
   public double getTop()
   {
      return 2*(hands.size()-1);
   }
   public double getMPs(String number)
   {
      for (Hand h: (Hand[]) hands.toArray(new Hand[0]))
         if (h.getNS().equals(number)) 
            return h.getNSMP();
         else if (h.getEW().equals(number))
            return h.getEWMP();
      return 0;
   }
   public boolean containsAverage()
   {
      for (Hand h: (Hand[]) hands.toArray(new Hand[0]))
         if (h.isAveraged()) return true;
      return false;
   }
   public boolean played(String number)
   {
      for (Hand h: (Hand[]) hands.toArray(new Hand[0]))
         if (h.getNS().equals(number)) 
            return true;
         else if (h.getEW().equals(number))
            return true;
      return false;
   }
   public void validate() throws BoardValidationException, HandParseException
   {
      // conditions:
      // no pair plays the board twice
      Set seen = new TreeSet();
      for (Hand h: (Hand[]) hands.toArray(new Hand[0])) {
         h.check();
         if (seen.contains(h.getNS()))
            throw new BoardValidationException("Board "+number+" has been played by pair "+h.getNS()+" twice.");
         else if (seen.contains(h.getEW()))
            throw new BoardValidationException("Board "+number+" has been played by pair "+h.getEW()+" twice.");
         else {
            seen.add(h.getNS());
            seen.add(h.getEW());
         }
      }
   }
   public void setNumber(String number) 
   { 
      this.number = number; 
      for (Hand h: (Hand[]) hands.toArray(new Hand[0]))
         h.setNumber(number);
   }
   public String toString() 
   { 
      return "Board number "+number+", played "+hands.size()+" times.";
   }
}
