package feederz;
import robocode.*;
import java.awt.geom.*;

public class DCTargeting {
	private AdvancedRobot bot;
	RobotData enemyData;
	boolean aiming;
	protected long lastAimTime = 0;
	public long firedCount = 0, hitCount = 0;
	private int maxLogSize = 30000;
	private double toleranceWidth = 20;
	public ScanInfo first, last;
	public int size = 0;

	private double lastDir = 1;
	private long lastRunStart = 0;
	private double lastRunTime = 0;
	private double lastVelocity;

	long startTime = 0;
	double bulletPower = 3;
	private long lastTime = 0;
	private double battleFieldWidth, battleFieldHeight;

	private double tolerance = 0;
	private double nextX = 0, nextY = 0, headOnAngle;

	public DCTargeting(AdvancedRobot b, RobotData ed) {
		bot = b;
		enemyData = ed;
	}

	public void init() {
		enemyData.position = null;
		aiming = false;
		battleFieldWidth = bot.getBattleFieldWidth();
		battleFieldHeight = bot.getBattleFieldHeight();
	}

	public void cleanUp() {
		enemyData.position = null;
		if (size == 0) return;
		while (size > maxLogSize) {
			first = first.next;
			size--;
		}
		first.previous = null;
	}

	public double calcBulletPower() {
		if (enemyData.position == null) return 0;
		double bulletPower = 0;
		bulletPower = enemyData.distance > 150 ? 1.9 : 3;
		bulletPower = Math.min(bulletPower, (enemyData.energy + .1) / 4);
		if (bulletPower * 6 >= bot.getEnergy()) bulletPower = bot.getEnergy() / 6;
		if (bulletPower >= bot.getEnergy() - .1) bulletPower = bot.getEnergy() - .1;
		bulletPower = Math.max(Rules.MIN_BULLET_POWER, Math.min(Rules.MAX_BULLET_POWER, bulletPower));
		return bulletPower;
	}

	public void setTurnAndFire() {
		Bullet b = null;
		double nextTurn;
		if (bot.getTurnRemaining() >= 0) nextTurn = Math.min(bot.getTurnRemaining(), 10 - .75 * Math.abs(bot.getVelocity()));
		else nextTurn = Math.max(bot.getTurnRemaining(), -10 + .75 * Math.abs(bot.getVelocity()));
		nextX = bot.getX() + bot.getVelocity() * Helpers.sinD(nextTurn);
		nextY = bot.getY() + bot.getVelocity() * Helpers.cosD(nextTurn);
		headOnAngle = Helpers.angleTo(nextX, nextY, enemyData.position.getX(), enemyData.position.getY());
		bulletPower = calcBulletPower();
		if (bot.getGunHeat() > bot.getGunCoolingRate() || bot.getEnergy() < bulletPower || bulletPower == 0) {
			aiming = false;
			bot.setTurnGunRight(Helpers.normalizeBearing(headOnAngle - bot.getGunHeading()));
		}
		else {
			if (aiming && bot.getGunTurnRemaining() == 0 && bot.getGunHeat() == 0) {
				b = bot.setFireBullet(bulletPower);
				aiming = false;
			}
			else if (!aiming) {
				lastAimTime = bot.getTime();
				double fireAngle = findBestAngle();
				if (fireAngle != 10000) {
					bot.setTurnGunRight(Helpers.normalizeBearing(fireAngle - bot.getGunHeading()));
					aiming = true;
				}
				else {
					bot.setTurnGunRight(Helpers.normalizeBearing(headOnAngle - bot.getGunHeading()));
				}
			}
		}

		if (b != null) {
			//robot da ban 1 phat
			firedCount++;
		}
	}

