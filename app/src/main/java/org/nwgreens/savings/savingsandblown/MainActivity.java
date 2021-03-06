package org.nwgreens.savings.savingsandblown;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int ADD_TRANSACTION_CLOSED = 101;
    public CookieManager mCookieManager;
    public NavigationView navigationView;
    public String currentBank;
    public ListView lvBanks;
    CategoriesResponse CategoriesResponse;
    SharedPreferences myPrefFile;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private BanksResponse banksResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        myPrefFile = getSharedPreferences("MyPrefFile", 0);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AddTransaction.class);
                i.putExtra("currentBank", currentBank);
                String categories = new Gson().toJson(CategoriesResponse);
                i.putExtra("categories", categories);
                startActivityForResult(i, ADD_TRANSACTION_CLOSED);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);


        drawer.setDrawerListener(toggle);
        toggle.syncState();

        SetupNavigationTray();


        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void SetupNavigationTray() {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        GetBankNames();
    }

    protected Void GetBankNames() {
        RequestQueue rq = MySingleton.getInstance(getApplicationContext()).getRequestQueue();
        GsonRequest<BanksResponse> gr = new GsonRequest<BanksResponse>(Request.Method.GET, BanksResponse.class,
                String.format("%sbanks", getApplicationContext().getResources().getText(R.string.url)),
                new Response.Listener<BanksResponse>() {
                    @Override
                    public void onResponse(BanksResponse response) {
                        try {
                            String[] banks = response.GetBanks();
                            banksResponse = response;
                            Menu m = navigationView.getMenu();
                            SubMenu subMenu = m.addSubMenu(Menu.NONE, Menu.NONE, 1, "Banks");
                            for (int i = 0; i < banks.length; i++) {
                                subMenu.add(banks[i]);
                            }

                            GetBankData(getIntent().getStringExtra("defaultBank"));
                        } catch (Exception ex) {
                            // banks = new String[0];
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast t = Toast.makeText(getApplicationContext(), "Log back in to continue", Toast.LENGTH_LONG);
                t.show();
                finish();
            }
        }

        );
        rq.add(gr);
        return null;
    }

    protected Void GetBankData(String bankName) {
        setTitle(bankName);
        currentBank = bankName;

        CategoriesResponse.GetCategories(getApplicationContext(), bankName, new Response.Listener<CategoriesResponse>() {
            @Override
            public void onResponse(CategoriesResponse response) {
                CategoriesResponse = response;
            }
        });

        BanksResponse.Bank bankDetail = banksResponse.GetBankDetail(bankName);

        SharedPreferences.Editor edit = myPrefFile.edit();
        edit.putString("budgetStartDate", bankDetail.getBudgetStartDate());
        edit.putString("budgetEndDate", bankDetail.getBudgetStartEndDate());
        edit.apply();

        RequestQueue rq = MySingleton.getInstance(getApplicationContext()).getRequestQueue();

        GsonRequest<Bank> gr = new GsonRequest<Bank>(Request.Method.GET, Bank.class,
                String.format("%sbanks/%s?PageNumber=1&StatusFilter=&CategoryFilter=&ShowFutureItems=true", getApplicationContext().getResources().getText(R.string.url), bankName),
                new Response.Listener<Bank>() {
                    @Override
                    public void onResponse(final Bank response) {

                        try {




                            TextView actual = (TextView) findViewById(R.id.actualBalance);
                            actual.setText(String.format("%.2f%n", response.getTotal().getActualBalance()));

                            TextView current = (TextView) findViewById(R.id.currentBalance);
                            current.setText(String.format("%.2f%n", response.getTotal().getClearedBalance()));

                            ListView lv = (ListView) findViewById(R.id.transactions);

                            BankItemAdapter adapter = new BankItemAdapter(getApplicationContext(), response.getTransactions(myPrefFile),
                                    myPrefFile.getInt("DisplayMode", R.id.actualBalance));
                            lv.setAdapter(adapter);
                            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    String transactionJson = new Gson().toJson(response.getTransactions(myPrefFile).get(position));
                                    String categories = new Gson().toJson(CategoriesResponse);

                                    Intent i = new Intent(getApplicationContext(), AddTransaction.class);
                                    i.putExtra("currentBank", currentBank);
                                    i.putExtra("editedGson", transactionJson);
                                    i.putExtra("categories", categories);
                                    startActivityForResult(i, ADD_TRANSACTION_CLOSED);


                                }
                            });
                            ;
                        } catch (Exception ex) {
                            // banks = new String[0];
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast t = Toast.makeText(getApplicationContext(), "Error getting banks", Toast.LENGTH_LONG);
                t.show();
            }
        }

        );
        rq.add(gr);
        return null;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
      /*  getMenuInflater().inflate(R.menu.main, menu);
        String[] banks = new String[]{"bank1", "bank2"};

        MenuItem item = (MenuItem) menu.findItem(R.id.BankListSpinner);
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.color.colorPrimaryDark);
        spinner.set
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, banks);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        item.setActionView(spinner);
        */
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            Intent i = new Intent(getApplicationContext(), FilterSettings.class);
            startActivityForResult(i, ADD_TRANSACTION_CLOSED);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        GetBankData(item.getTitle().toString());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD_TRANSACTION_CLOSED: {

                GetBankData(currentBank);

                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://org.nwgreens.savings.savingsandblown/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://org.nwgreens.savings.savingsandblown/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
