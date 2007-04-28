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

public class Contract
{
   public static final int NONE = 0;
   public static final int NORTH = 1;
   public static final int EAST = 2;
   public static final int ALL = 3;
   int nsscore;
   int ewscore;
   public Contract(String contract, char declarer, int vulnerability, int tricks) throws ContractParseException
   {
      char[] cs = contract.toCharArray();
      int val = 0;
      char den = ' ';
      int doubled = 1; 
      for (int i = 0; i < cs.length; i++) {
         switch (cs[i]) {
            case ' ': continue;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
               val = (int) (cs[i] - '0');
               continue;
            case 'H':
            case 'C':
            case 'D':
            case 'S':
            case 'N':
               den = cs[i];
               continue;
            case 'h':
               den = 'H';
               continue;
            case 'c':
               den = 'C';
               continue;
            case 'd':
               den = 'D';
               continue;
            case 's':
               den = 'S';
               continue;
            case 'n':
            case 't':
            case 'T':
               den = 'N';
               continue;
            case 'X':
            case 'x':
               doubled*=2;
               continue;
            default:
               throw new ContractParseException("Symbol: "+cs[i]+" not allowed in contract");
         }
      }
      if (val < 1 || val > 7) throw new ContractParseException("Value must be between 1 & 7");
      if (den == ' ') throw new ContractParseException("No denomination specified in contract");
      if (doubled > 4) throw new ContractParseException("Cannot re-re-double contracts!"); 

      if (Debug.debug) Debug.print(""+val+den+(1<doubled?"X":"")+(2<doubled?"X":"")+" by "+declarer+" making "+tricks);

      int score = 0;
      boolean vul = ((declarer == 'N' || declarer == 'S') && vulnerability == NORTH)
                 || ((declarer == 'E' || declarer == 'W') && vulnerability == EAST)
                 || vulnerability == ALL;
      if (Debug.debug) Debug.print("boolean "+vul+" = (("+declarer+" == 'N' || "+declarer+" == 'S') && "+vulnerability+" == "+NORTH+") || (("+declarer+" == 'E' || "+declarer+" == 'W') && "+vulnerability+" == "+EAST+") || "+vulnerability+" == "+ALL);

      // contract was made
      if (tricks >= (val+6)) {
         if (Debug.debug) Debug.print("MADE");
         // NT bonus
         if ('N' == den) score += 10*doubled;
         if (Debug.debug) Debug.print(score);

         // trick score
         int ts = 0;
         switch (den) {
            case 'C':
            case 'D':
               ts = 20;
               break;
            default:
               ts = 30;
               break;
         }
         score += ts*val*doubled;
         if (Debug.debug) Debug.print(score);

         // partscore/game bonus
         if (score < 100) score += 50;
         else if (vul)
            score += 500;
         else
            score += 300;
         if (Debug.debug) Debug.print(score);

         // double bonus
         if (2 == doubled) score += 50;
         // redouble bonus
         else if (4 == doubled) score += 100;
         if (Debug.debug) Debug.print(score);

         // slams
         if (6 == val && vul) score += 750;
         else if (6 == val) score += 500;
         else if (7 == val && vul) score += 1500;
         else if (7 == val) score += 1000;
         if (Debug.debug) Debug.print(score);

         // overtricks
         int ot = tricks - val - 6;
         if (Debug.debug) Debug.print("ot = "+ot+", doubled = "+doubled+", ts = "+ts);
         if (1 == doubled) {
            if (Debug.debug) Debug.print("diff = "+(ts*ot));
            score += ts*ot;
         } else {
            int vm = vul?2:1;
            if (Debug.debug) Debug.print("vm = "+vm+", diff = "+(100*(doubled-2)*vm*ot));
            score += 100*(doubled/2)*vm*ot;
         }
         if (Debug.debug) Debug.print(score);

         // assign to the correct side
         if ('N' == declarer || 'S' == declarer)
            nsscore = score;
         else
            ewscore = score;
      }
      // contract went off
      else {
         if (Debug.debug) Debug.print("SET");
         for (int i = 1; (val-i+6) >= tricks; i++) {
            switch (i) {
               case 1:
                  score += (vul?100:50)*doubled;
                  break;
               case 2:
               case 3:
                  if (1 == doubled)
                     score += (vul?100:50);
                  else 
                     score += (vul?150:100)*doubled;
                  break;
               default:
                  if (1 == doubled)
                     score += (vul?100:50);
                  else 
                     score += 150*doubled;
            }
         }

         // assign to the correct side
         if ('N' == declarer || 'S' == declarer)
            ewscore = score;
         else
            nsscore = score;
      }

      if (Debug.debug) Debug.print("score = "+score);
   }
   public double getNSScore()
   {
      return nsscore;
   }
   public double getEWScore()
   {
      return ewscore;
   }
}
