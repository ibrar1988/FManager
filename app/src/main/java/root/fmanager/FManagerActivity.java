package root.fmanager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FManagerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;
    private ImageView toolbarDeleteButton;
    private ImageView toolbarRenameButton;

    TextView toolbarOpenDirPath;

    private FloatingActionButton fab;

    int colorIcon;

    private RVFrag recyclerViewFrag;
    int colorCard;
    int colorCardSelected;

    static File appDataDir;

    private File homeDir;

    private boolean backPressedOnce = false;

    private void init() {
        String defaultHomeDir = getResources().getString(R.string.home_dir_default);
        String homeDirPath;

        Utils.context = this;

        appDataDir = new File(Environment.getExternalStorageDirectory() +
                File.separator + getResources().getString(R.string.app_name));
        //noinspection ResultOfMethodCallIgnored
        appDataDir.mkdir();

        if (PreferenceManager.getDefaultSharedPreferences(this).
                getString("homeDir", defaultHomeDir).equals(defaultHomeDir))
            homeDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        else homeDirPath = "/";
        homeDir = new File(homeDirPath);

        recyclerViewFrag = new RVFrag();
        Bundle args = new Bundle();
        args.putString("homeDirPath", homeDirPath);
        recyclerViewFrag.setArguments(args);
        getFragmentManager().beginTransaction()
                .replace(R.id.content, recyclerViewFrag).addToBackStack(null).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String defaultTheme = getResources().getString(R.string.theme_default);
        String currentTheme = PreferenceManager.getDefaultSharedPreferences(this).getString("theme", defaultTheme);
        if (currentTheme.equals(defaultTheme))
            setTheme(R.style.AppTheme_Light_NoActionBar);
        else setTheme(R.style.AppTheme_Dark_NoActionBar);

        setContentView(R.layout.activity_fmanager);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            if (currentTheme.equals(defaultTheme))
                toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay_Light);
            else toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay_Dark);
        }
        setSupportActionBar(toolbar);

        toolbarDeleteButton = (ImageView) findViewById(R.id.toolbar_delete);
        toolbarRenameButton = (ImageView) findViewById(R.id.toolbar_rename);

        toolbarOpenDirPath = (TextView) findViewById(R.id.toolbar_path);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            int fabMargin = Math.round(getResources().getDimension(R.dimen.fab_margin));
            ((ViewGroup.MarginLayoutParams)fab.getLayoutParams()).setMargins(0, 0,
                    fabMargin, fabMargin + toolbar.getLayoutParams().height);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showCreateNewDialog(view.getContext());
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HashSet<String> request = new HashSet<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                request.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                request.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!request.isEmpty()) {
                String[] permissions = new String[request.size()];
                permissions = request.toArray(permissions);
                ActivityCompat.requestPermissions(this, permissions, 1);
            } else {
                init();
            }
        } else {
            init();
        }
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean permissionsGranted = true;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED)
                permissionsGranted = false;
        }
        if (requestCode == 1 && permissionsGranted)
            init();
        else {
            Toast.makeText(this, "Please grant permissions for the app to work.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recyclerViewFrag != null)
            recyclerViewFrag.openDirectory(recyclerViewFrag.getCurrentOpenDir());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        }
        else if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (recyclerViewFrag == null) {
            super.onBackPressed();
        }
        else if (!recyclerViewFrag.getAdapter().selectedFiles.isEmpty()) {
            recyclerViewFrag.getAdapter().selectedFiles = null;
            recyclerViewFrag.getAdapter().selectedFiles = new HashSet<>();
            recyclerViewFrag.getAdapter().openDirectory(recyclerViewFrag.getAdapter().currentOpenDir);
            selectionModeToggle(recyclerViewFrag.getAdapter().selectedFiles.size());
        }
        else if (recyclerViewFrag.getAdapter().currentOpenDir.
                getAbsolutePath().equals(homeDir.getAbsolutePath())) {
            if (backPressedOnce) {
                finish();
                return;
            }
            backPressedOnce = true;
            Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
            new ScheduledThreadPoolExecutor(1).schedule(new Runnable() {
                @Override
                public void run() {
                    backPressedOnce = false;
                }
            }, 2, TimeUnit.SECONDS);
        }
        else {
            File parentFile;
            while (!(parentFile = recyclerViewFrag.getAdapter().currentOpenDir.getParentFile()).canRead()) {
                recyclerViewFrag.getAdapter().currentOpenDir = parentFile;
            }
            recyclerViewFrag.getAdapter().openDirectory(parentFile);
        }
    }

    @Override
    public void setTheme(@StyleRes int resId) {
        super.setTheme(resId);
        switch (resId) {
            case R.style.AppTheme_Dark_NoActionBar:
                colorCard = ContextCompat.getColor(this, R.color.cardview_dark_background);
                colorCardSelected = ContextCompat.getColor(this, R.color.colorAccentDT);
                colorIcon = ContextCompat.getColor(this, R.color.white_smoke);
                break;
            case R.style.AppTheme_Light_NoActionBar:
                colorCard = ContextCompat.getColor(this, R.color.cardview_light_background);
                colorCardSelected = ContextCompat.getColor(this, R.color.colorAccentLT);
                colorIcon = ContextCompat.getColor(this, R.color.smoky_black);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.fmanager, menu);
        return true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (recyclerViewFrag != null)
            recyclerViewFrag.openDirectory(recyclerViewFrag.getCurrentOpenDir());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                recyclerViewFrag.getFilter().filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                recyclerViewFrag.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_root) {
            recyclerViewFrag.openDirectory(homeDir = new File("/"));
        } else if (id == R.id.nav_int) {
            recyclerViewFrag.openDirectory(Environment.getExternalStorageDirectory());
        } /*else if (id == R.id.nav_ext) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void selectionModeToggle(int noOfItemsSelected) {
        if (noOfItemsSelected >= 1) {
            toolbar.getMenu().findItem(R.id.search).setVisible(false);
            fab.setImageResource(R.drawable.ic_lock);
            fab.setOnClickListener(recyclerViewFrag.getAdapter());
            toolbarDeleteButton.setVisibility(View.VISIBLE);
            toolbarDeleteButton.setColorFilter(colorIcon);
            toolbarDeleteButton.setOnClickListener(recyclerViewFrag.getAdapter());
        } else {
            toolbar.getMenu().findItem(R.id.search).setVisible(true);
            fab.setImageResource(R.drawable.ic_fab_plus);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCreateNewDialog(v.getContext());
                }
            });
            toolbarDeleteButton.setVisibility(View.GONE);
        }
        if (noOfItemsSelected == 1) {
            toolbarRenameButton.setVisibility(View.VISIBLE);
            toolbarRenameButton.setColorFilter(colorIcon);
            toolbarRenameButton.setOnClickListener(recyclerViewFrag.getAdapter());
        } else toolbarRenameButton.setVisibility(View.GONE);
    }

    private void showCreateNewDialog(final Context context) {
        final AlertDialog.Builder createNewDialog = new AlertDialog.Builder(context);
        createNewDialog.setTitle("Create new");

        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        final EditText edName = new EditText(context);
        edName.setEms(8);
        edName.setHint("folder name");
        layout.addView(edName);

        final EditText edExt = new EditText(context);
        edExt.setEms(2);
        edExt.setVisibility(View.GONE);
        edExt.setHint("ext");
        layout.addView(edExt);

        createNewDialog.setSingleChoiceItems(
                new CharSequence[]{"Folder", "File"}, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1) {
                            edName.setHint("file name");
                            edExt.setVisibility(View.VISIBLE);
                        } else if (which == 0) {
                            edName.setHint("folder name");
                            edExt.setVisibility(View.GONE);
                        }
                    }
                });

        createNewDialog.setView(layout);

        createNewDialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edName.getText().toString();
                String ext;
                if (name.isEmpty()) {
                    Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (edExt.getVisibility() == View.VISIBLE) {
                    ext = '.' + edExt.getText().toString();
                    File newFile = new File(recyclerViewFrag.getCurrentOpenDir().getAbsolutePath() +
                            File.separator + name + (ext.equals(".") ? "" : ext));
                    if (newFile.exists()) {
                        Toast.makeText(context,
                                "File with that name exists. Please select new name.", Toast.LENGTH_LONG).show();
                    } else try {
                        if (!newFile.createNewFile())
                            Toast.makeText(context,
                                    "Cannot create the file.", Toast.LENGTH_LONG).show();
                        else {
                            recyclerViewFrag.openDirectory(recyclerViewFrag.getCurrentOpenDir());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    File newFolder = new File(
                            recyclerViewFrag.getCurrentOpenDir().getAbsolutePath() + File.separator + name);
                    if (newFolder.exists()) {
                        Toast.makeText(context,
                                "Folder with that name exists. Please select new name.", Toast.LENGTH_LONG).show();
                    }
                    else if (!newFolder.mkdir())
                        Toast.makeText(context,
                                "Cannot create the folder.", Toast.LENGTH_LONG).show();
                    else {
                        recyclerViewFrag.openDirectory(recyclerViewFrag.getCurrentOpenDir());
                        Toast.makeText(context,
                                "Folder created.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        createNewDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        createNewDialog.show();
    }
}
