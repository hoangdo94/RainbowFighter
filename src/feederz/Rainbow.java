/**
 * Authors: Hieu + Hoang
 * Created: April 29
 */
package feederz;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;

import robocode.*;
import robocode.util.Utils;

public class Rainbow extends AdvancedRobot {
	RobotData robotData = new RobotData(this);
	ArrayList<RobotData> dataSeries = new ArrayList<RobotData>();
	Targeting gunController = new Targeting(this, dataSeries);
	LinkedList<Double> times = new LinkedList<Double>();
	LinkedList<Point2D.Double> positions = new LinkedList<Point2D.Double>();
	Color colors[] = { new Color(255, 0, 0), new Color(255, 127, 0),
			new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(75, 0, 130), new Color(143, 0, 255) };
	int direction = 1;
	int colorNum = 0;
	double wallLimit = 100;
	double radiusLimit = 300 - wallLimit;
	int aimMode = 2;
	double centerXLeft = 300;
	double centerYLeft = 300;
	double centerXRight = 200 + 300;
	double centerYRight = 300;
	double centerX = 400;
	double centerY = 300;
	int injuredTime = 0;
	boolean lockDirection = false;

	public void initializeRobot() {
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		setMaxVelocity(8);
	}

	public void changeAllFuckingColors() {
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

	/**
	 * Get the most suitable (i.e safest angle) that the robot can takes
	 * 
	 * @return angle in radians
	 */
	public double getPerfectAngle() {
//		double offset = 0;
		double angle = 0;
		// if (isAboutToHitWall()) {
		// return (getToCenterAngle() - getHeadingRadians()) + Math.PI / 2;
		// }

		if (robotData.distance > 400) {
			angle = robotData.bearingRadians;
		} else if (robotData.distance <= 400) {
			angle = robotData.bearingRadians + Math.PI / 2;
		} else if (robotData.distance < 200) {
			angle = robotData.bearingRadians + Math.PI / 2 + Math.PI / 5;
		}
		return angle;
	}

	public Point2D.Double getFuturePosition() {
		double bulletVelocity = 20 - 3 * robotData.deltaEnergy;
		double timeForBulletReaching = robotData.distance / bulletVelocity;
		double dis = getVelocity() * timeForBulletReaching;
		double futureX = getX() + dis
				* Math.cos(Math.PI / 2 - getHeadingRadians());
		double futureY = getY() + dis
				* Math.sin(Math.PI / 2 - getHeadingRadians());

		return new Point2D.Double(futureX, futureY);
	}

	public boolean isSafePlace(Point2D.Double consideringPosition,
			Point2D.Double currentPosition) {
		double radius = 10;
		boolean result = false;
		double evaluation = Math.pow(
				(currentPosition.x - consideringPosition.x), 2.0)
				+ Math.pow(currentPosition.y - consideringPosition.y, 2.0);
		if (evaluation > Math.pow(radius, 2.0)) {
			result = false;
		} else if (evaluation <= Math.pow(radius, 2.0)) {
			result = true;
		}
		return result;
	}

	public void controllRobotMode1() {
		double offset = 10;
		// if (isAboutToHitWall())
		// this.direction = this.direction * -1;
		if (!this.times.isEmpty() && getTime() > (this.times.getFirst() - offset)

		/*
		 * && isSafePlace(this.positions.getFirst(), new Point2D.Double( getX(),
		 * getY()))
		 */) {

			// this.direction = this.direction * -1;
			// }
			this.times.removeFirst();
			this.positions.removeFirst();
			double relativeanglengle = getPerfectAngle();
			if (Math.abs(relativeanglengle) > (Math.PI / 2)) {
				if (relativeanglengle < 0) {
					setTurnRightRadians(Math.PI + relativeanglengle);
				} else {
					setTurnLeftRadians(Math.PI - relativeanglengle);
				}
				setBack(100 * this.direction);
			} else {
				if (relativeanglengle < 0) {
					setTurnLeftRadians(-relativeanglengle);
				} else {
					setTurnRightRadians(relativeanglengle);
				}
				setAhead(100 * this.direction);
			}
		}
	}

	public void controllRobotMode2() {
		double offset = 10;
		if (!this.times.isEmpty() && !this.positions.isEmpty()
				&& getTime() > (this.times.getFirst() - offset)
		/*
		 * && isSafePlace(this.positions.getFirst(), new Point2D.Double( getX(),
		 * getY()))
		 */) {
			// if (isAboutToHitWall()) {
			// this.direction = 1;
			// } else {
			if (!this.lockDirection)
				this.direction = this.direction * -1;
			// }
			this.times.removeFirst();
			this.positions.removeFirst();
		}
		// TODO
		double relativeanglengle = getPerfectAngle();
		if (Math.abs(relativeanglengle) > (Math.PI / 2)) {
			if (relativeanglengle < 0) {
				setTurnRightRadians(Math.PI + relativeanglengle);
			} else {
				setTurnLeftRadians(Math.PI - relativeanglengle);
			}
			setBack(100 * this.direction);
		} else {
			if (relativeanglengle < 0) {
				setTurnLeftRadians(-relativeanglengle);
			} else {
				setTurnRightRadians(relativeanglengle);
			}
			setAhead(100 * this.direction);
		}
	}

	public void controllRobotMode3() {
		double relativeanglengle = getPerfectAngle();
		setTurnRightRadians(relativeanglengle);
		setAhead(1000);
	}

	boolean hitWallFlag = false;

	public void controlRobot() {
		if (robotData.distance > 400) {
			this.lockDirection = true;
			this.direction = 1;
		} else {
			this.lockDirection = false;
		}
		if (!isAboutToHitWall()) {
			hitWallFlag = false;
		}
		if (isAboutToHitWallRectangle()
				&& !isAboutToHitWallRectangle() == hitWallFlag) {
			this.direction = this.direction * -1;
			hitWallFlag = true;
		}
//		System.out.println(this.direction);
		//System.out.println("Distance = " + robotData.distance);
		if (this.injuredTime <= 1) {
			controllRobotMode1();
//			 System.out.println("Robot Controlling System: Mode 1");
		} else {
			controllRobotMode2();
//			 System.out.println("Robot Controlling System: Mode 2");
		}
		// TODO
	}

	public void run() {
		initializeRobot();
		changeAllFuckingColors();
		// System.out.println(choosingLargestRadius(new Point2D.Double(500,
		// 300),
		// new Point2D.Double(500, 400)));
		if (!isInGoodStartPosition()) {
			turnRightRadians(absoluteAngleTo(400, 300) - getHeadingRadians());
			ahead(distanceTo(new Point2D.Double(getX(), getY()),
					new Point2D.Double(400, 300)));
		}
		do {
			turnRadarRight(Double.POSITIVE_INFINITY);
		} while (true);
	}

	public void timingHandler() {
		if (robotData.decreaseEnergyDetected) {
			this.times.addLast(getTime() + predictTime(robotData.deltaEnergy));
			this.positions.addLast(getFuturePosition());
		}
	}

	public double predictTime(double firepower) {
		double velocity = 20 - 3 * firepower;
		return robotData.distance / velocity;
	}
	
	public void setFireAndTrackBullet(double power, double time) {
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 10) {
			Bullet bullet = setFireBullet(power);
			@SuppressWarnings("unused")
			BulletTracker bt = new BulletTracker(this, bullet, robotData.enemyName, time, 1);
		}
	}

