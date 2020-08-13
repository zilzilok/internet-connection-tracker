package ru.zilzilok.android_internet_alerts.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

import ru.zilzilok.android_internet_alerts.R;
import ru.zilzilok.android_internet_alerts.utils.AlertState;
import ru.zilzilok.android_internet_alerts.utils.ConnectionType;
import ru.zilzilok.android_internet_alerts.utils.ProgressButton;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    private boolean isPoopClicked;
    private ProgressButton checkButton;
    private ProgressButton addButton;
    ConstraintLayout stateLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences("lang", 0);
        String lang = sp.getString("lang", "ru");
        Locale locale = new Locale(lang);

        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name_actionbar);

        // Check button click listener
        View viewCheckButton = findViewById(R.id.buttonCheck);
        checkButton = new ProgressButton(MainActivity.this,
                viewCheckButton, getResources().getString(R.string.check_button));
        viewCheckButton.setOnClickListener(this::buttonCheckClicked);

        // Add button click listener
        View viewAddButton = findViewById(R.id.buttonAdd);
        addButton = new ProgressButton(MainActivity.this,
                viewAddButton, getResources().getString(R.string.monitor_button));
        viewAddButton.setOnClickListener(this::buttonAddClicked);

        // Current State
        stateLayout = findViewById(R.id.constraintLayoutState);
        ConstraintLayout stateProgressLayout = stateLayout.findViewById(R.id.stateProgress);
        stateLayout.setVisibility(View.GONE);
        ImageView stateButton = stateProgressLayout.findViewById(R.id.imageViewPoop);
        stateButton.setOnClickListener(this::buttonPoopClicked);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings1:
                Toast.makeText(this, "\"Network Connection Tracker\"\nby @zilzilok", Toast.LENGTH_LONG).show();
                break;
            case R.id.settings2:
                SharedPreferences sp = getSharedPreferences("lang", MODE_PRIVATE);
                String countryCode = sp.getString("lang", "ru");
                SharedPreferences.Editor editor = sp.edit();
                switch (countryCode) {
                    case "ru":
                        countryCode = "en";
                        break;
                    case "en":
                        countryCode = "ru";
                        break;
                }
                editor.putString("lang", countryCode);
                editor.apply();
                changeLang(countryCode);
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeLang(String lang) {
        if (lang.equalsIgnoreCase(""))
            return;
        Locale myLocale = new Locale(lang);

        Locale.setDefault(myLocale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = myLocale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        saveLocale(lang);

        startActivity(new Intent(this, SplashActivity.class));
        finish();
        Toast.makeText(this, getResources().getString(R.string.curr_lang), Toast.LENGTH_SHORT).show();
        overridePendingTransition(0, 0);
    }

    private void saveLocale(String lang) {
        String langPref = "Language";
        SharedPreferences prefs = getSharedPreferences("def_loc", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(langPref, lang);
        editor.apply();
    }

    public void buttonCheckClicked(View view) {
        checkButton.buttonActivated();
        checkButton.blockButton();
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Toast.makeText(this, ConnectionType.getNetworkClass(this), Toast.LENGTH_LONG).show();
            checkButton.buttonRestored();
            checkButton.unblockButton();
        }, 800);
    }

    private void buttonPoopClicked(View view) {
        isPoopClicked = true;
    }

    public void buttonAddClicked(View view) {
        isPoopClicked = false;
        Context wrapper = new ContextThemeWrapper(this, R.style.PopupStyle);
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        popupMenu.setOnMenuItemClickListener(this::popupMenuItemClicked);
        popupMenu.inflate(R.menu.popup_alerts_menu);
        popupMenu.show();
    }

    private boolean popupMenuItemClicked(MenuItem item) {
        AlertState alertState = new AlertState(item.toString());
        ConstraintLayout stateProgressLayout = stateLayout.findViewById(R.id.stateProgress);
        TextView stateText = stateProgressLayout.findViewById(R.id.progressStateTextView);
        stateText.setText(getResources().getString(R.string.wait) + "\n" + alertState.getConnectionType());
        startMonitor(alertState);
        return true;
    }

    private static final int BLOCK_BUTTON = 1;
    private static final int UNBLOCK_BUTTON = 2;
    private static final int VISIBLE_STATE = 3;
    private static final int GONE_STATE = 4;

    private static class TmpHandler extends Handler {
        MainActivity mthis;

        TmpHandler(MainActivity activity) {
            this.mthis = activity;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BLOCK_BUTTON:
                    mthis.addButton.blockButton();
                    break;
                case UNBLOCK_BUTTON:
                    mthis.addButton.unblockButton();
                    break;
                case VISIBLE_STATE:
                    mthis.stateLayout.setVisibility(View.VISIBLE);
                    break;
                case GONE_STATE:
                    mthis.stateLayout.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void startMonitor(AlertState state) {
        Handler handler = new TmpHandler(this);
        new Thread(() -> {
            handler.sendEmptyMessage(BLOCK_BUTTON);
            handler.sendEmptyMessage(VISIBLE_STATE);
            while (!isPoopClicked && !state.getConnectionType().equals(ConnectionType.getNetworkClass(this))) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            handler.sendEmptyMessage(UNBLOCK_BUTTON);
            handler.sendEmptyMessage(GONE_STATE);
            if (!isPoopClicked) {
                state.notifyAboutState(this);
            }
        }).start();
    }
}
