import data.World
import spaceport.bridge.Command
import spaceport.launchpad.element.Bind
import spaceport.launchpad.element.CSS
import spaceport.launchpad.element.Element
import spaceport.launchpad.element.Javascript

class Palette implements Element {

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
            def colors = world?.palette?.colors ?: [:]

            def swatches = new StringBuilder()
            for (int i = 0; i < 16; i++) {
                def key = String.valueOf(i)
                def color = colors[key] ?: DEFAULT_COLORS[key]
                swatches.append(
                    "<div class='palette-swatch' data-index='${i}' style='background-color:${color}' title='Color ${i}'>" +
                        "<span class='palette-index'>${i}</span>" +
                        "<input type='color' value='${color}' class='palette-picker' tabindex='-1'>" +
                    "</div>"
                )
            }

            return "<div class='palette-bar'>" +
                       "<div class='palette-label'>PALETTE</div>" +
                       "<div class='palette-swatches'>${swatches}</div>" +
                   "</div>"
        } catch (Exception e) {
            e.printStackTrace()
            return "<div class='palette-bar'>Palette error: ${e.message}</div>"
        }
    }

    @Javascript
    def constructed = /* language=javascript */ """
        function(element) {
            const swatches = element.querySelectorAll('.palette-swatch')

            swatches.forEach(swatch => {
                const picker = swatch.querySelector('.palette-picker')
                const index = swatch.dataset.index

                swatch.addEventListener('click', (e) => {
                    if (documentData['player-status'] !== 'GOD') return
                    if (e.target !== picker) picker.click()
                })

                picker.addEventListener('input', (e) => {
                    if (documentData['player-status'] !== 'GOD') return
                    const color = e.target.value
                    swatch.style.backgroundColor = color
                    if (documentData.palette && documentData.palette.colors) {
                        documentData.palette.colors[index] = color
                    }
                })

                picker.addEventListener('change', (e) => {
                    if (documentData['player-status'] !== 'GOD') return
                    const color = e.target.value
                    swatch.style.backgroundColor = color
                    // Push to documentData for canvas rendering
                    if (documentData.palette && documentData.palette.colors) {
                        documentData.palette.colors[index] = color
                    }
                    // Update server
                    element.changeColor({ 'index': index, 'color' : color })
                })
                
                
            })
        }
    """

    @Bind
    def changeColor(def specs) {
        println "[PALETTE] changeColor called — index: ${specs.index}, color: ${specs.color}"
        println "[PALETTE] world: ${world?._id ?: 'null'}"
        println "[PALETTE] palette before: ${world?.palette?.colors}"
        world.setPaletteColor(specs.index, specs.color)
        println "[PALETTE] palette after: ${world?.palette?.colors}"
    }

    @CSS
    def style = /* language=css */ """
        .palette-bar {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .palette-label {
            font-size: 9px;
            letter-spacing: 2px;
            color: var(--dim, #333340);
            writing-mode: vertical-lr;
            transform: rotate(180deg);
        }
        .palette-swatches {
            display: grid;
            grid-template-columns: repeat(3, auto);
            gap: 2px;
            width: fit-content;
        }
        .palette-swatch {
            width: 28px;
            height: 28px;
            cursor: pointer;
            position: relative;
            border: 1px solid #222;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: border-color 0.15s, box-shadow 0.15s;
        }
        .palette-swatch:hover {
            border-color: var(--neon-cyan, #00ffff);
            box-shadow: 0 0 6px rgba(0, 255, 255, 0.3);
        }
        .palette-index {
            font-size: 8px;
            color: rgba(255, 255, 255, 0.25);
            pointer-events: none;
            user-select: none;
        }
        .palette-picker {
            position: absolute;
            opacity: 0;
            width: 0;
            height: 0;
            pointer-events: none;
        }
    """

}
