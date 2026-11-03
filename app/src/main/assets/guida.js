// Guida "tocca qui" sulle pagine di script.google.com (non sulle pagine di login/permessi di Google).
// - Impostazioni (/home/usersettings): evidenzia "API Google Apps Script", poi l'interruttore.
//   Quando l'interruttore e acceso avvisa l'APK (dz-api-on): la finestra si chiude e l'installazione riparte.
// - Finestra "Review permissions" delle app: pagina a misura di telefono, spiegazione in italiano, pulsante evidenziato.
// La cornice non intercetta i tocchi (pointer-events:none): si tocca sempre il pulsante vero di Google.
// idea e sviluppo by Daniel Zavan
(function () {
  if (location.hostname !== 'script.google.com') return;
  var W = window;
  if (W.__dzGuida) { try { W.__dzGuida(); } catch (x) {} return; }

  var anello = null, etichetta = null, scheda = null, bersaglio = null, fatto = false, battito = 0, zoomFatto = false;

  function testo(e) { return (e.innerText || e.textContent || '').replace(/\s+/g, ' ').trim(); }
  function visibile(e) { var r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0; }
  function nostro(e) { return e.closest && e.closest('[data-dzg]'); }

  // Elemento piu piccolo, visibile, il cui testo corrisponde.
  function cerca(re, sel) {
    var n = document.querySelectorAll(sel), best = null, bl = 1e9;
    for (var i = 0; i < n.length; i++) {
      var e = n[i];
      if (nostro(e) || !visibile(e)) continue;
      var t = testo(e);
      if (!t || t.length > 120 || !re.test(t)) continue;
      if (t.length < bl) { best = e; bl = t.length; }
    }
    return best;
  }

  function stile(e, s) { for (var k in s) e.style[k] = s[k]; }
  // Pagina rimpicciolita (versione computer): ingrandisce la guida in proporzione, cosi resta leggibile.
  function scala() {
    var v = W.visualViewport, s = v && v.scale ? v.scale : 1;
    return Math.max(1, Math.min(4, 1 / s));
  }
  function vista() {
    var v = W.visualViewport;
    return v ? { x: v.offsetLeft, y: v.offsetTop, w: v.width, h: v.height } : { x: 0, y: 0, w: innerWidth, h: innerHeight };
  }
  function misure() {
    var k = scala();
    stile(anello, { borderWidth: (4 * k) + 'px', borderRadius: (12 * k) + 'px' });
    stile(etichetta, { fontSize: (17 * k) + 'px', padding: (9 * k) + 'px ' + (13 * k) + 'px', borderRadius: (10 * k) + 'px' });
    stile(scheda, { fontSize: (17 * k) + 'px', padding: (14 * k) + 'px ' + (16 * k) + 'px', borderRadius: (14 * k) + 'px', borderLeftWidth: (6 * k) + 'px' });
    if (scheda.style.display !== 'none') {
      var v = vista();
      stile(scheda, { left: (v.x + 10 * k) + 'px', width: (v.w - 20 * k) + 'px', right: 'auto', bottom: 'auto' });
      scheda.style.top = (v.y + v.h - scheda.offsetHeight - 14 * k) + 'px';
    }
    return k;
  }

  function crea() {
    if (anello && document.documentElement.contains(anello)) return;
    anello = document.createElement('div');
    etichetta = document.createElement('div');
    scheda = document.createElement('div');
    [anello, etichetta, scheda].forEach(function (e) {
      e.setAttribute('data-dzg', '1');
      stile(e, { position: 'fixed', zIndex: '2147483647', pointerEvents: 'none', display: 'none', boxSizing: 'border-box' });
      document.documentElement.appendChild(e);
    });
    stile(anello, { border: '4px solid #FF7AB6', borderRadius: '12px', boxShadow: '0 0 0 4px rgba(255,122,182,.35), 0 0 22px rgba(255,122,182,.9)', transition: 'opacity .25s' });
    stile(etichetta, { background: '#FF7AB6', color: '#111', font: '700 17px/1.2 system-ui,sans-serif', padding: '9px 13px', borderRadius: '10px', boxShadow: '0 4px 14px rgba(0,0,0,.35)', whiteSpace: 'nowrap' });
    stile(scheda, { left: '10px', right: '10px', bottom: '14px', background: '#1A1A1F', color: '#fff', font: '600 17px/1.4 system-ui,sans-serif', padding: '14px 16px', borderRadius: '14px', boxShadow: '0 6px 24px rgba(0,0,0,.45)', borderLeft: '6px solid #FF7AB6' });
  }

  function info(t, verde) {
    crea();
    if (!t) { scheda.style.display = 'none'; return; }
    scheda.textContent = t;
    scheda.style.display = 'block';
    scheda.style.borderLeftColor = verde ? '#9EFF9E' : '#FF7AB6';
    misure();
  }

  function indica(e, t) {
    crea();
    if (e !== bersaglio && e) {
      var r = e.getBoundingClientRect(), v = vista();
      if (r.top < v.y + 60 || r.bottom > v.y + v.h - 140 * scala() || r.left < v.x || r.right > v.x + v.w) {
        try { e.scrollIntoView({ block: 'center', inline: 'center' }); } catch (x) { e.scrollIntoView(); }
      }
    }
    bersaglio = e;
    etichetta.textContent = t || '';
    posiziona();
  }

  function posiziona() {
    if (!anello) return;
    var k = misure(), v = vista();
    if (!bersaglio || !document.documentElement.contains(bersaglio) || !visibile(bersaglio)) {
      anello.style.display = 'none'; etichetta.style.display = 'none'; return;
    }
    var r = bersaglio.getBoundingClientRect(), p = 6 * k;
    stile(anello, { display: 'block', left: (r.left - p) + 'px', top: (r.top - p) + 'px', width: (r.width + 2 * p) + 'px', height: (r.height + 2 * p) + 'px' });
    battito = (battito + 1) % 16;
    anello.style.opacity = battito < 8 ? '1' : '.45';
    etichetta.style.display = 'block';
    var h = etichetta.offsetHeight || 38, w = etichetta.offsetWidth || 150;
    var top = r.bottom + p + 8 * k;
    if (top + h > v.y + v.h - scheda.offsetHeight - 30 * k) top = r.top - p - 8 * k - h;
    etichetta.style.top = Math.max(v.y + 6 * k, top) + 'px';
    etichetta.style.left = Math.max(v.x + 8 * k, Math.min(r.left + r.width / 2 - w / 2, v.x + v.w - w - 8 * k)) + 'px';
  }

  // Pagine Google fatte per il computer: a misura di telefono.
  function misuraTelefono() {
    if (document.head && !document.querySelector('meta[name=viewport]')) {
      var m = document.createElement('meta');
      m.name = 'viewport';
      m.content = 'width=device-width, initial-scale=1';
      document.head.appendChild(m);
    }
  }

  function interruttore() {
    var c = document.querySelectorAll('[role=switch],[aria-checked],input[type=checkbox],button[aria-pressed],.mdc-switch,mat-slide-toggle');
    for (var i = 0; i < c.length; i++) if (!nostro(c[i]) && visibile(c[i])) return c[i];
    return null;
  }

  function acceso(sw) {
    if (sw) {
      var a = sw.getAttribute('aria-checked') || sw.getAttribute('aria-pressed');
      if (a === 'true' || sw.checked === true) return true;
      var d = sw.querySelector && sw.querySelector('input[type=checkbox],[role=switch],[aria-checked]');
      if (d && (d.checked === true || d.getAttribute('aria-checked') === 'true')) return true;
      if (/(^|[\s_-])(checked|selected|on)($|[\s_-])/i.test(String(sw.className || ''))) return true;
    }
    return !!cerca(/^(Attivato|Attiva|On|Enabled)$/i, 'div,span,p');
  }

  function passo() {
    if (fatto) return;
    if (location.pathname.indexOf('/home/usersettings') === 0) {
      misuraTelefono();
      var sw = interruttore();
      // Aperta dal menu "Spegni interruttore API": guida a spegnerlo, non avvisa l'APK.
      if (W.__dzSpegni) {
        if (sw && !acceso(sw)) {
          bersaglio = null; posiziona();
          info('Fatto ✓  Interruttore spento. Tocca «TORNA A …» in alto.', true);
          return;
        }
        if (sw) {
          indica(sw, 'Tocca l\'interruttore');
          info('Passo 2 di 2: spegni l\'interruttore.');
          return;
        }
        var r0 = cerca(/^(API Google Apps Script|Google Apps Script API)\b/i, 'a,button,[role=button],[role=link],[role=listitem],li,div,span');
        if (r0) {
          indica(r0, 'Tocca qui');
          info('Passo 1 di 2: tocca "API Google Apps Script".');
          return;
        }
        indica(null); info('');
        return;
      }
      if (acceso(sw)) {
        fatto = true;
        bersaglio = null; posiziona();
        info('Fatto ✓  Interruttore acceso. Torno all\'installazione...', true);
        try { if (W.DZ && W.DZ.msg) W.DZ.msg('dz-api-on'); } catch (x) {}
        return;
      }
      if (sw) {
        indica(sw, 'Tocca l\'interruttore');
        info('Passo 2 di 2: accendi l\'interruttore. Poi torni all\'installazione da solo.');
        return;
      }
      var riga = cerca(/^(API Google Apps Script|Google Apps Script API)\b/i, 'a,button,[role=button],[role=link],[role=listitem],li,div,span');
      if (riga) {
        indica(riga, 'Tocca qui');
        info('Passo 1 di 2: tocca "API Google Apps Script".');
        return;
      }
      indica(null); info('');
      return;
    }
    // Finestra "Autorizzazione necessaria" delle app Apps Script.
    var rev = cerca(/^(Review permissions|Rivedi (le )?autorizzazioni|Esamina (le )?autorizzazioni)$/i, 'a,button,[role=button],div,span');
    if (rev) {
      // Riquadro piccolo (pagina da computer): chiede all'APK di ingrandire la pagina, una volta.
      if (!zoomFatto) {
        zoomFatto = true;
        var d = rev, best = null, vw = vista().w;
        while (d && d !== document.body) { if (d.getBoundingClientRect().width < vw * 0.9) best = d; else break; d = d.parentElement; }
        if (best) {
          var f = Math.min(3, (vw * 0.92) / best.getBoundingClientRect().width);
          if (f > 1.15) { try { if (W.DZ && W.DZ.msg) W.DZ.msg('dz-zoom:' + f.toFixed(2)); } catch (x) {} }
        }
      }
      indica(rev, 'Tocca qui');
      info('Per usare l\'app, Google chiede il tuo permesso. Tocca il pulsante blu "' + testo(rev) + '" (Rivedi le autorizzazioni).');
      return;
    }
    indica(null); info('');
  }

  var attesa = null;
  function presto() {
    if (attesa) return;
    attesa = setTimeout(function () { attesa = null; try { passo(); } catch (x) {} }, 150);
  }
  W.__dzGuida = presto;
  try {
    new MutationObserver(presto).observe(document.documentElement, {
      childList: true, subtree: true, characterData: true,
      attributes: true, attributeFilter: ['aria-checked', 'aria-pressed', 'class', 'checked']
    });
  } catch (x) {}
  addEventListener('scroll', posiziona, true);
  addEventListener('resize', posiziona);
  setInterval(function () { try { posiziona(); } catch (x) {} }, 150);
  setInterval(function () { try { passo(); } catch (x) {} }, 1000);
  presto();
})();
