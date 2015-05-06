package feederz;
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
}
