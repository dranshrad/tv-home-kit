package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

/**
 * Home-tv addition (not upstream): what sits between the photograph and the apps.
 *
 * 1. Shade behind the text only. The launcher's header text was designed for a black background; over a bright
 *    photograph it washes out. Each photograph is measured once (luma) and gets just enough shade behind the top
 *    bar to keep white text readable; a dark photograph gets none.
 * 2. The shelf band: one dark glass panel behind the rows of apps, with a dot per row on its right edge.
 * 3. A preview card: the focused app's artwork, tilted, in the calmest part of the photograph, away from its
 *    middle. A photograph with no calm part gets a smaller card parked in the corner above the shelf, so the card
 *    never sits on the subject. It turns over when focus moves.
 * 4. The wave: the focused tile's neighbours in its row rise a little, as on a Mac dock.
 * 5. The arrange hint: one line that says what OK, the arrows and Back do while apps are being arranged.
 *
 * Nothing here animates while the screen is at rest.
 */
final class Stage {
    /** The card's width and height as a fraction of the screen's (it is 16:9, like the screen). */
    static final float CARD = 0.16f;
    /** The parked card (a photograph with no calm part) is this share of the usual size. */
    static final float PARKED = 0.8f;
    /** The most any shade may darken; past this the photograph is hidden rather than helped. */
    static final float MAX_SHADE = 0.55f;
    /** Linear luminance the shaded photograph may keep behind small text (4.5:1 against white) and large text (3.5:1). */
    static final float SMALL_TEXT = 0.18f, LARGE_TEXT = 0.25f;
    /** Thumbnail the photograph is measured on. */
    static final int W = 96, H = 54;
    /** Detail (mean step between neighbouring pixels) above which a place is not calm enough for the card. */
    static final float BUSY = 0.035f;
    /** The card stays below the top bar and this far above the shelf (fractions of the screen's height). */
    static final float STAGE_TOP = 0.26f, SHELF_GAP = 0.03f;
    /** How much the middle of the picture counts against a place: detail there weighs up to 1 + this. */
    static final float CENTRE_WEIGHT = 1.5f;
    /**
     * The shelf shows no name under the focused tile: the tiles carry their names, and a second copy under them
     * was cut short ("Bluetooth…") and ran into the row below. Set before the tiles are made.
     */
    static volatile boolean tileNamesHidden;

    private final Activity activity;
    private final float dp;
    private final int screenW, screenH;
    private final Drawable topShade;
    private final GradientDrawable bandFill;
    private final View band;
    private final LinearLayout dots;
    private final FrameLayout card;
    private final ImageView art;
    private final GradientDrawable rim;
    private final TextView hint;
    /** Where the shelf begins, as a fraction of the screen's height; the card stays above it. */
    private float shelfTop = 0.64f;
    private float[] luma;
    private float cx = 0.84f, cy = 0.40f, scale = 1f, top, bandShade = 0.30f;
    private String shownKey;
    private ValueAnimator shadeFade;

