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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.HashMap;
import java.util.List;

public class PatientDetailsActivity extends FragmentActivity implements OnMapReadyCallback  {

    private GoogleMap mMap;
    private Location mlastLocation;
    private LocationRequest mLocationRequest;
    private LatLng pickupLocation;
    private Button mlogout,mRequest,mSetting;
    private boolean requestbutton=false;  // for toggle button
    private Marker pickupMarker;
    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int REQUEST_CHECK_SETTINGS = 200;
    private Marker userMarker;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean zoomToUserLocation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_details);
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
        mRequest=(Button)findViewById(R.id.request);
        mSetting=(Button) findViewById(R.id.settings);
        mlogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent i=new Intent(PatientDetailsActivity.this,MainActivity.class);
                startActivity(i);
                finish();
                return;
            }
        });
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(requestbutton){


                    // remove request uber
                    requestbutton=false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListener);

                    if(driverID !=null){
                        DatabaseReference driverRef= FirebaseDatabase.getInstance().getReference().child("users").child("providers").child(driverID);
                        driverRef.setValue(true);
                        driverID=null;
                    }

                    driverFound=false;
                    radius=1;

                    String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();  //get user who is register on uber now
                    DatabaseReference ref= FirebaseDatabase.getInstance().getReference("customerRequest");  // create customerRequest in database
                    GeoFire geoFire=new GeoFire(ref);
                    geoFire.removeLocation(userId);

                    if(pickupMarker !=null){
                        pickupMarker.remove();
                    }
                    mRequest.setText("call uber");



                }
                else
                {
                    //make a request uber
                    requestbutton=true;

                    //save location in database of firebase
                    String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();  //get user who is register on uber now
                    DatabaseReference ref= FirebaseDatabase.getInstance().getReference("customerRequest");  // create customerRequest in database
                    GeoFire geoFire=new GeoFire(ref);
                    geoFire.setLocation(userId,new GeoLocation(mlastLocation.getLatitude(),mlastLocation.getLongitude()));

                    //add marker for user
                    pickupLocation = new LatLng(mlastLocation.getLatitude(), mlastLocation.getLongitude());
                    pickupMarker=mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pick up here"));
                    userMarker.remove();
                    mRequest.setText("Getting your driver ...");
                    mRequest.setEnabled(false);
                    getClosestDriver();

                }

            }
        });

        mSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(PatientDetailsActivity.this,PatientSettingActivity.class);
                startActivity(i);
                return;
            }
        });
    }

    private int radius=1;
    private boolean driverFound=false;
    private String driverID;
    GeoQuery geoQuery;
    private void getClosestDriver(){
        //get location of drivers from database of firebase
        DatabaseReference driverlocation=FirebaseDatabase.getInstance().getReference().child("driverAvailable");
        GeoFire geoFire=new GeoFire(driverlocation);

        // creates a new query around [userlocation_latitude, userlocation_longtitude] with a radius of 1 kilometers
        geoQuery=geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {  //The location of a key(driverId) now matches the query criteria.
                if(!driverFound && requestbutton){
                    driverFound=true;
                    driverID=key;                                         // closest driver id

                    // send patientId to driverMapActivity by put a new child("patientId") below driverID in database and in driverMapActivity we will receive it
                    DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("users").child("providers").child(driverID);
                    String patientId=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map=new HashMap();
                    map.put("patientId",patientId);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    mRequest.setText("Looking for driver location ...");
                }
            }

            @Override
            public void onKeyExited(String key) {   //The location of a key no longer matches the query criteria.

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {  //The location of a key changed but the location still matches the query criteria

            }

            @Override
            public void onGeoQueryReady() {  //All current data has been loaded from the server and all initial events have been fired.
                if(!driverFound){
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {  //There was an error while performing this query, e.g. a violation of security rules.

            }
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        driverLocationRef=FirebaseDatabase.getInstance().getReference().child("driverdWorking").child(driverID).child("l"); //child l have latitude(0) & logtitude(1)
        driverLocationRefListener=driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {          //when location change  make this code
                if(dataSnapshot.exists() && requestbutton){
                    List<Object> map=(List<Object>)dataSnapshot.getValue();
                    double locationLat=0;
                    double locationLag=0;
                    mRequest.setText("driver found");
                    mRequest.setEnabled(true);
                    if(map.get(0) !=null){
                        locationLat =Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1) !=null){
                        locationLag =Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatng=new LatLng(locationLat,locationLag);
                    if(mDriverMarker!=null){
                        mDriverMarker.remove();
                    }

                    //show the distance between customer and driver
                    Location locCustomer=new Location("");
                    locCustomer.setLatitude(pickupLocation.latitude);
                    locCustomer.setLongitude(pickupLocation.longitude);

                    Location locDriver=new Location("");
                    locDriver.setLatitude(driverLatng.latitude);
                    locDriver.setLongitude(driverLatng.longitude);

                    Float distance=locCustomer.distanceTo(locDriver);
                    if(distance<100){
                        mRequest.setText("driver here");
                    }
                    else{
                        mRequest.setText("driver found : "+ String.valueOf(distance));
                    }

                  //  mDriverMarker=mMap.addMarker(new MarkerOptions().position(driverLatng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)));
                    mDriverMarker=mMap.addMarker(new MarkerOptions().position(driverLatng).title("your driver"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
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
                            // or determine my location by blue point
                            //  mMap.setMyLocationEnabled(true);
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
                        resolvable.startResolutionForResult(PatientDetailsActivity.this, REQUEST_CHECK_SETTINGS);
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

        if(userMarker != null && mDriverMarker!=null)
            drawShortestPath();

        if (zoomToUserLocation)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 20));


        mlastLocation=location;
    }

    private void drawShortestPath() {
        zoomToUserLocation = false;
            GoogleMapUtil.zoomAndFitLocations(mMap, 150, userMarker.getPosition(), mDriverMarker.getPosition());
            GoogleMapUtil.getAndDrawPath(this, mMap, userMarker.getPosition(), mDriverMarker.getPosition(), null, true);
    }
}
