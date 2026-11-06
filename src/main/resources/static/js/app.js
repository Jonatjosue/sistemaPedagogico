(function () {
    const $ = id => document.getElementById(id);
    const root = document.documentElement;
    const csrf = {
        h: document.querySelector('meta[name="_csrf_header"]').content,
        t: document.querySelector('meta[name="_csrf"]').content
    };

    const OBJETIVOS = {
        COMPILAR_CON_ERROR: 'Objetivo: haz que el compilador reporte al menos un error.',
        COMPILAR_EXITOSO: 'Objetivo: escribe código que compile sin errores.',
        EJECUTAR_CON_ERROR: 'Objetivo: compila bien y haz que el programa falle al ejecutarse.',
        EJECUTAR_EXITOSO: 'Objetivo: compila y ejecuta el programa sin errores.'
    };
    const CONSOLA_VACIA = 'La salida de la compilación y la ejecución aparecerá aquí.';

    /* ---------- Menús ---------- */
    function cerrarMenus() {
        document.querySelectorAll('.menu-btn[aria-expanded]').forEach(b => {
            b.setAttribute('aria-expanded', 'false');
            if (b.nextElementSibling) b.nextElementSibling.hidden = true;
        });
    }
    document.querySelectorAll('.menu > .menu-btn').forEach(b => {
        b.addEventListener('click', e => {
            e.stopPropagation();
            const abierto = b.getAttribute('aria-expanded') === 'true';
            cerrarMenus();
            if (!abierto) {
                b.setAttribute('aria-expanded', 'true');
                b.nextElementSibling.hidden = false;
            }
        });
    });
    document.addEventListener('click', cerrarMenus);
    document.addEventListener('keydown', e => { if (e.key === 'Escape') cerrarMenus(); });

    document.querySelectorAll('[data-cerrar]').forEach(b =>
        b.addEventListener('click', () => b.closest('dialog').close()));

    /* ---------- Preferencias (fuente y tema) ---------- */
    let fs = parseInt(root.style.getPropertyValue('--fs')) || 16;
    let tema = root.dataset.theme || 'claro';
    let temporizador;

    function guardarPreferencias() {
        clearTimeout(temporizador);
        temporizador = setTimeout(() => {
            fetch('/api/preferencias', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', [csrf.h]: csrf.t },
                body: JSON.stringify({ tamanoFuente: fs, tema: tema })
            }).catch(() => {});
        }, 400);
    }
    function cambiarFuente(delta) {
        fs = Math.min(28, Math.max(12, fs + delta));
        root.style.setProperty('--fs', fs + 'px');
        guardarPreferencias();
    }
    function cambiarTema(t) {
        tema = t;
        root.dataset.theme = t;
        guardarPreferencias();
    }
    $('mMas').addEventListener('click', () => cambiarFuente(2));
    $('mMenos').addEventListener('click', () => cambiarFuente(-2));
    $('mClaro').addEventListener('click', () => cambiarTema('claro'));
    $('mOscuro').addEventListener('click', () => cambiarTema('oscuro'));

    /* ---------- Lecciones ---------- */
    const editor = $('editor');
    let activaId = null;
    let sesionId = null;
    let compilado = null;
    let ocupado = false;

    async function abrirDialogo() {
        const ul = $('listaLecciones');
        ul.innerHTML = '<li class="muted">Cargando…</li>';
        $('dlgLecciones').showModal();
        try {
            const r = await fetch('/api/lecciones');
            if (!r.ok) throw new Error();
            const lista = await r.json();
            ul.innerHTML = '';
            if (!lista.length) {
                ul.innerHTML = '<li class="muted">No hay lecciones disponibles.</li>';
                return;
            }
            lista.forEach(l => {
                const li = document.createElement('li');
                const b = document.createElement('button');
                b.type = 'button';
                b.textContent = l.titulo;
                b.addEventListener('click', () => cargar(l.id));
                li.appendChild(b);
                ul.appendChild(li);
            });
        } catch (e) {
            ul.innerHTML = '<li class="muted">No se pudo cargar la lista de lecciones.</li>';
        }
    }

    async function cargar(id) {
        try {
            const r = await fetch('/api/lecciones/' + id);
            if (!r.ok) throw new Error();
            const d = await r.json();
            const s = await postJson('/api/practica/abrir', { leccionId: d.id });
            if (!s.ok) throw new Error();
            sesionId = s.data.sesionId;
            activaId = d.id;
            compilado = null;
            $('nombreLeccion').textContent = d.titulo;
            $('contenidoLeccion').innerHTML = d.html;       // HTML ya escapado por el servidor
            const o = $('objetivo');
            o.textContent = OBJETIVOS[d.objetivo] || '';
            o.hidden = !o.textContent;
            editor.value = '';
            $('consola').textContent = CONSOLA_VACIA;
            $('dlgLecciones').close();
            refrescarBotones();
            editor.focus();
        } catch (e) {
            alert('No se pudo abrir la lección.');
        }
    }

    function cerrarLeccion() {
        if (sesionId) postJson('/api/practica/' + sesionId + '/cerrar', {});
        sesionId = null;
        activaId = null;
        compilado = null;
        $('nombreLeccion').textContent = 'Ninguna lección abierta';
        $('contenidoLeccion').innerHTML =
            '<p class="muted">Abre una lección desde el menú <b>Lecciones → Abrir lección</b>.</p>';
        $('objetivo').hidden = true;
        editor.value = '';
        $('consola').textContent = CONSOLA_VACIA;
        refrescarBotones();
    }

    $('mAbrir').addEventListener('click', abrirDialogo);
    $('mCerrar').addEventListener('click', cerrarLeccion);
    $('mAyuda').addEventListener('click', () => $('dlgAyuda').showModal());

    /* ---------- Editor: Tab inserta 4 espacios ---------- */
    editor.addEventListener('keydown', e => {
        if (e.key === 'Tab' && !e.shiftKey) {
            e.preventDefault();
            const s = editor.selectionStart, f = editor.selectionEnd;
            editor.setRangeText('    ', s, f, 'end');
        }
    });
    /* ---------- Administración de lecciones (solo con permiso) ---------- */
    if ($('mImportar')) {
        const ETIQ = { CREADA: 'Creada', ACTUALIZADA: 'Actualizada', RECHAZADA: 'Rechazada' };

        const pintarResultado = (cont, d) => {
            cont.textContent = '';
            const ul = document.createElement('ul');
            ul.className = 'res';
            d.lecciones.forEach(i => {
                const li = document.createElement('li');
                li.className = i.estado;
                const b = document.createElement('b');
                b.textContent = i.titulo;
                li.append(b, ' — ' + ETIQ[i.estado] + '. ' + i.detalle);
                ul.appendChild(li);
            });
            cont.appendChild(ul);
            if (d.avisos.length) {
                const p = document.createElement('p');
                p.className = 'avisos';
                p.textContent = 'Avisos: ' + d.avisos.join(' · ');
                cont.appendChild(p);
            }
        };

        $('mImportar').addEventListener('click', () => {
            $('archivoZip').value = '';
            $('resultadoImport').textContent = '';
            $('dlgImportar').showModal();
        });

        $('bImportar').addEventListener('click', async () => {
            const out = $('resultadoImport');
            const f = $('archivoZip').files[0];
            if (!f) { out.textContent = 'Selecciona un archivo .zip.'; return; }
            const fd = new FormData();
            fd.append('archivo', f);
            out.textContent = 'Importando…';
            $('bImportar').disabled = true;
            try {
                const r = await fetch('/admin/lecciones/importar', {
                    method: 'POST', headers: { [csrf.h]: csrf.t }, body: fd
                });
                const d = await r.json().catch(() => null);
                if (!r.ok) { out.textContent = (d && d.mensaje) || 'No se pudo importar el archivo.'; return; }
                pintarResultado(out, d);
            } catch (e) {
                out.textContent = 'No se pudo importar el archivo.';
            } finally {
                $('bImportar').disabled = false;
            }
        });

        const eliminar = async (l, li) => {
            if (!confirm('¿Eliminar la lección "' + l.titulo + '"? Su historial se conserva, pero ya no podrá abrirse.')) return;
            try {
                const r = await fetch('/admin/lecciones/' + l.id, { method: 'DELETE', headers: { [csrf.h]: csrf.t } });
                if (!r.ok && r.status !== 404) throw new Error();
                li.remove();
                if (activaId === l.id) cerrarLeccion();
            } catch (e) {
                alert('No se pudo eliminar la lección.');
            }
        };

        $('mEliminar').addEventListener('click', async () => {
            const ul = $('listaEliminar');
            ul.innerHTML = '<li class="muted">Cargando…</li>';
            $('dlgEliminar').showModal();
            try {
                const r = await fetch('/api/lecciones');
                if (!r.ok) throw new Error();
                const lista = await r.json();
                ul.innerHTML = '';
                if (!lista.length) { ul.innerHTML = '<li class="muted">No hay lecciones.</li>'; return; }
                lista.forEach(l => {
                    const li = document.createElement('li');
                    li.className = 'fila';
                    const s = document.createElement('span');
                    s.textContent = l.titulo;
                    const b = document.createElement('button');
                    b.type = 'button';
                    b.className = 'btn peligro';
                    b.textContent = 'Eliminar';
                    b.addEventListener('click', () => eliminar(l, li));
                    li.append(s, b);
                    ul.appendChild(li);
                });
            } catch (e) {
                ul.innerHTML = '<li class="muted">No se pudo cargar la lista.</li>';
            }
        });
    }

    /* ---------- Compilar y ejecutar ---------- */
    async function postJson(url, body) {
        try {
            const r = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrf.h]: csrf.t },
                body: JSON.stringify(body)
            });
            const data = await r.json().catch(() => null);
            return { ok: r.ok, status: r.status, data: data };
        } catch (e) {
            return { ok: false, status: 0, data: null };
        }
    }

    function refrescarBotones() {
        $('bCompilar').disabled = !sesionId || ocupado;
        $('bEjecutar').disabled = !sesionId || ocupado || compilado === null || editor.value !== compilado;
        const aviso = sesionId ? '' : 'Abre una lección primero';
        $('bCompilar').title = aviso;
        $('bEjecutar').title = aviso || (compilado === null || editor.value !== compilado ? 'Compila primero tu código' : '');
    }

    async function accionar(tipo) {
        if (!sesionId || ocupado) return;
        const codigo = editor.value;
        if (!codigo.trim()) { $('consola').textContent = 'Escribe tu código primero.'; return; }

        ocupado = true;
        refrescarBotones();
        $('consola').textContent = tipo === 'compilar' ? 'Compilando…' : 'Ejecutando…';

        const r = await postJson('/api/practica/' + sesionId + '/' + tipo, { codigo: codigo });
        ocupado = false;

        if (!r.ok) {
            $('consola').textContent = (r.data && r.data.mensaje) || 'No se pudo completar la operación. Intenta de nuevo.';
            refrescarBotones();
            return;
        }
        const d = r.data;
        if (tipo === 'compilar') compilado = d.compilo ? codigo : null;
        let texto = d.consola;
        if (d.completadaAhora) texto += '\n\n✔ ¡Objetivo cumplido! La lección quedó como completada.';
        $('consola').textContent = texto;
        refrescarBotones();
    }

    $('bCompilar').addEventListener('click', () => accionar('compilar'));
    $('bEjecutar').addEventListener('click', () => accionar('ejecutar'));
    editor.addEventListener('input', refrescarBotones);
    refrescarBotones();

    /* ---------- Historial ---------- */
    const ES_ADMIN = document.body.dataset.admin === 'true';
    const hist = { tipo: 'registros', pagina: 0 };

    const pill = (ok, si, no) => {
        const s = document.createElement('span');
        s.className = 'pill ' + (ok ? 'ok' : 'bad');
        s.textContent = ok ? si : no;
        return s;
    };

    const salidaBtn = r => {
        const texto = r.salida || '(sin salida)';
        const b = document.createElement('button');
        b.type = 'button';
        b.className = 'cell-out';
        b.textContent = texto.split('\n')[0];
        b.title = texto.length > 600 ? texto.slice(0, 600) + '…' : texto;    // al pasar el cursor
        b.addEventListener('click', () => {                                   // al tocar o hacer clic
            $('salidaTexto').textContent = texto;
            $('dlgSalida').showModal();
        });
        return b;
    };

    // Cada columna: [título, función que da el contenido, clase opcional]
    const COLUMNAS = {
        registros: () => [
            ...(ES_ADMIN ? [['Usuario', r => r.usuario, 'l'], ['Código / usuario', r => r.identificador]] : []),
            ['Lección', r => r.leccion, 'l'],
            ['Fecha inicio', r => r.inicio],
            ['Fecha fin', r => r.fin || 'En curso'],
            ['Estado', r => pill(r.estado === 'COMPLETADA', 'Completada', 'Pendiente')],
            ['Intentos acertados', r => r.acertados],
            ['Intentos fallidos', r => r.fallidos]
        ],
        intentos: () => [
            ...(ES_ADMIN ? [['Usuario', r => r.usuario, 'l'], ['Id sesión', r => r.sesionId]] : []),
            ['Lección', r => r.leccion, 'l'],
            ['Fecha intento', r => r.fecha],
            ['Tipo', r => r.tipo === 'COMPILAR' ? 'Compilar' : 'Ejecutar'],
            ['Resultado', r => pill(r.exito, 'Éxito', 'Fallo')],
            ['Estado de la lección', r => pill(r.estado === 'COMPLETADA', 'Completada', 'Pendiente')],
            ['Salida', salidaBtn, 'l']
        ],
        ingresos: () => [
            ['Usuario', r => r.usuario, 'l'],
            ['Código / usuario', r => r.identificador],
            ['Tipo', r => r.tipo === 'ADMIN' ? 'Administrador' : 'Estudiante'],
            ['Fecha ingreso', r => r.fecha],
            ['IP', r => r.ip || ''],
            ['User agent', r => r.userAgent || '', 'l']
        ]
    };

    function pintarTabla(d) {
        const cont = $('tablaHist');
        cont.textContent = '';
        if (!d.items.length) {
            const p = document.createElement('p');
            p.className = 'muted';
            p.textContent = 'Todavía no hay registros.';
            cont.appendChild(p);
            return;
        }
        const cols = COLUMNAS[hist.tipo]();
        const wrap = document.createElement('div');
        wrap.className = 'tabla-wrap';
        const t = document.createElement('table');
        t.className = 'tabla';

        const trh = document.createElement('tr');
        cols.forEach(c => { const th = document.createElement('th'); th.textContent = c[0]; trh.appendChild(th); });
        const thead = document.createElement('thead');
        thead.appendChild(trh);
        t.appendChild(thead);

        const tbody = document.createElement('tbody');
        d.items.forEach(row => {
            const tr = document.createElement('tr');
            cols.forEach(c => {
                const td = document.createElement('td');
                if (c[2]) td.className = c[2];
                const v = c[1](row);
                if (v instanceof Node) td.appendChild(v); else td.textContent = v == null ? '' : v;
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        t.appendChild(tbody);
        wrap.appendChild(t);
        cont.appendChild(wrap);
    }

    async function cargarHistorial() {
        $('tablaHist').textContent = 'Cargando…';
        let url = '/api/historial/' + hist.tipo + '?pagina=' + hist.pagina;
        if (hist.tipo === 'ingresos') {
            if ($('hDesde').value) url += '&desde=' + $('hDesde').value;
            if ($('hHasta').value) url += '&hasta=' + $('hHasta').value;
        }
        try {
            const r = await fetch(url);
            if (!r.ok) throw new Error();
            const d = await r.json();
            pintarTabla(d);
            const paginas = Math.max(d.totalPaginas, 1);
            $('hInfo').textContent = 'Página ' + (d.pagina + 1) + ' de ' + paginas + ' · ' + d.total + ' registro(s)';
            $('hPrev').disabled = d.pagina <= 0;
            $('hNext').disabled = d.pagina + 1 >= paginas;
        } catch (e) {
            $('tablaHist').textContent = 'No se pudo cargar el historial.';
            $('hInfo').textContent = '';
            $('hPrev').disabled = true;
            $('hNext').disabled = true;
        }
    }

    function abrirHistorial(tipo) {
        hist.tipo = tipo;
        hist.pagina = 0;
        document.querySelectorAll('#tabsHist button').forEach(b =>
            b.setAttribute('aria-selected', String(b.dataset.h === tipo)));
        $('filtrosHist').hidden = tipo !== 'ingresos';
        if (!$('dlgHistorial').open) $('dlgHistorial').showModal();
        cargarHistorial();
    }

    $('tabsHist').addEventListener('click', e => {
        const b = e.target.closest('button');
        if (b) abrirHistorial(b.dataset.h);
    });
    $('hPrev').addEventListener('click', () => { hist.pagina--; cargarHistorial(); });
    $('hNext').addEventListener('click', () => { hist.pagina++; cargarHistorial(); });
    $('hFiltrar').addEventListener('click', () => { hist.pagina = 0; cargarHistorial(); });
    $('mHistLec').addEventListener('click', () => abrirHistorial('registros'));
    $('mHistInt').addEventListener('click', () => abrirHistorial('intentos'));
    if ($('mHistIng')) $('mHistIng').addEventListener('click', () => abrirHistorial('ingresos'));

    /* ---------- Utilidades de administración ---------- */
    async function api(metodo, url, cuerpo) {
        try {
            const r = await fetch(url, {
                method: metodo,
                headers: { 'Content-Type': 'application/json', [csrf.h]: csrf.t },
                body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo)
            });
            const data = await r.json().catch(() => null);
            return { ok: r.ok, status: r.status, data: data };
        } catch (e) {
            return { ok: false, status: 0, data: null };
        }
    }
    const crear = (tag, cls, texto) => {
        const e = document.createElement(tag);
        if (cls) e.className = cls;
        if (texto !== undefined) e.textContent = texto;
        return e;
    };
    const msgDe = (r, def) => (r.data && r.data.mensaje) || def;

    /* ---------- Cambiar mi contraseña ---------- */
    if ($('mPass')) {
        $('mPass').addEventListener('click', () => {
            ['pActual', 'pNueva', 'pRep'].forEach(i => $(i).value = '');
            $('pMsg').textContent = '';
            $('pMsg').className = 'errtxt';
            $('dlgPass').showModal();
        });
        $('pGuardar').addEventListener('click', async () => {
            const m = $('pMsg');
            m.className = 'errtxt';
            if ($('pNueva').value !== $('pRep').value) { m.textContent = 'Las contraseñas nuevas no coinciden.'; return; }
            $('pGuardar').disabled = true;
            const r = await api('POST', '/api/admin/mi-password', { actual: $('pActual').value, nueva: $('pNueva').value });
            $('pGuardar').disabled = false;
            if (!r.ok) { m.textContent = msgDe(r, 'No se pudo cambiar la contraseña.'); return; }
            ['pActual', 'pNueva', 'pRep'].forEach(i => $(i).value = '');
            m.className = 'errtxt ok';
            m.textContent = 'Contraseña actualizada.';
        });
    }

    /* ---------- Administrar administradores ---------- */
    if ($('mAdministrar')) {
        const NOMBRES = { GESTIONAR_LECCIONES: 'Lecciones', GESTIONAR_USUARIOS: 'Administradores', VER_REPORTES: 'Estadística' };
        let editando = null;

        async function cargarAdmins() {
            const cont = $('tablaAdmins');
            cont.textContent = 'Cargando…';
            const r = await api('GET', '/api/admin/usuarios');
            if (!r.ok) { cont.textContent = msgDe(r, 'No se pudo cargar la lista.'); return; }

            const wrap = crear('div', 'tabla-wrap');
            const t = crear('table', 'tabla');
            const trh = crear('tr');
            ['Usuario', 'Nombre', 'Fecha de creación', 'Estado', 'Módulos', 'Acciones']
                .forEach(x => trh.appendChild(crear('th', null, x)));
            const thead = crear('thead');
            thead.appendChild(trh);
            t.appendChild(thead);

            const tb = crear('tbody');
            r.data.forEach(a => {
                const tr = crear('tr', a.activo ? '' : 'off');
                const c1 = crear('td', 'l', a.acceso + (a.tu ? ' (tú)' : ''));
                const c2 = crear('td', 'l', a.nombre);
                const c3 = crear('td', null, a.fecha);
                const c4 = crear('td');
                c4.appendChild(crear('span', 'pill ' + (a.activo ? 'ok' : 'bad'), a.activo ? 'Activo' : 'Inactivo'));
                const c5 = crear('td', 'l');
                if (a.permisos.length) a.permisos.forEach(p => c5.appendChild(crear('span', 'chip', NOMBRES[p] || p)));
                else c5.appendChild(crear('span', 'muted', 'Sin módulos'));
                const c6 = crear('td');

                const bEd = crear('button', 'btn chico', 'Editar');
                bEd.type = 'button';
                bEd.addEventListener('click', () => abrirForm(a));
                c6.append(bEd, ' ');

                const bAcc = crear('button', 'btn chico' + (a.activo ? ' peligro' : ''), a.activo ? 'Eliminar' : 'Reactivar');
                bAcc.type = 'button';
                if (a.tu && a.activo) { bAcc.disabled = true; bAcc.title = 'No puedes eliminarte a ti mismo'; }
                bAcc.addEventListener('click', async () => {
                    if (a.activo && !confirm('¿Eliminar a "' + a.nombre + '"? Quedará inactivo y no podrá ingresar.')) return;
                    const x = a.activo ? await api('DELETE', '/api/admin/usuarios/' + a.id)
                        : await api('POST', '/api/admin/usuarios/' + a.id + '/reactivar');
                    if (!x.ok) alert(msgDe(x, 'No se pudo completar la acción.'));
                    cargarAdmins();
                });
                c6.appendChild(bAcc);

                tr.append(c1, c2, c3, c4, c5, c6);
                tb.appendChild(tr);
            });
            t.appendChild(tb);
            wrap.appendChild(t);
            cont.textContent = '';
            cont.appendChild(wrap);
        }

        function abrirForm(a) {
            editando = a || null;
            $('afTitulo').textContent = a ? 'Editar administrador' : 'Nuevo administrador';
            $('afAcceso').value = a ? a.acceso : '';
            $('afAcceso').disabled = !!a;
            $('afNombre').value = a ? a.nombre : '';
            $('afPass').value = '';
            $('afHint').textContent = a ? 'Deja la contraseña vacía para no cambiarla.' : 'Mínimo 8 caracteres.';
            document.querySelectorAll('#dlgAdminForm input[type=checkbox]')
                .forEach(c => c.checked = !!a && a.permisos.includes(c.value));
            $('afMsg').textContent = '';
            $('dlgAdminForm').showModal();
        }

        $('mAdministrar').addEventListener('click', () => { $('dlgAdmins').showModal(); cargarAdmins(); });
        $('aNuevo').addEventListener('click', () => abrirForm(null));
        $('afGuardar').addEventListener('click', async () => {
            const cuerpo = {
                acceso: $('afAcceso').value,
                nombre: $('afNombre').value,
                password: $('afPass').value,
                permisos: [...document.querySelectorAll('#dlgAdminForm input[type=checkbox]:checked')].map(c => c.value)
            };
            $('afGuardar').disabled = true;
            const r = editando ? await api('PUT', '/api/admin/usuarios/' + editando.id, cuerpo)
                : await api('POST', '/api/admin/usuarios', cuerpo);
            $('afGuardar').disabled = false;
            if (!r.ok) { $('afMsg').textContent = msgDe(r, 'No se pudo guardar.'); return; }
            $('dlgAdminForm').close();
            cargarAdmins();
        });
    }

    /* ---------- Estadística ---------- */
    if ($('mEstadistica')) {
        const NS = 'http://www.w3.org/2000/svg';

        function grafico(serie) {
            const cont = $('sGrafico');
            cont.textContent = '';
            const W = 640, H = 220, L = 34, B = 26, T = 10;
            const max = Math.max(1, ...serie.map(p => p.usuarios));
            const top = Math.ceil(max / 4) * 4;
            const svg = document.createElementNS(NS, 'svg');
            svg.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
            svg.setAttribute('role', 'img');
            svg.setAttribute('aria-label', 'Estudiantes que ingresan por período');
            const add = (tag, attrs, texto) => {
                const e = document.createElementNS(NS, tag);
                for (const k in attrs) e.setAttribute(k, attrs[k]);
                if (texto !== undefined) e.textContent = texto;
                svg.appendChild(e);
                return e;
            };
            for (let g = 0; g <= 4; g++) {
                const y = T + (H - T - B) * (1 - g / 4);
                add('line', { x1: L, x2: W, y1: y, y2: y, stroke: 'var(--line)', 'stroke-opacity': '.25' });
                add('text', { x: L - 6, y: y + 4, 'text-anchor': 'end', class: 'axis' }, String(top * g / 4));
            }
            const n = serie.length, bw = (W - L) / n, paso = Math.ceil(n / 12);
            serie.forEach((p, i) => {
                const alto = (H - T - B) * p.usuarios / top, x = L + i * bw + bw * 0.18, w = bw * 0.64;
                const r = add('rect', { x: x, y: H - B - alto, width: w, height: alto, fill: 'var(--azul)' });
                const t = document.createElementNS(NS, 'title');
                t.textContent = p.etiqueta + ': ' + p.usuarios + ' estudiante(s)';
                r.appendChild(t);
                if (i % paso === 0) add('text', { x: x + w / 2, y: H - 8, 'text-anchor': 'middle', class: 'axis' }, p.etiqueta);
            });
            cont.appendChild(svg);
        }

        function ranking(titulo, lista, valor, maximo, color, formato) {
            const sec = crear('section', 'sec');
            sec.appendChild(crear('h3', null, titulo));
            const cuerpo = crear('div', 'cuerpo');
            if (!lista.length) cuerpo.appendChild(crear('p', 'muted', 'Sin datos suficientes (se necesitan al menos 3 intentos por lección).'));
            lista.forEach(l => {
                const fila = crear('div', 'row');
                const nombre = crear('span', null, l.leccion);
                nombre.title = l.leccion;
                const pista = crear('div', 'track');
                const relleno = crear('div', 'fill');
                relleno.style.width = Math.min(100, valor(l) / maximo * 100) + '%';
                relleno.style.background = color;
                pista.appendChild(relleno);
                fila.append(nombre, pista, crear('b', null, formato(l)));
                cuerpo.appendChild(fila);
            });
            sec.appendChild(cuerpo);
            return sec;
        }

        async function cargarStats() {
            $('sMsg').textContent = '';
            const q = new URLSearchParams({ desde: $('sDesde').value, hasta: $('sHasta').value, agrupar: $('sAgrupar').value });
            const r = await api('GET', '/api/estadisticas?' + q);
            if (!r.ok) { $('sMsg').textContent = msgDe(r, 'No se pudieron cargar las estadísticas.'); return; }
            const d = r.data;

            const total = d.acertados + d.fallidos;
            const kpis = [
                ['Estudiantes que ingresaron', d.usuariosUnicos, ''],
                ['Intentos acertados', d.acertados, 'var(--ok)'],
                ['Intentos fallidos', d.fallidos, '#d6403d'],
                ['Tasa de acierto', total ? Math.round(100 * d.acertados / total) + '%' : '—', '']
            ];
            $('sKpis').textContent = '';
            kpis.forEach(k => {
                const c = crear('div', 'kpi');
                const b = crear('b', null, String(k[1]));
                if (k[2]) b.style.color = k[2];
                c.append(b, k[0]);
                $('sKpis').appendChild(c);
            });

            grafico(d.serie);

            const usos = l => l.usos, pct = l => l.porcentaje || 0;
            const maxUso = Math.max(1, ...d.masUsadas.map(l => l.usos), ...d.menosUsadas.map(l => l.usos));
            const rk = $('sRank');
            rk.textContent = '';
            rk.append(
                ranking('Lecciones más utilizadas', d.masUsadas, usos, maxUso, 'var(--azul)', l => l.usos + ' usos'),
                ranking('Lecciones menos utilizadas', d.menosUsadas, usos, maxUso, 'var(--azul)', l => l.usos + ' usos'),
                ranking('Lecciones más simples (mayor % de acierto)', d.masSimples, pct, 100, 'var(--ok)', l => l.porcentaje + '%'),
                ranking('Lecciones más difíciles (menor % de acierto)', d.masDificiles, pct, 100, '#d6403d', l => l.porcentaje + '%')
            );

            const pl = $('sPorLeccion');
            pl.textContent = '';
            if (!d.porLeccion.length) pl.appendChild(crear('p', 'muted', 'No hay lecciones.'));
            d.porLeccion.forEach(l => {
                const t = l.acertados + l.fallidos;
                const fila = crear('div', 'row');
                const nombre = crear('span', null, l.leccion);
                nombre.title = l.leccion;
                const pista = crear('div', 'track');
                if (t) {
                    const a = crear('div', 'fill'), f = crear('div', 'fill');
                    a.style.width = (100 * l.acertados / t) + '%'; a.style.background = 'var(--ok)';
                    f.style.width = (100 * l.fallidos / t) + '%'; f.style.background = '#d6403d';
                    pista.append(a, f);
                }
                fila.append(nombre, pista, crear('b', null, l.acertados + ' / ' + l.fallidos));
                pl.appendChild(fila);
            });
        }

        $('mEstadistica').addEventListener('click', () => {
            if (!$('sDesde').value) {
                const d = new Date();
                $('sHasta').value = d.toLocaleDateString('en-CA');
                d.setDate(d.getDate() - 29);
                $('sDesde').value = d.toLocaleDateString('en-CA');
            }
            $('dlgStats').showModal();
            cargarStats();
        });
        $('sAplicar').addEventListener('click', cargarStats);
    }

})();