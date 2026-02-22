// ============================================
// ZZGOD — Game Screen
// ============================================

const canvas = document.getElementById('world-canvas');
const ctx    = canvas.getContext('2d');

// Disable smoothing for pixel-perfect rendering
ctx.imageSmoothingEnabled = false;

// Currently selected block for placement (set by BlockEditor)
window.selectedBlock = -1;


// ---- CRT Knob Initialization (apply stored values before first paint) ---- //

(function() {
    const vp = document.getElementById('game-viewport');
    if (!vp) return;
    const crt = parseInt(localStorage.getItem('zzgod-crt-intensity') ?? '50', 10);
    const scan = parseInt(localStorage.getItem('zzgod-scanline-speed') ?? '50', 10);
    vp.style.setProperty('--crt-intensity', (crt / 100).toString());
    if (scan === 0) {
        vp.style.setProperty('--scanline-speed', '8s');
        vp.style.setProperty('--scanline-paused', 'paused');
    } else {
        const dur = 0.5 + (1 - scan / 100) * 15.5;
        vp.style.setProperty('--scanline-speed', dur + 's');
        vp.style.setProperty('--scanline-paused', 'running');
    }
})();


// ---- Editable World Name ---- //

const worldNameEl = document.querySelector('.world-name');
if (worldNameEl) {
    worldNameEl.addEventListener('click', () => {
        if (documentData['player-status'] !== 'GOD') return;
        worldNameEl.contentEditable = 'true';
        worldNameEl.focus();
    });

    worldNameEl.addEventListener('blur', () => {
        worldNameEl.contentEditable = 'false';
        const name = worldNameEl.textContent.trim();
        if (!name) return;
        const be = document.querySelector('block-editor');
        if (be && be.renameWorld) be.renameWorld({ name: name });
    });

    worldNameEl.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') { e.preventDefault(); worldNameEl.blur(); }
        if (e.key === 'Escape') {
            worldNameEl.textContent = documentData.name || 'New World';
            worldNameEl.blur();
        }
    });
}


// ---- Default Palette ---- //

const DEFAULT_PALETTE = {
    '0': '#000000', '1': '#1a1a2e', '2': '#16213e', '3': '#0f3460',
    '4': '#39ff14', '5': '#00ffff', '6': '#bf00ff', '7': '#ff006e',
    '8': '#ff6600', '9': '#ffff00', '10': '#00ff88', '11': '#4444ff',
    '12': '#ff4444', '13': '#888888', '14': '#cccccc', '15': '#ffffff'
};

function getPaletteColor(index) {
    const key = String(index);
    const palette = documentData.palette?.colors || {};
    return palette[key] || DEFAULT_PALETTE[key] || '#000';
}


// ---- Rendering ---- //

const CANVAS_PX = 288;  // 36 * 8
const MAP_SIZE  = 36;   // 36x36 tile grid
const TILE_SIZE = 8;    // 288 / 36 = 8px per tile on canvas

let crosshair = { active: false, x: 0, y: 0 };

canvas.addEventListener('mousemove', (e) => {
    const rect = canvas.getBoundingClientRect();
    crosshair.x = (e.clientX - rect.left) / rect.width * CANVAS_PX;
    crosshair.y = (e.clientY - rect.top) / rect.height * CANVAS_PX;
    crosshair.active = true;
});

canvas.addEventListener('mouseleave', () => {
    crosshair.active = false;
    if (meepleTooltip) meepleTooltip.style.display = 'none';
});

// ---- Status Change Watcher ---- //

let lastStatus = documentData['player-status'];
const viewport = document.getElementById('game-viewport');
const queueInfo = document.querySelector('.queue-info');

