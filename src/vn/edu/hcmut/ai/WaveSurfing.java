package vn.edu.hcmut.ai;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import sun.security.x509.DeltaCRLIndicatorExtension;

public class WaveSurfing {
	AdvancedRobot ourRobot;
	public Point2D.Double ourRobotPosition; // our robot position
	public Point2D.Double enemyPosition;
	public static ArrayList<EnemyWave> enemyWaves;
	public static ArrayList<Integer> directionArray;
	public static final int BINS = 47;
	public static double statArray[] = new double[BINS];
	public static ArrayList<Double> absBearingsArray;
	public static double adversaryEnergy = 100.0; // Last known adversary's
	public static final int BULLET_FIRING_TIME_DELTA = 1;
	public static final int WALL_STICK = 160;
	public final double ROBOT_SIZE = 50;
	// 800 x 600 battlefield rectangle
	public static final int BATTLEFIELD_WIDTH = 800;
	public static final int BATTLEFIELD_HEIGHT = 600;
	static final int BOUNDARY_SIZE = 18;
	public static Rectangle2D.Double playingRectangle = new Rectangle2D.Double(
			BOUNDARY_SIZE, BOUNDARY_SIZE,
			BATTLEFIELD_WIDTH - BOUNDARY_SIZE * 2, BATTLEFIELD_HEIGHT
					- BOUNDARY_SIZE * 2);
	public final int MAX_PREDICTION_TICK_NUMBER = 800;
	public final int MAX_SURFING_DISTANCE = 400;

	public double hitTime = 0;

	public WaveSurfing(AdvancedRobot robot) {
		this.ourRobot = robot;
		enemyWaves = new ArrayList<EnemyWave>();
		directionArray = new ArrayList<Integer>();
		absBearingsArray = new ArrayList<Double>();
	}

