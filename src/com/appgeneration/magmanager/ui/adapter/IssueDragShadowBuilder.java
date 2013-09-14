package com.appgeneration.magmanager.ui.adapter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

public class IssueDragShadowBuilder extends View.DragShadowBuilder {

	// The drag shadow image, defined as a drawable thing
	private static Drawable shadow;

	// Defines the constructor for myDragShadowBuilder
	public IssueDragShadowBuilder(View v) {

		// Stores the View parameter passed to myDragShadowBuilder.
		super(v);

		// Creates a draggable image that will fill the Canvas provided by
		// the system.
		shadow = new ColorDrawable(Color.LTGRAY);
	}

	// Defines a callback that sends the drag shadow dimensions and touch
	// point back to the
	// system.
	@Override
	public void onProvideShadowMetrics(Point size, Point touch) {
		
		int shadowSize = 150;
		
		// The drag shadow is a ColorDrawable. This sets its dimensions to
		// be the same as the
		// Canvas that the system will provide. As a result, the drag shadow
		// will fill the
		// Canvas.
		shadow.setBounds(0, 0, shadowSize, shadowSize);

		// Sets the size parameter's width and height values. These get back
		// to the system
		// through the size parameter.
		size.set(shadowSize, shadowSize);

		// Sets the touch point's position to be in the middle of the drag
		// shadow
		touch.set(shadowSize / 2, shadowSize / 2);
	}

	// Defines a callback that draws the drag shadow in a Canvas that the
	// system constructs
	// from the dimensions passed in onProvideShadowMetrics().
	@Override
	public void onDrawShadow(Canvas canvas) {

		// Draws the ColorDrawable in the Canvas passed in from the system.
		shadow.draw(canvas);
	}
}