	public void aimNfire() {
		double power = gunController.calculateBulletPower();
		double time = gunController.aimAndReturnEstimateTime(power);
		setFireAndTrackBullet(power, time);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		robotData.updateData(e);
		dataSeries.add(robotData);
		controllRadar();
		controlRobot();
		timingHandler();
		aimNfire();
	}

	public void onHitByBullet(HitByBulletEvent e) {
		changeAllFuckingColors();
		this.injuredTime++;
		if (this.injuredTime >= 3) {
			this.injuredTime = 0;
			// this.direction = this.direction * -1;
		}
	}
	
	public void onCustomEvent(CustomEvent e) {
		Condition condition = e.getCondition();
		if (condition instanceof BulletTracker) {

			BulletTracker bt = (BulletTracker) condition;
			System.out.print(bt.getAimMethod() + "  ");
			if (bt.hitTarget()) {
				System.out.println("hit");
			} else {
				System.out.println("miss");
			}
		}
	}

	public void onHitWall(HitWallEvent e) {
		// Replace the next line with any behavior you would like
		// back(20);
		System.out.println("Hit wall");
		// setTurnRightRadians(absoluteAngleTo(400, 300)- getHeadingRadians());
		// setAhead(200);
		this.direction = this.direction * -1;
	}

	public double getToCenterAngle() {
		double currX = getX();
		double currY = getY();
		double angle = 0;
		Point2D.Double point = null;
		Point2D.Double pos = new Point2D.Double(getX(), getY());
		if (currX <= centerXLeft) {
			point = choosingLargestRadius(new Point2D.Double(centerXLeft,
					centerYLeft), pos);
			angle = absoluteAngleTo(point.x, point.y);
			// System.out.println("Tam  = " + point.x + ", " + point.y);

		} else if (currX >= centerXRight) {
			point = choosingLargestRadius(new Point2D.Double(centerXRight,
					centerYRight), pos);
			angle = absoluteAngleTo(point.x, point.y);
			// System.out.println("Tam  = " + point.x + ", " + point.y);

		} else {
			double delta = Math.sqrt((Math.pow(300, 2.0) + Math.pow(100, 2.0))) - 300;
			if (currY >= centerY) {
				angle = absoluteAngleTo(centerX - delta, centerY);
			} else {
				angle = absoluteAngleTo(centerX + delta, centerY);
			}
		}
		return angle;
	}

