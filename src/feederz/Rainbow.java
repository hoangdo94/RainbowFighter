/**
 * Authors: Hieu + Hoang
 * Created: April 29
 */
package feederz;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import robocode.*;
import robocode.util.Utils;

public class Rainbow extends AdvancedRobot {
	RobotData robotData = new RobotData(this);
	DCTargeting gunController = new DCTargeting(this, 800, 600);
	
	Color colors[] = { new Color(255, 0, 0), new Color(255, 127, 0),
			new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(75, 0, 130), new Color(143, 0, 255) };
	int colorNum = 0;
	int aimMode = 2;

	public void initializeRobot() {
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		gunController.initRound();
		setMaxVelocity(8);
	}

	public void changeAllColors() {
		Color c = colors[colorNum];
		setColors(c, c, c, Color.white, c);
		colorNum++;
		if (colorNum > 6)
			colorNum = 0;
	}
	
	public void run() {
		testRun();
		initializeRobot();
		changeAllColors();
		
		do {
			turnRadarRight(Double.POSITIVE_INFINITY);
		} while (true);
	}

	public void controllRadar() {
		double absBearing = robotData.bearingRadians + getHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing
				- getRadarHeadingRadians()) * 2);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		robotData.updateData(e);
		controllRadar();
		gunController.updateScannedRobot(e);
		testOnScannedRobot(e);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		changeAllColors();
		testOnHitBullet(e);
	}
	
	public void onBulletHit(BulletHitEvent e) {
		gunController.hitCount++;
	}

	public void onRoundEnded(RoundEndedEvent event) {
		gunController.cleanUpRound();
	}

//	private double distanceTo(Point2D.Double from, Point2D.Double to) {
//		return Math.hypot(to.x - from.x, to.y - from.y);
//	}

	public double absoluteAngleTo(double x, double y) {
		double angle = Math.atan2(x - getX(), y - getY());
		if (angle > 0) {
			return angle;
		} else {
			return Math.PI * 2 + angle;
		}
	}

	// =======================================================================
	// TEST WAVESURFING
	// =======================================================================
	Point2D.Double _myLocation;
	public Point2D.Double _enemyLocation; // enemy bot's location
	ArrayList<EnemyWave> _enemyWaves;
	public ArrayList<Integer> _surfDirections;
	public static int BINS = 52;
	public static double _surfStats[] = new double[BINS];
	public ArrayList<Double> _surfAbsBearings;
	// to keep track of enemy energy
	public static double _oppEnergy = 100.0;
	// 800 x 600 battlefield rectangle
	public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(
			18, 18, 764, 564);

	public void testRun() {
		_enemyWaves = new ArrayList<EnemyWave>();
		_surfDirections = new ArrayList<Integer>();
		_surfAbsBearings = new ArrayList<Double>();
	}

	public void testOnScannedRobot(ScannedRobotEvent e) {
		_myLocation = new Point2D.Double(getX(), getY());
		double latVel = getVelocity() * Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + getHeadingRadians();

		_surfDirections.add(0, new Integer((latVel >= 0) ? 1 : -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

		double bulletPower = _oppEnergy - e.getEnergy();
		if (bulletPower <= 3 && bulletPower >= 0.1
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - 1;
			ew.bulletVelocity = bulletVelocity(bulletPower);
			ew.distanceTraveled = bulletVelocity(bulletPower);
			ew.direction = ((Integer) _surfDirections.get(2)).intValue();
			ew.directAngle = ((Double) _surfAbsBearings.get(2)).doubleValue();
			ew.fireLocation = (Point2D.Double) _enemyLocation.clone(); // last
																		// tick

			_enemyWaves.add(ew);
		}
		_oppEnergy = e.getEnergy();
		// update waves
		_enemyLocation = project(_myLocation, absBearing, e.getDistance());
		updateWaves();
		doSurfing();
	}

	public void testOnHitBullet(HitByBulletEvent e) {
		// If the _enemyWaves collection is empty, we must have missed the
		// detection of this wave somehow.
		if (!_enemyWaves.isEmpty()) {
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet()
					.getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for (int x = 0; x < _enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled
						- _myLocation.distance(ew.fireLocation)) < 50
						&& Math.abs(bulletVelocity(e.getBullet().getPower())
								- ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
			}
		}
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) {
			return;
		}

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI / 2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI / 2), 1);
		}

		setBackAsFront(this, goAngle);
	}

	class EnemyWave {
		Point2D.Double fireLocation;
		long fireTime;
		double bulletVelocity, directAngle, distanceTraveled;
		int direction;

		public EnemyWave() {
		}
	}

	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);
			double distance = _myLocation.distance(ew.fireLocation)
					- ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}

	// got this from RaikoMicro, by Jamougha, but I think it's used by many
	// authors
	// - returns the absolute angle (in radians) from source to target points
	public static double absoluteBearing(Point2D.Double source,
			Point2D.Double target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point2D.Double botLocation, double angle,
			int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {

		double angle = Utils.normalRelativeAngle(goAngle
				- robot.getHeadingRadians());

		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1 * angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}

	// CREDIT: mini sized predictor from Apollon, by rozu
	// http://robowiki.net?Apollon
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) _myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(predictedPosition,
					absoluteBearing(surfWave.fireLocation, predictedPosition)
							+ (direction * (Math.PI / 2)), direction)
					- predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d
					* (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir
					: moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.fireLocation) < surfWave.distanceTraveled
					+ (counter * surfWave.bulletVelocity)
					+ surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);

		return predictedPosition;
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, calculate the index into our stat array for that factor.
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation) - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int) limit(0, (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}

	// CREDIT: from CassiusClay, by PEZ
	// - returns point length away from sourceLocation, at angle
	// robowiki.net?CassiusClay
	public static Point2D.Double project(Point2D.Double sourceLocation,
			double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
				sourceLocation.y + Math.cos(angle) * length);
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

	public static double bulletVelocity(double power) {
		return (20D - (3D * power));
	}

	public void updateWaves() {
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
			if (ew.distanceTraveled > _myLocation.distance(ew.fireLocation) + 50) {
				_enemyWaves.remove(x);
				x--;
			}
		}
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < BINS; x++) {
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			_surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}
}
