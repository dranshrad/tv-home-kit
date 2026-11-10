package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Home-tv addition: the measurements the stage takes of a photograph. */
public class StageTest {
    private static final float SHELF = 0.64f;

    private static int[] flat(int rgb) {
        int[] px = new int[Stage.W * Stage.H];
        java.util.Arrays.fill(px, rgb);
        return px;
    }

    /** A checkerboard: detail everywhere, far above Stage.BUSY. */
    private static int[] busy() {
        int[] px = new int[Stage.W * Stage.H];
        for (int y = 0; y < Stage.H; y++) {
            for (int x = 0; x < Stage.W; x++) px[y * Stage.W + x] = (x + y) % 2 == 0 ? 0xFFFFFF : 0x000000;
        }
        return px;
    }

    @Test
    public void aDarkPhotographGetsNoShadeBehindTheTopBar() {
        assertEquals(0f, Stage.topShade(Stage.luma(flat(0x202020))), 0f);
    }

    @Test
    public void aWhitePhotographIsBroughtToTheTargetAndNeverPastTheCap() {
        float shade = Stage.topShade(Stage.luma(flat(0xFFFFFF)));
        assertEquals(1 - Math.pow(Stage.SMALL_TEXT, 1 / 2.2), shade, 1e-4);
        assertTrue(shade <= Stage.MAX_SHADE);
        assertEquals(Stage.MAX_SHADE, Stage.shadeFor(1f, 0.01f), 0f);
    }

    @Test
    public void theShadeBringsTheZoneDownToTheTarget() {
        // A mid-bright zone: the shade is on gamma values, so the luminance left is brightness * (1 - s)^2.2.
        float brightness = 0.35f;
        float s = Stage.shadeFor(brightness, Stage.SMALL_TEXT);
        assertTrue(s > 0f && s < Stage.MAX_SHADE);
        assertEquals(Stage.SMALL_TEXT, brightness * Math.pow(1 - s, 2.2), 1e-4);
        assertEquals(0f, Stage.shadeFor(0.10f, Stage.SMALL_TEXT), 0f);
    }

    @Test
    public void theCardGoesWhereThePictureIsCalm() {
        // Busy everywhere except a flat patch on the right, in the upper band.
        int[] px = busy();
        float low = Stage.STAGE_TOP + Stage.CARD / 2;
        for (int y = 0; y < Stage.H; y++) {
            for (int x = 0; x < Stage.W; x++) {
                float fx = (x + 0.5f) / Stage.W, fy = (y + 0.5f) / Stage.H;
                if (Math.abs(fx - 0.82f) < 0.11f && Math.abs(fy - low) < 0.11f) px[y * Stage.W + x] = 0x6080A0;
            }
        }
        float[] spot = Stage.spot(Stage.luma(px), SHELF);
        assertEquals(0.82f, spot[0], 1e-4f);
        assertEquals(low, spot[1], 1e-4f);
        assertEquals(1f, spot[2], 0f);
    }

    @Test
    public void aPictureThatIsBusyEverywhereParksASmallerCardAboveTheShelf() {
        float[] spot = Stage.spot(Stage.luma(busy()), SHELF);
        assertEquals(0.86f, spot[0], 1e-4f);
        assertEquals(Stage.PARKED, spot[2], 0f);
        // The parked card ends SHELF_GAP above the shelf.
        assertEquals(SHELF - Stage.SHELF_GAP, spot[1] + Stage.CARD * Stage.PARKED / 2, 1e-4f);
    }

    @Test
    public void ofTwoEquallyCalmPlacesTheOneAwayFromTheMiddleWins() {
        // A flat picture is calm everywhere: the card must not take the middle.
        float[] spot = Stage.spot(Stage.luma(flat(0x6080A0)), SHELF);
        assertTrue(Math.abs(spot[0] - 0.5f) > 0.3f);
    }

    @Test
    public void noPictureAndNoRoomBothParkTheCard() {
        assertEquals(Stage.PARKED, Stage.spot(null, SHELF)[2], 0f);
        assertEquals(Stage.PARKED, Stage.spot(Stage.luma(flat(0x6080A0)), Stage.STAGE_TOP + 0.05f)[2], 0f);
    }

    @Test
    public void onlyNeighboursInTheSameRowRise() {
        assertTrue(Stage.beside(3, 4, 8));
        assertTrue(Stage.beside(5, 4, 8));
        assertFalse(Stage.beside(8, 7, 8));   // the first tile of the next row
        assertFalse(Stage.beside(6, 4, 8));
        assertFalse(Stage.beside(-1, 0, 8));
    }

    @Test
    public void blendMovesAColourTowardAnother() {
        assertEquals(0xFFFFFF, Stage.blend(0x102030, 0xFFFFFF, 1f));
        assertEquals(0x102030, Stage.blend(0x102030, 0xFFFFFF, 0f));
        assertEquals(0x808080, Stage.blend(0x000000, 0xFFFFFF, 0.5f));
    }
}
