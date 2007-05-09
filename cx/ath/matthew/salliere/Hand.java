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

import java.text.DecimalFormat;
import java.text.FieldPosition;

import java.util.Arrays;

public class Hand
{
   public static final int AVERAGE=1;
   public static final int AVERAGE_MINUS=2;
   public static final int AVERAGE_PLUS=3;
   String number = "";
   String ns = "";
   String ew = "";
   String contract = "";
   char declarer = ' ';
   int tricks;
   double nsscore;
   double ewscore;
   double nsmp;
   double ewmp;
   int ewavetype;
   int nsavetype;
   public Hand()
   {
   }
   public Hand(String[] data) throws HandParseException
   {
      if (data.length < 4) 
         throw new HandParseException("Insufficient fields. I cannot parse a board without at least board number, pair numbers and contract");
      this.number = data[0];
      this.ns = data[1];
      this.ew = data[2];
      this.contract = data[3];
      switch (data.length) {
         case 10:
            if (0 < data[9].length())
               try { this.ewmp = Double.parseDouble(data[9]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe); 
                  throw new HandParseException("Invalid Number of Match Points: "+data[9]);
               }
         case 9:
            if (0 < data[8].length())
               try { this.nsmp = Double.parseDouble(data[8]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe);
                  throw new HandParseException("Invalid Number of Match Points: "+data[8]);
               }
         case 8:
            if (0 < data[7].length())
               try { this.ewscore = Double.parseDouble(data[7]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe);
                  String av = data[7].toLowerCase();
                  if (av.startsWith("av") && av.length() == 3)
                     switch (av.charAt(2)) {
                        case '=':
                           ewavetype = AVERAGE;
                           break;
                        case '+':
                           ewavetype = AVERAGE_PLUS;
                           break;
                        case '-':
                           ewavetype = AVERAGE_MINUS;
                           break;
                        default:
                           throw new HandParseException("Invalid Average: "+data[7]);
                     }
                  else throw new HandParseException("Invalid Score: "+data[7]);
               }
         case 7:
            if (0 < data[6].length())
               try { this.nsscore = Double.parseDouble(data[6]);
               } catch (NumberFormatException NFe) {
                  if (Debug.debug) Debug.print(NFe);
                  String av = data[6].toLowerCase();
                  if (av.startsWith("av") && av.length() == 3)
                     switch (av.charAt(2)) {
                        case '=':
                           nsavetype = AVERAGE;
                           break;
                        case '+':
                           nsavetype = AVERAGE_PLUS;
                           break;
                        case '-':
                           nsavetype = AVERAGE_MINUS;
                           break;
                        default:
                           throw new HandParseException("Invalid Average: "+data[6]);
                     }
                  else throw new HandParseException("Invalid Score: "+data[6]);
               }
         case 6:
            if (0 < data[5].length())
               try { this.tricks = Integer.parseInt(data[5]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe); 
                  throw new HandParseException("Invalid Number of Tricks: "+data[5]);
               }
         case 5:
            if (data[4].length() > 0)
               this.declarer = data[4].charAt(0);
      }
   }
   public void score() throws ScoreException, ContractParseException, HandParseException
   {
      String[] n = number.split(":");
      String[] v = n[n.length-1].split(";");
      int num = 0;
      try {
         num = Integer.parseInt(v[0]);
      } catch (NumberFormatException NFe) { 
         if (Debug.debug) Debug.print(NFe); 
         throw new HandParseException("Invalid Hand Number: "+number);
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
      if (Debug.debug) Debug.print("number="+number+", num="+num+", n="+Arrays.asList(n)+", v="+Arrays.asList(v)+", vul="+vul);

      Contract c;
      try {
         c = new Contract(contract, declarer, vul, tricks);
         if (nsscore != 0 && nsscore != c.getNSScore()) throw new ScoreException("Calculated score as "+c.getNSScore()+" for NS but hand says "+this);
         nsscore = c.getNSScore();
         if (ewscore != 0 && ewscore != c.getEWScore()) throw new ScoreException("Calculated score as "+c.getEWScore()+" for EW but hand says "+this);
         ewscore = c.getEWScore();
         if (tricks != 0 && tricks != c.getTricks()) throw new ScoreException("Calculated tricks as "+c.getTricks()+" but hand says "+this);
         tricks = c.getTricks();
         contract = c.getContract();
         declarer = c.getDeclarer();
         if (Debug.debug) Debug.print(this);
      } catch (NoContractException NCe) {
         if (Debug.debug) Debug.print(NCe);
         /* not assigning scores on hands which have no contract. Must be averages or something. */
      }
   }

   public String getNumber() { return number; }
   public String getNS() { return ns; }
   public String getEW() { return ew; }
   public String getContract() { return contract; }
   public double getNSMP() { return nsmp; }
   public double getEWMP() { return ewmp; }
   public double getEWScore() { return ewscore; }
   public double getNSScore() { return nsscore; }
   public int getTricks() { return tricks; }
   public char getDeclarer() { return declarer; }

   public void setNSMP(double mp) { nsmp = mp; }
   public void setEWMP(double mp) { ewmp = mp; }
   public void setNumber(String number) { this.number = number; }
   public void setNS(String ns) { this.ns = ns; }
   public void setEW(String ew) { this.ew = ew; }
   public void setContract(String contract) { this.contract = contract; }
   public void setEWScore(double ewscore) { this.ewscore = ewscore; }
   public void setNSScore(double nsscore) { this.nsscore = nsscore; }
   public void setTricks(int tricks) { this.tricks = tricks; }
   public void setDeclarer(char declarer) { this.declarer = declarer; }

   public String toString() 
   { 
      StringBuilder sb = new StringBuilder();
      sb.append(number);
      sb.append(": ");
      sb.append(ns);
      sb.append(" vs ");
      sb.append(ew);
      sb.append(" ");
      sb.append(contract);
      sb.append(" by ");
      sb.append(declarer);
      sb.append(" making ");
      sb.append(tricks);
      sb.append(" tricks. ");
      sb.append(nsscore);
      sb.append(" to NS ");
      sb.append(ewscore);
      sb.append(" to EW ");
      sb.append(nsmp);
      sb.append(" to NS ");
      sb.append(ewmp);
      sb.append(" to EW.");
      return sb.toString();
   }
   public String[] export()
   {
      String[] rv = new String[10];
      rv[0] = number;
      rv[1] = ns;
      rv[2] = ew;
      rv[3] = contract;
      rv[4] = ""+declarer;
      rv[5] = ""+tricks;

      DecimalFormat format = new DecimalFormat("0.##");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      StringBuffer tmp = new StringBuffer();
      switch (nsavetype) {
         case AVERAGE:
            rv[6] = "av=";
            break;
         case AVERAGE_MINUS:
            rv[6] = "av-";
            break;
         case AVERAGE_PLUS:
            rv[6] = "av+";
            break;
         default:
            rv[6] = format.format(nsscore, tmp, field).toString();
            break;
      }

      tmp = new StringBuffer();
      switch (ewavetype) {
         case AVERAGE:
            rv[7] = "av=";
            break;
         case AVERAGE_MINUS:
            rv[7] = "av-";
            break;
         case AVERAGE_PLUS:
            rv[7] = "av+";
            break;
         default:
            rv[7] = format.format(ewscore, tmp, field).toString();
            break;
      }

      tmp = new StringBuffer();
      rv[8] = format.format(nsmp, tmp, field).toString();

      tmp = new StringBuffer();
      rv[9] = format.format(ewmp, tmp, field).toString();

      return rv;
   }
}
