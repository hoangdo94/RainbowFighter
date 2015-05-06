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
	public RobotData robotData = new RobotData(this);
	public ArrayList<RobotData> dataSeries = new ArrayList<RobotData>();
	Color colors[] = { new Color(255, 0, 0), new Color(255, 127, 0),
			new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(75, 0, 130), new Color(143, 0, 255) };
	static int colorNum = -1;
	public int aimMode = 2;
	public Point2D.Double ourRobotPosition; // our robot position
	// public Point2D.Double _enemyLocation; // enemy bot's location
	public ArrayList<EnemyWave> enemyWaves;
	public ArrayList<Integer> directionArray;
	public static final int BINS = 52;
	public static double statArray[] = new double[BINS];
	public ArrayList<Double> absBearingsArray;
	public static double adversaryEnergy = 100.0; // Last known adversary's
	public static final int BATTLEFIELD_WIDTH = 800;
	public static final int BATTLEFIELD_HEIGHT = 600;
	public static final int BULLET_FIRING_TIME_DELTA = 1;
	// 800 x 600 battlefield rectangle
	static final int BOUNDARY_SIZE = 18;
	public static Rectangle2D.Double playingRectangle = new java.awt.geom.Rectangle2D.Double(
			BOUNDARY_SIZE, BOUNDARY_SIZE, BATTLEFIELD_WIDTH - BOUNDARY_SIZE,
			BATTLEFIELD_HEIGHT - BOUNDARY_SIZE);
	DCTargeting gunController = new DCTargeting(this, robotData);

	public void initializeRobot() {
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		gunController.init();
		setMaxVelocity(8);
		// Initialize variables
		enemyWaves = new ArrayList<EnemyWave>();
		directionArray = new ArrayList<Integer>();
		absBearingsArray = new ArrayList<Double>();
	}

	public void run() {
		initializeRobot();
		if (colorNum == -1) {
			// Neu la round dau tien
			colorNum = 0;
			changeAllColors();
		}
		do {
			turnRadarRight(Double.POSITIVE_INFINITY);
		} while (true);
	}

	public void changeAllColors() {
		Color c = colors[colorNum];
		setColors(c, c, c, Color.white, c);
		colorNum++;
		if (colorNum > 6)
			colorNum = 0;
	}

	public void controllRadar() {
		double absBearing = robotData.bearingRadians + getHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing
				- getRadarHeadingRadians()) * 2);
	}

	public void controllRobot() {
		EnemyWave comingWave = getClosestSurfableWave();

		if (comingWave == null) {
			return;
		}

		double dangerLeft = checkDanger(comingWave, -1);
		double dangerRight = checkDanger(comingWave, 1);

		double goAngle = Helpers.getAbsoluteBearingAngle(comingWave.fireLocation,
				ourRobotPosition);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(ourRobotPosition, goAngle - (Math.PI / 2),
					-1);
		} else {
			goAngle = wallSmoothing(ourRobotPosition, goAngle + (Math.PI / 2),
					1);
		}

		goToAngle(this, goAngle);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		// robotData.updateData(e);
		dataSeries.add(robotData);
		// Update our robot position
		ourRobotPosition = new Point2D.Double(getX(), getY());
		double latVel = getVelocity() * Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + getHeadingRadians();

		directionArray.add(0, new Integer((latVel >= 0) ? 1 : -1));
		absBearingsArray.add(0, new Double(absBearing + Math.PI));

		double bulletPower = adversaryEnergy - e.getEnergy();
		if (bulletPower <= 3 && bulletPower >= 0.1 && directionArray.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - BULLET_FIRING_TIME_DELTA;
			ew.bulletVelocity = Helpers.getBulletVelocity(bulletPower);
			ew.distanceTraveled = Helpers.getBulletVelocity(bulletPower);
			ew.direction = ((Integer) directionArray.get(2)).intValue();
			ew.directAngle = ((Double) absBearingsArray.get(2)).doubleValue();
			ew.fireLocation = (Point2D.Double) this.robotData.position.clone(); // last
			// tick

			enemyWaves.add(ew);
		}
		adversaryEnergy = e.getEnergy();
		robotData.updateData(e);
		updateWaves();
		controllRobot();
		controllRadar();
		gunController.updateData();
		gunController.setTurnAndFire();
	}

	public void onHitByBullet(HitByBulletEvent e) {
		changeAllColors();
		if (!enemyWaves.isEmpty()) {
			Bullet hitBullet = e.getBullet();
			Point2D.Double hitBulletLocation = new Point2D.Double(
					hitBullet.getX(), hitBullet.getY());
			EnemyWave hitWave = null;

			for (int x = 0; x < enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled
						- ourRobotPosition.distance(ew.fireLocation)) < 50
						&& Math.abs(Helpers.getBulletVelocity(e.getBullet().getPower())
								- ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				updateStatArray(hitWave, hitBulletLocation);
				enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
			}
		}
	}
	
	public void onBulletHit(BulletHitEvent e) {
		gunController.hitCount++;
	}
	
	public void onRoundEnded(RoundEndedEvent event) {
		gunController.cleanUp();
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
		double closestDistance = Double.POSITIVE_INFINITY;
		EnemyWave closestWave = null;

		for (int x = 0; x < enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(x);
			double distance = ourRobotPosition.distance(ew.fireLocation)
					- ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance) {
				closestWave = ew;
				closestDistance = distance;
			}
		}

		return closestWave;
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = calculateIndex(surfWave,
				predictPosition(surfWave, direction));

		return statArray[index];
	}

	// CREDIT: Iterative WallSmoothing by Kawigi
	// - return absolute angle to move at after account for WallSmoothing
	// robowiki.net?WallSmoothing
	public double wallSmoothing(Point2D.Double botLocation, double angle,
			int orientation) {
		while (!playingRectangle.contains(RobotData
				.getPositionFromAngleAndDistance(botLocation, angle, 160))) {
			angle += orientation * 0.05;
		}
		return angle;
	}

	public static void goToAngle(AdvancedRobot robot, double goAngle) {

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
		Point2D.Double predictedPosition = (Point2D.Double) ourRobotPosition
				.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = wallSmoothing(
					predictedPosition,
					Helpers.getAbsoluteBearingAngle(surfWave.fireLocation,
							predictedPosition) + (direction * (Math.PI / 2)),
					direction)
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
					+ Helpers.getSuitableValueInRange(-maxTurning, moveAngle,
							maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir
					: moveDir);
			predictedVelocity = Helpers.getSuitableValueInRange(-8, predictedVelocity,
					8);

			// calculate the new predicted position
			predictedPosition = RobotData.getPositionFromAngleAndDistance(
					predictedPosition, predictedHeading, predictedVelocity);

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
	public static int calculateIndex(EnemyWave ew,
			Point2D.Double hittingPosition) {
		double offsetAngle = (Helpers.getAbsoluteBearingAngle(ew.fireLocation,
				hittingPosition) - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ Helpers.maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int) Helpers.getSuitableValueInRange(0, (factor * ((BINS - 1) / 2))
				+ ((BINS - 1) / 2), BINS - 1);
	}

	public void updateWaves() {
		for (int x = 0; x < enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(x);

			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
			// remove this wave if it pass 50 over out robot
			if (ew.distanceTraveled > ourRobotPosition
					.distance(ew.fireLocation) + 50) {
				enemyWaves.remove(x);
				x--;
			}
		}
	}

	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	public void updateStatArray(EnemyWave ew, Point2D.Double targetLocation) {
		int index = calculateIndex(ew, targetLocation);
		for (int x = 0; x < BINS; x++) {
			statArray[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}
}