	public void updateData() {
		//sap het energy thi ko ban nua (0)
		if (bot.getEnergy() <= .1) return;
		double velocity = enemyData.velocity;
		double x = enemyData.position.getX();
		double y = enemyData.position.getY();
		double heading;
		long t1 = bot.getTime() - startTime;
		if (t1 - lastTime > 20 || t1 < lastTime) {
			startTime = bot.getTime();
			lastRunStart = startTime;
		}
		t1 = bot.getTime() - startTime;

		lastDir = velocity != 0 ? velocity : lastDir;
		if (velocity != lastVelocity) {
			lastRunTime = Math.min(40.0, (double)t1 - lastRunStart) / 40;
			lastRunStart = t1;
		}
		heading = lastDir < 0 ? (enemyData.headingRadians + 180) % 360 : enemyData.headingRadians;
		ScanInfo currInfo = new ScanInfo(enemyData.position, heading, (Math.abs(velocity) / 8), t1);
		currInfo.dtm = Math.min(enemyData.distance, 800) / 800;
		currInfo.runTime = Math.min(40.0, (double)(t1 - lastRunStart)) / 40.0;//40
		currInfo.lastRunTime = lastRunTime;
		currInfo.myGunHeat = Math.min(1.5, bot.getGunHeat()) / 1.5;
		if (Math.abs(lastVelocity) > Math.abs(velocity)) currInfo.acc = 1;
		else if (Math.abs(lastVelocity) < Math.abs(velocity)) currInfo.acc = -1;
		lastVelocity = velocity;
		currInfo.atm = Math.abs(Helpers.normalizeBearing(heading - Math.toDegrees(enemyData.headingRadians + enemyData.bearingRadians))) / 180;

		double maxWDist = 400;
		double distV = 0, distH = 0;
		if (heading == 90 || heading == 270) distV = Double.POSITIVE_INFINITY;
		else if (heading < 90 || heading > 270) distV = (battleFieldHeight - y) / Helpers.cosD(heading);
		else distV = y / Helpers.cosD(heading - 180);
		if (heading == 180 || heading == 0) distH = Double.POSITIVE_INFINITY;
		else if (heading < 180) distH = (battleFieldWidth - x) / Helpers.cosD(heading - 90);
		else distH = x / Helpers.cosD(heading - 180 - 90);
		currInfo.dtwf = 1 - (Math.min(Math.min(distV, distH), maxWDist) / maxWDist);

		double h1 = (heading + 180) % 360;
		if (h1 < 0) h1 += 360;
		if (h1 == 90 || h1 == 270) distV = Double.POSITIVE_INFINITY;
		else if (h1 < 90 || h1 > 270) distV = (battleFieldHeight - y) / Helpers.cosD(h1);
		else distV = y / Helpers.cosD(h1 - 180);
		if (h1 == 180 || h1 == 0) distH = Double.POSITIVE_INFINITY;
		else if (h1 < 180) distH = (battleFieldWidth - x) / Helpers.cosD(h1 - 90);
		else distH = x / Helpers.cosD(h1 - 180 - 90);
		currInfo.dtwb = 1 - (Math.min(Math.min(distV, distH), maxWDist) / maxWDist);
		// --

		if (size == 0) {
			first = currInfo;
			last = currInfo;
		}
		else {
			last.next = currInfo;
			currInfo.previous = last;
			last = currInfo;
		}
		size++;
		lastTime = t1;
	}

