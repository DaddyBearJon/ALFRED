/**
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.thedysongang.bluetooth.activities;

import com.thedysongang.bluetooth.BluetoothActivity;
import com.thedysongang.bluetooth.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * This class makes use of the phones integrated accelerometer in order to
 * control the robot via tilting. The accelerometer sensor is very "jumpy" and
 * changes very frequently, this has necessitated a low-pass filter of sorts.
 */
public class AccelerometerControl extends BluetoothActivity implements SensorEventListener
{
	private AccelerometerSurfaceView svAccelerometer;
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private boolean enabled = false;
	private TextView tvAccX, tvAccY, tvAccZ, tvMoveX, tvMoveY, tvMoveZ;
	private float lastX = 0, lastY = 0, lastZ = 0, accNoise = 0.1f;
	private int moveLeft, moveRight;
	public static Bitmap ball;
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.accelerometer_control);

		// Set up accelerometer output fields
		tvAccX = (TextView) findViewById(R.id.tvAccX);
		tvAccY = (TextView) findViewById(R.id.tvAccY);
		tvAccZ = (TextView) findViewById(R.id.tvAccZ);

		// Show wheel speeds
		tvMoveX = (TextView) findViewById(R.id.tvMoveX);
		tvMoveY = (TextView) findViewById(R.id.tvMoveY);
		tvMoveZ = (TextView) findViewById(R.id.tvMoveZ);

		final Button bToggle = (Button) findViewById(R.id.bToggle);
		bToggle.setOnClickListener(new OnClickListener()
		{
			public void onClick(View arg0)
			{
				enabled = !enabled;
				if(enabled)
				{
					bToggle.setText(R.string.stop);
				}
				else
				{
					bToggle.setText(R.string.start);
					write("r");
				}
			}
		});
		ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
		svAccelerometer = (AccelerometerSurfaceView) findViewById(R.id.svVector);
		// Needed to make the SurfaceView background transparent
		svAccelerometer.setZOrderOnTop(true);
		svAccelerometer.getHolder().setFormat(PixelFormat.TRANSPARENT);
	}

	@Override
	protected void onResume()
	{
		enabled = false;
		lastX = 0;
		lastY = 0;
		accNoise = 0.1f;
		moveLeft = 0;
		moveRight = 0;

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

		svAccelerometer.onResumeMySurfaceView();
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		svAccelerometer.onPauseMySurfaceView();
		sensorManager.unregisterListener(this);
		super.onPause();
	}

	public void onAccuracyChanged(Sensor arg0, int arg1)
	{

	}

	public void onSensorChanged(SensorEvent event)
	{
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];

		// Some accelerometer sensors can change very often for no reason
		boolean change = false;

		if(Math.abs(lastX - x) > accNoise)
		{
			lastX = x;
			change = true;
		}

		if(Math.abs(lastY - y) > accNoise)
		{
			lastY = y;
			change = true;
		}
		
		if(Math.abs(lastZ - z) > accNoise)
		{
			lastZ = z - 9.81f;
			change = true;
		}

		// No significant enough change
		if(!change)
		{
			return;
		}

		// Show accelerator sensor values
		tvAccX.setText("X: " + String.format("%.02f", lastX));
		tvAccY.setText("Y: " + String.format("%.02f", lastY));
		tvAccZ.setText("Z: " + String.format("%.02f", (lastZ)));
		
		// Do now allow higher acceleration values than 5 or lower than -5
		/*if(lastX > 5)
		{
			lastX = 5;
		}
		else if(lastX < -5)
		{
			lastX = -5;
		}

		if(lastY > 5)
		{
			lastY = 5;
		}
		else if(lastY < -5)
		{
			lastY = -5;
		}*/

		// Only change speed by multiples of 5
		moveLeft = (int) (-5 * (Math.round(100 * lastY / 5) / 5) + (-5 * (Math.round(100 * lastX / 5) / 5)));
		moveRight = (int) (-5 * (Math.round(100 * lastY / 5) / 5) - (-5 * (Math.round(100 * lastX / 5) / 5)));

		// Do not allow speed values over 100 or under -100
		if(moveLeft > 100)
		{
			moveLeft = 100;
		}
		else if(moveLeft < -100)
		{
			moveLeft = -100;
		}

		if(moveRight > 100)
		{
			moveRight = 100;
		}
		else if(moveRight < -100)
		{
			moveRight = -100;
		}

		// Show wheel speeds
		tvMoveX.setText("L: " + Integer.toString(moveLeft));
		tvMoveY.setText("R: " + Integer.toString(moveRight));
		tvMoveZ.setText("U: " +  String.format("%.02f", (lastZ)));
		svAccelerometer.setVector(lastX, lastY);

		// Send data to robot
		if(enabled)
		{
			write("d:" + moveLeft + "," + moveRight);
		}
		else
		{
			return;
		}
	}
}
