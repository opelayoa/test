package pelayo.com.mx.gpstest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.CopyOnWriteArrayList;

public class GeolocationHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = GeolocationHelper.class.getSimpleName();

    /**
     * Constante que indica cada cuanto tiempo se tiene que obtener la localización
     * Es el tiempo más rápido
     */
    private static final int FASTEST_INTERVAL = 6 * 1000;
    /**
     * Constante que indica cada cuanto tiempo se tiene que obtener la localización
     */
    private static final int INTERVAL = 10 * 1000;

    /**
     * Constante que indica en cuánto tiempo caduca la localización más reciente
     */
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private CopyOnWriteArrayList<CallbackResponse<Location>> mCallbackList;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private Location lastLocation;

    private Context context;

    public GeolocationHelper(Context context) {
        this.context = context;
        buildGoogleApiClient();
        initLocationRequest();
        mCallbackList = new CopyOnWriteArrayList<>();
    }

    /**
     * Método sobrescrito de la interfaz GoogleApiClient.ConnectionCallbacks, llamado cuando
     * la conexión con el sistema de localización de Android ha sido exitosa
     *
     * @param bundle datos enviados
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnected(Bundle)
     */
    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected(@Nullable Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationEnabled()) {
                lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            } else {
                for (CallbackResponse<Location> listener : mCallbackList) {
                    listener.error(new Exception("Location permission not granted"));
                }
            }
        } else {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    /**
     * Método sobrescrito de la interfaz GoogleApiClient.ConnectionCallbacks, llamado cuando
     * la conexión con el sistema de localización de Android ha sido suspendida
     *
     * @param i código de suspension
     * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(int)
     */
    @Override
    public void onConnectionSuspended(int i) {
        String message = "Failed connection";
        if (i == CAUSE_NETWORK_LOST) {
            message = "Connection lost";
        } else if (i == CAUSE_SERVICE_DISCONNECTED) {
            message = "Service disconnected";
        }
        for (CallbackResponse<Location> listener : mCallbackList) {
            listener.error(new Exception(message));
        }
    }

    /**
     * Método sobrescrito de la interfaz GoogleApiClient.OnConnectionFailedListener, llamado cuando
     * la conexión con el sistema de localización de Android ha fallado
     *
     * @param connectionResult resultado del error (que lo originó)
     * @see com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener#onConnectionFailed(ConnectionResult)
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        for (CallbackResponse<Location> listener : mCallbackList) {
            listener.error(new Exception("code: " + connectionResult.getErrorCode() + ", message: " + connectionResult.getErrorMessage()));
        }
    }

    /**
     * Método sobrescrito de la interfaz LocationListener, el cual es llamado por el sistema
     * administrador de localización de Google cuando detecta que la localización del usuario ha cambiado
     *
     * @param location localización actualizada
     * @see LocationListener
     */
    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, lastLocation)) {
            lastLocation = location;
        }
        for (CallbackResponse<Location> callback : mCallbackList) {
            callback.sendValue(lastLocation);
        }
    }

    /**
     * Compara la nueva localización con la vieja y dice si la nueva es mejor
     *
     * @param location            nueva localización
     * @param currentBestLocation actual localización
     * @return true si la nueva es mejor
     */
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Compara los proveedores de localización son los mismos
     *
     * @param provider        proveedor de la localización nueva
     * @param currentProvider proveedor de la localización actual
     * @return true si son el mismo
     */
    private boolean isSameProvider(String provider, String currentProvider) {
        if (provider == null) {
            return currentProvider == null;
        }
        return provider.equals(currentProvider);
    }

    /**
     * Obtiene la mejor localización de forma síncrona
     *
     * @return mejor localización actual
     */
    @SuppressWarnings("MissingPermission")
    public Location getSyncLocation() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (isBetterLocation(location, lastLocation)) {
            lastLocation = location;
        }
        return lastLocation;
    }

    /**
     * Registra el Callback para obtener la última localización conocida por el sistema Android
     *
     * @param callback callback donde enviará la localización
     * @throws IllegalArgumentException si algunos de los parámetros es null
     */
    public void addLocationListener(CallbackResponse<Location> callback) {
        if (!mCallbackList.contains(callback)) {
            mCallbackList.add(callback);
        }
        if (mCallbackList.size() == 1) {
            googleApiClient.connect();
        }
    }

    /**
     * Borra el Callback para obtener la última localización conocida por el sistema Android
     *
     * @param callback callback a borrar
     * @throws IllegalArgumentException si algunos de los parámetros es null
     */
    public void removeLocationListener(CallbackResponse<Location> callback) {
        if (mCallbackList.contains(callback)) {
            mCallbackList.remove(callback);
            if (mCallbackList.size() == 0) {
                try {

                    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                    googleApiClient.disconnect();
                } catch (IllegalStateException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Crea la instancia de la clase GoogleApiClient el cual es el manejador del sistema de localización
     * de Android
     */
    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Crea la instancia de la clase LocationRequest el cual es la configuración de localización que
     * tendrá el GoogleApiClient
     */
    private void initLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Verifica si la localización del dispositivo está disponible
     *
     * @return true si puede obtener la localización
     */
    public boolean locationEnabled() {
        boolean isGrantedCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isGrantedFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return isGrantedCoarseLocation || isGrantedFineLocation;
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}