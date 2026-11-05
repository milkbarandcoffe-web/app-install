package com.dz.install;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.pm.PackageInstaller;
import android.provider.Settings;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.text.InputType;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.regex.Pattern;

/**
 * APK "NOME install" - idea e sviluppo by Daniel Zavan.
 * Primo avvio: apre l'INSTALLER dell'app (login, permessi e installazione dentro l'app).
 * L'INSTALLER manda il link dell'app installata (messaggi dz-app / dz-apri):
 * l'APK lo salva e dagli avvii successivi apre direttamente l'app.
 * Ingranaggio in basso a sinistra = menu.
 * DZ App (BuildConfig.UNICO): un APK per tutte le app. Elenco delle app installate, chiave dal link della mail
 * (dzapp://chiave?k=...) o scritta a mano; l'INSTALLER riceve la chiave e il link del servizio LICENZE.
 */
public class MainActivity extends Activity {

  static final int NERO = 0xFF0A0A0A;
  static final int[] COLORI = {0xFF9EFF9E, 0xFF5CE1E6, 0xFF7AA2FF, 0xFFC084FC, 0xFFFF7AB6};
  static final float TITOLO_SP = 36f;
  static final String FIRMA = "idea e sviluppo by Daniel Zavan";
  static final Pattern LINK_APP =
      Pattern.compile("^https://script\\.google\\.com/(a/[^/?#]+/)?macros/s/[A-Za-z0-9_-]+/exec$");

  /**
   * Nasconde il banner "creata da un utente di Google Apps Script" nel documento host
   * (script.google.com), senza toccare l'iframe dell'app. Installa anche, una volta per pagina,
   * un osservatore DOM (banner che ricompare) e un ascoltatore dei messaggi "dz-..." dell'INSTALLER.
   * Risposta: "ok", "no" (assente), "wait", "forzato"; con "+" se e arrivato il segnale "pronto".
   */
  static String jsBanner(boolean forza) {
    return "(function(){"
        + "function nasc(f){"
        + "var T=['creata da un utente di Google Apps Script','created by a Google Apps Script user'];"
        + "var n=document.querySelectorAll('div,header,nav,section,span,a,p'),m=null,ml=1e9,g=false;"
        + "for(var i=0;i<n.length;i++){var e=n[i];if(e.querySelector('iframe'))continue;"
        + "if(e.closest&&e.closest('[data-dzhb]')){g=true;continue;}"
        + "var t=e.textContent||'';"
        + "if(t.length<=ml){for(var k=0;k<T.length;k++){if(t.indexOf(T[k])!==-1){m=e;ml=t.length;break;}}}}"
        + "if(!m)return g?'ok':'no';"
        + "var x=m,b=null,W=window.innerWidth*0.7;"
        + "for(var s=0;s<8&&x&&x!==document.body&&x!==document.documentElement"
        + "&&!x.querySelector('iframe');s++){"
        + "var r=x.getBoundingClientRect();"
        + "if(r.top<80&&r.height>0&&r.height<220&&r.width>W)b=x;"
        + "else if(b&&r.height>=220)break;"
        + "x=x.parentElement;}"
        + "if(!b){if(!f)return 'wait';"
        + "m.setAttribute('data-dzhb','1');m.style.setProperty('display','none','important');return 'forzato';}"
        + "b.setAttribute('data-dzhb','1');"
        + "b.style.setProperty('display','none','important');"
        + "if(document.body){document.body.style.setProperty('margin-top','0','important');"
        + "document.body.style.setProperty('padding-top','0','important');}"
        + "return 'ok';}"
        + "if(!window.__dzhbObs&&window.MutationObserver){try{"
        + "window.__dzhbObs=new MutationObserver(function(){try{nasc(false);}catch(e){}});"
        + "window.__dzhbObs.observe(document.documentElement,{childList:true,subtree:true,characterData:true});"
        + "}catch(e){}}"
        + JS_ASCOLTA
        + "return nasc(" + forza + ")+(window.__dzPronto?'+':'');"
        + "})()";
  }

  /** Ascoltatore dei messaggi dell'INSTALLER (postMessage dall'iframe alla pagina principale). */
  static final String JS_ASCOLTA =
      "if(!window.__dzpL){window.__dzpL=1;try{window.addEventListener('message',function(e){"
      + "var d=e&&e.data;if(typeof d!=='string')return;"
      + "if(d==='dz-pronto'||d==='claudeexe-pronto')window.__dzPronto=1;"
      + "if(d.indexOf('dz-')===0&&window.DZ&&window.DZ.msg){try{window.DZ.msg(d);}catch(x){}}"
      + "});}catch(e){}}";

  /** Tendina sopra le pagine dei permessi Google (non tocca la pagina: la spinge sotto). */
  static final String TESTO_AIUTO =
      "Google chiede il permesso per l'app di Daniel Zavan.   ▲ nascondi\n"
      + "1. Se vedi «Google non ha verificato questa app»: tocca «Avanzate» (in basso a sinistra).\n"
      + "2. Scorri giù e tocca «Apri … (non sicura)».\n"
      + "3. Se compaiono delle caselle: «Seleziona tutto», poi «Continua».";
  static final String AIUTO_CHIUSO = "▼ Aiuto per i permessi Google: tocca qui";

  /** Tendina della finestra "Togli permessi Google" (aperta dal menu). */
  static final String TESTO_PERMESSI =
      "Togli i permessi Google.   ▲ nascondi\n"
      + "1. Tocca il nome dell'app (INSTALLER o il nome dell'app installata).\n"
      + "2. Tocca il pulsante per eliminare le connessioni, poi conferma.\n"
      + "3. Tocca «TORNA A …» in alto: l'app si ricarica e Google chiede di nuovo i permessi.";
  static final String URL_INTERRUTTORE = "https://script.google.com/home/usersettings";
  static final String URL_PERMESSI = "https://myaccount.google.com/connections";

  /**
   * Dopo il login: "1" se la pagina e un'app Apps Script (riquadro interno) o la pagina
   * "Autorizzazione necessaria", "0" se e una pagina di errore di Google.
   */
  static final String JS_PAGINA_APP =
      "(function(){try{if(document.querySelector('iframe'))return '1';"
      + "var t=(document.body&&document.body.innerText)||'';"
      + "return /Review permissions|Rivedi (le )?autorizzazioni|Esamina (le )?autorizzazioni/i.test(t)?'1':'0';"
      + "}catch(e){return '1';}})()";

  SharedPreferences prefs;
  // Finestra aperta dal menu (interruttore API o permessi Google): alla chiusura l'app si ricarica.
  WebView finMenu;
  boolean menuSpegni = false;
  // Dopo il login Google: se arriva la pagina di errore di Google, una ricarica automatica.
  boolean daLogin = false;
  boolean verificaLogin = false;
  long ultimaRicarica = 0;
  // DZ App (APK unico): elenco delle app con le chiavi, app aperta, chiave in installazione, ultimo link aperto.
  View casa;
  LinearLayout elenco;
  String corrente = "";
  String kInst = "";
  String ultimo = "";
  String chiaveAttesa = "";
  boolean zoomPrincipale = false;

  /** Misure della pagina per Info: dimensioni, densita, zoom, bordi di sistema visti dalla pagina. */
  static final String JS_MISURE =
      "(function(){try{var d=document.createElement('div');"
      + "d.style.cssText='position:fixed;top:0;left:0;visibility:hidden;padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom) 0';"
      + "document.documentElement.appendChild(d);var cs=getComputedStyle(d),t=cs.paddingTop,b=cs.paddingBottom;d.remove();"
      + "var v=window.visualViewport;return innerWidth+'x'+innerHeight+' dpr '+devicePixelRatio.toFixed(2)"
      + "+' zoom '+(v?v.scale.toFixed(2):'1')+' bordi '+t+'/'+b;}catch(e){return 'n/d';}})()";
  String guida = "";
  boolean apiOk = false;
  FrameLayout root;
  FrameLayout contenuto;
  WebView web;
  ProgressBar barra;
  final ArrayList<FrameLayout> finestre = new ArrayList<>();
  WebViewClient client;
  WebChromeClient chrome;
  // Scelta file per <input type=file> delle app (foto, documenti).
  ValueCallback<Uri[]> sceltaFile;
  static final int SCELTA_FILE = 4711;

