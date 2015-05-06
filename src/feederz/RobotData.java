package feederz;

import java.awt.Point;
import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;

public class RobotData {
	AdvancedRobot ourRobot;
	double bearingRadians; // radians
	double distance;
	double energy;
	double headingRadians; // radians
	double velocity;
	Point2D.Double position; // absolute position
	boolean decreaseEnergyDetected = false;
	double deltaEnergy;
	String enemyName;
	
	public RobotData(ScannedRobotEvent event, AdvancedRobot robot) {
		ourRobot = robot;
		this.bearingRadians = event.getBearingRadians();
		this.distance = event.getDistance();
		this.energy = event.getEnergy();
		this.headingRadians = event.getHeadingRadians();
		this.velocity = event.getVelocity();
		this.position = getPositionFromScannedRobotEvent(event);
		this.decreaseEnergyDetected = false;
		this.deltaEnergy = 0;
		this.enemyName = event.getName();
	}

	public RobotData(AdvancedRobot robot) {
		this.ourRobot = robot;
		this.bearingRadians = 0;
		this.distance = 0;
		this.energy = 0;
		this.headingRadians = 0;
		this.velocity = 0;
		this.position = new Point2D.Double(0.0, 0.0);
		this.decreaseEnergyDetected = false;
		this.deltaEnergy = 0;
		this.enemyName = "";
	}

	public Point2D.Double getPositionFromScannedRobotEvent(
			ScannedRobotEvent event) {
		double angle = ourRobot.getHeadingRadians() + event.getBearingRadians();
		Point2D.Double from = new Point2D.Double(this.ourRobot.getX(), this.ourRobot.getY());
		return getPositionFromAngleAndDistance(from, angle, this.distance);
	}
	
	public static Point2D.Double getPositionFromAngleAndDistance(Point2D.Double from,
			double angle, double length) {
		return new Point2D.Double(from.x + Math.sin(angle) * length,
				from.y + Math.cos(angle) * length);
	}

	public void updateData(ScannedRobotEvent event) {
		if (this.energy - event.getEnergy() >= 0.4) {
			this.decreaseEnergyDetected = true;
		} else {
			this.decreaseEnergyDetected = false;
		}
		this.deltaEnergy = this.energy - event.getEnergy();
		this.bearingRadians = event.getBearingRadians();
		this.distance = event.getDistance(); // note: need be placed before updating position statement
		this.energy = event.getEnergy();
		this.headingRadians = event.getHeadingRadians();
		this.velocity = event.getVelocity();
		this.position = getPositionFromScannedRobotEvent(event);
		Point2D.Double newPosition = getPositionFromScannedRobotEvent(event);
		this.position.setLocation(newPosition.x, newPosition.y);
		this.enemyName = event.getName();
	}
}