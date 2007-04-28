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

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class Hand
{
   String number;
   String ns;
   String ew;
   String contract;
   char declarer;
   int tricks;
   double nsscore;
   double ewscore;
   double nsmp;
   double ewmp;
   public Hand(String[] data) throws HandParseException
   {
      if (data.length < 5) 
         throw new HandParseException("Insufficient fields. I cannot parse a board without at least board number, pair numbers, contract and declarer");
      this.number = data[0];
      this.ns = data[1];
      this.ew = data[2];
      this.contract = data[3];
      this.declarer = data[4].charAt(0);
      switch (data.length) {
         case 10:
            if (0 < data[9].length())
               this.ewmp = Double.parseDouble(data[9]);
         case 9:
            if (0 < data[8].length())
               this.nsmp = Double.parseDouble(data[8]);
         case 8:
            if (0 < data[7].length())
               this.ewscore = Double.parseDouble(data[7]);
         case 7:
            if (0 < data[6].length())
               this.nsscore = Double.parseDouble(data[6]);
         case 6:
            if (0 < data[5].length())
               this.tricks = Integer.parseInt(data[5]);
      }
   }
   public void score() throws ScoreException, ContractParseException
   {
      int num = Integer.parseInt(number);
      int vul = Contract.NONE;
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
      Contract c = new Contract(contract, declarer, vul, tricks);
      if (nsscore != 0 && nsscore != c.getNSScore()) throw new ScoreException("Calculated score as "+c.getNSScore()+" for NS but hand says "+this);
      nsscore = c.getNSScore();
      if (ewscore != 0 && ewscore != c.getEWScore()) throw new ScoreException("Calculated score as "+c.getEWScore()+" for EW but hand says "+this);
      ewscore = c.getEWScore();
      if (tricks != 0 && tricks != c.getTricks()) throw new ScoreException("Calculated tricks as "+c.getTricks()+" but hand says "+this);
      tricks = c.getTricks();
      contract = c.getContract();
   }
   public String getNumber() { return number; }
   public String getNS() { return ns; }
   public String getEW() { return ew; }
   public String getContract() { return contract; }
   public double getNSMP() { return nsmp; }
   public double getEWMP() { return ewmp; }
   public double getEWScore() { return ewscore; }
   public double getNSScore() { return nsscore; }
   public void setNSMP(double mp) { nsmp = mp; }
   public void setEWMP(double mp) { ewmp = mp; }
   public int getTricks() { return tricks; }
   public char getDeclarer() { return declarer; }
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

      DecimalFormat format = new DecimalFormat("0.#");
      FieldPosition field = new FieldPosition(DecimalFormat.INTEGER_FIELD);

      StringBuffer tmp = new StringBuffer();
      rv[6] = format.format(nsscore, tmp, field).toString();

      tmp = new StringBuffer();
      rv[7] = format.format(ewscore, tmp, field).toString();

      tmp = new StringBuffer();
      rv[8] = format.format(nsmp, tmp, field).toString();

      tmp = new StringBuffer();
      rv[9] = format.format(ewmp, tmp, field).toString();

      return rv;
   }
}
