import data.World
import spaceport.launchpad.element.Bind
import spaceport.launchpad.element.CSS
import spaceport.launchpad.element.Element
import spaceport.launchpad.element.Javascript

class BlockEditor implements Element {

    World world

    static final DEFAULT_COLORS = [
        '0': '#000000', '1': '#1a1a2e', '2': '#16213e', '3': '#0f3460',
        '4': '#39ff14', '5': '#00ffff', '6': '#bf00ff', '7': '#ff006e',
        '8': '#ff6600', '9': '#ffff00', '10': '#00ff88', '11': '#4444ff',
        '12': '#ff4444', '13': '#888888', '14': '#cccccc', '15': '#ffffff'
    ]


    String prerender(String body, Map attributes) {
        try {
            world = World.getIfExists(attributes.'world-id'.clean().slugify(), 'worlds')

            // Block selector thumbnails (8x8 grid of the 64 blocks)
            def thumbs = new StringBuilder()
            for (int i = 0; i < world.board.size(); i++) {
                def block = world.board[i]
                thumbs.append(
                    "<div class='be-thumb' data-index='${i}' style='background-color:${block.color}' title='Block ${i}'></div>"
                )
            }

            // Color chips (16 palette colors for drawing)
            def chips = new StringBuilder()
            def colors = world?.palette?.colors ?: [:]
            for (int i = 0; i < 16; i++) {
                def key = String.valueOf(i)
                def color = colors[key] ?: DEFAULT_COLORS[key]
                chips.append(
                    "<div class='be-chip${i == 0 ? " active" : ""}' data-index='${i}' style='background-color:${color}' title='Color ${i}'></div>"
                )
            }

            return "<div class='be-panel'>" +
                       "<div class='be-label'>ARCHITECT</div>" +
                       "<div class='be-body'>" +
                           "<div class='be-selector'>${thumbs}</div>" +
                           "<div class='be-workspace'>" +
                               "<canvas class='be-canvas' width='8' height='8'></canvas>" +
                               "<div class='be-info'>" +
                                   "<span class='be-active-label'>BLOCK</span>" +
                                   "<span class='be-active-index'>--</span>" +
                               "</div>" +
                           "</div>" +
                           "<div class='be-colors'>${chips}</div>" +
                       "</div>" +
                   "</div>"
        } catch (Exception e) {
            e.printStackTrace()
            return "<div class='be-panel'>Block editor error: ${e.message}</div>"
        }
    }

    @Javascript
    def constructed = /* language=javascript */ """
        function(element) {
            const canvas = element.querySelector('.be-canvas')
            const ctx = canvas.getContext('2d')
            ctx.imageSmoothingEnabled = false

            let selectedBlock = -1
            let selectedColor = 0
            let currentGrid = new Array(64).fill(0)
            let saveTimer = null

            // ---- Thumbnail Rendering ---- //

            const thumbCanvas = document.createElement('canvas')
            thumbCanvas.width = 8
            thumbCanvas.height = 8
            const thumbCtx = thumbCanvas.getContext('2d')
            thumbCtx.imageSmoothingEnabled = false

            function renderThumb(index) {
                const board = documentData.board
                if (!board || !board[index]) return
                const block = board[index]
                const grid = block.grid && block.grid.length === 64 ? block.grid : new Array(64).fill(0)
                const baseColor = block.color || '#000000'
                for (let i = 0; i < 64; i++) {
                    const ci = grid[i]
                    thumbCtx.fillStyle = ci > 0 ? getColor(ci) : baseColor
                    thumbCtx.fillRect(i % 8, Math.floor(i / 8), 1, 1)
                }
                const thumb = element.querySelector(".be-thumb[data-index='" + index + "']")
                if (thumb) {
                    thumb.style.backgroundImage = 'url(' + thumbCanvas.toDataURL() + ')'
                    thumb.style.backgroundSize = 'cover'
                }
            }

            // ---- Block Selector ---- //

            const thumbs = element.querySelectorAll('.be-thumb')
            thumbs.forEach(thumb => {
                thumb.addEventListener('click', () => {
                    if (documentData['player-status'] !== 'GOD') return
                    selectedBlock = parseInt(thumb.dataset.index)
                    window.selectedBlock = selectedBlock
                    loadBlock(selectedBlock)
                    thumbs.forEach(t => t.classList.remove('active'))
                    thumb.classList.add('active')
                    element.querySelector('.be-active-index').textContent = selectedBlock
                })
            })

            function loadBlock(index) {
                const board = documentData.board
                if (!board || !board[index]) return
                const block = board[index]
                currentGrid = block.grid && block.grid.length === 64
                    ? [...block.grid]
                    : new Array(64).fill(0)
                renderGrid()
            }

            // ---- Color Chips ---- //

            const chips = element.querySelectorAll('[class^="be-chip"]')
            chips.forEach(chip => {
                chip.addEventListener('click', () => {
                    selectedColor = parseInt(chip.dataset.index)
                    chips.forEach(c => c.classList.remove('active'))
                    chip.classList.add('active')
                })
            })

            // ---- Canvas Rendering ---- //

            function getColor(paletteIndex) {
                return getPaletteColor(paletteIndex)
            }

            function renderGrid() {
                const board = documentData.board
                const block = board && board[selectedBlock]
                const baseColor = block?.color || '#000000'

                for (let i = 0; i < 64; i++) {
                    const ci = currentGrid[i]
                    ctx.fillStyle = ci > 0 ? getColor(ci) : baseColor
                    ctx.fillRect(i % 8, Math.floor(i / 8), 1, 1)
                }
            }

            // ---- Painting ---- //

            let painting = false

            canvas.addEventListener('mousedown', (e) => {
                if (documentData['player-status'] !== 'GOD') return
                if (selectedBlock < 0) return
                painting = true
                paint(e)
            })

            canvas.addEventListener('mousemove', (e) => {
                if (painting) paint(e)
            })

            window.addEventListener('mouseup', () => {
                if (painting) {
                    painting = false
                    scheduleSave()
                }
            })

            canvas.addEventListener('contextmenu', (e) => {
                e.preventDefault()
                if (documentData['player-status'] !== 'GOD') return
                if (selectedBlock < 0) return
                // Right-click: erase (set to 0 / base color)
                const prev = selectedColor
                selectedColor = 0
                paint(e)
                selectedColor = prev
                scheduleSave()
            })

            function paint(e) {
                const rect = canvas.getBoundingClientRect()
                const x = Math.floor((e.clientX - rect.left) / rect.width * 8)
                const y = Math.floor((e.clientY - rect.top) / rect.height * 8)
                if (x < 0 || x > 7 || y < 0 || y > 7) return
                const pi = y * 8 + x
                if (currentGrid[pi] === selectedColor) return
                currentGrid[pi] = selectedColor
                renderGrid()
                // Push to documentData so the world canvas picks it up
                if (documentData.board && documentData.board[selectedBlock]) {
                    documentData.board[selectedBlock].grid = [...currentGrid]
                }
                renderThumb(selectedBlock)
            }

            // ---- Server Sync (debounced) ---- //

            function scheduleSave() {
                if (saveTimer) clearTimeout(saveTimer)
                saveTimer = setTimeout(() => {
                    console.log('[BLOCK-EDITOR] Saving block ' + selectedBlock)
                    element.updateBlock({ blockIndex: selectedBlock, grid: [...currentGrid] })
                }, 300)
            }

            // ---- Initial thumbnail render (deferred) ---- //
            try { for (let i = 0; i < 64; i++) renderThumb(i) } catch(e) {}
        }
    """

