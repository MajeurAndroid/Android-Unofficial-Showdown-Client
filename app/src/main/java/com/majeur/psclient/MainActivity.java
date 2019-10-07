package com.majeur.psclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.majeur.psclient.io.DexIconLoader;
import com.majeur.psclient.io.GlideHelper;
import com.majeur.psclient.service.ShowdownService;
import com.majeur.psclient.util.Utils;
import com.majeur.psclient.widget.SwitchLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private Intent mShowdownServiceIntent;
    private ShowdownService mService;
    boolean mCanUnbindService = false;

    private BottomNavigationView mNavigationView;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ShowdownService.Binder binder = (ShowdownService.Binder) iBinder;
            mService = binder.getService();
            notifyServiceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Should never be called as our ShowdownService is running in the same process
        }
    };

    private DexIconLoader mDexIconLoader;
    private GlideHelper mGlideHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setStaticScreenDensity(getResources());
        mDexIconLoader = new DexIconLoader(this);
        mGlideHelper = new GlideHelper(this);
        setContentView(R.layout.activity_main);

        final SwitchLayout switchLayout = findViewById(R.id.fragment_container);
        mNavigationView = findViewById(R.id.bottom_navigation);
        mNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == mNavigationView.getSelectedItemId()) return false;
                clearBadge(menuItem.getItemId());
                switch (menuItem.getItemId()) {
                    case R.id.fragment_home:
                        switchLayout.smoothSwitchTo(0);
                        return true;
                    case R.id.fragment_battle:
                        switchLayout.smoothSwitchTo(1);
                        return true;
                    case R.id.fragment_chat:
                        switchLayout.smoothSwitchTo(2);
                        return true;
                    case R.id.fragment_teams:
                        switchLayout.smoothSwitchTo(3);
                        return true;
                    default:
                        return false;
                }
            }
        });

        mShowdownServiceIntent = new Intent(this, ShowdownService.class);
        startService(mShowdownServiceIntent);
    }

    @Override
    public void onBackPressed() {
        if (mNavigationView.getSelectedItemId() != R.id.fragment_home)
            mNavigationView.setSelectedItemId(R.id.fragment_home);
        else
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Are you sure you want to quit ?")
                    .setMessage("Connection to Showdown server will be closed.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
    }

    public int getSelectedFragmentId() {
        return mNavigationView.getSelectedItemId();
    }

    public void showBadge(int fragmentId) {
        BadgeDrawable badge = mNavigationView.getOrCreateBadge(fragmentId);
        badge.setBackgroundColor(getColor(R.color.accent));
    }

    public void clearBadge(int fragmentId) {
        mNavigationView.removeBadge(fragmentId);
    }

    public DexIconLoader getDexIconLoader() {
        return mDexIconLoader;
    }

    public GlideHelper getGlideHelper() {
        return mGlideHelper;
    }

    public ShowdownService getService() {
        return mCanUnbindService ? mService : null;
    }

    public HomeFragment getHomeFragment() {
        return (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_home);
    }

    public BattleFragment getBattleFragment() {
        return (BattleFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_battle);
    }

    public ChatFragment getChatFragment() {
        return (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_chat);
    }

    public TeamsFragment getTeamsFragment() {
        return (TeamsFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_teams);
    }

    private void notifyServiceBound() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof Callbacks)
                ((Callbacks) fragment).onServiceBound(mService);
        }
    }

    private void notifyServiceWillUnbound() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof Callbacks)
                ((Callbacks) fragment).onServiceWillUnbound(mService);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bindService(mShowdownServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE))
            mCanUnbindService = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCanUnbindService) {
            notifyServiceWillUnbound();
            unbindService(mServiceConnection);
            mService = null;
            mCanUnbindService = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations())
            stopService(mShowdownServiceIntent);
    }

    public void showHomeFragment() {
        mNavigationView.setSelectedItemId(R.id.fragment_home);
    }

    public void showBattleFragment() {
        mNavigationView.setSelectedItemId(R.id.fragment_battle);
    }

    public void setKeepScreenOn(boolean keep) {
        if (keep)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public interface Callbacks {

        public void onServiceBound(ShowdownService service);

        public void onServiceWillUnbound(ShowdownService service);
    }
}
