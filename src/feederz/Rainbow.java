/**
 * Authors: Hieu + Hoang
 * Created: April 29
 */
package feederz;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
		// double offset = 0;
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
		if (!this.times.isEmpty()
				&& getTime() > (this.times.getFirst() - offset)

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
		// System.out.println(this.direction);
		// System.out.println("Distance = " + robotData.distance);
		if (this.injuredTime <= 1) {
			controllRobotMode1();
			// System.out.println("Robot Controlling System: Mode 1");
		} else {
			controllRobotMode2();
			// System.out.println("Robot Controlling System: Mode 2");
		}
		// TODO
	}

	public void run() {
		testRun();
		initializeRobot();
		changeAllFuckingColors();
		// System.out.println(choosingLargestRadius(new Point2D.Double(500,
		// 300),
		// new Point2D.Double(500, 400)));
//		if (!isInGoodStartPosition()) {
//			turnRightRadians(absoluteAngleTo(400, 300) - getHeadingRadians());
//			ahead(distanceTo(new Point2D.Double(getX(), getY()),
//					new Point2D.Double(400, 300)));
//		}
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
			BulletTracker bt = new BulletTracker(this, bullet,
					robotData.enemyName, time, 1);
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
//		controlRobot();
		timingHandler();
		aimNfire();

		testOnScannedRobot(e);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		changeAllFuckingColors();
		this.injuredTime++;
		if (this.injuredTime >= 3) {
			this.injuredTime = 0;
			// this.direction = this.direction * -1;
		}
		
		testOnHitBullet(e);
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

	// ==============================================================
	Point2D.Double _myLocation;
    public Point2D.Double _enemyLocation;  // enemy bot's location
	ArrayList _enemyWaves;
    public ArrayList _surfDirections;
    public static int BINS = 52;
    public static double _surfStats[] = new double[BINS];
    public ArrayList _surfAbsBearings;
 // to keep track of enemy energy
    public static double _oppEnergy = 100.0;
    // 800 x 600 battlefield rectangle
    public static Rectangle2D.Double _fieldRect
        = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    
	public void testRun(){
		_enemyWaves = new ArrayList();
		_surfDirections = new ArrayList();
		_surfAbsBearings = new ArrayList();
	}
	
	public void testOnScannedRobot(ScannedRobotEvent e) {
		_myLocation = new Point2D.Double(getX(), getY());
        double latVel = getVelocity()*Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + getHeadingRadians();

		_surfDirections.add(0,
	            new Integer((latVel >= 0) ? 1 : -1));
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));

        double bulletPower = _oppEnergy - e.getEnergy();
		if (bulletPower <= 3 && bulletPower >= 0.1
	            && _surfDirections.size() > 2) {
	            EnemyWave ew = new EnemyWave();
	            ew.fireTime = getTime() - 1;
	            ew.bulletVelocity = bulletVelocity(bulletPower);
	            ew.distanceTraveled = bulletVelocity(bulletPower);
	            ew.direction = ((Integer)_surfDirections.get(2)).intValue();
	            ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
	            ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
	 
	            _enemyWaves.add(ew);
		}
	            _oppEnergy = e.getEnergy();
	         // update waves
	            _enemyLocation = project(_myLocation, absBearing, e.getDistance());
	            updateWaves();
	            doSurfing();
	}
	
	public void testOnHitBullet(HitByBulletEvent e){
		// If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;
 
            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < _enemyWaves.size(); x++) {
                EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled -
                    _myLocation.distance(ew.fireLocation)) < 50
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
    	Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
    	double predictedVelocity = getVelocity();
    	double predictedHeading = getHeadingRadians();
    	double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
    	do {
    		moveAngle =
                wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;
    		moveDir = 1;
 
    		if(Math.cos(moveAngle) < 0) {
    			moveAngle += Math.PI;
    			moveDir = -1;
    		}
 
    		moveAngle = Utils.normalRelativeAngle(moveAngle);
 
    		// maxTurning is built in like this, you can't turn more then this in one tick
    		maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
    		predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + limit(-maxTurning, moveAngle, maxTurning));
 
    		// this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to breack down
    		// otherwise you want to accelerate (look at the factor "2")
    		predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
    		predictedVelocity = limit(-8, predictedVelocity, 8);
 
    		// calculate the new predicted position
    		predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
    	} while(!intercepted && counter < 500);
 
    	return predictedPosition;
    }
    
 // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
            - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
            / maxEscapeAngle(ew.bulletVelocity) * ew.direction;
 
        return (int)limit(0,
            (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
            BINS - 1);
    }
    
 // CREDIT: from CassiusClay, by PEZ
    //   - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
            sourceLocation.y + Math.cos(angle) * length);
    }
    
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
    
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }
    
    public static double bulletVelocity(double power) {
        return (20D - (3D*power));
    }
    
    public void updateWaves() {
        for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
 
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                _myLocation.distance(ew.fireLocation) + 50) {
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