    @Bind
    def updateBlock(def specs) {
        println "[BLOCK-EDITOR] updateBlock — block: ${specs.blockIndex}"
        def blockIndex = specs.blockIndex as Integer
        def block = world.board[blockIndex]
        block.grid = specs.grid
        println "[BLOCK-EDITOR] grid updated (${specs.grid.size()} cells)"
    }

    @Bind
    def petMeeple(def specs) {
        def idx = specs.index as Integer
        if (idx >= 0 && idx < world.meeples.size()) {
            def m = world.meeples[idx]
            m.mood = Math.min(128, m.mood + 15)
            println "[BLOCK-EDITOR] petMeeple — ${m.name} mood now ${m.mood}"
        }
    }

    @Bind
    def renameWorld(def specs) {
        def newName = specs.name?.toString()?.trim()
        if (newName) {
            world.name = newName
            println "[BLOCK-EDITOR] renameWorld — '${newName}'"
        }
    }

    @Bind
    def placeTiles(def specs) {
        def batch = specs.tiles as Map
        println "[BLOCK-EDITOR] placeTiles — ${batch.size()} tiles"
        batch.each { tileIndex, blockIndex ->
            world.tiles[tileIndex as Integer] = blockIndex as Integer
        }
    }

    @CSS
    def style = /* language=css */ """
        .be-panel {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .be-label {
            font-size: 9px;
            letter-spacing: 2px;
            color: var(--dim, #333340);
        }
        .be-body {
            display: flex;
            gap: 12px;
            align-items: flex-start;
        }

        /* Block selector: 8x8 grid of thumbnails */
        .be-selector {
            display: grid;
            grid-template-columns: repeat(8, 10px);
            gap: 2px;
        }
        .be-thumb {
            width: 10px;
            height: 10px;
            border: 1px solid #1a1a1a;
            cursor: pointer;
            transition: border-color 0.1s;
            image-rendering: pixelated;
            image-rendering: crisp-edges;
        }
        .be-thumb:hover {
            border-color: var(--neon-cyan, #00ffff);
        }
        .be-thumb.active {
            border-color: var(--neon-pink, #ff006e);
            box-shadow: 0 0 4px rgba(255, 0, 110, 0.4);
        }

        /* Canvas workspace */
        .be-workspace {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 6px;
        }
        .be-canvas {
            width: 128px;
            height: 128px;
            image-rendering: pixelated;
            image-rendering: crisp-edges;
            border: 1px solid #222;
            cursor: crosshair;
        }
        .be-canvas:hover {
            border-color: var(--neon-cyan, #00ffff);
            box-shadow: 0 0 6px rgba(0, 255, 255, 0.15);
        }
        .be-info {
            display: flex;
            gap: 6px;
            font-size: 9px;
            color: var(--dim, #333340);
            letter-spacing: 1px;
        }
        .be-active-index {
            color: var(--neon-pink, #ff006e);
        }

        /* Color chips */
        .be-colors {
            display: grid;
            grid-template-columns: repeat(2, 12px);
            gap: 2px;
            align-self: stretch;
        }
        [class^="be-chip"] {
            width: 12px;
            height: 12px;
            border: 1px solid #1a1a1a;
            cursor: pointer;
            transition: border-color 0.1s;
        }
        [class^="be-chip"]:hover {
            border-color: var(--neon-cyan, #00ffff);
        }
        [class^="be-chip"].active {
            border-color: var(--neon-green, #39ff14);
            box-shadow: 0 0 4px rgba(57, 255, 20, 0.4);
        }
    """

}
