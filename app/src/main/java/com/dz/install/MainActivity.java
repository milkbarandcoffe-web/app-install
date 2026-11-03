package com.dz.install;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * APK "NOME install" - idea e sviluppo by Daniel Zavan.
 * Primo avvio: apre l'INSTALLER dell'app (login, permessi e installazione dentro l'app).
 * L'INSTALLER manda il link dell'app installata (messaggi dz-app / dz-apri):
 * l'APK lo salva e dagli avvii successivi apre direttamente l'app.
 * Ingranaggio in basso a sinistra = menu.
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
  String guida = "";
  boolean apiOk = false;
  FrameLayout root;
  FrameLayout contenuto;
  WebView web;
  ProgressBar barra;
  final ArrayList<FrameLayout> finestre = new ArrayList<>();
  WebViewClient client;
  WebChromeClient chrome;

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
    lpr.topMargin = dp(22);
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
    costruisci(stato == null);
  }

  void costruisci(final boolean carica) {
    giro++;
    contenuto = new FrameLayout(this);
    root.addView(contenuto, new FrameLayout.LayoutParams(-1, -1));

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
    web = nuovaWeb();
    contenuto.addView(web, 0, new FrameLayout.LayoutParams(-1, -1));
    Bundle st = salvato;
    salvato = null;
    if (!carica && st != null && web.restoreState(st) != null) {
      h.removeCallbacks(controlla);
      h.postDelayed(controlla, 150);
      return;
    }
    apri(inizio());
  }

  /** Link di partenza: l'app installata se nota, altrimenti l'INSTALLER. */
  String inizio() {
    String a = prefs.getString("app", "");
    return a.isEmpty() ? BuildConfig.INSTALLER : a;
  }

  void apri(String u) {
    if (web == null) return;
    finMenu = null;
    chiudiFinestre();
    copri();
    web.loadUrl(u);
  }

  // ---------- WebView ----------

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  WebView nuovaWeb() {
    WebView w = new WebView(this);
    w.setBackgroundColor(NERO);
    WebSettings s = w.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setSupportMultipleWindows(true);
    s.setJavaScriptCanOpenWindowsAutomatically(true);
    s.setLoadWithOverviewMode(true);
    s.setUseWideViewPort(true);
    s.setSupportZoom(true);
    s.setBuiltInZoomControls(true);
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
  }

  void messaggio(String m, WebView w) {
    if (m.startsWith("dz-zoom:")) {
      try {
        float f = Float.parseFloat(m.substring(8));
        if (f > 1f && f <= 3f) w.zoomBy(f);
      } catch (Exception ignored) {
      }
      return;
    }
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
    };
  }

  /** Finestra sopra l'app con la barra "TORNA A NOME" e una tendina di aiuto (testo aperto = testo). */
  WebView finestra(final String testo) {
    WebView p = nuovaWeb();
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
      apri(inizio());
    });
  }

  /** Apre dal menu una pagina Google in una finestra sopra l'app. Alla chiusura l'app si ricarica. */
  void apriImpostazione(String url, boolean spegni) {
    if (web == null) return;
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
          h.post(() -> apri(inizio()));
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
    String[] voci = {"Ricarica", "Aggiorna l'app", "Disinstalla l'app",
        "Spegni interruttore API", "Togli permessi Google", "Esci da Google", "Info"};
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
          apri(inizio());
        }
        toast("Uscito da Google");
      });
    });
  }

  void info() {
    String v = "?";
    try {
      v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception ignored) {
    }
    boolean collegata = !prefs.getString("app", "").isEmpty();
    new AlertDialog.Builder(this)
        .setTitle(BuildConfig.NOME + " " + v)
        .setMessage((collegata
            ? "App installata e collegata: si apre direttamente."
            : "App non ancora installata: si apre l'installazione.")
            + "\n\nAggiorna l'app: riapre l'installazione per aggiornarla o reinstallarla."
            + "\n\nDisinstalla l'app: la cartella dell'app nel Drive va nel cestino (30 giorni per recuperarla)."
            + "\n\nSpegni interruttore API: apre l'impostazione «API Google Apps Script» del tuo account."
            + "\n\nTogli permessi Google: apre l'elenco delle app con accesso al tuo account."
            + " Chiudendo la finestra l'app si ricarica."
            + "\n\nIngranaggio in basso a sinistra = menu."
            + "\n\n" + FIRMA)
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
    return ins;
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