	public boolean isAboutToHitWall() {
		double evaluation = 0;
		if (getX() <= 400) {
			evaluation = Math.pow(getX() - centerXLeft, 2.0)
					+ Math.pow(getY() - centerYLeft, 2.0);
		} else if (getX() >= 400) {
			evaluation = Math.pow(getX() - centerXRight, 2.0)
					+ Math.pow(getY() - centerYRight, 2.0);
		}

		if (evaluation >= Math.pow(this.radiusLimit, 2.0))
			return true;
		else if (getX() > centerXLeft && getX() < centerXRight)
			if (getY() > 600 - this.wallLimit || getY() < this.wallLimit)
				return true;
		return false;
	}

	public boolean isAboutToHitWallRectangle() {
		Double x = getX();
		Double y = getY();
		return (x <= this.wallLimit)
				|| (x >= getBattleFieldWidth() - this.wallLimit)
				|| (y <= this.wallLimit)
				|| (y >= getBattleFieldHeight() - this.wallLimit);
	}

	private double distanceTo(Point2D.Double from, Point2D.Double to) {
		return Math.hypot(to.x - from.x, to.y - from.y);
	}

	public double absoluteAngleTo(double x, double y) {
		double angle = Math.atan2(x - getX(), y - getY());
		if (angle > 0) {
			return angle;
		} else {
			return Math.PI * 2 + angle;
		}
	}

	public Point2D.Double choosingLargestRadius(Point2D.Double firstCenter,
			Point2D.Double pos) {
		double maxRadius = 300 - 50;
		Point2D.Double center = new Point2D.Double(firstCenter.x, firstCenter.y);
		double dis = distanceTo(center, pos);
		double delta = (dis - maxRadius) / (1 - Math.sqrt(2.0) / 2.0);
		double deltaxy = delta * Math.sqrt(2.0) / 2.0;
		System.out.println("printing something");
		System.out.println("x = " + getX() + " y = " + getY());
		System.out.println(distanceTo(center, pos) + " vs " + maxRadius);
		if (dis < maxRadius) {
			return center;
		}
		if (pos.x >= centerXRight) {
			if (pos.y > centerYRight) {
				center.setLocation(center.x + deltaxy, center.y + deltaxy);
			} else {
				center.setLocation(center.x + deltaxy, center.y - deltaxy);
			}
		} else if (pos.x <= centerXLeft) {
			if (pos.y > centerYLeft) {
				center.setLocation(center.x - deltaxy, center.y + deltaxy);
			} else {
				center.setLocation(center.x - deltaxy, center.y - deltaxy);
			}
		}
		System.out.println(center);
		System.out.println("end method");
		return center;
	}

	public boolean isInGoodStartPosition() {
		return (getX() >= 300 && getX() <= 500 && getY() >= 200 && getY() <= 400);
	}
}
