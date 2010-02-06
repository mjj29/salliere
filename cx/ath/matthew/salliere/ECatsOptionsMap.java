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

import java.util.HashMap;

import static cx.ath.matthew.salliere.Gettext._;

@SuppressWarnings("serial")
public class ECatsOptionsMap extends HashMap<String, String>
{
	public static void printOptions()
	{
      System.out.println("ECATS options: ");
      System.out.println("\tclubName = name of club (required)");
      System.out.println("\tsession = ECATS session number (required)");
      System.out.println("\tphone = contact phone number (required)");
      System.out.println("\tcountry = club country (required)");
      System.out.println("\tname = contact name");
      System.out.println("\tfax = contact fax number");
      System.out.println("\temail = contact email");
      System.out.println("\ttown = club town");
      System.out.println("\tcounty = club county");
      System.out.println("\tdate = event date");
      System.out.println("\tevent = event name");
      System.out.println("\tnboid = club NBO ID");
	}
   public ECatsOptionsMap(String options) throws ScoreException
   {
      super();
      put("name", "");
      put("fax", "");
      put("email", "");
      put("town", "");
      put("county", "");
      put("date", "");
      put("event", "");
      put("nboid", "");
      if (null != options) {
         String[] opts = options.split(",");
         for (String o: opts) {
            String[] kv = o.split(":");
            put(kv[0], kv[1]);
         }
         if (null == get("clubName")) throw new ScoreException(_("Required ECATS option not found: ")+"clubName");
         if (null == get("session")) throw new ScoreException(_("Required ECATS option not found: ")+"session");
         if (null == get("phone")) throw new ScoreException(_("Required ECATS option not found: ")+"phone");
         if (null == get("country")) throw new ScoreException(_("Required ECATS option not found: ")+"country");
      }
   }
}