	public double getPerfectAngleToGo() {
		EnemyWave comingWave = getClosestSurfableWave();

		if (comingWave == null) {
			return Double.POSITIVE_INFINITY;
		}

		double dangerLeft = checkDanger(comingWave, -1);
		double dangerRight = checkDanger(comingWave, 1);

		double goAngle = Helpers.getAbsoluteBearingAngle(
				comingWave.fireLocation, ourRobotPosition);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(ourRobotPosition, goAngle - (Math.PI / 2),
					-1);
		} else {
			goAngle = wallSmoothing(ourRobotPosition, goAngle + (Math.PI / 2),
					1);
		}
		return goAngle;
	}
	/**
	 * Update necessary information for controlling robot movement
	 * @param e
	 */
	public void updateData(ScannedRobotEvent e) {
		// Update our robot position
		ourRobotPosition = new Point2D.Double(ourRobot.getX(), ourRobot.getY());
		double latVel = ourRobot.getVelocity()
				* Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians()
				+ ourRobot.getHeadingRadians();

		directionArray.add(0, new Integer((latVel >= 0) ? 1 : -1));
		absBearingsArray.add(0, new Double(absBearing + Math.PI));

		double bulletPower = adversaryEnergy - e.getEnergy();
		// Check if it is a fake wave
		double deltaHitTime = (this.ourRobot.getTime() - BULLET_FIRING_TIME_DELTA)
				- this.hitTime;
		if (bulletPower <= 3 && bulletPower >= 0.1 && directionArray.size() > 2) {
			System.out.print("Checking ");
			if (Math.abs(deltaHitTime) >= 3) {
				EnemyWave ew = new EnemyWave();
				ew.fireTime = ourRobot.getTime() - BULLET_FIRING_TIME_DELTA;
				ew.bulletVelocity = Helpers.getBulletVelocity(bulletPower);
				ew.distanceTraveled = Helpers.getBulletVelocity(bulletPower);
				ew.direction = ((Integer) directionArray.get(2)).intValue();
				ew.directAngle = ((Double) absBearingsArray.get(2))
						.doubleValue();
				ew.fireLocation = (Point2D.Double) this.enemyPosition.clone(); // last
				// tick

				enemyWaves.add(ew);
				System.out.println("real");
			} else {
				System.out.println("fake");
			}
		}
		adversaryEnergy = e.getEnergy();
		this.enemyPosition = Helpers.getPositionFromAngleAndDistance(
				this.ourRobotPosition, absBearing, e.getDistance());
		updateWaves();
	}

	public void onBulletHit(BulletHitEvent e) {
		this.hitTime = this.ourRobot.getTime();
	}

	public void onHitByBullet(HitByBulletEvent e) {
		if (!enemyWaves.isEmpty()) {
			Bullet hitBullet = e.getBullet();
			Point2D.Double hitBulletLocation = new Point2D.Double(
					hitBullet.getX(), hitBullet.getY());
			EnemyWave hitWave = null;

			for (int x = 0; x < enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled
						- ourRobotPosition.distance(ew.fireLocation)) < 50
						&& Math.abs(Helpers.getBulletVelocity(e.getBullet()
								.getPower()) - ew.bulletVelocity) < 0.001) {
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

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		if (!enemyWaves.isEmpty()) {
			Bullet hitBullet = e.getHitBullet();
			Point2D.Double hitBulletLocation = new Point2D.Double(
					hitBullet.getX(), hitBullet.getY());
			EnemyWave hitWave = null;

			for (int x = 0; x < enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled
						- ew.fireLocation.distance(hitBulletLocation)) <= 40
				/*
				 * && Math.abs(Helpers.getBulletVelocity(hitBullet .getPower())
				 * - ew.bulletVelocity) < 0.001
				 */) {
					hitWave = ew;
					System.out.println("Bullet hit by Bulet");
					break;
				}
			}

			if (hitWave != null) {
				updateStatArray(hitWave, hitBulletLocation);
				enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
			}
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

	public double wallSmoothing(Point2D.Double botLocation, double angle,
			int orientation) {
		Point2D.Double guesingPosition = Helpers
				.getPositionFromAngleAndDistance(botLocation, angle, WALL_STICK);
		while (!playingRectangle.contains(guesingPosition)) {
			angle += orientation * 0.05;
			guesingPosition = Helpers.getPositionFromAngleAndDistance(
					botLocation, angle, WALL_STICK);
		}
		return angle;
	}

	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) ourRobotPosition
				.clone();
		double predictedVelocity = ourRobot.getVelocity();
		double predictedHeading = ourRobot.getHeadingRadians();
		double maxTurning, moveAngle, moveDir;
		double robotDistance, bulletDistance;
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
			predictedVelocity = Helpers.getSuitableValueInRange(-8,
					predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = Helpers.getPositionFromAngleAndDistance(
					predictedPosition, predictedHeading, predictedVelocity);

			counter++;
			robotDistance = predictedPosition.distance(surfWave.fireLocation);
			bulletDistance = surfWave.distanceTraveled
					+ (counter * surfWave.bulletVelocity)
					+ surfWave.bulletVelocity;
			if (robotDistance < bulletDistance) {
				intercepted = true;
			}
		} while (!intercepted && counter < MAX_PREDICTION_TICK_NUMBER);

		return predictedPosition;
	}

	public static int calculateIndex(EnemyWave ew,
			Point2D.Double hittingPosition) {
		double offsetAngle = (Helpers.getAbsoluteBearingAngle(ew.fireLocation,
				hittingPosition) - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ Helpers.maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int) Helpers.getSuitableValueInRange(0,
				(factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2), BINS - 1);
	}

	public void updateWaves() {
		for (int i = 0; i < enemyWaves.size(); i++) {
			EnemyWave ew = (EnemyWave) enemyWaves.get(i);
			ew.distanceTraveled = (ourRobot.getTime() - ew.fireTime)
					* ew.bulletVelocity;
			if (ew.distanceTraveled > ourRobotPosition
					.distance(ew.fireLocation) + ROBOT_SIZE) {
				enemyWaves.remove(i);
				i--;
			}
		}
	}

	public void updateStatArray(EnemyWave ew, Point2D.Double targetLocation) {
		int index = calculateIndex(ew, targetLocation);
		for (int x = 0; x < BINS; x++) {
			statArray[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}

	class EnemyWave {
		Point2D.Double fireLocation;
		long fireTime;
		double bulletVelocity, directAngle, distanceTraveled;
		int direction;

		public EnemyWave() {
		}
	}

}