function checkStatusChange() {
    const status = documentData['player-status'];
    if (status === lastStatus) return;
    const wasGod = lastStatus === 'GOD';
    lastStatus = status;

    if (status === 'GOD' && !wasGod) {
        // Becoming god — severe glitch
        viewport.classList.remove('glitch-warning', 'glitch-intense');
        viewport.classList.add('glitch-onset');
        setTimeout(() => viewport.classList.remove('glitch-onset'), 800);
        if (queueInfo) queueInfo.textContent = 'You are GOD. Shape the world.';
    }

    if (status !== 'GOD') {
        viewport.classList.remove('glitch-warning', 'glitch-intense', 'glitch-onset');
        if (queueInfo) queueInfo.textContent = 'Watching the void...';
        document.querySelectorAll('.panel').forEach(p => {
            if (p.style.display !== 'none') closePanel(p);
        });
    }
}

function updateGlitch() {
    if (documentData['player-status'] !== 'GOD') return;
    const t = documentData.timer;
    if (t != null && t <= 5) {
        viewport.classList.add('glitch-warning');
        if (t <= 2) {
            viewport.classList.add('glitch-intense');
        } else {
            viewport.classList.remove('glitch-intense');
        }
    } else {
        viewport.classList.remove('glitch-warning', 'glitch-intense');
    }
}


function render() {
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, CANVAS_PX, CANVAS_PX);

    renderTiles();
    renderMeeples();
    renderCrosshair();
    updateMeepleTooltip();
    updatePopInfo();
    updateTimer();
    checkStatusChange();
    updateGlitch();
}

function renderCrosshair() {
    if (!crosshair.active) return;
    const cx = Math.floor(crosshair.x) + 0.5;
    const cy = Math.floor(crosshair.y) + 0.5;
    ctx.strokeStyle = 'rgba(0, 255, 255, 0.5)';
    ctx.lineWidth = 1;
    const len = 8;
    ctx.beginPath();
    ctx.moveTo(cx, cy - len);
    ctx.lineTo(cx, cy + len);
    ctx.moveTo(cx - len, cy);
    ctx.lineTo(cx + len, cy);
    ctx.stroke();
}

function renderTiles() {
    const tiles = documentData.tiles;
    const board = documentData.board;
    if (!board) return;

    // If no tile map yet, fall back to rendering block definitions in a grid
    if (!tiles || tiles.length === 0) {
        for (let i = 0; i < board.length; i++) {
            const block = board[i];
            const col = i % 8;
            const row = Math.floor(i / 8);
            const x = col * 36;
            const y = row * 36;
            ctx.fillStyle = block.color || '#000000';
            ctx.fillRect(x, y, 36, 36);
            if (block.grid && block.grid.length > 0) {
                renderBlockPixels(block, x, y, 4);
            }
        }
        return;
    }

    // Render from the 36x36 tile map
    const total = MAP_SIZE * MAP_SIZE;
    for (let i = 0; i < tiles.length && i < total; i++) {
        const blockIdx = tiles[i];
        const block = board[blockIdx];
        if (!block) continue;

        const col = i % MAP_SIZE;
        const row = Math.floor(i / MAP_SIZE);
        const x = col * TILE_SIZE;
        const y = row * TILE_SIZE;

        // Base color fill
        ctx.fillStyle = block.color || '#000000';
        ctx.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Pixel detail from block grid
        if (block.grid && block.grid.length > 0) {
            renderBlockPixels(block, x, y, 1);
        }
    }
}

function renderBlockPixels(block, ox, oy, scale) {
    const grid = block.grid;
    for (let i = 0; i < grid.length && i < 64; i++) {
        const colorIdx = grid[i];
        if (colorIdx === 0 || colorIdx === undefined) continue;

        const cx = (i % 8) * scale;
        const cy = Math.floor(i / 8) * scale;

        ctx.fillStyle = getPaletteColor(colorIdx);
        ctx.fillRect(ox + cx, oy + cy, scale, scale);
    }
}

function renderMeeples() {
    const meeples = documentData.meeples;
    if (!meeples || meeples.length === 0) return;

    for (const m of meeples) {
        const [sw, sh] = (m.size || '1x1').split('x').map(Number);
        // Shadow pixel below for contrast on bright blocks
        ctx.fillStyle = 'rgba(0,0,0,0.45)';
        ctx.fillRect(m.x, CANVAS_PX - m.y - sh + 1, sw, sh);
        ctx.fillStyle = m.color || '#FF00FF';
        ctx.fillRect(m.x, CANVAS_PX - m.y - sh, sw, sh);
    }
}


