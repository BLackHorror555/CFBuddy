package in.aviaryan.cfbuddy.ui;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.parceler.Parcels;

import in.aviaryan.cfbuddy.R;
import in.aviaryan.cfbuddy.data.Contract;
import in.aviaryan.cfbuddy.model.User;
import in.aviaryan.cfbuddy.parser.UserParser;
import in.aviaryan.cfbuddy.utils.Helper;
import in.aviaryan.cfbuddy.utils.VolleyErrorListener;


public class UserActivity extends AppCompatActivity {

    Toolbar mToolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    RequestQueue queue;
    private final String TAG = "CFLOG_UA";
    private final String REAL_URL = "http://codeforces.com/api/user.info?handles=";
    private final String URL = Contract.Cache.makeUIDFromRealUri(REAL_URL);
    private Boolean searchEnabled = false;
    private Boolean searchIntentRecd = false;
    TextView handle;
    TextView fullname;
    TextView rating;
    TextView maxRating;
    TextView organization;
    TextView country;
    TextView city;
    ImageView avatar;
    SearchView searchView;
    private static Boolean isDestroyed = true;

    User user;
    UserParser userParser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        isDestroyed = false;

        Log.d(TAG, "on create");
        user = new User();
        Log.d(TAG, getIntent().getStringExtra("handle"));
        user.handle = getIntent().getStringExtra("handle");

        // restore user
        if (savedInstanceState != null && savedInstanceState.containsKey("user")){
            Parcelable parcelUser = savedInstanceState.getParcelable("user");
            if (parcelUser != null){
                user = Parcels.unwrap(parcelUser);
                Log.d(TAG, "restore state" + user.handle);
            }
        }

        // update UI
        mToolbar = (Toolbar) findViewById(R.id.ua_toolbar);
        mToolbar.setTitle(user.handle);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.ua_col_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // volley
        queue = Volley.newRequestQueue(this);

        // instantiate views
        handle = (TextView) findViewById(R.id.ua_handle);
        fullname = (TextView) findViewById(R.id.ua_fullname);
        rating = (TextView) findViewById(R.id.ua_rating);
        maxRating = (TextView) findViewById(R.id.ua_max_rating);
        organization = (TextView) findViewById(R.id.ua_org);
        country = (TextView) findViewById(R.id.ua_country);
        avatar = (ImageView) findViewById(R.id.ua_photo);
        city = (TextView) findViewById(R.id.ua_city);

        // fab share
        (findViewById(R.id.ua_share_fab)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.sharePlainText(UserActivity.this, "http://codeforces.com/profile/" + user.handle);
            }
        });

        // set text
        handle.setText(user.handle);

        fetchUser();

        // if find user
        if (getIntent().hasExtra("searchEnabled")){
            searchEnabled = true;
        }
    }

    public void fetchUser(){
        VolleyErrorListener vel = new VolleyErrorListener(this);
        userParser = new UserParser(this);
        // cache
        updateDisplayFromCache(Helper.getCache(getContentResolver(), URL + user.handle));
        // request
        String url = REAL_URL + user.handle;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, userParser, vel);
        queue.add(jsonObjectRequest);
    }

    public void updateDisplayFromCache(String cache){
        if (cache != null){
            updateDisplay(userParser.parse(userParser.stringToJson(cache)) );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.left_slide_in, R.anim.right_slide_out);
    }

    @Override
    protected void onDestroy() {
        queue.stop();
        // http://stackoverflow.com/questions/25823320/incorrect-image-rendered-with-glide-library
        isDestroyed = true;
        super.onDestroy();
    }

    public void updateDisplay(User mUser) {
        Log.d(TAG, mUser.toString());
        if (!user.handle.equals(mUser.handle))  // edge case, old callbacks
            return;
        user = mUser;
        // when collapsing toolbar and toolbar are used together
        // then setTitle of ctl works
        collapsingToolbarLayout.setTitle(user.handle);
        // removes searchView active
        if (searchIntentRecd){
            searchIntentRecd = false;
            mToolbar.setTitle(user.handle);
            setSupportActionBar(mToolbar);
        }
        // others
        handle.setText(user.handle);
        fullname.setText(user.name);
        rating.setText(user.rank + " (" + user.rating + ")");
        maxRating.setText(user.maxRank + " (" + user.maxRating + ")");
        organization.setText(undForNull(user.organisation));
        country.setText(undForNull(user.country));
        city.setText(undForNull(user.city));
        if (!isDestroyed){
            Glide.with(this).load(user.avatar)
                    .centerCrop().placeholder(R.drawable.ic_account_circle_accent_24px)
                    .crossFade().into(avatar);
        }
    }

    private String undForNull(String s){
        return (s == null) ? "undefined" : s;
    }

    public void updateCache(String data){
        Helper.updateCache(data, getContentResolver(), URL + user.handle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search_profile).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Change color
        // THINK later
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mToolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
//                mToolbar.setBackgroundColor(getResources().getColor(R.color.transparent));
                return false;
            }
        });

        if (searchEnabled){
            searchEnabled = false;
            searchView.setIconified(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            user = new User();
            user.handle = query;
            Log.d(TAG, "search " + query);
//            queue.stop();  // not needed now as old is already checked in updateDisplay
            searchIntentRecd = true;
            if (!searchView.isIconified()){
                searchView.setIconified(true);
            }
            fetchUser();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "parcelling");
        outState.putParcelable("user", Parcels.wrap(user));
    }
}
