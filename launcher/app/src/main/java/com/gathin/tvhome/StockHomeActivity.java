package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Home-tv addition (not upstream): opens the stock launcher's home screen on purpose, for a "Watchlist" tile.
 * With stock_home_redirect set, HomeKeyService sends that screen straight back here; this start tells it to
 * leave the screen alone until the Home button is pressed.
 */
public class StockHomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String stock = Config.load(this).stockHomeRedirect;
        if (stock.isEmpty()) stock = "com.google.android.apps.tv.launcherx";
        HomeKeyService.stockPass = true;
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(stock)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            HomeKeyService.stockPass = false;
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
