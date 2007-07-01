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