// ---- Meeple Tooltip ---- //

const meepleTooltip = document.getElementById('meeple-tooltip');
const HOVER_PAD = 4; // extra canvas-pixels around meeple for easier hovering

function getMoodLabel(mood) {
    if (mood < 16)  return 'dormant';
    if (mood < 36)  return 'calm';
    if (mood < 56)  return 'content';
    if (mood < 76)  return 'restless';
    if (mood < 96)  return 'excited';
    return 'frenzied';
}

function getAge(birth) {
    if (!birth) return '???';
    const years = (Date.now() - birth) / 86400000; // 1 day = 1 year
    if (years < 0.1) return '<0.1y';
    return years.toFixed(1) + 'y';
}

function updateMeepleTooltip() {
    if (!meepleTooltip || !crosshair.active) {
        if (meepleTooltip) meepleTooltip.style.display = 'none';
        return;
    }
    const meeples = documentData.meeples;
    if (!meeples || meeples.length === 0) {
        meepleTooltip.style.display = 'none';
        return;
    }

    const cx = crosshair.x;
    const cy = crosshair.y;
    let hit = null;

    for (const m of meeples) {
        const [sw, sh] = (m.size || '1x1').split('x').map(Number);
        const mx = m.x;
        const my = CANVAS_PX - m.y - sh; // canvas top-left y
        if (cx >= mx - HOVER_PAD && cx <= mx + sw + HOVER_PAD &&
            cy >= my - HOVER_PAD && cy <= my + sh + HOVER_PAD) {
            hit = m;
            break;
        }
    }

    if (!hit) {
        meepleTooltip.style.display = 'none';
        return;
    }

    const [sw, sh] = (hit.size || '1x1').split('x').map(Number);
    const vpRect = document.getElementById('game-viewport').getBoundingClientRect();
    const scale = vpRect.width / CANVAS_PX;
    const tipX = vpRect.left + (hit.x + sw) * scale + 8;
    const tipY = vpRect.top + (CANVAS_PX - hit.y - sh) * scale - 4;

    meepleTooltip.style.display = 'block';
    meepleTooltip.style.left = tipX + 'px';
    meepleTooltip.style.top = tipY + 'px';
    meepleTooltip.innerHTML =
        '<span class="tt-name">' + (hit.name || '???') + '</span>' +
        '<span class="tt-mood">' + getMoodLabel(hit.mood) + '</span>' +
        '<span class="tt-age">' + getAge(hit.birth) + '</span>';
}


// ---- Meeple Click (pet) ---- //

function getMeepleAtCursor() {
    const meeples = documentData.meeples;
    if (!meeples || !crosshair.active) return null;
    const cx = crosshair.x;
    const cy = crosshair.y;
    for (let i = 0; i < meeples.length; i++) {
        const m = meeples[i];
        const [sw, sh] = (m.size || '1x1').split('x').map(Number);
        const mx = m.x;
        const my = CANVAS_PX - m.y - sh;
        if (cx >= mx - HOVER_PAD && cx <= mx + sw + HOVER_PAD &&
            cy >= my - HOVER_PAD && cy <= my + sh + HOVER_PAD) {
            return i;
        }
    }
    return null;
}

canvas.addEventListener('click', (e) => {
    const idx = getMeepleAtCursor();
    if (idx === null) return;
    const m = documentData.meeples[idx];
    m.mood = Math.min(128, m.mood + 15);
    const be = document.querySelector('block-editor');
    if (be && be.petMeeple) be.petMeeple({ index: idx });
});


// ---- Tile Placement ---- //

let placing = false;
let placedTiles = {};  // batch: { tileIndex: blockIndex }

canvas.addEventListener('mousedown', (e) => {
    if (e.button !== 0) return;
    if (getMeepleAtCursor() !== null) return; // don't place tiles on meeple click
    if (documentData['player-status'] !== 'GOD') return;
    if (window.selectedBlock < 0) return;
    placing = true;
    placeTileAt(e);
});

