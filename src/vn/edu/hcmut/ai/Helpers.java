package vn.edu.hcmut.ai;
import java.awt.geom.Point2D;

import robocode.*;
import robocode.util.Utils;

final class Helpers {
	
	public static double sqr(double x) {
		return x * x;
	}
	
	public static double sinD(double ang) {
		return(Math.sin(Math.toRadians(ang)));
	}
	
	public static double cosD(double ang) {
		return(Math.cos(Math.toRadians(ang)));
	}
	
	public static double tanD(double ang) {
		return(Math.tan(Math.toRadians(ang)));
	}
	
	public static double getAbsoluteBearingAngle(Point2D.Double from,
			Point2D.Double to) {
		return Math.atan2(to.x - from.x, to.y - from.y);
	}
	
	public static double angleTo(double x1, double y1, double x2, double y2) {
		return(Math.toDegrees(Math.PI/2 - Math.atan2(y2 - y1, x2 - x1)));
	}
	
	public static double distanceTo(double x1, double y1, double x2, double y2) {
		return(Math.sqrt(sqr(x2 - x1) + sqr(y2 - y1)));
	}
	
	public static double distanceTo(AdvancedRobot bot, double x2, double y2) {
		return(Math.sqrt(sqr(x2 - bot.getX()) + sqr(y2 - bot.getY())));
	}
	
	public static double normalizeBearing(double ang) {
		ang = ang % 360;
		if (ang > 180) ang -= 360;
		if (ang < -180) ang += 360;
		return ang;
	}
	
	public static double getSuitableValueInRange(double min, double value,
			double max) {
		return Math.max(min, Math.min(value, max));
	}
	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0 / velocity);
	}

	public static double getBulletVelocity(double power) {
		return (20.0 - 3.0 * power);
	}
	
	public static Point2D.Double getPositionFromAngleAndDistance(Point2D.Double from,
			double angle, double length) {
		return new Point2D.Double(from.x + Math.sin(angle) * length,
				from.y + Math.cos(angle) * length);
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
	
	static double bulletVelocity(double power) {
		return 20 - 3 * power;
	}
	
	static Point2D project(Point2D sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
				sourceLocation.getY() + Math.cos(angle) * length);
	}
	
	static double absoluteBearing(Point2D source, Point2D target) {
		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
	}

	static int sign(double v) {
		return v < 0 ? -1 : 1;
	}
	
	static int minMax(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
