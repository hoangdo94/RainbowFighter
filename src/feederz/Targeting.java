package feederz;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import robocode.AdvancedRobot;
import robocode.Rules;

public class Targeting {
	private AdvancedRobot robot;
	private ArrayList<RobotData> dataSeries;
	
	public Targeting(AdvancedRobot robot, ArrayList<RobotData> dataSeries) {
		this.robot = robot;
		this.dataSeries = dataSeries;
	}
	
	private double absoluteBearing(double x1, double y1, double x2, double y2) {
		double xo = x2-x1;
		double yo = y2-y1;
		double hyp = Point2D.distance(x1, y1, x2, y2);
		double arcSin = Math.toDegrees(Math.asin(xo / hyp));
		double bearing = 0;

		if (xo > 0 && yo > 0) { // both pos: lower-Left
			bearing = arcSin;
		} else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
			bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
		} else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
			bearing = 180 - arcSin;
		} else if (xo < 0 && yo < 0) { // both neg: upper-right
			bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
		}

		return bearing;
	}

	private double normalizeBearing(double angle) {
		while (angle >  180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}

	public void setGunHeadingToPoint(Point2D point) {
		double absDeg = absoluteBearing(robot.getX(), robot.getY(), point.getX(), point.getY());
		robot.setTurnGunRight(normalizeBearing(absDeg - robot.getGunHeading()));
	}
	
	public double calculateBulletPower() {
		return Rules.MAX_BULLET_POWER * Math.min(1, 100/dataSeries.get(dataSeries.size() - 1).distance);
	}
	
	public Point2D.Double guessPosition(double diff, double velocity, double changehead) {
		RobotData enemyData = dataSeries.get(dataSeries.size() - 1);
		double newX, newY;

		if (Math.abs(changehead) > 0.00001) {
			double radius = velocity/changehead;
			double tothead = diff * changehead;
			newY = enemyData.position.y + (Math.sin(enemyData.headingRadians + tothead) * radius) - 
					(Math.sin(enemyData.headingRadians) * radius);
			newX = enemyData.position.x + (Math.cos(enemyData.headingRadians) * radius) - 
					(Math.cos(enemyData.headingRadians + tothead) * radius);
		}

		else {
			newY = enemyData.position.y + Math.cos(enemyData.headingRadians) * velocity * diff;
			newX = enemyData.position.x + Math.sin(enemyData.headingRadians) * velocity * diff;
		}
		return new Point2D.Double(newX, newY);
	}
	
	private double getMediumVelocity(int num) {
		int size = dataSeries.size();
		if (num >= size)
			num = size - 1;
		double v = 0;
		for (int i = 0; i < num; i++) {
			v += dataSeries.get(size - num - 1).velocity;
		}
		return v / num;
	}

	private double getMediumChangehead(int num) {
		int size = dataSeries.size();
		if (num >= size - 1)
			num = size - 2;
		double c = 0;
		for (int i = 0; i < num - 1; i++) {
			c += dataSeries.get(size - num - 1).headingRadians
					- dataSeries.get(size - num - 2).headingRadians;
		}
		return c / (num - 1);
	}
	
	public double aimCircular(double bulletPower) {
		RobotData enemyData = dataSeries.get(dataSeries.size() - 1);
		double bulletSpeed = Rules.getBulletSpeed(bulletPower);
		double changehead = getMediumChangehead(10);
		double velocity = getMediumVelocity(10);
		double diff = 0;
		Point2D p = enemyData.position;

		for (int i = 0; i < enemyData.distance/20; i++) {
			diff = Math.sqrt(Math.pow(p.getX() - robot.getX(), 2) + Math.pow(p.getY() - robot.getY(), 2))/ bulletSpeed;
			p = guessPosition(diff, velocity, changehead);
		}

		setGunHeadingToPoint(p);
		return diff;
	}

	public double aimAndReturnEstimateTime(double bulletPower) {
		return aimCircular(bulletPower);
	}

}