canvas.addEventListener('mousemove', (e) => {
    if (placing) placeTileAt(e);
});

window.addEventListener('mouseup', () => {
    if (placing) {
        placing = false;
        flushPlacement();
    }
});

function placeTileAt(e) {
    const rect = canvas.getBoundingClientRect();
    const x = Math.floor((e.clientX - rect.left) / rect.width * MAP_SIZE);
    const y = Math.floor((e.clientY - rect.top) / rect.height * MAP_SIZE);
    if (x < 0 || x >= MAP_SIZE || y < 0 || y >= MAP_SIZE) return;

    const tileIndex = y * MAP_SIZE + x;
    const blockIndex = window.selectedBlock;

    // Update locally for immediate feedback
    if (documentData.tiles) {
        documentData.tiles[tileIndex] = blockIndex;
    }

    // Batch for server
    placedTiles[tileIndex] = blockIndex;
}

function flushPlacement() {
    const batch = { ...placedTiles };
    placedTiles = {};
    if (Object.keys(batch).length === 0) return;

    // Find the block-editor element and call its placeTiles @Bind
    const be = document.querySelector('block-editor');
    if (be && be.placeTiles) {
        console.log('[GAME] Placing ' + Object.keys(batch).length + ' tiles');
        be.placeTiles({ tiles: batch });
    }
}


// ---- Game Loop ---- //

function loop() {
    render();
    requestAnimationFrame(loop);
}

requestAnimationFrame(loop);


// ---- Coordinate Display ---- //

const coordInfo = document.querySelector('.coord-info');

canvas.addEventListener('mousemove', (e) => {
    if (!coordInfo) return;
    const rect   = canvas.getBoundingClientRect();
    const scaleX = MAP_SIZE / rect.width;
    const scaleY = MAP_SIZE / rect.height;
    const tx = Math.floor((e.clientX - rect.left) * scaleX);
    const ty = Math.floor((e.clientY - rect.top) * scaleY);
    coordInfo.textContent = `${tx},${ty}`;
});

canvas.addEventListener('mouseleave', () => {
    if (coordInfo) coordInfo.textContent = '';
});


// ---- Timer Display ---- //

const timerEl = document.querySelector('.game-timer');
let lastTimer = -1;

function updateTimer() {
    if (!timerEl) return;
    const t = documentData.timer;
    if (t === lastTimer) return;
    lastTimer = t;
    const secs = (t != null) ? t : '--';
    timerEl.textContent = String(secs).padStart(2, '0');
}


// ---- Population Display ---- //

const popInfo = document.querySelector('.pop-info');
let lastPopStr = '';

function updatePopInfo() {
    if (!popInfo) return;
    const pop = documentData.meeples?.length || 0;
    const total = documentData.totalBorn || pop;
    const str = 'POP ' + pop + ' / ' + total;
    if (str !== lastPopStr) {
        popInfo.textContent = str;
        lastPopStr = str;
    }
}


// ---- Panel Toggle & Drag ---- //

const PANEL_MARGIN = 20;
const PANEL_STORAGE_KEY = 'zzgod-panels';

function loadPanelState() {
    try { return JSON.parse(localStorage.getItem(PANEL_STORAGE_KEY)) || {}; }
    catch { return {}; }
}

function savePanelState(id, data) {
    const state = loadPanelState();
    state[id] = { ...state[id], ...data };
    localStorage.setItem(PANEL_STORAGE_KEY, JSON.stringify(state));
}

function clampPanel(panel) {
    const rect = panel.getBoundingClientRect();
    const maxX = window.innerWidth - rect.width - PANEL_MARGIN;
    const maxY = window.innerHeight - rect.height - PANEL_MARGIN;
    panel.style.left = Math.max(PANEL_MARGIN, Math.min(parseFloat(panel.style.left) || 0, maxX)) + 'px';
    panel.style.top  = Math.max(PANEL_MARGIN, Math.min(parseFloat(panel.style.top)  || 0, maxY)) + 'px';
}

function openPanel(panel, btn) {
    panel.style.display = '';
    btn.classList.add('active');
    clampPanel(panel);
    savePanelState(panel.id, { open: true });
}

