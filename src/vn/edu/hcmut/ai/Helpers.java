package vn.edu.hcmut.ai;
import java.awt.geom.Point2D;

import robocode.*;
import robocode.util.Utils;

final class Helpers {
	//Helpers for Wave Surfing movement
	public static double getAbsoluteBearingAngle(Point2D.Double from,
			Point2D.Double to) {
		return Math.atan2(to.x - from.x, to.y - from.y);
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
	
	//Helpers for Guess Factor targeting
	public static Point2D project(Point2D sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
				sourceLocation.getY() + Math.cos(angle) * length);
	}

	public static int sign(double v) {
		return v < 0 ? -1 : 1;
	}
	
}