	public double findBestAngle() {

		int topCount = 50, angMax = 50;

		ScanInfo info = last;
		info.fired = true;
		ScanInfo currentInfo = info;
		double currentDistance = 0;
		boolean newRound = true;
		ScanInfo[] topInfo = new ScanInfo[topCount];
		double topDiff[] = new double[topCount];
		double bestAngle = 0;

		if (last == null) return(headOnAngle);

		long t1 = info.t;

		for (int i = 0; i < topCount; i++) {
			topDiff[i] = Double.POSITIVE_INFINITY;
			topInfo[i] = currentInfo;
		}

		while (info.previous != null) {
			if (newRound) {
				t1 = info.t;
				if (info.t <= t1) newRound = false;
			}
			else {
				currentDistance = 0;

				currentDistance += Helpers.sqr((currentInfo.dtm - info.dtm)) * 4;
				currentDistance += Helpers.sqr((currentInfo.atm -  info.atm));
				currentDistance += Helpers.sqr((currentInfo.v - info.v)) * 2;
				currentDistance += Helpers.sqr((currentInfo.dtwf - info.dtwf)) * 4;
				currentDistance += Helpers.sqr((currentInfo.dtwb - info.dtwb));
				currentDistance += Helpers.sqr((currentInfo.runTime - info.runTime));
				currentDistance += Helpers.sqr((currentInfo.lastRunTime - info.lastRunTime));
				currentDistance += Helpers.sqr((currentInfo.acc - info.acc) / 2);
				
				int i = topCount - 1;
				while (i >= 0 && currentDistance < topDiff[i]) i--;
				if (i < topCount - 1) {
					i++;
					for (int j = topCount - 2; j >= i; j--) {
						topDiff[j + 1] = topDiff[j];
						topInfo[j + 1] = topInfo[j];
					}
					topDiff[i] = currentDistance;
					topInfo[i] = info;
				}

				info = info.previous;
				if (info.t > t1) newRound = true;
			}
		}

		double dists[] = new double[topCount];
		for (int i = 0; i < topCount; i++) dists[i] = topDiff[i];

		double[][] angles = new double[topCount][4];
		for (int i = 0; i < topCount; i++) angles[i][0] = 1000;
		int angCount = 0;
		for (int i = 0; i < topCount && angCount < angMax; i++) {
			double ang = getGunAngle(topInfo[i]);
			if (ang < 1000) {
				ang = Helpers.normalizeBearing(ang - headOnAngle);
				angles[angCount][0] = ang;
				angles[angCount][1] = tolerance;
				angles[angCount][2] = 1;//topDiff[i];
				angles[angCount][3] = 1;//topDiff[i];
				for (int j = 0; j < angCount; j++) {
					if (angles[j][0] < 1000) {
						if (Math.abs(angles[j][0] - angles[angCount][0]) < angles[angCount][1]) {
							angles[j][2] += angles[angCount][3];//1;
						}
						if (Math.abs(angles[angCount][0] - angles[j][0]) < angles[j][1]) {
							angles[angCount][2] += angles[j][3];//1;
						}
					}
				}
				angCount++;
			}
		}
		double maxCount = 0; int maxIDX = 0;
		for (int j = 0; j < angCount; j++) {
			if (angles[j][2] > maxCount) {
				maxCount = angles[j][2];
				maxIDX = j;
			}
		}

		bestAngle = angles[maxIDX][0];
		if (bestAngle >= 1000) bestAngle = 0;

		return (headOnAngle + bestAngle);
	}

	public double getGunAngle(ScanInfo predictedInfo) {
		tolerance = 0;
		ScanInfo currInfo = last;
		ScanInfo endInfo = predictedInfo;
		long timeDelta = (bot.getTime() - startTime) - currInfo.t;
		double predx = 0, predy = 0, predDist = 0, predAng;
		double bulletSpeed = (20 - 3 * bulletPower);
		long bulletTime;
		for (int i = 0; i < 50; i++) {
			predDist = Helpers.distanceTo(endInfo.x, endInfo.y, predictedInfo.x, predictedInfo.y);
			predAng = Math.PI/2 - Math.atan2(endInfo.y - predictedInfo.y, endInfo.x - predictedInfo.x) - Math.toRadians(predictedInfo.d);
			predx = currInfo.x + predDist * Math.sin(Math.toRadians(currInfo.d) + predAng);
			predy = currInfo.y + predDist * Math.cos(Math.toRadians(currInfo.d) + predAng);
			predDist = Helpers.distanceTo(predx, predy, nextX, nextY);
			bulletTime = (long)(predDist / bulletSpeed) + 1;//<-- +1
			if (Math.abs(endInfo.t - predictedInfo.t - timeDelta - bulletTime) <= 1) break;
			endInfo = predictedInfo;
			while (endInfo.next != null && endInfo.t >= predictedInfo.t && (endInfo.t - predictedInfo.t - timeDelta) < bulletTime) {
				endInfo = endInfo.next;
			}
			if (endInfo.next == null || endInfo.t < predictedInfo.t) return (Double.POSITIVE_INFINITY);
		}
		if (predx < 0 || predx > battleFieldWidth
				|| predy < 0 || predy > battleFieldHeight) return (Double.POSITIVE_INFINITY);
		tolerance = Math.toDegrees(/*Math.atan*/(toleranceWidth / predDist));
		return (Math.toDegrees(Math.PI/2 - Math.atan2(predy - nextY, predx - nextX)));
	}

	public class ScanInfo {
		public double x = 0, y = 0, d = 0;
		public long t = 0;
		public double v = 0;
		public double acc = 0;
		public double atm = 0;
		public double dtm = 0;
		public double dtwf = 0;
		public double dtwb = 0;
		public double runTime = 0;
		public double lastRunTime = 0;
		public double myGunHeat = 0;
		public boolean fired = false;
		public ScanInfo previous;
		public ScanInfo next;

		public ScanInfo (Point2D.Double pos, double d1, double v1, long t1) {
			x = pos.getX();
			y = pos.getY();
			d = d1;
			v = v1;
			t = t1;
		}
	}
}										