function closePanel(panel) {
    panel.style.display = 'none';
    const btn = document.querySelector(`.tool-btn[data-panel="${panel.id}"]`);
    if (btn) btn.classList.remove('active');
    savePanelState(panel.id, { open: false });
}

function positionPanel(panel) {
    const saved = loadPanelState()[panel.id];
    if (saved && saved.left != null) {
        panel.style.left = saved.left + 'px';
        panel.style.top = saved.top + 'px';
    } else {
        const vp = document.getElementById('game-viewport').getBoundingClientRect();
        const rect = panel.getBoundingClientRect();
        if (panel.id === 'panel-palette') {
            panel.style.left = ((window.innerWidth - rect.width) / 2) + 'px';
            panel.style.top = (vp.top + 20) + 'px';
        } else if (panel.id === 'panel-architect') {
            panel.style.left = (vp.left + 20) + 'px';
            panel.style.top = (vp.bottom + 20) + 'px';
        }
    }
    panel.dataset.positioned = '1';
}

// Restore saved state on load
document.querySelectorAll('.panel').forEach(panel => {
    const saved = loadPanelState()[panel.id];
    if (saved?.open) {
        panel.style.display = '';
        positionPanel(panel);
        clampPanel(panel);
        const btn = document.querySelector(`.tool-btn[data-panel="${panel.id}"]`);
        if (btn) btn.classList.add('active');
    }
});

document.querySelectorAll('.tool-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const panel = document.getElementById(btn.dataset.panel);
        if (!panel) return;
        const opening = panel.style.display === 'none';
        if (opening) {
            if (!panel.dataset.positioned) positionPanel(panel);
            openPanel(panel, btn);
        } else {
            closePanel(panel);
        }
    });
});

document.querySelectorAll('.panel-close').forEach(btn => {
    btn.addEventListener('click', () => {
        const panel = document.getElementById(btn.dataset.panel);
        if (panel) closePanel(panel);
    });
});

window.addEventListener('resize', () => {
    document.querySelectorAll('.panel').forEach(panel => {
        if (panel.style.display !== 'none') clampPanel(panel);
    });
});

(function initPanelDrag() {
    let dragging = null;
    let offsetX = 0, offsetY = 0;

    document.addEventListener('mousedown', (e) => {
        const header = e.target.closest('.panel-header');
        if (!header || e.target.closest('.panel-close')) return;
        const panel = header.closest('.panel');
        dragging = panel;
        const rect = panel.getBoundingClientRect();
        offsetX = e.clientX - rect.left;
        offsetY = e.clientY - rect.top;
        e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
        if (!dragging) return;
        dragging.style.left = (e.clientX - offsetX) + 'px';
        dragging.style.top  = (e.clientY - offsetY) + 'px';
        clampPanel(dragging);
    });

    document.addEventListener('mouseup', () => {
        if (!dragging) return;
        savePanelState(dragging.id, {
            left: parseFloat(dragging.style.left),
            top: parseFloat(dragging.style.top),
        });
        dragging = null;
    });
})();


// ---- Share Dialog ---- //

(function() {
    const shareBtn = document.getElementById('share-btn');
    const overlay = document.getElementById('share-overlay');
    const urlInput = document.getElementById('share-url');
    const copyBtn = document.getElementById('share-copy');
    const closeBtn = document.getElementById('share-close');
    if (!shareBtn || !overlay) return;

    const worldId = window.location.pathname.split('/').pop();
    const shareUrl = 'https://dev.aufdemdev.spaceport.host/w/' + worldId;

    shareBtn.addEventListener('click', () => {
        urlInput.value = shareUrl;
        overlay.style.display = 'flex';
        urlInput.select();
    });

    copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(shareUrl);
        copyBtn.textContent = 'COPIED';
        setTimeout(() => { copyBtn.textContent = 'COPY'; }, 1500);
    });

    closeBtn.addEventListener('click', () => {
        overlay.style.display = 'none';
    });

    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.style.display = 'none';
    });
})();
