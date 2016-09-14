package root.fmanager;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

public class Settings extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String defaultTheme = getResources().getString(R.string.theme_default);
        if (PreferenceManager.getDefaultSharedPreferences(this).getString("theme", defaultTheme).equals(defaultTheme))
            setTheme(R.style.AppTheme_Light_NoActionBar);
        else setTheme(R.style.AppTheme_Dark_NoActionBar);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setSubtitle("Settings");
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFrag()).commit();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SettingsFrag extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            findPreference("theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(), "Restart app to apply theme.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
    }
}
