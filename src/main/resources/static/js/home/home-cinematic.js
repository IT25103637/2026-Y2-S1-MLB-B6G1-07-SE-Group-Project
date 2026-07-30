/**
 * Cinematic Home Page — scroll engine for the public landing sections.
 *
 * Progressive enhancement: every section is fully readable without this file.
 * Motion is only built when GSAP + ScrollTrigger are loaded and the visitor has
 * not asked for reduced motion. Lenis smooth scrolling is desktop-only.
 *
 * Integrates with safari-app.js through:
 *   - window.cineScrollTo(yOrElement)   used by existing anchor/scroll code
 *   - 'safari:userui' event             staff users hide the public sections
 *   - wrapped openModal/closeModal      pauses smooth scroll while a modal is open
 */
(function () {
    'use strict';

    const root = document.documentElement;
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isDesktop = window.matchMedia('(min-width: 769px) and (pointer: fine)').matches;
    const hasGsap = typeof window.gsap !== 'undefined' && typeof window.ScrollTrigger !== 'undefined';
    const motionOn = hasGsap && !reduceMotion;

    let lenis = null;
    let ctx = null;          // gsap.context holding every tween + ScrollTrigger
    let publicHidden = false;

    const navbar = document.querySelector('.navbar');
    const hero = document.querySelector('.cine-hero');
    const navOffset = () => (navbar ? navbar.offsetHeight : 72) + 8;

    // ------------------------------------------------------------------
    // Smooth scroll helper shared with safari-app.js
    // ------------------------------------------------------------------
    window.cineScrollTo = function (target) {
        const y = typeof target === 'number'
            ? target
            : target.getBoundingClientRect().top + window.pageYOffset - navOffset();
        if (lenis) {
            lenis.scrollTo(y, { duration: 1.4 });
        } else {
            window.scrollTo({ top: y, behavior: reduceMotion ? 'auto' : 'smooth' });
        }
    };

    // In-page links outside the navbar (the navbar already has its own handler)
    document.addEventListener('click', function (e) {
        const a = e.target.closest('a[href^="#"]');
        if (!a || a.closest('.navbar') || a.hasAttribute('onclick')) return;
        const id = a.getAttribute('href');
        if (id.length < 2) return;
        const el = document.getElementById(id.slice(1));
        if (!el || el.offsetParent === null) return;
        e.preventDefault();
        window.cineScrollTo(el);
        history.pushState(null, '', id);
    });

    // ------------------------------------------------------------------
    // Navbar: transparent over the hero, solid afterwards
    // ------------------------------------------------------------------
    function updateNav() {
        if (!navbar) return;
        const overHero = hero && !publicHidden && hero.offsetParent !== null &&
            window.pageYOffset < hero.offsetHeight - navbar.offsetHeight - 10;
        navbar.classList.toggle('is-over-hero', !!overHero);
    }
    window.addEventListener('scroll', updateNav, { passive: true });

    // ------------------------------------------------------------------
    // Live data for section 04 (read-only public endpoints)
    // ------------------------------------------------------------------
    const SLOT_LABELS = { MORNING: 'Morning', AFTERNOON: 'Afternoon', FULL_DAY: 'Full day' };

    function slotLabel(slot) {
        if (!slot) return '';
        const key = Object.keys(SLOT_LABELS).find(k => String(slot).toUpperCase().startsWith(k));
        return key ? SLOT_LABELS[key] : String(slot).replace(/_/g, ' ').toLowerCase();
    }

    function seatsLeft(s) {
        return Math.max(0, (Number(s.maxSlots) || 0) - (Number(s.bookedSlots) || 0));
    }

    function isBookable(s) {
        const status = String(s.status || 'AVAILABLE').toUpperCase();
        return !['CANCELLED', 'CLOSED', 'COMPLETED'].includes(status);
    }

    async function loadLiveData() {
        const statEls = document.querySelectorAll('[data-stat]');
        const list = document.getElementById('cineDepartures');
        if (!statEls.length) return;
        try {
            const [d, p, s] = await Promise.all([
                fetch('/api/public/destinations').then(r => r.ok ? r.json() : []),
                fetch('/api/public/packages').then(r => r.ok ? r.json() : []),
                fetch('/api/public/schedules').then(r => r.ok ? r.json() : [])
            ]);
            const today = new Date(); today.setHours(0, 0, 0, 0);
            const upcoming = (Array.isArray(s) ? s : [])
                .filter(x => x.scheduleDate && new Date(x.scheduleDate + 'T00:00:00') >= today && isBookable(x))
                .sort((a, b) => a.scheduleDate.localeCompare(b.scheduleDate));
            const values = {
                destinations: Array.isArray(d) ? d.length : 0,
                packages: Array.isArray(p) ? p.length : 0,
                trips: upcoming.length,
                seats: upcoming.reduce((sum, x) => sum + seatsLeft(x), 0)
            };
            statEls.forEach(el => countUp(el, values[el.dataset.stat] || 0));
            renderDepartures(list, upcoming.filter(x => seatsLeft(x) > 0).slice(0, 4));
        } catch (err) {
            statEls.forEach(el => { el.textContent = '–'; });
            if (list) list.innerHTML = '<li class="cine-dep cine-dep--empty">Live availability is unavailable right now. Browse the packages below.</li>';
        }
    }

    function countUp(el, value) {
        if (!motionOn) { el.textContent = value.toLocaleString(); return; }
        const obj = { v: 0 };
        gsap.to(obj, {
            v: value, duration: 1.6, ease: 'power3.out',
            scrollTrigger: { trigger: el, start: 'top 90%', once: true },
            onUpdate: () => { el.textContent = Math.round(obj.v).toLocaleString(); }
        });
    }

    function renderDepartures(list, items) {
        if (!list) return;
        list.innerHTML = '';
        if (!items.length) {
            list.innerHTML = '<li class="cine-dep cine-dep--empty">No open departures are scheduled yet. New trips are added regularly.</li>';
            return;
        }
        const fmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', weekday: 'short' });
        items.forEach(x => {
            const li = document.createElement('li');
            li.className = 'cine-dep';
            const date = document.createElement('span');
            date.className = 'cine-dep__date';
            date.textContent = fmt.format(new Date(x.scheduleDate + 'T00:00:00'));
            const name = document.createElement('span');
            name.className = 'cine-dep__name';
            name.textContent = (x.destination && x.destination.name) || 'Safari departure';
            const small = document.createElement('small');
            const pkg = (x.safariPackage && x.safariPackage.title) || '';
            small.textContent = [pkg, slotLabel(x.timeSlot)].filter(Boolean).join(' · ');
            name.appendChild(small);
            const seats = document.createElement('span');
            seats.className = 'cine-dep__seats';
            const n = seatsLeft(x);
            seats.textContent = n + (n === 1 ? ' seat left' : ' seats left');
            li.append(date, name, seats);
            list.appendChild(li);
        });
    }

    // ------------------------------------------------------------------
    // Text splitting: wraps each <br>-separated line in a mask
    // ------------------------------------------------------------------
    function splitLines(el) {
        if (el.dataset.splitDone) return el.querySelectorAll('.reveal-line > span');
        const lines = el.innerHTML.split(/<br\s*\/?>/i);
        el.setAttribute('aria-label', el.textContent.replace(/\s+/g, ' ').trim());
        el.innerHTML = lines.map(l => `<span class="reveal-line" aria-hidden="true"><span>${l.trim()}</span></span>`).join('');
        el.dataset.splitDone = '1';
        return el.querySelectorAll('.reveal-line > span');
    }

    // ------------------------------------------------------------------
    // Motion build / teardown
    // ------------------------------------------------------------------
    function buildMotion() {
        if (!motionOn || ctx) return;
        gsap.registerPlugin(ScrollTrigger);
        root.classList.add('cine-motion');

        if (isDesktop && typeof window.Lenis !== 'undefined' && !lenis) {
            lenis = new Lenis({ lerp: 0.11, wheelMultiplier: 0.95, smoothWheel: true });
            lenis.on('scroll', ScrollTrigger.update);
            gsap.ticker.add(t => { if (lenis) lenis.raf(t * 1000); });
            gsap.ticker.lagSmoothing(0);
        }

        ctx = gsap.context(() => {
            heroMotion();
            revealMotion();
            originMotion();
            fleetMotion();
            experienceMotion();
            bookMotion();
            finalMotion();
            backdropMotion();
            navSpy();
        });
        ScrollTrigger.refresh();
    }

    function destroyMotion() {
        if (ctx) { ctx.revert(); ctx = null; }
        root.classList.remove('cine-motion');
    }

    // 01 — hero intro + scroll-scrubbed camera push-in with depth layers
    function heroMotion() {
        if (!hero) return;
        const title = hero.querySelector('[data-split]');
        const lines = title ? splitLines(title) : [];
        const bg = hero.querySelector('[data-hero-bg]');
        const fg = hero.querySelector('[data-hero-fg]');
        const haze = hero.querySelector('[data-hero-haze]');
        const content = hero.querySelector('.cine-hero__content');

        // Intro animates wrappers/children; the scroll timeline below animates
        // different elements so the two never fight over the same property.
        gsap.timeline({ defaults: { ease: 'expo.out' } })
            .from(hero.querySelector('.cine-hero__media'), { scale: 1.1, duration: 2.6, ease: 'power2.out' }, 0)
            .from(lines, { yPercent: 110, duration: 1.4, stagger: 0.12 }, 0.25)
            .from(hero.querySelectorAll('[data-hero-in]'), { y: 24, autoAlpha: 0, duration: 1.2, stagger: 0.1 }, 0.55)
            .from('.cine-hero__meta', { autoAlpha: 0, duration: 1 }, 1.1);

        const layers = hero.querySelectorAll('[data-hero-video]');
        const push = gsap.timeline({
            defaults: { ease: 'none' },
            scrollTrigger: {
                trigger: hero,
                start: 'top top',
                end: isDesktop ? '+=160%' : '+=80%',
                pin: true,
                pinSpacing: true, // body is display:flex, where ScrollTrigger would default this to false
                scrub: isDesktop ? 0.6 : true,
                anticipatePin: 1,
                // Scroll position → moment in the generated hero clip
                onUpdate: self => heroMedia.draw(self.progress, self.isActive),
                onToggle: self => heroMedia.draw(self.progress, self.isActive)
            }
        });
        push.to(layers, { scale: 1.06 }, 0)
            .to(bg, { scale: isDesktop ? 1.32 : 1.18, yPercent: -2 }, 0)
            .to(fg, { scale: isDesktop ? 1.9 : 1.4, xPercent: -26, autoAlpha: 0 }, 0)
            .to(haze, { autoAlpha: 0.2, scale: 1.2 }, 0)
            .to(content, { y: -80, autoAlpha: 0, ease: 'power1.in' }, 0.15)
            .to('.cine-hero__shade', { opacity: 0.55 }, 0.2);
    }

    // ------------------------------------------------------------------
    // Hero clip. Landscape screens: the 1080p clip is fetched whole into
    // memory and follows the scroll position. Scrolling down *plays* the video
    // at a rate proportional to how far behind it is, so frames are decoded in
    // sequence (smooth 24 fps) and the motion eases to a stop with the scroll.
    // Scrolling up steps back with small seeks (browsers can't play in reverse).
    // Portrait phones: a 9:16 crop autoplays once and rests on its last frame.
    // ------------------------------------------------------------------
    const heroMedia = (function () {
        const video = document.querySelector('[data-hero-video]');
        const media = video && video.closest('.cine-hero__media');
        const END_PAD = 0.06;       // stop just before the end so play() never wraps to 0
        let scrub = false, ready = false, target = 0, onScreen = true, raf = 0;

        function reveal() {
            video.classList.add('is-ready');
            media.classList.add('has-frames');
        }

        function loop() { if (!raf) raf = requestAnimationFrame(tick); }

        function tick() {
            raf = 0;
            if (!ready) return;
            const end = video.duration - END_PAD;
            const t = Math.min(end, Math.max(0, target));
            const diff = t - video.currentTime;

            if (!onScreen) {                                   // pinned hero has scrolled away: just land there
                if (!video.paused) video.pause();
                if (Math.abs(diff) > 0.05 && !video.seeking) video.currentTime = t;
                return;
            }
            if (diff > 1.2 && !video.seeking) {                // far behind (e.g. very fast fling): jump most of the way
                video.currentTime = t - 0.3;
                loop();
            } else if (diff > 0.03 && video.currentTime < end) { // forward: play, speed follows the gap
                const rate = Math.min(8, Math.max(0.25, diff * 8));
                if (Math.abs(video.playbackRate - rate) > 0.05) video.playbackRate = rate;
                if (video.paused) video.play().catch(() => {});
                loop();
            } else if (diff < -0.05) {                         // backward: short eased seeks
                if (!video.paused) video.pause();
                if (!video.seeking) video.currentTime = video.currentTime + Math.max(diff * 0.35, -0.2);
                loop();
            } else if (!video.paused) {                        // caught up
                video.pause();
            }
        }

        function start() {
            if (!video || reduceMotion) return;                // poster only
            const saveData = navigator.connection && navigator.connection.saveData;
            if (saveData) return;
            video.muted = true;
            video.playsInline = true;
            scrub = window.innerWidth > 768 && window.innerWidth >= window.innerHeight && !!window.fetch;
            if (scrub) {
                video.addEventListener('loadeddata', () => { ready = true; video.pause(); reveal(); loop(); }, { once: true });
                // No animation frames in a hidden tab = nothing to rein the playback in, so pause
                document.addEventListener('visibilitychange', () => {
                    if (document.hidden) { if (!video.paused) video.pause(); } else loop();
                });
                const go = () => fetch(video.dataset.src)
                    .then(r => (r.ok ? r.blob() : Promise.reject(r.status)))
                    .then(blob => { video.src = URL.createObjectURL(blob); })
                    .catch(() => {});                          // poster + CSS push-in remain
                if (document.readyState === 'complete') go(); else window.addEventListener('load', go, { once: true });
            } else {
                video.addEventListener('playing', reveal, { once: true });
                video.src = video.dataset.srcPortrait;
                video.play().catch(() => {});                  // e.g. low-power mode: poster stays
            }
        }

        return {
            start,
            draw(progress, isActive) {
                if (!scrub || !video.duration) return;
                target = progress * video.duration;
                onScreen = isActive !== false;
                loop();
            }
        };
    })();

    // ------------------------------------------------------------------
    // Lazy looping clip (destination) + play-once clip (sunset)
    // ------------------------------------------------------------------
    function attachSource(video) {
        const s = video.querySelector('source[data-src]');
        if (s && !s.src) { s.src = s.dataset.src; video.load(); }
    }

    function initClips() {
        if (reduceMotion || !('IntersectionObserver' in window)) return; // posters only
        const loop = document.querySelector('[data-lazy-video]');
        if (loop) {
            loop.addEventListener('playing', () => loop.classList.add('is-playing'));
            new IntersectionObserver(entries => entries.forEach(e => {
                if (e.isIntersecting) { attachSource(loop); loop.play().catch(() => {}); }
                else if (!loop.paused) loop.pause();
            }), { rootMargin: '200px 0px' }).observe(loop);
        }
        const once = document.querySelector('[data-final-video]');
        if (once) {
            once.addEventListener('playing', () => once.classList.add('is-playing'));
            new IntersectionObserver(entries => entries.forEach(e => {
                if (e.isIntersecting && e.intersectionRatio > 0.5) {
                    attachSource(once);
                    if (once.ended) return;               // stays on its quiet final frame
                    once.play().catch(() => {});
                }
            }), { threshold: [0, 0.5, 0.75] }).observe(once);
            // warm the file shortly before the section arrives
            new IntersectionObserver((entries, obs) => entries.forEach(e => {
                if (e.isIntersecting) { attachSource(once); obs.disconnect(); }
            }), { rootMargin: '800px 0px' }).observe(once);
        }
    }

    // ------------------------------------------------------------------
    // 3D vehicle: <model-viewer> loads only when the section gets close
    // ------------------------------------------------------------------
    // Model axes (measured from the generated GLB): front/hood at +x, rear + spare wheel at -x,
    // roof at +y. Each spec step has a camera keyframe that frames its hotspot.
    const JEEP_HOTSPOTS = [
        { id: 0, label: 'Canvas canopy', position: '-0.15m 0.272m 0m', normal: '0m 1m 0m' },
        { id: 1, label: 'Tiered seating', position: '-0.12m 0.1m -0.06m', normal: '0m 1m 0m' },
        { id: 2, label: 'All-terrain 4×4', position: '-0.15m -0.19m -0.19m', normal: '0m 0m -1m' },
        { id: 3, label: 'Spare wheel', position: '-0.49m 0.06m 0m', normal: '-1m 0m 0m' }
    ];
    // [progress, theta, phi, radius%]
    const JEEP_KEYS = [[0, 125, 70, 92], [0.1, 125, 70, 88], [0.37, 185, 56, 78], [0.63, 235, 82, 72], [0.9, 295, 72, 82], [1, 305, 72, 84]];
    function jeepOrbit(p) {
        let k = 0;
        while (k < JEEP_KEYS.length - 2 && p > JEEP_KEYS[k + 1][0]) k++;
        const a = JEEP_KEYS[k], b = JEEP_KEYS[k + 1];
        let t = (p - a[0]) / (b[0] - a[0]);
        t = Math.min(1, Math.max(0, t));
        t = t * t * (3 - 2 * t); // smoothstep
        const v = i => (a[i] + (b[i] - a[i]) * t).toFixed(1);
        return `${v(1)}deg ${v(2)}deg ${v(3)}%`;
    }
    let fleetViewer = null;

    function initFleet() {
        const holder = document.querySelector('[data-fleet-viewer]');
        if (!holder || !('IntersectionObserver' in window)) return;
        new IntersectionObserver((entries, obs) => entries.forEach(e => {
            if (!e.isIntersecting) return;
            obs.disconnect();
            if (!customElements.get('model-viewer')) {
                const s = document.createElement('script');
                s.type = 'module';
                s.src = 'https://cdnjs.cloudflare.com/ajax/libs/model-viewer/3.5.0/model-viewer.min.js';
                document.head.appendChild(s);
            }
            const mv = document.createElement('model-viewer');
            mv.setAttribute('src', '/models/jeep.glb');
            mv.setAttribute('alt', '3D model of an open-top safari jeep. Drag to rotate.');
            mv.setAttribute('camera-orbit', jeepOrbit(0.1));
            mv.setAttribute('min-camera-orbit', 'auto 40deg auto');
            mv.setAttribute('max-camera-orbit', 'auto 88deg auto');
            mv.setAttribute('interaction-prompt', 'none');
            mv.setAttribute('shadow-intensity', '1.1');
            mv.setAttribute('shadow-softness', '0.9');
            mv.setAttribute('exposure', '1.1');
            mv.setAttribute('tone-mapping', 'aces');
            mv.setAttribute('environment-image', 'neutral');
            mv.setAttribute('camera-controls', '');
            mv.setAttribute('disable-zoom', '');
            mv.setAttribute('disable-pan', '');
            mv.setAttribute('touch-action', 'pan-y');
            if (!motionOn || !isDesktop) { mv.setAttribute('auto-rotate', ''); mv.setAttribute('auto-rotate-delay', '0'); mv.setAttribute('rotation-per-second', '14deg'); }
            JEEP_HOTSPOTS.forEach(h => {
                const b = document.createElement('div');
                b.className = 'cine-hotspot' + (!motionOn || !isDesktop || h.id === 0 ? ' is-active' : '');
                b.slot = 'hotspot-' + h.id;
                b.dataset.position = h.position;
                b.dataset.normal = h.normal;
                b.dataset.visibilityAttribute = 'visible';
                b.innerHTML = '<span></span>';
                b.firstChild.textContent = h.label;
                mv.appendChild(b);
            });
            const noBar = document.createElement('div');       // the poster covers loading; no progress line
            noBar.slot = 'progress-bar';
            mv.appendChild(noBar);
            mv.addEventListener('load', () => { mv.classList.add('is-loaded'); holder.classList.add('has-model'); });
            mv.addEventListener('error', () => mv.remove());   // poster stays
            holder.appendChild(mv);
            fleetViewer = mv;
        }), { rootMargin: '900px 0px' }).observe(holder);
    }

    // Pinned on desktop: scroll moves the camera around the jeep, one feature per spec step
    function fleetMotion() {
        const stage = document.querySelector('[data-fleet-stage]');
        if (!stage || !isDesktop) return;
        const specs = gsap.utils.toArray(stage.querySelectorAll('[data-spec]'));
        const n = specs.length;
        specs[0].classList.add('is-active');
        ScrollTrigger.create({
            trigger: stage,
            start: 'top top',
            end: '+=' + Math.round(window.innerHeight * 2.2),
            pin: true,
            pinSpacing: true,
            scrub: true,
            onUpdate: self => {
                const p = self.progress;
                const i = Math.min(n - 1, Math.floor(p * n));
                specs.forEach((li, k) => li.classList.toggle('is-active', k === i));
                if (fleetViewer) {
                    fleetViewer.cameraOrbit = jeepOrbit(p);
                    fleetViewer.querySelectorAll('.cine-hotspot').forEach((h, k) => h.classList.toggle('is-active', k === i));
                }
            }
        });
    }

    // Generic reveals for headings and blocks outside the hero
    function revealMotion() {
        document.querySelectorAll('.cine-section:not(.cine-hero):not(.cine-final) [data-split]').forEach(el => {
            gsap.from(splitLines(el), {
                yPercent: 110, duration: 1.3, ease: 'expo.out', stagger: 0.1,
                scrollTrigger: { trigger: el, start: 'top 85%' }
            });
        });
        gsap.utils.toArray('[data-reveal]').forEach(el => {
            gsap.from(el, {
                y: 36, autoAlpha: 0, duration: 1.1, ease: 'power3.out',
                scrollTrigger: { trigger: el, start: 'top 88%' }
            });
        });
        gsap.utils.toArray('#destinations .section-title, #safariPackages .section-title, .feedback-header').forEach(el => {
            gsap.from(el, { y: 30, autoAlpha: 0, duration: 1, ease: 'power3.out', scrollTrigger: { trigger: el, start: 'top 88%' } });
        });
    }

    // 02 — masked image reveal + inner parallax
    function originMotion() {
        const frame = document.querySelector('[data-mask-reveal]');
        if (!frame) return;
        gsap.fromTo(frame,
            { clipPath: 'inset(14% 10% 14% 10%)' },
            { clipPath: 'inset(0% 0% 0% 0%)', ease: 'none', scrollTrigger: { trigger: frame, start: 'top 90%', end: 'top 30%', scrub: true } });
        const img = frame.querySelector('[data-parallax]');
        if (img) {
            gsap.fromTo(img, { yPercent: -8 }, {
                yPercent: 8, ease: 'none',
                scrollTrigger: { trigger: frame, start: 'top bottom', end: 'bottom top', scrub: true }
            });
        }
    }

    // 03 — pinned chapters: vehicle → guide → journey → wild
    function experienceMotion() {
        const stage = document.querySelector('[data-exp-stage]');
        if (!stage) return;
        const slides = gsap.utils.toArray(stage.querySelectorAll('.cine-exp__slide'));
        const chapters = gsap.utils.toArray(stage.querySelectorAll('.cine-exp__chapter'));
        const dots = gsap.utils.toArray(stage.querySelectorAll('.cine-exp__progress li'));
        const n = slides.length;

        gsap.set(slides, { autoAlpha: 0 });
        gsap.set(chapters, { autoAlpha: 0, y: 40 });
        gsap.set(slides[0], { autoAlpha: 1 });
        gsap.set(chapters[0], { autoAlpha: 1, y: 0 });

        const tl = gsap.timeline({
            defaults: { ease: 'none' },
            scrollTrigger: {
                trigger: stage,
                start: 'top top',
                end: () => '+=' + window.innerHeight * (isDesktop ? n : n * 0.7),
                pin: true,
                pinSpacing: true, // body is display:flex, where ScrollTrigger would default this to false
                scrub: isDesktop ? 0.5 : true,
                anticipatePin: 1,
                onUpdate: self => {
                    const i = Math.min(n - 1, Math.floor(self.progress * n));
                    dots.forEach((d, k) => d.classList.toggle('is-active', k === i));
                }
            }
        });

        slides.forEach((slide, i) => {
            const img = slide.querySelector('img');
            // slow camera drift inside each chapter
            tl.fromTo(img, { scale: 1.12, xPercent: i % 2 ? 2 : -2 }, { scale: 1, xPercent: 0, duration: 1 }, i);
            if (i === 0) return;
            tl.to(slides[i - 1], { autoAlpha: 0, duration: 0.3 }, i - 0.15)
              .to(slide, { autoAlpha: 1, duration: 0.3 }, i - 0.15)
              .to(chapters[i - 1], { autoAlpha: 0, y: -40, duration: 0.25 }, i - 0.2)
              .to(chapters[i], { autoAlpha: 1, y: 0, duration: 0.3 }, i - 0.05);
        });
    }

    // 05 — background drift
    function bookMotion() {
        const bg = document.querySelector('[data-parallax-bg]');
        if (!bg) return;
        gsap.fromTo(bg, { yPercent: -8, scale: 1.05 }, {
            yPercent: 8, scale: 1, ease: 'none',
            scrollTrigger: { trigger: bg.parentElement, start: 'top bottom', end: 'bottom top', scrub: true }
        });
    }

    // 06 — pinned sunset, image settles while the title rises
    function finalMotion() {
        const sec = document.querySelector('.cine-final');
        if (!sec) return;
        const img = sec.querySelector('[data-final-img]');
        const title = sec.querySelector('[data-split]');
        const tl = gsap.timeline({
            defaults: { ease: 'none' },
            scrollTrigger: {
                trigger: sec,
                start: 'top top',
                end: isDesktop ? '+=80%' : '+=40%',
                pin: true,
                pinSpacing: true, // body is display:flex, where ScrollTrigger would default this to false
                scrub: isDesktop ? 0.6 : true,
                anticipatePin: 1
            }
        });
        gsap.fromTo(img, { scale: 1.25, yPercent: -4 }, {
            scale: 1.08, yPercent: 0, ease: 'none',
            scrollTrigger: { trigger: sec, start: 'top bottom', end: 'top top', scrub: true }
        });
        const fade = sec.querySelector('.cine-final__fade');
        if (fade) gsap.fromTo(fade, { opacity: 1 }, { opacity: 0, ease: 'none', scrollTrigger: { trigger: sec, start: 'top 70%', end: 'top top', scrub: true } });
        tl.to(img, { scale: 1, duration: 1 }, 0)
          .from(splitLines(title), { yPercent: 110, stagger: 0.12, duration: 0.5, ease: 'power3.out' }, 0)
          .from(sec.querySelectorAll('[data-final-in]'), { autoAlpha: 0, y: 30, stagger: 0.15, duration: 0.4, ease: 'power2.out' }, 0.35);
    }

    // Scenic backdrops drift slowly against the content (the image is sticky,
    // so the movement stays subtle however tall the section is)
    function backdropMotion() {
        gsap.utils.toArray('[data-backdrop]').forEach(img => {
            const section = img.closest('section');
            gsap.fromTo(img, { yPercent: -3, scale: 1.1 }, {
                yPercent: 3, scale: 1.1, ease: 'none',
                scrollTrigger: { trigger: section, start: 'top bottom', end: 'bottom top', scrub: true }
            });
        });
    }

    // Active nav link while scrolling
    function navSpy() {
        const links = document.querySelectorAll('.navbar .nav-link[href^="#"]');
        links.forEach(link => {
            const target = document.querySelector(link.getAttribute('href'));
            if (!target) return;
            ScrollTrigger.create({
                trigger: target,
                start: 'top 45%',
                end: 'bottom 45%',
                onToggle: self => link.classList.toggle('is-active', self.isActive)
            });
        });
    }

    // ------------------------------------------------------------------
    // Subtle magnetic buttons (desktop, fine pointer, motion allowed)
    // ------------------------------------------------------------------
    function initMagnetic() {
        if (!isDesktop || reduceMotion) return;
        document.querySelectorAll('[data-magnetic]').forEach(btn => {
            btn.addEventListener('pointermove', e => {
                const r = btn.getBoundingClientRect();
                const dx = (e.clientX - r.left - r.width / 2) / r.width;
                const dy = (e.clientY - r.top - r.height / 2) / r.height;
                btn.style.setProperty('--btn-x', (dx * 8).toFixed(1) + 'px');
                btn.style.setProperty('--btn-y', (dy * 6).toFixed(1) + 'px');
            });
            btn.addEventListener('pointerleave', () => {
                btn.style.setProperty('--btn-x', '0px');
                btn.style.setProperty('--btn-y', '0px');
            });
        });
    }

    // ------------------------------------------------------------------
    // Integration with the existing application
    // ------------------------------------------------------------------
    function wrapModals() {
        document.querySelectorAll('.modal-overlay, .modal-dialog, .table-responsive').forEach(el => el.setAttribute('data-lenis-prevent', ''));
        ['openModal', 'closeModal'].forEach(name => {
            const original = window[name];
            if (typeof original !== 'function') return;
            window[name] = function () {
                const result = original.apply(this, arguments);
                const sync = () => {
                    if (!lenis) return;
                    document.querySelector('.modal-overlay.show:not(.closing)') ? lenis.stop() : lenis.start();
                };
                sync();
                setTimeout(sync, 200); // closeModal removes .show after its 150ms animation
                return result;
            };
        });
    }

    // Staff users see dashboards only — tear motion down so pins leave no gaps
    let dashboardJumpDone = false;
    document.addEventListener('safari:userui', e => {
        publicHidden = !!(e.detail && e.detail.hidePublic);
        if (publicHidden) {
            destroyMotion();
            // reverting pins restores their pre-pin inline styles, so re-hide
            document.querySelectorAll('.cinema-public').forEach(el => { el.style.display = 'none'; });
        } else {
            buildMotion();
        }
        updateNav();
        // Post-login redirects land on /#dashboard, /#customer-dashboard, etc.
        // The dashboard now sits below the pinned hero, so take the user to it.
        const dash = document.getElementById('dashboardSection');
        if (!dashboardJumpDone && /dashboard/i.test(location.hash) && dash && dash.style.display !== 'none') {
            dashboardJumpDone = true;
            setTimeout(() => {
                if (ctx) ScrollTrigger.refresh();
                const y = dash.getBoundingClientRect().top + window.pageYOffset - navOffset();
                if (lenis) lenis.scrollTo(y, { immediate: true }); else window.scrollTo(0, y);
            }, 150);
        }
    });

    // Layout changes (cards rendered, dashboards opened) → re-measure triggers
    let refreshTimer = null;
    if (hasGsap && 'ResizeObserver' in window) {
        let lastH = document.body.scrollHeight;
        new ResizeObserver(() => {
            const h = document.body.scrollHeight;
            if (Math.abs(h - lastH) < 2) return;
            lastH = h;
            clearTimeout(refreshTimer);
            refreshTimer = setTimeout(() => { if (ctx) ScrollTrigger.refresh(); }, 200);
        }).observe(document.body);
    }

    function init() {
        root.classList.add('cine-ready');
        wrapModals();
        initMagnetic();
        buildMotion();
        heroMedia.start();
        initClips();
        initFleet();
        loadLiveData();
        updateNav();
        // Deep links such as /#safariPackages after pin spacing is known
        if (location.hash.length > 1) {
            const el = document.getElementById(location.hash.slice(1));
            if (el) setTimeout(() => window.cineScrollTo(el), 400);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
