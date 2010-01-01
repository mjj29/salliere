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

public abstract class MasterPointScale
{
	public static void printScales()
	{
		System.out.println("Available master point scales:");
		System.out.println("\tA, Club = Club scale");
		System.out.println("\tB, District = District scale");
		System.out.println("\tC, County = County scale");
		System.out.println("\tD, Regional = Regional scale");
		System.out.println("\tE, National = National scale");
		System.out.println("\tQA, Club Quali-Final = Club Quali-Final scale");
		System.out.println("\tQB, District Quali-Final = District Quali-Final scale");
		System.out.println("\tQC, County Quali-Final = County Quali-Final scale");
		System.out.println("\tQD, Regional Quali-Final = Regional Quali-Final scale");
		System.out.println("\tQE, National Quali-Final = National Quali-Final scale");
	}
	public static MasterPointScale getScale(String scale) throws ScoreException
	{
		if (Debug.debug) Debug.print(Debug.INFO, "Using master point scale "+scale);
		if (null == scale) return new ClubScale();

		String upscale = scale.toUpperCase();
		// scale names in the handbook
		if ("A".equals(upscale)) return new ClubScale();
		if ("B".equals(upscale)) return new DistrictScale();
		if ("C".equals(upscale)) return new CountyScale();
		if ("D".equals(upscale)) return new RegionalScale();
		if ("E".equals(upscale)) return new NationalScale();
		if ("QA".equals(upscale)) return new ClubQualiFinalScale();
		if ("QB".equals(upscale)) return new DistrictQualiFinalScale();
		if ("QC".equals(upscale)) return new CountyQualiFinalScale();
		if ("QD".equals(upscale)) return new RegionalQualiFinalScale();
		if ("QE".equals(upscale)) return new NationalQualiFinalScale();

		// human-readable names
		if ("CLUB".equals(upscale)) return new ClubScale();
		if ("DISTRICT".equals(upscale)) return new DistrictScale();
		if ("COUNTY".equals(upscale)) return new CountyScale();
		if ("REGIONAL".equals(upscale)) return new RegionalScale();
		if ("NATIONAL".equals(upscale)) return new NationalScale();
		if ("CLUB QUALI-FINAL".equals(upscale)) return new ClubQualiFinalScale();
		if ("DISTRICT QUALI-FINAL".equals(upscale)) return new DistrictQualiFinalScale();
		if ("COUNTY QUALI-FINAL".equals(upscale)) return new CountyQualiFinalScale();
		if ("REGIONAL QUALI-FINAL".equals(upscale)) return new RegionalQualiFinalScale();
		if ("NATIONAL QUALI-FINAL".equals(upscale)) return new NationalQualiFinalScale();

		throw new ScoreException(_("Could not find master point scale: ")+scale);
	}

   public int minPairs(int pairsize) { return pairsize == 1 ? 8 : 6; }
   public int numberAwards(int pairs, int pairsize, int boards)
	{
		int arity = 1 == pairsize ? 4 : 2;
		double awards = (double) (pairs/arity);
		if (boards < 18) awards /= 4;
		else if (boards < 36) awards /= 3;
		else awards /= 2;
		return (int) Math.ceil(awards * arity);
	}
   public double getTop(int pairs, int pairsize, int boards) { return numberAwards(pairs, pairsize, boards) * getRate(pairsize); }
   public abstract int getMax(int boards);
   public abstract int getMin();
   public abstract double getRate(int pairsize);
}

class ClubScale extends MasterPointScale
{
   public int getMax(int boards) { 
		if (boards < 12) return 0;
		if (boards < 18) return 75;
		if (boards < 36) return 100; 
		return 300;
	}
   public int getMin() { return 6; }
   public double getRate(int pairsize) { return 1 == pairsize ? 3: 6; }
}
class DistrictScale extends MasterPointScale
{
   public int getMax(int boards) { 
		if (boards < 12) return 0;
		if (boards < 18) return 113;
		if (boards < 36) return 150; 
		return 450;
	}
   public int getMin() { return 9; }
   public double getRate(int pairsize) { return 1 == pairsize ? 4.5: 9; }
}
class CountyScale extends MasterPointScale
{
   public int getMax(int boards) { 
		if (boards < 12) return 0;
		if (boards < 18) return 150;
		if (boards < 36) return 200; 
		return 600;
	}
   public int getMin() { return 12; }
   public double getRate(int pairsize) { return 1 == pairsize ? 6: 12; }
}
class RegionalScale extends MasterPointScale
{
   public int getMax(int boards) { 
		if (boards < 12) return 0;
		if (boards < 18) return 225;
		if (boards < 36) return 300; 
		return 900;
	}
   public int getMin() { return 18; }
   public double getRate(int pairsize) { return 1 == pairsize ? 9: 18; }
}
class NationalScale extends MasterPointScale
{
   public int getMax(int boards) { 
		if (boards < 12) return 0;
		if (boards < 18) return 300;
		if (boards < 36) return 400; 
		return 1200;
	}
   public int getMin() { return 24; }
   public double getRate(int pairsize) { return 1 == pairsize ? 12: 24; }
}

abstract class QualiFinalScale extends MasterPointScale
{
   public int numberAwards(int pairs, int pairsize, int boards)
	{
		int arity = 1 == pairsize ? 4 : 2;
		double awards = (double) (pairs/arity);
		awards /= 4;
		return (int) Math.ceil(awards * arity);
	}
}

class ClubQualiFinalScale extends QualiFinalScale
{
   public int getMin() { return 6; }
   public double getRate(int pairsize) { return 1 == pairsize ? 5.5: 11; }
   public int getMax(int boards) { return 300; } 
}
class DistrictQualiFinalScale extends QualiFinalScale
{
   public int getMin() { return 9; }
   public double getRate(int pairsize) { return 1 == pairsize ? 8.25: 16.5; }
   public int getMax(int boards) { return 450; } 
}
class CountyQualiFinalScale extends QualiFinalScale
{
   public int getMin() { return 12; }
   public double getRate(int pairsize) { return 1 == pairsize ? 11: 22; }
   public int getMax(int boards) { return 600; } 
}
class RegionalQualiFinalScale extends QualiFinalScale
{
   public int getMin() { return 18; }
   public double getRate(int pairsize) { return 1 == pairsize ? 16.5: 33; }
   public int getMax(int boards) { return 900; } 
}
class NationalQualiFinalScale extends QualiFinalScale
{
   public int getMin() { return 24; }
   public double getRate(int pairsize) { return 1 == pairsize ? 22: 44; }
   public int getMax(int boards) { return 1200; } 
}