  // Copertura con la schermata di attesa: resta finche il banner non e nascosto.
  final Handler h = new Handler(Looper.getMainLooper());
  FrameLayout copertura;
  boolean coperto = false;
  boolean finito = false;
  boolean avviata = false;
  boolean pronto = false;
  long inizio = 0;
  long avvio = 0;
  long fine = 0;
  long bannerOk = 0;
  long richiesta = 0;
  int giro = 0;
  Bundle salvato;

  TextView titolo;
  View[] puntini;
  ValueAnimator onda;
  LinearGradient sfumatura;
  final Matrix mat = new Matrix();

  final Runnable controlla = new Runnable() {
    @Override
    public void run() {
      if (!coperto || web == null) return;
      long ora = SystemClock.uptimeMillis();
      if (!avviata) {
        if (ora - inizio > 20000) scopri();
        else h.postDelayed(this, 150);
        return;
      }
      final boolean scaduto = ora - avvio > 12000 || (finito && ora - fine > 1500);
      web.evaluateJavascript(jsBanner(scaduto), r -> {
        if (!coperto) return;
        String v = r == null ? "" : r.replace("\"", "");
        boolean segnale = v.endsWith("+");
        if (segnale) v = v.substring(0, v.length() - 1);
        long adesso = SystemClock.uptimeMillis();
        boolean ok = v.equals("ok") || v.equals("forzato");
        boolean assente = v.equals("no") && finito && adesso - fine > 600;
        if ((ok || assente || scaduto) && bannerOk == 0) bannerOk = adesso;
        // Si scopre quando l'INSTALLER segnala "pronto", oppure poco dopo la fine del caricamento
        // (le app installate non mandano il segnale). Riserva: 6 s dopo il banner.
        boolean via = pronto || segnale || (finito && adesso - fine > 1200) || adesso - bannerOk > 6000;
        if (bannerOk > 0 && via) scopriDopoDisegno();
        else h.postDelayed(MainActivity.this.controlla, 100);
      });
    }
  };

  void copri() {
    coperto = true;
    finito = false;
    avviata = false;
    pronto = false;
    bannerOk = 0;
    richiesta++;
    inizio = SystemClock.uptimeMillis();
    avvio = inizio;
    if (copertura != null) {
      copertura.animate().cancel();
      copertura.setAlpha(1f);
      copertura.setVisibility(View.VISIBLE);
    }
    avviaAttesa();
    h.removeCallbacks(controlla);
    h.postDelayed(controlla, 150);
  }

  void scopriDopoDisegno() {
    h.removeCallbacks(controlla);
    if (web == null) {
      scopri();
      return;
    }
    final long id = ++richiesta;
    web.postVisualStateCallback(id, new WebView.VisualStateCallback() {
      @Override
      public void onComplete(long rid) {
        if (rid == richiesta && coperto) scopri();
      }
    });
    h.postDelayed(() -> {
      if (id == richiesta && coperto) scopri();
    }, 1000);
  }

  void scopri() {
    coperto = false;
    h.removeCallbacks(controlla);
    final FrameLayout c = copertura;
    if (c == null || c.getVisibility() != View.VISIBLE) {
      fermaAttesa();
      return;
    }
    c.animate().cancel();
    c.animate().alpha(0f).setDuration(220).withEndAction(() -> {
      if (coperto || c != copertura) return;
      c.setVisibility(View.GONE);
      c.setAlpha(1f);
      fermaAttesa();
    }).start();
  }

  // ---------- Schermata di attesa ----------

  View costruisciAttesa() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setGravity(Gravity.CENTER_HORIZONTAL);
    l.setPadding(dp(16), 0, dp(16), 0);

    String nome = BuildConfig.NOME;
    float sp = TITOLO_SP;
    if (nome.length() > 14) sp = Math.max(22f, TITOLO_SP * 14f / nome.length());

    titolo = new TextView(this);
    titolo.setText(nome);
    titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    titolo.setTypeface(Typeface.DEFAULT_BOLD);
    titolo.setLetterSpacing(0.04f);
    titolo.setTextColor(Color.WHITE);
    titolo.setSingleLine(true);
    titolo.setGravity(Gravity.CENTER);
    sfumatura = null;
    titolo.addOnLayoutChangeListener((v, l1, t1, r1, b1, l0, t0, r0, b0) -> {
      int w = v.getWidth();
      if (w <= 0 || (sfumatura != null && r1 - l1 == r0 - l0)) return;
      sfumatura = new LinearGradient(0, 0, w, 0, COLORI, null, Shader.TileMode.MIRROR);
      titolo.getPaint().setShader(sfumatura);
      titolo.invalidate();
    });

