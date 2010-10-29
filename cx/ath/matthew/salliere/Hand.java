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

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.MessageFormat;

import java.util.Arrays;

public class Hand
{
   public static final int AVERAGE=1;
   public static final int AVERAGE_MINUS=2;
   public static final int AVERAGE_PLUS=3;

   private boolean check_forced(String[] data, int ofs)
   {
      if ('!' == data[ofs].charAt(0)) {
         data[ofs] = data[ofs].substring(1);
         return true;
      }
      else return false;
   }

   String number = "";
   String ns = "";
   String ew = "";
   String contract = "";
   char declarer = ' ';
   int tricks;
   double nsscore;
   double ewscore;
   boolean forced_nsscore;
   boolean forced_ewscore;
   double nsmp;
   double ewmp;
   boolean forced_nsmp;
   boolean forced_ewmp;
   int ewavetype;
   int nsavetype;
	Board board;
   public Hand()
   {
   }
	@SuppressWarnings("fallthrough")
   public Hand(String[] data) throws HandParseException
   {
      if (data.length < 4) {
         String extra = "";
         if (data.length > 0)
            extra = MessageFormat.format(_(" (board might be number {0})"), new Object[] { data[0] });
         throw new HandParseException(_("Insufficient fields. I cannot parse a board without at least board number, pair numbers and contract")+extra);
      }
      this.number = data[0];
      this.ns = data[1];
      this.ew = data[2];
      this.contract = data[3];
      switch (data.length) {
         case 10:
            if (0 < data[9].length()) {
               forced_ewmp = check_forced(data, 9);
               try { this.ewmp = Double.parseDouble(data[9]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe); 
                  throw new HandParseException(_("Invalid Number of Match Points: ")+data[9]);
               }
            }
         case 9:
            if (0 < data[8].length()) {
               forced_nsmp = check_forced(data, 8);
               try { this.nsmp = Double.parseDouble(data[8]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe);
                  throw new HandParseException(_("Invalid Number of Match Points: ")+data[8]);
               }
            }
         case 8:
            if (0 < data[7].length()) {
               forced_ewscore = check_forced(data, 7);
               try { this.ewscore = Double.parseDouble(data[7]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe);
                  String av = data[7].toLowerCase();
                  if (av.startsWith("av") && av.length() == 3) {
							if (Debug.debug) Debug.print("Parsing average, mode character is "+av.charAt(2));
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
                           throw new HandParseException(_("Invalid Average: ")+data[7]);
                     }
						}
                  else throw new HandParseException(_("Invalid Score: ")+data[7]);
               }
            }
         case 7:
            if (0 < data[6].length()) {
               forced_nsscore = check_forced(data, 6);
               try { this.nsscore = Double.parseDouble(data[6]);
               } catch (NumberFormatException NFe) {
                  if (Debug.debug) Debug.print(NFe);
                  String av = data[6].toLowerCase();
                  if (av.startsWith("av") && av.length() == 3) {
							if (Debug.debug) Debug.print("Parsing average, mode character is "+av.charAt(2));
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
                           throw new HandParseException(_("Invalid Average: ")+data[6]);
                     }
						}
                  else throw new HandParseException(_("Invalid Score: ")+data[6]);
               }
            }
         case 6:
            if (0 < data[5].length())
               try { this.tricks = Integer.parseInt(data[5]);
               } catch (NumberFormatException NFe) { 
                  if (Debug.debug) Debug.print(NFe); 
                  throw new HandParseException(_("Invalid Number of Tricks: ")+data[5]);
               }
         case 5:
            if (data[4].length() > 0)
               this.declarer = data[4].charAt(0);
      }
   }
	@SuppressWarnings("fallthrough")
   public void score() throws ScoreException, ContractParseException, HandParseException
   {
      String[] n = number.split(":");
      String[] v = n[n.length-1].split(";");
      int num = 0;
      try {
         num = Integer.parseInt(v[0]);
      } catch (NumberFormatException NFe) { 
         if (Debug.debug) Debug.print(NFe); 
         throw new HandParseException(_("Invalid Hand Number: ")+number);
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
         if (!forced_nsscore) {
            if (nsscore != 0 && nsscore != c.getNSScore()) 
               throw new ScoreException(
                     MessageFormat.format(
                        _("Calculated score as {0} for NS but hand says {1}."),
                        new Object[] { c.getNSScore(), this }));
            nsscore = c.getNSScore();
         }
         if (!forced_ewscore) {
            if (ewscore != 0 && ewscore != c.getEWScore())
               throw new ScoreException(
                     MessageFormat.format(
                        _("Calculated score as {0} for EW but hand says {1}."),
                        new Object[] { c.getEWScore(), this }));
            ewscore = c.getEWScore();
         }
         if (tricks != 0 && tricks != c.getTricks())
            throw new ScoreException(
                  MessageFormat.format(
                     _("Calculated tricks as {0} but hand says {1}."),
                     new Object[] { c.getTricks(), this }));
         tricks = c.getTricks();
			if (0 == tricks)
				System.out.println(_("Warning, 0 tricks taken on board ")+board.getNumber());
         contract = c.getContract();
         declarer = c.getDeclarer();
         if (Debug.debug) Debug.print(this);
      } catch (NoContractException NCe) {
         if (Debug.debug) Debug.print(NCe);
         /* not assigning scores on hands which have no contract. Must be averages or something. */
      }
   }

	@SuppressWarnings("fallthrough")
   public void check() throws HandParseException
   {
      try {
         if (tricks > 13)
               throw new HandParseException(MessageFormat.format(_("Cannot take {0} tricks!"), new Object[] { tricks }));
         Contract c = new Contract(contract, declarer, 0, tricks);
         if (!c.isPassOut())
            switch (declarer) {
               case 'n':
               case 's':
               case 'e':
               case 'w':
               case 'N':
               case 'S':
               case 'E':
               case 'W':
                  break;
               default:
                  throw new HandParseException(MessageFormat.format(_("On board {0} declarer should be one of NSEW, it is actually {1}."), new Object[] { number, declarer}));
            }
      } catch (ContractParseException CPe) {
         if (Debug.debug) Debug.print(CPe);
         throw new HandParseException(MessageFormat.format(_("Failed to parse contract on board {0}: {1}"), new Object[] { number, contract}));
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
   public boolean isAveraged() { return !(0 == ewavetype && 0 == nsavetype); }
   public int getEWAverage() { return ewavetype; }
   public int getNSAverage() { return nsavetype; }
   public boolean hasForcedNSMP() { return forced_nsmp; }
   public boolean hasForcedEWMP() { return forced_ewmp; }
   public boolean hasForcedNSScore() { return forced_nsscore; }
   public boolean hasForcedEWScore() { return forced_ewscore; }
	public Board getBoard() { return board; }

	public void setBoard(Board b) { board = b; }
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
   public void setEWScore(String ewscore) throws HandParseException
   {
      if (0 < ewscore.length())
         try { this.ewscore = Double.parseDouble(ewscore);
         } catch (NumberFormatException NFe) { 
            if (Debug.debug) Debug.print(NFe);
            String av = ewscore.toLowerCase();
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
                     throw new HandParseException(_("Invalid Average: ")+ewscore);
               }
            else throw new HandParseException(_("Invalid Score: ")+ewscore);
         }
   }
   public void setNSScore(String nsscore) throws HandParseException
   { 
      if (0 < nsscore.length())
         try { this.nsscore = Double.parseDouble(nsscore);
         } catch (NumberFormatException NFe) { 
            if (Debug.debug) Debug.print(NFe);
            String av = nsscore.toLowerCase();
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
                     throw new HandParseException(_("Invalid Average: ")+nsscore);
               }
            else throw new HandParseException(_("Invalid Score: ")+nsscore);
         }
   }
   public void setForcedNSMP(boolean forced) { forced_nsmp = forced; }
   public void setForcedEWMP(boolean forced) { forced_ewmp = forced; }
   public void setForcedNSScore(boolean forced) { forced_nsscore = forced; }
   public void setForcedEWScore(boolean forced) { forced_ewscore = forced; }

   public String toString() 
   { 
      StringBuilder sb = new StringBuilder();
      sb.append(number);
      sb.append(": ");
      sb.append(ns);
      sb.append(_(" vs "));
      sb.append(ew);
      sb.append(" ");
      sb.append(contract);
      sb.append(_(" by "));
      sb.append(declarer);
      sb.append(_(" making "));
      sb.append(tricks);
      sb.append(_(" tricks. "));
      sb.append(nsscore);
      if (forced_nsscore) sb.append(_(" (forced)"));
      sb.append(_(" to NS "));
      sb.append(ewscore);
      if (forced_ewscore) sb.append(_(" (forced)"));
      sb.append(_(" to EW "));
      sb.append(nsmp);
      if (forced_nsmp) sb.append(_(" (forced)"));
      sb.append(_(" to NS "));
      sb.append(ewmp);
      if (forced_ewmp) sb.append(_(" (forced)"));
      sb.append(_(" to EW."));
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
            rv[6] = (forced_nsscore?"!":"") + format.format(nsscore, tmp, field).toString();
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
            rv[7] = (forced_ewscore?"!":"") + format.format(ewscore, tmp, field).toString();
            break;
      }

      tmp = new StringBuffer();
      rv[8] = (forced_nsmp?"!":"") + format.format(nsmp, tmp, field).toString();

      tmp = new StringBuffer();
      rv[9] = (forced_ewmp?"!":"") + format.format(ewmp, tmp, field).toString();

      return rv;
   }
}
