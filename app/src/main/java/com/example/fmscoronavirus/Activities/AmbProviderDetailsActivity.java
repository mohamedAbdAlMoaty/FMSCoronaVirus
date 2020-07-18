package com.example.fmscoronavirus.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.fmscoronavirus.R;
import com.example.fmscoronavirus.Utils.GoogleMapUtil;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class AmbProviderDetailsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int REQUEST_CHECK_SETTINGS = 200;
    private Marker userMarker;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean zoomToUserLocation = true;
    private Location mlastLocation;

    private Button mlogout;
    private String patientId="";
    private boolean isLoggingOut=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amb_provider_details);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    showUserLocationOnMap(location);
                    break;
                }
            }
        };


        mlogout=(Button)findViewById(R.id.logout);
        mlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isLoggingOut=true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent i=new Intent(AmbProviderDetailsActivity.this,MainActivity.class);
                startActivity(i);
                finish();
                return;
            }
        });
        receiveCustomer();
    }

    private void receiveCustomer(){
        String driverId=FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference receiveCudtomerRef= FirebaseDatabase.getInstance().getReference().child("users").child("providers").child(driverId).child("patientId");
        receiveCudtomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    patientId=dataSnapshot.getValue().toString();      // receive customerId from customerMapActivity
                    receiveCustomerLocation();
                }
                else
                {
                    patientId="";
                    if(mCustomerMarker !=null){
                        mCustomerMarker.remove();
                    }
                    if(customerLocationRefListener!=null){
                        customerLocationRef.removeEventListener(customerLocationRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private Marker mCustomerMarker;
    private DatabaseReference customerLocationRef;
    private ValueEventListener customerLocationRefListener;
    private void receiveCustomerLocation(){

        customerLocationRef=FirebaseDatabase.getInstance().getReference().child("customerRequest").child(patientId).child("l"); //child l have latitude(0) & logtitude(1)
        customerLocationRefListener= customerLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {          //when location change  make this code
                if(dataSnapshot.exists() && !patientId.equals("")){
                    List<Object> map=(List<Object>)dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLag=0;
                    if(map.get(0) !=null){
                        locationLat =Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) !=null){
                        locationLag =Double.parseDouble(map.get(1).toString());
                    }
                    LatLng customerLatng=new LatLng(locationLat,locationLag);
                    mCustomerMarker=mMap.addMarker(new MarkerOptions().position(customerLatng).title("your customer"));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isLoggingOut){
            disconnectDriver();
        }
    }



    private void disconnectDriver(){
       // (Deprecated)(LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(mLocationCallback);
        //delete location in database of firebase
        String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();  //get user who is register on uber now
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("driverAvailable");
        GeoFire geoFire=new GeoFire(ref);
        geoFire.removeLocation(userId);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // to determine my location by blue point
        //  mMap.setMyLocationEnabled(true);
        initUserLocation();
    }

    private void initUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            showUserLocationOnMap(location);
                        }
                    }
                });
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                createLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(AmbProviderDetailsActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                createLocationUpdates();
            } else {
                Toast.makeText(this, "Can't Detect Your Location", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, null /* Looper */);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initUserLocation();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showUserLocationOnMap(Location location) {


        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null)
            userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
        else
            userMarker.setPosition(userLatLng);

        if(userMarker != null && mCustomerMarker!=null)
            drawShortestPath();

        if (zoomToUserLocation)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 20));


        mlastLocation = location;
        //save location in database of firebase
        String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();  //get user who is register on uber now
        DatabaseReference refAvalible= FirebaseDatabase.getInstance().getReference("driverAvailable");  // create driveavalible in database
        DatabaseReference refWorking= FirebaseDatabase.getInstance().getReference("driverdWorking");  // create driverdWorking in database
        GeoFire geoFireAvalible=new GeoFire(refAvalible);
        GeoFire geoFireWorking=new GeoFire(refWorking);
        switch (patientId)
        {
            case "":          //the customer dont make an order so custmerId is empty so put location in driverAvailable
                geoFireWorking.removeLocation(userId);
                geoFireAvalible.setLocation(userId,new GeoLocation(mlastLocation.getLatitude(),mlastLocation.getLongitude()));
                break;

            default:      //the customer  make an order so custmerId is exist so put location in driverWorking
                geoFireAvalible.removeLocation(userId);
                geoFireWorking.setLocation(userId,new GeoLocation(mlastLocation.getLatitude(),mlastLocation.getLongitude()));
                break;
        }

    }

    private void drawShortestPath() {
        zoomToUserLocation = false;
            GoogleMapUtil.zoomAndFitLocations(mMap, 150, userMarker.getPosition(), mCustomerMarker.getPosition());
            GoogleMapUtil.getAndDrawPath(this, mMap, userMarker.getPosition(), mCustomerMarker.getPosition(), null, true);

    }
}
