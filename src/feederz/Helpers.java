package feederz;
import java.awt.geom.Point2D;

import robocode.*;

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
}