    TextView sotto = new TextView(this);
    sotto.setText(FIRMA);
    sotto.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITOLO_SP * 0.40f);
    sotto.setTextColor(0xFF8C8C8C);
    sotto.setGravity(Gravity.CENTER);

    LinearLayout riga = new LinearLayout(this);
    riga.setOrientation(LinearLayout.HORIZONTAL);
    riga.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    riga.setPadding(0, dp(16), 0, 0);
    // Il salto dei pallini usa lo spazio sopra (padding): senza questo restano tagliati.
    riga.setClipToPadding(false);
    riga.setClipChildren(false);
    puntini = new View[COLORI.length];
    for (int i = 0; i < COLORI.length; i++) {
      View p = new View(this);
      GradientDrawable g = new GradientDrawable();
      g.setShape(GradientDrawable.OVAL);
      g.setColor(COLORI[i]);
      p.setBackground(g);
      p.setAlpha(0.55f);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(11), dp(11));
      lp.leftMargin = dp(6);
      lp.rightMargin = dp(6);
      riga.addView(p, lp);
      puntini[i] = p;
    }

    l.addView(titolo, new LinearLayout.LayoutParams(-2, -2));
    LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(-2, -2);
    lps.topMargin = dp(6);
    l.addView(sotto, lps);
    LinearLayout.LayoutParams lpr = new LinearLayout.LayoutParams(-2, -2);
    lpr.topMargin = dp(6);
    l.addView(riga, lpr);

    onda = ValueAnimator.ofFloat(0f, 1f);
    onda.setDuration(1300);
    onda.setRepeatCount(ValueAnimator.INFINITE);
    onda.setInterpolator(new LinearInterpolator());
    final float salto = dp(12);
    onda.addUpdateListener(a -> {
      float t = (float) a.getAnimatedValue();
      for (int i = 0; i < puntini.length; i++) {
        float p = t - i * 0.11f;
        if (p < 0) p += 1f;
        float s = p < 0.45f ? (float) Math.sin(Math.PI * p / 0.45f) : 0f;
        puntini[i].setTranslationY(-salto * s);
        puntini[i].setScaleX(1f + 0.25f * s);
        puntini[i].setScaleY(1f + 0.25f * s);
        puntini[i].setAlpha(0.55f + 0.45f * s);
      }
      if (sfumatura != null && titolo != null) {
        float g = (SystemClock.uptimeMillis() % 3000) / 3000f;
        mat.setTranslate(g * 2f * titolo.getWidth(), 0);
        sfumatura.setLocalMatrix(mat);
        titolo.invalidate();
      }
    });
    return l;
  }

  void avviaAttesa() {
    if (onda == null) return;
    if (onda.isPaused()) onda.resume();
    else if (!onda.isStarted()) onda.start();
  }

  void fermaAttesa() {
    if (onda != null) onda.cancel();
  }

  // ---------- Avvio ----------

  @Override
  protected void onCreate(Bundle stato) {
    super.onCreate(stato);
    preparaVoce();
    prefs = getSharedPreferences("cfg", MODE_PRIVATE);
    guida = leggiAsset("guida.js");
    root = new FrameLayout(this);
    root.setBackgroundColor(NERO);
    root.setOnApplyWindowInsetsListener(this::bordi);
    setContentView(root);
    if (Build.VERSION.SDK_INT >= 33) {
      getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
          OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::indietro);
    }
    salvato = stato;
    if (BuildConfig.UNICO) {
      chiaveAttesa = chiaveDa(getIntent());
      salvato = null;
    }
    costruisci(stato == null);
  }

  void costruisci(final boolean carica) {
    giro++;
    contenuto = new FrameLayout(this);
    root.addView(contenuto, new FrameLayout.LayoutParams(-1, -1));
    if (BuildConfig.UNICO) {
      casa = costruisciCasa();
      casa.setVisibility(View.GONE);
      root.addView(casa, new FrameLayout.LayoutParams(-1, -1));
    }

    barra = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    barra.setIndeterminate(true);
    barra.setVisibility(View.GONE);

    copertura = new FrameLayout(this);
    copertura.setBackgroundColor(NERO);
    copertura.setClickable(true);
    copertura.addView(costruisciAttesa(), new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

    root.addView(copertura, new FrameLayout.LayoutParams(-1, -1));
    root.addView(barra, new FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP));

    TextView g = new TextView(this);
    g.setText("⚙");
    g.setTextSize(22);
    g.setTextColor(Color.WHITE);
    g.setAlpha(0.35f);
    g.setPadding(dp(14), dp(10), dp(14), dp(10));
    g.setOnClickListener(v -> menu());
    root.addView(g, new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START));

    copri();
    final int mio = giro;
    // La WebView (pesante) si crea dopo il primo fotogramma con la schermata di attesa.
    root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      @Override
      public boolean onPreDraw() {
        root.getViewTreeObserver().removeOnPreDrawListener(this);
        h.post(() -> {
          if (mio != giro || isFinishing() || isDestroyed()) return;
          creaWeb(carica);
        });
        return true;
      }
    });
  }

  void creaWeb(boolean carica) {
    creaClient();
    web = nuovaWeb(true);
    contenuto.addView(web, 0, new FrameLayout.LayoutParams(-1, -1));
    Bundle st = salvato;
    salvato = null;
    if (!carica && st != null && web.restoreState(st) != null) {
      h.removeCallbacks(controlla);
      h.postDelayed(controlla, 150);
      return;
    }
    if (BuildConfig.UNICO) avvioUnico();
    else apri(inizio());
    controllaVersione();
  }

  /** Link di partenza: l'app installata se nota, altrimenti l'INSTALLER. */
  String inizio() {
    String a = prefs.getString("app", "");
    return a.isEmpty() ? BuildConfig.INSTALLER : a;
  }

  void apri(String u) {
    if (web == null) return;
    ultimo = u;
    if (casa != null) casa.setVisibility(View.GONE);
    finMenu = null;
    chiudiFinestre();
    copri();
    web.loadUrl(u);
  }

  // ---------- WebView ----------

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  WebView nuovaWeb(boolean principale) {
    WebView w = new WebView(this);
    w.setBackgroundColor(NERO);
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setSupportMultipleWindows(true);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    // Zoom con le dita solo nelle finestre "TORNA A" (pagine Google da computer). Nella pagina principale no:
    // con lo zoom attivo le app non stavano piu intere nello schermo (versione del 2/11 22:15).
    s.setSupportZoom(!principale);
    s.setBuiltInZoomControls(!principale);
    s.setDisplayZoomControls(false);
    CookieManager cm = CookieManager.getInstance();
    cm.setAcceptCookie(true);
    cm.setAcceptThirdPartyCookies(w, true);
    w.addJavascriptInterface(new Ponte(w), "DZ");
    w.setWebViewClient(client);
    w.setWebChromeClient(chrome);
    return w;
  }

  /**
   * Messaggi dalle pagine: dz-pronto, dz-app:URL (salva), dz-apri:URL (salva e apre),
   * dz-api-on (interruttore acceso: chiude la finestra e riprende l'installazione),
   * dz-rimossa (app disinstallata: dimentica il link), dz-zoom:F (ingrandisce la pagina).
   */
  class Ponte {
    final WebView w;

    Ponte(WebView w) {
      this.w = w;
    }

    @JavascriptInterface
    public void msg(String m) {
      if (m == null || m.length() > 400) return;
      final String s = m;
      runOnUiThread(() -> messaggio(s, w));
    }

    // Voce del telefono per le pagine (la WebView non ha speechSynthesis).
    // voce(): 1 pronta, 2 si sta preparando, 0 assente. voceStato(): 1 parla, 2 finito, 0 ferma.
    @JavascriptInterface
    public int voce() {
      return ttsPronto;
    }

    @JavascriptInterface
    public void parla(String t, float v) {
      parlaVoce(t, v);
    }

    @JavascriptInterface
    public void zitto() {
      zittoVoce();
    }

    @JavascriptInterface
    public int voceStato() {
      return ttsStato;
    }
  }

  // ---------- Voce (TextToSpeech) ----------

  TextToSpeech tts;
  volatile int ttsPronto = 2;
  volatile int ttsStato = 0;
  volatile String ttsId = "";

  void preparaVoce() {
    try {
      tts = new TextToSpeech(getApplicationContext(), st -> {
        if (st != TextToSpeech.SUCCESS || tts == null) {
          ttsPronto = 0;
          return;
        }
        int r = tts.setLanguage(Locale.ITALY);
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
          ttsPronto = 0;
          return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
          @Override
          public void onStart(String id) {
          }

          @Override
          public void onDone(String id) {
            if (id != null && id.equals(ttsId)) ttsStato = 2;
          }

          @Override
          @SuppressWarnings("deprecation")
          public void onError(String id) {
            if (id != null && id.equals(ttsId)) ttsStato = 2;
          }

          @Override
          public void onError(String id, int code) {
            if (id != null && id.equals(ttsId)) ttsStato = 2;
          }

          @Override
          public void onStop(String id, boolean interrotta) {
            if (id != null && id.equals(ttsId)) ttsStato = 2;
          }
        });
        ttsPronto = 1;
      });
    } catch (Exception e) {
      tts = null;
      ttsPronto = 0;
    }
  }

  void parlaVoce(String t, float v) {
    if (tts == null || ttsPronto != 1 || t == null || t.trim().isEmpty()) {
      ttsStato = 2;
      return;
    }
    if (t.length() > 3900) t = t.substring(0, 3900);
    String id = "dz" + SystemClock.uptimeMillis();
    ttsId = id;
    ttsStato = 1;
    try {
      tts.setSpeechRate(v > 0.3f && v < 3f ? v : 1f);
      tts.setPitch(1.05f);
      if (tts.speak(t, TextToSpeech.QUEUE_FLUSH, new Bundle(), id) != TextToSpeech.SUCCESS) ttsStato = 2;
    } catch (Exception e) {
      ttsStato = 2;
    }
  }

  void zittoVoce() {
    ttsStato = 2;
    try {
      if (tts != null) tts.stop();
    } catch (Exception e) {
      // niente
    }
  }

  void messaggio(String m, WebView w) {
    if (m.equals("dz-api-off")) {
      if (w != finMenu || !menuSpegni) return;
      menuSpegni = false;
      toast("Interruttore spento ✓");
      final WebView f = w;
      h.postDelayed(() -> {
        if (finMenu == f) chiudi(f);
      }, 1200);
      return;
    }
    if (m.startsWith("dz-zoom:")) {
      try {
        float f = Float.parseFloat(m.substring(8));
        if (f > 1f && f <= 3f) {
          if (w == web) {
            // Pagina "Rivedi autorizzazioni" nella pagina principale: zoom solo fino alla prossima pagina.
            w.getSettings().setSupportZoom(true);
            w.getSettings().setBuiltInZoomControls(true);
            zoomPrincipale = true;
          }
          w.zoomBy(f);
        }
      } catch (Exception ignored) {
      }
      return;
    }
    if (BuildConfig.UNICO && messaggioUnico(m, w)) return;
    if (m.equals("dz-api-on")) {
      if (apiOk || w == finMenu) return;
      apiOk = true;
      toast("Interruttore attivo ✓");
      h.postDelayed(() -> {
        apiOk = false;
        apri(BuildConfig.INSTALLER + "&riprendi=1");
      }, 1300);
      return;
    }
    if (m.equals("dz-rimossa")) {
      prefs.edit().remove("app").apply();
      return;
    }
    if (m.equals("dz-pronto") || m.equals("claudeexe-pronto")) {
      pronto = true;
      return;
    }
    if (m.startsWith("dz-app:")) {
      salvaApp(m.substring(7));
      return;
    }
    if (m.startsWith("dz-apri:")) {
      String u = m.substring(8);
      if (salvaApp(u)) apri(u);
    }
  }

  boolean salvaApp(String u) {
    if (!LINK_APP.matcher(u).matches()) return false;
    if (!u.equals(prefs.getString("app", ""))) prefs.edit().putString("app", u).apply();
    return true;
  }

  void creaClient() {
    client = new WebViewClient() {
      @Override
      public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        if (!r.isForMainFrame()) return false;
        Uri u = r.getUrl();
        if (dentro(u)) return false;
        apriFuori(u);
        if (v != web) chiudi(v);
        return true;
      }

      @Override
      public void onPageStarted(WebView v, String u, Bitmap f) {
        aiuto(v, u);
        if (v != web) return;
        segnaLogin(Uri.parse(u).getHost());
        if (zoomPrincipale) {
          zoomPrincipale = false;
          v.getSettings().setSupportZoom(false);
          v.getSettings().setBuiltInZoomControls(false);
        }
        if (barra != null) barra.setVisibility(View.VISIBLE);
        // Solo script.google.com ha il banner: lo copriamo finche non sparisce.
        // Le pagine di login Google restano visibili.
        if ("script.google.com".equals(Uri.parse(u).getHost())) {
          if (!coperto) copri();
          richiesta++;
          avviata = true;
          bannerOk = 0;
          finito = false;
          pronto = false;
          avvio = SystemClock.uptimeMillis();
          h.removeCallbacks(controlla);
          h.postDelayed(controlla, 150);
        } else {
          scopri();
        }
      }

      @Override
      public void onPageFinished(WebView v, String u) {
        CookieManager.getInstance().flush();
        v.evaluateJavascript("(function(){" + JS_ASCOLTA + "})()", null);
        aiuto(v, u);
        boolean gas = "script.google.com".equals(Uri.parse(u).getHost());
        if (!guida.isEmpty() && gas) {
          // Finestra "Spegni interruttore API" dal menu: la guida indica come spegnerlo.
          if (v == finMenu && menuSpegni) v.evaluateJavascript("window.__dzSpegni=1;", null);
          v.evaluateJavascript(guida, null);
        }
        if (v != web) return;
        if (barra != null) barra.setVisibility(View.GONE);
        finito = true;
        fine = SystemClock.uptimeMillis();
        if (verificaLogin && gas) {
          verificaLogin = false;
          h.postDelayed(MainActivity.this::verificaDopoLogin, 400);
        }
      }

      @Override
      public void doUpdateVisitedHistory(WebView v, String u, boolean ricarica) {
        aiuto(v, u);
        if (v == web) segnaLogin(Uri.parse(u).getHost());
      }

      @Override
      public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
        if (v == web && r.isForMainFrame()) scopri();
      }
    };

    // Finestre nuove (permessi Google, impostazioni, link "nuova scheda"): dentro l'app, sopra la pagina.
    chrome = new WebChromeClient() {
      @Override
      public boolean onCreateWindow(WebView v, boolean dialog, boolean user, Message msg) {
        WebView p = finestra(TESTO_AIUTO);
        ((WebView.WebViewTransport) msg.obj).setWebView(p);
        msg.sendToTarget();
        return true;
      }

      @Override
      public void onCloseWindow(WebView w) {
        chiudi(w);
      }

      @Override
      public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams fp) {
        if (sceltaFile != null) sceltaFile.onReceiveValue(null);
        sceltaFile = cb;
        try {
          Intent i = fp.createIntent();
          i.addCategory(Intent.CATEGORY_OPENABLE);
          if (fp.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
          startActivityForResult(Intent.createChooser(i, "Scegli"), SCELTA_FILE);
          return true;
        } catch (Exception e) {
          sceltaFile = null;
          Toast.makeText(MainActivity.this, "Nessuna app per scegliere i file", Toast.LENGTH_LONG).show();
          return false;
        }
      }
    };
  }

  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    super.onActivityResult(req, res, data);
    if (req != SCELTA_FILE || sceltaFile == null) return;
    Uri[] r = null;
    if (res == RESULT_OK && data != null) {
      ClipData cd = data.getClipData();
      if (cd != null && cd.getItemCount() > 0) {
        r = new Uri[cd.getItemCount()];
        for (int k = 0; k < r.length; k++) r[k] = cd.getItemAt(k).getUri();
      } else {
        r = WebChromeClient.FileChooserParams.parseResult(res, data);
      }
    }
    sceltaFile.onReceiveValue(r);
    sceltaFile = null;
  }

  /** Finestra sopra l'app con la barra "TORNA A NOME" e una tendina di aiuto (testo aperto = testo). */
  WebView finestra(final String testo) {
    WebView p = nuovaWeb(false);
    FrameLayout f = new FrameLayout(MainActivity.this);
    f.setBackgroundColor(NERO);
    f.setTag(p);
    LinearLayout col = new LinearLayout(MainActivity.this);
    col.setOrientation(LinearLayout.VERTICAL);
    TextView bx = new TextView(MainActivity.this);
    bx.setText("←  TORNA A " + BuildConfig.NOME.toUpperCase());
    bx.setTextColor(Color.WHITE);
    bx.setTextSize(17);
    bx.setTypeface(Typeface.DEFAULT_BOLD);
    bx.setSingleLine(true);
    bx.setGravity(Gravity.CENTER_VERTICAL);
    bx.setPadding(dp(18), 0, dp(18), 0);
    bx.setBackgroundColor(BuildConfig.COLORE);
    bx.setOnClickListener(x -> chiudi(p));
    col.addView(bx, new LinearLayout.LayoutParams(-1, dp(54)));
    final TextView aiuto = new TextView(MainActivity.this);
    aiuto.setText(testo);
    aiuto.setTextColor(0xFFF2F2F2);
    aiuto.setTextSize(15);
    aiuto.setLineSpacing(0, 1.15f);
    aiuto.setPadding(dp(16), dp(10), dp(16), dp(12));
    aiuto.setBackgroundColor(0xFF2A2340);
    aiuto.setVisibility(View.GONE);
    aiuto.setOnClickListener(x -> {
      boolean chiuso = AIUTO_CHIUSO.contentEquals(aiuto.getText());
      aiuto.setText(chiuso ? testo : AIUTO_CHIUSO);
    });
    col.addView(aiuto, new LinearLayout.LayoutParams(-1, -2));
    p.setTag(aiuto);
    col.addView(p, new LinearLayout.LayoutParams(-1, 0, 1));
    f.addView(col, new FrameLayout.LayoutParams(-1, -1));
    finestre.add(f);
    contenuto.addView(f, new FrameLayout.LayoutParams(-1, -1));
    return p;
  }

  /**
   * Mostra la tendina di aiuto sulle pagine dei permessi Google (accounts.google.*, percorso con "oauth")
   * e sulla pagina myaccount.google.com della finestra "Togli permessi Google".
   */
  void aiuto(WebView v, String u) {
    Object t = v.getTag();
    if (!(t instanceof View) || u == null) return;
    Uri x = Uri.parse(u);
    String host = x.getHost() == null ? "" : x.getHost().toLowerCase();
    String path = x.getPath() == null ? "" : x.getPath().toLowerCase();
    boolean si = host.startsWith("accounts.google.") && path.contains("oauth");
    if (v == finMenu && !menuSpegni) si = host.equals("myaccount.google.com");
    ((View) t).setVisibility(si ? View.VISIBLE : View.GONE);
  }

  /** Tiene traccia del passaggio dal login Google alla pagina dell'app (solo pagina principale). */
  void segnaLogin(String host) {
    if (host == null) return;
    host = host.toLowerCase();
    if (host.startsWith("accounts.google.")) {
      daLogin = true;
      verificaLogin = false;
    } else if (host.equals("script.google.com") && daLogin) {
      daLogin = false;
      verificaLogin = true;
    }
  }

  /**
   * Subito dopo il login Google a volte mostra la sua pagina di errore: i cookie non sono ancora pronti.
   * Se la pagina non e l'app, ricarica il link di partenza una volta (al massimo una ogni 30 s).
   * La schermata di attesa resta sopra: la pagina di errore non si vede.
   */
  void verificaDopoLogin() {
    if (web == null || !finestre.isEmpty()) return;
    String cu = web.getUrl();
    if (cu == null || !"script.google.com".equals(Uri.parse(cu).getHost())) return;
    web.evaluateJavascript(JS_PAGINA_APP, r -> {
      if (!"\"0\"".equals(r) || web == null) return;
      long ora = SystemClock.uptimeMillis();
      if (ultimaRicarica != 0 && ora - ultimaRicarica < 30000) return;
      ultimaRicarica = ora;
      apri(ultimo.isEmpty() ? inizio() : ultimo);
    });
  }

  /** Apre dal menu una pagina Google in una finestra sopra l'app. Alla chiusura l'app si ricarica. */
  void apriImpostazione(String url, boolean spegni) {
    if (web == null) return;
    if (casa != null) casa.setVisibility(View.GONE);
    finMenu = null;
    chiudiFinestre();
    menuSpegni = spegni;
    finMenu = finestra(spegni ? TESTO_AIUTO : TESTO_PERMESSI);
    finMenu.loadUrl(url);
  }

  String leggiAsset(String n) {
    try (java.io.InputStream in = getAssets().open(n)) {
      java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
      byte[] b = new byte[8192];
      int k;
      while ((k = in.read(b)) > 0) o.write(b, 0, k);
      return o.toString("UTF-8");
    } catch (Exception e) {
      return "";
    }
  }

  void chiudi(WebView w) {
    for (int i = finestre.size() - 1; i >= 0; i--) {
      FrameLayout f = finestre.get(i);
      if (f.getTag() == w) {
        finestre.remove(i);
        contenuto.removeView(f);
        w.stopLoading();
        h.post(w::destroy);
        // Finestra del menu chiusa: ricarica l'app, cosi legge subito interruttore e permessi nuovi.
        if (w == finMenu) {
          finMenu = null;
          menuSpegni = false;
          h.post(this::ricarica);
        }
        return;
      }
    }
  }

  void chiudiFinestre() {
    while (!finestre.isEmpty()) {
      Object t = finestre.get(finestre.size() - 1).getTag();
      if (t instanceof WebView) chiudi((WebView) t);
      else contenuto.removeView(finestre.remove(finestre.size() - 1));
    }
  }

  WebView cima() {
    if (finestre.isEmpty()) return web;
    Object t = finestre.get(finestre.size() - 1).getTag();
    return t instanceof WebView ? (WebView) t : web;
  }

  // ---------- Menu ----------

  void menu() {
    if (BuildConfig.UNICO) {
      menuUnico();
      return;
    }
    String[] voci = {"Ricarica", "Aggiorna l'app", "Disinstalla l'app",
        "Spegni interruttore API", "Togli permessi Google", "Esci da Google", "Info", "Chiudi l'app",
        "Nuova versione dell'APK"};
    new AlertDialog.Builder(this)
        .setTitle(BuildConfig.NOME)
        .setItems(voci, (d, i) -> {
          switch (i) {
            case 0:
              apri(inizio());
              break;
            case 1:
              apri(BuildConfig.INSTALLER);
              break;
            case 2:
              apri(BuildConfig.INSTALLER + "&azione=disinstalla");
              break;
            case 3:
              apriImpostazione(URL_INTERRUTTORE, true);
              break;
            case 4:
              apriImpostazione(URL_PERMESSI, false);
              break;
            case 5:
              esci();
              break;
            case 7:
              finishAndRemoveTask();
              break;
            case 8:
              aggiornaDZ(false);
              break;
            default:
              info();
          }
        })
        .show();
  }

  void esci() {
    WebStorage.getInstance().deleteAllData();
    CookieManager.getInstance().removeAllCookies(ok -> {
      CookieManager.getInstance().flush();
      runOnUiThread(() -> {
        if (web != null) {
          web.clearCache(true);
          if (BuildConfig.UNICO) mostraCasa();
          else apri(inizio());
        }
        toast("Uscito da Google");
      });
    });
  }

  void info() {
    if (web == null || (casa != null && casa.getVisibility() == View.VISIBLE)) {
      mostraInfo("");
      return;
    }
    web.evaluateJavascript(JS_MISURE, r -> mostraInfo(r == null ? "" : r.replace("\"", "")));
  }

  void mostraInfo(String misure) {
    String v = "?";
    try {
      v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception ignored) {
    }
    String testo;
    if (BuildConfig.UNICO) {
      testo = "Le tue app: " + leggiApps().length() + "."
          + "\n\nAggiungi un'app: apri il link della mail di Daniel, oppure menu, Aggiungi app con chiave."
          + "\n\nTieni premuta un'app nell'elenco per aggiornarla o disinstallarla."
          + "\n\nAggiorna " + BuildConfig.NOME + ": controlla se c'è una versione nuova di questa app e la installa (lo controllo anche da solo una volta al giorno)."
          + "\n\nIngranaggio in basso a sinistra = menu.";
    } else {
      boolean collegata = !prefs.getString("app", "").isEmpty();
      testo = (collegata
          ? "App installata e collegata: si apre direttamente."
          : "App non ancora installata: si apre l'installazione.")
          + "\n\nAggiorna l'app: riapre l'installazione per aggiornarla o reinstallarla."
          + "\n\nDisinstalla l'app: la cartella dell'app nel Drive va nel cestino (30 giorni per recuperarla)."
          + "\n\nIngranaggio in basso a sinistra = menu.";
    }
    testo += "\n\nSpegni interruttore API: apre l'impostazione «API Google Apps Script» del tuo account. Quando l'interruttore è spento torni indietro da solo."
        + "\n\nTogli permessi Google: apre l'elenco delle app con accesso al tuo account."
        + " Chiudendo la finestra l'app si ricarica.";
    if (!misure.isEmpty()) {
      int tz = web == null ? 100 : web.getSettings().getTextZoom();
      testo += "\n\nControllo pagina: " + misure + " testo " + tz + "%";
    }
    new AlertDialog.Builder(this)
        .setTitle(BuildConfig.NOME + " " + v)
        .setMessage(testo + "\n\n" + FIRMA)
        .setPositiveButton("OK", null)
        .show();
  }

  // ---------- Navigazione ----------

  /** Pagine che restano dentro l'app: app, contenuto GAS, login e impostazioni Google. */
  boolean dentro(Uri u) {
    String h = u.getHost();
    String sch = u.getScheme();
    if (h == null || sch == null) return false;
    if (!sch.equals("https") && !sch.equals("http")) return false;
    h = h.toLowerCase();
    if (h.equals("script.google.com") || h.endsWith(".googleusercontent.com")) return true;
    if (h.startsWith("accounts.google.") || h.equals("accounts.youtube.com")
        || h.equals("myaccount.google.com")) return true;
    String p = u.getPath();
    return h.equals("www.google.com") && p != null && p.startsWith("/accounts");
  }

  void apriFuori(Uri u) {
    String sch = u.getScheme();
    if ("https".equals(sch) || "http".equals(sch)) {
      apriChrome(u.toString());
      return;
    }
    try {
      startActivity(new Intent(Intent.ACTION_VIEW, u));
    } catch (Exception e) {
      toast("Nessuna app per aprire il link");
    }
  }

  /** Scheda Chrome integrata (protocollo Custom Tabs, senza librerie). */
  void apriChrome(String u) {
    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(u));
    Bundle extra = new Bundle();
    extra.putBinder("android.support.customtabs.extra.SESSION", null);
    i.putExtras(extra);
    i.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", NERO);
    i.putExtra("android.support.customtabs.extra.TITLE_VISIBILITY", 1);
    if (haChrome()) i.setPackage("com.android.chrome");
    try {
      startActivity(i);
    } catch (Exception e) {
      i.setPackage(null);
      try {
        startActivity(i);
      } catch (Exception e2) {
        toast("Nessun browser disponibile");
      }
    }
  }

  boolean haChrome() {
    try {
      getPackageManager().getPackageInfo("com.android.chrome", 0);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  void indietro() {
    if (BuildConfig.UNICO) {
      if (casa != null && casa.getVisibility() == View.VISIBLE) {
        finish();
        return;
      }
      WebView c = cima();
      if (c != null && c != web) {
        if (c.canGoBack()) c.goBack();
        else chiudi(c);
        return;
      }
      mostraCasa();
      return;
    }
    WebView c = cima();
    if (c == null) {
      finish();
      return;
    }
    if (c.canGoBack()) c.goBack();
    else if (c != web) chiudi(c);
    else finish();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onBackPressed() {
    indietro();
  }

  // ---------- DZ App (APK unico) ----------

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);
    if (i != null && AZIONE_INSTALLA.equals(i.getAction())) {
      esitoInstalla(i);
      return;
    }
    setIntent(i);
    if (!BuildConfig.UNICO) return;
    String k = chiaveDa(i);
    if (k.isEmpty()) return;
    if (web == null) {
      chiaveAttesa = k;
      return;
    }
    apriChiave(k);
  }

  // ---------- Aggiornamento di questa APK (come il Viewer 1.10) ----------
  // Versione e APK dalla release pubblica di questa app (tag = APP_ID, note "NOME - versione 1.N - ...").
  static final String AZIONE_INSTALLA = "com.dz.install.INSTALLA";
  static final String URL_REL = "https://api.github.com/repos/milkbarandcoffe-web/app-install/releases/tags/";
  volatile boolean aggiornando = false;

  String versioneMia() {
    try {
      return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception e) {
      return "0";
    }
  }

  /** {versione, link dell'APK} oppure null se non leggibile. */
  String[] releaseOnline() {
    try {
      HttpURLConnection c = (HttpURLConnection) new URL(URL_REL + Uri.encode(BuildConfig.APP_ID)).openConnection();
      c.setConnectTimeout(15000);
      c.setReadTimeout(15000);
      c.setRequestProperty("Accept", "application/vnd.github+json");
      c.setRequestProperty("User-Agent", "DZApp/" + versioneMia());
      if (c.getResponseCode() != 200) return null;
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      try (InputStream in = c.getInputStream()) {
        byte[] x = new byte[8192];
        int n;
        while ((n = in.read(x)) > 0) b.write(x, 0, n);
      }
      JSONObject j = new JSONObject(b.toString("UTF-8"));
      Matcher m = Pattern.compile("versione (\\d+(?:\\.\\d+)+)").matcher(j.optString("body", ""));
      String apk = "";
      JSONArray a = j.optJSONArray("assets");
      for (int i = 0; a != null && i < a.length(); i++) {
        JSONObject o = a.optJSONObject(i);
        if (o != null && o.optString("name", "").toLowerCase().endsWith(".apk")) {
          apk = o.optString("browser_download_url", "");
          break;
        }
      }
      if (!m.find() || !apk.startsWith("https://")) return null;
      return new String[] {m.group(1), apk};
    } catch (Exception e) {
      return null;
    }
  }

  static boolean piuNuova(String a, String b) {
    String[] x = a.split("\\."), y = b.split("\\.");
    for (int i = 0; i < Math.max(x.length, y.length); i++) {
      int p = i < x.length ? numero(x[i]) : 0, q = i < y.length ? numero(y[i]) : 0;
      if (p != q) return p > q;
    }
    return false;
  }

  static int numero(String s) {
    try {
      return Integer.parseInt(s.replaceAll("\\D", ""));
    } catch (Exception e) {
      return 0;
    }
  }

  /** All'avvio, al massimo una volta ogni 20 ore: se c'e una versione nuova lo dice. */
  void controllaVersione() {
    long ora = System.currentTimeMillis(), ult = prefs.getLong("ctrlVersione", 0);
    if (ora - ult < 20L * 3600 * 1000) return;
    prefs.edit().putLong("ctrlVersione", ora).apply();
    h.postDelayed(() -> aggiornaDZ(true), 6000);
  }

  /** silenzioso = controllo automatico: parla solo se c'e una versione nuova. */
  void aggiornaDZ(boolean silenzioso) {
    if (aggiornando) {
      if (!silenzioso) toast("Aggiornamento gia in corso");
      return;
    }
    if (!silenzioso) toast("Controllo la versione...");
    new Thread(() -> {
      String[] r = releaseOnline();
      String mia = versioneMia();
      if (r == null) {
        if (!silenzioso) ui("Non riesco a controllare la versione: riprova piu tardi");
        return;
      }
      if (!piuNuova(r[0], mia)) {
        if (!silenzioso) ui("Hai gia l'ultima versione (" + mia + ")");
        return;
      }
      runOnUiThread(() -> {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
            .setTitle(BuildConfig.NOME + " " + r[0])
            .setMessage("C'e una versione nuova di " + BuildConfig.NOME + ": " + r[0] + " (tu hai la " + mia + ").\n\n"
                + "Le tue app e le chiavi restano.")
            .setPositiveButton("AGGIORNA", (d, w) -> scaricaDZ(r))
            .setNegativeButton("DOPO", null)
            .show();
      });
    }).start();
  }

  void ui(String m) {
    runOnUiThread(() -> toast(m));
  }

  void scaricaDZ(String[] r) {
    if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
      new AlertDialog.Builder(this)
          .setTitle("Aggiorna " + BuildConfig.NOME)
          .setMessage("Android chiede il permesso di installare gli aggiornamenti da questa app.\n\n"
              + "1. Premi APRI IMPOSTAZIONI.\n2. Attiva \"Consenti da questa fonte\".\n"
              + "3. Torna qui: menu, Aggiorna " + BuildConfig.NOME + ".")
          .setPositiveButton("APRI IMPOSTAZIONI", (d, w) -> {
            try {
              startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                  Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
              toast("Apri Impostazioni, App, " + BuildConfig.NOME + ", Installa app sconosciute");
            }
          })
          .setNegativeButton("ANNULLA", null)
          .show();
      return;
    }
    aggiornando = true;
    toast("Scarico la versione " + r[0] + "...");
    new Thread(() -> {
      try {
        installaApk(r[1]);
      } catch (Exception e) {
        ui("Aggiornamento non riuscito: " + e.getMessage());
      } finally {
        aggiornando = false;
      }
    }).start();
  }

  /** Scarica l'APK dentro una sessione di PackageInstaller: Android chiede la conferma. */
  void installaApk(String url) throws Exception {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setInstanceFollowRedirects(true);
    c.setConnectTimeout(15000);
    c.setReadTimeout(60000);
    c.setRequestProperty("User-Agent", "DZApp/" + versioneMia());
    int code = c.getResponseCode();
    if (code != 200) throw new Exception("download HTTP " + code);
    long tot = c.getContentLengthLong();
    PackageInstaller pi = getPackageManager().getPackageInstaller();
    PackageInstaller.SessionParams sp =
        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
    sp.setAppPackageName(getPackageName());
    int id = pi.createSession(sp);
    PackageInstaller.Session s = pi.openSession(id);
    try {
      try (InputStream in = c.getInputStream(); OutputStream out = s.openWrite("app.apk", 0, tot > 0 ? tot : -1)) {
        byte[] x = new byte[65536];
        int n;
        while ((n = in.read(x)) > 0) out.write(x, 0, n);
        s.fsync(out);
      }
      Intent i = new Intent(this, MainActivity.class).setAction(AZIONE_INSTALLA);
      int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
      s.commit(PendingIntent.getActivity(this, id, i, fl).getIntentSender());
    } catch (Exception e) {
      s.abandon();
      throw e;
    } finally {
      s.close();
    }
  }

  @SuppressWarnings("deprecation")
  void esitoInstalla(Intent it) {
    int st = it.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
    if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
      Intent conferma = Build.VERSION.SDK_INT >= 33
          ? it.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
          : (Intent) it.getParcelableExtra(Intent.EXTRA_INTENT);
      if (conferma == null) {
        toast("Aggiornamento non riuscito: conferma mancante");
        return;
      }
      try {
        startActivity(conferma);
      } catch (Exception e) {
        toast("Aggiornamento non riuscito: " + e.getMessage());
      }
    } else if (st == PackageInstaller.STATUS_SUCCESS) {
      toast("Aggiornato");
    } else if (st == PackageInstaller.STATUS_FAILURE_ABORTED) {
      toast("Aggiornamento annullato");
    } else {
      String m = it.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
      toast("Aggiornamento non riuscito" + (m != null ? ": " + m : ""));
    }
  }

  /** Chiave nel formato ABCD-EFGH-JKLM, oppure "" se non valida. */
  static String normChiave(String s) {
    String k = s == null ? "" : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    if (k.length() != 12) return "";
    return k.substring(0, 4) + "-" + k.substring(4, 8) + "-" + k.substring(8);
  }

  /** Chiave dal link dzapp://chiave?k=... (pagina della mail). */
  String chiaveDa(Intent i) {
    if (i == null || i.getData() == null) return "";
    Uri u = i.getData();
    if (!"dzapp".equals(u.getScheme())) return "";
    return normChiave(u.getQueryParameter("k"));
  }

  String urlInstaller(String k) {
    return BuildConfig.INSTALLER + "?k=" + Uri.encode(k) + "&s=" + Uri.encode(BuildConfig.SERVIZIO) + "&apk=1";
  }

  void avvioUnico() {
    String k = chiaveAttesa;
    chiaveAttesa = "";
    if (!k.isEmpty()) apriChiave(k);
    else mostraCasa();
  }

  void apriChiave(String k) {
    kInst = k;
    corrente = "";
    titoloAttesa(BuildConfig.NOME);
    apri(urlInstaller(k));
  }

  void apriApp(JSONObject a) {
    corrente = a.optString("id", "");
    kInst = a.optString("k", "");
    titoloAttesa(a.optString("nome", BuildConfig.NOME));
    apri(a.optString("url", ""));
  }

  void aggiornaApp(JSONObject a) {
    String k = a.optString("k", "");
    if (k.isEmpty()) {
      chiediChiave();
      return;
    }
    apriChiave(k);
  }

  void disinstallaApp(JSONObject a) {
    String k = a.optString("k", "");
    if (k.isEmpty()) {
      chiediChiave();
      return;
    }
    kInst = k;
    corrente = "";
    titoloAttesa(a.optString("nome", BuildConfig.NOME));
    apri(urlInstaller(k) + "&azione=disinstalla");
  }

  void ricarica() {
    if (!BuildConfig.UNICO) {
      apri(inizio());
      return;
    }
    if (casa != null && casa.getVisibility() == View.VISIBLE) return;
    if (!ultimo.isEmpty()) apri(ultimo);
    else mostraCasa();
  }

  /** Testo della schermata di attesa: nome dell'app che si apre. */
  void titoloAttesa(String n) {
    if (titolo == null || n == null || n.isEmpty()) return;
    float sp = TITOLO_SP;
    if (n.length() > 14) sp = Math.max(22f, TITOLO_SP * 14f / n.length());
    titolo.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    titolo.setText(n);
    sfumatura = null;
    titolo.requestLayout();
  }

  JSONArray leggiApps() {
    try {
      return new JSONArray(prefs.getString("apps", "[]"));
    } catch (Exception e) {
      return new JSONArray();
    }
  }

  void salvaApps(JSONArray a) {
    prefs.edit().putString("apps", a.toString()).apply();
  }

  JSONObject trovaApp(String id) {
    JSONArray a = leggiApps();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      if (o != null && id.equals(o.optString("id"))) return o;
    }
    return null;
  }

  /** Aggiunge o aggiorna un'app dell'elenco (messaggi dz-app2 / dz-apri2 dell'INSTALLER). */
  JSONObject registraApp(String json) {
    try {
      JSONObject x = new JSONObject(json);
      String id = x.optString("id", ""), nome = x.optString("nome", ""), url = x.optString("url", "");
      String col = x.optString("colore", "#534AB7"), k = normChiave(x.optString("k", ""));
      if (!id.matches("[a-z0-9-]{1,60}") || nome.isEmpty() || nome.length() > 40) return null;
      if (!LINK_APP.matcher(url).matches()) return null;
      if (!col.matches("#[0-9a-fA-F]{6}")) col = "#534AB7";
      if (k.isEmpty()) k = kInst;
      JSONObject nuovo = new JSONObject();
      nuovo.put("id", id).put("nome", nome).put("url", url).put("colore", col).put("k", k);
      JSONArray a = leggiApps(), b = new JSONArray();
      boolean messo = false;
      for (int i = 0; i < a.length(); i++) {
        JSONObject o = a.optJSONObject(i);
        if (o == null) continue;
        if (id.equals(o.optString("id"))) {
          if (!messo) b.put(nuovo);
          messo = true;
        } else {
          b.put(o);
        }
      }
      if (!messo) b.put(nuovo);
      salvaApps(b);
      aggiornaCasa();
      return nuovo;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Licenza terminata con rimozione approvata (messaggio dell'app bloccata): apre la rimozione nell'INSTALLER,
   * che toglie l'app dal Drive dell'amico (i suoi dati restano) e poi manda dz-rimossa2 per toglierla dall'elenco.
   */
  String rimozioneK = "";
  long rimozioneT = 0;

  void rimuoviApp(String k) {
    if (k.isEmpty()) return;
    long ora = SystemClock.elapsedRealtime();
    if (k.equals(rimozioneK) && ora - rimozioneT < 60000) return;
    rimozioneK = k;
    rimozioneT = ora;
    String nome = BuildConfig.NOME;
    JSONArray a = leggiApps();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      if (o != null && k.equals(o.optString("k"))) nome = o.optString("nome", nome);
    }
    kInst = k;
    corrente = "";
    titoloAttesa(nome);
    apri(urlInstaller(k) + "&azione=rimuovi");
  }

  void togliApp(String id) {
    JSONArray a = leggiApps(), b = new JSONArray();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      if (o != null && !id.equals(o.optString("id"))) b.put(o);
    }
    salvaApps(b);
    if (id.equals(corrente)) corrente = "";
    aggiornaCasa();
  }

  /** Messaggi dell'INSTALLER e delle app per DZ App. I messaggi per l'APK "NOME install" sono ignorati. */
  boolean messaggioUnico(String m, WebView w) {
    if (m.startsWith("dz-app2:")) {
      registraApp(m.substring(8));
      return true;
    }
    if (m.startsWith("dz-apri2:")) {
      JSONObject a = registraApp(m.substring(9));
      if (a != null) apriApp(a);
      return true;
    }
    if (m.startsWith("dz-rimossa2:")) {
      togliApp(m.substring(12));
      return true;
    }
    if (m.equals("dz-licenza")) {
      chiediChiave();
      return true;
    }
    if (m.startsWith("dz-rimuovi:")) {
      rimuoviApp(normChiave(m.substring(11)));
      return true;
    }
    if (m.equals("dz-api-on")) {
      if (apiOk || w == finMenu || kInst.isEmpty()) return true;
      apiOk = true;
      toast("Interruttore attivo ✓");
      final String k = kInst;
      h.postDelayed(() -> {
        apiOk = false;
        apri(urlInstaller(k) + "&riprendi=1");
      }, 1300);
      return true;
    }
    return m.startsWith("dz-app:") || m.startsWith("dz-apri:") || m.equals("dz-rimossa");
  }

  void chiediChiave() {
    final EditText e = new EditText(this);
    e.setHint("ABCD-EFGH-JKLM");
    e.setSingleLine(true);
    e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    FrameLayout f = new FrameLayout(this);
    f.setPadding(dp(22), dp(6), dp(22), 0);
    f.addView(e);
    final AlertDialog d = new AlertDialog.Builder(this)
        .setTitle("Aggiungi app")
        .setMessage("Scrivi la chiave che hai ricevuto da Daniel (12 lettere e numeri).")
        .setView(f)
        .setPositiveButton("CONTINUA", null)
        .setNegativeButton("Annulla", null)
        .create();
    d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
      String k = normChiave(e.getText().toString());
      if (k.isEmpty()) {
        e.setError("La chiave ha 12 lettere e numeri, per esempio ABCD-EFGH-JKLM");
        return;
      }
      d.dismiss();
      apriChiave(k);
    }));
    d.show();
  }

  void menuUnico() {
    final ArrayList<String> voci = new ArrayList<>();
    final ArrayList<Runnable> azioni = new ArrayList<>();
    boolean inCasa = casa != null && casa.getVisibility() == View.VISIBLE;
    if (!inCasa) {
      voci.add("Chiudi questa app");
      azioni.add(this::mostraCasa);
    }
    voci.add("Aggiungi app con chiave");
    azioni.add(this::chiediChiave);
    if (!inCasa) {
      voci.add("Ricarica");
      azioni.add(this::ricarica);
    }
    final JSONObject cur = corrente.isEmpty() ? null : trovaApp(corrente);
    if (cur != null && !inCasa) {
      voci.add("Aggiorna questa app");
      azioni.add(() -> aggiornaApp(cur));
      voci.add("Disinstalla questa app");
      azioni.add(() -> disinstallaApp(cur));
    }
    voci.add("Spegni interruttore API");
    azioni.add(() -> apriImpostazione(URL_INTERRUTTORE, true));
    voci.add("Togli permessi Google");
    azioni.add(() -> apriImpostazione(URL_PERMESSI, false));
    voci.add("Esci da Google");
    azioni.add(this::esci);
    voci.add("Aggiorna " + BuildConfig.NOME);
    azioni.add(() -> aggiornaDZ(false));
    voci.add("Info");
    azioni.add(this::info);
    voci.add("Esci da " + BuildConfig.NOME);
    azioni.add(this::finishAndRemoveTask);
    new AlertDialog.Builder(this)
        .setTitle(BuildConfig.NOME)
        .setItems(voci.toArray(new String[0]), (d, i) -> azioni.get(i).run())
        .show();
  }

  void menuApp(final JSONObject o) {
    String[] voci = {"Apri", "Aggiorna l'app", "Disinstalla l'app", "Togli dall'elenco"};
    new AlertDialog.Builder(this)
        .setTitle(o.optString("nome"))
        .setItems(voci, (d, i) -> {
          if (i == 0) apriApp(o);
          else if (i == 1) aggiornaApp(o);
          else if (i == 2) disinstallaApp(o);
          else togliApp(o.optString("id"));
        })
        .show();
  }

  /** Schermata con l'elenco delle app (sopra la pagina, sotto la schermata di attesa). */
  void mostraCasa() {
    corrente = "";
    ultimo = "";
    if (casa == null) return;
    finMenu = null;
    chiudiFinestre();
    aggiornaCasa();
    casa.setVisibility(View.VISIBLE);
    scopri();
    if (web != null) {
      web.stopLoading();
      web.loadUrl("about:blank");
    }
  }

  View costruisciCasa() {
    ScrollView sv = new ScrollView(this);
    sv.setBackgroundColor(NERO);
    sv.setFillViewport(true);
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(dp(16), dp(36), dp(16), dp(90));
    final TextView t = new TextView(this);
    t.setText(BuildConfig.NOME);
    t.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITOLO_SP);
    t.setTypeface(Typeface.DEFAULT_BOLD);
    t.setLetterSpacing(0.04f);
    t.setGravity(Gravity.CENTER);
    t.setTextColor(Color.WHITE);
    t.addOnLayoutChangeListener((v, l1, t1, r1, b1, l0, t0, r0, b0) -> {
      int w = v.getWidth();
      if (w <= 0 || r1 - l1 == r0 - l0) return;
      t.getPaint().setShader(new LinearGradient(w * 0.2f, 0, w * 0.8f, 0, COLORI, null, Shader.TileMode.MIRROR));
      t.invalidate();
    });
    TextView s = new TextView(this);
    s.setText(FIRMA);
    s.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITOLO_SP * 0.40f);
    s.setTextColor(0xFF8C8C8C);
    s.setGravity(Gravity.CENTER);
    l.addView(t, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout.LayoutParams ls = new LinearLayout.LayoutParams(-1, -2);
    ls.topMargin = dp(6);
    l.addView(s, ls);
    elenco = new LinearLayout(this);
    elenco.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams le = new LinearLayout.LayoutParams(-1, -2);
    le.topMargin = dp(26);
    l.addView(elenco, le);
    TextView piu = new TextView(this);
    piu.setText("+  AGGIUNGI APP CON CHIAVE");
    piu.setTextSize(15.5f);
    piu.setTypeface(Typeface.DEFAULT_BOLD);
    piu.setTextColor(Color.WHITE);
    piu.setGravity(Gravity.CENTER);
    piu.setPadding(dp(14), dp(15), dp(14), dp(15));
    piu.setBackground(tondo(BuildConfig.COLORE, 0, 13));
    piu.setOnClickListener(v -> chiediChiave());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(14);
    l.addView(piu, lp);
    sv.addView(l, new FrameLayout.LayoutParams(-1, -2));
    return sv;
  }

  GradientDrawable tondo(int colore, int bordo, int raggio) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(colore);
    g.setCornerRadius(dp(raggio));
    if (bordo != 0) g.setStroke(dp(1), bordo);
    return g;
  }

  static String sigla(String n) {
    String[] w = n.trim().split("[\\s_\\-]+");
    String a = w.length > 0 && !w[0].isEmpty() ? w[0] : "?";
    String b = w.length > 1 ? w[w.length - 1] : (a.length() > 1 ? a.substring(1) : "");
    return (a.substring(0, 1) + (b.isEmpty() ? "" : b.substring(0, 1))).toUpperCase();
  }

  void aggiornaCasa() {
    if (elenco == null) return;
    elenco.removeAllViews();
    JSONArray a = leggiApps();
    if (a.length() == 0) {
      TextView v = new TextView(this);
      v.setText("Nessuna app ancora.\n\nApri il link della mail che ti ha mandato Daniel, oppure tocca AGGIUNGI APP CON CHIAVE e scrivi la chiave.");
      v.setTextColor(0xFFAAAAAA);
      v.setTextSize(15);
      v.setLineSpacing(0, 1.15f);
      v.setPadding(dp(16), dp(16), dp(16), dp(16));
      v.setBackground(tondo(0xE61A1A1F, 0xFF2A2A30, 16));
      elenco.addView(v, new LinearLayout.LayoutParams(-1, -2));
      return;
    }
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.optJSONObject(i);
      if (o != null) elenco.addView(riga(o));
    }
  }

  View riga(final JSONObject o) {
    LinearLayout r = new LinearLayout(this);
    r.setOrientation(LinearLayout.HORIZONTAL);
    r.setGravity(Gravity.CENTER_VERTICAL);
    r.setPadding(dp(14), dp(12), dp(14), dp(12));
    r.setBackground(tondo(0xE61A1A1F, 0xFF2A2A30, 16));
    int col;
    try {
      col = Color.parseColor(o.optString("colore", "#534AB7"));
    } catch (Exception e) {
      col = BuildConfig.COLORE;
    }
    TextView ic = new TextView(this);
    ic.setText(sigla(o.optString("nome", "?")));
    ic.setTextColor(Color.WHITE);
    ic.setTextSize(20);
    ic.setTypeface(Typeface.DEFAULT_BOLD);
    ic.setGravity(Gravity.CENTER);
    ic.setBackground(tondo(col, 0, 17));
    r.addView(ic, new LinearLayout.LayoutParams(dp(56), dp(56)));
    LinearLayout tx = new LinearLayout(this);
    tx.setOrientation(LinearLayout.VERTICAL);
    tx.setPadding(dp(14), 0, 0, 0);
    TextView n = new TextView(this);
    n.setText(o.optString("nome"));
    n.setTextColor(Color.WHITE);
    n.setTextSize(18);
    n.setTypeface(Typeface.DEFAULT_BOLD);
    TextView s = new TextView(this);
    s.setText("Tocca per aprire · tieni premuto per altro");
    s.setTextColor(0xFF999999);
    s.setTextSize(12.5f);
    tx.addView(n);
    tx.addView(s);
    r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
    r.setOnClickListener(v -> apriApp(o));
    r.setOnLongClickListener(v -> {
      menuApp(o);
      return true;
    });
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(10);
    r.setLayoutParams(lp);
    return r;
  }


  // ---------- Bordi schermo (edge-to-edge) ----------

  @SuppressWarnings("deprecation")
  WindowInsets bordi(View v, WindowInsets ins) {
    int l, t, r, b;
    if (Build.VERSION.SDK_INT >= 30) {
      Insets x = ins.getInsets(WindowInsets.Type.systemBars()
          | WindowInsets.Type.ime() | WindowInsets.Type.displayCutout());
      l = x.left;
      t = x.top;
      r = x.right;
      b = x.bottom;
    } else {
      l = ins.getSystemWindowInsetLeft();
      t = ins.getSystemWindowInsetTop();
      r = ins.getSystemWindowInsetRight();
      b = ins.getSystemWindowInsetBottom();
    }
    v.setPadding(l, t, r, b);
    // I bordi sono gia fuori grazie al padding: la pagina non li deve ricevere e aggiungere di nuovo (viewport-fit=cover).
    return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : ins.consumeSystemWindowInsets();
  }

  // ---------- Ciclo di vita ----------

  @Override
  protected void onResume() {
    super.onResume();
    if (web != null) web.onResume();
    if (coperto) avviaAttesa();
  }

  @Override
  protected void onPause() {
    zittoVoce();
    if (onda != null && onda.isRunning()) onda.pause();
    if (web != null) web.onPause();
    CookieManager.getInstance().flush();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    if (web != null) web.saveState(out);
  }

  @Override
  protected void onDestroy() {
    h.removeCallbacksAndMessages(null);
    try {
      if (tts != null) tts.shutdown();
    } catch (Exception e) {
      // niente
    }
    tts = null;
    fermaAttesa();
    finMenu = null;
    chiudiFinestre();
    if (web != null) {
      ViewGroup p = (ViewGroup) web.getParent();
      if (p != null) p.removeView(web);
      web.destroy();
      web = null;
    }
    super.onDestroy();
  }

  // ---------- Utilita ----------

  int dp(int x) {
    return (int) (x * getResources().getDisplayMetrics().density + 0.5f);
  }

  void toast(String m) {
    Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
  }
}
