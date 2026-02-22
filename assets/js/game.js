// ============================================
// ZZGOD — Game Screen
// ============================================

const canvas = document.getElementById('world-canvas');
const ctx    = canvas.getContext('2d');

// Disable smoothing for pixel-perfect rendering
ctx.imageSmoothingEnabled = false;


// ---- Rendering ---- //

const BOARD_COLS = 8;   // 64 blocks arranged 8x8
const BLOCK_SIZE = 48;  // 384 / 8 = 48px per block on canvas

function render() {
    ctx.fillStyle = '#000000';
    ctx.fillRect(0, 0, 384, 384);

    renderBoard();
    renderMeeples();
}

function renderBoard() {
    const board = documentData.board;
    if (!board) return;

    for (let i = 0; i < board.length; i++) {
        const block = board[i];
        const col   = i % BOARD_COLS;
        const row   = Math.floor(i / BOARD_COLS);
        const x     = col * BLOCK_SIZE;
        const y     = row * BLOCK_SIZE;

        ctx.fillStyle = block.color || '#000000';
        ctx.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);

        if (block.grid && block.grid.length > 0) {
            renderBlockGrid(block, x, y);
        }
    }
}

function renderBlockGrid(block, ox, oy) {
    const grid    = block.grid;
    const palette = documentData.palette?.colors || {};
    const cellSize = BLOCK_SIZE / 8;

    for (let i = 0; i < grid.length && i < 64; i++) {
        const colorIdx = grid[i];
        if (colorIdx === 0 || colorIdx === undefined) continue;

        const cx = (i % 8) * cellSize;
        const cy = Math.floor(i / 8) * cellSize;

        ctx.fillStyle = palette[colorIdx] || block.color || '#111';
        ctx.fillRect(ox + cx, oy + cy, cellSize, cellSize);
    }
}

function renderMeeples() {
    const meeples = documentData.meeples;
    if (!meeples || meeples.length === 0) return;

    for (const m of meeples) {
        const [sw, sh] = (m.size || '1x1').split('x').map(Number);

        ctx.fillStyle = m.color || '#FF00FF';
        ctx.fillRect(m.x, 384 - m.y - sh, sw, sh);
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
    const scaleX = 384 / rect.width;
    const scaleY = 384 / rect.height;
    const px = Math.floor((e.clientX - rect.left) * scaleX);
    const py = Math.floor((e.clientY - rect.top) * scaleY);
    coordInfo.textContent = `${px},${py}`;
});

canvas.addEventListener('mouseleave', () => {
    if (coordInfo) coordInfo.textContent = '';
});
