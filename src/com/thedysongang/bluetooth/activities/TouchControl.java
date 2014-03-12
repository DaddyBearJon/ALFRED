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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

public class TouchControl extends BluetoothActivity implements OnTouchListener, SurfaceHolder.Callback
{
	private TextView tvTouchX, tvTouchY, tvDegrees;
	private Button bToggle;
	private SurfaceView svTouchArea;
	private SurfaceHolder svTouchAreaHolder;
	private Canvas canvas;
	private Bitmap joystick;
	private int touchX, touchY, Degrees;
	private float theta, thetaDeg;
	private boolean running;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.touch_control);

		tvTouchX = (TextView) findViewById(R.id.tvTouchX); 
		tvTouchX.setText("X: 0");
		tvTouchY = (TextView) findViewById(R.id.tvTouchY); 
		tvTouchY.setText("Y: 0");
		tvDegrees = (TextView) findViewById(R.id.tvDegrees);
		tvDegrees.setText("0" + "°");
		 

		svTouchArea = (SurfaceView) findViewById(R.id.svTouchArea);
		// Needed to make the SurfaceView background transparent
		svTouchArea.setZOrderOnTop(true);
		svTouchAreaHolder = svTouchArea.getHolder();
		svTouchAreaHolder.addCallback(this);
		svTouchAreaHolder.setFormat(PixelFormat.TRANSPARENT);
		svTouchArea.setOnTouchListener(this);

		joystick = BitmapFactory.decodeResource(getResources(), R.drawable.joystick);

		bToggle = (Button) findViewById(R.id.bToggle);
	}

	@Override
	protected void onResume()
	{
		theta = 0;
		thetaDeg = 0;
		Degrees = 0;
		super.onResume();
	}

	public boolean onTouch(View v, MotionEvent event)
	{
		switch(event.getAction())
		{
		case MotionEvent.ACTION_UP:
			// Stop robot
			touchX = 0;
			touchY = 0;
			theta = 0;
			thetaDeg = 0;
			Degrees = 0;
			drawJoystick(v.getWidth() / 2, v.getHeight() / 2);
			write("s,0,0");
			break;
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			touchX = (int) (100 * (2 * (float) event.getX() / v.getWidth() - 1));
			touchY = (int) (100 * (1 - 2 * (float) event.getY() / v.getHeight()));
			
			theta = (float) Math.atan2(touchX, touchY); // Convert Cartesian Coordinates to Radians Seems Android has X and Y from Landscape PErspective
			thetaDeg = (float) Math.toDegrees(theta);  // Convert from Radians to Degrees
			
			if (thetaDeg < 0.0) // atan2 gives range of -pi to + pi, so we get -180 to + 180 we want 0 - 360
			{
				thetaDeg = (float) (360.0 + thetaDeg); 
			}
			
			Degrees = (int) thetaDeg;

			drawJoystick(event.getX(), event.getY());

	/*		moveLeft = touchY + touchX;
			moveRight = touchY - touchX;

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

			if(running)
			{
				write("s," + moveLeft + "," + moveRight);
			} */
			break;
		}
		tvTouchX.setText("X: " + touchX);
		tvTouchY.setText("Y: " + touchY);
		tvDegrees.setText(Degrees + "°");
		return true;
	}

	public void buttonClick(View v)
	{
		running = !running;
		if(running)
		{
			bToggle.setText(R.string.stop);
		}
		else
		{
			bToggle.setText(R.string.start);
		}
	}

	private void drawJoystick(float x, float y)
	{
		canvas = svTouchAreaHolder.lockCanvas();
		canvas.drawColor(0, Mode.CLEAR);
		canvas.drawBitmap(joystick, x - joystick.getWidth() / 2, y - joystick.getHeight() / 2, null);
		svTouchAreaHolder.unlockCanvasAndPost(canvas);
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
	{
		// Not used
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		// Draw the joy stick on the surface as soon as ready
		drawJoystick(svTouchArea.getWidth() / 2, svTouchArea.getHeight() / 2);
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// Not used
	}
}