    Stage(Activity activity, FrameLayout root, boolean withCard) {
        this.activity = activity;
        dp = activity.getResources().getDisplayMetrics().density;
        screenW = activity.getResources().getDisplayMetrics().widthPixels;
        screenH = activity.getResources().getDisplayMetrics().heightPixels;

        // One layer right above the photograph, under everything the launcher draws. The card's shadow needs a parent
        // of its own: an elevated child of root would be drawn over the text and the tiles.
        FrameLayout layer = new FrameLayout(activity);
        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        layer.setClipChildren(false);

        topShade = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF000000, 0xB3000000, 0x00000000});
        topShade.setAlpha(0);
        View topView = new View(activity);
        topView.setBackground(topShade);
        layer.addView(topView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(screenH * 0.30f)));

        bandFill = new GradientDrawable();
        bandFill.setCornerRadius(22 * dp);
        bandFill.setColor(0xFF000000);
        bandFill.setAlpha(0);
        GradientDrawable edge = new GradientDrawable();
        edge.setCornerRadius(22 * dp);
        edge.setColor(0x00000000);
        edge.setStroke(Math.round(dp), 0x2EFFFFFF);
        band = new View(activity);
        band.setBackground(bandFill);
        band.setForeground(edge);
        band.setVisibility(View.GONE);
        layer.addView(band, new FrameLayout.LayoutParams(0, 0));

        dots = new LinearLayout(activity);
        dots.setOrientation(LinearLayout.VERTICAL);
        dots.setGravity(Gravity.CENTER);
        dots.setVisibility(View.GONE);
        layer.addView(dots, new FrameLayout.LayoutParams(Math.round(12 * dp), 0));

        if (withCard) {
            int w = Math.round(screenW * CARD), h = Math.round(w * 9f / 16f);
            card = new FrameLayout(activity);
            final float radius = 12 * dp;
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            card.setClipToOutline(true);
            card.setElevation(12 * dp);
            card.setCameraDistance(w * 3.2f);   // close enough for the turn to read as depth
            card.setAlpha(0f);
            art = new ImageView(activity);
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(art, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rim = new GradientDrawable();
            rim.setCornerRadius(radius);
            rim.setColor(0x00000000);
            card.setForeground(rim);
            layer.addView(card, new FrameLayout.LayoutParams(w, h));
            place(false);
        } else {
            card = null;
            art = null;
            rim = null;
        }
        root.addView(layer, Math.min(1, root.getChildCount()), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        hint = new TextView(activity);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        hint.setTextColor(0xFFFFFFFF);
        hint.setSingleLine(true);
        hint.setFocusable(false);
        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(24 * dp);
        pill.setColor(0xE6101216);
        pill.setStroke(Math.round(dp), 0x66FFFFFF);
        hint.setBackground(pill);
        hint.setPadding(Math.round(22 * dp), Math.round(11 * dp), Math.round(22 * dp), Math.round(11 * dp));
        hint.setVisibility(View.GONE);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hp.topMargin = Math.round(screenH * 0.26f);
        root.addView(hint, hp);
    }

    // ---- text over a photograph --------------------------------------------------------------------

    /** Solid white with a soft dark edge, for header text that sits on a photograph. */
    static void legible(float dp, int color, TextView... views) {
        for (TextView v : views) {
            if (v == null) continue;
            v.setTextColor(color);
            float size = v.getTextSize();
            v.setShadowLayer(Math.max(5 * dp, size * 0.16f), 0, dp, 0xB8000000);
        }
    }

    // ---- measuring a photograph ---------------------------------------------------------------------

    /**
     * The brightness (0 to 1, gamma values) of the part of a decoded wallpaper that the screen shows (it is
     * centre-cropped), W by H, row by row. Null when it cannot be read.
     */
    static float[] luma(Bitmap decoded, int screenW, int screenH) {
        try {
            int bw = decoded.getWidth(), bh = decoded.getHeight();
            float fit = Math.max(screenW / (float) bw, screenH / (float) bh);
            int vw = Math.max(1, Math.min(bw, Math.round(screenW / fit)));
            int vh = Math.max(1, Math.min(bh, Math.round(screenH / fit)));
            // The whole picture needs no crop; createBitmap would copy a mutable bitmap in full.
            Bitmap crop = vw == bw && vh == bh ? decoded
                    : Bitmap.createBitmap(decoded, (bw - vw) / 2, (bh - vh) / 2, vw, vh);
            Bitmap small = Bitmap.createScaledBitmap(crop, W, H, true);
            int[] px = new int[W * H];
            small.getPixels(px, 0, W, 0, 0, W, H);
            if (small != crop && small != decoded) small.recycle();
            if (crop != decoded) crop.recycle();
            return luma(px);
        } catch (RuntimeException | OutOfMemoryError failure) {
            return null;
        }
    }

    /** The same from plain pixels (0xRRGGBB, alpha ignored). */
    static float[] luma(int[] px) {
        float[] luma = new float[px.length];
        for (int i = 0; i < luma.length; i++) {
            int p = px[i];
            luma[i] = (0.2126f * ((p >> 16) & 255) + 0.7152f * ((p >> 8) & 255) + 0.0722f * (p & 255)) / 255f;
        }
        return luma;
    }

    /** Shade for the top bar: behind the greeting, clock and date; the picture's caption; the buttons. */
    static float topShade(float[] luma) {
        return Math.max(shadeFor(bright(luma, 0.05f, 0.03f, 0.60f, 0.20f), SMALL_TEXT),
                Math.max(shadeFor(bright(luma, 0.76f, 0.19f, 0.96f, 0.25f), SMALL_TEXT),
                        shadeFor(bright(luma, 0.60f, 0.10f, 0.95f, 0.19f), LARGE_TEXT)));
    }

    /**
     * Where the card goes: {centre x, centre y, scale}. The calmest place between the top bar and the shelf, with
     * the middle of the picture counted against a place; a picture that is busy everywhere parks a smaller card in
     * the corner right above the shelf.
     */
    static float[] spot(float[] luma, float shelfTop) {
        float half = CARD / 2;
        float low = STAGE_TOP + half, high = shelfTop - SHELF_GAP - half;
        float[] parked = {0.86f, shelfTop - SHELF_GAP - half * PARKED, PARKED};
        if (luma == null || high <= low) return parked;
        float best = Float.MAX_VALUE, bestX = 0, bestY = 0, calmest = Float.MAX_VALUE;
        for (int row = 0; row < 3; row++) {
            float y = low + (high - low) * row / 2f;
            for (int col = 0; col <= 18; col++) {
                float x = 0.14f + 0.04f * col;
                float detail = detail(luma, x - half, y - half, x + half, y + half);
                calmest = Math.min(calmest, detail);
                float middle = 1f - Math.min(1f, (float) Math.hypot(x - 0.5f, y - 0.45f) / 0.45f);
                float score = detail * (1f + CENTRE_WEIGHT * middle);
                if (score < best) {
                    best = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        return calmest > BUSY ? parked : new float[]{bestX, bestY, 1f};
    }

    /** The 85th percentile of linear luminance in a zone given in fractions of the picture. */
    static float bright(float[] luma, float x0, float y0, float x1, float y1) {
        int ax = clamp(Math.round(x0 * W), 0, W - 1), bx = clamp(Math.round(x1 * W), ax + 1, W);
        int ay = clamp(Math.round(y0 * H), 0, H - 1), by = clamp(Math.round(y1 * H), ay + 1, H);
        float[] zone = new float[(bx - ax) * (by - ay)];
        int n = 0;
        for (int y = ay; y < by; y++) for (int x = ax; x < bx; x++) zone[n++] = luma[y * W + x];
        Arrays.sort(zone);
        return (float) Math.pow(zone[Math.min(n - 1, Math.round(n * 0.85f))], 2.2);
    }

    /**
     * How much black to lay over a zone this bright so that it ends at the target. The shade is blended on gamma
     * values, so a share s of black scales linear luminance by (1 - s)^2.2.
     */
    static float shadeFor(float brightness, float target) {
        if (brightness <= target) return 0f;
        return Math.min(MAX_SHADE, 1f - (float) Math.pow(target / brightness, 1 / 2.2));
    }

    /** Mean step between neighbouring pixels in a zone: low in sky, water and blur, high on buildings and faces. */
    static float detail(float[] luma, float x0, float y0, float x1, float y1) {
        int ax = clamp(Math.round(x0 * W), 0, W - 2), bx = clamp(Math.round(x1 * W), ax + 1, W - 1);
        int ay = clamp(Math.round(y0 * H), 0, H - 2), by = clamp(Math.round(y1 * H), ay + 1, H - 1);
        float sum = 0f;
        for (int y = ay; y < by; y++) {
            for (int x = ax; x < bx; x++) {
                float here = luma[y * W + x];
                sum += Math.abs(luma[y * W + x + 1] - here) + Math.abs(luma[(y + 1) * W + x] - here);
            }
        }
        return sum / ((bx - ax) * (by - ay));
    }

    private static int clamp(int v, int low, int high) {
        return v < low ? low : Math.min(v, high);
    }

    // ---- following the photograph and the shelf -----------------------------------------------------

    /** A new photograph is on screen (null: none, or it could not be measured). Runs on the main thread. */
    void onWallpaper(float[] measured) {
        luma = measured;
        final float fromTop = top, fromBand = bandShade;
        top = luma == null ? 0f : topShade(luma);
        // The band is never clear: it holds the rows together and carries the focused tile's name.
        bandShade = luma == null ? 0.30f
                : Math.max(0.30f, shadeFor(bright(luma, 0.06f, shelfTop, 0.94f, 0.98f), LARGE_TEXT));
        if (shadeFade != null) shadeFade.cancel();
        shadeFade = ValueAnimator.ofFloat(0f, 1f).setDuration(600);
        shadeFade.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            topShade.setAlpha(Math.round(255 * (fromTop + (top - fromTop) * t)));
            bandFill.setAlpha(Math.round(255 * (fromBand + (bandShade - fromBand) * t)));
        });
        shadeFade.start();
        choose(true);
    }

    /** The rows of apps sit in this rectangle (screen pixels); the band is drawn behind them and the card stays above. */
    void setShelf(int left, int topPx, int right, int bottom) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) band.getLayoutParams();
        if (lp.leftMargin == left && lp.topMargin == topPx && lp.width == right - left && lp.height == bottom - topPx
                && band.getVisibility() == View.VISIBLE) return;
        lp.leftMargin = left;
        lp.topMargin = topPx;
        lp.width = right - left;
        lp.height = bottom - topPx;
        band.setLayoutParams(lp);
        band.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams dl = (FrameLayout.LayoutParams) dots.getLayoutParams();
        dl.leftMargin = right - Math.round(22 * dp);
        dl.topMargin = topPx;
        dl.height = bottom - topPx;
        dots.setLayoutParams(dl);
        shelfTop = topPx / (float) screenH;
        onWallpaper(luma);   // the band's zone and the card's room both follow the shelf
    }

    /** One dot per row of apps on the band's right edge, the rows on screen drawn bright. Fewer than two rows: none. */
    void rows(int count, int first, int visible) {
        if (count <= visible) {
            dots.setVisibility(View.GONE);
            return;
        }
        dots.setVisibility(View.VISIBLE);
        while (dots.getChildCount() > count) dots.removeViewAt(dots.getChildCount() - 1);
        while (dots.getChildCount() < count) {
            View dot = new View(activity);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(3 * dp);
            dot.setBackground(shape);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(5 * dp), Math.round(5 * dp));
            lp.topMargin = lp.bottomMargin = Math.round(3 * dp);
            dots.addView(dot, lp);
        }
        for (int i = 0; i < count; i++) {
            boolean on = i >= first && i < first + visible;
            ((GradientDrawable) dots.getChildAt(i).getBackground()).setColor(on ? 0xF2FFFFFF : 0x59FFFFFF);
        }
    }

    /** Works out the card's place for the picture and the shelf as they are now, and moves the card if it changed. */
    private void choose(boolean glide) {
        float[] s = spot(luma, shelfTop);
        if (s[0] == cx && s[1] == cy && s[2] == scale) return;
        cx = s[0];
        cy = s[1];
        scale = s[2];
        place(glide);
    }

    /** Puts the card on its spot, turned toward the middle of the screen; glides there when asked. */
    private void place(boolean glide) {
        if (card == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) card.getLayoutParams();
        int left = Math.round(cx * screenW - lp.width / 2f);
        int topPx = Math.round(cy * screenH - lp.height / 2f);
        float dx = lp.leftMargin - left, dy = lp.topMargin - topPx;
        lp.leftMargin = left;
        lp.topMargin = topPx;
        card.setLayoutParams(lp);
        card.animate().cancel();
        if (glide && card.getAlpha() > 0f) {
            card.setTranslationX(dx);
            card.setTranslationY(dy);
            card.animate().translationX(0f).translationY(0f).rotationY(rest()).scaleX(scale).scaleY(scale).alpha(1f)
                    .setDuration(700).start();
        } else {
            card.setTranslationX(0f);
            card.setTranslationY(0f);
            card.setRotationY(rest());
            card.setScaleX(scale);
            card.setScaleY(scale);
        }
    }

    /** The card faces the middle of the screen: turned left on the right half, right on the left half. */
    private float rest() {
        return cx > 0.5f ? -14f : 14f;
    }

    // ---- the preview card ---------------------------------------------------------------------------

    /** Shows this app on the card. fromRight: focus moved right, so the new card comes in from the right. */
    void show(String key, Drawable live, int accent, boolean fromRight) {
        if (card == null || key == null || key.equals(shownKey)) return;
        Drawable copy = live != null && live.getConstantState() != null
                ? live.getConstantState().newDrawable(activity.getResources()).mutate() : null;
        boolean first = shownKey == null || card.getAlpha() == 0f;
        shownKey = key;
        card.animate().cancel();
        if (copy == null) {   // nothing to draw: no card for this app
            card.animate().alpha(0f).setDuration(120).start();
            return;
        }
        final float side = (fromRight ? 1f : -1f);
        final Runnable arrive = () -> {
            art.setImageDrawable(copy);
            card.setBackgroundColor(darken(accent));
            rim.setStroke(Math.round(1.5f * dp), 0xB3000000 | blend(accent, 0xFFFFFF, 0.6f));
            card.setRotationY(rest() + side * 42f);
            card.setTranslationX(side * 24 * dp);
            card.setTranslationY(0f);
            card.setScaleX(scale * 0.93f);
            card.setScaleY(scale * 0.93f);
            card.animate().alpha(1f).rotationY(rest()).translationX(0f).scaleX(scale).scaleY(scale)
                    .setDuration(380).setInterpolator(new OvershootInterpolator(1.1f)).withLayer().start();
        };
        if (first) {
            card.setAlpha(0f);
            arrive.run();
            return;
        }
        card.animate().alpha(0f).rotationY(rest() - side * 42f).translationX(-side * 24 * dp)
                .scaleX(scale * 0.93f).scaleY(scale * 0.93f)
                .setDuration(110).setInterpolator(null).withLayer().withEndAction(arrive).start();
    }

    /** Puts the card away (arranging apps); the next show() brings it back. */
    void hideCard() {
        if (card == null) return;
        shownKey = null;
        card.animate().cancel();
        card.animate().alpha(0f).setDuration(150).start();
    }

    private static int darken(int color) {
        return 0xFF000000 | blend(color, 0x000000, 0.72f);
    }

    /** color moved toward other by share (0 to 1), as 0xRRGGBB. */
    static int blend(int color, int other, float share) {
        int r = Math.round(((color >> 16) & 255) * (1 - share) + ((other >> 16) & 255) * share);
        int g = Math.round(((color >> 8) & 255) * (1 - share) + ((other >> 8) & 255) * share);
        int b = Math.round((color & 255) * (1 - share) + (other & 255) * share);
        return r << 16 | g << 8 | b;
    }

    // ---- the wave -----------------------------------------------------------------------------------

    /** Whether a tile beside the focused one rises: the next or previous position, in the same row. */
    static boolean beside(int pos, int focused, int columns) {
        return pos >= 0 && focused >= 0 && Math.abs(pos - focused) == 1 && pos / columns == focused / columns;
    }

    /** The tiles beside the focused one rise a little; every other tile is at rest. on false lays all flat. */
    void wave(RecyclerView grid, int focused, int columns, boolean on) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View item = grid.getChildAt(i);
            View tile = item.findViewById(R.id.card);
            if (tile == null || tile.isFocused()) continue;
            float target = on && beside(grid.getChildAdapterPosition(item), focused, columns) ? 1.055f : 1f;
            if (tile.getScaleX() == target) continue;
            tile.animate().scaleX(target).scaleY(target).setDuration(200).start();
        }
    }

    // ---- the arrange hint ---------------------------------------------------------------------------

    /** One line under the top bar, or none (null). */
    void hint(String text) {
        hint.animate().cancel();
        if (text == null) {
            hint.setVisibility(View.GONE);
            return;
        }
        hint.setText(text);
        if (hint.getVisibility() != View.VISIBLE) {
            hint.setAlpha(0f);
            hint.setVisibility(View.VISIBLE);
            hint.animate().alpha(1f).setDuration(180).start();
        }
    }
}
