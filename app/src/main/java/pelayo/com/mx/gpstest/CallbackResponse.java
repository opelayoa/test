package pelayo.com.mx.gpstest;

/**
 * Created by Daniel Pelayo on 25/12/2018.
 */

/**
 * Callback estándar utilizado para devolver valores desde otro proceso aquel donde no sabemos cuando terminará
 */
public interface CallbackResponse<T> {

    /**
     * Respuesta positiva desde el proceso hacia aquel que utilice este callback
     *
     * @param response respuesta enviada
     */
    void sendValue(T response);

    /**
     * Respuesta negativa desde el proceso hacia aquel que utilize este callback
     *
     * @param e error generado
     */
    void error(Exception e);

}