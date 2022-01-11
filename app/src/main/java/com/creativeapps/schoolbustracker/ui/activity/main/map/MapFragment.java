package com.creativeapps.schoolbustracker.ui.activity.main.map;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.creativeapps.schoolbustracker.R;
import com.creativeapps.schoolbustracker.data.Util;
import com.creativeapps.schoolbustracker.data.network.models.Parent;
import com.creativeapps.schoolbustracker.data.network.models.Payload;
import com.creativeapps.schoolbustracker.ui.activity.login.LoginActivity;
import com.creativeapps.schoolbustracker.ui.activity.main.MainActivity;
import com.creativeapps.schoolbustracker.ui.activity.main.MainActivityModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

public class MapFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    final String TAG = "MapFragment";

    //constants to set the mode of the map
    private static final int AUTO = 0;//AUTO: map is adjusted automatically to view both the parent's home and driver locations
    private static final int MANUAL = 1;//MANUAL: settings of the map (zoom level, position, ..) adjusted manually by the user
    //default zoom level for the Google map
    private static final int DEFAULT_ZOOM = 13;
    //view model of the main activity
    private MainActivityModel mViewModel;
    private ImageView mRefreshBusLocation;
    //Google map object
    private GoogleMap mGoogleMap;
    //parent object that holds parent information
    private Parent mParent;

    private int mViewMode = AUTO;
    //bus marker
    private Marker mBusMarker;
    //home marker
    private Marker mParentHomeMarker;
    //school marker
    private Marker mSchoolMarker;

    //Text view that display the connectivity status of the app (online or offline)
    private TextView mStatus;
    //Layout that contains the connectivity status
    private RelativeLayout mStatusLayout;

    private long last_time=0;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //inflate the layout
        View root = inflater.inflate(R.layout.fragment_map, container, false);
        //instantiate the view model object
        mViewModel = ((MainActivity) getActivity()).createViewModel();

        return root;

    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        //show location image view is hidden until Google map is loaded correctly
        mRefreshBusLocation = view.findViewById(R.id.refreshBusLocation);
        mRefreshBusLocation.setVisibility(View.INVISIBLE);
        mRefreshBusLocation.setOnClickListener(this);

        mStatus = view.findViewById(R.id.status);
        mStatusLayout = view.findViewById(R.id.statusLayout);

        //start Google map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            //center the map on the current location of the bus
            case R.id.refreshBusLocation:
                mViewMode = AUTO;
                adjustMapToPickDropBusLocations();
                break;
            default:
                break;
        }
    }

    /*make the status "online" */
    private void setOnlineTitle()
    {
        mStatusLayout.setVisibility(View.INVISIBLE);
    }

    /*make the status "offline" with red background*/
    private void setOfflineTitle()
    {
        mStatusLayout.setVisibility(View.VISIBLE);
        mStatus.setText("offline");
        mStatusLayout.setBackgroundColor(Color.RED);
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop observing live data when the fragment is paused
        mViewModel.getParent().removeObservers(this);
        mViewModel.getDriverRealTimeData().removeObservers(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //observe changes for the position of the driver
        mViewModel.getDriverRealTimeData().observe(this, new PosDriverObserver());
        //observe changes for connectivity status
        mViewModel.getConnectivityStatus().observe(this, new ConnectivityStatusObserver());
    }





    @Override
    public void onMapReady(GoogleMap googleMap) {
        //when the map is ready
        this.mGoogleMap = googleMap;

        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if(reason!=REASON_DEVELOPER_ANIMATION)
                    mViewMode = MANUAL;
            }
        });

        //get the last saved parent information from the SharedPreference
        mParent = Util.getSavedObjectFromPreference(getContext(),
                "mPreference", "Parent", Parent.class);

        //check if there are already saved data for the parent
        if(mParent !=null) {
            //if so, update the parent information by getting the latest parent data from the server
            mViewModel.getParentServer(mParent.getCountry_code(), mParent.getTel_number(), mParent.getSecretKey());

            //observe changes for parent information
            mViewModel.getParent().observe(this, new ParentObserver());
        }


        //update the gui on the map after the map is ready. The function will make the "refresh bus"
        // image view visible to the user
        updateMapUI();
    }

    /*update the gui on the map after the map is ready. The function will make the "show location"
    and "show homes" image views visible to the user*/
    private void updateMapUI() {
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mRefreshBusLocation.setVisibility(View.VISIBLE);
    }

    /*put the pickup/drop-off location of the parent on the map*/
    private void updatePickupDropoffMarker(LatLng parentHomeLocation) {
        if (mGoogleMap == null)
            return;

        //define marker options with a home icon
        MarkerOptions marker_option = new MarkerOptions()
                .position(parentHomeLocation)
                .title("Your pickup/drop-off location")
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.home));

        if (mParentHomeMarker == null) {
            // adding marker
            mParentHomeMarker = mGoogleMap.addMarker(marker_option);
        } else //update the marker position
            mParentHomeMarker.setPosition(parentHomeLocation);

    }

    /*adjust map to show both the parent's home and the bus locations*/
    private void adjustMapToPickDropBusLocations() {
        if (mGoogleMap == null) {
            return;
        }



        if(mViewMode==AUTO)
        {
            //construct LatLngBounds that include the parent's home and the bus locations
            LatLngBounds.Builder builder = new LatLngBounds.Builder();


            //check if the parent's home marker already on map
            if (mParentHomeMarker != null) {
                //if so, include it in the LatLngBounds object
                builder.include(mParentHomeMarker.getPosition());
                //check if the bus marker already on map
                if (mBusMarker != null)
                    //if so, include it in the LatLngBounds object
                    builder.include(mBusMarker.getPosition());

                try {
                    //build the bound with included homes locations
                    LatLngBounds homeBusLocationsBounds = builder.build();

                    //adjust map with animation to show both the parent's home and the bus locations
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(homeBusLocationsBounds,200));

                } catch (Exception e) {

                }
            }
            else
            {
                //check if the bus marker already on map
                if (mBusMarker != null)
                {
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(
                            mBusMarker.getPosition()).zoom(DEFAULT_ZOOM).build();

                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        }
        else if(mViewMode==MANUAL)
        {
            CameraPosition cameraPosition = new CameraPosition.Builder().target(
                    mBusMarker.getPosition()).zoom(mGoogleMap.getCameraPosition().zoom).build();

            mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }


    }

    /*set bus icon marker location on Google map. The function initialize the bus marker if it is
    not initialized yet, or move the marker with animation to a new position*/
    private void setBusLocation(Payload bus_real_time_data) {

        //verify that Google map is loaded correctly before proceed
        if (mGoogleMap == null) {
            return;
        }
        //if the bus marker is not initialized, initialize it and add it to the map on the
        // specified location
        if (mBusMarker == null) {
            //define marker options with a bus icon
            MarkerOptions marker_option = new MarkerOptions()
                    .position(new LatLng(bus_real_time_data.lat, bus_real_time_data.lng))
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.school_bus));

            // adding marker
            mBusMarker = mGoogleMap.addMarker(marker_option);
            //mBusMarker.setTitle(bus_real_time_data.distance+"");
            mBusMarker.showInfoWindow();
        } else //if the bus marker is already initialized before, move it to the new location
        {
            //convert from LatLng to Location object
            Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(bus_real_time_data.lat);
            location.setLongitude(bus_real_time_data.lng);
            Log.d(TAG, "setBusLocation: " + bus_real_time_data.distance + "," + System.currentTimeMillis() + "," +last_time);

            double speed = bus_real_time_data.speed;
//            if(speed == 0)
//            {
//                if(last_time!=0)
//                {
//                    speed = 3600*bus_real_time_data.distance / (System.currentTimeMillis()-last_time);
//                }
//            }

            mBusMarker.setTitle(String.format("%.2f", speed)+" km/h");

            last_time = System.currentTimeMillis();

            mBusMarker.showInfoWindow();
            //Helper method for smooth animation
            animateBusMarker(mBusMarker, location);
        }

    }

    /*set school icon marker location on Google map. The function initialize the school marker if it is
    not initialized yet, or move the marker to a new position*/
    private void setSchoolLocation(LatLng latLng) {
        if (mGoogleMap == null)
            return;

        //define marker options with a home icon
        MarkerOptions marker_option = new MarkerOptions()
                .position(latLng)
                .title("School location")
                .flat(true)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.school));

        if (mSchoolMarker == null) {
            // adding marker
            mSchoolMarker = mGoogleMap.addMarker(marker_option);
        } else
            mSchoolMarker.setPosition(latLng);
    }

    /*helper function to animate the change in position of the bus location so that it appears with smooth motion on
    the map*/
    public void animateBusMarker(final Marker marker, final Location location) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();
        final double startRotation = marker.getRotation();
        final long duration = 500;
        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);

                double lng = t * location.getLongitude() + (1 - t)
                        * startLatLng.longitude;
                double lat = t * location.getLatitude() + (1 - t)
                        * startLatLng.latitude;

                float rotation = (float) (t * location.getBearing() + (1 - t)
                        * startRotation);

                marker.setPosition(new LatLng(lat, lng));
                marker.setRotation(rotation);

                adjustMapToPickDropBusLocations();

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }





    /*observe changes for the position of the driver*/
    private class PosDriverObserver implements Observer<Payload> {

        @Override
        public void onChanged(@Nullable Payload posDriver) {
            if (posDriver == null) return;

            Log.d(TAG,
                    "PosDriverObserver => onChanged: posDriver " + posDriver.lat + ", " +
                            posDriver.lng);
            //the bus change its location so update it on Google map
            setBusLocation(posDriver);
            adjustMapToPickDropBusLocations();
        }
    }


    //observer for parent data, which when changed, the status of the app becomes "online" and the
    // updated parent data is saved to SharedPreferences. If the parent is not verified, the user
    // is redirected to the login activity
    private class ParentObserver implements Observer<Parent> {

        @Override
        public void onChanged(@Nullable Parent parent) {
            ((MainActivity)getActivity()).showHideProgressBar(false);
            if (parent == null) {

                //get the last saved parent information from the SharedPreference
                mParent = Util.getSavedObjectFromPreference(getContext(),
                        "mPreference", "Parent", Parent.class);
            } else {
                mParent = parent;
                Log.d(TAG, "ParentObserver => onChanged: " + parent.getName());
                Util.saveObjectToSharedPreference(getContext(),
                        "mPreference", "Parent", parent);
            }

            //if the parent is not verified, go to the login activity
            if (mParent.getVerified() != 1) {
                //logout
                ((MainActivity)getActivity()).logout();
            } else {
                //if the driver location information available
                if (mParent.getDriver() != null && mParent.getDriver().getLast_latitude() != null &&
                        mParent.getDriver().getLast_longitude() != null) {
                    //set bus icon marker location on Google map
                    setBusLocation(new Payload(mParent.getDriver().getLast_latitude(), mParent.getDriver().getLast_longitude()));
                }
                //if the school location information available
                if (mParent.getSchool() != null && mParent.getSchool().getLast_latitude() != null &&
                        mParent.getSchool().getLast_longitude() != null) {
                    //set school icon marker location on Google map
                    setSchoolLocation(new LatLng(mParent.getSchool().getLast_latitude(),
                            mParent.getSchool().getLast_longitude()));
                }
                //if the parent location information available
                if (mParent.getAddress_latitude() != null && mParent.getAddress_longitude() != null) {
                    //set parent's home icon marker location on Google map

                    updatePickupDropoffMarker(new LatLng(mParent.getAddress_latitude(),
                            mParent.getAddress_longitude()));
                }
                //adjust map to show both the parent's home and the bus locations
                adjustMapToPickDropBusLocations();
            }
        }

    }

    /*observer for connectivity status. If no connection with the backend web socket, the app will
    display a red bar at the bottom*/
    private class ConnectivityStatusObserver implements Observer<Boolean> {

        @Override
        public void onChanged(Boolean connectivityStatus) {
            if(connectivityStatus == null)
                return;

            if(connectivityStatus)
                setOnlineTitle();
            else
                setOfflineTitle();
        }
    }
}