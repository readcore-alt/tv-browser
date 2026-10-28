package com.custom.tvbrowser;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private View punteroMouse;

    // Coordenadas del puntero en la pantalla
    private float punteroX = 960f; // Centro horizontal (Full HD)
    private float punteroY = 540f; // Centro vertical
    private final float VELOCIDAD = 35f; // Velocidad de movimiento por pulsación

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);

        // 1. Motor de Google Chrome (WebView ultra optimizado para TV)
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false); // Reproducir videos sin trabas
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        // Fingir ser un navegador de PC moderno para que cargue las mejores versiones web
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.google.com");

        rootLayout.addView(webView);

        // 2. El Puntero Virtual (Círculo rojo brillante visible para el mando)
        punteroMouse = new View(this);
        int tamanoPuntero = 28; // Tamaño en píxeles
        FrameLayout.LayoutParams paramsPuntero = new FrameLayout.LayoutParams(tamanoPuntero, tamanoPuntero);
        punteroMouse.setLayoutParams(paramsPuntero);

        GradientDrawable estiloPuntero = new GradientDrawable();
        estiloPuntero.setShape(GradientDrawable.OVAL);
        estiloPuntero.setColor(Color.parseColor("#FF1744")); // Rojo vibrante fácil de ver
        estiloPuntero.setStroke(3, Color.WHITE); // Borde blanco
        punteroMouse.setBackground(estiloPuntero);

        rootLayout.addView(punteroMouse);
        actualizarPosicionPuntero();

        setContentView(rootLayout);
    }

    private void actualizarPosicionPuntero() {
        if (punteroMouse != null) {
            punteroMouse.setX(punteroX);
            punteroMouse.setY(punteroY);
        }
    }

    // 3. Control del Puntero y Clic con el Mando de la Tele
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int anchoPantalla = getWindow().getDecorView().getWidth();
        int altoPantalla = getWindow().getDecorView().getHeight();

        if (anchoPantalla == 0) anchoPantalla = 1920;
        if (altoPantalla == 0) altoPantalla = 1080;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                punteroY -= VELOCIDAD;
                if (punteroY < 30) {
                    punteroY = 30;
                    webView.scrollBy(0, -120); // Hace scroll hacia arriba
                }
                actualizarPosicionPuntero();
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                punteroY += VELOCIDAD;
                if (punteroY > altoPantalla - 50) {
                    punteroY = altoPantalla - 50;
                    webView.scrollBy(0, 120); // Hace scroll hacia abajo
                }
                actualizarPosicionPuntero();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                punteroX -= VELOCIDAD;
                if (punteroX < 10) punteroX = 10;
                actualizarPosicionPuntero();
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                punteroX += VELOCIDAD;
                if (punteroX > anchoPantalla - 40) punteroX = anchoPantalla - 40;
                actualizarPosicionPuntero();
                return true;

            // Al presionar OK: SIMULA UN CLIC REAL DEL RATÓN en la posición exacta del puntero
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                simularClicRaton(punteroX, punteroY);
                return true;

            // Botón Atrás: Regresar en el historial de navegación si se puede
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                break;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void simularClicRaton(float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent eventoAbajo = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        webView.dispatchTouchEvent(eventoAbajo);
        eventoAbajo.recycle();

        MotionEvent eventoArriba = MotionEvent.obtain(downTime, eventTime + 50, MotionEvent.ACTION_UP, x, y, 0);
        webView.dispatchTouchEvent(eventoArriba);
        eventoArriba.recycle();
    }
}
