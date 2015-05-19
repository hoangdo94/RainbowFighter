/**
 * Authors: Hieu + Hoang
 * Created: April 29
 */
package vn.edu.hcmut.ai;

import java.awt.Color;

import robocode.*;
import robocode.util.Utils;

public class feederz_spring2015 extends AdvancedRobot {
	Color colors[] = { new Color(255, 0, 0), new Color(255, 127, 0),
			new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 0, 255),
			new Color(75, 0, 130), new Color(143, 0, 255) };
	static int colorNum = -1;
	
	WaveSurfing waveSurfing = new WaveSurfing(this);
	GFTargeting gunController = new GFTargeting(this);

	public void initializeRobot() {
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		//gunController.init();
		setMaxVelocity(8);
	}
	
	public void changeAllColors() {
		Color c = colors[colorNum];
		setColors(c, c, c, Color.white, c);
		colorNum++;
		if (colorNum > 6)
			colorNum = 0;
	}


	public void run() {
		initializeRobot();
		if (colorNum == -1) {
			colorNum = 0;
			changeAllColors();
		}
		do {
			turnRadarRight(Double.POSITIVE_INFINITY);
		} while (true);
	}


	public void controllRadar(ScannedRobotEvent e) {
		double absBearing = e.getBearingRadians() + getHeadingRadians();
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing
				- getRadarHeadingRadians()) * 2);
	}

	public void controllRobot() {
		double goAngle = waveSurfing.getPerfectAngleToGo();
		if (goAngle == Double.POSITIVE_INFINITY)
			return;
		Helpers.goToAngle(this, goAngle);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		waveSurfing.updateData(e);
		//gunController.updateData();
		//gunController.setTurnAndFire();
		gunController.updateData(e);
		controllRadar(e);
		controllRobot();
	}

	public void onHitByBullet(HitByBulletEvent e) {
		changeAllColors();
		waveSurfing.onHitByBullet(e);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){
		waveSurfing.onBulletHitBullet(e);
	}
	
	public void onBulletHit(BulletHitEvent e) {
		//gunController.hitCount++;
		waveSurfing.onBulletHit(e);
	}
	
	public void onRoundEnded(RoundEndedEvent event) {
		//gunController.cleanUp();
	}
}
