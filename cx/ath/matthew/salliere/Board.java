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
   class HandScoreComparer implements Comparator {
      public int compare(Object obj1, Object obj2)
      {
         return (int) (((Hand) obj2).getNSScore() - ((Hand) obj2).getEWScore() + 
                       ((Hand) obj1).getEWScore() - ((Hand) obj1).getNSScore());
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
   public void matchPointA() throws ScoreException
   {
      // order by (NS score) - (EW score)
      Collections.sort(hands, new HandScoreComparer());
      Hand[] hs = (Hand[]) hands.toArray(new Hand[0]);

      // create score-frequency table
      Map ewfrequencies = new HashMap();
      Map nsfrequencies = new HashMap();
      int avcount = 0;
      for (int i = 0; i < hs.length; i++) {
         if (Debug.debug) Debug.print(hs[i]);
         if (hs[i].isAveraged())
            avcount++;
         else {
            Double freq = (Double) nsfrequencies.get(hs[i].getNSScore());
            if (null != freq)
               nsfrequencies.put(hs[i].getNSScore(),freq+1.0);
            else
               nsfrequencies.put(hs[i].getNSScore(),1.0);

            freq = (Double) ewfrequencies.get(hs[i].getEWScore());
            if (null != freq)
               ewfrequencies.put(hs[i].getEWScore(),freq+1.0);
            else
               ewfrequencies.put(hs[i].getEWScore(),1.0);
         }
      }

      // increase frequencies for averages
      double increment = 1.0 + ( (double) avcount / (double) ( hs.length - avcount ) );
      for (Double f: (Double[]) ewfrequencies.keySet().toArray(new Double[0]))
         ewfrequencies.put(f, ((Double) ewfrequencies.get(f))*increment);
      for (Double f: (Double[]) nsfrequencies.keySet().toArray(new Double[0]))
         nsfrequencies.put(f, ((Double) nsfrequencies.get(f))*increment);

      double avp = 0.60 * getTop();
      double ave = 0.50 * getTop();
      double avm = 0.40 * getTop();

      for (int i = 0; i < hs.length; i++) {
         double ewmps = 0.0;
         double nsmps = 0.0;
         if (hs[i].isAveraged()) {
            switch (hs[i].getEWAverage()) {
               case Hand.AVERAGE: ewmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: ewmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: ewmps = avm;
                                        break;
            }
            switch (hs[i].getNSAverage()) {
               case Hand.AVERAGE: nsmps = ave;
                                  break;
               case Hand.AVERAGE_PLUS: nsmps = avp;
                                       break;
               case Hand.AVERAGE_MINUS: nsmps = avm;
                                        break;
            }
         } else {
            nsmps = ((Double) nsfrequencies.get(hs[i].getNSScore())) - 1;
            for (Double f: (Double[]) nsfrequencies.keySet().toArray(new Double[0]))
               if (f < hs[i].getNSScore()) nsmps += ( 2 * ((Double) nsfrequencies.get(f)) );

            ewmps = ((Double) ewfrequencies.get(hs[i].getEWScore())) - 1;
            for (Double f: (Double[]) ewfrequencies.keySet().toArray(new Double[0]))
               if (f < hs[i].getEWScore()) ewmps += ( 2 * ((Double) ewfrequencies.get(f)) );
         }

         if (hs[i].getNSMP() != nsmps && hs[i].getNSMP() > 0) throw new ScoreException("Calculated "+nsmps+" MPs for NS, but hand says: "+hs[i]);
         hs[i].setNSMP(nsmps);
         if (Debug.debug) Debug.print("Setting NSMP on board "+number+" to "+nsmps);

         if (hs[i].getEWMP() != ewmps && hs[i].getEWMP() > 0) throw new ScoreException("Calculated "+ewmps+" MPs for EW, but hand says: "+hs[i]);
         hs[i].setEWMP(ewmps);
         if (Debug.debug) Debug.print("Setting EWMP on board "+number+" to "+ewmps);
      }
   }

   public void matchPoint() throws ScoreException
   {
      if (containsAverage()) {
         if (Debug.debug) Debug.print("Ignoring board "+number+" because of average");
         return;
      }
      Collections.sort(hands, new HandScoreComparer());
      Hand[] hs = (Hand[]) hands.toArray(new Hand[0]);
      for (int i = 0; i < hs.length; i++) {
         if (Debug.debug) Debug.print(hs[i]);
         double mps = 0;
         int j;
         for (j = i-1; j >=0 && 
                           hs[i].getNSScore() == hs[j].getNSScore() && 
                           hs[i].getEWScore() == hs[j].getEWScore(); j--) mps++;
         for (j = i+1; j < hs.length && 
                           hs[i].getNSScore() == hs[j].getNSScore() && 
                           hs[i].getEWScore() == hs[j].getEWScore(); j++) mps++;
         mps += 2* (hs.length-j);
         if (hs[i].getNSMP() != mps && hs[i].getNSMP() > 0) throw new ScoreException("Calculated "+mps+" MPs for NS, but hand says: "+hs[i]);
         hs[i].setNSMP(mps);
         if (Debug.debug) Debug.print("Setting NSMP on board "+number+" to "+mps);
         mps = getTop()-mps;
         if (hs[i].getEWMP() != mps && hs[i].getEWMP() > 0) throw new ScoreException("Calculated "+mps+" MPs for EW, but hand says: "+hs[i]);
         hs[i].setEWMP(mps);
         if (Debug.debug) Debug.print("Setting EWMP on board "+number+" to "+mps);
         if (Debug.debug) Debug.print(hs[i]);
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
