package com.ucsdtasks.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionApi;
import com.google.android.gms.location.places.internal.PlaceEntity;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.ucsdtasks.backend.UCSDTask;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.R.attr.checked;
import static com.ucsdtasks.android.R.id.task_name;


public class CreateTask extends AppCompatActivity {
    static int CHANGE_LOCATION = 1;
    TextView location;

    String task_name;
    Double longitude = null;
    Double latitude = null;
    String description;
    String starting_offer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_task);
        location = (TextView) findViewById(R.id.specify_location);

        if (longitude == null && latitude == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                longitude = extras.getDouble("Longitude");
                latitude = extras.getDouble("Latitude");
            }
        }
    }

    public void pushTask(View view) {

        // Linking UI to code level
        final EditText task_name_ET = (EditText) findViewById(R.id.task_name);
        final EditText starting_offer_ET = (EditText) findViewById(R.id.starting_offer);
        final EditText description_ET = (EditText) findViewById(R.id.description);

        // Error checking
        if (task_name_ET.getText().toString().matches("") ||
                starting_offer_ET.getText().toString().matches("")) {
            Snackbar.make(view, "Field(s) cannot be empty", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Storing and parsing from string to wanted data type
        task_name = task_name_ET.getText().toString();


        description = description_ET.getText().toString();
        starting_offer = starting_offer_ET.getText().toString();
        createTask(task_name, latitude, longitude, starting_offer, description);
        super.onBackPressed();
    }


    public void createTask(String title, double lat, double lon, String asking_price, String description) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference push = ref.child("tasks").push();
        String key = push.getKey();
        UCSDTask task = new UCSDTask(key, title, uid, asking_price, description);
        push.setValue(task);

        GeoFire geoFire = new GeoFire(ref.child("geofire"));
        geoFire.setLocation(key, new GeoLocation(lat, lon));
        Toast.makeText(this, "Task Submitted", Toast.LENGTH_SHORT).show();
    }

    public void openPlaceSelector(View view){
        Intent intent = new Intent(this, LocationPickerActivity.class);
        Bundle extras = getIntent().getExtras();
        float zoom;
        if (extras != null) {
            zoom = extras.getFloat("Zoom");
        }
        else {
            zoom = 15.0f;
        }

        intent.putExtra("Longitude", longitude);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Zoom", zoom);
        startActivityForResult(intent, CHANGE_LOCATION);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHANGE_LOCATION) {
            if (resultCode == RESULT_OK) {
                latitude = data.getDoubleExtra("Latitude", latitude);
                longitude = data.getDoubleExtra("Longitude", longitude);

                // Change the text in the box to the selected place
                Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<android.location.Address> addresses;
                try {
                    addresses = gcd.getFromLocation(latitude, longitude, 1);
                } catch (IOException e) {
                    addresses = null;
                }

                if (addresses == null || addresses.size() == 0) {
                    location.setText("" + latitude + ", " + longitude);
                }
                else {
                    location.setText(addresses.get(0).getAddressLine(0));
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // add the back button in actionbar
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This method update the status of the post whether the user wants to have it available
     * for his/her facebook friend to see.
     *
     * @param view
     * @return privacy_public which is the status of the publicity.
     */
    public boolean onRadioButtonClicked(View view) {
        boolean privacy_public = false;
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.public_radio:
                if(checked)
                    privacy_public = true;
                break;
            case R.id.private_radio:
                if(checked)
                    privacy_public = false;
                break;
        }
        return privacy_public;
    }
}
