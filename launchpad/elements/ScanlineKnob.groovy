import spaceport.launchpad.element.CSS
import spaceport.launchpad.element.Element
import spaceport.launchpad.element.Javascript

class ScanlineKnob implements Element {

    String prerender(String body, Map attributes) {
        return "<div class='knob-wrap scanline-knob-wrap'>" +
                   "<div class='knob' data-value='50'><div class='knob-indicator'></div></div>" +
                   "<div class='knob-label'>SCAN</div>" +
               "</div>"
    }

    @Javascript
    def constructed = /* language=javascript */ """
        function(element) {
            const knob = element.querySelector('.knob');
            const STORAGE_KEY = 'zzgod-scanline-speed';
            let value = parseInt(localStorage.getItem(STORAGE_KEY) ?? '50', 10);
            let dragging = false;
            let startX = 0;
            let startValue = 0;

            function update() {
                const angle = -135 + (value / 100) * 270;
                knob.style.transform = 'rotate(' + angle + 'deg)';
                const viewport = document.getElementById('game-viewport');
                if (viewport) {
                    if (value === 0) {
                        viewport.style.setProperty('--scanline-speed', '8s');
                        viewport.style.setProperty('--scanline-paused', 'paused');
                    } else {
                        const dur = 0.5 + (1 - value / 100) * 15.5;
                        viewport.style.setProperty('--scanline-speed', dur + 's');
                        viewport.style.setProperty('--scanline-paused', 'running');
                    }
                }
                knob.dataset.value = value;
                localStorage.setItem(STORAGE_KEY, value);
            }

            update();

            knob.addEventListener('mousedown', (e) => {
                e.preventDefault();
                dragging = true;
                startX = e.clientX;
                startValue = value;
            });

            window.addEventListener('mousemove', (e) => {
                if (!dragging) return;
                const delta = e.clientX - startX;
                value = Math.max(0, Math.min(100, startValue + Math.round(delta / 2)));
                update();
            });

            window.addEventListener('mouseup', () => {
                dragging = false;
            });
        }
    """

    @CSS
    def style = /* language=css */ """
        .scanline-knob-wrap .knob-indicator {
            background: var(--neon-green, #39ff14);
            box-shadow: 0 0 6px var(--neon-green, #39ff14);
        }
        .scanline-knob-wrap .knob:hover {
            border-color: var(--neon-green, #39ff14);
            box-shadow: 0 0 10px rgba(57, 255, 20, 0.15), inset 0 0 8px rgba(0, 0, 0, 0.4);
        }
    """

}
