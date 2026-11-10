package tv.home.tiles;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.tv.TvContract;
import android.os.Bundle;
import android.widget.Toast;

/**
 * One activity behind the home-screen tiles (the aliases in the manifest). A tile with an "input" meta-data
 * value switches the TV to that input, the way the TV's own Inputs menu does; its optional "app" value names the
 * TV's own input viewer, for a TV where more than one app answers. The Watchlist tile opens Google TV's home
 * through the launcher, which then leaves that screen alone until Home is pressed.
 */
public class TileActivity extends Activity {
    private static final String LAUNCHER = "com.gathin.tvhome";
    private static final String GOOGLE_TV = "com.google.android.apps.tv.launcherx";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            startActivity(target().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException | PackageManager.NameNotFoundException unavailable) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private Intent target() throws PackageManager.NameNotFoundException {
        ComponentName alias = getIntent().getComponent();
        Bundle meta = alias == null ? null
                : getPackageManager().getActivityInfo(alias, PackageManager.GET_META_DATA).metaData;
        String input = meta == null ? null : meta.getString("input");
        if (input != null) {
            Intent view = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(TvContract.buildChannelUriForPassthroughInput(input), "vnd.android.cursor.item/channel");
            String app = meta.getString("app");
            return app == null ? view : view.setPackage(app);
        }
        Intent viaLauncher = new Intent().setClassName(LAUNCHER, LAUNCHER + ".StockHomeActivity");
        if (getPackageManager().resolveActivity(viaLauncher, 0) != null) return viaLauncher;
        return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(GOOGLE_TV);
    }
}
