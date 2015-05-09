package vn.edu.hcmut.ai;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.Condition;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class GFTargeting {
	private AdvancedRobot robot;
	private static double lateralDirection;
	private static double lastEnemyVelocity;
	
	public GFTargeting(AdvancedRobot robot) {
		this.robot = robot;
	}
	
	public void updateData(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		double enemyDistance = e.getDistance();
		double enemyVelocity = e.getVelocity();
		if (enemyVelocity != 0) {
			lateralDirection = Helpers.sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
		}
		BulletWave wave = new BulletWave(robot);
		wave.gunLocation = new Point2D.Double(robot.getX(), robot.getY());
		BulletWave.targetLocation = Helpers.project(wave.gunLocation, enemyAbsoluteBearing, enemyDistance);
		wave.lateralDirection = lateralDirection;
		wave.bulletPower = calcBulletPower(e);
		wave.setSegmentations(enemyDistance, enemyVelocity, lastEnemyVelocity);
		lastEnemyVelocity = enemyVelocity;
		wave.bearing = enemyAbsoluteBearing;
		robot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - robot.getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
		robot.setFire(wave.bulletPower);
		if (robot.getEnergy() >= wave.bulletPower) {
			robot.addCustomEvent(wave);
		}
		robot.setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - robot.getRadarHeadingRadians()) * 2);
	}
	
	public double calcBulletPower(ScannedRobotEvent e) {
		double bulletPower = 0;
		bulletPower = e.getDistance() > 150 ? 1.9 : 3;
		bulletPower = Math.min(bulletPower, (e.getEnergy() + .1) / 4);
		if (bulletPower * 6 >= robot.getEnergy()) bulletPower = robot.getEnergy() / 6;
		if (bulletPower >= robot.getEnergy() - .1) bulletPower = robot.getEnergy() - .1;
		bulletPower = Math.max(Rules.MIN_BULLET_POWER, Math.min(Rules.MAX_BULLET_POWER, bulletPower));
		return bulletPower;
	}
}

class BulletWave extends Condition {
	static Point2D targetLocation;

	double bulletPower;
	Point2D gunLocation;
	double bearing;
	double lateralDirection;

	private static final double MAX_DISTANCE = 900;
	private static final int DISTANCE_INDEXES = 5;
	private static final int VELOCITY_INDEXES = 5;
	private static final int BINS = 25;
	private static final int MIDDLE_BIN = (BINS - 1) / 2;
	private static final double MAX_ESCAPE_ANGLE = 0.7;
	private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double)MIDDLE_BIN;
	
	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];

	private int[] buffer;
	private AdvancedRobot robot;
	private double distanceTraveled;
	
	BulletWave(AdvancedRobot _robot) {
		this.robot = _robot;
	}
	
	public boolean test() {
		advance();
		if (hasArrived()) {
			buffer[currentBin()]++;
			robot.removeCustomEvent(this);
		}
		return false;
	}

	double mostVisitedBearingOffset() {
		return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
	}
	
	void setSegmentations(double distance, double velocity, double lastVelocity) {
		int distanceIndex = (int)(distance / (MAX_DISTANCE / DISTANCE_INDEXES));
		int velocityIndex = (int)Math.abs(velocity / 2);
		int lastVelocityIndex = (int)Math.abs(lastVelocity / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
	}

	private void advance() {
		distanceTraveled += Rules.getBulletSpeed(bulletPower);
	}

	private boolean hasArrived() {
		return distanceTraveled > gunLocation.distance(targetLocation) - 18;
	}
	
	private int currentBin() {
		int bin = (int)Math.round(((Utils.normalRelativeAngle(Helpers.absoluteBearing(gunLocation, targetLocation) - bearing)) /
				(lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
		return Helpers.minMax(bin, 0, BINS - 1);
	}
	
	private int mostVisitedBin() {
		int mostVisited = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (buffer[i] > buffer[mostVisited]) {
				mostVisited = i;
			}
		}
		return mostVisited;
	}	
}