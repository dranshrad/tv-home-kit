package com.gathin.tvhome;

import android.view.KeyEvent;
import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Home-tv addition (not upstream): the focused tile arrives leaning toward the way the remote was pressed and
 * springs back to flat. Runs only with advanced_effects on and reduced_motion off; nothing moves once it settles.
 */
final class Lean {
    /** The D-pad key that moved focus last; tiles record it before focus leaves them. */
    static int lastDpad = KeyEvent.KEYCODE_DPAD_RIGHT;

    private static final float SIDE_DEGREES = 15f, VERTICAL_DEGREES = 11f;

    private Lean() {
    }

    /** Start the lean on a view that just took focus (the tile, and the glass highlight over it). */
    static void arrive(View v) {
        settle(v);
        boolean vertical = lastDpad == KeyEvent.KEYCODE_DPAD_UP || lastDpad == KeyEvent.KEYCODE_DPAD_DOWN;
        float from = vertical
                ? (lastDpad == KeyEvent.KEYCODE_DPAD_DOWN ? VERTICAL_DEGREES : -VERTICAL_DEGREES)
                : (lastDpad == KeyEvent.KEYCODE_DPAD_RIGHT ? -SIDE_DEGREES : SIDE_DEGREES);
        SpringAnimation spring = new SpringAnimation(v, vertical ? DynamicAnimation.ROTATION_X : DynamicAnimation.ROTATION_Y, 0f);
        spring.setSpring(new SpringForce(0f).setStiffness(150f).setDampingRatio(0.42f));
        spring.setStartValue(from);
        v.setTag(R.id.lean_spring, spring);
        spring.start();
    }

    /** Stop a running lean and lay the view flat (focus left it, or a new lean starts). */
    static void settle(View v) {
        Object running = v.getTag(R.id.lean_spring);
        if (running instanceof SpringAnimation) ((SpringAnimation) running).cancel();
        v.setTag(R.id.lean_spring, null);
        v.setRotationX(0f);
        v.setRotationY(0f);
    }
}
